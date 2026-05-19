package com.outfuseplayer.data.smb

import android.content.Context
import android.util.JsonReader
import android.util.JsonToken
import android.util.JsonWriter
import java.io.File
import kotlin.math.absoluteValue

class SmbScanIndexStore(context: Context) {
    private val filesDir = context.applicationContext.filesDir

    fun loadSignatures(sourceId: String, rootPath: String): Map<String, String> {
        val file = indexFile(sourceId, rootPath)
        if (!file.exists() || file.length() == 0L) return emptyMap()
        return runCatching {
            JsonReader(file.reader(Charsets.UTF_8)).use { reader ->
                buildMap {
                    reader.beginArray()
                    while (reader.hasNext()) {
                        var path = ""
                        var signature = ""
                        reader.beginObject()
                        while (reader.hasNext()) {
                            when (reader.nextName()) {
                                "path" -> path = reader.nextStringOrEmpty()
                                "signature" -> signature = reader.nextStringOrEmpty()
                                else -> reader.skipValue()
                            }
                        }
                        reader.endObject()
                        if (signature.isNotBlank()) put(path, signature)
                    }
                    reader.endArray()
                }
            }
        }.getOrDefault(emptyMap())
    }

    fun hasSignatures(sourceId: String, rootPath: String): Boolean {
        val file = indexFile(sourceId, rootPath)
        return file.exists() && file.length() > 0L
    }

    private fun JsonReader.nextStringOrEmpty(): String =
        if (peek() == JsonToken.NULL) {
            nextNull()
            ""
        } else {
            nextString()
        }

    fun saveSignatures(sourceId: String, rootPath: String, signatures: Map<String, String>) {
        val file = indexFile(sourceId, rootPath)
        val temp = File(file.parentFile, "${file.name}.tmp")
        JsonWriter(temp.writer(Charsets.UTF_8)).use { writer ->
            writer.beginArray()
            signatures.forEach { (path, signature) ->
                if (signature.isNotBlank()) {
                    writer.beginObject()
                    writer.name("path").value(path)
                    writer.name("signature").value(signature)
                    writer.endObject()
                }
            }
            writer.endArray()
        }
        if (!temp.renameTo(file)) {
            temp.copyTo(file, overwrite = true)
            temp.delete()
        }
    }

    private fun indexFile(sourceId: String, rootPath: String): File {
        val key = "$sourceId|${rootPath.toRemotePath()}"
        return File(filesDir, "smb_scan_index_${key.hashCode().absoluteValue}.json")
    }
}
