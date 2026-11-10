package com.tencent.devops.auth.crypto

import com.tencent.devops.common.api.util.ShaUtils
import com.tencent.devops.common.security.util.BkCryptoUtil
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class Oauth2AccessTokenCryptoHelper {
    @Value("\${aes.auth:}")
    private var aesKey: String = ""

    @Value("\${aes.used-auth-keys:}")
    private var usedAesKeys: String = ""

    fun currentKeySha(): String = ShaUtils.sha256Fingerprint(aesKey)

    fun encryptSm4ButAes(content: String): String {
        return BkCryptoUtil.encryptSm4ButAes(aesKey = aesKey, content = content)
    }

    /**
     * 密钥轮换时使用：优先用历史密钥解密旧数据，再用当前密钥重新加密。
     */
    fun refreshSm4OrAes(content: String): String {
        return BkCryptoUtil.encryptSm4ButAes(
            aesKey = aesKey,
            content = BkCryptoUtil.decryptSm4OrAesForRefresh(
                aesKey = aesKey,
                usedAesKeys = BkCryptoUtil.parseAesKeys(usedAesKeys),
                content = content
            )
        )
    }
}
