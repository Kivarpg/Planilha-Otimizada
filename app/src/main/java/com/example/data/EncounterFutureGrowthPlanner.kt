package com.example.data

/**
 * Strategic preference only. Nodes contain routes whose non-Essence minima and
 * spirit conditions are already satisfied by the planned NPC. Essence up to 5
 * is a future goal, never permission to acquire the target now.
 * Unknown/count prerequisites, missing definitions and cycles earn no bonus.
 * Current legality, XP costs, mandatory slots and tree order stay with callers.
 */
internal object EncounterFutureGrowthPlanner {
    data class Node(val name: String, val essence: Int, val routes: List<List<EncounterRequirement>>)
    data class Result(val scores: Map<String, Int>, val budgetExhausted: Boolean)

    fun plan(nodes: List<Node>, maxWork: Int = 20_000): Result {
        require(maxWork > 0)
        val byName = nodes.groupBy { it.name }.filterValues { it.size == 1 }.mapValues { it.value.single() }
        val complete = mutableMapOf<String, Set<String>>()
        var work = 0
        var exhausted = false
        lateinit var resolve: (String, Set<String>) -> Set<String>?
        fun requirement(req: EncounterRequirement, visiting: Set<String>): Set<String>? = when (req) {
            is EncounterRequirement.Charm -> resolve(req.name, visiting)
            is EncounterRequirement.AnyOf -> req.alternatives.mapNotNull { requirement(it, visiting) }
                .minWithOrNull(compareBy<Set<String>> { it.size }.thenBy { it.sorted().joinToString("|") })
            else -> null // Do not invent which charms satisfy aggregate or unknown requirements.
        }
        resolve = resolve@ { name, visiting ->
            if (++work > maxWork) { exhausted = true; return@resolve null }
            if (name in visiting || visiting.size >= 64) return@resolve null
            complete[name]?.let { return@resolve it }
            val node = byName[name]?.takeIf { it.essence in 1..5 } ?: return@resolve null
            val plans = node.routes.mapNotNull { route ->
                val result = linkedSetOf(name)
                for (req in route) {
                    val prerequisites = requirement(req, visiting + name) ?: return@mapNotNull null
                    result.addAll(prerequisites)
                }
                result.toSet()
            }
            plans.minWithOrNull(compareBy<Set<String>> { it.size }.thenBy { it.sorted().joinToString("|") })
                ?.also { complete[name] = it }
        }
        val scores = mutableMapOf<String, Int>()
        for (target in nodes) {
            if (exhausted) break
            val path = resolve(target.name, emptySet()) ?: continue
            // Higher Essence first; complete prerequisite depth is a bounded tie-break.
            val value = target.essence * 100 + path.size.coerceAtMost(20)
            for (name in path) scores[name] = maxOf(scores[name] ?: 0, value)
            if (exhausted) break
        }
        // A partial traversal must not bias selection toward catalog order.
        return if (exhausted) Result(emptyMap(), true) else Result(scores, false)
    }
}
