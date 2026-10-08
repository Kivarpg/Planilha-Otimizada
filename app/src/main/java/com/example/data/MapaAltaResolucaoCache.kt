package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Visualizador em tiles do mapa HD.
 *
 * O arquivo original (5780x3740) permanece intacto no APK. Em vez de
 * decodificá-lo inteiro para a RAM, somente as regiões atualmente visíveis
 * são decodificadas em tiles de 512x512. Um cache LRU limita a quantidade de
 * pixels HD residentes simultaneamente.
 *
 * Em zoom baixo, o MapaInterativo usa o mapa_creation (fallback leve). A
 * resolução original passa a ser usada progressivamente conforme o usuário
 * aproxima o mapa, sem alterar o arquivo fonte nem perder detalhes.
 */
object MapaAltaResolucaoCache {
    data class TileKey(val x: Int, val y: Int)

    private const val MAP_WIDTH = 5780
    private const val MAP_HEIGHT = 3740
    const val TILE_SIZE = 512
    private const val MAX_TILES = 32
    const val ZOOM_MIN_HD = 1.35f

    private val tiles: SnapshotStateMap<TileKey, ImageBitmap> = mutableStateMapOf()
    private val lru = object : LinkedHashMap<TileKey, ImageBitmap>(MAX_TILES, 0.75f, true) {}
    private val loading = mutableSetOf<TileKey>()
    private var decoder: BitmapRegionDecoder? = null
    private var decoderContextPackage: String? = null

    val mapaLargura: Int get() = MAP_WIDTH
    val mapaAltura: Int get() = MAP_HEIGHT

    fun tile(key: TileKey): ImageBitmap? = tiles[key]

    /**
     * Solicita os tiles necessários. A lista é deduplicada e limitada ao
     * conjunto visível calculado pela UI. Cada tile é carregado em IO e só
     * então publicado no estado do Compose.
     */
    suspend fun carregarTiles(context: Context, keys: List<TileKey>) {
        val faltantes = synchronized(this) {
            keys.distinct().filter { key ->
                !tiles.containsKey(key) && loading.add(key)
            }
        }
        if (faltantes.isEmpty()) return

        val resultados = try {
            withContext(Dispatchers.IO) {
                faltantes.mapNotNull { key ->
                    try {
                        val bitmap = decodificarTile(context.applicationContext, key)
                        if (bitmap != null) key to bitmap.asImageBitmap() else null
                    } catch (_: OutOfMemoryError) {
                        null
                    } catch (_: Exception) {
                        null
                    } finally {
                        synchronized(this@MapaAltaResolucaoCache) { loading.remove(key) }
                    }
                }
            }
        } catch (_: kotlinx.coroutines.CancellationException) {
            synchronized(this) { faltantes.forEach(loading::remove) }
            throw kotlinx.coroutines.CancellationException()
        }

        val pinned = keys.toSet()
        resultados.forEach { (key, image) ->
            synchronized(this) {
                lru[key] = image
                tiles[key] = image
                while (lru.size > MAX_TILES) {
                    val oldest = lru.entries.firstOrNull { it.key !in pinned }?.key ?: break
                    lru.remove(oldest)
                    tiles.remove(oldest)
                }
            }
        }
        synchronized(this) {
            faltantes.filterNot { key -> resultados.any { it.first == key } }
                .forEach(loading::remove)
        }
    }

    @Suppress("DEPRECATION")
    private fun decodificarTile(context: Context, key: TileKey): Bitmap? {
        if (key.x < 0 || key.y < 0) return null
        val left = key.x * TILE_SIZE
        val top = key.y * TILE_SIZE
        if (left >= MAP_WIDTH || top >= MAP_HEIGHT) return null
        val right = minOf(left + TILE_SIZE, MAP_WIDTH)
        val bottom = minOf(top + TILE_SIZE, MAP_HEIGHT)

        val d = synchronized(this) {
            if (decoder == null || decoderContextPackage != context.packageName) {
                decoder?.recycle()
                val resId = context.resources.getIdentifier(
                    "mapa_creation_hd", "drawable", context.packageName
                )
                if (resId == 0) return@synchronized null
                decoder = BitmapRegionDecoder.newInstance(
                    context.resources.openRawResource(resId), false
                )
                decoderContextPackage = context.packageName
            }
            decoder
        } ?: return null

        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        return synchronized(d) {
            d.decodeRegion(android.graphics.Rect(left, top, right, bottom), options)
        }
    }

    fun limpar() {
        synchronized(this) {
            tiles.clear()
            lru.clear()
            loading.clear()
            decoder?.recycle()
            decoder = null
            decoderContextPackage = null
        }
    }
}
