package com.example.viewmodel

import com.example.model.CharacterSheet
import com.example.model.HabilidadeCustomizada
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

// Ações de Artes Marciais (Aba 4).
//
// Recebe experienceActions e charmsActions: updateMartialArtValue precisa
// debitar XP no Modo Experiência e sanitizar Encantos sempre que o nível
// muda (mesmo padrão de AbilitiesActions.setAbilityRating).
class MartialArtsActions(
    private val sheetState: MutableStateFlow<CharacterSheet>,
    private val experienceActions: ExperienceActions,
    private val charmsActions: CharmsActions
) {

    // Nome do Mérito que habilita a Habilidade de Artes Marciais — regra
    // do próprio sistema (ver descrição do mérito: "Este Mérito permite
    // que o personagem adquira a Habilidade de Artes Marciais"). Sem ele,
    // nenhum estilo pode ser criado, independente de quantos o jogador já
    // tenha — o Mérito habilita a Habilidade como um todo, não é
    // consumido por estilo.
    private val MERITO_ARTISTA_MARCIAL = "Artista Marcial"
    private val HABILIDADE_BRIGA = "Briga"

    fun possuiMeritoArtistaMarcial(sheet: CharacterSheet): Boolean =
        sheet.merits.any { it.nome.trim().equals(MERITO_ARTISTA_MARCIAL, ignoreCase = true) }

    fun possuiBrigaMinima(sheet: CharacterSheet): Boolean =
        (sheet.abilities[HABILIDADE_BRIGA] ?: 0) >= 1

    /**
     * Pré-requisitos para cadastrar um estilo de Arte Marcial:
     * 1) Briga ≥ 1
     * 2) Mérito "Artista Marcial" (exceto Sangue de Dragão, que já tem acesso nativo)
     * 3) O nome não pode estar duplicado entre os estilos já cadastrados
     *
     * Retorna false (sem criar nada) quando falta algum pré-requisito —
     * a UI usa esse retorno pra mostrar a mensagem explicando o motivo do
     * bloqueio, em vez de fechar o diálogo como se tivesse dado certo.
     */
    fun addMartialArt(nome: String): Boolean {
        val cleanName = nome.trim()
        if (cleanName.isBlank()) return false
        var adicionada = false
        sheetState.update { current ->
            // A validação e a inserção usam o mesmo snapshot. Isso elimina as
            // duas leituras externas de sheetState.value e evita que uma
            // mudança concorrente entre validação e mutação altere o resultado.
            val ehSangueDeDragao = current.tipoPersonagem.equals("SangueDeDragao", ignoreCase = false)
            val temMerito = possuiMeritoArtistaMarcial(current)
            val temBriga = possuiBrigaMinima(current)
            val jaPossuiEstilo = current.martialArts.any {
                it.nome.trim().equals(cleanName, ignoreCase = true)
            }
            val habilitada = temBriga && (ehSangueDeDragao || temMerito) && !jaPossuiEstilo
            if (!habilitada) return@update current
            adicionada = true
            val novaArte = HabilidadeCustomizada(nome = cleanName, valor = 0)
            current.copy(martialArts = current.martialArts + novaArte)
        }
        return adicionada
    }

    fun removeMartialArt(id: String) {
        sheetState.update { current ->
            val index = current.martialArts.indexOfFirst { it.id == id }
            if (index < 0) return@update current
            current.copy(martialArts = current.martialArts.toMutableList().also { it.removeAt(index) })
        }
    }

    fun updateMartialArtValue(id: String, valInt: Int) {
        sheetState.update { current ->
            val novo = valInt.coerceIn(0, 5)
            val index = current.martialArts.indexOfFirst { it.id == id }
            if (index < 0) return@update current
            val ma = current.martialArts[index]
            val atual = ma.valor
            if (novo == atual) return@update current

            if (current.planilhaConcluida && novo > atual) {
                val favorecida = current.casteAbilities.contains("Briga") || current.favoredAbilities.contains("Briga")
                val custo = (atual until novo).sumOf { n -> SheetCalculations.custoAumentoHabilidade(n, favorecida) }
                val debitado = experienceActions.debitarExperiencia(current, custo, "Arte Marcial: ${ma.nome} ($atual → $novo)") ?: return@update current
                // Só materializa a lista depois de confirmar o débito de XP.
                // Em tentativas sem saldo, evita uma cópia mutável desnecessária.
                val martialArts = debitado.martialArts.toMutableList()
                martialArts[index] = ma.copy(valor = novo)
                // Aumentar Artes Marciais só pode liberar requisitos; não
                // invalida Encantos já adquiridos. Evita percorrer novamente
                // todos os Encantos no caminho quente de avanço por XP.
                return@update debitado.copy(martialArts = martialArts)
            }

            val martialArts = current.martialArts.toMutableList()
            martialArts[index] = ma.copy(valor = novo)
            experienceActions.aplicarSeSaldoBpPermitir(current, current.copy(martialArts = martialArts))
        }
    }
}
