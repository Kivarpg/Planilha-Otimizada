package com.example.data

/**
 * Resolve stacking por canal E grupo explícito.
 * Efeitos independentes não são suprimidos só porque outro grupo usa MAX_OF.
 */
internal object EncounterStackingSemantics {
    enum class Mode { ADDITIVE, MAX_OF, REPLACES, DOES_NOT_STACK, SHARED_CAP, UNKNOWN }

    data class Contribution(
        val id:String,
        val channel:String,
        val amount:Int?,
        val mode:Mode,
        val stackingGroup:String = channel,
        val cap:Int? = null,
        val replacesId:String? = null
    )

    data class Resolution(
        val knownTotal:Int?,
        val realizedIds:Set<String>,
        val suppressedIds:Set<String>,
        val uncertain:Boolean
    )

    fun resolve(contributions:List<Contribution>):Resolution {
        if(contributions.isEmpty()) return Resolution(0,emptySet(),emptySet(),false)
        val ids=contributions.map { it.id }
        if(ids.any { it.isBlank() } || ids.toSet().size!=ids.size)
            return Resolution(null,emptySet(),emptySet(),true)
        if(contributions.any { it.channel.isBlank() || it.stackingGroup.isBlank() })
            return Resolution(null,emptySet(),emptySet(),true)
        if(contributions.map{it.channel}.distinct().size!=1)
            return Resolution(null,emptySet(),emptySet(),true)
        if(contributions.any { it.cap!=null && it.cap<0 })
            return Resolution(null,emptySet(),emptySet(),true)
        if(contributions.any { it.replacesId==it.id })
            return Resolution(null,emptySet(),emptySet(),true)
        val knownIds=ids.toSet()
        if(contributions.any { it.replacesId!=null && it.replacesId !in knownIds })
            return Resolution(null,emptySet(),emptySet(),true)
        if(contributions.any { it.mode==Mode.UNKNOWN || it.amount==null })
            return Resolution(null,contributions.map{it.id}.toSet(),emptySet(),true)

        fun amount(c:Contribution):Int = requireNotNull(c.amount) {
            "Quantidade já deveria ter sido validada antes da resolução."
        }

        val byId=contributions.associateBy { it.id }
        val replaced=contributions.mapNotNull { it.replacesId }.filter { it in byId }.toSet()
        val active=contributions.filter { it.id !in replaced }
        val suppressed=replaced.toMutableSet()
        val realized=mutableSetOf<String>()
        var total=0

        for((_,group) in active.groupBy { it.stackingGroup }.toSortedMap()) {
            val modes=group.map { it.mode }.toSet()
            if(modes.size>1 && Mode.ADDITIVE !in modes) {
                return Resolution(null,active.map{it.id}.toSet(),suppressed,true)
            }

            when {
                group.any { it.mode==Mode.DOES_NOT_STACK || it.mode==Mode.MAX_OF } -> {
                    // A competição é local ao stackingGroup.
                    val competitive=group.filter {
                        it.mode==Mode.DOES_NOT_STACK || it.mode==Mode.MAX_OF
                    }
                    val additive=group.filter { it.mode==Mode.ADDITIVE || it.mode==Mode.REPLACES }
                    val best=competitive.sortedWith(compareByDescending<Contribution>(::amount).thenBy { it.id }).first()
                    try { total=Math.addExact(total,amount(best)) } catch(_:ArithmeticException) { return Resolution(null,realized,suppressed,true) }
                    realized+=best.id
                    suppressed+=competitive.filter { it.id!=best.id }.map { it.id }
                    for(c in additive.sortedBy { it.id }) { try { total=Math.addExact(total,amount(c)) } catch(_:ArithmeticException) { return Resolution(null,realized,suppressed,true) }; realized+=c.id }
                }
                else -> {
                    val subtotal=try { group.fold(0){a,c->Math.addExact(a,amount(c))} } catch(_:ArithmeticException) { return Resolution(null,realized,suppressed,true) }
                    val caps=group.filter { it.mode==Mode.SHARED_CAP }.mapNotNull { it.cap }
                    try { total=Math.addExact(total,if(caps.isNotEmpty()) minOf(subtotal,caps.min()) else subtotal) } catch(_:ArithmeticException) { return Resolution(null,realized,suppressed,true) }
                    realized+=group.map { it.id }
                }
            }
        }
        return Resolution(total,realized,suppressed,false)
    }
}

internal object EncounterDynamicCapResolver {
    data class DynamicContribution(
        val id:String,
        val channel:String,
        val amount:EncounterQuantityExpression,
        val mode:EncounterStackingSemantics.Mode,
        val stackingGroup:String = channel,
        val cap:EncounterQuantityExpression? = null
    )

    fun resolve(
        contributions:List<DynamicContribution>,
        build:EncounterBuildQuantities
    ):EncounterStackingSemantics.Resolution {
        if(contributions.any { it.cap!=null && it.cap.resolve(build.values)==null })
            return EncounterStackingSemantics.Resolution(
                null,contributions.map{it.id}.toSet(),emptySet(),true
            )
        return EncounterStackingSemantics.resolve(contributions.map {
            EncounterStackingSemantics.Contribution(
                it.id,it.channel,it.amount.resolve(build.values),it.mode,
                it.stackingGroup,it.cap?.resolve(build.values)
            )
        })
    }
}
