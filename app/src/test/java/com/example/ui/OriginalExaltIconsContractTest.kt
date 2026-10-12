package com.example.ui

import java.io.File
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals

/** User-supplied icon artwork must never be replaced by generated previews. */
class OriginalExaltIconsContractTest {
    @Test fun `all fourteen supplied caste and aspect images retain original bytes`() {
        val originals = mapOf(
            "src/main/res/drawable/caste_dawn.webp" to "4df10c3f49b0e65c8d317774bdacf5e8fa2c0bf2ca97a43a961d93530d52baa8",
            "src/main/res/drawable/caste_zenith.webp" to "4afa0c4d3ad45e5fdd396712a5324e80f54af0acf176239a4c718fee018b1fee",
            "src/main/res/drawable/caste_twilight.webp" to "c4c0c12f90d7a1efc63faf555cc627ab8eb4810cf56ae1b6c1e63acff85cc214",
            "src/main/res/drawable/caste_night.webp" to "c5abc7d8cc1811cad0d67393b9a5680dc828d2b523985988c61e8efa1944e9ef",
            "src/main/res/drawable/caste_eclipse.webp" to "f25f3bc41b512a3dbeb23cae444bee691157e010427e313b80f386a421e6135a",
            "src/main/res/drawable-nodpi/aspecto_ar.webp" to "c2db64315517cff4d8fccb8b52a5ccf10f113de4a6532e93cfe30eb6786d3fd1",
            "src/main/res/drawable-nodpi/aspecto_terra.webp" to "b27ca7e1e8eef796cd5e1a50c7273c9ce579a366c53b1606d6639da22b7d8f71",
            "src/main/res/drawable-nodpi/aspecto_fogo.webp" to "158658afe0faf06678c77bcf770fb941d1439a60dc43138c66076231a3e3f42f",
            "src/main/res/drawable-nodpi/aspecto_agua.webp" to "6160fc1047b46d6984696d1beda6269cee6237bee9d5a7820a2156cd329e1c76",
            "src/main/res/drawable-nodpi/aspecto_madeira.webp" to "9b080a9dba268c17c8048511d3a642e1118e0659c42242be3db043180df11e29",
            "src/main/res/drawable-nodpi/caste_lua_cheia.webp" to "3ff149aafd4582a8299da723c5bdcea6a81277f54b03cd1e8309cf4895e3108c",
            "src/main/res/drawable-nodpi/caste_lua_minguante.webp" to "c36fbb3fa9b380d543d17f3e74a8bd419e1b11b187c872c44993018a5285ae35",
            "src/main/res/drawable-nodpi/caste_lua_nova.webp" to "b5dd9c265381593260d186732c4cfe76ae34dff516aecf11d8071f485ae85548",
            "src/main/res/drawable-nodpi/caste_sem_casta.webp" to "c69e5dfb600d855f703d8db142408a7d525fe7a42df6b0d97ea5fa7b4aeffefd"
        )
        originals.forEach { (path, expected) ->
            val actual = MessageDigest.getInstance("SHA-256").digest(File(path).readBytes())
                .joinToString("") { "%02x".format(it.toInt() and 0xff) }
            assertEquals(expected, actual, "Original artwork changed: $path")
        }
    }
}
