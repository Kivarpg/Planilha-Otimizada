package com.example.data

/**
 * Expressões quantitativas resolvidas SOMENTE a partir da ficha/build.
 * Não estimam rolagens, frequência de combate, dano esperado ou comportamento.
 */
internal sealed interface EncounterQuantityExpression {
    fun resolve(values: Map<String, Int>): Int?

    data class Constant(val value: Int) : EncounterQuantityExpression {
        override fun resolve(values: Map<String, Int>) = value
    }

    data class Trait(val key: String) : EncounterQuantityExpression {
        override fun resolve(values: Map<String, Int>) = values[key]
    }

    data class Add(
        val terms: List<EncounterQuantityExpression>
    ) : EncounterQuantityExpression {
        override fun resolve(values: Map<String, Int>): Int? {
            val resolved = terms.map { it.resolve(values) }
            if (resolved.any { it == null }) return null
            return try {
                resolved.filterNotNull().fold(0) { acc, v -> Math.addExact(acc, v) }
            } catch (_: ArithmeticException) { null }
        }
    }

    data class Min(
        val terms: List<EncounterQuantityExpression>
    ) : EncounterQuantityExpression {
        override fun resolve(values: Map<String, Int>): Int? =
            terms.map { it.resolve(values) }.takeIf { it.none { v -> v == null } }
                ?.filterNotNull()?.minOrNull()
    }

    data class Max(
        val terms: List<EncounterQuantityExpression>
    ) : EncounterQuantityExpression {
        override fun resolve(values: Map<String, Int>): Int? =
            terms.map { it.resolve(values) }.takeIf { it.none { v -> v == null } }
                ?.filterNotNull()?.maxOrNull()
    }

    data class Multiply(
        val left: EncounterQuantityExpression,
        val right: EncounterQuantityExpression
    ) : EncounterQuantityExpression {
        override fun resolve(values: Map<String, Int>): Int? {
            val a=left.resolve(values) ?: return null
            val b=right.resolve(values) ?: return null
            return try { Math.multiplyExact(a,b) }
            catch (_: ArithmeticException) { null }
        }
    }

    data class HalfRoundedUp(
        val value: EncounterQuantityExpression
    ) : EncounterQuantityExpression {
        override fun resolve(values: Map<String, Int>): Int? {
            val v=value.resolve(values) ?: return null
            return if (v >= 0) v/2 + v%2
            else v/2 // domínio negativo não deve overflowar; validação decide se é admissível.
        }
    }

    data object Unknown : EncounterQuantityExpression {
        override fun resolve(values: Map<String, Int>) = null
    }
}

internal data class EncounterBuildQuantities(
    val values: Map<String, Int>
)
