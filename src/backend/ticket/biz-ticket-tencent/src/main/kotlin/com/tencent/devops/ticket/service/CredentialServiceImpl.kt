package com.tencent.devops.ticket.service

import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.util.AESUtil
import com.tencent.devops.common.api.util.DHUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.BSAuthPermissionApi
import com.tencent.devops.common.auth.api.BSAuthResourceApi
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.code.TicketAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.ticket.dao.CredentialDao
import com.tencent.devops.ticket.pojo.Credential
import com.tencent.devops.ticket.pojo.CredentialCreate
import com.tencent.devops.ticket.pojo.CredentialInfo
import com.tencent.devops.ticket.pojo.CredentialPermissions
import com.tencent.devops.ticket.pojo.CredentialUpdate
import com.tencent.devops.ticket.pojo.CredentialWithPermission
import com.tencent.devops.ticket.pojo.enums.CredentialType
import com.tencent.devops.ticket.util.CredentialUtil
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.Base64
import javax.ws.rs.NotFoundException
import javax.ws.rs.core.Response

@Service
class CredentialServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val client: Client,
    private val authPermissionApi: BSAuthPermissionApi,
    private val authResourceApi: BSAuthResourceApi,
    private val serviceCode: TicketAuthServiceCode,
    private val credentialDao: CredentialDao
) : CredentialService {
    private val resourceType = AuthResourceType.TICKET_CREDENTIAL
    private val aesKey = "G/I%yP{?ST}2TXPg"
    private val credentialMixer = "******"
    private val credentialIdMaxSize = 32

    fun hasCreatePermission(userId: String, projectId: String): Boolean {
        return validatePermission(userId, projectId, AuthPermission.CREATE)
    }

    override fun userCreate(
        userId: String,
        projectId: String,
        credential: CredentialCreate,
        authGroupList: List<BkAuthGroup>?
    ) {
        validatePermission(userId, projectId, AuthPermission.CREATE, "用户($userId)在工程($projectId)下没有凭据创建权限")

        if (credentialDao.has(dslContext, projectId, credential.credentialId)) {
            throw OperationException("名称${credential.credentialId}已存在")
        }
        if (!CredentialUtil.isValid(credential)) {
            throw OperationException("凭证格式不正确")
        }
        if (credential.credentialId.length > credentialIdMaxSize) {
            throw OperationException("凭证ID不能超过32位")
        }

        val credentialV1 = AESUtil.encrypt(aesKey, credential.v1)
        val credentialV2 = if (credential.v2.isNullOrEmpty()) {
            null
        } else {
            AESUtil.encrypt(aesKey, credential.v2!!)
        }
        val credentialV3 = if (credential.v3.isNullOrEmpty()) {
            null
        } else {
            AESUtil.encrypt(aesKey, credential.v3!!)
        }
        val credentialV4 = if (credential.v4.isNullOrEmpty()) {
            null
        } else {
            AESUtil.encrypt(aesKey, credential.v4!!)
        }

        createGrantResource(userId, projectId, credential.credentialId, authGroupList)
        credentialDao.create(dslContext,
            projectId,
            userId,
            credential.credentialId,
            credential.credentialType.name,
            credentialV1,
            credentialV2,
            credentialV3,
            credentialV4,
            credential.credentialRemark
        )
    }

    override fun userEdit(userId: String, projectId: String, credentialId: String, credential: CredentialUpdate) {
        validatePermission(userId, projectId, credentialId, AuthPermission.EDIT, "用户($userId)在工程($projectId)下没有凭据($credentialId)的编辑权限")

        if (!credentialDao.has(dslContext, projectId, credentialId)) {
            throw OperationException("凭证$credentialId 不存在")
        }
        if (!CredentialUtil.isValid(credential)) {
            throw OperationException("凭证格式不正确")
        }

        val credentialV1 = if (credential.v1 == credentialMixer) {
            null
        } else {
            AESUtil.encrypt(aesKey, credential.v1)
        }
        val credentialV2 = if (credential.v2 == credentialMixer || credential.v2.isNullOrEmpty()) {
            null
        } else {
            AESUtil.encrypt(aesKey, credential.v2!!)
        }
        val credentialV3 = if (credential.v3 == credentialMixer || credential.v3.isNullOrEmpty()) {
            null
        } else {
            AESUtil.encrypt(aesKey, credential.v3!!)
        }
        val credentialV4 = if (credential.v4 == credentialMixer || credential.v4.isNullOrEmpty()) {
            null
        } else {
            AESUtil.encrypt(aesKey, credential.v4!!)
        }

        credentialDao.updateIgnoreNull(dslContext,
                projectId,
                credentialId,
                credentialV1,
                credentialV2,
                credentialV3,
                credentialV4,
                credential.credentialRemark)
    }

    override fun userDelete(userId: String, projectId: String, credentialId: String) {
        validatePermission(userId, projectId, credentialId, AuthPermission.DELETE, "用户($userId)在工程($projectId)下没有凭据($credentialId)的删除权限")

        deleteResource(projectId, credentialId)
        credentialDao.delete(dslContext, projectId, credentialId)
    }

    override fun userList(userId: String, projectId: String, credentialTypes: List<CredentialType>?, offset: Int, limit: Int, keyword: String?): SQLPage<CredentialWithPermission> {
        val permissionToListMap = filterCredentials(userId, projectId, setOf(AuthPermission.LIST, AuthPermission.DELETE, AuthPermission.VIEW, AuthPermission.EDIT))
        val hasListPermissionCredentialIdList = permissionToListMap[AuthPermission.LIST]!!
        val hasDeletePermissionCredentialIdList = permissionToListMap[AuthPermission.DELETE]!!
        val hasViewPermissionCredentialIdList = permissionToListMap[AuthPermission.VIEW]!!
        val hasEditPermissionCredentialIdList = permissionToListMap[AuthPermission.EDIT]!!

        val count = credentialDao.countByProject(dslContext, projectId, credentialTypes?.toSet(), hasListPermissionCredentialIdList.toSet())
        val credentialRecordList = credentialDao.listByProject(dslContext, projectId, credentialTypes?.toSet(), hasListPermissionCredentialIdList.toSet(), offset, limit, keyword)
        val credentialList = credentialRecordList.map {
            val hasDeletePermission = hasDeletePermissionCredentialIdList.contains(it.credentialId)
            val hasViewPermission = hasViewPermissionCredentialIdList.contains(it.credentialId)
            val hasEditPermission = hasEditPermissionCredentialIdList.contains(it.credentialId)
            CredentialWithPermission(
                it.credentialId,
                CredentialType.valueOf(it.credentialType),
                it.credentialRemark,
                it.createdTime.timestamp(),
                credentialMixer,
                credentialMixer,
                credentialMixer,
                credentialMixer,
                CredentialPermissions(
                        hasDeletePermission,
                        hasViewPermission,
                        hasEditPermission
                )
            )
        }
        return SQLPage(count, credentialList)
    }

    override fun hasPermissionList(
        userId: String,
        projectId: String,
        credentialTypes: List<CredentialType>?,
        bkAuthPermission: AuthPermission,
        offset: Int?,
        limit: Int?,
        keyword: String?
    ): SQLPage<Credential> {
        val hasPermissionList = filterCredential(userId, projectId, bkAuthPermission)

        val count = credentialDao.countByProject(dslContext, projectId, credentialTypes?.toSet(), hasPermissionList.toSet())
        val credentialRecordList = credentialDao.listByProject(dslContext, projectId, credentialTypes?.toSet(), hasPermissionList.toSet(), offset, limit, keyword)
        val credentialList = credentialRecordList.map {
            Credential(
                    it.credentialId,
                    CredentialType.valueOf(it.credentialType),
                    it.credentialRemark,
                    it.createdTime.timestamp(),
                    credentialMixer,
                    credentialMixer,
                    credentialMixer,
                    credentialMixer
            )
        }
        return SQLPage(count, credentialList)
    }

    override fun serviceList(projectId: String, offset: Int, limit: Int): SQLPage<Credential> {
        val count = credentialDao.countByProject(dslContext, projectId)
        val credentialRecords = credentialDao.listByProject(dslContext, projectId, offset, limit)
        val result = credentialRecords.map {
            Credential(
                    it.credentialId,
                    CredentialType.valueOf(it.credentialType),
                    it.credentialRemark,
                    it.createdTime.timestamp(),
                    credentialMixer,
                    credentialMixer,
                    credentialMixer,
                    credentialMixer

            )
        }
        return SQLPage(count, result)
    }

    override fun serviceCheck(projectId: String, credentialId: String) {
        if (!credentialDao.has(dslContext, projectId, credentialId)) {
            throw NotFoundException("Credential $credentialId does not exists")
        }
    }

    override fun userShow(userId: String, projectId: String, credentialId: String): CredentialWithPermission {
        validatePermission(userId, projectId, credentialId, AuthPermission.VIEW, "用户($userId)在工程($projectId)下没有凭据($credentialId)的查看权限")

        val hasViewPermission = true
        val hasDeletePermission = validatePermission(userId, projectId, credentialId, AuthPermission.DELETE)
        val hasEditPermission = validatePermission(userId, projectId, credentialId, AuthPermission.EDIT)

        val credentialRecord = credentialDao.get(dslContext, projectId, credentialId)
        val credentialV1 = AESUtil.decrypt(aesKey, credentialRecord.credentialV1)
        val credentialV2 = if (credentialRecord.credentialV2 == null) {
            null
        } else {
            AESUtil.decrypt(aesKey, credentialRecord.credentialV2)
        }
        val credentialV3 = if (credentialRecord.credentialV3 == null) {
            null
        } else {
            AESUtil.decrypt(aesKey, credentialRecord.credentialV3)
        }
        val credentialV4 = if (credentialRecord.credentialV4 == null) {
            null
        } else {
            AESUtil.decrypt(aesKey, credentialRecord.credentialV4)
        }

        return CredentialWithPermission(
                credentialId,
                CredentialType.valueOf(credentialRecord.credentialType),
                credentialRecord.credentialRemark,
                credentialRecord.updatedTime.timestamp(),
                credentialV1,
                credentialV2,
                credentialV3,
                credentialV4,
                CredentialPermissions(
                        hasDeletePermission,
                        hasViewPermission,
                        hasEditPermission
                )
        )
    }

    override fun userGet(userId: String, projectId: String, credentialId: String): CredentialWithPermission {
        validatePermission(userId, projectId, credentialId, AuthPermission.VIEW, "用户($userId)在工程($projectId)下没有凭据($credentialId)的查看权限")

        val hasViewPermission = true
        val hasDeletePermission = validatePermission(userId, projectId, credentialId, AuthPermission.DELETE)
        val hasEditPermission = validatePermission(userId, projectId, credentialId, AuthPermission.EDIT)

        val credentialRecord = credentialDao.get(dslContext, projectId, credentialId)
        return CredentialWithPermission(
                credentialId,
                CredentialType.valueOf(credentialRecord.credentialType),
                credentialRecord.credentialRemark,
                credentialRecord.updatedTime.timestamp(),
                credentialMixer,
                credentialMixer,
                credentialMixer,
                credentialMixer,
                CredentialPermissions(
                        hasDeletePermission,
                        hasViewPermission,
                        hasEditPermission
                )
        )
    }

    override fun buildGet(buildId: String, credentialId: String, publicKey: String): CredentialInfo {
        val buildBasicInfoResult = client.get(ServiceBuildResource::class).serviceBasic(buildId)
        if (buildBasicInfoResult.isNotOk()) {
            throw RemoteServiceException("Failed to build the basic information based on the buildId")
        }
        val buildBasicInfo = buildBasicInfoResult.data ?: throw RemoteServiceException("Failed to build the basic information based on the buildId")
        return serviceGet(buildBasicInfo.projectId, credentialId, publicKey)
    }

    override fun buildGetDetail(buildId: String, credentialId: String): Map<String, String> {
        val buildBasicInfoResult = client.get(ServiceBuildResource::class).serviceBasic(buildId)
        if (buildBasicInfoResult.isNotOk()) {
            throw RemoteServiceException("Failed to build the basic information based on the buildId")
        }
        val buildBasicInfo = buildBasicInfoResult.data ?: throw RemoteServiceException("Failed to build the basic information based on the buildId")
        val credentialInfo = serviceGet(buildBasicInfo.projectId, credentialId)
        val keyMap = CredentialType.getKeyMap(credentialInfo.credentialType.name)
        val credentialMap = mutableMapOf<String, String?>()
        credentialMap["v1"] = credentialInfo.v1
        credentialMap["v2"] = credentialInfo.v2
        credentialMap["v3"] = credentialInfo.v3
        credentialMap["v4"] = credentialInfo.v4

        val ret = mutableMapOf<String, String>()
        keyMap.forEach { (k, v) ->
            ret[v] = credentialMap[k] ?: ""
        }

        return ret
    }

    override fun serviceGet(projectId: String, credentialId: String, publicKey: String): CredentialInfo {
        val credentialRecord = credentialDao.get(dslContext, projectId, credentialId)

        val publicKeyByteArray = Base64.getDecoder().decode(publicKey)
        val serverDHKeyPair = DHUtil.initKey(publicKeyByteArray)
        val serverPublicKeyByteArray = serverDHKeyPair.publicKey
        val serverPrivateKeyByteArray = serverDHKeyPair.privateKey
        val serverBase64PublicKey = String(Base64.getEncoder().encode(serverPublicKeyByteArray))

        val credentialV1 = encryptCredential(credentialRecord.credentialV1, publicKeyByteArray, serverPrivateKeyByteArray)
        val credentialV2 = if (credentialRecord.credentialV2 == null) {
            null
        } else {
            encryptCredential(credentialRecord.credentialV2, publicKeyByteArray, serverPrivateKeyByteArray)
        }
        val credentialV3 = if (credentialRecord.credentialV3 == null) {
            null
        } else {
            encryptCredential(credentialRecord.credentialV3, publicKeyByteArray, serverPrivateKeyByteArray)
        }
        val credentialV4 = if (credentialRecord.credentialV4 == null) {
            null
        } else {
            encryptCredential(credentialRecord.credentialV4, publicKeyByteArray, serverPrivateKeyByteArray)
        }

        return CredentialInfo(
                serverBase64PublicKey,
                CredentialType.valueOf(credentialRecord.credentialType),
                credentialV1,
                credentialV2,
                credentialV3,
                credentialV4)
    }

    override fun serviceGet(projectId: String, credentialId: String): Credential {
        val record = credentialDao.get(dslContext, projectId, credentialId)

        val credentialV1 = AESUtil.decrypt(aesKey, record.credentialV1)
        val credentialV2 = if (record.credentialV2 == null) {
            null
        } else {
            AESUtil.decrypt(aesKey, record.credentialV2)
        }
        val credentialV3 = if (record.credentialV3 == null) {
            null
        } else {
            AESUtil.decrypt(aesKey, record.credentialV3)
        }
        val credentialV4 = if (record.credentialV4 == null) {
            null
        } else {
            AESUtil.decrypt(aesKey, record.credentialV4)
        }

        return Credential(
                record.credentialId,
                CredentialType.valueOf(record.credentialType),
                record.credentialRemark,
                record.updatedTime.timestamp(),
                credentialV1,
                credentialV2,
                credentialV3,
                credentialV4
        )
    }

    private fun encryptCredential(aesEncryptedCredential: String, publicKeyByteArray: ByteArray, serverPrivateKeyByteArray: ByteArray): String {
        try {
            val credential = AESUtil.decrypt(aesKey, aesEncryptedCredential)
            val credentialEncryptedContent = DHUtil.encrypt(credential.toByteArray(), publicKeyByteArray, serverPrivateKeyByteArray)
            return String(Base64.getEncoder().encode(credentialEncryptedContent))
        } catch (t: Throwable) {
            logger.warn("Fail to encrypt credential($aesEncryptedCredential) of publicKey(${String(Base64.getEncoder().encode(publicKeyByteArray))}) " +
                    "and servicePrivateKey(${String(Base64.getEncoder().encode(serverPrivateKeyByteArray))})", t)
            throw t
        }
    }

    fun validatePermission(user: String, projectId: String, bkAuthPermission: AuthPermission, message: String) {
        if (!validatePermission(user, projectId, bkAuthPermission)) {
            throw CustomException(Response.Status.FORBIDDEN, message)
        }
    }

    fun validatePermission(user: String, projectId: String, resourceCode: String, bkAuthPermission: AuthPermission, message: String) {
        if (!validatePermission(user, projectId, resourceCode, bkAuthPermission)) {
            throw CustomException(Response.Status.FORBIDDEN, message)
        }
    }

    private fun validatePermission(user: String, projectId: String, bkAuthPermission: AuthPermission): Boolean {
        return authPermissionApi.validateUserResourcePermission(user, serviceCode, resourceType, projectId, bkAuthPermission)
    }

    private fun validatePermission(user: String, projectId: String, resourceCode: String, bkAuthPermission: AuthPermission): Boolean {
        return authPermissionApi.validateUserResourcePermission(user, serviceCode, resourceType, projectId, resourceCode, bkAuthPermission)
    }

    private fun filterCredential(user: String, projectId: String, bkAuthPermission: AuthPermission): List<String> {
        return authPermissionApi.getUserResourceByPermission(user, serviceCode, resourceType, projectId, bkAuthPermission, null)
    }

    private fun filterCredentials(user: String, projectId: String, bkAuthPermissions: Set<AuthPermission>): Map<AuthPermission, List<String>> {
        return authPermissionApi.getUserResourcesByPermissions(user, serviceCode, resourceType, projectId, bkAuthPermissions, null)
    }

    private fun createResource(user: String, projectId: String, credentialId: String) {
        authResourceApi.createResource(user, serviceCode, resourceType, projectId, credentialId, credentialId)
    }

    fun createGrantResource(user: String, projectId: String, credentialId: String, authGroupList: List<BkAuthGroup>?) {
        authResourceApi.createGrantResource(user, serviceCode, resourceType, projectId, credentialId, credentialId, authGroupList)
    }

    private fun deleteResource(projectId: String, credentialId: String) {
        authResourceApi.deleteResource(serviceCode, resourceType, projectId, credentialId)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CredentialService::class.java)
    }
}

/**
fun main(argv: Array<String>) {
    val credential = "123456"

    val dh = DHUtil.initKey()
    val server = DHUtil.initKey(dh.publicKey)
    println("sender keys:")
    println("PublicKey: ${String(Base64.getEncoder().encode(dh.publicKey))}")
    println("PrivateKey: ${String(Base64.getEncoder().encode(dh.privateKey))}")
    println()

    println("receiver keys:")
    println("PublicKey: ${String(Base64.getEncoder().encode(server.publicKey))}")
    println("PrivateKey: ${String(Base64.getEncoder().encode(server.privateKey))}")
    println()

    val credentialEncryptedContent = DHUtil.encrypt(credential.toByteArray(), dh.publicKey, server.privateKey)
    println("Encrypted: ${String(Base64.getEncoder().encode(credentialEncryptedContent))}")

    val credentialDecryptedContent = DHUtil.decrypt(credentialEncryptedContent, server.publicKey, dh.privateKey)
    println("Decrypted: ${String(credentialDecryptedContent)}")
}
 */
