package ai.orhan.data.auth

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Reversible fake (byte reversal) using the real stored format; no Keystore needed. */
private class FakeTokenCrypto(var failEncrypt: Boolean = false) : TokenCrypto {
    var encryptCalls = 0
    var decryptCalls = 0
    override fun encrypt(plain: String): String {
        encryptCalls++
        if (failEncrypt) throw IllegalStateException("keystore unavailable")
        return TokenFormat.encode(byteArrayOf(1, 2, 3), plain.toByteArray().reversedArray())
    }
    override fun decrypt(stored: String): String? {
        decryptCalls++
        return TokenFormat.decryptWith(stored) { iv, ct ->
            require(iv.contentEquals(byteArrayOf(1, 2, 3))); ct.reversedArray()
        }
    }
}

class TokenCryptoTest {
    @Test fun formatRoundTrip() {
        val s = TokenFormat.encode(byteArrayOf(9, 8), byteArrayOf(7, 6, 5))
        assertTrue(s.startsWith("enc:v1:"))
        val (iv, ct) = TokenFormat.decode(s)!!
        assertArrayEquals(byteArrayOf(9, 8), iv)
        assertArrayEquals(byteArrayOf(7, 6, 5), ct)
    }

    @Test fun malformedDecodesToNull() {
        assertNull(TokenFormat.decode("enc:v1:"))
        assertNull(TokenFormat.decode("enc:v1:abc"))
        assertNull(TokenFormat.decode("enc:v1:!!!:###"))
    }

    @Test fun legacyPlaintextPassesThrough() {
        assertFalse(TokenFormat.isEncrypted("eyJhbGciOi"))
        assertEquals("eyJhbGciOi", FakeTokenCrypto().decrypt("eyJhbGciOi"))
    }

    @Test fun decryptFailureReturnsNull() {
        assertNull(TokenFormat.decryptWith(TokenFormat.encode(byteArrayOf(1), byteArrayOf(2))) { _, _ -> error("bad tag") })
    }

    @Test fun sealThenLoadRoundTrips() {
        val c = FakeTokenCrypto()
        val a = sealToken(c, "access"); val r = sealToken(c, "refresh")
        assertTrue(TokenFormat.isEncrypted(a) && TokenFormat.isEncrypted(r))
        assertFalse(a.contains("access"))
        assertEquals(AuthSession("access", "refresh", 42L, "e@x"), sessionFromStored(c, a, r, 42L, "e@x"))
        assertEquals(2, c.encryptCalls); assertEquals(2, c.decryptCalls)
    }

    @Test fun legacySessionStillLoads() {
        assertEquals(AuthSession("a", "r", 0L, null), sessionFromStored(FakeTokenCrypto(), "a", "r", null, null))
    }

    @Test fun undecryptableTokenMeansSignedOut() {
        val bad = TokenFormat.encode(byteArrayOf(9), byteArrayOf(1))
        assertNull(sessionFromStored(FakeTokenCrypto(), bad, "r", 1L, null))
        assertNull(sessionFromStored(FakeTokenCrypto(), "a", "enc:v1:junk", 1L, null))
    }

    @Test fun encryptFailureFallsBackWithoutThrowing() {
        assertEquals("tok", sealToken(FakeTokenCrypto(failEncrypt = true), "tok"))
    }
}
