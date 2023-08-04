package com.tencent.devops.notify.tencentcloud.utils

import com.tencent.devops.notify.tencentcloud.pojo.TencentCloudSignatureConfig
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone
import java.util.TreeMap
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.xml.bind.DatatypeConverter

object TencentCloudSignatureUtil {
    private val UTF8 = StandardCharsets.UTF_8
    private const val CT_JSON = "application/json; charset=utf-8"

    @Throws(Exception::class)
    fun hmac256(key: ByteArray?, msg: String): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        val secretKeySpec = SecretKeySpec(key, mac.algorithm)
        mac.init(secretKeySpec)
        return mac.doFinal(msg.toByteArray(UTF8))
    }

    @Throws(Exception::class)
    fun sha256Hex(s: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val d = md.digest(s.toByteArray(UTF8))
        return DatatypeConverter.printHexBinary(d).toLowerCase()
    }

    @Throws(Exception::class)
    fun signature(config: TencentCloudSignatureConfig): Map<String, String> {
        val service = config.service
        val host = config.host
        val region = config.region
        val action = config.action
        val version = config.version
        val algorithm = config.algorithm
        val timestamp = System.currentTimeMillis() / 1000

        val sdf = SimpleDateFormat("yyyy-MM-dd")
        // 注意时区，否则容易出错
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        val date = sdf.format(Date(timestamp * 1000))

        // ************* 步骤 1：拼接规范请求串 *************
        val httpRequestMethod = config.httpRequestMethod
        val canonicalUri = "/"
        val canonicalQueryString = ""
        val canonicalHeaders = "content-type:application/json; charset=utf-8\nhost:$host\n"
        val signedHeaders = "content-type;host"
        val hashedRequestPayload = sha256Hex(config.payload)
        val canonicalRequest = "$httpRequestMethod\n$canonicalUri\n$canonicalQueryString\n" +
            "$canonicalHeaders\n$signedHeaders\n$hashedRequestPayload"

        // ************* 步骤 2：拼接待签名字符串 *************
        val credentialScope = "$date/$service/tc3_request"
        val hashedCanonicalRequest = sha256Hex(canonicalRequest)
        val stringToSign = """
            $algorithm
            $timestamp
            $credentialScope
            $hashedCanonicalRequest
            """.trimIndent()

        // ************* 步骤 3：计算签名 *************
        val secretDate = hmac256(("TC3${config.secretKey}").toByteArray(UTF8), date)
        val secretService = hmac256(secretDate, service)
        val secretSigning = hmac256(secretService, "tc3_request")
        val signature = DatatypeConverter.printHexBinary(hmac256(secretSigning, stringToSign)).toLowerCase()

        // ************* 步骤 4：拼接 Authorization *************
        val authorization = (algorithm + " " + "Credential=" + config.secretId + "/" + credentialScope + ", " +
            "SignedHeaders=" + signedHeaders + ", " + "Signature=" + signature)
        val headers = TreeMap<String, String>()
        headers["Authorization"] = authorization
        headers["Content-Type"] = CT_JSON
        headers["Host"] = host
        headers["X-TC-Action"] = action
        headers["X-TC-Timestamp"] = timestamp.toString()
        headers["X-TC-Version"] = version
        headers["X-TC-Region"] = region
        return headers
    }
}
