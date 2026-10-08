package com.zenx.yugen.play.data.crypto

import android.util.Base64
import java.net.URLEncoder
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object VrfEngine {

    fun encrypt(input: String, operations: List<Triple<Int, String, List<String>>>): String {
        var vrf = input
        operations.forEach { item ->
            when (item.second) {
                "exchange" -> vrf = exchange(vrf, item.third)
                "rc4" -> vrf = rc4Encrypt(item.third[0], vrf)
                "reverse" -> vrf = vrf.reversed()
                "base64" -> vrf = Base64.encode(vrf.toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP).toString(Charsets.UTF_8).trimEnd('=')
            }
        }
        return URLEncoder.encode(vrf, "utf-8")
    }

    fun rc4Encrypt(key: String, input: String): String {
        val rc4Key = SecretKeySpec(key.toByteArray(), "RC4")
        val cipher = Cipher.getInstance("RC4")
        cipher.init(Cipher.ENCRYPT_MODE, rc4Key, cipher.parameters)
        val output = cipher.doFinal(input.toByteArray())
        return Base64.encode(output, Base64.URL_SAFE or Base64.NO_WRAP).toString(Charsets.UTF_8).trimEnd('=')
    }

    fun exchange(input: String, keys: List<String>): String {
        if (keys.size < 2) return input
        val key1 = keys[0]
        val key2 = keys[1]
        return input.map { i ->
            val idx = key1.indexOf(i)
            if (idx != -1 && idx < key2.length) key2[idx] else i
        }.joinToString("")
    }
}
