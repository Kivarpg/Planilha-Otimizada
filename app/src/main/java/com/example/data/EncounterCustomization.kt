package com.example.data

/** Estado semântico de cada escolha do modo Foco da Aba 11. */
enum class EncounterCustomizationMode { AUTOMATICO, NENHUM, EXPLICITO }

const val ENCOUNTER_FOCUS_SORCERY = "Feitiçaria"

/** Decisões estruturais opcionais do modo Foco da Aba 11.
 *
 * O modo é preservado até o gerador: AUTOMATICO permite a regra normal escolher,
 * NENHUM impede que aquela dimensão seja usada como preferência, e EXPLICITO usa
 * o valor fornecido pelo usuário.
 */
data class EncounterCustomization(
    val foco: String? = null,
    val ataque: String? = null,
    val defesa: String? = null,
    val secundaria: String? = null,
    val focoMode: EncounterCustomizationMode = if (foco == null) EncounterCustomizationMode.AUTOMATICO else EncounterCustomizationMode.EXPLICITO,
    val ataqueMode: EncounterCustomizationMode = if (ataque == null) EncounterCustomizationMode.AUTOMATICO else EncounterCustomizationMode.EXPLICITO,
    val defesaMode: EncounterCustomizationMode = if (defesa == null) EncounterCustomizationMode.AUTOMATICO else EncounterCustomizationMode.EXPLICITO,
    val secundariaMode: EncounterCustomizationMode = if (secundaria == null) EncounterCustomizationMode.AUTOMATICO else EncounterCustomizationMode.EXPLICITO
) {
    private fun explicit(value: String?, mode: EncounterCustomizationMode): String? =
        value?.takeIf { mode == EncounterCustomizationMode.EXPLICITO }

    val focoExplicito: String? get() = explicit(foco, focoMode)
    val ataqueExplicito: String? get() = explicit(ataque, ataqueMode)
    val defesaExplicita: String? get() = explicit(defesa, defesaMode)
    val secundariaExplicita: String? get() = explicit(secundaria, secundariaMode)

    fun focoEfetivo(fallbackAutomatico: String?): String? = when (focoMode) {
        EncounterCustomizationMode.AUTOMATICO -> fallbackAutomatico
        EncounterCustomizationMode.NENHUM -> null
        EncounterCustomizationMode.EXPLICITO -> foco
    }



    /**
     * Traduz a diretiva Feitiçaria para a âncora mecânica esperada pelo gerador,
     * sem inserir "Feitiçaria" nos domínios canônicos de Habilidade/Atributo.
     */
    fun focoMecanico(fallbackAutomatico: String?, ancoraFeiticaria: String): String? {
        val efetivo = focoEfetivo(fallbackAutomatico)
        return if (rotaFeiticaria || efetivo == ENCOUNTER_FOCUS_SORCERY) ancoraFeiticaria else efetivo
    }

    /** NENHUM ofensivo não herda o Foco como preferência de combate. */
    fun focoParaAtaque(focoEfetivo: String?): String? =
        focoEfetivo.takeUnless { ataqueMode == EncounterCustomizationMode.NENHUM }

    /** Diretiva estrutural: Feitiçaria é um Foco, não uma Habilidade/Atributo canônico. */
    val rotaFeiticaria: Boolean get() =
        focoMode == EncounterCustomizationMode.EXPLICITO && foco == ENCOUNTER_FOCUS_SORCERY

    val rotaArtesMarciais: Boolean get() = ataqueExplicito == EncounterGenerationRules.ATTACK_MARTIAL_ARTS
    val isEmpty: Boolean get() =
        focoMode == EncounterCustomizationMode.AUTOMATICO &&
            ataqueMode == EncounterCustomizationMode.AUTOMATICO &&
            defesaMode == EncounterCustomizationMode.AUTOMATICO &&
            secundariaMode == EncounterCustomizationMode.AUTOMATICO &&
            foco == null && ataque == null && defesa == null && secundaria == null
}
