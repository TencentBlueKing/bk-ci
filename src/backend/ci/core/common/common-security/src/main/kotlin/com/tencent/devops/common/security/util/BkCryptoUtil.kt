package com.tencent.devops.common.security.util

import com.tencent.bk.sdk.crypto.cryptor.SymmetricCryptorFactory
import com.tencent.bk.sdk.crypto.cryptor.consts.CryptorNames
import com.tencent.devops.common.api.util.AESUtil
import com.tencent.devops.common.service.utils.SpringContextUtil

/**
 * 加解密算法工具
 */
object BkCryptoUtil {
    /**
     * 加密SM4(没有开启则使用AES)
     */
    fun encryptSm4ButAes(sm4Key: String, aesKey: String, content: String): String {
        val sm4Enabled = SpringContextUtil.getValue("sm4.enabled") == "true"
        return if (sm4Enabled) {
            SymmetricCryptorFactory.getCryptor(CryptorNames.SM4).encrypt(sm4Key, content)
        } else {
            AESUtil.encrypt(aesKey, content)
        }
    }

    /**
     * 解密SM4或者AES
     */
    fun decryptSm4OrAes(sm4Key: String, aesKey: String, content: String): String {
        val crypto = SymmetricCryptorFactory.getCryptor(CryptorNames.SM4)
        return if (content.startsWith(crypto.stringCipherPrefix)) {
            crypto.decrypt(sm4Key, content)
        } else {
            AESUtil.decrypt(aesKey, content)
        }
    }

    /**
     * 加密SM4(没有开启则不加密)
     */
    fun encryptSm4ButNone(sm4Key: String, content: String): String {
        val sm4Enabled = SpringContextUtil.getValue("sm4.enabled") == "true"
        return if (sm4Enabled) {
            SymmetricCryptorFactory.getCryptor(CryptorNames.SM4).encrypt(sm4Key, content)
        } else {
            content
        }
    }

    /**
     * 解密SM4或者非加密
     */
    fun decryptSm4orNone(sm4Key: String, content: String): String {
        val crypto = SymmetricCryptorFactory.getCryptor(CryptorNames.SM4)
        return if (content.startsWith(crypto.stringCipherPrefix)) {
            crypto.decrypt(sm4Key, content)
        } else {
            content
        }
    }
}
