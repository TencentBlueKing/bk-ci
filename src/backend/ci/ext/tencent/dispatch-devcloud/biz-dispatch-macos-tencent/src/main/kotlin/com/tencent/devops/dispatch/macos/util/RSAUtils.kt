package com.tencent.devops.dispatch.macos.util

import org.jolokia.util.Base64Util
import java.io.ByteArrayOutputStream
import java.security.PrivateKey
import java.security.PublicKey
import javax.crypto.Cipher

object RSAUtils {
    private const val transformation = "RSA"
    private const val ENCRYPT_MAX_SIZE = 117
    private const val DECRYPT_MAX_SIZE = 256
    /**
     * 私钥加密
     */
    fun encryptByPrivateKey(str: String, privateKey: PrivateKey): String {
        val byteArray = str.toByteArray()
        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.ENCRYPT_MODE, privateKey)

        // 定义缓冲区
        var temp: ByteArray? = null
        // 当前偏移量
        var offset = 0

        val outputStream = ByteArrayOutputStream()

        while (byteArray.size - offset > 0) {
            // 剩余的部分大于最大加密字段，则加密117个字节的最大长度
            if (byteArray.size - offset >= ENCRYPT_MAX_SIZE) {
                temp = cipher.doFinal(byteArray, offset, ENCRYPT_MAX_SIZE)
                // 偏移量增加117
                offset += ENCRYPT_MAX_SIZE
            } else {
                // 如果剩余的字节数小于117，则加密剩余的全部
                temp = cipher.doFinal(byteArray, offset, (byteArray.size - offset))
                offset = byteArray.size
            }
            outputStream.write(temp)
        }
        outputStream.close()
        return Base64Util.encode(outputStream.toByteArray())
    }

    /**
     * 公钥加密
     */
    fun encryptByPublicKey(str: String, publicKey: PublicKey): String {
        val byteArray = str.toByteArray()
        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)

        var temp: ByteArray? = null
        var offset = 0

        val outputStream = ByteArrayOutputStream()

        while (byteArray.size - offset > 0) {
            if (byteArray.size - offset >= ENCRYPT_MAX_SIZE) {
                temp = cipher.doFinal(byteArray, offset, ENCRYPT_MAX_SIZE)
                offset += ENCRYPT_MAX_SIZE
            } else {
                temp = cipher.doFinal(byteArray, offset, (byteArray.size - offset))
                offset = byteArray.size
            }
            outputStream.write(temp)
        }

        outputStream.close()
        return Base64Util.encode(outputStream.toByteArray())
    }

    /**
     * 私钥解密
     * 注意Exception in thread "main" javax.crypto.IllegalBlockSizeException:
     * Data must not be longer than 256 bytes
     * 关于到底是128个字节还是256个，我也很迷糊了，我写成128的时候就报这个错误，改成256后就没事了
     */
    fun decryptByPrivateKey(str: String, privateKey: PrivateKey): String {
        val byteArray = Base64Util.decode(str)
        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.DECRYPT_MODE, privateKey)

        // 定义缓冲区
        var temp: ByteArray? = null
        // 当前偏移量
        var offset = 0

        val outputStream = ByteArrayOutputStream()

        while (byteArray.size - offset > 0) {
            // 剩余的部分大于最大解密字段，则加密限制的最大长度
            if (byteArray.size - offset >= DECRYPT_MAX_SIZE) {
                temp = cipher.doFinal(byteArray, offset, DECRYPT_MAX_SIZE)
                // 偏移量增加128
                offset += DECRYPT_MAX_SIZE
            } else {
                // 如果剩余的字节数小于最大长度，则解密剩余的全部
                temp = cipher.doFinal(byteArray, offset, (byteArray.size - offset))
                offset = byteArray.size
            }
            outputStream.write(temp)
        }
        outputStream.close()
        return String(outputStream.toByteArray())
    }

    /**
     * 公钥解密
     */
    fun decryptByPublicKey(str: String, publicKey: PublicKey): String {
        val byteArray = Base64Util.decode(str)
        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.DECRYPT_MODE, publicKey)

        var temp: ByteArray? = null
        var offset = 0

        val outputStream = ByteArrayOutputStream()

        while (byteArray.size - offset > 0) {
            if (byteArray.size - offset >= DECRYPT_MAX_SIZE) {
                temp = cipher.doFinal(byteArray, offset, DECRYPT_MAX_SIZE)
                offset += DECRYPT_MAX_SIZE
            } else {
                temp = cipher.doFinal(byteArray, offset, (byteArray.size - offset))
                offset = byteArray.size
            }
            outputStream.write(temp)
        }
        outputStream.close()
        return String(outputStream.toByteArray())
    }
}
