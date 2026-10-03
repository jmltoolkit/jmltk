/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.vcg

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties
import java.util.concurrent.ConcurrentHashMap

/**
 * Storage for previously computed solver answers of verification-condition queries.
 *
 * The keys are content hashes (see [VcgFacade.contentKey]): two verification runs
 * whose queries send the exact same content to the SMT solver share an entry, no
 * matter how the individual conditions are named.
 *
 * Implementations must be thread-safe: [VcgFacade.check] is invoked concurrently
 * from the facade's [java.util.concurrent.ForkJoinPool].
 */
interface VcgCache {
    /** The stored per-condition statuses for [key], or `null` when absent. */
    fun get(key: String): List<VcgResult.Status>?

    /** Remembers [statuses] (one entry per condition, in emission order) for [key]. */
    fun put(key: String, statuses: List<VcgResult.Status>)

    /** Number of cached queries. */
    val size: Int

    /** Re-reads the persisted state (no-op for in-memory caches). */
    fun load() {}

    /** Writes the state to the backing store (no-op for in-memory caches). */
    fun save() {}
}

/** A process-local [VcgCache] that is never persisted. */
class InMemoryVcgCache : VcgCache {
    private val entries = ConcurrentHashMap<String, List<VcgResult.Status>>()

    override fun get(key: String): List<VcgResult.Status>? = entries[key]

    override fun put(key: String, statuses: List<VcgResult.Status>) {
        entries[key] = statuses
    }

    override val size: Int
        get() = entries.size
}

/**
 * A [VcgCache] persisted as a [java.util.Properties] file: one entry per query, the
 * value being the comma-separated per-condition statuses in emission order.
 *
 * [load] is invoked by the caller (e.g. before starting a verification run) and
 * [save] afterwards; both are no-ops when the file does not exist / is unreadable.
 */
class FileVcgCache(private val file: Path) : VcgCache {
    private val entries = ConcurrentHashMap<String, List<VcgResult.Status>>()

    override fun get(key: String): List<VcgResult.Status>? = entries[key]

    override fun put(key: String, statuses: List<VcgResult.Status>) {
        entries[key] = statuses
    }

    override val size: Int
        get() = entries.size

    override fun load() {
        if (!Files.exists(file)) return
        try {
            val props = Properties()
            Files.newInputStream(file).use { props.load(it) }
            entries.clear()
            for ((key, value) in props) {
                entries[key.toString()] = parse(value.toString())
            }
        } catch (e: IOException) {
            // a corrupt or half-written cache is not an error: start fresh
        }
    }

    override fun save() {
        try {
            file.toAbsolutePath().parent?.let { Files.createDirectories(it) }
            val props = Properties()
            for ((key, statuses) in entries) {
                props.setProperty(key, statuses.joinToString(",") { it.name })
            }
            Files.newOutputStream(file).use { props.store(it, "vcg solver-result cache (content-hash -> statuses)") }
        } catch (e: IOException) {
            // ignore: a read-only location must not abort a verification run
        }
    }

    private fun parse(value: String): List<VcgResult.Status> =
        if (value.isEmpty() || value == ",") {
            emptyList()
        } else {
            value.split(',').map { VcgResult.Status.valueOf(it) }
        }
}
