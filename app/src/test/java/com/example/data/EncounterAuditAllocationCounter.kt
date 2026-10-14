package com.example.data

/** JVM-only allocation instrumentation; -1 explicitly means unsupported. */
internal object EncounterAuditAllocationCounter {
    private val counter: (() -> Long)? = runCatching {
        val management = Class.forName("java.lang.management.ManagementFactory")
        val bean = management.getMethod("getThreadMXBean").invoke(null)
        val extended = Class.forName("com.sun.management.ThreadMXBean")
        check(extended.isInstance(bean))
        check(extended.getMethod("isThreadAllocatedMemorySupported").invoke(bean) == true)
        if (extended.getMethod("isThreadAllocatedMemoryEnabled").invoke(bean) != true) {
            extended.getMethod("setThreadAllocatedMemoryEnabled", Boolean::class.javaPrimitiveType).invoke(bean, true)
        }
        val read = extended.getMethod("getThreadAllocatedBytes", Long::class.javaPrimitiveType)
        val reader: () -> Long = { (read.invoke(bean, Thread.currentThread().id) as Number).toLong() }
        reader
    }.getOrNull()

    fun bytes(): Long = counter?.let { runCatching(it).getOrDefault(-1L) } ?: -1L
    fun delta(before: Long, after: Long): Long = if (before >= 0 && after >= before) after - before else -1L
}
