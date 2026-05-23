package com.tencent.devops.process.crypto

import com.tencent.devops.common.api.util.ShaUtils
import com.tencent.devops.common.security.util.BkCryptoUtil
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class PipelineCallbackCryptoHelper {
    @Value("\${project.callback.aes-key}")
    private lateinit var aesKey: String

    @Value("\${project.callback.used-aes-keys:}")
    private var usedAesKeys: String = ""

    fun currentKeySha(): String = ShaUtils.sha256Fingerprint(aesKey)

    fun encryptSm4ButAes(content: String): String = BkCryptoUtil.encryptSm4ButAes(aesKey, content)

    fun decryptSm4OrAes(content: String): String {
        return BkCryptoUtil.decryptSm4OrAes(
            aesKey = aesKey,
            usedAesKeys = BkCryptoUtil.parseAesKeys(usedAesKeys),
            content = content
        )
    }

    /**
     * 密钥轮换时使用：优先用历史密钥解密旧数据，再用当前密钥重新加密。
     */
    fun refreshSm4OrAes(content: String): String {
        return BkCryptoUtil.encryptSm4ButAes(
            aesKey,
            BkCryptoUtil.decryptSm4OrAesForRefresh(aesKey, BkCryptoUtil.parseAesKeys(usedAesKeys), content)
        )
    }
}
