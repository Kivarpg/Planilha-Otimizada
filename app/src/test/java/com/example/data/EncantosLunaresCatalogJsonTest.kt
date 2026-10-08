package com.example.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class EncantosLunaresCatalogJsonTest {
    private fun catalog(): List<JSONObject> {
        val path = System.getProperty("user.dir").orEmpty() + "/src/main/assets/encantos_lunares.json"
        val root = JSONObject(File(path).readText(Charsets.UTF_8))
        val array = root.getJSONArray("encantos")
        return (0 until array.length()).map { array.getJSONObject(it) }
    }

    @Test
    fun tecnicaDaPenaAgulhaEhEncantoIndependenteEReferenciadaPeloSeguinte() {
        val charms = catalog()
        val tecnica = charms.filter { it.optString("nome") == "Técnica da Pena Agulha" }
        assertEquals(1, tecnica.size)
        val def = tecnica.single()
        assertEquals("Needle Quill Technique", def.optString("nome_ingles"))
        assertEquals("1m", def.optString("custo"))
        assertEquals("Destreza 2, Essência 1", def.optString("mins"))
        assertEquals("Reflexivo", def.optString("tipo"))
        assertEquals("Nenhuma", def.optString("palavras_chave"))
        assertEquals("Instantânea", def.optString("duracao"))
        assertEquals("Nenhum", def.optString("pre_requisitos"))

        val panoplia = charms.single { it.optString("nome") == "Panóplia de Guerreiros de Muitos Braços" }
        val panopliaText = panoplia.getJSONArray("conteudo").toString()
        assertFalse(panopliaText.contains("Técnica da Pena Agulha"))

        val following = charms.single { it.optString("nome") == "Alquimia do Escarro de Bombardeiro" }
        assertEquals("Técnica da Pena Agulha", following.optString("pre_requisitos"))
        assertNotNull(def.getJSONArray("conteudo"))
        assertTrue(def.getJSONArray("conteudo").length() >= 2)
    }
}
