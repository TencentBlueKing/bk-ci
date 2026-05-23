package com.tencent.devops.process.crypto

import com.tencent.devops.common.api.util.AESUtil
import com.tencent.devops.common.api.util.ShaUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class ProcessCryptoHelper {
    @Value("\${project.callback.aes-key}")
    private lateinit var aesKey: String

    @Value("\${project.callback.used-aes-keys:}")
    private var usedAesKeys: List<String> = emptyList()

    fun currentKeySha(): String = ShaUtils.sha256Fingerprint(aesKey)

    fun encrypt(content: String): String = AESUtil.encrypt(aesKey, content)

    fun decrypt(content: String): String = decryptByKeys(keys = listOf(aesKey) + usedAesKeys, content = content)

    fun refresh(content: String): String {
        return AESUtil.encrypt(aesKey, decryptByKeys(keys = usedAesKeys + aesKey, content = content))
    }

    private fun decryptByKeys(keys: List<String>, content: String): String {
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
}
