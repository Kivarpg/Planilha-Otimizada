package com.example.feature.charmtree

import com.example.model.Encanto

/**
 * Converte qualquer catálogo carregado no modelo da árvore.
 *
 * A árvore não conhece o formato original do JSON. A única fonte de verdade
 * para as ligações é `Encanto.preRequisitos`, que vem diretamente do campo
 * `pre_requisitos` do catálogo. As referências são resolvidas contra o
 * catálogo inteiro, por nome ou ID, sem usar separadores fixos. Isso permite
 * que nomes contenham vírgulas, ponto e vírgula ou outros sinais sem quebrar
 * a estrutura da árvore.
 */
fun List<Encanto>.paraArvoreDePreRequisitos(
    canonicalPrerequisites: ((String) -> Set<String>)? = null
): List<CharmTreeEntry> {
    val entries = map { encanto ->
        val id = encanto.id.trim().ifBlank { encanto.nome.trim() }
        val abilityLabel = if (encanto.mins.startsWith("Arte Marcial", ignoreCase = true)) {
            "Arte Marcial"
        } else {
            encanto.habilidadeVinculada
        }
        CharmTreeEntry(
            id = id,
            name = encanto.nome,
            description = encanto.descricao,
            ability = abilityLabel,
            essenceMinimum = encanto.minEssencia,
            abilityMinimum = encanto.minHabilidade
        )
    }

    val byId = entries.associateBy { it.id }
    val prerequisiteTextById = this.associate { encanto ->
        encanto.id.trim().ifBlank { encanto.nome.trim() } to encanto.preRequisitos
    }
    // O conjunto de candidatos é imutável durante esta construção. Prepará-lo
    // uma única vez evita reconstruir groupBy/distinct/sort para cada Encanto.
    val resolver = CharmPrerequisiteReferenceParser.resolver(entries)
    val resolved = entries.map { entry ->
        val resolution = resolver.resolve(prerequisiteTextById[entry.id].orEmpty())
        val canonical = canonicalPrerequisites?.invoke(entry.id).orEmpty()
        entry.copy(
            // O catálogo canônico tem precedência quando resolve dependências
            // reais; o parser visual continua como fallback para MA/legado e
            // texto ainda não migrado. A união impede perda de arestas.
            prerequisiteIds = (canonical + resolution.prerequisiteIds).distinct(),
            unresolvedPrerequisiteText = resolution.unresolvedText
        )
    }

    // `byId` é deliberadamente materializado aqui para validar a unicidade do
    // identificador antes que o catálogo entre no construtor do grafo. A
    // construção final usa a ordem original do JSON.
    if (byId.size != entries.size) {
        throw IllegalStateException("Catálogo de Encantos contém IDs duplicados; a árvore não pode determinar os pré-requisitos com segurança.")
    }

    return resolved
}
