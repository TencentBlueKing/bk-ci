package com.tencent.devops.ticket.service

import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.util.AESUtil
import com.tencent.devops.common.api.util.DHUtil
import com.tencent.devops.common.api.util.ShaUtils
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.BSAuthPermissionApi
import com.tencent.devops.common.auth.api.BSAuthResourceApi
import com.tencent.devops.common.auth.code.TicketAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.ticket.dao.CertDao
import com.tencent.devops.ticket.dao.CertEnterpriseDao
import com.tencent.devops.ticket.dao.CertTlsDao
import com.tencent.devops.ticket.pojo.Cert
import com.tencent.devops.ticket.pojo.CertIOSInfo
import com.tencent.devops.ticket.pojo.CertPermissions
import com.tencent.devops.ticket.pojo.CertWithPermission
import com.tencent.devops.ticket.pojo.CertEnterpriseInfo
import com.tencent.devops.ticket.pojo.CertAndroidInfo
import com.tencent.devops.ticket.pojo.CertTlsInfo
import com.tencent.devops.ticket.pojo.CertEnterprise
import com.tencent.devops.ticket.pojo.CertIOS
import com.tencent.devops.ticket.pojo.CertAndroid
import com.tencent.devops.ticket.pojo.CertAndroidWithCredential
import com.tencent.devops.ticket.pojo.CertTls
import com.tencent.devops.ticket.pojo.enums.CertAndroidType
import com.tencent.devops.ticket.pojo.enums.CertType
import com.tencent.devops.ticket.util.CertUtil
import com.tencent.devops.ticket.util.MobileProvisionUtil
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.charset.Charset
import java.time.LocalDateTime
import java.util.Base64
import javax.ws.rs.core.Response

@Service
class CertServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val client: Client,
    private val authPermissionApi: BSAuthPermissionApi,
    private val authResourceApi: BSAuthResourceApi,
    private val certDao: CertDao,
    private val certTlsDao: CertTlsDao,
    private val certEnterpriseDao: CertEnterpriseDao,
    private val credentialService: CredentialServiceImpl,
    private val serviceCode: TicketAuthServiceCode
) : CertService {
    private val resourceType = AuthResourceType.TICKET_CERT
    private val aesKey = "gHi(xG9Af)jEvCx&"
    private val certMaxSize = 64 * 1024
    private val certIdMaxSize = 32

    fun hasCreatePermission(userId: String, projectId: String): Boolean {
        return validatePermission(userId, projectId, AuthPermission.CREATE)
    }

    override fun uploadIos(
        userId: String,
        projectId: String,
        certId: String,
        certRemark: String?,
        certCredentialId: String?,
        p12InputStream: InputStream,
        p12Disposition: FormDataContentDisposition,
        mpInputStream: InputStream,
        mpDisposition: FormDataContentDisposition
    ) {
        validatePermission(userId, projectId, AuthPermission.CREATE, "用户($userId)在工程($projectId)下没有证书创建权限")

        if (certCredentialId != null) {
            credentialService.validatePermission(
                    userId,
                    projectId,
                    certCredentialId,
                    AuthPermission.USE,
                    "用户($userId)在工程($projectId)下没有凭据($certCredentialId)的使用权限"
            )
        }
        if (certDao.has(dslContext, projectId, certId)) {
            throw OperationException("名称${certId}已存在")
        }

        val p12FileContent = read(p12InputStream)
        val mpFileContent = read(mpInputStream)
        if (p12FileContent.size > certMaxSize) {
            throw OperationException("p12文件大小不能超过64k")
        }
        if (mpFileContent.size > certMaxSize) {
            throw OperationException("证书描述文件大小不能超过64k")
        }
        if (certId.length > certIdMaxSize) {
            throw OperationException("证书名称不能超过32位")
        }

        val mpInfo = MobileProvisionUtil.parse(mpFileContent) ?: throw OperationException("不合法的mobileprovision文件")

        val credentialId = certCredentialId ?: ""
        val remark = certRemark ?: ""
        val certType = CertType.IOS.value
        val p12FileName = String(p12Disposition.fileName.toByteArray(Charset.forName("ISO-8859-1")))
        val p12EncryptedFileContent = AESUtil.encrypt(aesKey, p12FileContent)
        val mpFileName = String(mpDisposition.fileName.toByteArray(Charset.forName("ISO-8859-1")))
        val mpEncryptedFileContent = AESUtil.encrypt(aesKey, mpFileContent)
        val jksFileName = ""
        val jksEncryptedFileContent = ByteArray(0)
        val jksAlias = ""
        val jksAliasCredentialId = ""
        val developerName = mpInfo.name
        val teamName = mpInfo.teamName
        val uuid = mpInfo.uuid
        val expireDate = mpInfo.expireDate

        createResource(userId, projectId, certId)
        certDao.create(
                dslContext,
                projectId,
                certId,
                userId,
                certType,
                remark,
                p12FileName,
                p12EncryptedFileContent,
                mpFileName,
                mpEncryptedFileContent,
                jksFileName,
                jksEncryptedFileContent,
                jksAlias,
                jksAliasCredentialId,
                developerName,
                teamName,
                uuid,
                expireDate,
                credentialId
        )
    }

    override fun updateIos(
        userId: String,
        projectId: String,
        certId: String,
        certRemark: String?,
        certCredentialId: String?,
        p12InputStream: InputStream?,
        p12Disposition: FormDataContentDisposition?,
        mpInputStream: InputStream?,
        mpDisposition: FormDataContentDisposition?
    ) {
        validatePermission(userId, projectId, certId, AuthPermission.EDIT, "用户($userId)在工程($projectId)下没有证书编辑权限")
        if (!certDao.has(dslContext, projectId, certId)) {
            throw OperationException("名称${certId}不存在")
        }

        if (certCredentialId != null) {
            credentialService.validatePermission(
                    userId,
                    projectId,
                    certCredentialId,
                    AuthPermission.USE,
                    "用户($userId)在工程($projectId)下没有凭据($certCredentialId)的使用权限"
            )
        }

        val p12FileContent = if (p12InputStream != null) read(p12InputStream) else null
        val mpFileContent = if (mpInputStream != null) read(mpInputStream) else null
        if (p12FileContent != null && p12FileContent.size > certMaxSize) {
            throw OperationException("p12文件大小不能超过64k")
        }
        if (mpFileContent != null && mpFileContent.size > certMaxSize) {
            throw OperationException("证书描述文件大小不能超过64k")
        }

        val mpInfo = if (mpFileContent != null) MobileProvisionUtil.parse(mpFileContent)
                ?: throw OperationException("不合法的mobileprovision文件")
        else null

        val credentialId = certCredentialId ?: ""
        val remark = certRemark ?: ""
        val p12FileName = if (p12Disposition != null) String(p12Disposition.fileName.toByteArray(Charset.forName("ISO-8859-1"))) else null
        val p12EncryptedFileContent = if (p12FileContent != null) AESUtil.encrypt(aesKey, p12FileContent) else null
        val mpFileName = if (mpDisposition != null) String(mpDisposition.fileName.toByteArray(Charset.forName("ISO-8859-1"))) else null
        val mpEncryptedFileContent = if (mpFileContent != null) AESUtil.encrypt(aesKey, mpFileContent) else null
        val jksFileName = ""
        val jksEncryptedFileContent = ByteArray(0)
        val jksAlias = ""
        val jksAliasCredentialId = ""
        val developerName = mpInfo?.name
        val teamName = mpInfo?.teamName
        val uuid = mpInfo?.uuid
        val expireDate = mpInfo?.expireDate

        certDao.update(
                dslContext,
                projectId,
                certId,
                userId,
                remark,
                p12FileName,
                p12EncryptedFileContent,
                mpFileName,
                mpEncryptedFileContent,
                jksFileName,
                jksEncryptedFileContent,
                jksAlias,
                jksAliasCredentialId,
                developerName,
                teamName,
                uuid,
                expireDate,
                credentialId
        )
    }

    override fun uploadEnterprise(
        userId: String,
        projectId: String,
        certId: String,
        certRemark: String?,
        mpInputStream: InputStream,
        mpDisposition: FormDataContentDisposition
    ) {
        validatePermission(userId, projectId, AuthPermission.CREATE, "用户($userId)在工程($projectId)下没有证书创建权限")

        if (certDao.has(dslContext, projectId, certId)) {
            throw OperationException("名称${certId}已存在")
        }

        val mpFileContent = read(mpInputStream)
        if (mpFileContent.size > certMaxSize) {
            throw OperationException("证书描述文件大小不能超过64k")
        }
        if (certId.length > certIdMaxSize) {
            throw OperationException("证书名称不能超过32位")
        }

        val mpInfo = MobileProvisionUtil.parse(mpFileContent) ?: throw OperationException("不合法的mobileprovision文件")

        val remark = certRemark ?: ""
        val certType = CertType.ENTERPRISE.value
        val mpFileName = String(mpDisposition.fileName.toByteArray(Charset.forName("ISO-8859-1")))
        val mpEncryptedFileContent = AESUtil.encrypt(aesKey, mpFileContent)
        val developerName = mpInfo.name
        val teamName = mpInfo.teamName
        val uuid = mpInfo.uuid
        val expireDate = mpInfo.expireDate

        // 只保存描述文件，其他置空
        val jksFileName = ""
        val jksEncryptedFileContent = ByteArray(0)
        val jksAlias = ""
        val jksAliasCredentialId = ""
        val p12FileName = ""
        val p12EncryptedFileContent = ByteArray(0)
        val credentialId = null

        createResource(userId, projectId, certId)
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            certDao.create(
                    transactionContext,
                    projectId,
                    certId,
                    userId,
                    certType,
                    remark,
                    p12FileName,
                    p12EncryptedFileContent,
                    mpFileName,
                    mpEncryptedFileContent,
                    jksFileName,
                    jksEncryptedFileContent,
                    jksAlias,
                    jksAliasCredentialId,
                    developerName,
                    teamName,
                    uuid,
                    expireDate,
                    credentialId
            )
            certEnterpriseDao.create(
                    transactionContext,
                    projectId,
                    certId,
                    mpFileName,
                    mpEncryptedFileContent,
                    developerName,
                    teamName,
                    uuid,
                    expireDate
            )
        }
    }

    override fun updateEnterprise(
        userId: String,
        projectId: String,
        certId: String,
        certRemark: String?,
        mpInputStream: InputStream?,
        mpDisposition: FormDataContentDisposition?
    ) {
        validatePermission(userId, projectId, certId, AuthPermission.EDIT, "用户($userId)在工程($projectId)下没有证书编辑权限")
        if (!certDao.has(dslContext, projectId, certId)) {
            throw OperationException("名称${certId}不存在")
        }

        val certEnterpriseRecord = certEnterpriseDao.get(dslContext, projectId, certId)

        val mpFileContent = if (mpInputStream != null) read(mpInputStream) else null
        if (mpFileContent != null && mpFileContent.size > certMaxSize) {
            throw OperationException("证书描述文件大小不能超过64k")
        }

        val mpInfo = if (mpFileContent != null) MobileProvisionUtil.parse(mpFileContent)
                ?: throw OperationException("不合法的mobileprovision文件")
        else null

        val remark = certRemark ?: ""
        // 在更新操作中，当描述文件为空的时候，读取数据库中的数据
        val mpFileName = if (mpDisposition != null) String(mpDisposition.fileName.toByteArray(Charset.forName("ISO-8859-1"))) else certEnterpriseRecord.certMpFileName
        val mpEncryptedFileContent = if (mpFileContent != null) AESUtil.encrypt(aesKey, mpFileContent) else certEnterpriseRecord.certMpFileContent
        val developerName = mpInfo?.name ?: certEnterpriseRecord.certDeveloperName
        val teamName = mpInfo?.teamName ?: certEnterpriseRecord.certTeamName
        val uuid = mpInfo?.uuid ?: certEnterpriseRecord.certUuid
        val expireDate = mpInfo?.expireDate ?: certEnterpriseRecord.certExpireDate

        // 只保存描述文件，其他置空
        val jksFileName = ""
        val jksEncryptedFileContent = ByteArray(0)
        val jksAlias = ""
        val jksAliasCredentialId = ""
        val p12FileName = ""
        val p12EncryptedFileContent = ByteArray(0)
        val credentialId = null

        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            certDao.update(
                    transactionContext,
                    projectId,
                    certId,
                    userId,
                    remark,
                    p12FileName,
                    p12EncryptedFileContent,
                    mpFileName,
                    mpEncryptedFileContent,
                    jksFileName,
                    jksEncryptedFileContent,
                    jksAlias,
                    jksAliasCredentialId,
                    developerName,
                    teamName,
                    uuid,
                    expireDate,
                    credentialId
            )
            certEnterpriseDao.update(
                    transactionContext,
                    projectId,
                    certId,
                    mpFileName,
                    mpEncryptedFileContent,
                    developerName,
                    teamName,
                    uuid,
                    expireDate
            )
        }
    }

    override fun uploadAndroid(
        userId: String,
        projectId: String,
        certId: String,
        certRemark: String?,
        credentialId: String,
        alias: String,
        aliasCredentialId: String,
        inputStream: InputStream,
        disposition: FormDataContentDisposition
    ) {
        validatePermission(userId, projectId, AuthPermission.CREATE, "用户($userId)在工程($projectId)下没有证书创建权限")
        credentialService.validatePermission(
                userId,
                projectId,
                credentialId,
                AuthPermission.USE,
                "用户($userId)在工程($projectId)下没有凭据($credentialId)的使用权限"
        )
        credentialService.validatePermission(
                userId,
                projectId,
                aliasCredentialId,
                AuthPermission.USE,
                "用户($userId)在工程($projectId)下没有凭据($aliasCredentialId)的使用权限"
        )

        if (certDao.has(dslContext, projectId, certId)) {
            throw OperationException("证书${certId}已存在")
        }

        val jksFileContent = read(inputStream)
        if (jksFileContent.size > certMaxSize) {
            throw OperationException("JKS文件大小不能超过64k")
        }

        val credential = credentialService.serviceGet(projectId, credentialId)
        if (!CertUtil.validJksPassword(jksFileContent, credential.v1)) {
            throw OperationException("证书密码错误")
        }

        val aliasCredential = credentialService.serviceGet(projectId, aliasCredentialId)
        if (!CertUtil.validJksAlias(jksFileContent, credential.v1, alias, aliasCredential.v1)) {
            throw OperationException("证书别名或者别名密码错误")
        }

        val remark = certRemark ?: ""
        val certType = CertType.ANDROID.value
        val jksFileName = String(disposition.fileName.toByteArray(Charset.forName("ISO-8859-1")))
        val jksEncryptedFileContent = AESUtil.encrypt(aesKey, jksFileContent)
        val p12FileName = ""
        val p12EncryptedFileContent = ByteArray(0)
        val mpFileName = ""
        val mpEncryptedFileContent = ByteArray(0)
        val developerName = ""
        val teamName = ""
        val uuid = ""
        val jksInfo = CertUtil.parseJks(jksFileContent, credential.v1, alias, aliasCredential.v1)
        val expireDate = jksInfo.expireDate

        createResource(userId, projectId, certId)
        certDao.create(
                dslContext,
                projectId,
                certId,
                userId,
                certType,
                remark,
                p12FileName,
                p12EncryptedFileContent,
                mpFileName,
                mpEncryptedFileContent,
                jksFileName,
                jksEncryptedFileContent,
                alias,
                aliasCredentialId,
                developerName,
                teamName,
                uuid,
                expireDate,
                credentialId
        )
    }

    override fun updateAndroid(
        userId: String,
        projectId: String,
        certId: String,
        certRemark: String?,
        credentialId: String,
        alias: String,
        aliasCredentialId: String,
        inputStream: InputStream?,
        disposition: FormDataContentDisposition?
    ) {
        validatePermission(userId, projectId, certId, AuthPermission.EDIT, "用户($userId)在工程($projectId)下没有证书编辑权限")
        val certRecord = certDao.getOrNull(dslContext, projectId, certId)
        certRecord ?: throw OperationException("证书${certId}不存在")

        credentialService.validatePermission(
                userId,
                projectId,
                credentialId,
                AuthPermission.USE,
                "用户($userId)在工程($projectId)下没有凭据($credentialId)的使用权限"
        )

        credentialService.validatePermission(
                userId,
                projectId,
                aliasCredentialId,
                AuthPermission.USE,
                "用户($userId)在工程($projectId)下没有凭据($aliasCredentialId)的使用权限"
        )

        val jksFileContent = if (inputStream != null) read(inputStream) else AESUtil.decrypt(aesKey, certRecord.certJksFileContent)
        if (jksFileContent.size > certMaxSize) {
            throw OperationException("JKS文件大小不能超过64k")
        }

        val credential = credentialService.serviceGet(projectId, credentialId)
        if (!CertUtil.validJksPassword(jksFileContent, credential.v1)) {
            throw OperationException("证书密码错误")
        }

        val aliasCredential = credentialService.serviceGet(projectId, aliasCredentialId)
        if (!CertUtil.validJksAlias(jksFileContent, credential.v1, alias, aliasCredential.v1)) {
            throw OperationException("证书别名或者别名密码错误")
        }

        val remark = certRemark ?: ""
        val jksFileName = if (disposition != null) String(disposition.fileName.toByteArray(Charset.forName("ISO-8859-1"))) else null
        val jksEncryptedFileContent = if (jksFileContent != null) AESUtil.encrypt(aesKey, jksFileContent) else null
        val p12FileName = ""
        val p12EncryptedFileContent = ByteArray(0)
        val mpFileName = ""
        val mpEncryptedFileContent = ByteArray(0)
        val developerName = ""
        val teamName = ""
        val uuid = ""
        val jksInfo = CertUtil.parseJks(jksFileContent, credential.v1, alias, aliasCredential.v1)
        val expireDate = jksInfo.expireDate

        certDao.update(
                dslContext,
                projectId,
                certId,
                userId,
                remark,
                p12FileName,
                p12EncryptedFileContent,
                mpFileName,
                mpEncryptedFileContent,
                jksFileName,
                jksEncryptedFileContent,
                alias,
                aliasCredentialId,
                developerName,
                teamName,
                uuid,
                expireDate,
                credentialId
        )
    }

    override fun uploadTls(
        userId: String,
        projectId: String,
        certId: String,
        certRemark: String?,
        serverCrtInputStream: InputStream,
        serverCrtDisposition: FormDataContentDisposition,
        serverKeyInputStream: InputStream,
        serverKeyDisposition: FormDataContentDisposition,
        clientCrtInputStream: InputStream?,
        clientCrtDisposition: FormDataContentDisposition?,
        clientKeyInputStream: InputStream?,
        clientKeyDisposition: FormDataContentDisposition?
    ) {
        validatePermission(userId, projectId, AuthPermission.CREATE, "用户($userId)在工程($projectId)下没有证书创建权限")
        if (certDao.has(dslContext, projectId, certId)) {
            throw OperationException("证书${certId}已被他人使用")
        }
        if (certId.length > certIdMaxSize) {
            throw OperationException("证书名称不能超过32位")
        }

        val serverCrtFileName = String(serverCrtDisposition.fileName.toByteArray(Charset.forName("ISO-8859-1")))
        val serverCrtFile = AESUtil.encrypt(aesKey, read(serverCrtInputStream))
        val serverKeyFileName = String(serverKeyDisposition.fileName.toByteArray(Charset.forName("ISO-8859-1")))
        val serverKeyFile = AESUtil.encrypt(aesKey, read(serverKeyInputStream))

        val clientCrtFileName = if (clientCrtDisposition != null) {
            String(clientCrtDisposition.fileName.toByteArray(Charset.forName("ISO-8859-1")))
        } else {
            null
        }
        val clientCrtFile = if (clientCrtInputStream != null) {
            AESUtil.encrypt(aesKey, read(clientCrtInputStream))
        } else {
            null
        }
        val clientKeyFileName = if (clientKeyDisposition != null) {
            String(clientKeyDisposition.fileName.toByteArray(Charset.forName("ISO-8859-1")))
        } else {
            null
        }
        val clientKeyFile = if (clientKeyInputStream != null) {
            AESUtil.encrypt(aesKey, read(clientKeyInputStream))
        } else {
            null
        }

        if (serverCrtFile.size > certMaxSize || serverKeyFile.size > certMaxSize ||
                (clientCrtFile != null && clientCrtFile.size > certMaxSize) ||
                (clientKeyFile != null && clientKeyFile.size > certMaxSize)
        ) {
            throw OperationException("文件大小不能超过64k")
        }

        val remark = certRemark ?: ""
        val certType = CertType.TLS.value
        val p12FileName = ""
        val p12EncryptedFileContent = ByteArray(0)
        val mpFileName = ""
        val mpEncryptedFileContent = ByteArray(0)
        val jksFileName = ""
        val jksEncryptedFileContent = ByteArray(0)
        val jksAlias = ""
        val jksAliasCredentialId = ""
        val developerName = ""
        val teamName = ""
        val uuid = ""
        val expireDate = LocalDateTime.now().plusYears(10L)
        val credentialId = null

        createResource(userId, projectId, certId)
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            certDao.create(
                    transactionContext,
                    projectId,
                    certId,
                    userId,
                    certType,
                    remark,
                    p12FileName,
                    p12EncryptedFileContent,
                    mpFileName,
                    mpEncryptedFileContent,
                    jksFileName,
                    jksEncryptedFileContent,
                    jksAlias,
                    jksAliasCredentialId,
                    developerName,
                    teamName,
                    uuid,
                    expireDate,
                    credentialId
            )
            certTlsDao.create(
                    transactionContext,
                    projectId,
                    certId,
                    serverCrtFileName,
                    serverCrtFile,
                    serverKeyFileName,
                    serverKeyFile,
                    clientCrtFileName,
                    clientCrtFile,
                    clientKeyFileName,
                    clientKeyFile
            )
        }
    }

    override fun updateTls(
        userId: String,
        projectId: String,
        certId: String,
        certRemark: String?,
        serverCrtInputStream: InputStream?,
        serverCrtDisposition: FormDataContentDisposition?,
        serverKeyInputStream: InputStream?,
        serverKeyDisposition: FormDataContentDisposition?,
        clientCrtInputStream: InputStream?,
        clientCrtDisposition: FormDataContentDisposition?,
        clientKeyInputStream: InputStream?,
        clientKeyDisposition: FormDataContentDisposition?
    ) {
        validatePermission(userId, projectId, certId, AuthPermission.EDIT, "用户($userId)在工程($projectId)下没有证书编辑权限")
        if (!certDao.has(dslContext, projectId, certId)) {
            throw OperationException("证书${certId}不存在")
        }
        val certTlsRecord = certTlsDao.get(dslContext, projectId, certId)

        val serverCrtFileName = if (serverCrtDisposition != null) {
            String(serverCrtDisposition.fileName.toByteArray(Charset.forName("ISO-8859-1")))
        } else {
            certTlsRecord.certServerCrtFileName
        }
        val serverCrtFile = if (serverCrtInputStream != null) {
            AESUtil.encrypt(aesKey, read(serverCrtInputStream))
        } else {
            certTlsRecord.certServerCrtFile
        }
        val serverKeyFileName = if (serverKeyDisposition != null) {
            String(serverKeyDisposition.fileName.toByteArray(Charset.forName("ISO-8859-1")))
        } else {
            certTlsRecord.certServerKeyFileName
        }
        val serverKeyFile = if (serverKeyInputStream != null) {
            AESUtil.encrypt(aesKey, read(serverKeyInputStream))
        } else {
            certTlsRecord.certServerKeyFile
        }

        val clientCrtFileName = if (clientCrtDisposition != null) {
            String(clientCrtDisposition.fileName.toByteArray(Charset.forName("ISO-8859-1")))
        } else {
            certTlsRecord.certClientCrtFileName
        }
        val clientCrtFile = if (clientCrtInputStream != null) {
            AESUtil.encrypt(aesKey, read(clientCrtInputStream))
        } else {
            certTlsRecord.certClientCrtFile
        }
        val clientKeyFileName = if (clientKeyDisposition != null) {
            String(clientKeyDisposition.fileName.toByteArray(Charset.forName("ISO-8859-1")))
        } else {
            certTlsRecord.certClientKeyFileName
        }
        val clientKeyFile = if (clientKeyInputStream != null) {
            AESUtil.encrypt(aesKey, read(clientKeyInputStream))
        } else {
            certTlsRecord.certClientKeyFile
        }

        if (serverCrtFile.size > certMaxSize || serverKeyFile.size > certMaxSize ||
                (clientCrtFile != null && clientCrtFile.size > certMaxSize) ||
                (clientKeyFile != null && clientKeyFile.size > certMaxSize)
        ) {
            throw OperationException("文件大小不能超过64k")
        }

        val remark = certRemark ?: ""
        val p12FileName = ""
        val p12EncryptedFileContent = ByteArray(0)
        val mpFileName = ""
        val mpEncryptedFileContent = ByteArray(0)
        val jksFileName = ""
        val jksEncryptedFileContent = ByteArray(0)
        val jksAlias = ""
        val jksAliasCredentialId = ""
        val developerName = ""
        val teamName = ""
        val uuid = ""
        val expireDate = LocalDateTime.now().plusYears(10L)
        val credentialId = null

        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            certDao.update(
                transactionContext,
                projectId,
                certId,
                userId,
                remark,
                p12FileName,
                p12EncryptedFileContent,
                mpFileName,
                mpEncryptedFileContent,
                jksFileName,
                jksEncryptedFileContent,
                jksAlias,
                jksAliasCredentialId,
                developerName,
                teamName,
                uuid,
                expireDate,
                credentialId
            )
            certTlsDao.update(
                transactionContext,
                projectId,
                certId,
                serverCrtFileName,
                serverCrtFile,
                serverKeyFileName,
                serverKeyFile,
                clientCrtFileName,
                clientCrtFile,
                clientKeyFileName,
                clientKeyFile
            )
        }
    }

    override fun delete(userId: String, projectId: String, certId: String) {
        validatePermission(
                userId,
                projectId,
                certId,
                AuthPermission.DELETE,
                "用户($userId)在工程($projectId)下没有证书($certId)的删除权限"
        )

        deleteResource(projectId, certId)
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            certDao.delete(transactionContext, projectId, certId)
            certTlsDao.delete(transactionContext, projectId, certId)
            certEnterpriseDao.delete(transactionContext, projectId, certId)
        }
    }

    override fun list(
        userId: String,
        projectId: String,
        certType: String?,
        offset: Int,
        limit: Int
    ): SQLPage<CertWithPermission> {
        val permissionToListMap = filterCerts(userId, projectId, setOf(AuthPermission.LIST, AuthPermission.DELETE, AuthPermission.EDIT))
        val hasListPermissionCertIdList = permissionToListMap[AuthPermission.LIST]!!
        val hasDeletePermissionCertIdList = permissionToListMap[AuthPermission.DELETE]!!
        val hasEditPermissionCertIdList = permissionToListMap[AuthPermission.EDIT]!!

        logger.info("$permissionToListMap $hasListPermissionCertIdList $hasDeletePermissionCertIdList")

        val count = certDao.countByProject(dslContext, projectId, certType, hasListPermissionCertIdList.toSet())
        val certRecordList =
                certDao.listByProject(dslContext, projectId, certType, hasListPermissionCertIdList.toSet(), offset, limit)
        val certList = certRecordList.map {
            val hasDeletePermission = hasDeletePermissionCertIdList.contains(it.certId)
            val hasEditPermission = hasEditPermissionCertIdList.contains(it.certId)
            CertWithPermission(
                it.certId,
                it.certType,
                it.certUserId,
                it.certRemark,
                it.certCreateTime.timestamp(),
                it.certExpireDate.timestamp(),
                it.credentialId ?: "",
                it.certJksAlias ?: "",
                it.certJksAliasCredentialId ?: "",
                CertPermissions(hasDeletePermission, hasEditPermission)
            )
        }
        return SQLPage(count, certList)
    }

    override fun hasPermissionList(
        userId: String,
        projectId: String,
        certType: String?,
        bkAuthPermission: AuthPermission,
        offset: Int,
        limit: Int
//            ,isCommonEnterprise: Boolean?
    ): SQLPage<Cert> {
        val hasPermissionCertIdList = filterCert(userId, projectId, bkAuthPermission)

        var count = certDao.countByProject(dslContext, projectId, certType, hasPermissionCertIdList.toSet())
        val certRecordList =
                certDao.listByProject(dslContext, projectId, certType, hasPermissionCertIdList.toSet(), offset, limit)
        val certList = certRecordList.map {
            Cert(
                it.certId,
                it.certType,
                it.certUserId,
                it.certRemark,
                it.certCreateTime.timestamp(),
                it.certExpireDate.timestamp(),
                it.credentialId ?: ""
            )
        }
//        // 当访问的是企业签名证书的时候，并且需要通用描述文件的时候,过期时间
//        if (isCommonEnterprise != null && isCommonEnterprise == true && certType != null && certType == "enterprise") {
//            certList.add(
//                    0,
//                    Cert(
//                            "企业证书通用描述",
//                            "enterprise",
//                            "devops",
//                            "企业证书通用描述",
//                            0L,
//                            0L,
//                            ""
//                    )
//            )
//            count++
//        }
        return SQLPage(count, certList)
    }

    override fun getIos(projectId: String, certId: String): CertIOSInfo {
        val certRecord = certDao.get(dslContext, projectId, certId)
        return CertIOSInfo(
            certId,
            certRecord.certP12FileName,
            certRecord.certMpFileName,
            certRecord.credentialId,
            certRecord.certRemark
        )
    }

    override fun getEnterprise(projectId: String, certId: String): CertEnterpriseInfo {
        val certRecord = certDao.get(dslContext, projectId, certId)
        return CertEnterpriseInfo(
            certId,
            certRecord.certMpFileName,
            certRecord.certRemark
        )
    }

    override fun getAndroid(projectId: String, certId: String): CertAndroidInfo {
        val certRecord = certDao.get(dslContext, projectId, certId)
        return CertAndroidInfo(
            certId,
            certRecord.certJksFileName,
            certRecord.credentialId,
            certRecord.certJksAlias,
            certRecord.certJksAliasCredentialId,
            certRecord.certRemark
        )
    }

    override fun getTls(projectId: String, certId: String): CertTlsInfo {
        val certRecord = certDao.get(dslContext, projectId, certId)
        val certTlsRecord = certTlsDao.get(dslContext, projectId, certId)
        return CertTlsInfo(
            certId,
            certTlsRecord.certServerCrtFileName,
            certTlsRecord.certServerKeyFileName,
            certTlsRecord.certClientCrtFileName,
            certTlsRecord.certClientKeyFileName,
            certRecord.certRemark
        )
    }

    override fun queryIos(buildId: String, certId: String, publicKey: String): CertIOS {
        val buildBasicInfoResult = client.get(ServiceBuildResource::class).serviceBasic(buildId)
        if (buildBasicInfoResult.isNotOk()) {
            throw RemoteServiceException("Failed to build the basic information based on the buildId")
        }
        val buildBasicInfo = buildBasicInfoResult.data
                ?: throw RemoteServiceException("Failed to build the basic information based on the buildId")

        val certRecord = certDao.get(dslContext, buildBasicInfo.projectId, certId)
        val publicKeyByteArray = Base64.getDecoder().decode(publicKey)
        val serverDHKeyPair = DHUtil.initKey(publicKeyByteArray)
        val serverPublicKeyByteArray = serverDHKeyPair.publicKey
        val serverPrivateKeyByteArray = serverDHKeyPair.privateKey
        val serverBase64PublicKey = String(Base64.getEncoder().encode(serverPublicKeyByteArray))

        val p12FileName = certRecord.certP12FileName
        val p12FileContent = AESUtil.decrypt(aesKey, certRecord.certP12FileContent)
        val p12Base64Content = encryptCert(p12FileContent, publicKeyByteArray, serverPrivateKeyByteArray)

        val mpFileName = certRecord.certMpFileName
        val mpFileContent = AESUtil.decrypt(aesKey, certRecord.certMpFileContent)
        val mpBase64Content = encryptCert(mpFileContent, publicKeyByteArray, serverPrivateKeyByteArray)

        val credentialId = certRecord.credentialId

        return CertIOS(serverBase64PublicKey, p12FileName, p12Base64Content, mpFileName, mpBase64Content, credentialId)
    }

    override fun queryEnterprise(buildId: String, certId: String, publicKey: String): CertEnterprise {
        val buildBasicInfoResult = client.get(ServiceBuildResource::class).serviceBasic(buildId)
        if (buildBasicInfoResult.isNotOk()) {
            throw RemoteServiceException("Failed to build the basic information based on the buildId")
        }
        val buildBasicInfo = buildBasicInfoResult.data
                ?: throw RemoteServiceException("Failed to build the basic information based on the buildId")

        val certRecord = certDao.get(dslContext, buildBasicInfo.projectId, certId)
        // 生成公钥和密钥
        val publicKeyByteArray = Base64.getDecoder().decode(publicKey)
        val serverDHKeyPair = DHUtil.initKey(publicKeyByteArray)
        val serverPublicKeyByteArray = serverDHKeyPair.publicKey
        val serverPrivateKeyByteArray = serverDHKeyPair.privateKey
        val serverBase64PublicKey = String(Base64.getEncoder().encode(serverPublicKeyByteArray))

        // 加密内容
        val mpFileName = certRecord.certMpFileName
        val mpFileContent = AESUtil.decrypt(aesKey, certRecord.certMpFileContent)
        val mpBase64Content = encryptCert(mpFileContent, publicKeyByteArray, serverPrivateKeyByteArray)
        val mpFileSha1 = ShaUtils.sha1(mpFileContent)

        return CertEnterprise(serverBase64PublicKey, mpFileName, mpBase64Content, mpFileSha1)
    }

    override fun queryEnterpriseByProject(projectId: String, certId: String, publicKey: String): CertEnterprise {
        val certRecord = certDao.get(dslContext, projectId, certId)
        // 生成公钥和密钥
        val publicKeyByteArray = Base64.getDecoder().decode(publicKey)
        val serverDHKeyPair = DHUtil.initKey(publicKeyByteArray)
        val serverPublicKeyByteArray = serverDHKeyPair.publicKey
        val serverPrivateKeyByteArray = serverDHKeyPair.privateKey
        val serverBase64PublicKey = String(Base64.getEncoder().encode(serverPublicKeyByteArray))

        // 加密内容
        val mpFileName = certRecord.certMpFileName
        val mpFileContent = AESUtil.decrypt(aesKey, certRecord.certMpFileContent)
        val mpBase64Content = encryptCert(mpFileContent, publicKeyByteArray, serverPrivateKeyByteArray)
        val mpFileSha1 = ShaUtils.sha1(mpFileContent)

        return CertEnterprise(serverBase64PublicKey, mpFileName, mpBase64Content, mpFileSha1)
    }

    override fun queryAndroid(buildId: String, certId: String, publicKey: String): CertAndroid {
        val buildBasicInfoResult = client.get(ServiceBuildResource::class).serviceBasic(buildId)
        if (buildBasicInfoResult.isNotOk()) {
            throw RemoteServiceException("Failed to build the basic information based on the buildId")
        }
        val buildBasicInfo = buildBasicInfoResult.data
                ?: throw RemoteServiceException("Failed to build the basic information based on the buildId")

        val certRecord = certDao.get(dslContext, buildBasicInfo.projectId, certId)
        val publicKeyByteArray = Base64.getDecoder().decode(publicKey)
        val serverDHKeyPair = DHUtil.initKey(publicKeyByteArray)
        val serverPublicKeyByteArray = serverDHKeyPair.publicKey
        val serverPrivateKeyByteArray = serverDHKeyPair.privateKey
        val serverBase64PublicKey = String(Base64.getEncoder().encode(serverPublicKeyByteArray))

        val jksFileName = certRecord.certJksFileName
        val jksFileContent = AESUtil.decrypt(aesKey, certRecord.certJksFileContent)
        val jksBase64Content = encryptCert(jksFileContent, publicKeyByteArray, serverPrivateKeyByteArray)

        val credentialId = certRecord.credentialId
        val alias = certRecord.certJksAlias
        val aliasCredentialId = certRecord.certJksAliasCredentialId

        val type = if (jksFileName.endsWith(".jks")) CertAndroidType.JKS else CertAndroidType.KEYSTORE

        return CertAndroid(
            serverBase64PublicKey,
            type,
            jksFileName,
            jksBase64Content,
            credentialId,
            alias,
            aliasCredentialId
        )
    }

    override fun queryAndroidByProject(projectId: String, certId: String, publicKey: String): CertAndroidWithCredential {
        val certRecord = certDao.get(dslContext, projectId, certId)
        val publicKeyByteArray = Base64.getDecoder().decode(publicKey)
        val serverDHKeyPair = DHUtil.initKey(publicKeyByteArray)
        val serverPublicKeyByteArray = serverDHKeyPair.publicKey
        val serverPrivateKeyByteArray = serverDHKeyPair.privateKey
        val serverBase64PublicKey = String(Base64.getEncoder().encode(serverPublicKeyByteArray))

        val jksFileName = certRecord.certJksFileName
        val jksFileContent = AESUtil.decrypt(aesKey, certRecord.certJksFileContent)
        val jksBase64Content = encryptCert(jksFileContent, publicKeyByteArray, serverPrivateKeyByteArray)
        val jksFileSha1 = ShaUtils.sha1(jksFileContent)

        val credentialId = certRecord.credentialId
        val credentialRecord = credentialService.serviceGet(projectId, credentialId)
        val credential = String(
            Base64.getEncoder().encode(
                DHUtil.encrypt(
                    credentialRecord.v1.toByteArray(),
                    publicKeyByteArray,
                    serverPrivateKeyByteArray
                )
            )
        )

        val alias = certRecord.certJksAlias
        val aliasCredentialId = certRecord.certJksAliasCredentialId
        val aliasCredentialRecord = credentialService.serviceGet(projectId, aliasCredentialId)
        val aliasCredential = String(
            Base64.getEncoder().encode(
                DHUtil.encrypt(
                    aliasCredentialRecord.v1.toByteArray(),
                    publicKeyByteArray,
                    serverPrivateKeyByteArray
                )
            )
        )

        val type = if (jksFileName.endsWith(".jks")) CertAndroidType.JKS else CertAndroidType.KEYSTORE

        return CertAndroidWithCredential(
            serverBase64PublicKey,
            type,
            jksFileName,
            jksBase64Content,
            jksFileSha1,
            credential,
            alias,
            aliasCredential
        )
    }

    override fun queryTlsByProject(projectId: String, certId: String, publicKey: String): CertTls {
        val certTlsRecord = certTlsDao.get(dslContext, projectId, certId)
        val publicKeyByteArray = Base64.getDecoder().decode(publicKey)
        val serverDHKeyPair = DHUtil.initKey(publicKeyByteArray)
        val serverPublicKeyByteArray = serverDHKeyPair.publicKey
        val serverPrivateKeyByteArray = serverDHKeyPair.privateKey
        val serverBase64PublicKey = String(Base64.getEncoder().encode(serverPublicKeyByteArray))

        val serverCrtFileName = certTlsRecord.certServerCrtFileName
        val serverCrtFile = AESUtil.decrypt(aesKey, certTlsRecord.certServerCrtFile)
        val serverBase64CrtFile = encryptCert(serverCrtFile, publicKeyByteArray, serverPrivateKeyByteArray)
        val serverCrtSha1 = ShaUtils.sha1(serverCrtFile)

        val serverKeyFileName = certTlsRecord.certServerKeyFileName
        val serverKeyFile = AESUtil.decrypt(aesKey, certTlsRecord.certServerKeyFile)
        val serverBase64KeyFile = encryptCert(serverKeyFile, publicKeyByteArray, serverPrivateKeyByteArray)
        val serverKeySha1 = ShaUtils.sha1(serverKeyFile)

        var clientCrtFileName: String? = null
        var clientBase64CrtFile: String? = null
        var clientCrtSha1: String? = null
        if (certTlsRecord.certClientCrtFile != null && certTlsRecord.certClientCrtFileName != null) {
            clientCrtFileName = certTlsRecord.certClientCrtFileName
            val clientCrtFile = AESUtil.decrypt(aesKey, certTlsRecord.certClientCrtFile)
            clientBase64CrtFile = encryptCert(clientCrtFile, publicKeyByteArray, serverPrivateKeyByteArray)
            clientCrtSha1 = ShaUtils.sha1(clientCrtFile)
        }

        var clientKeyFileName: String? = null
        var clientBase64KeyFile: String? = null
        var clientKeySha1: String? = null
        if (certTlsRecord.certClientKeyFile != null && certTlsRecord.certClientKeyFileName != null) {
            clientKeyFileName = certTlsRecord.certClientKeyFileName
            val clientKeyFile = AESUtil.decrypt(aesKey, certTlsRecord.certClientKeyFile)
            clientBase64KeyFile = encryptCert(clientKeyFile, publicKeyByteArray, serverPrivateKeyByteArray)
            clientKeySha1 = ShaUtils.sha1(clientKeyFile)
        }

        return CertTls(
            serverBase64PublicKey,
            serverCrtFileName,
            serverBase64CrtFile,
            serverCrtSha1,
            serverKeyFileName,
            serverBase64KeyFile,
            serverKeySha1,
            clientCrtFileName,
            clientBase64CrtFile,
            clientCrtSha1,
            clientKeyFileName,
            clientBase64KeyFile,
            clientKeySha1
        )
    }

    private fun encryptCert(
        cert: ByteArray,
        publicKeyByteArray: ByteArray,
        serverPrivateKeyByteArray: ByteArray
    ): String {
        val certEncryptedContent = DHUtil.encrypt(cert, publicKeyByteArray, serverPrivateKeyByteArray)
        return String(Base64.getEncoder().encode(certEncryptedContent))
    }

    private fun read(inputStream: InputStream): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        inputStream.copyTo(byteArrayOutputStream)
        return byteArrayOutputStream.toByteArray()
    }

    fun validatePermission(user: String, projectId: String, bkAuthPermission: AuthPermission, message: String) {
        if (!validatePermission(user, projectId, bkAuthPermission)) {
            throw CustomException(Response.Status.FORBIDDEN, message)
        }
    }

    fun validatePermission(
        user: String,
        projectId: String,
        resourceCode: String,
        bkAuthPermission: AuthPermission,
        message: String
    ) {
        if (!validatePermission(user, projectId, resourceCode, bkAuthPermission)) {
            throw CustomException(Response.Status.FORBIDDEN, message)
        }
    }

    private fun validatePermission(user: String, projectId: String, bkAuthPermission: AuthPermission): Boolean {
        return authPermissionApi.validateUserResourcePermission(
            user,
            serviceCode,
            resourceType,
            projectId,
            bkAuthPermission
        )
    }

    private fun validatePermission(
        user: String,
        projectId: String,
        resourceCode: String,
        bkAuthPermission: AuthPermission
    ): Boolean {
        return authPermissionApi.validateUserResourcePermission(
            user,
            serviceCode,
            resourceType,
            projectId,
            resourceCode,
            bkAuthPermission
        )
    }

    private fun filterCert(user: String, projectId: String, bkAuthPermission: AuthPermission): List<String> {
        return authPermissionApi.getUserResourceByPermission(
            user,
            serviceCode,
            resourceType,
            projectId,
            bkAuthPermission,
            null
        )
    }

    private fun filterCerts(
        user: String,
        projectId: String,
        bkAuthPermissions: Set<AuthPermission>
    ): Map<AuthPermission, List<String>> {
        return authPermissionApi.getUserResourcesByPermissions(
            user,
            serviceCode,
            resourceType,
            projectId,
            bkAuthPermissions,
            null
        )
    }

    private fun createResource(user: String, projectId: String, certId: String) {
        authResourceApi.createResource(user, serviceCode, resourceType, projectId, certId, certId)
    }

    private fun deleteResource(projectId: String, certId: String) {
        authResourceApi.deleteResource(serviceCode, resourceType, projectId, certId)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CertService::class.java)
    }
}

/*
fun main(args: Array<String>) {
    val clientPublicKey = "MEcwLQYJKoZIhvcNAQMBMCACExZWAhV0cUBBckkhWWg0c0IIBYcCBRI0VniQAgIAgAMWAAITCn86zqL29CQVMaTpvHCWbESa6Q=="
    val clientPrivateKey = "MEcCAQAwLQYJKoZIhvcNAQMBMCACExZWAhV0cUBBckkhWWg0c0IIBYcCBRI0VniQAgIAgAQTAhEA8UyMBVrTnzeudrtq9OU0hw=="
    println("Client PublicKey: $clientPublicKey")
    println("Client PrivateKey: $clientPrivateKey")

    val serverPublicKey = "MEcwLQYJKoZIhvcNAQMBMCACExZWAhV0cUBBckkhWWg0c0IIBYcCBRI0VniQAgIAgAMWAAITAnIcqjdIz9TqTpcSV3wTrhJv7w=="
    val serverContent = "K8vCZzN4vKI="
    println("Server PublicKey: $serverPublicKey")
    println("Encrypted: $serverContent")

    val serverPb = Base64.getDecoder().decode(serverPublicKey)
    val clientPi = Base64.getDecoder().decode(clientPrivateKey)
    val contentByteArray = Base64.getDecoder().decode(serverContent)

    val decryptContent = DHUtil.decrypt(contentByteArray, serverPb, clientPi)
    println("Decrypted: ${String(decryptContent)}")
}
*/
