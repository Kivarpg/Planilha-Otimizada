package com.example.data

/**
 * Assinatura estrutural de uma ação criada/aprimorada.
 * Não escolhe ação; apenas prova compatibilidade.
 */
internal object EncounterActionSignature {
    data class Signature(
        val kind:EncounterCharmActivationCompatibility.ActionKind,
        val governingTrait:String?=null,
        val weaponClasses:Set<String> = emptySet(),
        val ranges:Set<String> = emptySet(),
        val properties:Set<String> = emptySet(),
        val charmEnhanceable:Boolean?=null
    )
    data class Requirement(
        val kinds:Set<EncounterCharmActivationCompatibility.ActionKind>,
        val governingTraits:Set<String> = emptySet(),
        val weaponClasses:Set<String> = emptySet(),
        val ranges:Set<String> = emptySet(),
        val requiredProperties:Set<String> = emptySet(),
        val requiresCharmEnhanceable:Boolean=true
    )
    enum class Compatibility { COMPATIBLE, INCOMPATIBLE, UNKNOWN }

    fun compatible(action:Signature,req:Requirement):Compatibility {
        if(req.kinds.isNotEmpty() && action.kind !in req.kinds) return Compatibility.INCOMPATIBLE
        if(req.requiresCharmEnhanceable) when(action.charmEnhanceable) {
            false -> return Compatibility.INCOMPATIBLE
            null -> return Compatibility.UNKNOWN
            true -> Unit
        }
        fun dimension(actual:String?,allowed:Set<String>):Compatibility {
            if(allowed.isEmpty()) return Compatibility.COMPATIBLE
            if(actual==null) return Compatibility.UNKNOWN
            return if(actual in allowed) Compatibility.COMPATIBLE else Compatibility.INCOMPATIBLE
        }
        val trait=dimension(action.governingTrait,req.governingTraits)
        if(trait!=Compatibility.COMPATIBLE) return trait
        if(req.weaponClasses.isNotEmpty()) {
            if(action.weaponClasses.isEmpty()) return Compatibility.UNKNOWN
            if(action.weaponClasses.intersect(req.weaponClasses).isEmpty()) return Compatibility.INCOMPATIBLE
        }
        if(req.ranges.isNotEmpty()) {
            if(action.ranges.isEmpty()) return Compatibility.UNKNOWN
            if(action.ranges.intersect(req.ranges).isEmpty()) return Compatibility.INCOMPATIBLE
        }
        if(!action.properties.containsAll(req.requiredProperties)) return Compatibility.INCOMPATIBLE
        return Compatibility.COMPATIBLE
    }
}
