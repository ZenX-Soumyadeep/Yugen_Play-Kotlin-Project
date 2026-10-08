package com.zenx.yugen.play.data.crypto

import android.util.Base64
import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object AesCipher {

    fun decryptCbc(encryptedBase64: String, key: String, iv: String): String? {
        return try {
            val keyBytes = ByteArray(32)
            val keySrc = key.toByteArray(StandardCharsets.UTF_8)
            System.arraycopy(keySrc, 0, keyBytes, 0, keySrc.size.coerceAtMost(32))

            val ivBytes = iv.toByteArray(StandardCharsets.UTF_8)
            val encrypted = Base64.decode(
                encryptedBase64.replace('-', '+').replace('_', '/'),
                Base64.DEFAULT
            )

            if (encrypted.isNotEmpty() && encrypted.size % 16 == 0) {
                val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                cipher.init(
                    Cipher.DECRYPT_MODE,
                    SecretKeySpec(keyBytes, "AES"),
                    IvParameterSpec(ivBytes)
                )
                val decrypted = cipher.doFinal(encrypted)
                String(decrypted, StandardCharsets.UTF_8)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}
