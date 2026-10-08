package com.example.data

/**
 * Geometria/cardinalidade estrutural de alvos.
 *
 * Um poder pode oferecer alternativas ("um alvo OU vários"), repetir ataques
 * contra o mesmo alvo, atingir uma área indiscriminadamente ou selecionar
 * subconjuntos. Não atribui valor tático a nenhuma dessas opções.
 */
internal object EncounterTargetShape {
    enum class Kind {
        SELF, CHARACTER, ALLY, ENEMY, BATTLE_GROUP, OBJECT, AREA, CREATED_ENTITY, UNKNOWN
    }
    enum class Selection {
        SINGLE, MULTIPLE_DISTINCT, REPEATED_SAME, SINGLE_OR_MULTIPLE,
        ALL_IN_AREA, SELECTED_IN_AREA, UNKNOWN
    }
    enum class Inclusion {
        ONLY_ALLOWED_ACTORS, MAY_INCLUDE_ALLIES, MAY_INCLUDE_SELF, INDISCRIMINATE, UNKNOWN
    }

    data class Shape(
        val kinds:Set<Kind>,
        val selection:Selection,
        val minApplications:Int?=null,
        val maxApplications:EncounterQuantityExpression?=null,
        val inclusion:Inclusion=Inclusion.ONLY_ALLOWED_ACTORS
    )

    data class Requirement(
        val allowedKinds:Set<Kind>,
        val acceptedSelections:Set<Selection>,
        val minimumApplications:Int?=null,
        val forbidsFriendlyFire:Boolean=false
    )

    /**
     * TRUE = existe uma alternativa estrutural comprovadamente compatível.
     * FALSE = nenhuma alternativa pode satisfazer.
     * null = dados insuficientes.
     */
    fun compatible(
        shape:Shape, requirement:Requirement, build:EncounterBuildQuantities
    ):Boolean? {
        if(shape.kinds.isEmpty() || requirement.allowedKinds.isEmpty()) return false
        if(shape.kinds.contains(Kind.UNKNOWN) || shape.selection==Selection.UNKNOWN) return null
        if(shape.minApplications!=null && shape.minApplications<0) return null
        if(requirement.minimumApplications!=null && requirement.minimumApplications<0) return null
        fun kindCompatible(actual:Kind,allowed:Kind):Boolean? = when {
            actual==Kind.UNKNOWN || allowed==Kind.UNKNOWN -> null
            actual==allowed -> true
            actual==Kind.CHARACTER && allowed in setOf(Kind.SELF,Kind.ALLY,Kind.ENEMY) -> null
            allowed==Kind.CHARACTER && actual in setOf(Kind.SELF,Kind.ALLY,Kind.ENEMY) -> true
            else -> false
        }
        val kindChecks=shape.kinds.flatMap { a -> requirement.allowedKinds.map { b -> kindCompatible(a,b) } }
        if(kindChecks.any { it==true }) Unit
        else if(kindChecks.any { it==null }) return null
        else return false

        if(requirement.forbidsFriendlyFire) {
            if(shape.inclusion in setOf(Inclusion.MAY_INCLUDE_ALLIES,Inclusion.INDISCRIMINATE))
                return false
            if(shape.inclusion==Inclusion.UNKNOWN) return null
        }

        val selectionCompatible = when(shape.selection) {
            Selection.SINGLE_OR_MULTIPLE ->
                requirement.acceptedSelections.any {
                    it==Selection.SINGLE || it==Selection.MULTIPLE_DISTINCT ||
                    it==Selection.SINGLE_OR_MULTIPLE
                }
            else -> shape.selection in requirement.acceptedSelections
        }
        if(!selectionCompatible) return false

        val needed=requirement.minimumApplications ?: return true
        val min=shape.minApplications
        if(min!=null && min>=needed) return true
        val max=shape.maxApplications?.resolve(build.values) ?: return null
        if(min!=null && max<min) return null
        return max>=needed
    }
}
