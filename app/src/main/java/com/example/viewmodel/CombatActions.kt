package com.example.viewmodel

import com.example.model.Arma
import com.example.model.Armadura
import com.example.model.ArmorStatsTable
import com.example.model.CharacterSheet
import com.example.model.WeaponStatsTable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

// Domínio de Combate (Aba 5) — motes (comitamento de armas/armaduras),
// Força de Vontade, armas, armaduras, e Contador de Iniciativa. O maior
// domínio depois de Encantos: a lógica de comitamento de motes é
// compartilhada entre armas e armaduras (mesmas contas de
// gastos/comitados), por isso vive toda num arquivo só em vez de
// separada em WeaponsActions/ArmorActions.
class CombatActions(
    private val sheetState: MutableStateFlow<CharacterSheet>,
    private val commitmentError: MutableStateFlow<String?>,
    private val experienceActions: ExperienceActions
) {

    fun updateMotesPessoaisGastos(valInt: Int) = sheetState.update {
        it.copy(motesPessoaisGastos = valInt.coerceIn(it.motesPessoaisComitados, it.motesPessoaisMax()))
    }

    fun updateMotesPerifericosGastos(valInt: Int) = sheetState.update {
        it.copy(motesPerifericosGastos = valInt.coerceIn(it.motesPerifericosComitados, it.motesPerifericosMax()))
    }

    // Trilha única de Força de Vontade (1 a 10): usada tanto pelos botões "+"/"-"
    // (que somam/subtraem 1 ao valor atual) quanto pelo toque direto em uma
    // bolinha da escala (que define o valor diretamente).
    // Nível permanente de Força de Vontade (comprado com XP): nunca abaixo de 5.
    fun updateForcaVontadeBase(valInt: Int) = sheetState.update { current ->
        val novaBase = valInt.coerceIn(5, 10)
        val atual = current.forcaVontadeBase
        if (current.planilhaConcluida) {
            // Depois de concluída, só pode aumentar (com XP) — nunca diminuir.
            if (novaBase < atual) return@update current
            if (novaBase > atual) {
                val custo = (novaBase - atual) * 8
                val debitado = experienceActions.debitarExperiencia(current, custo, "Força de Vontade ($atual → $novaBase)") ?: return@update current
                return@update debitado.copy(forcaVontadeBase = novaBase, forcaVontadeUsados = debitado.forcaVontadeUsados.filter { it <= novaBase }.toSet())
            }
            return@update current
        }
        current.copy(forcaVontadeBase = novaBase, forcaVontadeUsados = current.forcaVontadeUsados.filter { it <= novaBase }.toSet()).let { mutado ->
            experienceActions.aplicarSeSaldoBpPermitir(current, mutado)
        }
    }

    /** Atualiza o gasto de Força de Vontade sem permitir saltos ou remoções parciais. */
    fun toggleForcaVontadeUsado(indice: Int) = sheetState.update { current ->
        val novosUsados = com.example.model.WillpowerTrackLogic.toggle(
            usados = current.forcaVontadeUsados,
            valor = current.forcaVontadeBase,
            indice = indice
        )
        if (novosUsados == current.forcaVontadeUsados) current
        else current.copy(forcaVontadeUsados = novosUsados)
    }

    // --- Weapons CRUD ---
    // Cadastra uma arma a partir das categorias escolhidas no popup de
    // configuração, calculando automaticamente Iniciativa, Decisivo, Defesa,
    // Dano e Dano Mínimo conforme a tabela de estatísticas.
    fun addWeapon(nome: String, habilidadeVinculada: String, atributoBriga: String?, tipoArma: String, categoriaPeso: String, ataqueDesarmado: Boolean = false, etiquetas: List<String> = emptyList()) {
        if (nome.isBlank()) return
        sheetState.update { current ->
            val base = Arma(
                nome = nome.trim(),
                habilidadeVinculada = habilidadeVinculada,
                atributoBriga = if (habilidadeVinculada == "Briga") atributoBriga else null,
                tipoArma = tipoArma,
                categoriaPeso = categoriaPeso,
                ataqueDesarmado = ataqueDesarmado,
                etiquetas = etiquetas
            )
            val ehCorpoACorpo = habilidadeVinculada == "Armas Brancas" || habilidadeVinculada == "Briga"
            val w = if (ehCorpoACorpo) {
                base.copy(
                    iniciativa = current.ataqueIniciativaCalculado(base).toString(),
                    decisivo = current.ataqueDecisivoCalculado(base).toString(),
                    defesa = WeaponStatsTable.corpoACorpo(tipoArma, categoriaPeso).defesa,
                    dano = WeaponStatsTable.corpoACorpo(tipoArma, categoriaPeso).dano.toString(),
                    danoMinimo = WeaponStatsTable.corpoACorpo(tipoArma, categoriaPeso).danoMinimo.toString()
                )
            } else {
                val (dano, danoMinimo) = WeaponStatsTable.distancia(tipoArma, categoriaPeso)
                base.copy(
                    iniciativa = current.ataqueIniciativaCalculado(base).toString(),
                    decisivo = current.ataqueDecisivoCalculado(base).toString(),
                    defesa = 0,
                    dano = dano.toString(),
                    danoMinimo = danoMinimo.toString()
                )
            }
            current.copy(weapons = current.weapons + w)
        }
    }

    // Permite ajustar manualmente qualquer valor efetivo da arma via long
    // press, marcando-a como modificada (indicador visual na Aba 7).
    fun updateWeaponManual(id: String, iniciativa: String, decisivo: String, defesa: Int, dano: String, danoMinimo: String) {
        sheetState.update { current ->
            val index = current.weapons.indexOfFirst { it.id == id }
            if (index < 0) return@update current
            val weapons = current.weapons.toMutableList()
            weapons[index] = weapons[index].copy(
                iniciativa = iniciativa, decisivo = decisivo, defesa = defesa,
                dano = dano, danoMinimo = danoMinimo, modificadaManualmente = true
            )
            current.copy(weapons = weapons)
        }
    }

    fun removeWeapon(id: String) {
        sheetState.update { current ->
            val index = current.weapons.indexOfFirst { it.id == id }
            if (index < 0) return@update current
            current.copy(weapons = current.weapons.toMutableList().also { it.removeAt(index) })
        }
    }

    // Um jogador pode ter mais de uma arma equipada/comitada simultaneamente,
    // LOGICA: arma pesada (mundana ou artefato) ocupa as duas mãos — não pode
    // coexistir com nenhuma outra arma equipada, leve ou média. Compartilhada
    // entre os dois caminhos de equipar (com e sem custo em motes), já que a
    // regra vale para ambos os tipos.
    private fun validarCombinacaoDePeso(current: CharacterSheet, novaArmaId: String, novaCategoriaPeso: String): String? {
        var existeOutraEquipavel = false
        var existePesada = false
        for (weapon in current.weapons) {
            if (!weapon.equipada || weapon.id == novaArmaId) continue
            existeOutraEquipavel = true
            if (weapon.categoriaPeso == "Pesada") existePesada = true
            // Para uma arma nova pesada, qualquer outra já é suficiente para
            // determinar o erro. Para uma nova leve/média, uma pesada também
            // encerra a busca.
            if (novaCategoriaPeso == "Pesada" || existePesada) break
        }
        if (novaCategoriaPeso == "Pesada" && existeOutraEquipavel) {
            return "Uma arma pesada ocupa as duas mãos — não pode ser equipada junto com outra arma."
        }
        if (existePesada) {
            return "Já há uma arma pesada equipada, ocupando as duas mãos."
        }
        return null
    }

    // LOGICA: comita motes numa arma Artefato — NÃO equipa automaticamente.
    // Comitar e Equipar são independentes: dá pra comitar sem equipar. Para
    // equipar, veja toggleWeaponEquipped — lá sim a arma precisa já estar
    // comitada como pré-condição.
    fun comitarArma(id: String, motesPessoais: Int, motesPerifericos: Int): Boolean {
        var sucesso = false
        var erro: String? = null
        sheetState.update { c ->
            val index = c.weapons.indexOfFirst { it.id == id }
            if (index < 0) return@update c
            val weapon = c.weapons[index]
            if (weapon.motesPessoaisComitados > 0 || weapon.motesPerifericosComitados > 0) return@update c
            val custo = if (weapon.ataqueDesarmado) 0 else WeaponStatsTable.comitamento(weapon.tipoArma)
            if (motesPessoais < 0 || motesPerifericos < 0 || motesPessoais + motesPerifericos != custo) return@update c
            if (motesPessoais > c.motesPessoaisDisponiveis() || motesPerifericos > c.motesPerifericosDisponiveis()) {
                erro = "Você não possui motes suficientes na reserva escolhida."
                return@update c
            }
            val weapons = c.weapons.toMutableList()
            weapons[index] = weapon.copy(
                motesPessoaisComitados = motesPessoais,
                motesPerifericosComitados = motesPerifericos
            )
            sucesso = true
            c.copy(
                weapons = weapons,
                motesPessoaisGastos = c.motesPessoaisGastos + motesPessoais,
                motesPerifericosGastos = c.motesPerifericosGastos + motesPerifericos,
                motesPessoaisComitados = c.motesPessoaisComitados + motesPessoais,
                motesPerifericosComitados = c.motesPerifericosComitados + motesPerifericos
            )
        }
        erro?.let { commitmentError.value = it }
        return sucesso
    }

    // Alias mantido por compatibilidade com chamadores existentes.
    fun equipWeaponWithMoteSource(id: String, motesPessoais: Int, motesPerifericos: Int): Boolean =
        comitarArma(id, motesPessoais, motesPerifericos)

    // LOGICA: descomita uma arma. Como "Equipar exige Comitar" é um
    // invariante permanente (não só no momento de marcar a caixa), descomitar
    // também desequipa — não dá pra ficar equipada sem estar comitada.
    fun descomitarArma(id: String) {
        sheetState.update { c ->
            val index = c.weapons.indexOfFirst { it.id == id }
            if (index < 0) return@update c
            val weapon = c.weapons[index]
            if (weapon.motesPessoaisComitados == 0 && weapon.motesPerifericosComitados == 0) return@update c
            val weapons = c.weapons.toMutableList()
            weapons[index] = weapon.copy(equipada = false, motesPessoaisComitados = 0, motesPerifericosComitados = 0)
            c.copy(
                weapons = weapons,
                motesPessoaisGastos = (c.motesPessoaisGastos - weapon.motesPessoaisComitados).coerceAtLeast(0),
                motesPerifericosGastos = (c.motesPerifericosGastos - weapon.motesPerifericosComitados).coerceAtLeast(0),
                motesPessoaisComitados = (c.motesPessoaisComitados - weapon.motesPessoaisComitados).coerceAtLeast(0),
                motesPerifericosComitados = (c.motesPerifericosComitados - weapon.motesPerifericosComitados).coerceAtLeast(0)
            )
        }
    }

    // LOGICA: ver comitarArma. Aqui é o caminho de equipar/desequipar
    // fisicamente. Para armas com custo em motes (Artefato), equipar exige
    // que a arma já esteja comitada — não dá pra marcar "Equipar" antes de
    // "Comitar". Desequipar NÃO libera os motes comitados (Comitar persiste
    // independente de Equipar — ver descomitarArma para isso).
    fun toggleWeaponEquipped(id: String) {
        var erro: String? = null
        var alterado = false
        sheetState.update { c ->
            val index = c.weapons.indexOfFirst { it.id == id }
            if (index < 0) return@update c
            val weapon = c.weapons[index]
            if (!weapon.equipada) {
                val custo = if (weapon.ataqueDesarmado) 0 else WeaponStatsTable.comitamento(weapon.tipoArma)
                val estaComitada = weapon.motesPessoaisComitados > 0 || weapon.motesPerifericosComitados > 0
                if (custo > 0 && !estaComitada) {
                    erro = "Esta arma precisa estar Comitada antes de ser Equipada."
                    return@update c
                }
                erro = validarCombinacaoDePeso(c, weapon.id, weapon.categoriaPeso)
                if (erro != null) return@update c
            }
            val weapons = c.weapons.toMutableList()
            weapons[index] = weapon.copy(equipada = !weapon.equipada)
            alterado = true
            c.copy(weapons = weapons)
        }
        if (erro != null) {
            commitmentError.value = erro
        }
    }

    // --- Armaduras CRUD ---
    fun addArmor(nome: String, tipoArmadura: String, categoriaPeso: String, marcadores: List<String> = emptyList()) {
        if (nome.isBlank()) return
        val stats = ArmorStatsTable.stats(tipoArmadura, categoriaPeso)
        val armor = Armadura(
            nome = nome.trim(),
            tipoArmadura = tipoArmadura,
            categoriaPeso = categoriaPeso,
            absorcao = stats.absorcao,
            dureza = stats.dureza,
            penalidadeMobilidade = stats.penalidadeMobilidade,
            marcadores = marcadores
        )
        sheetState.update { it.copy(armaduras = it.armaduras + armor) }
    }

    fun removeArmor(id: String) {
        sheetState.update { current ->
            val index = current.armaduras.indexOfFirst { it.id == id }
            if (index < 0) return@update current
            current.copy(armaduras = current.armaduras.toMutableList().also { it.removeAt(index) })
        }
    }

    // Apenas uma armadura pode estar comitada por vez: marcar uma desmarca as demais.
    // O custo de comitamento depende da categoria (Mundana = 0; Artefato = 4/5/6
    // conforme o peso), e comitar é bloqueado se ultrapassar o limite de motes.
    // LOGICA: mesmo fluxo de motes da arma, mas com a regra extra de que só
    // uma armadura pode estar comitada por vez — trocar de armadura devolve
    // primeiro os motes da anterior antes de debitar a nova. Não
    // simplificar/reverter sem confirmar antes.
    fun equipArmorWithMoteSource(id: String, motesPessoais: Int, motesPerifericos: Int): Boolean {
        var sucesso = false
        var erro: String? = null
        sheetState.update { c ->
            val index = c.armaduras.indexOfFirst { it.id == id }
            if (index < 0) return@update c
            val armor = c.armaduras[index]
            if (armor.equipada) return@update c
            val custo = ArmorStatsTable.stats(armor.tipoArmadura, armor.categoriaPeso).comitamento
            if (motesPessoais < 0 || motesPerifericos < 0 || motesPessoais + motesPerifericos != custo) {
                return@update c
            }
            if (motesPessoais > c.motesPessoaisDisponiveis() || motesPerifericos > c.motesPerifericosDisponiveis()) {
                erro = "Você não possui motes suficientes na reserva escolhida."
                return@update c
            }
            val armaduras = c.armaduras.toMutableList()
            var motesPessoaisAnteriores = 0
            var motesPerifericosAnteriores = 0
            for (i in armaduras.indices) {
                val a = armaduras[i]
                if (a.equipada) {
                    motesPessoaisAnteriores += a.motesPessoaisComitados
                    motesPerifericosAnteriores += a.motesPerifericosComitados
                    if (i != index) {
                        armaduras[i] = a.copy(
                            equipada = false,
                            motesPessoaisComitados = 0,
                            motesPerifericosComitados = 0
                        )
                    }
                }
            }
            armaduras[index] = armaduras[index].copy(
                equipada = true,
                motesPessoaisComitados = motesPessoais,
                motesPerifericosComitados = motesPerifericos
            )
            sucesso = true
            c.copy(
                armaduras = armaduras,
                motesPessoaisGastos = c.motesPessoaisGastos - motesPessoaisAnteriores + motesPessoais,
                motesPerifericosGastos = c.motesPerifericosGastos - motesPerifericosAnteriores + motesPerifericos,
                motesPessoaisComitados = c.motesPessoaisComitados - motesPessoaisAnteriores + motesPessoais,
                motesPerifericosComitados = c.motesPerifericosComitados - motesPerifericosAnteriores + motesPerifericos
            )
        }
        erro?.let { commitmentError.value = it }
        return sucesso
    }

    // LOGICA: ver equipArmorWithMoteSource — caminho de desequipar sem
    // substituir por outra, devolve os motes comitados pela armadura.
    fun toggleArmorEquipped(id: String) {
        var erro: String? = null
        sheetState.update { c ->
            val index = c.armaduras.indexOfFirst { it.id == id }
            if (index < 0) return@update c
            val armor = c.armaduras[index]
            if (!armor.equipada) {
                val custoArmadura = ArmorStatsTable.stats(armor.tipoArmadura, armor.categoriaPeso).comitamento
                if (custoArmadura > 0) {
                    erro = "Esta armadura exige escolher a reserva de motes ao comitar."
                    return@update c
                }
                val prospectivo = c.weapons.sumOf { it.motesPessoaisComitados + it.motesPerifericosComitados } + custoArmadura
                if (prospectivo > c.limiteComitamentoCalculado()) {
                    erro = "Você não possui motes suficientes para o comitamento."
                    return@update c
                }
                val armaduras = c.armaduras.toMutableList()
                var motesPessoaisAnteriores = 0
                var motesPerifericosAnteriores = 0
                for (i in armaduras.indices) {
                    val a = armaduras[i]
                    if (a.equipada || a.motesPessoaisComitados != 0 || a.motesPerifericosComitados != 0) {
                        if (a.equipada) {
                            motesPessoaisAnteriores += a.motesPessoaisComitados
                            motesPerifericosAnteriores += a.motesPerifericosComitados
                        }
                        if (i != index) {
                            armaduras[i] = a.copy(
                                equipada = false,
                                motesPessoaisComitados = 0,
                                motesPerifericosComitados = 0
                            )
                        }
                    }
                }
                armaduras[index] = armaduras[index].copy(equipada = true)
                return@update c.copy(
                    armaduras = armaduras,
                    motesPessoaisGastos = (c.motesPessoaisGastos - motesPessoaisAnteriores).coerceAtLeast(0),
                    motesPerifericosGastos = (c.motesPerifericosGastos - motesPerifericosAnteriores).coerceAtLeast(0),
                    motesPessoaisComitados = (c.motesPessoaisComitados - motesPessoaisAnteriores).coerceAtLeast(0),
                    motesPerifericosComitados = (c.motesPerifericosComitados - motesPerifericosAnteriores).coerceAtLeast(0)
                )
            }
            val armaduras = c.armaduras.toMutableList()
            armaduras[index] = armor.copy(equipada = false, motesPessoaisComitados = 0, motesPerifericosComitados = 0)
            c.copy(
                armaduras = armaduras,
                motesPessoaisGastos = (c.motesPessoaisGastos - armor.motesPessoaisComitados).coerceAtLeast(0),
                motesPerifericosGastos = (c.motesPerifericosGastos - armor.motesPerifericosComitados).coerceAtLeast(0),
                motesPessoaisComitados = (c.motesPessoaisComitados - armor.motesPessoaisComitados).coerceAtLeast(0),
                motesPerifericosComitados = (c.motesPerifericosComitados - armor.motesPerifericosComitados).coerceAtLeast(0)
            )
        }
        erro?.let { commitmentError.value = it }
    }

    // --- Contador de Iniciativa ---
    fun updateIniciativa(valInt: Int) = sheetState.update {
        it.copy(iniciativaValor = valInt.coerceIn(-999, 999))
    }

    fun ajustarIniciativa(delta: Int) = sheetState.update { current ->
        val base = current.iniciativaValor ?: 0
        current.copy(iniciativaValor = (base + delta).coerceIn(-999, 999))
    }

    fun limparCombate() = sheetState.update { it.copy(iniciativaValor = null) }
}
