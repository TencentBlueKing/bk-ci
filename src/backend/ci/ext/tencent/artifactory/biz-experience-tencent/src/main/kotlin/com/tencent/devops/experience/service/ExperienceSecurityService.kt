package com.tencent.devops.experience.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.math.BigInteger
import java.security.MessageDigest

@Service
class ExperienceSecurityService {
    @Value("\${tuxiaochao.product.key:#{null}}")
    val productKey: String? = null

    fun getClearSign(
        openId: String,
        nickName: String,
        avatar: String
    ): String {
        val content = openId.plus(nickName).plus(avatar).plus(productKey!!)
        val md = MessageDigest.getInstance("MD5")
        val bytes = md.digest(content.toByteArray())
        val bigInt = BigInteger(1, bytes)
        val md5 = bigInt.toString(HEXADECIMAL)
        return md5.padStart(THIRTY_TWO_CHARACTER_STRING, '0')
    }

    companion object {
        private const val HEXADECIMAL = 16
        private const val THIRTY_TWO_CHARACTER_STRING = 32
    }
}
