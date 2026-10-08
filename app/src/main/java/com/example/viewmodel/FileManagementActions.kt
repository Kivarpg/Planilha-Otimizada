package com.example.viewmodel

import com.example.model.CharacterType
import com.example.model.isDragonBlooded
import com.example.model.isLunar
import com.example.data.SheetRepository
import com.example.data.ShareCodeCodec
import com.example.model.CharacterSheet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

// Camada de orquestração: Novo/Carregar/Salvar/Exportar/Importar/Backup,
// e Planilha Concluída/reversão/validação de salvamento. Deliberadamente
// extraída por último — é o único domínio que lê o resultado de TODOS os
// outros ao mesmo tempo (computeCompletionIssues, calculateBpBreakdown),
// e depende de mais estado de UI (savedSheets, saveValidationEvent,
// showReversionConfirm) do que qualquer domínio de aba específica.
class FileManagementActions(
    private val sheetState: MutableStateFlow<CharacterSheet>,
    private val savedSheets: MutableStateFlow<List<CharacterSheet>>,
    private val saveValidationEvent: MutableStateFlow<ValidationResult?>,
    private val showReversionConfirm: MutableStateFlow<Boolean>,
    private val repository: SheetRepository,
    private val charmsActions: CharmsActions,
    private val encantosSolares: List<com.example.data.EncantoSolarDefinition>,
    private val feiticos: List<com.example.data.FeiticoDefinition>,
    private val onSheetChanged: (CharacterSheet) -> Unit = {}
) {

    fun reloadSavedSheets() {
        savedSheets.value = repository.getAllSheets()
    }

    fun loadSheet(sheet: CharacterSheet) {
        val normalized = charmsActions.sanitizeKnownCharms(SheetCalculations.normalizeEssence(sheet))
        sheetState.value = normalized
        onSheetChanged(normalized)
        com.example.ui.theme.aplicarPaletaPorTemplate(normalized.tipoPersonagem)
        repository.saveActiveSheet(normalized)
    }

    fun createNewSheet(tipoPersonagem: String = CharacterType.SOLAR) {
        val essenciaInicial = if (tipoPersonagem.isDragonBlooded()) 2 else 1
        val newSheet = CharacterSheet(id = UUID.randomUUID().toString(), tipoPersonagem = tipoPersonagem, essencia = essenciaInicial)
        sheetState.value = newSheet
        onSheetChanged(newSheet)
        repository.saveActiveSheet(newSheet)
        reloadSavedSheets()
    }

    fun exportarComoCodigo(): Result<String> {
        val current = sheetState.value
        if (SheetCalculations.computeCompletionIssues(current).isNotEmpty()) {
            return Result.failure(IllegalStateException("A planilha precisa estar completa (luz verde) antes de gerar um código de compartilhamento."))
        }
        return ShareCodeCodec.exportar(current, encantosSolares, feiticos)
    }

    fun aplicarPlanilhaImportada(sheet: CharacterSheet) {
        val normalized = charmsActions.sanitizeKnownCharms(SheetCalculations.normalizeEssence(sheet))
        sheetState.value = normalized
        onSheetChanged(normalized)
    }

    fun listarBackupsPeriodicos(): List<SheetRepository.BackupSnapshot> =
        repository.listarBackups()

    fun restaurarBackupPeriodico(snapshot: SheetRepository.BackupSnapshot): Boolean {
        return try {
            val sheet = com.example.data.CharacterSheetJsonCodec.decode(snapshot.json)
            val normalized = charmsActions.sanitizeKnownCharms(SheetCalculations.normalizeEssence(sheet))
            sheetState.value = normalized
            onSheetChanged(normalized)
            repository.saveActiveSheet(normalized)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun deleteSheet(sheetId: String) {
        repository.deleteSheet(sheetId)
        reloadSavedSheets()
        sheetState.value = repository.loadActiveSheet()
        onSheetChanged(sheetState.value)
    }

    fun dismissValidationDialog() {
        saveValidationEvent.value = null
    }

    fun confirmSaveIncompleteSheet() {
        salvarComNomeSequencial(sheetState.value)
        reloadSavedSheets()
        saveValidationEvent.value = ValidationResult(
            isValid = true,
            errors = emptyList(),
            requiresConfirmation = false
        )
    }

    private fun salvarComNomeSequencial(sheet: CharacterSheet): CharacterSheet {
        val dataFormatada = java.text.SimpleDateFormat("dd.MM.yy", java.util.Locale.forLanguageTag("pt-BR")).format(java.util.Date())
        val numero = if (sheet.numeroSequencial == 0) repository.proximoNumeroSequencial() else sheet.numeroSequencial
        val atualizado = sheet.copy(numeroSequencial = numero, dataSalvamento = dataFormatada)
        sheetState.value = atualizado
        repository.saveActiveSheet(atualizado)
        return atualizado
    }

    fun toggleModoLivre() {
        sheetState.update { it.copy(modoLivre = !it.modoLivre) }
    }

    // Conclusão automática (ver observador em SheetViewModel.init): só age
    // quando a planilha JÁ está completa, silenciosamente — não existe mais
    // gatilho manual do usuário aqui, então não há por que reportar
    // pendências (não haveria pra quem mostrar isso).
    fun marcarPlanilhaConcluida() {
        val current = sheetState.value
        if (current.planilhaConcluida) return
        if (SheetCalculations.computeCompletionIssues(current).isNotEmpty()) return
        val normalized = charmsActions.sanitizeKnownCharms(SheetCalculations.normalizeEssence(current))
        val snapshot = com.example.data.CharacterSheetJsonCodec.encode(normalized.copy(experienciaGastaTotal = 0))
        sheetState.update { normalized.copy(planilhaConcluida = true, snapshotConclusao = snapshot, experienciaGastaTotal = 0, historicoExperiencia = emptyList()) }
    }

    fun solicitarDesmarcarPlanilhaConcluida() {
        if (!sheetState.value.planilhaConcluida) return
        showReversionConfirm.value = true
    }

    fun cancelarReversaoPlanilhaConcluida() {
        showReversionConfirm.value = false
    }

    fun confirmarReversaoPlanilhaConcluida() {
        val current = sheetState.value
        val restaurado = if (current.snapshotConclusao.isNotBlank()) {
            try {
                com.example.data.CharacterSheetJsonCodec.decode(current.snapshotConclusao)
            } catch (e: Exception) {
                current
            }
        } else current
        sheetState.value = SheetCalculations.normalizeEssence(restaurado.copy(planilhaConcluida = false, snapshotConclusao = "", experienciaGastaTotal = 0, historicoExperiencia = emptyList()))
        showReversionConfirm.value = false
    }

    fun validateAndSaveSheet() {
        val current = sheetState.value
        val bpInfo = SheetCalculations.calculateBpBreakdown(current)
        val errors = mutableListOf<String>()

        if (bpInfo.remainingBalance < 0) {
            errors.add("Saldo de Pontos de Bônus insuficiente (${bpInfo.remainingBalance} pontos restantes). O saldo final deve ser maior ou igual a 0.")
        }
        if (current.tipoPersonagem.isLunar()) {
            if (current.lunarCasteAttributesEscolhidos.size != 2) {
                errors.add("A quantidade de Atributos de Casta Lunar deve ser exatamente 2 (atualmente ${current.lunarCasteAttributesEscolhidos.size}).")
            }
            if (current.favoredAttributes.size != 2) {
                errors.add("A quantidade de Atributos Favorecidos Lunar deve ser exatamente 2 (atualmente ${current.favoredAttributes.size}).")
            }
            val duplicados = current.lunarCasteAttributesEscolhidos.toSet().intersect(current.favoredAttributes.toSet())
            if (duplicados.isNotEmpty()) {
                errors.add("Atributos de Casta Lunar não podem também ser Atributos Favorecidos: ${duplicados.joinToString(", ")}.")
            }
        } else {
            if (current.casteAbilities.size != 5) {
                errors.add("A quantidade de Habilidades de Casta marcadas deve ser exatamente 5 (atualmente ${current.casteAbilities.size}).")
            }
            if (current.favoredAbilities.size != 5) {
                errors.add("A quantidade de Habilidades Favorecidas marcadas deve ser exatamente 5 (atualmente ${current.favoredAbilities.size}).")
            }
            if (current.supernalAbility.isNullOrEmpty()) {
                errors.add("A Habilidade Supernal não pode ser nula. Selecione 1 Habilidade Supernal dentre as Habilidades de Casta.")
            }
        }
        val casteOrFavoredCharmsCount = SheetCalculations.contarEncantosDeCastaOuFavorecidos(current)
        if (casteOrFavoredCharmsCount < 5) {
            errors.add("A planilha deve possuir pelo menos 5 Encantos/Feitiços de Casta ou Favorecidos (atualmente $casteOrFavoredCharmsCount).")
        }

        if (errors.isNotEmpty()) {
            saveValidationEvent.value = ValidationResult(isValid = false, errors = errors, requiresConfirmation = true)
        } else {
            salvarComNomeSequencial(current)
            reloadSavedSheets()
            saveValidationEvent.value = ValidationResult(isValid = true, errors = emptyList())
        }
    }
}
