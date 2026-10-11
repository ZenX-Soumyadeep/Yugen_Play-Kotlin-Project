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
            var cleanBase64 = encryptedBase64.replace('-', '+').replace('_', '/')
            while (cleanBase64.length % 4 != 0) {
                cleanBase64 += "="
            }
            val encrypted = try {
                Base64.decode(cleanBase64, Base64.DEFAULT)
            } catch (_: Exception) {
                try {
                    Base64.decode(encryptedBase64, Base64.URL_SAFE or Base64.NO_WRAP)
                } catch (_: Exception) {
                    ByteArray(0)
                }
            }

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
