package com.tencent.devops.sign.service.impl

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.DHUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.sign.api.constant.SignMessageCode
import com.tencent.devops.sign.service.MobileProvisionService
import com.tencent.devops.ticket.api.ServiceCertResource
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.*

@Service
class MobileProvisionServiceImpl  @Autowired constructor(
        private val client: Client
) : MobileProvisionService {

    companion object {
        private val logger = LoggerFactory.getLogger(MobileProvisionServiceImpl::class.java)
        private val pairKey = DHUtil.initKey()
        private val privateKey = pairKey.privateKey
        private val publicKey = String(Base64.getEncoder().encode(pairKey.publicKey))
    }

    override fun downloadMobileProvision(mobileProvisionDir: File, projectId: String, mobileProvisionId: String): File {
        // 从ticket模块获取描述文件
        val mpInfo = client.get(ServiceCertResource::class).getEnterprise(projectId, mobileProvisionId, publicKey).data
                ?: throw ErrorCodeException(errorCode = SignMessageCode.ERROR_MP_NOT_EXIST, defaultMessage = "描述文件不存在。")
        val publicKeyServer = Base64.getDecoder().decode(mpInfo.publicKey)
        val mpContent = Base64.getDecoder().decode(mpInfo.mobileProvisionContent)
        val mobileProvision = DHUtil.decrypt(mpContent, publicKeyServer, privateKey)
        val mobileProvisionFile = File("${mobileProvisionDir.canonicalPath}/$mobileProvisionId.mobileprovision")
        mobileProvisionFile.writeBytes(mobileProvision)
        return mobileProvisionFile
    }

}
