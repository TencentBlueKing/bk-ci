package com.tencent.devops.dispatch.macos.service

import com.tencent.devops.common.api.util.AESUtil
import com.tencent.devops.common.api.util.DHUtil
import com.tencent.devops.dispatch.macos.dao.BuildTaskDao
import com.tencent.devops.dispatch.macos.dao.DevcloudVirtualMachineDao
import com.tencent.devops.dispatch.macos.pojo.PasswordInfo
import com.tencent.devops.dispatch.macos.util.RSAUtils
import com.tencent.devops.model.dispatch.macos.tables.records.TBuildTaskRecord
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

    @Value("\${macos.devCloud.defaultPassword:}")
    private lateinit var defaultPassword: String

    companion object {
        private val logger = LoggerFactory.getLogger(BuildTaskService::class.java)
    }

    fun getByBuildIdAndVmSeqId(
        buildId: String,
        vmSeqId: String?,
        executeCount: Int?
    ): Result<TBuildTaskRecord> {
        return buildTaskDao.listByBuildIdAndVmSeqId(dslContext, buildId, vmSeqId, executeCount)
    }

    fun getPassword(
        projectId: String,
        pipelineId: String,
        buildId: String,
        realIp: String,
        publicKey: String,
        padding: Boolean
    ): PasswordInfo? {
        logger.info("publicKey:$publicKey")
        logger.info("realIp:$realIp")
        val passwordOrigin = getPasswordOrigin(realIp)
        if (passwordOrigin.isNullOrBlank()) {
            throw RuntimeException("The vm Ip's password is not existed")
        }

        val publicKeyByteArray = Base64.getDecoder().decode(publicKey)
        val serverDHKeyPair = DHUtil.initKey(publicKeyByteArray)
        val serverPublicKeyByteArray = serverDHKeyPair.publicKey
        val serverPrivateKeyByteArray = serverDHKeyPair.privateKey
        val serverBase64PublicKey = String(Base64.getEncoder().encode(serverPublicKeyByteArray))

        val password = encryptCredential(
            aesEncryptedCredential = passwordOrigin,
            publicKeyByteArray = publicKeyByteArray,
            serverPrivateKeyByteArray = serverPrivateKeyByteArray,
            padding = padding
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
        serverPrivateKeyByteArray: ByteArray,
        padding: Boolean
    ): String {
        try {
            val credentialEncryptedContent = DHUtil.encrypt(
                aesEncryptedCredential.toByteArray(),
                publicKeyByteArray,
                serverPrivateKeyByteArray,
                padding
            )
            return String(Base64.getEncoder().encode(credentialEncryptedContent))
        } catch (ignored: Throwable) {
            throw ignored
        }
    }

    private fun getPasswordOrigin(realIp: String): String? {
        val buildTaskRecord = buildTaskDao.getByVmIp(dslContext, realIp)
        val encryptPassword = if (buildTaskRecord == null) {
            logger.info("buildTask does not has this ip:$realIp, use default password.")
            defaultPassword
        } else {
            val devcloudVmInfoRecord = devcloudVirtualMachineDao.getByVmId(dslContext, buildTaskRecord.vmId)
            if (devcloudVmInfoRecord == null) {
                logger.info("devcloud macos does not has this ip:$realIp")
                null
            } else {
                devcloudVmInfoRecord.password
            }
        }

        if (encryptPassword == null) {
            logger.info("devcloud macos ip:$realIp,password is null")
            return null
        }

        val keyFactory = KeyFactory.getInstance("RSA")
        val privateKey =
            keyFactory.generatePrivate(PKCS8EncodedKeySpec(Base64.getMimeDecoder().decode(rsaPrivateKey)))
        val passwordDevcloud = RSAUtils.decryptByPrivateKey(encryptPassword, privateKey)
        logger.info("devcloud macos ip:$realIp,password:$passwordDevcloud")
        return AESUtil.encrypt(aesKey, passwordDevcloud)
    }
}
