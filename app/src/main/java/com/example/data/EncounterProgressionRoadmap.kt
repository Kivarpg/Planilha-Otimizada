package com.example.data

import com.example.model.EncounterProgressionRoadmap
import com.example.model.EncounterProgressionStep
import com.example.model.NpcEncontro
import java.security.MessageDigest

/**
 * Planejamento individual da progressão por XP.
 *
 * O roadmap continua sendo uma otimização derivada: o estado do NPC permanece
 * como fonte de verdade. Cada passo carrega uma pré-condição determinística,
 * permitindo descartar silenciosamente um plano antigo quando o NPC ou o
 * contrato do algoritmo mudou.
 */
object EncounterProgressionRoadmapService {
    const val SCHEMA_VERSION = 1
    // A escolha ofensiva Lunar agora integra os passos e o fingerprint do NPC.
    // Planos persistidos antes desse contrato nao podem ser reutilizados.
    const val ALGORITHM_VERSION = 7
    const val CATALOG_VERSION = 1
    private const val MAX_PASSOS_DE_SEGURANCA = 128
    /** Quantidade pequena de passos antecipados para evitar planejar um marco inteiro de uma vez. */
    const val PASSOS_PREFETCH = 3

    private const val HEX_DIGITS = "0123456789abcdef"

    /**
     * SHA-256 em hexadecimal sem passar por String.format/Formatter para cada byte.
     * Fingerprints são calculados repetidamente no fluxo de +XP/prefetch; a
     * conversão direta evita 32 formatações genéricas e mantém exatamente o
     * mesmo texto hexadecimal minúsculo usado pelo cache existente.
     */
    internal fun sha256Hex(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
        val chars = CharArray(digest.size * 2)
        var out = 0
        for (byte in digest) {
            val unsigned = byte.toInt() and 0xff
            chars[out++] = HEX_DIGITS[unsigned ushr 4]
            chars[out++] = HEX_DIGITS[unsigned and 0x0f]
        }
        return String(chars)
    }

    private fun proximoMarcoXp(npc: NpcEncontro): Int? =
        EncounterExperienceProgressionRules.nextMilestone(npc.tipoExaltado, npc.xpGastoTotal)

    /**
     * Fingerprint dos campos que a expansão de XP efetivamente consulta para
     * decidir a próxima compra. Campos apenas derivados/exibidos (motes,
     * vitalidade, equipamento etc.) não invalidam o plano: ao aplicar um passo,
     * esses derivados são recalculados a partir do estado atual.
     *
     * A seleção é específica por tipo porque Solar/Sangue de Dragão e Lunar
     * leem estados diferentes do NPC durante a expansão.
     */
    fun fingerprint(npc: NpcEncontro): String {
        fun esc(value: String): String = value.replace("\\", "\\\\").replace("|", "\\|")
        fun mapStable(map: Map<String, Int>): String =
            map.toSortedMap().entries.joinToString(",") { "${esc(it.key)}=${it.value}" }
        fun listStable(list: List<String>): String = list.map(::esc).sorted().joinToString(",")
        fun charmsStable(): String = npc.charms
            .map { listOf(it.nome, it.habilidadeVinculada).joinToString("~", transform = ::esc) }
            .sorted()
            .joinToString(";")
        fun specialtiesStable(): String = npc.especialidades
            .map { it.habilidade }
            .sorted()
            .joinToString(",", transform = ::esc)

        val canonical = buildString {
            append("tipo=").append(npc.tipoExaltado.name).append('|')
            append("xpAtual=").append(npc.xpAtual).append('|')
            append("xpGasto=").append(npc.xpGastoTotal).append('|')
            append("corpo=").append(npc.corpoDeTouroCount).append('|')
            append("charms=").append(charmsStable()).append('|')
            append("focoExplicito=").append(esc(npc.focoProgressaoExplicito.orEmpty())).append('|')
            append("ataqueLunar=").append(esc(npc.lunarAtaqueEscolhido.orEmpty())).append('|')

            when (npc.tipoExaltado) {
                com.example.model.TipoExaltadoEncontro.SOLAR,
                com.example.model.TipoExaltadoEncontro.SANGUE_DE_DRAGAO -> {
                    // A política de prioridade/custo da expansão lê estes
                    // campos através das fachadas Solar/Sangue de Dragão.
                    append("arquetipo=").append(npc.arquetipo.name).append('|')
                    append("habilidadePrincipal=").append(esc(npc.habilidadePrincipal)).append('|')
                    append("habilidadeSuporte=").append(esc(npc.habilidadeSuporte)).append('|')
                    append("fav=").append(listStable(npc.habilidadesFavorecidas)).append('|')
                    append("supernal=").append(esc(npc.habilidadeSupernal)).append('|')
                    append("casta=").append(esc(npc.casta)).append('|')
                    append("abilities=").append(mapStable(npc.abilities)).append('|')
                    append("spec=").append(specialtiesStable())
                }
                com.example.model.TipoExaltadoEncontro.LUNAR -> {
                    // Lunar usa Atributos, não Habilidades, para elegibilidade.
                    // A prioridade também depende do arquétipo + favorecidas +
                    // Atributos de Casta Lunar.
                    append("arquetipo=").append(npc.arquetipo.name).append('|')
                    append("fav=").append(listStable(npc.habilidadesFavorecidas)).append('|')
                    append("lunarCasta=").append(listStable(npc.lunarAtributosCasta)).append('|')
                    // A política Lunar deriva o contexto de rota das Formas
                    // Espirituais e da taxonomia persistida de traits. Alterar
                    // qualquer um desses campos pode mudar qual Encanto é
                    // priorizado, portanto um roadmap anterior deixa de ser válido.
                    append("formaEspiritual=").append(esc(npc.formaEspiritual)).append('|')
                    append("formaEspiritualSecundaria=").append(esc(npc.formaEspiritualSecundaria)).append('|')
                    append("lunarPrimaryTraits=").append(listStable(npc.lunarPrimaryArchetypeTraits)).append('|')
                    append("lunarTraits=").append(listStable(npc.lunarArchetypeTraits)).append('|')
                    append("attributes=").append(mapStable(npc.attributes))
                }
            }
            append('|').append("algo=").append(ALGORITHM_VERSION)
            append('|').append("catalogo=").append(CATALOG_VERSION)
        }
        return sha256Hex(canonical)
    }

    /**
     * Fingerprint do conteúdo efetivamente usado pelo roadmap. O hash é
     * calculado uma vez por catálogo carregado e armazenado no roadmap;
     * assim, uma alteração real no catálogo invalida o plano sem precisar
     * incrementar manualmente CATALOG_VERSION a cada mudança de dados.
     */
    fun catalogFingerprint(
        solarCatalogo: List<EncantoSolarDefinition>,
        dragonCatalogo: List<EncantoSangueDeDragaoDefinition>,
        lunarCatalogo: List<EncantoLunarDefinition>,
        martialCatalogo: List<EstiloArteMarcialDefinition> = emptyList()
    ): String {
        fun stable(value: Any?): String = value.toString().replace("\\", "\\\\").replace("|", "\\|")
        val solar = solarCatalogo.map {
            listOf(it.id, it.habilidade, it.nome, it.nomeIngles, it.custo, it.minsTexto, it.minHabilidade, it.minEssencia, it.tipo, it.palavrasChave, it.duracao, it.preRequisitos, it.descricao, it.quadros.toString()).joinToString("~", transform = ::stable)
        }.sorted()
        val dragon = dragonCatalogo.map {
            listOf(it.id, it.habilidade, it.nome, it.nomeIngles, it.custo, it.minsTexto, it.minHabilidade, it.minEssencia, it.tipo, it.palavrasChave, it.duracao, it.preRequisitos, it.descricao, it.quadros.toString()).joinToString("~", transform = ::stable)
        }.sorted()
        val lunar = lunarCatalogo.map {
            listOf(it.id, it.atributo, it.subdivisao ?: "", it.nome, it.nomeIngles, it.custo, it.minsTexto, it.minAtributo, it.minEssencia, it.tipo, it.palavrasChave, it.duracao, it.preRequisitos, it.descricao, it.quadros.toString()).joinToString("~", transform = ::stable)
        }.sorted()
        val martial = martialCatalogo.flatMap { estilo ->
            estilo.encantos.map { encanto ->
                listOf(
                    estilo.id, estilo.nomePt, estilo.nomeEn, estilo.armaDoEstiloModo,
                    estilo.armasEspecificas.sorted().joinToString(","), estilo.armaduraCategoria,
                    estilo.tiposExaltadosPermitidos.map { it.name }.sorted().joinToString(","),
                    encanto.id, encanto.estiloId, encanto.habilidade, encanto.nome, encanto.nomeIngles,
                    encanto.custo, encanto.minsTexto, encanto.minHabilidade, encanto.minEssencia,
                    encanto.tipo, encanto.palavrasChave, encanto.duracao, encanto.preRequisitos,
                    encanto.descricao, encanto.quadros.toString()
                ).joinToString("~", transform = ::stable)
            }
        }.sorted()
        val canonical = "solar=${solar.joinToString(";")}|dragon=${dragon.joinToString(";")}|lunar=${lunar.joinToString(";")}|martial=${martial.joinToString(";")}"
        return sha256Hex(canonical)
    }

    fun construir(
        npc: NpcEncontro,
        solarCatalogo: List<EncantoSolarDefinition>,
        dragonCatalogo: List<EncantoSangueDeDragaoDefinition>,
        lunarCatalogo: List<EncantoLunarDefinition>,
        maxPassos: Int = PASSOS_PREFETCH,
        catalogFingerprintPrecalculado: String? = null,
        verificarCancelamento: () -> Unit = {}
    ): EncounterProgressionRoadmap {
        verificarCancelamento()
        val limitePassos = maxPassos.coerceIn(1, PASSOS_PREFETCH)
        // Em produção os catálogos permanecem os mesmos durante a vida do
        // EncounterNpcActions. O fingerprint já é calculado uma vez nesse
        // escopo; reutilizá-lo evita serializar e hashear todos os Encantos
        // novamente a cada janela curta de prefetch do +XP. O parâmetro é
        // opcional para manter chamadas independentes/testes autocontidos.
        val fingerprintCatalogos = catalogFingerprintPrecalculado
            ?: catalogFingerprint(solarCatalogo, dragonCatalogo, lunarCatalogo)
        verificarCancelamento()
        val marcoXp = proximoMarcoXp(npc) ?: return EncounterProgressionRoadmap(
            schemaVersion = SCHEMA_VERSION,
            algorithmVersion = ALGORITHM_VERSION,
            catalogVersion = CATALOG_VERSION,
            catalogFingerprint = fingerprintCatalogos
        )

        var atual = npc
        val passos = mutableListOf<EncounterProgressionStep>()
        val expansor = when (npc.tipoExaltado) {
            com.example.model.TipoExaltadoEncontro.SOLAR ->
                SolarEncounterGenerator.criarExpansorXpPreparado(solarCatalogo)
            com.example.model.TipoExaltadoEncontro.SANGUE_DE_DRAGAO ->
                DragonBloodedEncounterGenerator.criarExpansorXpPreparado(dragonCatalogo, npc)
            com.example.model.TipoExaltadoEncontro.LUNAR ->
                EncounterExperienceLunar.criarExpansorXpPreparado(
                    lunarCatalogo,
                    EncounterRulePolicy.lunarAttributePriorityFor(npc)
                )
        }
        verificarCancelamento()
        var iteracoes = 0
        // O cache não tenta materializar todo o caminho até o próximo marco.
        // Uma janela curta reduz o custo inicial e mantém o fallback como
        // mecanismo natural para renovar o cache quando os passos terminarem.
        while (
            atual.xpGastoTotal < marcoXp &&
                passos.size < limitePassos &&
                iteracoes++ < MAX_PASSOS_DE_SEGURANCA
        ) {
            verificarCancelamento()
            val antes = atual
            val expansao = expansor.expand(antes)
            verificarCancelamento()
            atual = expansao.npcResultante
            val lote = expansao.batchAplicado
            if (
                    atual.xpAtual == antes.xpAtual &&
                        atual.xpGastoTotal == antes.xpGastoTotal &&
                        atual.historicoXpBatches.size == antes.historicoXpBatches.size
                    ) break

            // Cada chamada de +XP é um passo, mesmo quando nenhum XP é gasto.
            // O lote zero é importante porque a chamada ainda acrescenta os 5 XP
            // disponíveis e pode ser justamente o que torna uma compra possível
            // na chamada seguinte. Ignorá-lo faria o roadmap divergir do algoritmo
            // normal e tornaria impossível planejar um NPC recém-criado com saldo 0.
            val charmsAdicionados = if (atual.charms.size > antes.charms.size) {
                atual.charms.subList(antes.charms.size, atual.charms.size)
            } else {
                emptyList()
            }
            passos += EncounterProgressionStep(
                preconditionFingerprint = fingerprint(antes),
                catalogFingerprint = fingerprintCatalogos,
                xpGasto = lote.xpGasto,
                encantos = charmsAdicionados,
                habilidadeMelhorada = lote.habilidadeMelhorada,
                pontosGanhosNaHabilidade = lote.pontosGanhosNaHabilidade,
                especializacaoAdicionada = lote.especializacaoAdicionada,
                pontosForcaDeVontadeComprados = lote.pontosForcaDeVontadeComprados,
                lunarAtaqueEscolhido = atual.lunarAtaqueEscolhido
            )
        }
        // Evita devolver um planejamento cancelado durante a montagem do último passo.
        verificarCancelamento()
        return EncounterProgressionRoadmap(
            passos = passos,
            proximoPasso = 0,
            schemaVersion = SCHEMA_VERSION,
            algorithmVersion = ALGORITHM_VERSION,
            catalogVersion = CATALOG_VERSION,
            catalogFingerprint = fingerprintCatalogos
        )
    }

    fun proximo(
        npc: NpcEncontro,
        roadmap: EncounterProgressionRoadmap?,
        expectedCatalogFingerprint: String = ""
    ): EncounterProgressionStep? {
        roadmap ?: return null
        if (roadmap.schemaVersion != SCHEMA_VERSION ||
            roadmap.algorithmVersion != ALGORITHM_VERSION ||
            roadmap.catalogVersion != CATALOG_VERSION ||
            (expectedCatalogFingerprint.isNotBlank() && roadmap.catalogFingerprint != expectedCatalogFingerprint)
        ) return null
        val passo = roadmap.passos.getOrNull(roadmap.proximoPasso) ?: return null
        return passo.takeIf { it.preconditionFingerprint.isBlank() || it.preconditionFingerprint == fingerprint(npc) }
    }

    fun avancar(roadmap: EncounterProgressionRoadmap): EncounterProgressionRoadmap =
        roadmap.copy(
            proximoPasso = (roadmap.proximoPasso + 1)
                .coerceAtMost(roadmap.passos.size)
        )
}
