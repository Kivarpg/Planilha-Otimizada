package com.example.data

import com.example.model.ArmaEncontro
import com.example.model.ArmaduraEncontro
import com.example.model.ArmorStatsTable
import com.example.model.CaixaVitalidade
import com.example.model.WeaponStatsTable
import kotlin.random.Random

/** Pure equipment and vitality-track rules used by encounter generation. */
object EncounterEquipmentService {
    private val PESOS_ARMA_ARMADURA = listOf("Leve", "Média", "Pesada")

    /**
     * Monta uma ArmaEncontro a partir de uma escolha MANUAL do catálogo
     * (não sorteada) — pedido explícito do usuário: editor de equipamento
     * do NPC, permitindo trocar a arma já atribuída por qualquer uma
     * cadastrada pra Habilidade escolhida. Reaproveita a mesma tabela de
     * estatísticas por peso/tipo já usada em selecionarArma().
     */
    fun montarArmaSelecionada(
        habilidadeCombate: String,
        armaCatalogo: WeaponCatalog.CatalogoArma
    ): ArmaEncontro {
        val tipo = "Artefato"
        val peso = armaCatalogo.peso
        val nomeArma = "${armaCatalogo.nome} ($peso)"
        val ataqueADistancia = habilidadeCombate in listOf("Arqueirismo", "Arremesso") ||
            (habilidadeCombate == "Artes Marciais" &&
                armaCatalogo.etiquetas.any { it.contains("Pistola de Fogo", ignoreCase = true) })
        return if (ataqueADistancia) {
            val (dano, danoMinimo) = WeaponStatsTable.distancia(tipo, peso)
            val precisaoRef = WeaponStatsTable.corpoACorpo(tipo, peso).precisao
            ArmaEncontro(
                nome = nomeArma,
                peso = peso,
                tipo = tipo,
                precisao = precisaoRef,
                dano = dano,
                defesa = 0,
                motesComitados = WeaponStatsTable.comitamento(tipo),
                etiquetas = armaCatalogo.etiquetas
            )
        } else {
            val stats = WeaponStatsTable.corpoACorpo(tipo, peso)
            ArmaEncontro(
                nome = nomeArma,
                peso = peso,
                tipo = tipo,
                precisao = stats.precisao,
                dano = stats.dano,
                defesa = stats.defesa,
                motesComitados = WeaponStatsTable.comitamento(tipo),
                etiquetas = armaCatalogo.etiquetas
            )
        }
    }

    /**
     * Monta uma ArmaduraEncontro a partir de uma escolha MANUAL do
     * catálogo — mesmo espírito de montarArmaSelecionada(), pro editor de
     * equipamento do NPC.
     */
    fun montarArmaduraSelecionada(armaduraCatalogo: ArmorCatalog.CatalogoArmadura): ArmaduraEncontro {
        val peso = armaduraCatalogo.peso
        val stats = ArmorStatsTable.stats("Artefato", peso)
        return ArmaduraEncontro(
            nome = armaduraCatalogo.nome,
            peso = peso,
            tipo = "Artefato",
            absorcao = stats.absorcao,
            dureza = stats.dureza,
            penalidadeMobilidade = stats.penalidadeMobilidade,
            motesComitados = stats.comitamento,
            marcadores = armaduraCatalogo.marcadores,
            custoMeritoArtefato = armaduraCatalogo.custoMerito
        )
    }

    fun selecionarArma(habilidadeCombate: String, random: Random, artefato: Boolean = habilidadeCombate != "Briga"): ArmaEncontro? {
        // Corrigido: antes, "habilidadeCombate == Briga" sempre forçava
        // "Mundana" aqui dentro, IGNORANDO o parâmetro artefato — então
        // passar artefato=true explicitamente (como o chamador agora faz)
        // não tinha efeito nenhum pra Briga. Só o parâmetro decide agora.
        val tipo = if (!artefato) "Mundana" else "Artefato"
        val pesosPermitidos = when (habilidadeCombate) {
            "Briga" -> listOf("Leve")
            "Arremesso" -> listOf("Leve", "Média")
            else -> PESOS_ARMA_ARMADURA // Armas Brancas, Arqueirismo
        }
        val peso = pesosPermitidos.random(random)
        // Nome real do catálogo (Artefato) em vez do genérico "Arma de X
        // (peso)" — pedido explícito do usuário. Mundana continua com o
        // nome genérico, já que o catálogo é só de armas Artefato.
        val candidatas = if (artefato) WeaponCatalog.candidatas(habilidadeCombate, peso) else emptyList()
        val armaCatalogo = candidatas.randomOrNull(random)
        val nomeArma = armaCatalogo?.let { "${it.nome} ($peso)" } ?: "Arma de $habilidadeCombate ($peso)"
        val etiquetasArma = armaCatalogo?.etiquetas ?: emptyList()
        val distanciaOuCorpoACorpo = habilidadeCombate in listOf("Arqueirismo", "Arremesso")
        return if (distanciaOuCorpoACorpo) {
            val (dano, danoMinimo) = WeaponStatsTable.distancia(tipo, peso)
            // "Distância" não tem tabela de Precisão própria no app — ver
            // nota no início do arquivo. Reaproveita a Precisão de
            // corpo-a-corpo Artefato pro mesmo peso, só como valor de
            // referência consistente.
            val precisaoRef = WeaponStatsTable.corpoACorpo(tipo, peso).precisao
            ArmaEncontro(
                nome = nomeArma,
                peso = peso,
                tipo = tipo,
                precisao = precisaoRef,
                dano = dano,
                defesa = 0,
                motesComitados = WeaponStatsTable.comitamento(tipo),
                etiquetas = etiquetasArma
            )
        } else {
            val stats = WeaponStatsTable.corpoACorpo(tipo, peso)
            ArmaEncontro(
                nome = nomeArma,
                peso = peso,
                tipo = tipo,
                precisao = stats.precisao,
                dano = stats.dano,
                defesa = stats.defesa,
                motesComitados = WeaponStatsTable.comitamento(tipo),
                etiquetas = etiquetasArma
            )
        }
    }

    /**
     * Equipamento padrão do gerador de Encontros: todo NPC recebe uma Arma
     * Artefato e uma Armadura Artefato. Os custos são registrados depois na
     * distribuição de Méritos: 3 pontos para cada Artefato.
     */
    fun selecionarEquipamentoParaEncontro(
        habilidadeCombate: String,
        random: Random,
        permitirDoisArtefatos: Boolean
    ): Pair<ArmaEncontro?, ArmaduraEncontro> {
        if (permitirDoisArtefatos || habilidadeCombate == "Briga") {
            // Corrigido: Briga caía no valor PADRÃO de selecionarArma
            // (artefato = habilidadeCombate != "Briga" → false), então
            // NPCs desarmados nunca recebiam Arma Artefato — contrariando
            // a regra de que todo NPC recebe Arma E Armadura Artefato
            // simultaneamente (usada pelo serviço de Méritos, que exige
            // isso via require()). Forçado explicitamente artefato=true.
            return selecionarArma(habilidadeCombate, random, artefato = true) to selecionarArmadura(random, habilidadeCombate)
        }
        val armaArtefato = random.nextBoolean()
        val arma = selecionarArma(habilidadeCombate, random, artefato = armaArtefato)
        val armadura = selecionarArmadura(
            random = random,
            habilidadeCombate = habilidadeCombate,
            tipoArmadura = if (armaArtefato) "Mundana" else "Artefato"
        )
        return arma to armadura
    }

    fun selecionarArmadura(random: Random, habilidadeCombate: String? = null, tipoArmadura: String = "Artefato"): ArmaduraEncontro {
        // Combatentes à distância (Arqueirismo/Arremesso): armadura pesada
        // deve ser rara (1 em cada 10) — leve/média nos outros 9 em 10,
        // pra não prejudicar Evasão. Corpo a corpo continua sorteando os
        // 3 pesos igualmente.
        val ehDistancia = habilidadeCombate == "Arqueirismo" || habilidadeCombate == "Arremesso"
        val peso = if (ehDistancia) {
            if (random.nextInt(0, 10) < 9) listOf("Leve", "Média").random(random) else "Pesada"
        } else {
            PESOS_ARMA_ARMADURA.random(random)
        }
        val stats = ArmorStatsTable.stats(tipoArmadura, peso)
        // Nome real do catálogo (Artefato) em vez do genérico "Armadura X
        // (Artefato)" — mesmo padrão das armas. A Armadura de Seda (custo
        // de Mérito 4 em vez do padrão 3) é favorecida pra NPCs de Briga
        // na maioria das vezes, mas não exclusivamente — pedido explícito
        // do usuário.
        val armaduraCatalogo = if (tipoArmadura == "Artefato") ArmorCatalog.sortear(peso, habilidadeCombate, random) else null
        val nomeArmadura = armaduraCatalogo?.let { "${it.nome} ($peso)" } ?: "Armadura $peso ($tipoArmadura)"
        return ArmaduraEncontro(
            nome = nomeArmadura,
            peso = peso,
            tipo = tipoArmadura,
            absorcao = stats.absorcao,
            dureza = stats.dureza,
            penalidadeMobilidade = stats.penalidadeMobilidade,
            motesComitados = stats.comitamento,
            marcadores = armaduraCatalogo?.marcadores ?: emptyList(),
            custoMeritoArtefato = armaduraCatalogo?.custoMerito ?: 3
        )
    }

    fun trilhaVitalidadePorVigor(vigor: Int, corpoDeTouroCount: Int): List<CaixaVitalidade> =
        EncounterVitalityService.trilhaVitalidadeSolar(vigor, corpoDeTouroCount)

    /**
     * Ajusta equipamento à rota marcial já escolhida. O estilo manda no
     * equipamento, não o contrário. Preserva equipamento existente quando legal.
     */
    fun ajustarParaEstiloMarcial(
        estilo: EstiloArteMarcialDefinition,
        armaAtual: ArmaEncontro?,
        armaduraAtual: ArmaduraEncontro,
        random: Random
    ): Pair<ArmaEncontro?, ArmaduraEncontro> {
        val armadura = if (EncounterMartialArtsSelectionService.armaduraCompativel(estilo, armaduraAtual.peso)) {
            armaduraAtual
        } else {
            val pesos = when (estilo.armaduraCategoria.lowercase()) {
                "leve" -> listOf("Leve")
                "leve_media" -> listOf("Leve", "Média")
                "media" -> listOf("Média")
                "incompativel" -> emptyList()
                else -> listOf("Leve", "Média", "Pesada")
            }
            if (pesos.isEmpty()) {
                // Estilo incompatível com armadura: representa ausência de proteção
                // com a estrutura existente, sem inventar um novo tipo de equipamento.
                ArmaduraEncontro(
                    nome = "Sem armadura", peso = "Leve", tipo = "Mundana",
                    absorcao = 0, dureza = 0, penalidadeMobilidade = 0,
                    motesComitados = 0, marcadores = emptyList(), custoMeritoArtefato = 0
                )
            } else selecionarArmadura(random, "Briga", "Artefato").let { candidata ->
                if (candidata.peso in pesos) candidata else {
                    val peso = pesos.random(random)
                    val stats = ArmorStatsTable.stats("Artefato", peso)
                    val cat = ArmorCatalog.sortear(peso, "Briga", random)
                    ArmaduraEncontro(
                        nome = cat?.let { "${it.nome} ($peso)" } ?: "Armadura $peso (Artefato)",
                        peso = peso, tipo = "Artefato", absorcao = stats.absorcao,
                        dureza = stats.dureza, penalidadeMobilidade = stats.penalidadeMobilidade,
                        motesComitados = stats.comitamento, marcadores = cat?.marcadores ?: emptyList(),
                        custoMeritoArtefato = cat?.custoMerito ?: 3
                    )
                }
            }
        }

        val armaJaCompativel = armaAtual != null &&
            EncounterMartialAttackCompatibility.supportsWeapon(estilo, armaAtual.nome)
        val arma = when {
            estilo.armaDoEstiloModo.equals("desarmado", true) -> selecionarArma("Briga", random, artefato = false)
            armaJaCompativel -> armaAtual
            else -> {
                val candidatas = WeaponCatalog.candidatasPorHabilidade("Artes Marciais").filter { cat ->
                    EncounterMartialAttackCompatibility.supportsWeapon(estilo, cat.nome)
                }
                candidatas.randomOrNull(random)?.let { montarArmaSelecionada("Artes Marciais", it) }
                    ?: selecionarArma("Briga", random, artefato = false)
            }
        }
        return arma to armadura
    }

}
