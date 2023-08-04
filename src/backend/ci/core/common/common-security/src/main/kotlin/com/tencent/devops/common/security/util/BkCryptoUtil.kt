package com.tencent.devops.common.security.util

import com.tencent.bk.sdk.crypto.cryptor.SymmetricCryptorFactory
import com.tencent.bk.sdk.crypto.cryptor.consts.CryptorNames
import com.tencent.devops.common.api.util.AESUtil
import com.tencent.devops.common.service.utils.SpringContextUtil

/**
 * 加解密算法工具
 */
object BkCryptoUtil {

    private val UTF8 = charset("UTF-8")
    private val SM4_CRYPTO = SymmetricCryptorFactory.getCryptor(CryptorNames.SM4)
    private val SM4_KEY = "b*SnKm#3%t4"

    /**
     * 加密SM4(没有开启则使用AES)
     */
    fun encryptSm4ButAes(sm4Key: String, aesKey: String, content: String): String {
        return if (isSm4Enabled()) {
            SM4_CRYPTO.encrypt(sm4Key, content)
        } else {
            AESUtil.encrypt(aesKey, content)
        }
    }

    /**
     * 加密SM4(没有开启则使用AES)
     */
    fun encryptSm4ButAes(sm4Key: String, aesKey: String, content: ByteArray): ByteArray? {
        return if (isSm4Enabled()) {
            SM4_CRYPTO.encrypt(sm4Key.toByteArray(UTF8), content)
        } else {
            AESUtil.encrypt(aesKey, content)
        }
    }

    /**
     * 解密SM4或者AES
     */
    fun decryptSm4OrAes(sm4Key: String, aesKey: String, content: String): String {
        return if (content.startsWith(SM4_CRYPTO.stringCipherPrefix)) {
            SM4_CRYPTO.decrypt(sm4Key, content)
        } else {
            AESUtil.decrypt(aesKey, content)
        }
    }

    /**
     * 解密SM4或者AES
     */
    fun decryptSm4OrAes(sm4Key: String, aesKey: String, content: ByteArray): ByteArray? {
        return if (content.toString(UTF8).startsWith(SM4_CRYPTO.stringCipherPrefix)) {
            SM4_CRYPTO.decrypt(sm4Key.toByteArray(UTF8), content)
        } else {
            AESUtil.decrypt(aesKey, content)
        }
    }

    /**
     * 加密SM4(没有开启则不加密)
     */
    fun encryptSm4ButNone(sm4Key: String, content: String): String {
        return if (isSm4Enabled()) {
            SM4_CRYPTO.encrypt(sm4Key, content)
        } else {
            content
        }
    }

    /**
     * 解密SM4或者非加密
     */
    fun decryptSm4orNone(sm4Key: String, content: String): String {
        return if (content.startsWith(SM4_CRYPTO.stringCipherPrefix)) {
            SM4_CRYPTO.decrypt(sm4Key, content)
        } else {
            content
        }
    }

    fun encryptSm4ButOther(content: String, other: (String) -> String): String {
        return if (isSm4Enabled()) {
            SM4_CRYPTO.encrypt(SM4_KEY, content)
        } else {
            other(content)
        }
    }

    fun decryptSm4ButOther(content: String, other: (String) -> String): String {
        return if (content.startsWith(SM4_CRYPTO.stringCipherPrefix)) {
            SM4_CRYPTO.decrypt(SM4_KEY, content)
        } else {
            other(content)
        }
    }

    private fun isSm4Enabled() = SpringContextUtil.getValue("sm4.enabled") == "true"
}
