package com.tencent.devops.dispatch.macos.service

import com.tencent.devops.common.api.util.AESUtil
import com.tencent.devops.common.api.util.DHUtil
import com.tencent.devops.dispatch.macos.dao.BuildTaskDao
import com.tencent.devops.dispatch.macos.dao.DevcloudVirtualMachineDao
import com.tencent.devops.dispatch.macos.pojo.PasswordInfo
import com.tencent.devops.dispatch.macos.util.RSAUtils
import com.tencent.devops.model.dispatch.macos.tables.records.TBuildTaskRecord
import org.jolokia.util.Base64Util
import org.jooq.DSLContext
import org.jooq.Result
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.security.KeyFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64

@Service
class BuildTaskService @Autowired constructor(
    private val dslContext: DSLContext,
    private val buildTaskDao: BuildTaskDao,
    private val devcloudVirtualMachineDao: DevcloudVirtualMachineDao
) {

    @Value("\${macos.credential.aes-key:C/R%3{?OS}IeGT21}")
    private lateinit var aesKey: String

    @Value("\${macos.devCloud.rsaPrivateKey:}")
    private lateinit var rsaPrivateKey: String

    companion object {
        private val logger = LoggerFactory.getLogger(BuildTaskService::class.java)
    }

    fun getByBuildIdAndVmSeqId(
        buildId: String,
        vmSeqId: String?,
        executeCount: Int?
    ): Result<TBuildTaskRecord> {
        var buildRecord = buildTaskDao.listByBuildIdAndVmSeqId(dslContext, buildId, vmSeqId, executeCount)
        // 如果构建记录为空，可能是因为取消时分配构建IP接口还未完成，等待30s
        if (buildRecord.isEmpty()) {
            Thread.sleep(30000)
            buildRecord = buildTaskDao.listByBuildIdAndVmSeqId(dslContext, buildId, vmSeqId, executeCount)
        }

        return buildRecord
    }

    fun getPassword(
        projectId: String,
        pipelineId: String,
        buildId: String,
        realIp: String,
        publicKey: String
    ): PasswordInfo? {
        logger.info("publicKey:$publicKey")
        logger.info("realIp:$realIp")
        val builTaskRecord = buildTaskDao.getByVmIp(dslContext, realIp)
        if (builTaskRecord == null) {
            logger.info("builTask does not has this ip:$realIp")
            return null
        }
        logger.info("builTaskRecord:$builTaskRecord")

        logger.info("ip comes from devcloud:$realIp")
        val devcloudVmInfoRecord = devcloudVirtualMachineDao.getByVmId(dslContext, builTaskRecord.vmId)
        val passwordOrigin = if (devcloudVmInfoRecord == null) {
            logger.info("devcloud macos does not has this ip:$realIp")
            null
        } else {
            val keyFactory = KeyFactory.getInstance("RSA")
            val privateKey = keyFactory.generatePrivate(PKCS8EncodedKeySpec(Base64Util.decode(rsaPrivateKey)))
            val passwordDevcloud = RSAUtils.decryptByPrivateKey(devcloudVmInfoRecord.password, privateKey)
            logger.info("devcloud macos ip:$realIp,password:$passwordDevcloud")
            AESUtil.encrypt(aesKey, passwordDevcloud)
        }

        val publicKeyByteArray = Base64.getDecoder().decode(publicKey)
        val serverDHKeyPair = DHUtil.initKey(publicKeyByteArray)
        val serverPublicKeyByteArray = serverDHKeyPair.publicKey
        val serverPrivateKeyByteArray = serverDHKeyPair.privateKey
        val serverBase64PublicKey = String(Base64.getEncoder().encode(serverPublicKeyByteArray))
        if (passwordOrigin.isNullOrBlank()) {
            throw RuntimeException("The vm Ip's password is not existed")
        }
        val password = encryptCredential(
            aesEncryptedCredential = passwordOrigin!!,
            publicKeyByteArray = publicKeyByteArray,
            serverPrivateKeyByteArray = serverPrivateKeyByteArray
        )
        logger.info("passwordOrigin:$passwordOrigin")
        logger.info("serverBase64PublicKey:$serverBase64PublicKey")
        logger.info("password:$password")
        return PasswordInfo(
            publicKey = serverBase64PublicKey,
            password = password
        )
    }

    fun encryptCredential(
        aesEncryptedCredential: String,
        publicKeyByteArray: ByteArray,
        serverPrivateKeyByteArray: ByteArray
    ): String {
        try {
            val credentialEncryptedContent =
                DHUtil.encrypt(aesEncryptedCredential.toByteArray(), publicKeyByteArray, serverPrivateKeyByteArray)
            return String(Base64.getEncoder().encode(credentialEncryptedContent))
        } catch (ignored: Throwable) {
            throw ignored
        }
    }
}
