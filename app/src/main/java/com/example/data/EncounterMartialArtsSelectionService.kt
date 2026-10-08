package com.example.data

import com.example.model.EncantoEncontro
import com.example.model.TipoExaltadoEncontro
import kotlin.random.Random

/** Construção marcial da Aba 11. Mantém estilo/árvore separados de Encantos de Briga. */
internal object EncounterMartialArtsSelectionService {
    const val INITIAL_MARTIAL_CHARMS = 5

    data class Selection(
        val estilo: EstiloArteMarcialDefinition,
        val encantos: List<EncantoArteMarcialDefinition>
    )

    fun selecionar(
        estilos: List<EstiloArteMarcialDefinition>,
        tipo: TipoExaltadoEncontro,
        briga: Int,
        essencia: Int,
        random: Random,
        armaduraPeso: String? = null,
        quantidade: Int = INITIAL_MARTIAL_CHARMS
    ): Selection? {
        if (quantidade <= 0) return null
        // A armadura corrente não torna um estilo ilegal: a etapa de equipamento
        // pode trocar a classe ou remover a armadura para acomodar a rota marcial.
        val disponiveis = estilos.filter { estilo ->
            tipo in estilo.tiposExaltadosPermitidos
        }
        if (disponiveis.isEmpty()) return null

        // Estilo principal: privilegia aquele que consegue sustentar a árvore
        // mais profunda com os requisitos atuais. Empates continuam variados.
        val preparados = disponiveis.shuffled(random).map { estilo ->
            estilo to construirArvore(estilo, briga, essencia, quantidade)
        }.filter { it.second.isNotEmpty() }
        val escolhido = preparados.maxByOrNull { it.second.size } ?: return null
        return Selection(escolhido.first, escolhido.second)
    }


    fun armaduraCompativel(estilo: EstiloArteMarcialDefinition, peso: String?): Boolean {
        if (peso == null) return true // sem armadura é uma configuração válida para qualquer estilo
        return when (estilo.armaduraCategoria.lowercase()) {
            "todas" -> true
            "leve" -> peso.equals("Leve", true)
            "leve_media" -> peso.equals("Leve", true) || peso.equals("Média", true) || peso.equals("Media", true)
            "media" -> peso.equals("Média", true) || peso.equals("Media", true)
            "incompativel" -> false
            else -> true
        }
    }

    internal fun armaNpcCompativel(
        estilo: EstiloArteMarcialDefinition,
        armaNome: String?
    ): Boolean = EncounterMartialAttackCompatibility.supportsWeapon(estilo, armaNome)

    internal fun armaduraNpcCompativel(
        estilo: EstiloArteMarcialDefinition,
        armaduraNome: String?,
        armaduraPeso: String?
    ): Boolean {
        if (armaduraNome.equals("Sem armadura", ignoreCase = true)) {
            // Ausência de armadura é compatível inclusive com estilos que
            // proíbem armadura; não confundir o placeholder estrutural "Leve"
            // com uma armadura leve realmente equipada.
            return true
        }
        return armaduraCompativel(estilo, armaduraPeso)
    }

    internal fun requisitoMarcialSatisfeito(
        requisito: String,
        catalogoPorNome: Map<String, EncantoArteMarcialDefinition>,
        conhecidos: Set<String>
    ): Boolean {
        val normalizado = EncantosSolaresCatalog.normalize(requisito)
        val encanto = catalogoPorNome[normalizado]
        if (encanto != null) return EncantosSolaresCatalog.normalize(encanto.nome) in conhecidos

        // Requisitos textuais que não nomeiam um Encanto do próprio estilo
        // (Mérito, arma, atributo etc.) são tratados pelas camadas de rota/
        // equipamento. Um nome com aparência de Encanto não pode ser
        // silenciosamente considerado satisfeito só porque faltou no catálogo.
        val pareceNomeDeEncanto = requisito.trim().split(Regex("""\s+""")).size >= 2 &&
            requisito.none { it.isDigit() } &&
            !requisito.contains("Mérito", true) &&
            !requisito.contains("Merito", true) &&
            !requisito.contains("Essência", true) &&
            !requisito.contains("Essencia", true) &&
            !requisito.contains("Briga", true) &&
            !requisito.contains("Artes Marciais", true)
        return !pareceNomeDeEncanto
    }

    private fun construirArvore(
        estilo: EstiloArteMarcialDefinition,
        briga: Int,
        essencia: Int,
        quantidade: Int
    ): List<EncantoArteMarcialDefinition> {
        data class Preparado(
            val encanto: EncantoArteMarcialDefinition,
            val nomeNormalizado: String,
            val requisitos: List<String>,
            val tags: Set<EncounterCombatSynergy.Tag>
        )

        val catalogo = estilo.encantos
        val porNome = catalogo.associateBy { EncantosSolaresCatalog.normalize(it.nome) }
        val preparados = catalogo.map { encanto ->
            val raw = encanto.preRequisitos.trim()
            val requisitos = if (
                raw.isBlank() || raw.equals("Nenhum", true) || raw == "—" || raw == "-"
            ) {
                emptyList()
            } else {
                raw.split(Regex("""\s*(?:,|;|\be\b)\s*""", RegexOption.IGNORE_CASE))
                    .map { it.trim() }
                    .filter { it.isNotBlank() && !it.equals("Nenhum", true) }
            }
            Preparado(
                encanto = encanto,
                nomeNormalizado = EncantosSolaresCatalog.normalize(encanto.nome),
                requisitos = requisitos,
                tags = EncounterCombatSynergy.tags(
                    EncounterCombatSynergy.text(encanto.toEncantoSolarDefinition())
                )
            )
        }
        val selecionados = mutableListOf<EncantoArteMarcialDefinition>()
        val tagsSelecionadas = mutableListOf<Set<EncounterCombatSynergy.Tag>>()
        val nomes = mutableSetOf<String>()

        fun legal(preparado: Preparado): Boolean =
            preparado.encanto.minHabilidade <= briga &&
                preparado.encanto.minEssencia <= essencia &&
                preparado.requisitos.all { req ->
                    requisitoMarcialSatisfeito(req, porNome, nomes)
                }

        while (selecionados.size < quantidade) {
            val candidatos = preparados.filter {
                it.nomeNormalizado !in nomes && legal(it)
            }
            if (candidatos.isEmpty()) break
            val escolhido = candidatos.maxWithOrNull(
                compareBy<Preparado> {
                    EncounterCombatSynergy.scoreTags(it.tags, tagsSelecionadas)
                }.thenBy { it.requisitos.size }
                    .thenBy { it.encanto.minEssencia }
                    .thenBy { it.encanto.minHabilidade }
                    .thenByDescending { it.encanto.nome }
            ) ?: break
            selecionados += escolhido.encanto
            tagsSelecionadas += escolhido.tags
            nomes += escolhido.nomeNormalizado
        }
        return selecionados
    }

    fun integrarMantendoQuantidade(
        encantosNormais: List<EncantoEncontro>,
        selecao: Selection?,
        quantidadeTotal: Int = EncounterGenerationRules.ENCANTOS_INICIAIS,
        nomesProtegidos: Set<String> = emptySet()
    ): List<EncantoEncontro> {
        if (selecao == null) return encantosNormais
        val marciais = selecao.encantos.map {
            EncantoEncontro(it.nome, selecao.estilo.nomePt, it.custo)
        }
        val vagasNormais = (quantidadeTotal - marciais.size).coerceAtLeast(0)
        // A seleção normal é ordenada por construção; retirar terminais do fim
        // preserva melhor os pré-requisitos já adquiridos. Alguns Encantos podem
        // sustentar a legalidade estrutural de outros (p.ex. Alma da Quimera),
        // então seus índices são preservados antes de preencher as vagas restantes.
        if (nomesProtegidos.isEmpty()) return encantosNormais.take(vagasNormais) + marciais
        val indicesProtegidos = encantosNormais.indices
            .filter { encantosNormais[it].nome in nomesProtegidos }
            .take(vagasNormais)
            .toMutableSet()
        encantosNormais.indices.asSequence()
            .filterNot { it in indicesProtegidos }
            .take((vagasNormais - indicesProtegidos.size).coerceAtLeast(0))
            .forEach(indicesProtegidos::add)
        val preservados = indicesProtegidos.sorted().map(encantosNormais::get)
        return preservados + marciais
    }
}
