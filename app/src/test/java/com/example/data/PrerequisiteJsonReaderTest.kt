package com.example.data

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PrerequisiteJsonReaderTest {
    @Test
    fun acceptsLegacyString() {
        val json = JSONObject().put("pre_requisitos", "Encanto A, Encanto B")
        assertEquals("Encanto A, Encanto B", json.optPrerequisitosJson())
    }

    @Test
    fun acceptsFutureStringArrayWithoutDroppingReferences() {
        val json = JSONObject().put(
            "pre_requisitos",
            JSONArray().put("Encanto A").put("Uma Arma, Dois Golpes")
        )
        assertEquals("Encanto A; Uma Arma, Dois Golpes", json.optPrerequisitosJson())
    }
    @Test
    fun acceptsCommonCamelCaseAliases() {
        val json = JSONObject().put("preRequisitos", JSONArray().put("Encanto A"))
        assertEquals("Encanto A", json.optPrerequisitosJson())
    }

}
