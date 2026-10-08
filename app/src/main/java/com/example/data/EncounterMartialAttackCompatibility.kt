package com.example.data

/**
 * Afinidade de equipamento entre estilos de Artes Marciais.
 *
 * Arma igual/forma igual é uma vantagem, NÃO uma condição de legalidade.
 * Estilos com armas diferentes continuam válidos na mesma ficha e podem
 * alternar a configuração de ataque. A legalidade concreta de um Encanto
 * continua sendo determinada pelos requisitos daquele Encanto/estilo.
 */
internal object EncounterMartialAttackCompatibility {
    enum class AttackForm { UNARMED, FIREARM, OTHER_WEAPON }

    private fun normalized(text: String) = text.lowercase()
        .replace('–', '-').replace('—', '-')

    fun forms(style: EstiloArteMarcialDefinition): Set<AttackForm> {
        val mode = normalized(style.armaDoEstiloModo)
        val weaponText = buildString {
            append(style.armaDoEstiloTexto.orEmpty())
            append(' ')
            append(style.armasEspecificas.joinToString(" "))
        }.let(::normalized)
        return buildSet {
            if ("desarmado" in mode || "desarmad" in weaponText || "hibrido" in mode)
                add(AttackForm.UNARMED)
            if (listOf("pistola", "revólver", "revolver", "varinha de fogo", "firewand")
                    .any(weaponText::contains)) add(AttackForm.FIREARM)
            if (style.armasEspecificas.isNotEmpty() &&
                style.armasEspecificas.any {
                    val w = normalized(it)
                    listOf("pistola", "revólver", "revolver", "varinha de fogo", "firewand")
                        .none(w::contains)
                }
            ) add(AttackForm.OTHER_WEAPON)
        }
    }


    private fun weaponTokens(style: EstiloArteMarcialDefinition): Set<String> =
        style.armasEspecificas.asSequence()
            .map(::normalized)
            .map { it.substringBefore(" (").trim() }
            .filter(String::isNotBlank)
            .toSet()

    private fun shareSpecificWeapon(first: EstiloArteMarcialDefinition, second: EstiloArteMarcialDefinition): Boolean {
        val a = weaponTokens(first)
        val b = weaponTokens(second)
        return a.any { left ->
            b.any { right ->
                left == right || left.contains(right) || right.contains(left) ||
                    ("pistola" in left && ("pistola" in right || "revólver" in right || "revolver" in right)) ||
                    ("pistola" in right && ("pistola" in left || "revólver" in left || "revolver" in left))
            }
        }
    }

    /** Compartilhar forma/arma aumenta a afinidade, mas nunca veta dois estilos. */
    fun sharedAttackForms(first: EstiloArteMarcialDefinition, second: EstiloArteMarcialDefinition): Set<AttackForm> =
        forms(first).intersect(forms(second))

    fun weaponAffinity(first: EstiloArteMarcialDefinition, second: EstiloArteMarcialDefinition): Int {
        val shared = sharedAttackForms(first, second)
        return when {
            shared.isEmpty() -> 0
            AttackForm.FIREARM in shared && shareSpecificWeapon(first, second) -> 4
            AttackForm.UNARMED in shared -> 3
            AttackForm.OTHER_WEAPON in shared && shareSpecificWeapon(first, second) -> 2
            else -> 0
        }
    }

    fun supportsWeapon(style: EstiloArteMarcialDefinition, weaponName: String?): Boolean {
        if (weaponName == null) return AttackForm.UNARMED in forms(style)
        val weapon = normalized(weaponName)
        if (AttackForm.FIREARM in forms(style) &&
            listOf("pistola", "revólver", "revolver", "varinha de fogo", "firewand")
                .any(weapon::contains)
        ) return true
        return style.armasEspecificas.any { allowed ->
            val token = normalized(allowed)
            val baseToken = token.substringBefore(" (").trim()
            val baseWeapon = weapon.substringBefore(" (").trim()
            weapon.contains(token) || weapon.contains(baseToken) ||
                token.contains(baseWeapon) ||
                ("pistola" in token && ("pistola" in weapon || "revólver" in weapon || "revolver" in weapon))
        }
    }

    /** Compatibilidade da configuração atual; usada para bônus, não para legalidade global. */
    fun canShareAttackWithWeapon(
        first: EstiloArteMarcialDefinition,
        second: EstiloArteMarcialDefinition,
        weaponName: String?
    ): Boolean = supportsWeapon(first, weaponName) && supportsWeapon(second, weaponName)
}
