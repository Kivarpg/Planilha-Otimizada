package com.example.iniciativas

/**
 * Regras puras e allocation-conscious compartilhadas pelo controller e pela UI.
 *
 * Não cria coleções intermediárias nas operações mais frequentes da Aba 12.
 */
internal object IniciativasRules {
    // PERFORMANCE: uma única instância de Comparator por ordenação, em vez
    // da cadeia compareBy/thenBy que criava múltiplos objetos auxiliares.

    fun elegiveis(participantes: List<ParticipanteIniciativa>): List<ParticipanteIniciativa> {
        val max = maiorIniciativaElegivel(participantes)
        if (max == Int.MIN_VALUE) return emptyList()
        return participantes.filter { !it.jaAgiramNesteTurno && it.iniciativa == max }
    }


    /**
     * Calcula a lista e os IDs elegíveis na mesma passagem após encontrar a
     * maior iniciativa. Evita repetir a varredura da lista quando o controller
     * precisa das duas representações.
     */
    data class ElegiveisComIds(
        val participantes: List<ParticipanteIniciativa>,
        val ids: Set<String>
    )

    fun elegiveisComIds(participantes: List<ParticipanteIniciativa>): ElegiveisComIds {
        val max = maiorIniciativaElegivel(participantes)
        if (max == Int.MIN_VALUE) return ElegiveisComIds(emptyList(), emptySet())

        val resultado = ArrayList<ParticipanteIniciativa>()
        val ids = HashSet<String>()
        for (p in participantes) {
            if (!p.jaAgiramNesteTurno && p.iniciativa == max) {
                resultado += p
                ids += p.id
            }
        }
        return ElegiveisComIds(resultado, ids)
    }

    fun idsElegiveis(participantes: List<ParticipanteIniciativa>): Set<String> {
        val max = maiorIniciativaElegivel(participantes)
        if (max == Int.MIN_VALUE) return emptySet()
        val ids = HashSet<String>()
        for (p in participantes) {
            if (!p.jaAgiramNesteTurno && p.iniciativa == max) ids.add(p.id)
        }
        return ids
    }

    /**
     * Ordena a lista da Aba 12 exclusivamente pelo número de Iniciativa
     * (maior no topo). Pedido explícito: a ordem visual não depende de quem
     * já agiu nem de outros critérios — apenas do valor atual de iniciativa.
     *
     * Conforme o turno avança (transferências, Crash, Clash, etc.) e os
     * números mudam, a lista é naturalmente reorganizada na próxima leitura
     * de [participantesOrdenados].
     *
     * Empates usam [ordemInsercao] só como desempate estável (determinismo).
     */
    fun ordenar(participantes: List<ParticipanteIniciativa>): List<ParticipanteIniciativa> {
        val comparator = Comparator<ParticipanteIniciativa> { a, b ->
            if (a.iniciativa != b.iniciativa) return@Comparator b.iniciativa.compareTo(a.iniciativa)
            a.ordemInsercao.compareTo(b.ordemInsercao)
        }
        return participantes.sortedWith(comparator)
    }

    /** Finds mutual declarations in O(n), without a temporary 'vistos' set. */
    fun paresClash(declaracoes: Map<String, String>): List<Pair<String, String>> {
        if (declaracoes.size < 2) return emptyList()
        val pares = ArrayList<Pair<String, String>>(declaracoes.size / 2)
        for ((a, b) in declaracoes) {
            if (a < b && declaracoes[b] == a) pares += a to b
        }
        return pares
    }

    private fun maiorIniciativaElegivel(participantes: List<ParticipanteIniciativa>): Int {
        var max = Int.MIN_VALUE
        for (p in participantes) {
            if (!p.jaAgiramNesteTurno && p.iniciativa > max) max = p.iniciativa
        }
        return max
    }
}
