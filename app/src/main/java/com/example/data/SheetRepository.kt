package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import com.example.iniciativas.IniciativasState
import com.example.model.CharacterSheet
import com.example.model.Npc
import com.example.model.RatingStyle

/**
 * Application-facing persistence facade.
 *
 * Storage concerns are split into small stores so changes to one persisted
 * domain do not require editing this class or unrelated serialization code.
 */
class SheetRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val preferences = SheetPreferencesStore(prefs)
    private val backups = BackupStore(prefs)
    private val sheets = SheetIndexStore(prefs)
    private val npcs = NpcStore(prefs)
    private val encounterNpcRepository = SharedPreferencesEncounterNpcRepository(
        prefs = prefs,
        onFailure = { logErroInterno("Falha na persistência dos NPCs de Encontro (Aba 11)", it) },
    )
    private val initiatives = InitiativesStore(prefs)
    private val historicoCombates = HistoricoCombateStore(prefs)

    private val backupPersistence = BackupPersistenceCoordinator(
        write = { sheet -> backups.save(CharacterSheetJsonCodec.encode(sheet)) },
        onFailure = { logErroInterno("Falha na persistência assíncrona do backup periódico", it) }
    )

    private val sheetPersistence = SheetPersistenceCoordinator(
        write = { sheet ->
            sheets.saveActive(sheet)
            sheets.saveToIndex(
                sheet,
                onItemError = { logErroInterno("Falha ao carregar uma planilha do índice", it) },
                onIndexError = { logErroInterno("Falha ao ler o índice de planilhas salvas", it) }
            )
        },
        onFailure = { logErroInterno("Falha na persistência assíncrona da planilha ativa", it) }
    )

    data class BackupSnapshot(val timestamp: String, val json: String)

    fun proximoNumeroSequencial(): Int = preferences.nextSequence()

    fun salvarBackupPeriodico(sheet: CharacterSheet) {
        backupPersistence.submit(sheet)
    }

    fun listarBackups(): List<BackupSnapshot> = backups.load().map { BackupSnapshot(it.timestamp, it.json) }

    fun salvarNpcs(npcs: List<Npc>) = this.npcs.save(npcs)

    fun carregarNpcs(): List<Npc> = this.npcs.load { logErroInterno("Falha ao carregar NPCs manuais (Aba 9)", it) }

    fun salvarNpcsEncontro(npcs: List<com.example.model.NpcEncontro>) =
        encounterNpcRepository.save(npcs)

    fun carregarNpcsEncontro(): List<com.example.model.NpcEncontro> =
        encounterNpcRepository.load()

    fun salvarIniciativas(state: IniciativasState) = initiatives.save(state)

    fun carregarIniciativas(): IniciativasState =
        initiatives.load { logErroInterno("Falha ao carregar o estado da Aba 12 (Conflito)", it) }

    fun carregarHistoricoCombates(): List<com.example.iniciativas.HistoricoCombateEntry> = historicoCombates.carregar()

    fun adicionarHistoricoCombate(entry: com.example.iniciativas.HistoricoCombateEntry): List<com.example.iniciativas.HistoricoCombateEntry> =
        historicoCombates.adicionar(entry)

    fun getRatingStyle(): RatingStyle = preferences.ratingStyle()

    fun setRatingStyle(style: RatingStyle) = preferences.setRatingStyle(style)

    fun loadActiveSheet(): CharacterSheet {
        val loaded = sheetPersistence.latestPending()
            ?: sheets.loadActive { logErroInterno("Falha ao carregar a planilha ativa", it) }
        val migrated = migrateLegacyBattleGroups(loaded, allowGlobalLegacy = true)
        if (migrated != loaded) persistSheet(migrated)
        return migrated
    }

    fun saveActiveSheet(sheet: CharacterSheet) {
        // Enfileira em vez de executar a serialização/gravação no thread que
        // chamou o ViewModel. O coordinator roda em Dispatchers.IO e mantém
        // somente o snapshot mais recente pendente.
        sheetPersistence.submit(sheet)
    }

    private fun persistSheet(sheet: CharacterSheet) {
        sheetPersistence.submit(sheet)
    }

    private fun migrateLegacyBattleGroups(sheet: CharacterSheet, allowGlobalLegacy: Boolean): CharacterSheet {
        if (sheet.battleGroups.isNotEmpty()) return sheet

        val bySheetRaw = prefs.getString(LEGACY_BY_SHEET_KEY, null)
        if (!bySheetRaw.isNullOrBlank()) {
            runCatching {
                val root = org.json.JSONObject(bySheetRaw)
                val encoded = root.optString(sheet.id, "")
                if (encoded.isNotBlank()) {
                    val groups = decodeLegacyBattleGroups(encoded)
                    if (groups.isNotEmpty()) {
                        root.remove(sheet.id)
                        prefs.edit().putString(LEGACY_BY_SHEET_KEY, root.toString()).apply()
                        return sheet.copy(battleGroups = groups)
                    }
                }
            }.onFailure { logErroInterno("Falha na migração dos Battle Groups legados", Exception(it)) }
        }

        if (allowGlobalLegacy) {
            val legacyRaw = prefs.getString(LEGACY_GLOBAL_KEY, null)
            if (!legacyRaw.isNullOrBlank()) {
                runCatching {
                    val groups = decodeLegacyBattleGroups(legacyRaw)
                    if (groups.isNotEmpty()) {
                        prefs.edit().remove(LEGACY_GLOBAL_KEY).apply()
                        return sheet.copy(battleGroups = groups)
                    }
                }.onFailure { logErroInterno("Falha na migração dos Battle Groups globais legados", Exception(it)) }
            }
        }
        return sheet
    }

    private fun decodeLegacyBattleGroups(json: String): List<com.example.model.BattleGroup> {
        val root = org.json.JSONArray(json)
        return buildList {
            for (i in 0 until root.length()) {
                val o = root.getJSONObject(i)
                val custom = o.optJSONObject("custom")?.let { c ->
                    val attacks = c.optJSONArray("attacks")?.let { arr -> buildList {
                        for (j in 0 until arr.length()) {
                            val a = arr.getJSONObject(j)
                            add(com.example.model.BattleGroupCustomAttack(
                                a.optString("name"), a.optInt("attackBase"), a.optInt("damageBase"),
                                if (a.isNull("minimumDice")) null else a.optInt("minimumDice")
                            ))
                        }
                    } } ?: emptyList()
                    com.example.model.BattleGroupCustomStats(
                        c.optInt("joinBattle"), attacks, c.optInt("defenseBase"), c.optInt("magnitudeBase"),
                        c.optInt("soakBase"), c.optInt("senses"), c.optInt("resolve"), c.optString("resist"),
                        if (c.isNull("routDifficulty")) null else c.optInt("routDifficulty"), c.optBoolean("perfectMorale")
                    )
                }
                add(com.example.model.BattleGroup(
                    id = o.optString("id"), name = o.optString("name"),
                    troopTypeName = if (o.isNull("troopTypeName")) null else o.optString("troopTypeName", "").ifBlank { null },
                    size = o.optInt("size", 1).coerceIn(1, 5),
                    drill = runCatching { com.example.model.BattleGroupDrill.valueOf(o.optString("drill", "AVERAGE")) }.getOrDefault(com.example.model.BattleGroupDrill.POOR),
                    might = o.optInt("might", 0).coerceIn(0, 3), customStats = custom
                ))
            }
        }
    }


    fun getAllSheets(): List<CharacterSheet> {
        val active = loadActiveSheet()
        val pendingActive = sheetPersistence.latestPending()
        val persistedSheets = sheets.loadAll(
            onItemError = { logErroInterno("Falha ao carregar uma planilha do índice", it) },
            onIndexError = { logErroInterno("Falha ao ler o índice de planilhas salvas", it) }
        )
        val allSheets = if (pendingActive == null) persistedSheets else {
            val replaced = persistedSheets.map { if (it.id == pendingActive.id) pendingActive else it }.toMutableList()
            if (replaced.none { it.id == pendingActive.id }) replaced.add(pendingActive)
            replaced
        }
        return allSheets.map { sheet ->
            val migrated = if (sheet.id == active.id) active else migrateLegacyBattleGroups(sheet, allowGlobalLegacy = false)
            if (migrated != sheet) {
                sheets.saveToIndex(
                    migrated,
                    onItemError = { logErroInterno("Falha ao carregar uma planilha do índice", it) },
                    onIndexError = { logErroInterno("Falha ao ler o índice de planilhas salvas", it) }
                )
            }
            migrated
        }
    }

    fun deleteSheet(sheetId: String) {
        val remaining = sheets.delete(
            sheetId,
            onItemError = { logErroInterno("Falha ao carregar uma planilha do índice", it) },
            onIndexError = { logErroInterno("Falha ao ler o índice de planilhas salvas", it) }
        )
        if (loadActiveSheet().id == sheetId) {
            val replacement = remaining.firstOrNull() ?: CharacterSheet()
            sheets.saveActive(replacement)
            if (remaining.isEmpty()) {
                sheets.saveToIndex(
                    replacement,
                    onItemError = { logErroInterno("Falha ao carregar uma planilha do índice", it) },
                    onIndexError = { logErroInterno("Falha ao ler o índice de planilhas salvas", it) }
                )
            }
        }
    }

    fun close() {
        sheetPersistence.close()
        backupPersistence.close()
        encounterNpcRepository.close()
    }

    private fun logErroInterno(contexto: String, e: Exception) {
        if (BuildConfig.DEBUG) {
            android.util.Log.w(TAG, contexto, e)
        }
    }

    private companion object {
        const val TAG = "SheetRepository"
        const val PREFS_NAME = "exalted_prefs"
        const val LEGACY_GLOBAL_KEY = "exalted_battle_groups"
        const val LEGACY_BY_SHEET_KEY = "exalted_battle_groups_by_sheet"
    }
}
