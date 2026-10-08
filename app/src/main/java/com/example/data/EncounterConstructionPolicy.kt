package com.example.data

import com.example.model.ArquetipoEncontro
import com.example.model.ExaltedConstants
import com.example.model.TipoExaltadoEncontro

/**
 * Fonte única das regras estruturais e das preferências de construção da Aba 11.
 *
 * Regras MANDATORY são invariantes da identidade/planilha e podem invalidar uma
 * construção. PREFERENCES apenas orientam o sorteio e não devem ser tratadas
 * como erro quando a aleatoriedade ou o catálogo impedirem sua aplicação.
 */
internal object EncounterConstructionPolicy {
    enum class Kind { MANDATORY, PREFERENCE }

    data class Rule(
        val id: String,
        val kind: Kind,
        val description: String
    )

    fun identityRules(tipo: TipoExaltadoEncontro): List<Rule> = when (tipo) {
        TipoExaltadoEncontro.SOLAR -> listOf(
            Rule("solar.favored.count", Kind.MANDATORY, "5 Habilidades Favorecidas, incluindo a Supernal"),
            Rule("solar.supernal", Kind.MANDATORY, "Supernal presente entre as 5 Favorecidas"),
            Rule("solar.essence", Kind.MANDATORY, "Essência inicial 1")
        )
        TipoExaltadoEncontro.SANGUE_DE_DRAGAO -> listOf(
            Rule("db.aspect", Kind.MANDATORY, "Aspecto válido"),
            Rule("db.aspect.skills", Kind.MANDATORY, "As 5 Habilidades do Aspecto permanecem identificáveis"),
            Rule("db.favored.count", Kind.MANDATORY, "5 Habilidades Favorecidas adicionais"),
            Rule("db.favored.disjoint", Kind.MANDATORY, "Favorecidas adicionais fora das 5 Habilidades do Aspecto"),
            Rule("db.essence", Kind.MANDATORY, "Essência inicial 2")
        )
        TipoExaltadoEncontro.LUNAR -> listOf(
            Rule("lunar.caste.attributes", Kind.MANDATORY, "2 Atributos de Casta"),
            Rule("lunar.favored.attributes", Kind.MANDATORY, "2 Atributos Favorecidos adicionais"),
            Rule("lunar.special.attributes", Kind.MANDATORY, "4 Atributos especiais distintos"),
            Rule("lunar.essence", Kind.MANDATORY, "Essência inicial 1")
        )
    }

    fun baseConstructionRules(): List<Rule> = listOf(
        Rule("attributes.base.total", Kind.MANDATORY, "Atributos base somam 27 pontos"),
        Rule("attributes.base.categories", Kind.MANDATORY, "Categorias base preservam 11/9/7"),
        Rule("attributes.base.primary", Kind.MANDATORY, "A categoria primária possui pelo menos um Atributo 5"),
        Rule("abilities.normal.total", Kind.MANDATORY, "A distribuição normal usa exatamente 28 pontos"),
        Rule("bonus.total", Kind.MANDATORY, "A distribuição de PB encerra sem saldo não contabilizado"),
        Rule("combat.focus", Kind.MANDATORY, "Existe uma única Habilidade de combate principal e sua defesa correspondente")
    )

    /**
     * Arquétipo define somente a direção principal. O restante da ficha pertence
     * ao gerador e deve ser preenchido com a melhor combinação legal disponível.
     * Foco explícito é uma intenção mais forte do usuário e deve prevalecer sobre
     * preferências automáticas, sem substituir as invariantes de legalidade.
     */
    fun archetypePreferences(arquetipo: ArquetipoEncontro): List<Rule> = listOf(
        Rule("archetype.primary.attribute", Kind.MANDATORY, "Pelo menos um Atributo do arquétipo chega a 5"),
        Rule("archetype.primary.direction", Kind.PREFERENCE, "O arquétipo define a competência principal; o sistema preenche autonomamente as competências restantes"),
        Rule("archetype.combat.focus", Kind.PREFERENCE, "Uma única Habilidade de combate recebe o foco principal"),
        Rule("archetype.martial.arts", Kind.PREFERENCE, "A preferência por Artes Marciais varia por arquétipo"),
        Rule("archetype.charm.blocks", Kind.PREFERENCE, "Encantos aprofundam Árvores em blocos antes de abrir muitas Árvores"),
        Rule("build.current.synergy", Kind.PREFERENCE, "Entre opções legais, priorizar eficiência atual e sinergia da construção"),
        Rule("build.future.growth", Kind.PREFERENCE, "Entre opções próximas, favorecer progressão futura coerente e de baixo custo"),
        Rule("build.controlled.variety", Kind.PREFERENCE, "Aleatoriedade atua entre alternativas de qualidade comparável")
    )

    fun isKnownAttribute(name: String): Boolean = name in ExaltedConstants.ALL_ATTRIBUTES
    fun isKnownAbility(name: String): Boolean = name in ExaltedConstants.ALL_25_ABILITIES
}
