/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.ticket.service

import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.util.DHUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.api.util.ShaUtils
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.ticket.constant.TicketMessageCode.CERTIFICATE_ALIAS_OR_PASSWORD_WRONG
import com.tencent.devops.ticket.constant.TicketMessageCode.CERTIFICATE_PASSWORD_WRONG
import com.tencent.devops.ticket.constant.TicketMessageCode.CERT_ALREADY_EXISTS
import com.tencent.devops.ticket.constant.TicketMessageCode.CERT_NOT_FOUND
import com.tencent.devops.ticket.constant.TicketMessageCode.CERT_USED_BY_OTHERS
import com.tencent.devops.ticket.constant.TicketMessageCode.FILE_SIZE_CANT_EXCEED
import com.tencent.devops.ticket.constant.TicketMessageCode.ILLEGAL_FILE
import com.tencent.devops.ticket.constant.TicketMessageCode.NAME_ALREADY_EXISTS
import com.tencent.devops.ticket.constant.TicketMessageCode.NAME_NO_EXISTS
import com.tencent.devops.ticket.constant.TicketMessageCode.NAME_SIZE_CANT_EXCEED
import com.tencent.devops.ticket.constant.TicketMessageCode.USER_NO_ENGINEERING_CERT_OPERATE_PERMISSIONS
import com.tencent.devops.ticket.constant.TicketMessageCode.USER_NO_ENGINEERING_CREDENTIAL_OPERATE_PERMISSIONS
import com.tencent.devops.ticket.dao.CertDao
import com.tencent.devops.ticket.dao.CertEnterpriseDao
import com.tencent.devops.ticket.dao.CertTlsDao
import com.tencent.devops.ticket.pojo.Cert
import com.tencent.devops.ticket.pojo.CertAndroid
import com.tencent.devops.ticket.pojo.CertAndroidInfo
import com.tencent.devops.ticket.pojo.CertAndroidWithCredential
import com.tencent.devops.ticket.pojo.CertEnterprise
import com.tencent.devops.ticket.pojo.CertEnterpriseInfo
import com.tencent.devops.ticket.pojo.CertIOS
import com.tencent.devops.ticket.pojo.CertIOSInfo
import com.tencent.devops.ticket.pojo.CertPermissions
import com.tencent.devops.ticket.pojo.CertTls
import com.tencent.devops.ticket.pojo.CertTlsInfo
import com.tencent.devops.ticket.pojo.CertWithPermission
import com.tencent.devops.ticket.pojo.enums.CertAndroidType
import com.tencent.devops.ticket.pojo.enums.CertType
import com.tencent.devops.ticket.util.MobileProvisionUtil
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.charset.Charset
import java.time.LocalDateTime
import java.util.Base64
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Suppress("ALL")
@Service
class CertServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val client: Client,
    private val certDao: CertDao,
    private val certTlsDao: CertTlsDao,
    private val certEnterpriseDao: CertEnterpriseDao,
    private val credentialService: CredentialService,
    private val certPermissionService: CertPermissionService,
    private val certHelper: CertHelper
) : CertService {
    private val certMaxSize = 64 * 1024
    private val certIdMaxSize = 32

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
        val create = AuthPermission.CREATE
        certPermissionService.validatePermission(
            userId,
            projectId,
            create,
            MessageUtil.getMessageByLocale(
                USER_NO_ENGINEERING_CERT_OPERATE_PERMISSIONS,
                I18nUtil.getLanguage(userId),
                arrayOf(
                    userId,
                    projectId,
                    "",
                    create.getI18n(I18nUtil.getLanguage(userId))
                )
            )
        )

        if (certCredentialId != null) {
            val use = AuthPermission.USE
            certPermissionService.validatePermission(
                userId = userId,
                projectId = projectId,
                resourceCode = certCredentialId,
                authPermission = use,
                message = MessageUtil.getMessageByLocale(
                    USER_NO_ENGINEERING_CREDENTIAL_OPERATE_PERMISSIONS,
                    I18nUtil.getLanguage(userId),
                    arrayOf(
                        userId,
                        projectId,
                        certCredentialId,
                        use.getI18n(I18nUtil.getLanguage(userId))
                    )
                )
            )
        }
        if (certDao.has(dslContext, projectId, certId)) {
            throw OperationException(
                MessageUtil.getMessageByLocale(NAME_ALREADY_EXISTS, I18nUtil.getLanguage(userId), arrayOf(certId))
            )
        }

        val p12FileContent = read(p12InputStream)
        val mpFileContent = read(mpInputStream)
        if (p12FileContent.size > certMaxSize) {
            throw OperationException(
                    MessageUtil.getMessageByLocale(
                        FILE_SIZE_CANT_EXCEED,
                        I18nUtil.getLanguage(userId),
                        arrayOf("p12", "64k")
                    )
            )
        }
        if (mpFileContent.size > certMaxSize) {
            throw OperationException(
                MessageUtil.getMessageByLocale(
                    FILE_SIZE_CANT_EXCEED,
                    I18nUtil.getLanguage(userId),
                    arrayOf("mobileprovision", "64k")
                )
            )
        }
        if (certId.length > certIdMaxSize) {
            throw OperationException(
                MessageUtil.getMessageByLocale(
                    NAME_SIZE_CANT_EXCEED,
                    I18nUtil.getLanguage(userId),
                    arrayOf("cert", "32bit")
                )
            )
        }

        val mpInfo = MobileProvisionUtil.parse(mpFileContent) ?: throw OperationException(
            MessageUtil.getMessageByLocale(ILLEGAL_FILE, I18nUtil.getLanguage(userId), arrayOf("mobileprovision"))
        )

        val credentialId = certCredentialId ?: ""
        val remark = certRemark ?: ""
        val certType = CertType.IOS.value
        val p12FileName = String(p12Disposition.fileName.toByteArray(Charset.forName("ISO-8859-1")))
        val p12EncryptedFileContent = certHelper.encryptBytes(p12FileContent)!!
        val mpFileName = String(mpDisposition.fileName.toByteArray(Charset.forName("ISO-8859-1")))
        val mpEncryptedFileContent = certHelper.encryptBytes(mpFileContent)!!
        val jksFileName = ""
        val jksEncryptedFileContent = ByteArray(0)
        val jksAlias = ""
        val jksAliasCredentialId = ""
        val developerName = mpInfo.name
        val teamName = mpInfo.teamName
        val uuid = mpInfo.uuid
        val expireDate = mpInfo.expireDate

        certPermissionService.createResource(userId, projectId, certId)
        certDao.create(
            dslContext = dslContext,
            projectId = projectId,
            certId = certId,
            certUserId = userId,
            certType = certType,
            certRemark = remark,
            certP12FileName = p12FileName,
            certP12FileContent = p12EncryptedFileContent,
            certMpFileName = mpFileName,
            certMpFileContent = mpEncryptedFileContent,
            certJksFileName = jksFileName,
            certJksFileContent = jksEncryptedFileContent,
            certJksAlias = jksAlias,
            certJksAliasCredentialId = jksAliasCredentialId,
            certDeveloperName = developerName,
            certTeamName = teamName,
            certUUID = uuid,
            certExpireDate = expireDate,
            credentialId = credentialId
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
        val edit = AuthPermission.EDIT
        certPermissionService.validatePermission(
            userId,
            projectId,
            certId,
            edit,
            MessageUtil.getMessageByLocale(
                USER_NO_ENGINEERING_CERT_OPERATE_PERMISSIONS,
                I18nUtil.getLanguage(userId),
                arrayOf(
                    userId,
                    projectId,
                    "",
                    edit.getI18n(I18nUtil.getLanguage(userId))
                )
            )
        )
        if (!certDao.has(dslContext, projectId, certId)) {
            throw OperationException(
                MessageUtil.getMessageByLocale(NAME_NO_EXISTS, I18nUtil.getLanguage(userId), arrayOf(certId))
            )
        }

        if (certCredentialId != null) {
            val use = AuthPermission.USE
            certPermissionService.validatePermission(
                userId = userId,
                projectId = projectId,
                resourceCode = certCredentialId,
                authPermission = use,
                message = MessageUtil.getMessageByLocale(
                    USER_NO_ENGINEERING_CREDENTIAL_OPERATE_PERMISSIONS,
                    I18nUtil.getLanguage(userId),
                    arrayOf(
                        userId,
                        projectId,
                        certCredentialId,
                        use.getI18n(I18nUtil.getLanguage(userId))
                    )
                )
            )
        }

        val p12FileContent = if (p12InputStream != null) read(p12InputStream) else null
        val mpFileContent = if (mpInputStream != null) read(mpInputStream) else null
        if (p12FileContent != null && p12FileContent.size > certMaxSize) {
            throw OperationException(
                MessageUtil.getMessageByLocale(
                    FILE_SIZE_CANT_EXCEED,
                    I18nUtil.getLanguage(userId),
                    arrayOf("p12", "64k")
                )
            )
        }
        if (mpFileContent != null && mpFileContent.size > certMaxSize) {
            throw OperationException(
                MessageUtil.getMessageByLocale(
                    FILE_SIZE_CANT_EXCEED,
                    I18nUtil.getLanguage(userId),
                    arrayOf("mobileprovision", "64k")
                )
            )
        }

        val mpInfo = if (mpFileContent != null) MobileProvisionUtil.parse(mpFileContent)
            ?: throw OperationException(
                MessageUtil.getMessageByLocale(ILLEGAL_FILE, I18nUtil.getLanguage(userId), arrayOf("mobileprovision"))
            )
        else null

        val credentialId = certCredentialId ?: ""
        val remark = certRemark ?: ""
        val p12FileName =
            if (p12Disposition != null) {
                String(p12Disposition.fileName.toByteArray(Charset.forName("ISO-8859-1")))
            } else null
        val p12EncryptedFileContent = certHelper.encryptBytes(p12FileContent)
        val mpFileName =
            if (mpDisposition != null) {
                String(mpDisposition.fileName.toByteArray(Charset.forName("ISO-8859-1")))
            } else null
        val mpEncryptedFileContent = if (mpFileContent != null) certHelper.encryptBytes(mpFileContent) else null
        val jksFileName = ""
        val jksEncryptedFileContent = ByteArray(0)
        val jksAlias = ""
        val jksAliasCredentialId = ""
        val developerName = mpInfo?.name
        val teamName = mpInfo?.teamName
        val uuid = mpInfo?.uuid
        val expireDate = mpInfo?.expireDate

        certDao.update(
            dslContext = dslContext,
            projectId = projectId,
            certId = certId,
            certUserId = userId,
            certRemark = remark,
            certP12FileName = p12FileName,
            certP12FileContent = p12EncryptedFileContent,
            certMpFileName = mpFileName,
            certMpFileContent = mpEncryptedFileContent,
            certJksFileName = jksFileName,
            certJksFileContent = jksEncryptedFileContent,
            certJksAlias = jksAlias,
            certJksAliasCredentialId = jksAliasCredentialId,
            certDeveloperName = developerName,
            certTeamName = teamName,
            certUUID = uuid,
            certExpireDate = expireDate,
            credentialId = credentialId
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
        val create = AuthPermission.CREATE
        certPermissionService.validatePermission(
            userId,
            projectId,
            create,
            MessageUtil.getMessageByLocale(
                USER_NO_ENGINEERING_CERT_OPERATE_PERMISSIONS,
                I18nUtil.getLanguage(userId),
                arrayOf(
                    userId,
                    projectId,
                    "",
                    create.getI18n(I18nUtil.getLanguage(userId))
                )
            )
        )

        if (certDao.has(dslContext, projectId, certId)) {
            throw OperationException(
                MessageUtil.getMessageByLocale(NAME_ALREADY_EXISTS, I18nUtil.getLanguage(userId), arrayOf(certId))
            )
        }

        val mpFileContent = read(mpInputStream)
        if (mpFileContent.size > certMaxSize) {
            throw OperationException(
                MessageUtil.getMessageByLocale(
                    FILE_SIZE_CANT_EXCEED,
                    I18nUtil.getLanguage(userId),
                    arrayOf("mobileprovision", "64k")
                )
            )
        }
        if (certId.length > certIdMaxSize) {
            throw OperationException(
                MessageUtil.getMessageByLocale(
                    NAME_SIZE_CANT_EXCEED,
                    I18nUtil.getLanguage(userId),
                    arrayOf("cert", "32bit")
                )
            )
        }

        val mpInfo = MobileProvisionUtil.parse(mpFileContent) ?: throw OperationException(
            MessageUtil.getMessageByLocale(ILLEGAL_FILE, I18nUtil.getLanguage(userId), arrayOf("mobileprovision"))
        )

        val remark = certRemark ?: ""
        val certType = CertType.ENTERPRISE.value
        val mpFileName = String(mpDisposition.fileName.toByteArray(Charset.forName("ISO-8859-1")))
        val mpEncryptedFileContent = certHelper.encryptBytes(mpFileContent)!!
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

        certPermissionService.createResource(userId, projectId, certId)
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            certDao.create(
                dslContext = transactionContext,
                projectId = projectId,
                certId = certId,
                certUserId = userId,
                certType = certType,
                certRemark = remark,
                certP12FileName = p12FileName,
                certP12FileContent = p12EncryptedFileContent,
                certMpFileName = mpFileName,
                certMpFileContent = mpEncryptedFileContent,
                certJksFileName = jksFileName,
                certJksFileContent = jksEncryptedFileContent,
                certJksAlias = jksAlias,
                certJksAliasCredentialId = jksAliasCredentialId,
                certDeveloperName = developerName,
                certTeamName = teamName,
                certUUID = uuid,
                certExpireDate = expireDate,
                credentialId = credentialId
            )
            certEnterpriseDao.create(
                dslContext = transactionContext,
                projectId = projectId,
                certId = certId,
                certMpFileName = mpFileName,
                certMpFileContent = mpEncryptedFileContent,
                certDeveloperName = developerName,
                certTeamName = teamName,
                certUUID = uuid,
                certExpireDate = expireDate
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
        val edit = AuthPermission.EDIT
        certPermissionService.validatePermission(
            userId,
            projectId,
            certId,
            edit,
            MessageUtil.getMessageByLocale(
                USER_NO_ENGINEERING_CERT_OPERATE_PERMISSIONS,
                I18nUtil.getLanguage(userId),
                arrayOf(
                    userId,
                    projectId,
                    "",
                    edit.getI18n(I18nUtil.getLanguage(userId))
                )
            )
        )
        if (!certDao.has(dslContext, projectId, certId)) {
            throw OperationException(
                MessageUtil.getMessageByLocale(NAME_ALREADY_EXISTS, I18nUtil.getLanguage(userId), arrayOf(certId))
            )
        }

        val certEnterpriseRecord = certEnterpriseDao.get(dslContext, projectId, certId)

        val mpFileContent = if (mpInputStream != null) read(mpInputStream) else null
        if (mpFileContent != null && mpFileContent.size > certMaxSize) {
            throw OperationException(
                MessageUtil.getMessageByLocale(NAME_ALREADY_EXISTS, I18nUtil.getLanguage(userId), arrayOf(certId))
            )
        }

        val mpInfo = if (mpFileContent != null) MobileProvisionUtil.parse(mpFileContent)
            ?: throw OperationException(
                MessageUtil.getMessageByLocale(ILLEGAL_FILE, I18nUtil.getLanguage(userId), arrayOf("mobileprovision"))
            )
        else null

        val remark = certRemark ?: ""
        // 在更新操作中，当描述文件为空的时候，读取数据库中的数据
        val mpFileName =
            if (mpDisposition != null) String(mpDisposition.fileName.toByteArray(Charset.forName("ISO-8859-1")))
            else certEnterpriseRecord.certMpFileName
        val mpEncryptedFileContent = certHelper.encryptBytes(mpFileContent) ?: certEnterpriseRecord.certMpFileContent
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
                dslContext = transactionContext,
                projectId = projectId,
                certId = certId,
                certUserId = userId,
                certRemark = remark,
                certP12FileName = p12FileName,
                certP12FileContent = p12EncryptedFileContent,
                certMpFileName = mpFileName,
                certMpFileContent = mpEncryptedFileContent,
                certJksFileName = jksFileName,
                certJksFileContent = jksEncryptedFileContent,
                certJksAlias = jksAlias,
                certJksAliasCredentialId = jksAliasCredentialId,
                certDeveloperName = developerName,
                certTeamName = teamName,
                certUUID = uuid,
                certExpireDate = expireDate,
                credentialId = credentialId
            )
            certEnterpriseDao.update(
                dslContext = transactionContext,
                projectId = projectId,
                certId = certId,
                certMpFileName = mpFileName,
                certMpFileContent = mpEncryptedFileContent,
                certDeveloperName = developerName,
                certTeamName = teamName,
                certUUID = uuid,
                certExpireDate = expireDate
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
        val create = AuthPermission.CREATE
        certPermissionService.validatePermission(
            userId,
            projectId,
            create,
            MessageUtil.getMessageByLocale(
                USER_NO_ENGINEERING_CERT_OPERATE_PERMISSIONS,
                I18nUtil.getLanguage(userId),
                arrayOf(
                    userId,
                    projectId,
                    "",
                    create.getI18n(I18nUtil.getLanguage(userId))
                )
            )
        )
        val use = AuthPermission.USE
        certPermissionService.validatePermission(
            userId = userId,
            projectId = projectId,
            resourceCode = credentialId,
            authPermission = use,
            message = MessageUtil.getMessageByLocale(
                USER_NO_ENGINEERING_CREDENTIAL_OPERATE_PERMISSIONS,
                I18nUtil.getLanguage(userId),
                arrayOf(
                    userId,
                    projectId,
                    credentialId,
                    use.getI18n(I18nUtil.getLanguage(userId))
                )
            )
        )
        certPermissionService.validatePermission(
            userId = userId,
            projectId = projectId,
            resourceCode = aliasCredentialId,
            authPermission = AuthPermission.USE,
            message = MessageUtil.getMessageByLocale(
                USER_NO_ENGINEERING_CREDENTIAL_OPERATE_PERMISSIONS,
                I18nUtil.getLanguage(userId),
                arrayOf(
                    userId,
                    projectId,
                    aliasCredentialId,
                    use.getI18n(I18nUtil.getLanguage(userId))
                )
            )
        )

        if (certDao.has(dslContext, projectId, certId)) {
            throw OperationException(
                MessageUtil.getMessageByLocale(CERT_ALREADY_EXISTS, I18nUtil.getLanguage(userId), arrayOf(certId))
            )
        }

        val jksFileContent = read(inputStream)
        if (jksFileContent.size > certMaxSize) {
            throw OperationException(
                MessageUtil.getMessageByLocale(
                    FILE_SIZE_CANT_EXCEED,
                    I18nUtil.getLanguage(userId),
                    arrayOf("JKS", "64k")
                )
            )
        }

        val credential = credentialService.serviceGet(projectId, credentialId)
        if (!certHelper.validJksPassword(jksFileContent, credential.v1)) {
            throw OperationException(
                MessageUtil.getMessageByLocale(CERTIFICATE_PASSWORD_WRONG, I18nUtil.getLanguage(userId))
            )
        }

        val aliasCredential = credentialService.serviceGet(projectId, aliasCredentialId)
        if (!certHelper.validJksAlias(jksFileContent, credential.v1, alias, aliasCredential.v1)) {
            throw OperationException(
                MessageUtil.getMessageByLocale(CERTIFICATE_ALIAS_OR_PASSWORD_WRONG, I18nUtil.getLanguage(userId))
            )
        }

        val remark = certRemark ?: ""
        val certType = CertType.ANDROID.value
        val jksFileName = String(disposition.fileName.toByteArray(Charset.forName("ISO-8859-1")))
        val jksEncryptedFileContent = certHelper.encryptBytes(jksFileContent)!!
        val p12FileName = ""
        val p12EncryptedFileContent = ByteArray(0)
        val mpFileName = ""
        val mpEncryptedFileContent = ByteArray(0)
        val developerName = ""
        val teamName = ""
        val uuid = ""
        val jksInfo = certHelper.parseJks(jksFileContent, credential.v1, alias, aliasCredential.v1)
        val expireDate = jksInfo.expireDate

        certPermissionService.createResource(userId, projectId, certId)
        certDao.create(
            dslContext = dslContext,
            projectId = projectId,
            certId = certId,
            certUserId = userId,
            certType = certType,
            certRemark = remark,
            certP12FileName = p12FileName,
            certP12FileContent = p12EncryptedFileContent,
            certMpFileName = mpFileName,
            certMpFileContent = mpEncryptedFileContent,
            certJksFileName = jksFileName,
            certJksFileContent = jksEncryptedFileContent,
            certJksAlias = alias,
            certJksAliasCredentialId = aliasCredentialId,
            certDeveloperName = developerName,
            certTeamName = teamName,
            certUUID = uuid,
            certExpireDate = expireDate,
            credentialId = credentialId
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
        val edit = AuthPermission.EDIT
        certPermissionService.validatePermission(
            userId = userId,
            projectId = projectId,
            resourceCode = certId,
            authPermission = edit,
            message = MessageUtil.getMessageByLocale(
                USER_NO_ENGINEERING_CERT_OPERATE_PERMISSIONS,
                I18nUtil.getLanguage(userId),
                arrayOf(
                    userId,
                    projectId,
                    "",
                    edit.getI18n(I18nUtil.getLanguage(userId))
                )
            )
        )

        val certRecord = certDao.getOrNull(dslContext, projectId, certId)
            ?: throw OperationException(
                MessageUtil.getMessageByLocale(CERT_NOT_FOUND, I18nUtil.getLanguage(userId), arrayOf(certId))
            )

        val use = AuthPermission.USE
        certPermissionService.validatePermission(
            userId = userId,
            projectId = projectId,
            resourceCode = credentialId,
            authPermission = use,
            message = MessageUtil.getMessageByLocale(
                USER_NO_ENGINEERING_CREDENTIAL_OPERATE_PERMISSIONS,
                I18nUtil.getLanguage(userId),
                arrayOf(
                    userId,
                    projectId,
                    credentialId,
                    use.getI18n(I18nUtil.getLanguage(userId))
                )
            )
        )

        certPermissionService.validatePermission(
            userId = userId,
            projectId = projectId,
            resourceCode = aliasCredentialId,
            authPermission = use,
            message = MessageUtil.getMessageByLocale(
                USER_NO_ENGINEERING_CREDENTIAL_OPERATE_PERMISSIONS,
                I18nUtil.getLanguage(userId),
                arrayOf(
                    userId,
                    projectId,
                    aliasCredentialId,
                    use.getI18n(I18nUtil.getLanguage(userId))
                )
            )
        )

        val jksFileContent =
            if (inputStream != null) read(inputStream)
            else certHelper.decryptBytes(certRecord.certJksFileContent)!!
        if (jksFileContent.size > certMaxSize) {
            throw OperationException(
                MessageUtil.getMessageByLocale(
                    FILE_SIZE_CANT_EXCEED,
                    I18nUtil.getLanguage(userId),
                    arrayOf("JKS", "64k")
                )
            )
        }

        val credential = credentialService.serviceGet(projectId, credentialId)
        if (!certHelper.validJksPassword(jksFileContent, credential.v1)) {
            throw OperationException(
                MessageUtil.getMessageByLocale(CERTIFICATE_PASSWORD_WRONG, I18nUtil.getLanguage(userId))
            )
        }

        val aliasCredential = credentialService.serviceGet(projectId, aliasCredentialId)
        if (!certHelper.validJksAlias(jksFileContent, credential.v1, alias, aliasCredential.v1)) {
            throw OperationException(
                MessageUtil.getMessageByLocale(CERTIFICATE_ALIAS_OR_PASSWORD_WRONG, I18nUtil.getLanguage(userId))
            )
        }

        val remark = certRemark ?: ""
        val jksFileName =
            if (disposition != null) String(disposition.fileName.toByteArray(Charset.forName("ISO-8859-1")))
            else null
        val jksEncryptedFileContent = certHelper.encryptBytes(jksFileContent)
        val p12FileName = ""
        val p12EncryptedFileContent = ByteArray(0)
        val mpFileName = ""
        val mpEncryptedFileContent = ByteArray(0)
        val developerName = ""
        val teamName = ""
        val uuid = ""
        val jksInfo = certHelper.parseJks(jksFileContent, credential.v1, alias, aliasCredential.v1)
        val expireDate = jksInfo.expireDate

        certDao.update(
            dslContext = dslContext,
            projectId = projectId,
            certId = certId,
            certUserId = userId,
            certRemark = remark,
            certP12FileName = p12FileName,
            certP12FileContent = p12EncryptedFileContent,
            certMpFileName = mpFileName,
            certMpFileContent = mpEncryptedFileContent,
            certJksFileName = jksFileName,
            certJksFileContent = jksEncryptedFileContent,
            certJksAlias = alias,
            certJksAliasCredentialId = aliasCredentialId,
            certDeveloperName = developerName,
            certTeamName = teamName,
            certUUID = uuid,
            certExpireDate = expireDate,
            credentialId = credentialId
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
        val create = AuthPermission.CREATE
        certPermissionService.validatePermission(
            userId,
            projectId,
            create,
            MessageUtil.getMessageByLocale(
                USER_NO_ENGINEERING_CERT_OPERATE_PERMISSIONS,
                I18nUtil.getLanguage(userId),
                arrayOf(
                    userId,
                    projectId,
                    "",
                    create.getI18n(I18nUtil.getLanguage(userId))
                )
            )
        )
        if (certDao.has(dslContext, projectId, certId)) {
            throw OperationException(
                MessageUtil.getMessageByLocale(CERT_USED_BY_OTHERS, I18nUtil.getLanguage(userId), arrayOf(certId))
            )
        }
        if (certId.length > certIdMaxSize) {
            throw OperationException(
                MessageUtil.getMessageByLocale(
                    NAME_SIZE_CANT_EXCEED,
                    I18nUtil.getLanguage(userId),
                    arrayOf("cert", "32bit")
                )
            )
        }

        val serverCrtFileName = String(serverCrtDisposition.fileName.toByteArray(Charset.forName("ISO-8859-1")))
        val serverCrtFile = certHelper.encryptBytes(read(serverCrtInputStream))!!
        val serverKeyFileName = String(serverKeyDisposition.fileName.toByteArray(Charset.forName("ISO-8859-1")))
        val serverKeyFile = certHelper.encryptBytes(read(serverKeyInputStream))!!

        val clientCrtFileName = if (clientCrtDisposition != null) {
            String(clientCrtDisposition.fileName.toByteArray(Charset.forName("ISO-8859-1")))
        } else {
            null
        }
        val clientCrtFile = if (clientCrtInputStream != null) {
            certHelper.encryptBytes(read(clientCrtInputStream))
        } else {
            null
        }
        val clientKeyFileName = if (clientKeyDisposition != null) {
            String(clientKeyDisposition.fileName.toByteArray(Charset.forName("ISO-8859-1")))
        } else {
            null
        }
        val clientKeyFile = if (clientKeyInputStream != null) {
            certHelper.encryptBytes(read(clientKeyInputStream))
        } else {
            null
        }

        if (serverCrtFile.size > certMaxSize || serverKeyFile.size > certMaxSize ||
            (clientCrtFile != null && clientCrtFile.size > certMaxSize) ||
            (clientKeyFile != null && clientKeyFile.size > certMaxSize)
        ) {
            throw OperationException(
                MessageUtil.getMessageByLocale(
                    FILE_SIZE_CANT_EXCEED,
                    I18nUtil.getLanguage(userId),
                    arrayOf("", "64k")
                )
            )
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

        certPermissionService.createResource(userId, projectId, certId)
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            certDao.create(
                dslContext = transactionContext,
                projectId = projectId,
                certId = certId,
                certUserId = userId,
                certType = certType,
                certRemark = remark,
                certP12FileName = p12FileName,
                certP12FileContent = p12EncryptedFileContent,
                certMpFileName = mpFileName,
                certMpFileContent = mpEncryptedFileContent,
                certJksFileName = jksFileName,
                certJksFileContent = jksEncryptedFileContent,
                certJksAlias = jksAlias,
                certJksAliasCredentialId = jksAliasCredentialId,
                certDeveloperName = developerName,
                certTeamName = teamName,
                certUUID = uuid,
                certExpireDate = expireDate,
                credentialId = credentialId
            )
            certTlsDao.create(
                dslContext = transactionContext,
                projectId = projectId,
                certId = certId,
                serverCrtFileName = serverCrtFileName,
                serverCrtFile = serverCrtFile,
                serverKeyFileName = serverKeyFileName,
                serverKeyFile = serverKeyFile,
                clientCrtFileName = clientCrtFileName,
                clientCrtFile = clientCrtFile,
                clientKeyFileName = clientKeyFileName,
                clientKeyFile = clientKeyFile
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
        val edit = AuthPermission.EDIT
        certPermissionService.validatePermission(
            userId = userId,
            projectId = projectId,
            resourceCode = certId,
            authPermission = edit,
            message = MessageUtil.getMessageByLocale(
                USER_NO_ENGINEERING_CERT_OPERATE_PERMISSIONS,
                I18nUtil.getLanguage(userId),
                arrayOf(
                    userId,
                    projectId,
                    "",
                    edit.getI18n(I18nUtil.getLanguage(userId))
                )
            )
        )
        if (!certDao.has(dslContext, projectId, certId)) {
            throw OperationException(
                MessageUtil.getMessageByLocale(CERT_NOT_FOUND, I18nUtil.getLanguage(userId), arrayOf(certId))
            )
        }
        val certTlsRecord = certTlsDao.get(dslContext, projectId, certId)

        val serverCrtFileName = if (serverCrtDisposition != null) {
            String(serverCrtDisposition.fileName.toByteArray(Charset.forName("ISO-8859-1")))
        } else {
            certTlsRecord.certServerCrtFileName
        }
        val serverCrtFile = if (serverCrtInputStream != null) {
            certHelper.encryptBytes(read(serverCrtInputStream))!!
        } else {
            certTlsRecord.certServerCrtFile
        }
        val serverKeyFileName = if (serverKeyDisposition != null) {
            String(serverKeyDisposition.fileName.toByteArray(Charset.forName("ISO-8859-1")))
        } else {
            certTlsRecord.certServerKeyFileName
        }
        val serverKeyFile = if (serverKeyInputStream != null) {
            certHelper.encryptBytes(read(serverKeyInputStream))!!
        } else {
            certTlsRecord.certServerKeyFile
        }

        val clientCrtFileName = if (clientCrtDisposition != null) {
            String(clientCrtDisposition.fileName.toByteArray(Charset.forName("ISO-8859-1")))
        } else {
            certTlsRecord.certClientCrtFileName
        }
        val clientCrtFile = if (clientCrtInputStream != null) {
            certHelper.encryptBytes(read(clientCrtInputStream))!!
        } else {
            certTlsRecord.certClientCrtFile
        }
        val clientKeyFileName = if (clientKeyDisposition != null) {
            String(clientKeyDisposition.fileName.toByteArray(Charset.forName("ISO-8859-1")))
        } else {
            certTlsRecord.certClientKeyFileName
        }
        val clientKeyFile = if (clientKeyInputStream != null) {
            certHelper.encryptBytes(read(clientKeyInputStream))!!
        } else {
            certTlsRecord.certClientKeyFile
        }

        if (serverCrtFile.size > certMaxSize || serverKeyFile.size > certMaxSize ||
            (clientCrtFile != null && clientCrtFile.size > certMaxSize) ||
            (clientKeyFile != null && clientKeyFile.size > certMaxSize)
        ) {
            throw OperationException(
                MessageUtil.getMessageByLocale(
                    FILE_SIZE_CANT_EXCEED,
                    I18nUtil.getLanguage(userId),
                    arrayOf("", "64k")
                )
            )
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
                dslContext = transactionContext,
                projectId = projectId,
                certId = certId,
                certUserId = userId,
                certRemark = remark,
                certP12FileName = p12FileName,
                certP12FileContent = p12EncryptedFileContent,
                certMpFileName = mpFileName,
                certMpFileContent = mpEncryptedFileContent,
                certJksFileName = jksFileName,
                certJksFileContent = jksEncryptedFileContent,
                certJksAlias = jksAlias,
                certJksAliasCredentialId = jksAliasCredentialId,
                certDeveloperName = developerName,
                certTeamName = teamName,
                certUUID = uuid,
                certExpireDate = expireDate,
                credentialId = credentialId
            )
            certTlsDao.update(
                dslContext = transactionContext,
                projectId = projectId,
                certId = certId,
                serverCrtFileName = serverCrtFileName,
                serverCrtFile = serverCrtFile,
                serverKeyFileName = serverKeyFileName,
                serverKeyFile = serverKeyFile,
                clientCrtFileName = clientCrtFileName,
                clientCrtFile = clientCrtFile,
                clientKeyFileName = clientKeyFileName,
                clientKeyFile = clientKeyFile
            )
        }
    }

    override fun delete(userId: String, projectId: String, certId: String) {
        val delete = AuthPermission.DELETE
        certPermissionService.validatePermission(
            userId,
            projectId,
            certId,
            delete,
            MessageUtil.getMessageByLocale(
                USER_NO_ENGINEERING_CERT_OPERATE_PERMISSIONS,
                I18nUtil.getLanguage(userId),
                arrayOf(
                    userId,
                    projectId,
                    certId,
                    delete.getI18n(I18nUtil.getLanguage(userId))
                )
            )
        )

        certPermissionService.deleteResource(projectId, certId)
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
        val permissionToListMap = certPermissionService.filterCerts(
            userId = userId,
            projectId = projectId,
            authPermissions = setOf(AuthPermission.LIST, AuthPermission.DELETE, AuthPermission.EDIT, AuthPermission.USE)
        )
        val hasListPermissionCertIdList = permissionToListMap[AuthPermission.LIST]!!
        val hasDeletePermissionCertIdList = permissionToListMap[AuthPermission.DELETE]!!
        val hasEditPermissionCertIdList = permissionToListMap[AuthPermission.EDIT]!!
        val hasUsePermissionCertIdList = permissionToListMap[AuthPermission.USE]!!
        logger.info("$permissionToListMap $hasListPermissionCertIdList $hasDeletePermissionCertIdList")

        val count = certDao.countByProject(dslContext, projectId, certType, hasListPermissionCertIdList.toSet())
        val certRecordList =
            certDao.listByProject(dslContext, projectId, certType, hasListPermissionCertIdList.toSet(), offset, limit)
        val certList = certRecordList.map {
            val hasDeletePermission = hasDeletePermissionCertIdList.contains(it.certId)
            val hasEditPermission = hasEditPermissionCertIdList.contains(it.certId)
            val hasUsePermission = hasUsePermissionCertIdList.contains(it.certId)
            CertWithPermission(
                certId = it.certId,
                certType = it.certType,
                creator = it.certUserId,
                certRemark = it.certRemark,
                createTime = it.certCreateTime.timestamp(),
                expireTime = it.certExpireDate.timestamp(),
                credentialId = it.credentialId ?: "",
                alias = it.certJksAlias ?: "",
                aliasCredentialId = it.certJksAliasCredentialId ?: "",
                permissions = CertPermissions(
                    delete = hasDeletePermission,
                    edit = hasEditPermission,
                    use = hasUsePermission
                )
            )
        }
        return SQLPage(count, certList)
    }

    override fun list(projectId: String, offset: Int, limit: Int): SQLPage<Cert> {
        val count = certDao.countByProject(dslContext, projectId, null)
        val certList = mutableListOf<Cert>()
        val certInfos = certDao.listByProject(dslContext, projectId, offset, limit)
        certInfos.map {
            certList.add(
                Cert(
                    certId = it.certId,
                    certType = it.certType,
                    creator = it.certUserId,
                    credentialId = it.credentialId,
                    createTime = it.certCreateTime.timestamp(),
                    certRemark = it.certRemark,
                    expireTime = it.certExpireDate.timestamp()
                )
            )
        }

        return SQLPage(
            count = count,
            records = certList
        )
    }

    override fun hasPermissionList(
        userId: String,
        projectId: String,
        certType: String?,
        authPermission: AuthPermission,
        offset: Int,
        limit: Int
    ): SQLPage<Cert> {
        val hasPermissionCertIdList = certPermissionService.filterCert(userId, projectId, authPermission)

        val count = certDao.countByProject(dslContext, projectId, certType, hasPermissionCertIdList.toSet())
        val certRecordList =
            certDao.listByProject(dslContext, projectId, certType, hasPermissionCertIdList.toSet(), offset, limit)
        val certList = certRecordList.map {
            Cert(
                certId = it.certId,
                certType = it.certType,
                creator = it.certUserId,
                certRemark = it.certRemark,
                createTime = it.certCreateTime.timestamp(),
                expireTime = it.certExpireDate.timestamp(),
                credentialId = it.credentialId ?: ""
            )
        }
        return SQLPage(count, certList)
    }

    override fun getIos(userId: String, projectId: String, certId: String): CertIOSInfo {
        certPermissionService.validatePermission(
            userId = userId,
            projectId = projectId,
            resourceCode = certId,
            authPermission = AuthPermission.VIEW,
            message = I18nUtil.getCodeLanMessage(
                messageCode = USER_NO_ENGINEERING_CERT_OPERATE_PERMISSIONS,
                params = arrayOf(userId, projectId, certId, AuthPermission.VIEW.getI18n(I18nUtil.getLanguage(userId)))
            )
        )
        val certRecord = certDao.get(dslContext, projectId, certId)
        return CertIOSInfo(
            certId = certId,
            p12FileName = certRecord.certP12FileName,
            mobileProvisionFileName = certRecord.certMpFileName,
            credentialId = certRecord.credentialId,
            remark = certRecord.certRemark
        )
    }

    override fun getEnterprise(projectId: String, certId: String): CertEnterpriseInfo {
        val certRecord = certDao.get(dslContext, projectId, certId)
        return CertEnterpriseInfo(
            certId = certId,
            mobileProvisionFileName = certRecord.certMpFileName,
            remark = certRecord.certRemark
        )
    }

    override fun getAndroid(userId: String, projectId: String, certId: String): CertAndroidInfo {
        certPermissionService.validatePermission(
            userId = userId,
            projectId = projectId,
            resourceCode = certId,
            authPermission = AuthPermission.VIEW,
            message = I18nUtil.getCodeLanMessage(
                messageCode = USER_NO_ENGINEERING_CERT_OPERATE_PERMISSIONS,
                params = arrayOf(userId, projectId, certId, AuthPermission.VIEW.getI18n(I18nUtil.getLanguage(userId)))
            )
        )
        val certRecord = certDao.get(dslContext, projectId, certId)
        return CertAndroidInfo(
            certId = certId,
            jksFileName = certRecord.certJksFileName,
            credentialId = certRecord.credentialId,
            alias = certRecord.certJksAlias,
            aliasCredentialId = certRecord.certJksAliasCredentialId,
            remark = certRecord.certRemark
        )
    }

    override fun getTls(projectId: String, certId: String): CertTlsInfo {
        val certRecord = certDao.get(dslContext, projectId, certId)
        val certTlsRecord = certTlsDao.get(dslContext, projectId, certId)
        return CertTlsInfo(
            certId = certId,
            serverCrtFileName = certTlsRecord.certServerCrtFileName,
            serverKeyFileName = certTlsRecord.certServerKeyFileName,
            clientCrtFileName = certTlsRecord.certClientCrtFileName,
            clientKeyFileName = certTlsRecord.certClientKeyFileName,
            remark = certRecord.certRemark
        )
    }

    override fun queryIos(projectId: String, buildId: String, certId: String, publicKey: String): CertIOS {
        val buildBasicInfoResult = client.get(ServiceBuildResource::class).serviceBasic(projectId, buildId)
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
        val p12FileContent = certHelper.decryptBytes(certRecord.certP12FileContent)!!
        val p12Base64Content = encryptCert(p12FileContent, publicKeyByteArray, serverPrivateKeyByteArray)

        val mpFileName = certRecord.certMpFileName
        val mpFileContent = certHelper.decryptBytes(certRecord.certMpFileContent)!!
        val mpBase64Content = encryptCert(mpFileContent, publicKeyByteArray, serverPrivateKeyByteArray)

        val credentialId = certRecord.credentialId

        return CertIOS(serverBase64PublicKey, p12FileName, p12Base64Content, mpFileName, mpBase64Content, credentialId)
    }

    override fun queryEnterprise(
        projectId: String,
        buildId: String,
        certId: String,
        publicKey: String
    ): CertEnterprise {
        val buildBasicInfoResult = client.get(ServiceBuildResource::class).serviceBasic(projectId, buildId)
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
        val mpFileContent = certHelper.decryptBytes(certRecord.certMpFileContent)!!
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
        val mpFileContent = certHelper.decryptBytes(certRecord.certMpFileContent)!!
        val mpBase64Content = encryptCert(mpFileContent, publicKeyByteArray, serverPrivateKeyByteArray)
        val mpFileSha1 = ShaUtils.sha1(mpFileContent)

        return CertEnterprise(serverBase64PublicKey, mpFileName, mpBase64Content, mpFileSha1)
    }

    override fun queryAndroid(projectId: String, buildId: String, certId: String, publicKey: String): CertAndroid {
        val buildBasicInfoResult = client.get(ServiceBuildResource::class).serviceBasic(projectId, buildId)
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
        val jksFileContent = certHelper.decryptBytes(certRecord.certJksFileContent)!!
        val jksBase64Content = encryptCert(jksFileContent, publicKeyByteArray, serverPrivateKeyByteArray)

        val credentialId = certRecord.credentialId
        val alias = certRecord.certJksAlias
        val aliasCredentialId = certRecord.certJksAliasCredentialId

        val type = if (jksFileName.endsWith(".jks")) CertAndroidType.JKS else CertAndroidType.KEYSTORE

        return CertAndroid(
            publicKey = serverBase64PublicKey,
            type = type,
            jksFileName = jksFileName,
            jksContent = jksBase64Content,
            credentialId = credentialId,
            alias = alias,
            aliasCredentialId = aliasCredentialId
        )
    }

    override fun queryAndroidByProject(
        projectId: String,
        certId: String,
        publicKey: String
    ): CertAndroidWithCredential {
        val certRecord = certDao.get(dslContext, projectId, certId)
        val publicKeyByteArray = Base64.getDecoder().decode(publicKey)
        val serverDHKeyPair = DHUtil.initKey(publicKeyByteArray)
        val serverPublicKeyByteArray = serverDHKeyPair.publicKey
        val serverPrivateKeyByteArray = serverDHKeyPair.privateKey
        val serverBase64PublicKey = String(Base64.getEncoder().encode(serverPublicKeyByteArray))

        val jksFileName = certRecord.certJksFileName
        val jksFileContent = certHelper.decryptBytes(certRecord.certJksFileContent)!!
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
            publicKey = serverBase64PublicKey,
            type = type,
            fileName = jksFileName,
            fileContent = jksBase64Content,
            fileSha1 = jksFileSha1,
            credential = credential,
            alias = alias,
            aliasCredential = aliasCredential
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
        val serverCrtFile = certHelper.decryptBytes(certTlsRecord.certServerCrtFile)!!
        val serverBase64CrtFile = encryptCert(serverCrtFile, publicKeyByteArray, serverPrivateKeyByteArray)
        val serverCrtSha1 = ShaUtils.sha1(serverCrtFile)

        val serverKeyFileName = certTlsRecord.certServerKeyFileName
        val serverKeyFile = certHelper.decryptBytes(certTlsRecord.certServerKeyFile)!!
        val serverBase64KeyFile = encryptCert(serverKeyFile, publicKeyByteArray, serverPrivateKeyByteArray)
        val serverKeySha1 = ShaUtils.sha1(serverKeyFile)

        var clientCrtFileName: String? = null
        var clientBase64CrtFile: String? = null
        var clientCrtSha1: String? = null
        if (certTlsRecord.certClientCrtFile != null && certTlsRecord.certClientCrtFileName != null) {
            clientCrtFileName = certTlsRecord.certClientCrtFileName
            val clientCrtFile = certHelper.decryptBytes(certTlsRecord.certClientCrtFile)!!
            clientBase64CrtFile = encryptCert(clientCrtFile, publicKeyByteArray, serverPrivateKeyByteArray)
            clientCrtSha1 = ShaUtils.sha1(clientCrtFile)
        }

        var clientKeyFileName: String? = null
        var clientBase64KeyFile: String? = null
        var clientKeySha1: String? = null
        if (certTlsRecord.certClientKeyFile != null && certTlsRecord.certClientKeyFileName != null) {
            clientKeyFileName = certTlsRecord.certClientKeyFileName
            val clientKeyFile = certHelper.decryptBytes(certTlsRecord.certClientKeyFile)!!
            clientBase64KeyFile = encryptCert(clientKeyFile, publicKeyByteArray, serverPrivateKeyByteArray)
            clientKeySha1 = ShaUtils.sha1(clientKeyFile)
        }

        return CertTls(
            publicKey = serverBase64PublicKey,
            serverCrtFileName = serverCrtFileName,
            serverCrtFile = serverBase64CrtFile,
            serverCrtSha1 = serverCrtSha1,
            serverKeyFileName = serverKeyFileName,
            serverKeyFile = serverBase64KeyFile,
            serverKeySha1 = serverKeySha1,
            clientCrtFileName = clientCrtFileName,
            clientCrtFile = clientBase64CrtFile,
            clientCrtSha1 = clientCrtSha1,
            clientKeyFileName = clientKeyFileName,
            clientKeyFile = clientBase64KeyFile,
            clientKeySha1 = clientKeySha1
        )
    }

    override fun getCertByIds(certIds: Set<String>): List<Cert>? {
        val certList = mutableListOf<Cert>()
        val records = certDao.listByIds(
            dslContext = dslContext,
            certIds = certIds
        )
        records.map {
            certList.add(
                Cert(
                    certId = it.certId,
                    certType = it.certType,
                    creator = it.certUserId,
                    credentialId = it.credentialId,
                    createTime = it.certCreateTime.timestamp(),
                    certRemark = it.certRemark,
                    expireTime = it.certExpireDate.timestamp()
                )
            )
        }
        return certList
    }

    override fun searchByCertId(projectId: String, offset: Int, limit: Int, certId: String): SQLPage<Cert> {
        val count = certDao.countByIdLike(dslContext, projectId, certId)
        val certList = mutableListOf<Cert>()
        val certInfos = certDao.searchByIdLike(
            dslContext = dslContext,
            projectId = projectId,
            offset = offset,
            limit = limit,
            certId = certId
        )
        certInfos.map {
            certList.add(
                Cert(
                    certId = it.certId,
                    certType = it.certType,
                    creator = it.certUserId,
                    credentialId = it.credentialId,
                    createTime = it.certCreateTime.timestamp(),
                    certRemark = it.certRemark,
                    expireTime = it.certExpireDate.timestamp()
                )
            )
        }

        return SQLPage(
            count = count,
            records = certList
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

    companion object {
        private val logger = LoggerFactory.getLogger(CertService::class.java)
    }
}
