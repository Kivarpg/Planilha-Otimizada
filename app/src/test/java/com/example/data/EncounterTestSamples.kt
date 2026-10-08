package com.example.data

internal object EncounterTestSamples {
    private const val FAST_SAMPLE_LIMIT = 1

    fun count(fullCount: Int): Int =
        if (System.getProperty("exalted.fullAudit") == "true") fullCount
        else minOf(fullCount, FAST_SAMPLE_LIMIT)
}
