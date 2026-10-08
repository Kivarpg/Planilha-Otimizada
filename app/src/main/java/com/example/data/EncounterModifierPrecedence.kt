package com.example.data

/**
 * Resolve precedência explícita entre modificadores.
 *
 * Nunca usa ordem do JSON, ordem de leitura, nome ou ordem de aquisição
 * como substituto para uma regra mecânica de precedência.
 */
internal object EncounterModifierPrecedence {

    data class ModifierNode(
        val id: String,
        val before: Set<String> = emptySet(),
        val after: Set<String> = emptySet()
    )

    sealed interface Result {
        data class Ordered(val ids: List<String>) : Result
        data class Ambiguous(val remainingIds: Set<String>) : Result
        data class Cycle(val ids: Set<String>) : Result
        data class Invalid(val reason: String, val ids: Set<String>) : Result
    }

    fun resolve(nodes: Collection<ModifierNode>): Result {
        if (nodes.isEmpty()) return Result.Ordered(emptyList())
        val rawIds=nodes.map { it.id }
        val duplicates=rawIds.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        if (duplicates.isNotEmpty())
            return Result.Invalid("IDs de modificador duplicados.", duplicates)
        val ids=rawIds.toSet()
        val dangling=nodes.flatMap { n -> (n.before+n.after).filter { it !in ids }.map { n.id to it } }
        if(dangling.isNotEmpty())
            return Result.Invalid("Precedência referencia modificador inexistente.",
                dangling.flatMap { listOf(it.first,it.second) }.toSet())
        if(nodes.any { it.id in it.before || it.id in it.after })
            return Result.Invalid("Modificador possui precedência autorreferente.",
                nodes.filter { it.id in it.before || it.id in it.after }.map { it.id }.toSet())
        val edges=mutableMapOf<String,MutableSet<String>>()
        val indegree=ids.associateWith { 0 }.toMutableMap()
        ids.forEach { edges[it]=mutableSetOf() }

        fun edge(a:String,b:String) {
            if (a !in ids || b !in ids || a==b) return
            if (edges.getValue(a).add(b)) indegree[b]=indegree.getValue(b)+1
        }
        for (n in nodes) {
            n.before.forEach { edge(n.id,it) }
            n.after.forEach { edge(it,n.id) }
        }

        val ordered=mutableListOf<String>()
        var ambiguous=false
        while (ordered.size < ids.size) {
            val available=indegree.filter { (k,v) -> v==0 && k !in ordered }.keys
            if (available.isEmpty()) {
                val remaining=ids-ordered.toSet()
                return Result.Cycle(remaining)
            }
            if (available.size>1) ambiguous=true
            // Deterministic iteration is only for stable diagnostics, NOT semantic precedence.
            val chosen=available.sorted().first()
            ordered+=chosen
            edges.getValue(chosen).forEach { indegree[it]=indegree.getValue(it)-1 }
        }

        return if (ambiguous && nodes.size>1)
            Result.Ambiguous(ids)
        else Result.Ordered(ordered)
    }
}
