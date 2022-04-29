package com.tencent.devops.dispatch.bcs.utils

import org.bouncycastle.util.encoders.Hex
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object ShaUtils {

    fun sha1(str: ByteArray): String {
        // 指定sha1算法
        val digest = MessageDigest.getInstance("SHA-1")
        digest.update(str)
        // 获取字节数组
        val messageDigest = digest.digest()

        // 字节数组转换为 十六进制 数
        return messageDigest.toHexString()
    }

    private fun ByteArray.toHexString() = joinToString("") { String.format("%02x", it) }

    fun hmacSha1(key: ByteArray, data: ByteArray): String {
        val secretKey = SecretKeySpec(key, "HmacSHA1")
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(secretKey)
        val messageDigest = mac.doFinal(data)

        // 字节数组转换为 十六进制 数
        return messageDigest.toHexString()
    }

    fun isEqual(shaA: String, shaB: String): Boolean {
        return isEqual(Hex.decode(shaA), Hex.decode(shaB))
    }

    fun isEqual(shaA: ByteArray, shaB: ByteArray): Boolean {
        return MessageDigest.isEqual(shaA, shaB)
    }

    fun sha256(str: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(str.toByteArray()).toHexString()
    }
}
