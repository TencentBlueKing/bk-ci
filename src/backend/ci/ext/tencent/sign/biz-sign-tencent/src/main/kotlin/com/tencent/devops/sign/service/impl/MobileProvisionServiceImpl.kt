package com.tencent.devops.sign.service.impl

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.DHUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.sign.api.constant.SignMessageCode
import com.tencent.devops.sign.service.MobileProvisionService
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.io.File
import java.time.Instant
import java.util.*


@Service
class MobileProvisionServiceImpl  @Autowired constructor(
        private val client: Client
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
        val url = String.format(keyStoreUrl + keyStoreCertUrl, mobileProvisionId, "MP")
        return mobileProvisionDir
    }

    private fun generateToken(): String? {
        val claims = mutableMapOf<String, Any>()
        claims["authId"] = keyStoreAuthId
        claims["timeMillis"] = Instant.now().epochSecond
        token = Jwts.builder().setClaims(claims)
                .signWith(SignatureAlgorithm.HS256, keyStoreAuthSecret).compact()
        return token
    }

    /*
    * keystore的jwt有效期为3分钟，设置为
    * */ServiceBuildResource
    @Scheduled(fixedDelay = 1 * 60 * 1000)
    fun refreshToken() {
        logger.info("Refresh keystore jwt token")
        generateToken()
    }

}

fun main(argv: Array<String>) {
    val keyStoreAuthId = "devops"
    val keyStoreAuthSecret = "a21c218df41f6d7fd032535fe20394e2"
    val claims = mutableMapOf<String, Any>()
    claims["authId"] = keyStoreAuthId
    claims["timeMillis"] = Instant.now().epochSecond
    val token = Jwts.builder().setClaims(claims)
            .signWith(SignatureAlgorithm.HS256, keyStoreAuthSecret).compact()
}


