package com.koscribe.prototype

import android.content.Context
import android.content.pm.PackageManager
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/** Device-local credential vault. Call from a worker thread; never log exceptions or key material. */
class ApiKeyStore(context: Context) {
    private val app = context.applicationContext
    private val baseFile = File(app.noBackupFilesDir, FILE_NAME)
    private val encryptedFile = AtomicFile(baseFile)

    /** Includes damaged or incomplete entries. Use this before any controlled fake-key test. */
    fun hasStoredEntry(): Boolean = synchronized(lock) {
        try {
            baseFile.exists() || File(baseFile.path + ".new").exists() ||
                File(baseFile.path + ".bak").exists() || keyStore().containsAlias(ALIAS)
        } catch (_: Exception) { true }
    }

    fun hasKey(): Boolean = synchronized(lock) {
        val plain = decrypt() ?: return@synchronized false
        try {
            valid(String(plain, Charsets.UTF_8))
        } finally {
            plain.fill(0)
        }
    }

    /** Returns the credential only to the caller's memory; null means missing or unusable. */
    fun readKey(): String? = synchronized(lock) {
        val plain = decrypt() ?: return@synchronized null
        try {
            String(plain, Charsets.UTF_8).takeIf { valid(it) }
        } finally {
            plain.fill(0)
        }
    }

    /** Replaces the encrypted file atomically. An invalid Keystore entry must be deleted explicitly. */
    fun saveKey(value: String): Boolean = synchronized(lock) {
        if (!valid(value)) return@synchronized false
        if (hasStoredEntry()) {
            val existing = decrypt() ?: return@synchronized false
            try {
                if (!valid(String(existing, Charsets.UTF_8))) return@synchronized false
            } finally {
                existing.fill(0)
            }
        }
        val normalized = value.trim()
        val plain = normalized.toByteArray(Charsets.UTF_8)
        try {
            if (plain.size > MAX_KEY_BYTES) return@synchronized false
            val key = getOrCreateKey() ?: return@synchronized false
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv
            if (iv.size != IV_BYTES) return@synchronized false
            val ciphertext = cipher.doFinal(plain)
            val payload = MAGIC + iv + ciphertext
            var stream: java.io.FileOutputStream? = null
            try {
                stream = encryptedFile.startWrite()
                stream.write(payload)
                stream.fd.sync()
                encryptedFile.finishWrite(stream)
                stream = null
                val stored = decrypt() ?: return@synchronized false
                try { stored.contentEquals(plain) } finally { stored.fill(0) }
            } catch (_: Exception) {
                if (stream != null) encryptedFile.failWrite(stream)
                false
            } finally {
                ciphertext.fill(0)
                payload.fill(0)
            }
        } catch (_: Exception) {
            false
        } finally {
            plain.fill(0)
        }
    }

    /** Removes ciphertext before deleting the alias. Repeating this operation is safe. */
    fun deleteKey(): Boolean = synchronized(lock) {
        try { encryptedFile.delete() } catch (_: Exception) { /* Continue with alias removal. */ }
        val aliasGone = try {
            val store = keyStore()
            if (store.containsAlias(ALIAS)) store.deleteEntry(ALIAS)
            !store.containsAlias(ALIAS)
        } catch (_: Exception) {
            false
        }
        val filesGone = try {
            !baseFile.exists() && !File(baseFile.path + ".new").exists() &&
                !File(baseFile.path + ".bak").exists()
        } catch (_: Exception) { false }
        filesGone && aliasGone
    }

    private fun decrypt(): ByteArray? {
        try {
            val key = existingKey() ?: return null
            val bytes = encryptedFile.openRead().use { stream ->
                val buffer = ByteArray(MAX_BLOB_BYTES + 1)
                var count = 0
                while (count < buffer.size) {
                    val n = stream.read(buffer, count, buffer.size - count)
                    if (n < 0) break
                    count += n
                }
                if (count > MAX_BLOB_BYTES) return null
                buffer.copyOf(count)
            }
            try {
                if (bytes.size < MAGIC.size + IV_BYTES + TAG_BYTES ||
                    !bytes.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)) return null
                val cipher = Cipher.getInstance(TRANSFORMATION)
                val iv = bytes.copyOfRange(MAGIC.size, MAGIC.size + IV_BYTES)
                cipher.init(Cipher.DECRYPT_MODE, key, javax.crypto.spec.GCMParameterSpec(128, iv))
                val result = cipher.doFinal(bytes, MAGIC.size + IV_BYTES,
                    bytes.size - MAGIC.size - IV_BYTES)
                if (result.isEmpty() || result.size > MAX_KEY_BYTES) {
                    result.fill(0)
                    return null
                }
                return result
            } finally {
                bytes.fill(0)
            }
        } catch (_: Exception) {
            return null
        }
    }

    private fun existingKey(): SecretKey? = keyStore().getKey(ALIAS, null) as? SecretKey

    private fun getOrCreateKey(): SecretKey? {
        existingKey()?.let { return it }
        if (keyStore().containsAlias(ALIAS)) return null
        val strongBoxAvailable = app.packageManager.hasSystemFeature(
            PackageManager.FEATURE_STRONGBOX_KEYSTORE)
        if (strongBoxAvailable) {
            try { return generateKey(strongBox = true) } catch (_: Exception) {
                // Some devices report the feature but cannot provision this particular key.
                if (keyStore().containsAlias(ALIAS)) return null
            }
        }
        return generateKey(strongBox = false)
    }

    private fun generateKey(strongBox: Boolean): SecretKey {
        val spec = KeyGenParameterSpec.Builder(ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .setIsStrongBoxBacked(strongBox)
            .build()
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            .apply { init(spec) }.generateKey()
    }

    private fun keyStore(): KeyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    companion object {
        private val lock = Any()
        private const val ALIAS = "KoScribeApiCredentialV1"
        private const val FILE_NAME = "api_credential.v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private val MAGIC = byteArrayOf(0x4b, 0x53, 0x43, 0x01)
        private const val IV_BYTES = 12
        private const val TAG_BYTES = 16
        private const val MAX_KEY_BYTES = 4096
        private const val MAX_BLOB_BYTES = MAX_KEY_BYTES + 64

        /** Check before trimming, so a trailing newline cannot become an HTTP header value. */
        internal fun valid(value: String): Boolean {
            if (value.length > MAX_KEY_BYTES || value.isEmpty()) return false
            if (value.any { it.code < 0x20 || it.code in 0x7f..0x9f }) return false
            return value.trim().isNotEmpty()
        }
    }
}
