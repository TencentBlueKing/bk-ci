package com.tencent.bkrepo.maven.util

import java.math.BigInteger
import java.security.MessageDigest

object DigestUtils {
    private val sha512Digest = MessageDigest.getInstance("SHA-512")

    fun sha512(buffer: ByteArray, offset: Int, length: Int): String {
        // MessageDigest是thread unsafe的，解决concurrent时会报java.lang.ArrayIndexOutOfBoundsException: null
        synchronized(sha512Digest) {
            sha512Digest.update(buffer, offset, length)
            return hexToString(sha512Digest.digest(), 128)
        }
    }

    private fun hexToString(byteArray: ByteArray, length: Int): String {
        val hashInt = BigInteger(1, byteArray)
        val hashText = hashInt.toString(16)
        return if (hashText.length < length) "0".repeat(length - hashText.length) + hashText else hashText
    }
}
