package com.tencent.devops.store.common.crypto

import com.tencent.devops.common.api.util.AESUtil
import com.tencent.devops.common.api.util.ShaUtils
import com.tencent.devops.common.security.util.BkCryptoUtil
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class StoreCryptoHelper {
    @Value("\${aes.aesKey}")
    private lateinit var aesKey: String

    @Value("\${aes.usedAesKeys:}")
    private var usedAesKeys: List<String> = emptyList()

    fun currentKeySha(): String = ShaUtils.sha256Fingerprint(aesKey)

    fun encryptSm4ButAes(content: String): String = BkCryptoUtil.encryptSm4ButAes(aesKey, content)

    fun decryptSm4OrAes(content: String): String {
        return BkCryptoUtil.decryptSm4OrAes(aesKey = aesKey, usedAesKeys = usedAesKeys, content = content)
    }

    /**
     * 密钥轮换时使用：优先用历史密钥解密旧数据，再用当前密钥重新加密。
     */
    fun refreshSm4OrAes(content: String): String {
        return BkCryptoUtil.encryptSm4ButAes(
            aesKey,
            BkCryptoUtil.decryptSm4OrAesForRefresh(aesKey = aesKey, usedAesKeys = usedAesKeys, content = content)
        )
    }

    fun encryptAes(content: String): String = AESUtil.encrypt(aesKey, content)

    fun decryptAes(content: String): String = decryptAesByKeys(keys = listOf(aesKey) + usedAesKeys, content = content)

    /**
     * 仅用于历史 AES 数据的密钥轮换，优先用历史密钥解密后再用当前密钥加密。
     */
    fun refreshAes(content: String): String {
        return AESUtil.encrypt(aesKey, decryptAesByKeys(keys = usedAesKeys + aesKey, content = content))
    }

    private fun decryptAesByKeys(keys: List<String>, content: String): String {
        var lastError: Throwable? = null
        keys.distinct().forEach { key ->
            try {
                return AESUtil.decrypt(key, content)
            } catch (ignored: Throwable) {
                lastError = ignored
            }
        }
        throw lastError ?: IllegalArgumentException("No available aes key")
    }
}
