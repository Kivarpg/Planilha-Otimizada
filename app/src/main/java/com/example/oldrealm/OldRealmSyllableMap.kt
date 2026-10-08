package com.example.oldrealm

/** Canonical Old Realm syllable inventory used by this integration. */
object OldRealmSyllableMap {
    private val selectedGlyphIndex = mapOf(
        "A" to 1, "E" to 0, "I" to 3, "O" to 4, "U" to 5,
        "BA" to 19, "BE" to 20, "BI" to 21, "BO" to 23, "BU" to 24,
        "KA" to 29, "KE" to 31, "KI" to 32, "KO" to 28, "KU" to 34,
        "CHA" to 42, "CHE" to 43, "CHI" to 45, "CHO" to 44, "CHU" to 46,
        "DA" to 54, "DE" to 55, "DI" to 53, "DO" to 56, "DU" to 57,
        "FA" to 61, "FE" to 62, "FI" to 63, "FO" to 64, "FU" to 66,
        "GA" to 71, "GE" to 72, "GI" to 73, "GO" to 70, "GU" to 74,
        "HA" to 75, "HE" to 76, "HI" to 78, "HO" to 79, "HU" to 80,
        "LA" to 87, "LE" to 83, "LI" to 81, "LO" to 84, "LU" to 85,
        "MA" to 96, "ME" to 89, "MI" to 90, "MO" to 92, "MU" to 93,
        "NA" to 110, "NE" to 113, "NI" to 109, "NO" to 114, "NU" to 115,
        "PA" to 119, "PE" to 117, "PI" to 118, "PO" to 124, "PU" to 126,
        "RA" to 123, "RE" to 120, "RI" to 122, "RO" to 137, "RU" to 142,
        "SA" to 128, "SE" to 133, "SI" to 136, "SO" to 144, "SU" to 130,
        "TA" to 147, "TE" to 149, "TI" to 146, "TO" to 150, "TU" to 151,
        "WA" to 153, "WE" to 154, "WI" to 152, "WO" to 157, "WU" to 158,
        "XA" to 159, "XE" to 163, "XI" to 160, "XO" to 168, "XU" to 161,
        "YA" to 162, "YE" to 166, "YI" to 164, "YO" to 173, "YU" to 174,
    )

    val keys: Set<String> = selectedGlyphIndex.keys

    fun glyphString(syllable: String): String? =
        selectedGlyphIndex[syllable]?.let { String(Character.toChars(0xE000 + it)) }
}
