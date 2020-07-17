package com.tencent.devops.sign.service.impl

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.AESUtil
import com.tencent.devops.common.api.util.DHUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.sign.Constants.KEYSTORE_CATEGORY_PROVISION
import com.tencent.devops.sign.Constants.KEYSTORE_HTTP_HEADER_AUTH
import com.tencent.devops.sign.Constants.KEYSTORE_HTTP_HEADER_IP
import com.tencent.devops.sign.api.constant.SignMessageCode
import com.tencent.devops.sign.service.MobileProvisionService
import com.tencent.devops.sign.utils.EncryptUtil
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import okhttp3.Request
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.io.File
import java.net.InetAddress
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.*


@Service
class MobileProvisionServiceImpl  @Autowired constructor(
) : MobileProvisionService {
    @Value("\${keystore.url:}")
    private val keyStoreUrl = "https://proxy.test.keystore.oa.com"

    @Value("\${keystore.certListUrl:}")
    private val keyStoreCertListUrl = "/api/auth/getCertList?appId=%s"

    @Value("\${keystore.certUrl:}")
    private val keyStoreCertUrl = "/api/auth/getCert?id=%s&category=%s"

    @Value("\${keystore.authId:}")
    private val keyStoreAuthId = "devops"

    @Value("\${keystore.authSecret:}")
    private val keyStoreAuthSecret = "a21c218df41f6d7fd032535fe20394e2"

    companion object {
        private val logger = LoggerFactory.getLogger(MobileProvisionServiceImpl::class.java)
        private val pairKey = DHUtil.initKey()
        private val privateKey = pairKey.privateKey
        private val publicKey = String(Base64.getEncoder().encode(pairKey.publicKey))
        private var token: String? = null

    }

    override fun downloadMobileProvision(mobileProvisionDir: File, projectId: String, mobileProvisionId: String): File {
        // 从keystore下载文件
        val mobileProvisionFile = File("${mobileProvisionDir.canonicalPath}/$mobileProvisionId.mobileprovision")
        val url = String.format(keyStoreUrl + keyStoreCertUrl, mobileProvisionId, KEYSTORE_CATEGORY_PROVISION)
        val headers = mutableMapOf<String, String>()
        headers[KEYSTORE_HTTP_HEADER_AUTH] = token?:""
        headers[KEYSTORE_HTTP_HEADER_IP] = InetAddress.getLocalHost().hostAddress
        OkhttpUtils.doGet(url, headers).use { resp ->
            if(resp.code() != 200 || resp.body() == null) {
                throw ErrorCodeException(
                        errorCode = SignMessageCode.ERROR_MP_NOT_EXIST,
                        defaultMessage = "KeyStore描述文件($mobileProvisionId)不存在或者下载失败。"
                )
            }
            val decryptedMobileProvisionEncrypt = resp.body()!!.bytes()
            val decryptedMobileProvisionDecrypt = EncryptUtil.decrypt(decryptedMobileProvisionEncrypt ,keyStoreAuthSecret)
            mobileProvisionFile.writeBytes(decryptedMobileProvisionDecrypt)
        }
        return mobileProvisionFile
    }

    private fun generateToken() {
        val claims = mutableMapOf<String, Any>()
        claims["authId"] = keyStoreAuthId
        claims["timeMillis"] = Instant.now().epochSecond
        val key = Keys.hmacShaKeyFor(keyStoreAuthSecret.toByteArray(StandardCharsets.UTF_8))
        token = Jwts.builder().setClaims(claims)
                .signWith(SignatureAlgorithm.HS256, key).compact()
    }

    /*
    * keystore的jwt有效期为3分钟，设置为
    * */
    @Scheduled(fixedDelay = 1 * 60 * 1000)
    fun refreshToken() {
        logger.info("Refresh keystore jwt token")
        generateToken()
    }

}
//
//fun main(args: Array<String>) {
//    val keyStoreAuthId = "devops"
//    val keyStoreAuthSecret = "a21c218df41f6d7fd032535fe20394e2"
//    val mobileProvisionDir  = File("/data/enterprise_sign_tmp/freyzheng/11111111/222222222/test.ipa.mobileProvisionDir")
//    val mobileProvisionId = "16722ba2-6fd1-45ef-a009-f94b20bc0d4f"
//    val keyStoreUrl = "https://proxy.test.keystore.oa.com"
//    val keyStoreCertUrl = "/api/auth/getCert?id=%s&category=%s"
//
//
//    val claims = mutableMapOf<String, Any>()
//    claims["authId"] = keyStoreAuthId
//    claims["timeMillis"] = Instant.now().epochSecond
//    val key = Keys.hmacShaKeyFor(keyStoreAuthSecret.toByteArray(StandardCharsets.UTF_8))
//    val token = Jwts.builder().setClaims(claims)
//            .signWith(key, SignatureAlgorithm.HS256).compact()
//    // 从keystore下载文件
//    val mobileProvisionFile = File("${mobileProvisionDir.canonicalPath}/$mobileProvisionId.mobileprovision")
//    val url = String.format(keyStoreUrl + keyStoreCertUrl, mobileProvisionId, KEYSTORE_CATEGORY_PROVISION)
//    val request = Request.Builder()
//            .url(url)
//            .addHeader(KEYSTORE_HTTP_HEADER_AUTH, token)
//            .addHeader(KEYSTORE_HTTP_HEADER_IP, InetAddress.getLocalHost().hostAddress)
//            .build()
//    val headers = mutableMapOf<String, String>()
//    headers[KEYSTORE_HTTP_HEADER_AUTH] = token ?:""
//    headers[KEYSTORE_HTTP_HEADER_IP] = InetAddress.getLocalHost().hostAddress
//    OkhttpUtils.doGet(url, headers).use { resp ->
//        if(resp.code() != 200 || resp.body() == null) {
//            throw ErrorCodeException(
//                    errorCode = SignMessageCode.ERROR_MP_NOT_EXIST,
//                    defaultMessage = "KeyStore描述文件不存在或者下载失败。"
//            )
//        }
//        val decryptedMobileProvisionEncrypt = resp.body()!!.bytes()
//        val decryptedMobileProvisionDecrypt = EncryptUtil.decrypt(decryptedMobileProvisionEncrypt ,keyStoreAuthSecret)
//        mobileProvisionFile.writeBytes(decryptedMobileProvisionDecrypt)
//    }
//    val a = 1
//}