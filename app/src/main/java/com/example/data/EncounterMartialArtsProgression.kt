package com.example.data

import com.example.model.EncantoEncontro
import com.example.model.HistoricoXpBatch
import com.example.model.NpcEncontro
import kotlin.random.Random

/**
 * Progressão da rota marcial principal. Não mistura o domínio de Briga:
 * somente Encantos pertencentes ao estilo persistido entram nesta compra.
 */
internal object EncounterMartialArtsProgression {
    private const val CUSTO_ENCANTO_MARCIAL_XP = 5

    private data class EncantoMarcialPreparado(
        val nomeNormalizado: String,
        val requisitos: List<String>
    )

    private data class EstiloMarcialPreparado(
        val porNome: Map<String, EncantoArteMarcialDefinition>,
        val encantos: Map<EncantoArteMarcialDefinition, EncantoMarcialPreparado>
    )

    // Nome normalizado, índice por nome e parsing de pré-requisitos dependem
    // apenas do catálogo imutável do estilo. Mantê-los por identidade da lista
    // evita reconstruir essas estruturas em cada clique de +XP sem reter
    // catálogos antigos quando o ViewModel deixa de referenciá-los.
    private val estilosPreparadosCache =
        java.util.Collections.synchronizedMap(
            java.util.WeakHashMap<EstiloArteMarcialDefinition, EstiloMarcialPreparado>()
        )

    private fun estiloPreparado(estilo: EstiloArteMarcialDefinition): EstiloMarcialPreparado =
        synchronized(estilosPreparadosCache) {
            estilosPreparadosCache[estilo] ?: run {
                val porNome = estilo.encantos.associateBy {
                    EncantosSolaresCatalog.normalize(it.nome)
                }
                val encantos = estilo.encantos.associateWith { encanto ->
                    val raw = encanto.preRequisitos.trim()
                    val requisitos = if (
                        raw.isBlank() || raw.equals("Nenhum", true) || raw == "—" || raw == "-"
                    ) {
                        emptyList()
                    } else {
                        raw.split(Regex("""\s*(?:,|;|\be\b)\s*""", RegexOption.IGNORE_CASE))
                            .map(String::trim)
                            .filter(String::isNotBlank)
                    }
                    EncantoMarcialPreparado(
                        nomeNormalizado = EncantosSolaresCatalog.normalize(encanto.nome),
                        requisitos = requisitos
                    )
                }
                EstiloMarcialPreparado(porNome = porNome, encantos = encantos)
                    .also { estilosPreparadosCache[estilo] = it }
            }
        }

    data class Resultado(
        val npc: NpcEncontro,
        val estiloSelecionado: String,
        val abriuNovoEstilo: Boolean,
        val compraDeRequisito: Boolean
    )

    fun expandirComResultado(
        npc: NpcEncontro,
        estilos: List<EstiloArteMarcialDefinition>
    ): Resultado? {
        if (npc.estiloArtesMarciais.isBlank()) return null
        fun localizar(nome: String): EstiloArteMarcialDefinition? = estilos.firstOrNull {
            it.nomePt.equals(nome, true) || it.nomeEn.equals(nome, true) || it.id.equals(nome, true)
        }
        val estiloPrincipal = localizar(npc.estiloArtesMarciais) ?: return null
        val estilosAtivos = listOf(estiloPrincipal) +
            npc.estilosArtesMarciaisAdicionais.mapNotNull(::localizar).filter { it.id != estiloPrincipal.id }

        val conhecidos = npc.charms.map { EncantosSolaresCatalog.normalize(it.nome) }.toSet()
        val briga = npc.abilities["Briga"] ?: 0

        val preparadosPorEstilo = HashMap<EstiloArteMarcialDefinition, EstiloMarcialPreparado>()
        fun preparado(estilo: EstiloArteMarcialDefinition): EstiloMarcialPreparado =
            preparadosPorEstilo.getOrPut(estilo) { estiloPreparado(estilo) }
        fun requisitos(
            estilo: EstiloArteMarcialDefinition,
            encanto: EncantoArteMarcialDefinition
        ): List<String> = preparado(estilo).encantos.getValue(encanto).requisitos

        val candidatosCache = HashMap<EstiloArteMarcialDefinition, List<EncantoArteMarcialDefinition>>()
        fun candidatosLegais(estilo: EstiloArteMarcialDefinition): List<EncantoArteMarcialDefinition> =
            candidatosCache.getOrPut(estilo) {
                val preparado = preparado(estilo)
                estilo.encantos.filter { encanto ->
                    val encantoPreparado = preparado.encantos.getValue(encanto)
                    encantoPreparado.nomeNormalizado !in conhecidos &&
                        encanto.minHabilidade <= briga &&
                        encanto.minEssencia <= npc.essencia &&
                        encantoPreparado.requisitos.all { req ->
                            EncounterMartialArtsSelectionService.requisitoMarcialSatisfeito(
                                req, preparado.porNome, conhecidos
                            )
                        }
                }
            }

        // Profundidade antes de amplitude: esgota opções legais do principal,
        // depois continua estilos adicionais já abertos.
        var estilo = estilosAtivos.firstOrNull { candidatosLegais(it).isNotEmpty() }

        // Um novo estilo só abre quando o principal já está substancialmente
        // desenvolvido e não possui próximo nó legal. Isso evita "colecionar"
        // entradas rasas em muitos estilos.
        var abriuNovoEstilo = false
        if (estilo == null) {
            val adquiridosPrincipal = estiloPrincipal.encantos.count {
                EncantosSolaresCatalog.normalize(it.nome) in conhecidos
            }
            val minimoParaNovoEstilo = minOf(5, estiloPrincipal.encantos.size)
            if (adquiridosPrincipal < minimoParaNovoEstilo) return null

            val idsAtivos = estilosAtivos.map { it.id }.toSet()
            estilo = estilos.asSequence()
                .filter { it.id !in idsAtivos }
                .filter { npc.tipoExaltado in it.tiposExaltadosPermitidos }
                // Equipamento é preferência, não barreira. A ordenação abaixo
                // favorece estilos que reutilizam melhor arma/armadura dos estilos
                // já ativos; a reconciliação posterior pode trocar ou remover
                // armadura quando isso produzir um conjunto legal.
                .filter { candidatosLegais(it).isNotEmpty() }
                .maxWithOrNull(
                    compareBy<EstiloArteMarcialDefinition> { candidatoEstilo ->
                        estilosAtivos.sumOf {
                            EncounterCombatSynergy.martialEquipmentAffinity(it, candidatoEstilo)
                        }
                    }.thenBy { candidatosLegais(it).size }
                        .thenBy { estiloNovo ->
                            candidatosLegais(estiloNovo).maxOfOrNull { candidato ->
                                EncounterCombatSynergy.score(
                                    EncounterCombatSynergy.text(candidato.toEncantoSolarDefinition()),
                                    emptyList()
                                )
                            } ?: Int.MIN_VALUE
                        }
                        .thenByDescending { it.nomePt }
                )
                ?: return null
            abriuNovoEstilo = true
        }

        val estiloSelecionado = estilo ?: return null
        val candidatos = candidatosLegais(estiloSelecionado)
        if (candidatos.isEmpty()) return null

        // Reconciliar a configuração defensiva dos estilos ativos. Armadura é
        // preferência de sinergia, não barreira de aquisição: quando a armadura
        // corrente não serve ao conjunto, procurar uma classe comum; se nenhuma
        // existir, lutar sem armadura é a configuração comum válida.
        val estilosAposCompra = if (abriuNovoEstilo) estilosAtivos + estiloSelecionado else estilosAtivos
        val armaduraAtual = npc.armadura
        val armaduraReconciliada = if (armaduraAtual != null &&
            estilosAposCompra.all {
                EncounterMartialArtsSelectionService.armaduraNpcCompativel(
                    it, armaduraAtual.nome, armaduraAtual.peso
                )
            }
        ) armaduraAtual else if (armaduraAtual != null) {
            val pesoComum = EncounterMartialArmorAffinity.bestCommonArmor(estilosAposCompra)
            if (pesoComum == null) {
                com.example.model.ArmaduraEncontro(
                    nome = "Sem armadura", peso = "Leve", tipo = "Mundana",
                    absorcao = 0, dureza = 0, penalidadeMobilidade = 0,
                    motesComitados = 0, marcadores = emptyList(), custoMeritoArtefato = 0
                )
            } else {
                // Reusar o serviço de equipamento para obter uma armadura legal
                // para o estilo selecionado; se o conjunto exigir classe mais
                // restrita, a ausência de armadura continua sendo o fallback seguro.
                EncounterEquipmentService.ajustarParaEstiloMarcial(
                    estiloSelecionado, npc.arma, armaduraAtual, Random(0)
                ).second.takeIf { candidata ->
                    estilosAposCompra.all {
                        EncounterMartialArtsSelectionService.armaduraNpcCompativel(
                            it, candidata.nome, candidata.peso
                        )
                    }
                } ?: com.example.model.ArmaduraEncontro(
                    nome = "Sem armadura", peso = "Leve", tipo = "Mundana",
                    absorcao = 0, dureza = 0, penalidadeMobilidade = 0,
                    motesComitados = 0, marcadores = emptyList(), custoMeritoArtefato = 0
                )
            }
        } else null

        val conhecidosDoEstilo = estiloSelecionado.encantos.filter {
            EncantosSolaresCatalog.normalize(it.nome) in conhecidos
        }
        val escolhido = candidatos.maxWithOrNull(
            compareBy<EncantoArteMarcialDefinition> {
                EncounterCombatSynergy.score(
                    EncounterCombatSynergy.text(it.toEncantoSolarDefinition()),
                    conhecidosDoEstilo.map { c -> EncounterCombatSynergy.text(c.toEncantoSolarDefinition()) }
                )
            }.thenBy { requisitos(estiloSelecionado, it).size }
                .thenBy { it.minEssencia }
                .thenBy { it.minHabilidade }
                .thenByDescending { it.nome }
        ) ?: return null

        val escolhidoNormalizado = EncantosSolaresCatalog.normalize(escolhido.nome)
        val compraDeRequisito = estiloSelecionado.encantos.any { alvo ->
            EncantosSolaresCatalog.normalize(alvo.nome) !in conhecidos &&
                requisitos(estiloSelecionado, alvo).any { req ->
                    EncantosSolaresCatalog.normalize(req) == escolhidoNormalizado
                }
        }

        val novo = EncantoEncontro(escolhido.nome, estiloSelecionado.nomePt, escolhido.custo)
        val batch = HistoricoXpBatch(
            xpGasto = CUSTO_ENCANTO_MARCIAL_XP,
            nomesEncantosAdicionados = listOf(escolhido.nome)
        )
        val atualizado = npc.copy(
            charms = npc.charms + novo,
            estilosArtesMarciaisAdicionais = if (abriuNovoEstilo)
                (npc.estilosArtesMarciaisAdicionais + estiloSelecionado.nomePt).distinct()
            else npc.estilosArtesMarciaisAdicionais,
            // Cada chamada concede 5 XP e esta compra consome os mesmos 5:
            // o saldo anterior permanece. Não transformar XP gasto em saldo livre.
            xpAtual = npc.xpAtual,
            xpGastoTotal = npc.xpGastoTotal + CUSTO_ENCANTO_MARCIAL_XP,
            historicoXpBatches = npc.historicoXpBatches + batch,
            armadura = armaduraReconciliada
        )
        return Resultado(
            npc = atualizado,
            estiloSelecionado = estiloSelecionado.nomePt,
            abriuNovoEstilo = abriuNovoEstilo,
            compraDeRequisito = compraDeRequisito
        )
    }

    fun expandirSePossivel(
        npc: NpcEncontro,
        estilos: List<EstiloArteMarcialDefinition>
    ): NpcEncontro? = expandirComResultado(npc, estilos)?.npc
}
