package com.example.data

import com.example.model.FeiticoEncontro
import com.example.model.NpcEncontro
import com.example.model.TipoExaltadoEncontro

/** Regras puras da gaveta de Feitiços dos NPCs da Aba 11. */
object EncounterNpcSpellManagement {
    // Os nomes dos Encantos de desbloqueio são constantes: normalizá-los
    // apenas uma vez evita trabalho repetido ao exibir/atualizar cada NPC.
    private val circleCharmNames = linkedMapOf(
        "Terrestre" to "Feitiçaria do Círculo Terrestre",
        "Celestial" to "Feitiçaria do Círculo Celestial",
        "Solar" to "Feitiçaria do Círculo Solar"
    ).mapValues { (_, nome) -> EncantosSolaresCatalog.normalize(nome) }

    fun circulosPermitidos(tipo: TipoExaltadoEncontro): Set<String> = when (tipo) {
        TipoExaltadoEncontro.SANGUE_DE_DRAGAO -> setOf("Terrestre")
        TipoExaltadoEncontro.LUNAR -> setOf("Terrestre", "Celestial")
        TipoExaltadoEncontro.SOLAR -> setOf("Terrestre", "Celestial", "Solar")
    }

    fun circulosDesbloqueados(npc: NpcEncontro): Set<String> {
        val permitidosPelaExaltacao = circulosPermitidos(npc.tipoExaltado)
        if (npc.charms.isEmpty()) return emptySet()
        // Normalizar cada Encanto apenas uma vez, mesmo quando o NPC
        // desbloqueia mais de um circulo de Feiticaria.
        val nomesPossuidos = npc.charms.asSequence()
            .map { EncantosSolaresCatalog.normalize(it.nome) }
            .toHashSet()
        return buildSet {
            circleCharmNames.forEach { (circulo, encanto) ->
                if (
                    circulo in permitidosPelaExaltacao &&
                    encanto in nomesPossuidos
                ) add(circulo)
            }
        }
    }

    fun disponiveis(npc: NpcEncontro, catalogo: FeiticariaCatalog): List<FeiticoDefinition> {
        if (!podeGerenciarFeiticoInicial(npc)) return emptyList()
        return circulosDesbloqueados(npc).flatMap(catalogo::paraCirculo)
    }

    fun podeGerenciarFeiticoInicial(npc: NpcEncontro): Boolean = npc.podeGerenciarFeiticoInicial()

    /**
     * Invariante de criação: qualquer NPC que já possua o Círculo Terrestre
     * nasce com a vaga gratuita materializada. A escolha continua editável
     * até o primeiro lote de XP.
     */
    fun garantirInicialNaCriacao(
        npc: NpcEncontro,
        catalogo: List<FeiticoDefinition>,
        random: kotlin.random.Random
    ): NpcEncontro {
        if (npc.primeiroXpRecebido || "Terrestre" !in circulosDesbloqueados(npc)) return npc

        val inicialExistente = npc.feiticoInicialNome?.let { nome ->
            npc.feiticos.firstOrNull { it.circulo == "Terrestre" && EncantosSolaresCatalog.sameName(it.nome, nome) }
        }
        if (inicialExistente != null) return npc

        // Indexa os nomes possuidos uma unica vez e coleta candidatos
        // elegiveis na mesma passagem pelo catalogo. A ordem de ambas as
        // listas permanece identica, preservando o sorteio por seed.
        val nomesPossuidos = npc.feiticos.asSequence()
            .filter { it.circulo == "Terrestre" }
            .map { EncantosSolaresCatalog.normalize(it.nome) }
            .toHashSet()
        val terrestres = ArrayList<FeiticoDefinition>()
        val candidatosNovos = ArrayList<FeiticoDefinition>()
        val nomesCandidatos = HashSet<String>()
        for (def in catalogo) {
            if (def.circulo != "Terrestre") continue
            terrestres.add(def)
            val nomeNormalizado = EncantosSolaresCatalog.normalize(def.nome)
            // Um mesmo Feitiço pode aparecer repetido no catálogo: não
            // deve receber chances extras no sorteio da vaga gratuita.
            if (nomeNormalizado !in nomesPossuidos && nomesCandidatos.add(nomeNormalizado)) {
                candidatosNovos.add(def)
            }
        }
        if (terrestres.isEmpty()) return npc
        val escolhido = candidatosNovos.randomOrNull(random)
            ?: run {
                // Quando todos os candidatos ja foram aprendidos, usar um
                // indice por nome evita uma segunda busca quadratica. A
                // primeira definicao do catalogo continua tendo precedencia.
                val definicoesPorNome = LinkedHashMap<String, FeiticoDefinition>()
                terrestres.forEach { def ->
                    definicoesPorNome.putIfAbsent(EncantosSolaresCatalog.normalize(def.nome), def)
                }
                npc.feiticos.firstNotNullOfOrNull { existente ->
                    definicoesPorNome[EncantosSolaresCatalog.normalize(existente.nome)]
                }
            }
            ?: terrestres.random(random)

        val jaExiste = EncantosSolaresCatalog.normalize(escolhido.nome) in nomesPossuidos
        val feiticos = if (jaExiste) npc.feiticos else npc.feiticos + FeiticoEncontro(escolhido.nome, escolhido.circulo, escolhido.custo)
        return npc.copy(feiticos = feiticos, feiticoInicialNome = escolhido.nome)
    }

    /**
     * Gerencia somente a identidade/vaga do Feitiço inicial. Outros Feitiços
     * do NPC são preservados integralmente. Depois do primeiro +5 XP é no-op.
     */
    fun atualizarInicial(npc: NpcEncontro, def: FeiticoDefinition, adicionar: Boolean): NpcEncontro {
        if (!podeGerenciarFeiticoInicial(npc)) return npc
        if (def.circulo !in circulosDesbloqueados(npc)) return npc

        val ehInicialAtual = npc.feiticoInicialNome?.let {
            EncantosSolaresCatalog.sameName(it, def.nome)
        } == true
        if (adicionar && ehInicialAtual) return npc
        if (!adicionar && !ehInicialAtual) return npc

        if (!adicionar) {
            // A vaga inicial/gratuita deixa de conceder este Feitiço. Os demais
            // Feitiços permanecem intocados.
            val restantes = npc.feiticos.filterNot {
                EncantosSolaresCatalog.sameName(it.nome, npc.feiticoInicialNome.orEmpty())
            }
            return npc.copy(feiticos = restantes, feiticoInicialNome = null)
        }

        val antigoInicial = npc.feiticoInicialNome
        val preservados = if (antigoInicial.isNullOrBlank()) {
            npc.feiticos
        } else {
            npc.feiticos.filterNot { EncantosSolaresCatalog.sameName(it.nome, antigoInicial) }
        }
        val alvoJaExiste = preservados.any { EncantosSolaresCatalog.sameName(it.nome, def.nome) }
        val novos = if (alvoJaExiste) preservados else preservados + FeiticoEncontro(def.nome, def.circulo, def.custo)
        return npc.copy(feiticos = novos, feiticoInicialNome = def.nome)
    }


}
