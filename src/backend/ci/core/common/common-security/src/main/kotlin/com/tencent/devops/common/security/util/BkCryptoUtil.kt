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

    /**
     * 加密SM4(没有开启则使用AES)
     */
    fun encryptSm4ButAes(aesKey: String, content: String): String {
        return encryptSm4ButOther(content) {
            AESUtil.encrypt(aesKey, it)
        }
    }

    /**
     * 加密SM4(没有开启则使用AES)
     */
    fun encryptSm4ButAes(aesKey: String, content: ByteArray): ByteArray {
        return encryptSm4ButOther(content) {
            AESUtil.encrypt(aesKey, it)
        }
    }

    /**
     * 解密SM4或者AES
     */
    fun decryptSm4OrAes(aesKey: String, content: String): String {
        return decryptSm4OrOther(content) {
            AESUtil.decrypt(aesKey, it)
        }
    }

    /**
     * 解密SM4或者AES
     */
    fun decryptSm4OrAes(aesKey: String, content: ByteArray): ByteArray {
        return decryptSm4OrOther(content) {
            AESUtil.decrypt(aesKey, it)
        }
    }

    /**
     * 加密SM4(没有开启则不加密)
     */
    fun encryptSm4ButNone(content: String): String {
        return encryptSm4ButOther(content) { it }
    }

    /**
     * 解密SM4或者非加密
     */
    fun decryptSm4orNone(content: String): String {
        return decryptSm4OrOther(content) { it }
    }

    /**
     * 加密SM4(没有开启则使用其他算法)
     */
    fun encryptSm4ButOther(content: String, other: (String) -> String): String {
        return if (isSm4Enabled()) {
            SM4_CRYPTO.encrypt(sm4Key(), content)
        } else {
            other(content)
        }
    }

    /**
     * 加密SM4(没有开启则使用其他算法)
     */
    private fun encryptSm4ButOther(content: ByteArray, other: (ByteArray) -> ByteArray): ByteArray {
        return if (isSm4Enabled()) {
            SM4_CRYPTO.encrypt(sm4Key().toByteArray(UTF8), content)
        } else {
            other(content)
        }
    }

    /**
     * 解密SM4或者其他
     */
    fun decryptSm4OrOther(content: String, other: (String) -> String): String {
        return if (content.startsWith(SM4_CRYPTO.stringCipherPrefix)) {
            SM4_CRYPTO.decrypt(sm4Key(), content)
        } else {
            other(content)
        }
    }

    /**
     * 解密SM4或者其他
     */
    private fun decryptSm4OrOther(content: ByteArray, other: (ByteArray) -> ByteArray): ByteArray {
        return if (content.toString(UTF8).startsWith(SM4_CRYPTO.stringCipherPrefix)) {
            SM4_CRYPTO.decrypt(sm4Key().toByteArray(UTF8), content)
        } else {
            other(content)
        }
    }

    private fun isSm4Enabled() = SpringContextUtil.getValue("sm4.enabled") == "true"

    private fun sm4Key() = SpringContextUtil.getValue("sm4.key") ?: "b*SnKm#3%t4"
}
