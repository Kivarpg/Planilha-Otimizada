package com.example.data

internal object EncounterSynergyLedger {
    enum class Layer { PAIR, PATH, IDENTITY, COVERAGE, DESTINATION, RESOURCE, STATE }
    data class Evidence(
        val causalKey:String,val layer:Layer,val value:Int,val confidence:Double,
        val sourceIds:Set<String>
    )
    data class Aggregate(
        val value:Int?,val retained:List<Evidence>,val discarded:List<Evidence>,
        val valid:Boolean=true
    )

    fun aggregate(items:Collection<Evidence>):Aggregate {
        if(items.any { it.causalKey.isBlank() || it.confidence !in 0.0..1.0 })
            return Aggregate(null,emptyList(),items.toList(),false)

        val retained=mutableListOf<Evidence>()
        val discarded=mutableListOf<Evidence>()
        for((_,group) in items.groupBy { it.causalKey }.toSortedMap()) {
            val ordered=group.sortedWith(
                compareByDescending<Evidence> { it.value.toDouble()*it.confidence }
                    .thenByDescending { it.confidence }
                    .thenBy { it.layer.name }
                    .thenBy { it.sourceIds.sorted().joinToString("|") }
            )
            retained+=ordered.first()
            discarded+=ordered.drop(1)
        }
        val total=try {
            retained.fold(0) { acc,e ->
                Math.addExact(acc,(e.value.toDouble()*e.confidence).toInt())
            }
        } catch (_:ArithmeticException) { return Aggregate(null,retained,discarded,false) }
        return Aggregate(total,retained,discarded,true)
    }
}
