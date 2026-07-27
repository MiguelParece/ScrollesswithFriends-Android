/*
 * Copyright (C) 2026 Scrolless
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.scrolless.app.accessibility.debug

import android.accessibilityservice.AccessibilityService
import android.content.ContentValues
import android.graphics.Rect
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.RequiresApi
import java.io.File
import java.util.Locale
import timber.log.Timber

/**
 * Debug-only dump of the accessibility tree of whatever is currently on screen.
 *
 * Detection rules are written against view IDs and content descriptions that only the
 * real app can tell us — they differ between app versions and locales — so this exists to
 * capture them from a device, and stays around as the re-capture tool for when an app
 * renames things underneath us.
 */
internal object AccessibilityTreeDumper {

    private const val MAX_NODES = 3_000
    private const val MAX_DEPTH = 60
    private const val LOG_CHUNK_CHARS = 3_500
    private const val TAG = "TreeDump"
    private const val DOWNLOADS_SUBFOLDER = "Scrolless"

    /** @property location where the dump landed, for the confirmation toast; null when writing failed. */
    data class DumpResult(val nodeCount: Int, val truncated: Boolean, val location: String?)

    fun dump(service: AccessibilityService, label: String, nowMillis: Long): DumpResult {
        val builder = StringBuilder()
        var nodeCount = 0
        var truncated = false

        for (window in service.windows) {
            val root = window.root ?: continue
            if (root.packageName?.toString() == service.packageName) continue // our own overlays

            builder.appendLine(header(service, label, nowMillis, root, window.type))

            val stack = ArrayDeque<Pair<AccessibilityNodeInfo, Int>>()
            stack.addLast(root to 0)
            while (stack.isNotEmpty()) {
                if (nodeCount >= MAX_NODES) {
                    truncated = true
                    break
                }
                val (node, depth) = stack.removeLast()
                builder.appendLine(describe(node, depth))
                nodeCount++

                if (depth < MAX_DEPTH) {
                    for (index in node.childCount - 1 downTo 0) {
                        node.getChild(index)?.let { stack.addLast(it to depth + 1) }
                    }
                }
            }
            builder.appendLine()
        }

        val text = builder.toString()
        logChunked(text)
        val location = writeDump(service, label, nowMillis, text)

        if (truncated) Timber.tag(TAG).w("Dump truncated at %d nodes", MAX_NODES)
        return DumpResult(nodeCount = nodeCount, truncated = truncated, location = location)
    }

    /** The locale matters: a detection rule based on a content description only holds for it. */
    private fun header(
        service: AccessibilityService,
        label: String,
        nowMillis: Long,
        root: AccessibilityNodeInfo,
        windowType: Int,
    ): String {
        val metrics = service.resources.displayMetrics
        return buildString {
            appendLine("=== label=$label ts=$nowMillis pkg=${root.packageName} win=$windowType")
            appendLine("=== screen=${metrics.widthPixels}x${metrics.heightPixels} locale=${Locale.getDefault()}")
        }
    }

    private fun describe(node: AccessibilityNodeInfo, depth: Int): String {
        val bounds = Rect().also(node::getBoundsInScreen)
        return buildString {
            append("  ".repeat(depth))
            append("d=$depth")
            append(" id=${node.viewIdResourceName ?: "-"}")
            append(" cls=${node.className ?: "-"}")
            append(" vis=${node.isVisibleToUser}")
            append(" sel=${node.isSelected}")
            append(" clk=${node.isClickable}")
            append(" bounds=[${bounds.left},${bounds.top},${bounds.right},${bounds.bottom}]")
            append(" text=\"${node.text ?: ""}\"")
            append(" desc=\"${node.contentDescription ?: ""}\"")
        }
    }

    /** Logcat drops entries beyond ~4 kB, so long dumps are split into numbered chunks. */
    private fun logChunked(text: String) {
        val chunks = text.chunked(LOG_CHUNK_CHARS)
        chunks.forEachIndexed { index, chunk ->
            Timber.tag(TAG).i("[%d/%d]\n%s", index + 1, chunks.size, chunk)
        }
    }

    /**
     * Writes to the shared Downloads folder, which any file manager can reach — unlike
     * `Android/data`, which recent Android versions hide from the user. Needs no
     * permission on API 29+; older versions would, so they keep the app-private folder.
     */
    private fun writeDump(service: AccessibilityService, label: String, nowMillis: Long, text: String): String? {
        val fileName = "tree-$label-$nowMillis.txt"
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            writeToDownloads(service, fileName, text)
        } else {
            writeToAppFolder(service, fileName, text)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun writeToDownloads(service: AccessibilityService, fileName: String, text: String): String? = try {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "text/plain")
            put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$DOWNLOADS_SUBFOLDER")
        }
        val resolver = service.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error("MediaStore refused the insert")
        resolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) } ?: error("No output stream for $uri")
        "Download/$DOWNLOADS_SUBFOLDER/$fileName"
    } catch (e: Exception) {
        Timber.tag(TAG).e(e, "Failed to write dump to Downloads, falling back to app folder")
        writeToAppFolder(service, fileName, text)
    }

    private fun writeToAppFolder(service: AccessibilityService, fileName: String, text: String): String? = try {
        val directory = File(service.getExternalFilesDir(null), "inspector").apply { mkdirs() }
        File(directory, fileName).apply { writeText(text) }.absolutePath
    } catch (e: Exception) {
        Timber.tag(TAG).e(e, "Failed to write dump file")
        null
    }
}
