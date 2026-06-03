package com.sysupdate

object CryptoCore {
    init {
        System.loadLibrary("crypto_core")
    }

    @JvmStatic external fun encryptBytes(
        key: ByteArray, iv: ByteArray, data: ByteArray): ByteArray

    @JvmStatic external fun decryptBytes(
        key: ByteArray, iv: ByteArray, data: ByteArray): ByteArray

    @JvmStatic external fun generateSecureRandom(length: Int): ByteArray

    // ── Hex helpers ───────────────────────────────────────────────────────────
    fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    fun String.fromHex(): ByteArray {
        check(length % 2 == 0)
        return chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }
}
