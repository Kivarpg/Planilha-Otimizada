package com.example.data

import com.example.model.ArquetipoEncontro
import com.example.model.ExaltedConstants

/** Shared immutable rules used by all NPC encounter generators. */
internal object EncounterGenerationRules {
    // APPROVED VISUAL CUSTOMIZATION
    // DO NOT REMOVE OR MODIFY WITHOUT VISUAL IMPACT REVIEW
    // This file contains generation rules only; the marker is retained so
    // future refactors do not accidentally mix generation and UI concerns.

    val ATTRIBUTE_GROUPS: Map<ArquetipoEncontro, List<String>> = mapOf(
        ArquetipoEncontro.FISICO to ExaltedConstants.PHYSICAL_ATTRIBUTES,
        ArquetipoEncontro.SOCIAL to ExaltedConstants.SOCIAL_ATTRIBUTES,
        ArquetipoEncontro.MENTAL to ExaltedConstants.MENTAL_ATTRIBUTES
    )

    const val HABILIDADE_OCULTISMO = "Ocultismo"
    const val MIN_OCULTISMO_FEITICARIA = 3
    // Quantidade de Encantos de Ocultismo exigida como pré-requisito da Feitiçaria.
    // É distinta do nível mínimo da Habilidade Ocultismo (3).
    const val QUANTIDADE_OCULTISMO_FEITICARIA = 4
    const val CHANCE_FEITICARIA_MENTAL_EM_DEZ = 9
    const val ENCANTOS_INICIAIS = 15
    const val ENCANTOS_INICIAIS_COM_FEITICARIA_MENTAL = ENCANTOS_INICIAIS - 1
    const val ATRIBUTOS_BASE = 1
    const val ATRIBUTOS_MAX = 5
    const val ESSENCIA_SOLAR = 1
    const val ESSENCIA_SANGUE_DE_DRAGAO = 2
    const val ESSENCIA_LUNAR = 1

    const val ATTACK_MARTIAL_ARTS = "Artes Marciais"

    /** Habilidades reais. Artes Marciais é um método/rota, não uma quinta Habilidade. */
    val COMBAT_ABILITIES: List<String> = listOf(
        "Armas Brancas", "Arqueirismo", "Arremesso", "Briga"
    )
    val ATTACK_METHODS: List<String> = COMBAT_ABILITIES + ATTACK_MARTIAL_ARTS

    fun habilidadeRealDoMetodoDeAtaque(metodo: String?): String? = when (metodo) {
        ATTACK_MARTIAL_ARTS -> "Briga"
        in COMBAT_ABILITIES -> metodo
        else -> null
    }

    val SOCIAL_ABILITIES: List<String> = listOf(
        "Presença", "Performance", "Socialização"
    )

    val MENTAL_ABILITIES: List<String> = listOf(
        "Ocultismo", "Conhecimento", "Medicina", "Investigação"
    )

    val LUNAR_ABILITY_PROFILES: Map<ArquetipoEncontro, List<String>> = mapOf(
        ArquetipoEncontro.FISICO to listOf(
            "Atletismo", "Resistência", "Sobrevivência", "Furtividade", "Prontidão", "Esquiva"
        ),
        ArquetipoEncontro.SOCIAL to listOf(
            "Presença", "Performance", "Socialização", "Integridade", "Burocracia", "Linguística", "Furtividade"
        ),
        ArquetipoEncontro.MENTAL to listOf(
            "Ocultismo", "Conhecimento", "Investigação", "Medicina", "Burocracia", "Linguística", "Prontidão"
        )
    )

    /** A defesa obrigatória depende apenas da Habilidade de combate escolhida. */
    fun habilidadeDefensivaPara(habilidadeCombate: String): String = when (habilidadeCombate) {
        "Armas Brancas", "Briga" -> habilidadeCombate
        "Arqueirismo", "Arremesso" -> "Esquiva"
        else -> error("Habilidade de combate inválida: $habilidadeCombate")
    }

    /**
     * Distribui os três grupos de Atributos sem repetir sorteios.
     * A mesma função é usada pelos três tipos de Exaltado para manter a regra
     * {primário +8, secundário +6, terciário +4} em um único ponto.
     */
    fun gruposDeAtributoPara(arquetipo: ArquetipoEncontro, random: kotlin.random.Random): Triple<List<String>, List<String>, List<String>> {
        val primarios = ATTRIBUTE_GROUPS.getValue(arquetipo)
        val naoPrimarios = ATTRIBUTE_GROUPS
            .filterKeys { it != arquetipo }
            .values
            .toList()
            .shuffled(random)
        return Triple(primarios, naoPrimarios[0], naoPrimarios[1])
    }

    /** Mantém a mesma semântica de 9 em 10 sem trocar o consumo do Random. */
    fun sortearFeiticariaMental(random: kotlin.random.Random): Boolean =
        random.nextInt(0, 10) < CHANCE_FEITICARIA_MENTAL_EM_DEZ

    /** Seleciona a Habilidade de combate, usando o Supernal somente quando ele é de combate. */
    fun escolherHabilidadeCombate(supernal: String?, random: kotlin.random.Random): String =
        supernal?.takeIf { it in COMBAT_ABILITIES } ?: COMBAT_ABILITIES.random(random)

    /**
     * Ordem do modo Foco: escolha explícita > Foco ofensivo > regra normal do gerador.
     * Para Lunares o foco é um Atributo, portanto naturalmente cai no último caso.
     */
    fun resolverHabilidadeCombate(
        escolhaUsuario: String?,
        focoUsuario: String?,
        supernal: String?,
        random: kotlin.random.Random
    ): String = habilidadeRealDoMetodoDeAtaque(escolhaUsuario)
        ?: focoUsuario?.takeIf { it in COMBAT_ABILITIES }
        ?: escolherHabilidadeCombate(supernal, random)


    fun arquetipoParaHabilidade(habilidade: String): ArquetipoEncontro = when {
        habilidade in COMBAT_ABILITIES || habilidade in LUNAR_ABILITY_PROFILES.getValue(ArquetipoEncontro.FISICO) -> ArquetipoEncontro.FISICO
        habilidade in SOCIAL_ABILITIES || habilidade in LUNAR_ABILITY_PROFILES.getValue(ArquetipoEncontro.SOCIAL) -> ArquetipoEncontro.SOCIAL
        else -> ArquetipoEncontro.MENTAL
    }

    fun arquetipoParaAtributo(atributo: String): ArquetipoEncontro = when (atributo) {
        in ExaltedConstants.PHYSICAL_ATTRIBUTES -> ArquetipoEncontro.FISICO
        in ExaltedConstants.SOCIAL_ATTRIBUTES -> ArquetipoEncontro.SOCIAL
        in ExaltedConstants.MENTAL_ATTRIBUTES -> ArquetipoEncontro.MENTAL
        else -> error("Atributo inválido para personalização: $atributo")
    }

    fun priorizarAtributoSelecionado(attributes: Map<String, Int>, atributo: String): Map<String, Int> {
        if (atributo !in ALL_ATTRIBUTES) return attributes
        val grupo = ATTRIBUTE_GROUPS.getValue(arquetipoParaAtributo(atributo))
        val maior = grupo.maxOf { attributes[it] ?: 1 }
        val atual = attributes[atributo] ?: 1
        if (atual >= maior) return attributes
        val doador = grupo.firstOrNull { (attributes[it] ?: 1) == maior && it != atributo } ?: return attributes
        return attributes.toMutableMap().apply {
            this[atributo] = maior
            this[doador] = atual
        }
    }

    val ALL_ATTRIBUTES: List<String> = ExaltedConstants.PHYSICAL_ATTRIBUTES +
        ExaltedConstants.SOCIAL_ATTRIBUTES +
        ExaltedConstants.MENTAL_ATTRIBUTES
}
