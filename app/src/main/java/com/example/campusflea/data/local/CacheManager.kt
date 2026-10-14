package com.example.campusflea.data.local

import android.content.Context
import coil.Coil
import coil.imageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object CacheManager {

    suspend fun getTotalCacheSize(context: Context): Long = withContext(Dispatchers.IO) {
        getDirSize(context.cacheDir)
    }

    suspend fun clearAll(context: Context) = withContext(Dispatchers.IO) {
        Coil.imageLoader(context).diskCache?.clear()
        clearDirectory(context.cacheDir)
    }

    fun formatSize(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
        else -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
    }

    private fun getDirSize(file: File): Long {
        if (file.isFile) return file.length()
        return file.listFiles()?.sumOf { getDirSize(it) } ?: 0L
    }

    private fun clearDirectory(dir: File) {
        dir.listFiles()?.forEach { child ->
            if (child.isDirectory) clearDirectory(child)
            child.delete()
        }
    }
}
