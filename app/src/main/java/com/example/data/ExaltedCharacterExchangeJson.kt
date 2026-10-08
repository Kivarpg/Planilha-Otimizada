package com.example.data

import org.json.JSONArray
import org.json.JSONObject

internal object ExaltedCharacterExchangeJson {
    fun encode(exchange: ExaltedCharacterExchange): String = JSONObject().apply {
        put("schemaVersion", exchange.schemaVersion)
        put("id", exchange.id)
        put("name", exchange.name)
        put("exaltType", exchange.exaltType)
        put("archetype", exchange.archetype)
        put("caste", exchange.caste)
        put("essence", exchange.essence)
        put("attributes", JSONObject(exchange.attributes))
        put("abilities", JSONObject(exchange.abilities))
        put("charms", JSONArray().apply {
            exchange.charms.forEach { charm ->
                put(JSONObject().apply {
                    put("canonicalId", charm.canonicalId ?: JSONObject.NULL)
                    put("name", charm.name)
                    put("linkedTrait", charm.linkedTrait)
                    put("cost", charm.cost)
                })
            }
        })
        put("spells", JSONArray().apply {
            exchange.spells.forEach { spell ->
                put(JSONObject().apply {
                    put("canonicalId", spell.canonicalId ?: JSONObject.NULL)
                    put("name", spell.name)
                    put("circle", spell.circle)
                    put("cost", spell.cost)
                    put("initial", spell.initial)
                })
            }
        })
        put("martialStyleIds", JSONArray(exchange.martialStyleIds))
        put("spiritForms", JSONArray(exchange.spiritForms))
        put("xp", JSONObject().apply {
            put("current", exchange.xp.current)
            put("spent", exchange.xp.spent)
            put("firstXpReceived", exchange.xp.firstXpReceived)
        })
        put("combat", JSONObject().apply {
            put("mainAction", exchange.combat.mainAction)
            put("decisiveAction", exchange.combat.decisiveAction)
            put("primaryDefense", exchange.combat.primaryDefense ?: JSONObject.NULL)
            put("dodge", exchange.combat.dodge)
            put("soak", exchange.combat.soak)
            put("hardness", exchange.combat.hardness)
            put("resolve", exchange.combat.resolve)
            put("guile", exchange.combat.guile)
            put("joinBattle", exchange.combat.joinBattle)
            put("currentInitiative", exchange.combat.currentInitiative)
            put("damage", exchange.combat.damage)
        })
    }.toString()
}
