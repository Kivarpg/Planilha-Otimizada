package com.example.data

/**
 * Compatibilidade temporal/operacional entre Encantos.
 *
 * Regra de projeto:
 * afinidade mecânica != combinação realizável.
 *
 * Base: Exalted 3e Core, seção "Type":
 * - Simple: é uma ação; não entra em flurry; regra geral = um Simple por rodada.
 * - Supplemental: aprimora uma ação válida; vários podem aprimorar a mesma ação,
 *   mas o mesmo Charm não é empilhado consigo mesmo na mesma ação.
 * - Reflexive: depende de sua janela/gatilho e não ganha stacking irrestrito.
 * - Permanent: benefício passivo; não disputa uma ação de ativação.
 *
 * Exceções explícitas do texto de um Charm podem substituir a regra geral.
 */
internal object EncounterCharmActivationCompatibility {

    enum class CharmType {
        SIMPLE,
        SUPPLEMENTAL,
        REFLEXIVE,
        PERMANENT,
        UNKNOWN
    }

    enum class Window {
        PASSIVE,
        SAME_ACTION,
        SAME_ATTACK,
        SAME_DEFENSE,
        SAME_ROLL,
        SAME_TURN,
        SAME_ROUND,
        CROSS_ROUND,
        PERSISTENT_STATE
    }

    enum class DurationClass {
        INSTANT,
        ONE_TURN,
        ONE_ROUND,
        ONE_SCENE,
        INDEFINITE,
        PERMANENT,
        CONDITIONAL,
        UNKNOWN
    }

    enum class ActionKind {
        ATTACK,
        WITHERING_ATTACK,
        DECISIVE_ATTACK,
        DEFENSE,
        MOVEMENT,
        DISENGAGE,
        RUSH,
        AIM,
        GRAPPLE,
        SOCIAL,
        CRAFT,
        SORCERY,
        OTHER
    }

    enum class StackingRule {
        NORMAL,
        NO_SELF_STACK,
        EXPLICIT_EXCEPTION
    }

    data class Profile(
        val charmId: String,
        val type: CharmType,
        val duration: DurationClass = DurationClass.UNKNOWN,
        val supplements: Set<ActionKind> = emptySet(),
        val createsAction: ActionKind? = null,
        val createsAttack: Boolean = false,
        val modifiesStaticValue: Boolean = false,
        val effects: List<EncounterMechanicalEffect> = emptyList(),
        val explicitAllowedWith: Set<String> = emptySet(),
        val explicitAllowedWindow: Map<String,Window> = emptyMap(),
        val explicitForbiddenWith: Set<String> = emptySet(),
        val stackingRule: StackingRule = StackingRule.NORMAL
    )

    enum class Compatibility {
        SAME_ACTION,
        SAME_ROUND_SEPARATE_WINDOW,
        CROSS_ROUND,
        PASSIVE_SUPPORT,
        BUILD_ONLY,
        FORBIDDEN
    }

    data class Result(
        val compatibility: Compatibility,
        val realizableNow: Boolean,
        val reason: String
    )

    fun evaluate(
        a: Profile,
        b: Profile,
        requiredStateFromA: String? = null
    ): Result {
        if (b.charmId in a.explicitForbiddenWith || a.charmId in b.explicitForbiddenWith) {
            return Result(
                Compatibility.FORBIDDEN,
                false,
                "Incompatibilidade explícita entre os Encantos."
            )
        }

        if (b.charmId in a.explicitAllowedWith || a.charmId in b.explicitAllowedWith) {
            val window=a.explicitAllowedWindow[b.charmId] ?: b.explicitAllowedWindow[a.charmId]
            return when(window) {
                Window.SAME_ACTION,Window.SAME_ATTACK,Window.SAME_DEFENSE,Window.SAME_ROLL ->
                    Result(Compatibility.SAME_ACTION,true,"Exceção explícita autoriza esta janela.")
                Window.SAME_TURN,Window.SAME_ROUND ->
                    Result(Compatibility.SAME_ROUND_SEPARATE_WINDOW,true,"Exceção explícita autoriza coexistência na rodada.")
                Window.CROSS_ROUND,Window.PERSISTENT_STATE ->
                    Result(Compatibility.CROSS_ROUND,false,"Exceção explícita autoriza relação entre rodadas.")
                Window.PASSIVE -> Result(Compatibility.PASSIVE_SUPPORT,true,"Exceção explícita é passiva.")
                null -> Result(Compatibility.BUILD_ONLY,false,
                    "Há permissão explícita, mas a janela operacional não foi modelada; não presumir SAME_ACTION.")
            }
        }

        if (a.type == CharmType.PERMANENT || b.type == CharmType.PERMANENT) {
            return Result(
                Compatibility.PASSIVE_SUPPORT,
                true,
                "Encanto Permanente fornece suporte passivo e não disputa a ação de ativação."
            )
        }

        // Regra crítica: dois Simple não são uma combinação de mesma rodada.
        if (a.type == CharmType.SIMPLE && b.type == CharmType.SIMPLE) {
            val support = if (requiredStateFromA != null) {
                EncounterEffectRealizability.supports(
                    requiredMechanic = requiredStateFromA,
                    effects = a.effects,
                    facts = emptySet(),
                    requireCrossRound = true
                )
            } else null

            return if (support?.supported == true) {
                Result(
                    Compatibility.CROSS_ROUND,
                    false,
                    "Dois Encantos Simples não combinam na mesma rodada; a relação só é realizável em rodadas diferentes porque um efeito produtor específico persiste."
                )
            } else {
                Result(
                    Compatibility.BUILD_ONLY,
                    false,
                    "Dois Encantos Simples não podem ser tratados como combinação na mesma rodada; a duração global do Charm não prova persistência do efeito necessário."
                )
            }
        }

        if (a.type == CharmType.SUPPLEMENTAL && b.type == CharmType.SUPPLEMENTAL) {
            val shared = a.supplements.intersect(b.supplements)
            return if (shared.isNotEmpty() && a.charmId != b.charmId) {
                Result(
                    Compatibility.SAME_ACTION,
                    true,
                    "Os dois Encantos Suplementares podem aprimorar uma mesma ação válida."
                )
            } else {
                Result(
                    Compatibility.BUILD_ONLY,
                    false,
                    "Não foi identificada uma ação comum válida; o mesmo Encanto também não pode empilhar consigo mesmo na mesma ação."
                )
            }
        }

        // Um Simple pode criar uma ação que um Supplemental legalmente aprimore.
        if (a.type == CharmType.SIMPLE && b.type == CharmType.SUPPLEMENTAL) {
            val action = a.createsAction
            return if (action != null && action in b.supplements) {
                Result(
                    Compatibility.SAME_ACTION,
                    true,
                    "O Encanto Simples cria uma ação que o Encanto Suplementar pode legalmente aprimorar."
                )
            } else {
                Result(
                    Compatibility.BUILD_ONLY,
                    false,
                    "O Encanto Suplementar não possui uma ação válida identificada dentro da ação criada pelo Encanto Simples."
                )
            }
        }

        if (b.type == CharmType.SIMPLE && a.type == CharmType.SUPPLEMENTAL) {
            return evaluate(b, a, requiredStateFromA = null)
        }

        // Reflexive exige gatilho/janela específica; o tipo sozinho não autoriza stacking.
        if (a.type == CharmType.REFLEXIVE || b.type == CharmType.REFLEXIVE) {
            return Result(
                Compatibility.SAME_ROUND_SEPARATE_WINDOW,
                false,
                "Há compatibilidade potencial, mas a realizabilidade depende do gatilho, alvo da melhoria e regra específica do Reflexivo; o tipo sozinho não autoriza empilhamento."
            )
        }

        return Result(
            Compatibility.BUILD_ONLY,
            false,
            "Afinidade de construção possível, mas a combinação operacional não foi demonstrada."
        )
    }
}
