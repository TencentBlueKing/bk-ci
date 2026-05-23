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
     * 解密SM4或者AES，AES解密时按当前密钥、历史密钥顺序依次尝试。
     */
    fun decryptSm4OrAes(aesKey: String, usedAesKeys: List<String>, content: String): String {
        return decryptSm4OrAesByKeys(keys = listOf(aesKey) + usedAesKeys, content = content)
    }

    /**
     * 仅用于AES key刷新场景的解密。
     *
     * AES解密时优先尝试历史密钥，普通解密场景应使用[decryptSm4OrAes]。
     */
    fun decryptSm4OrAesForRefresh(aesKey: String, usedAesKeys: List<String>, content: String): String {
        return decryptSm4OrAesByKeys(keys = usedAesKeys + aesKey, content = content)
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
     * 解密SM4或者AES字节数据，AES解密时按当前密钥、历史密钥顺序依次尝试。
     */
    fun decryptSm4OrAes(aesKey: String, usedAesKeys: List<String>, content: ByteArray): ByteArray {
        return decryptSm4OrAesByKeys(keys = listOf(aesKey) + usedAesKeys, content = content)
    }

    /**
     * 仅用于AES key刷新场景的字节数据解密。
     *
     * AES解密时优先尝试历史密钥，普通解密场景应使用[decryptSm4OrAes]。
     */
    fun decryptSm4OrAesForRefresh(aesKey: String, usedAesKeys: List<String>, content: ByteArray): ByteArray {
        return decryptSm4OrAesByKeys(keys = usedAesKeys + aesKey, content = content)
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

    /**
     * 使用多个AES密钥依次尝试解密字符串密文。
     */
    private fun decryptSm4OrAesByKeys(keys: List<String>, content: String): String {
        if (content.startsWith(SM4_CRYPTO.stringCipherPrefix)) {
            return SM4_CRYPTO.decrypt(sm4Key(), content)
        }
        var lastError: Throwable? = null
        keys.filter { it.isNotBlank() }.distinct().forEach { key ->
            try {
                return AESUtil.decrypt(key, content)
            } catch (ignored: Throwable) {
                lastError = ignored
            }
        }
        throw lastError ?: IllegalArgumentException("No available aes key")
    }

    /**
     * 使用多个AES密钥依次尝试解密字节密文。
     */
    private fun decryptSm4OrAesByKeys(keys: List<String>, content: ByteArray): ByteArray {
        if (content.toString(UTF8).startsWith(SM4_CRYPTO.stringCipherPrefix)) {
            return SM4_CRYPTO.decrypt(sm4Key().toByteArray(UTF8), content)
        }
        var lastError: Throwable? = null
        keys.filter { it.isNotBlank() }.distinct().forEach { key ->
            try {
                return AESUtil.decrypt(key, content)
            } catch (ignored: Throwable) {
                lastError = ignored
            }
        }
        throw lastError ?: IllegalArgumentException("No available aes key")
    }

    private fun isSm4Enabled() = SpringContextUtil.getValue("sm4.enabled") == "true"

    private fun sm4Key() = SpringContextUtil.getValue("sm4.key") ?: "b*SnKm#3%t4"
}
