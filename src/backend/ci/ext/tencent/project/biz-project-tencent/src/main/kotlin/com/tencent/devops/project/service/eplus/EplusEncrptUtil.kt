package com.tencent.devops.project.service.eplus

import com.tencent.devops.common.api.util.JsonUtil
import java.net.URLEncoder
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import javax.crypto.Cipher

object EplusEncrptUtil {

    private fun encodeURIComponentSafe(str: String): String {
        return URLEncoder.encode(str, "UTF-8")
            .replace("!", "%21")
            .replace("'", "%27")
            .replace("(", "%28")
            .replace(")", "%29")
            .replace("*", "%2A")
    }

    // 支持PEM格式的公钥加载
    private fun loadPublicKey(pemPublicKey: String): PublicKey {
        val publicKeyPEM = pemPublicKey
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("\\s+".toRegex(), "")

        val keyBytes = Base64.getDecoder().decode(publicKeyPEM)
        val keySpec = X509EncodedKeySpec(keyBytes)
        return KeyFactory.getInstance("RSA").generatePublic(keySpec)
    }

    data class JsonData(
        val nid: Int,
        val pid: Int,
        val user: String,
        val filter: List<Filter>
    )

    data class Filter(
        val col: String,
        val op: String,
        val `val`: String
    )

    fun encryptPanelToken(publicKey: String, jsonData: JsonData): String {
        // 1. JSON序列化
        val jsonString = JsonUtil.toJson(jsonData, false)

        // 2. RSA加密（强制使用2048位密钥长度）
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding").apply {
            init(Cipher.ENCRYPT_MODE, loadPublicKey(publicKey.trimIndent()))
        }
        val encrypted = cipher.doFinal(jsonString.toByteArray(Charsets.UTF_8))

        // 3. Base64编码（不换行，不添加结束符）
        val base64 = Base64.getEncoder().encodeToString(encrypted)

        // 4. 安全URI编码
        return encodeURIComponentSafe(base64)
    }
}
