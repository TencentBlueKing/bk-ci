package com.tencent.devops.common.notify

import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.DESKeySpec
import javax.crypto.spec.IvParameterSpec

object DesUtil {

    fun encrypt(message: String, key: String): ByteArray {
        // 密钥只能是8位
        val newKey = getKey(key)
        val cipher = Cipher.getInstance("DES/CBC/PKCS5Padding")

        val desKeySpec = DESKeySpec(newKey.toByteArray(charset("UTF-8")))

        val keyFactory = SecretKeyFactory.getInstance("DES")
        val secretKey = keyFactory.generateSecret(desKeySpec)
        val iv = IvParameterSpec(newKey.toByteArray(charset("UTF-8")))
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv)

        return cipher.doFinal(message.toByteArray(charset("UTF-8")))
    }

    fun toHexString(b: ByteArray): String {
        val hexString = StringBuilder()
        for (aB in b) {
            var plainText = Integer.toHexString(0xff and aB.toInt())
            if (plainText.length < 2)
                plainText = "0" + plainText
            hexString.append(plainText)
        }

        return hexString.toString()
    }

    private fun getKey(key: String): String {
        val tmp = "--------"
        return (key + tmp).substring(0, 8)
    }
}
