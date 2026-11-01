package ai.orhan.data.auth

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encrypts auth tokens at rest. Implementations return the stored form from [encrypt]
 * and the plaintext from [decrypt]. [decrypt] must return legacy (unprefixed) values as-is
 * and return null (never throw) when an encrypted value can't be decrypted.
 */
interface TokenCrypto {
    fun encrypt(plain: String): String
    fun decrypt(stored: String): String?
}

/** Pure helpers for the `enc:v1:<b64 iv>:<b64 ciphertext>` stored format. */
object TokenFormat {
    const val PREFIX = "enc:v1:"

    fun isEncrypted(stored: String): Boolean = stored.startsWith(PREFIX)

    fun encode(iv: ByteArray, ciphertext: ByteArray): String =
        PREFIX + Base64.getEncoder().encodeToString(iv) + ":" + Base64.getEncoder().encodeToString(ciphertext)

    /** Returns (iv, ciphertext) or null if malformed. Only call for [isEncrypted] values. */
    fun decode(stored: String): Pair<ByteArray, ByteArray>? = runCatching {
        val body = stored.removePrefix(PREFIX)
        val sep = body.indexOf(':')
        require(sep > 0 && sep < body.length - 1)
        val iv = Base64.getDecoder().decode(body.substring(0, sep))
        val ct = Base64.getDecoder().decode(body.substring(sep + 1))
        require(iv.isNotEmpty() && ct.isNotEmpty())
        iv to ct
    }.getOrNull()

    /**
     * Shared decrypt logic: legacy plaintext passes through; prefixed values are parsed and
     * handed to [open]; any failure yields null.
     */
    inline fun decryptWith(stored: String, open: (iv: ByteArray, ct: ByteArray) -> ByteArray): String? {
        if (!isEncrypted(stored)) return stored
        val (iv, ct) = decode(stored) ?: return null
        return runCatching { String(open(iv, ct), Charsets.UTF_8) }.getOrNull()
    }
}

/** Production implementation: AndroidKeyStore AES-256-GCM key, alias [ALIAS]. */
class KeystoreTokenCrypto : TokenCrypto {
    private fun key(): SecretKey {
        val ks = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        gen.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return gen.generateKey()
    }

    override fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        return TokenFormat.encode(cipher.iv, cipher.doFinal(plain.toByteArray(Charsets.UTF_8)))
    }

    override fun decrypt(stored: String): String? = TokenFormat.decryptWith(stored) { iv, ct ->
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
        cipher.doFinal(ct)
    }

    companion object {
        const val ALIAS = "orhan_auth_aes_key"
        private const val KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
