package com.example.data

import com.example.model.NpcEncontro

/**
 * Modelo neutro de intercâmbio. Não conhece Compose, SharedPreferences,
 * JSONObject ou layout de PDF. Exportadores concretos devem consumir isto.
 */
internal data class ExaltedCharacterExchange(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val id: String,
    val name: String,
    val exaltType: String,
    val archetype: String,
    val caste: String,
    val essence: Int,
    val attributes: Map<String, Int>,
    val abilities: Map<String, Int>,
    val charms: List<ExchangeCharm>,
    val spells: List<ExchangeSpell>,
    val martialStyleIds: List<String>,
    val spiritForms: List<String>,
    val xp: ExchangeXp,
    val combat: ExchangeCombat
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
    }
}

internal data class ExchangeCharm(
    val canonicalId: String?,
    val name: String,
    val linkedTrait: String,
    val cost: String
)

internal data class ExchangeSpell(
    val canonicalId: String?,
    val name: String,
    val circle: String,
    val cost: String,
    val initial: Boolean
)

internal data class ExchangeXp(
    val current: Int,
    val spent: Int,
    val firstXpReceived: Boolean
)

internal data class ExchangeCombat(
    val mainAction: Int,
    val decisiveAction: Int,
    val primaryDefense: Int?,
    val dodge: Int,
    val soak: Int,
    val hardness: Int,
    val resolve: Int,
    val guile: Int,
    val joinBattle: Int,
    val currentInitiative: Int,
    val damage: String
)

internal object ExaltedCharacterExchangeMapper {
    fun fromNpc(
        npc: NpcEncontro,
        catalog: PreparedEncounterCatalog
    ): ExaltedCharacterExchange {
        fun canonicalIdForName(name: String, allowedNamespaces: Set<String>): String? =
            catalog.findByName(name)
                .filter { it.id.namespace in allowedNamespaces }
                .singleOrNull()
                ?.id?.value

        val exaltNamespace = when (npc.tipoExaltado) {
            com.example.model.TipoExaltadoEncontro.SOLAR -> PreparedEncounterCatalog.NS_SOLAR
            com.example.model.TipoExaltadoEncontro.SANGUE_DE_DRAGAO -> PreparedEncounterCatalog.NS_DRAGON_BLOODED
            com.example.model.TipoExaltadoEncontro.LUNAR -> PreparedEncounterCatalog.NS_LUNAR
        }
        val charmNamespaces = setOf(exaltNamespace, PreparedEncounterCatalog.NS_MARTIAL_CHARM)

        return ExaltedCharacterExchange(
            id = npc.id,
            name = npc.nome,
            exaltType = npc.tipoExaltado.name,
            archetype = npc.arquetipo.name,
            caste = npc.casta,
            essence = npc.essencia,
            attributes = npc.attributes.toSortedMap(),
            abilities = npc.abilities.toSortedMap(),
            charms = npc.charms.map {
                ExchangeCharm(
                    canonicalId = canonicalIdForName(it.nome, charmNamespaces),
                    name = it.nome,
                    linkedTrait = it.habilidadeVinculada,
                    cost = it.custo
                )
            },
            spells = npc.feiticos.map {
                ExchangeSpell(
                    canonicalId = canonicalIdForName(it.nome, setOf(PreparedEncounterCatalog.NS_SPELL)),
                    name = it.nome,
                    circle = it.circulo,
                    cost = it.custo,
                    initial = it.nome == npc.feiticoInicialNome
                )
            },
            martialStyleIds = buildList {
                npc.estiloArtesMarciais.takeIf(String::isNotBlank)?.let(::add)
                addAll(npc.estilosArtesMarciaisAdicionais.filter(String::isNotBlank))
            }.distinct(),
            spiritForms = listOf(npc.formaEspiritual, npc.formaEspiritualSecundaria)
                .filter(String::isNotBlank),
            xp = ExchangeXp(npc.xpAtual, npc.xpGastoTotal, npc.primeiroXpRecebido),
            combat = ExchangeCombat(
                mainAction = npc.acaoPrincipal,
                decisiveAction = npc.acaoDecisiva,
                primaryDefense = npc.defesaPrimaria,
                dodge = npc.esquiva,
                soak = npc.absorcao,
                hardness = npc.dureza,
                resolve = npc.perseveranca,
                guile = npc.astucia,
                joinBattle = npc.juntarABatalha,
                currentInitiative = npc.iniciativaAtual,
                damage = npc.dano
            )
        )
    }
}
