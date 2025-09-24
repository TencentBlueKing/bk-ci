package com.tencent.devops.remotedev.utils

import org.slf4j.LoggerFactory

/**
 * Token加密工具类
 * 实现奇偶位对换加密算法
 */
object TokenEncryptUtil {

    private val logger = LoggerFactory.getLogger(TokenEncryptUtil::class.java)

    /**
     * 对Token进行奇偶位对换加密
     *
     * @param originalToken 原始token字符串
     * @return 加密后的token字符串
     */
    fun encryptToken(originalToken: String): String {
        if (!validateTokenFormat(originalToken)) {
            logger.warn("Invalid token format for encryption")
            return originalToken
        }

        try {
            val chars = originalToken.toCharArray()
            val length = chars.size

            // 奇偶位对换：奇数位和偶数位字符对换
            for (i in 0 until length - 1 step 2) {
                val temp = chars[i]
                chars[i] = chars[i + 1]
                chars[i + 1] = temp
            }

            val encryptedToken = String(chars)
            logger.debug("Token encrypted successfully, original length: $length")
            return encryptedToken
        } catch (e: Exception) {
            logger.error("Failed to encrypt token", e)
            throw e
        }
    }

    /**
     * 验证Token格式是否有效
     *
     * @param token 待验证的token字符串
     * @return token格式是否有效
     */
    fun validateTokenFormat(token: String): Boolean {
        return token.isNotEmpty()
    }
}
