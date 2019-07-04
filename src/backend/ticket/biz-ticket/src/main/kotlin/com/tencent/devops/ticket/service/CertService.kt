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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
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
import com.tencent.devops.common.api.util.ShaUtils
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.auth.api.BkAuthPermission
import com.tencent.devops.common.client.Client
import com.tencent.devops.process.api.ServiceBuildResource
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

@Service
class CertService @Autowired constructor(
    private val dslContext: DSLContext,
    private val client: Client,
    private val certDao: CertDao,
    private val certTlsDao: CertTlsDao,
    private val certEnterpriseDao: CertEnterpriseDao,
    private val credentialService: CredentialService,
    private val certPermissionService: CertPermissionService,
    private val certHelper: CertHelper
) {
    private val certMaxSize = 64 * 1024
    private val certIdMaxSize = 32

    fun uploadIos(
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
        certPermissionService.validatePermission(
            userId,
            projectId,
            BkAuthPermission.CREATE,
            "用户($userId)在工程($projectId)下没有证书创建权限"
        )

        if (certCredentialId != null) {
            certPermissionService.validatePermission(
                userId = userId,
                projectId = projectId,
                resourceCode = certCredentialId,
                bkAuthPermission = BkAuthPermission.USE,
                message = "用户($userId)在工程($projectId)下没有凭据($certCredentialId)的使用权限"
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
            credentialId = certId,
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
            certId = credentialId
        )
    }

    fun updateIos(
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
        certPermissionService.validatePermission(
            userId,
            projectId,
            certId,
            BkAuthPermission.EDIT,
            "用户($userId)在工程($projectId)下没有证书编辑权限"
        )
        if (!certDao.has(dslContext, projectId, certId)) {
            throw OperationException("名称${certId}不存在")
        }

        if (certCredentialId != null) {
            certPermissionService.validatePermission(
                userId = userId,
                projectId = projectId,
                resourceCode = certCredentialId,
                bkAuthPermission = BkAuthPermission.USE,
                message = "用户($userId)在工程($projectId)下没有凭据($certCredentialId)的使用权限"
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
        val p12FileName =
            if (p12Disposition != null) String(p12Disposition.fileName.toByteArray(Charset.forName("ISO-8859-1"))) else null
        val p12EncryptedFileContent = certHelper.encryptBytes(p12FileContent)
        val mpFileName =
            if (mpDisposition != null) String(mpDisposition.fileName.toByteArray(Charset.forName("ISO-8859-1"))) else null
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

    fun uploadEnterprise(
        userId: String,
        projectId: String,
        certId: String,
        certRemark: String?,
        mpInputStream: InputStream,
        mpDisposition: FormDataContentDisposition
    ) {
        certPermissionService.validatePermission(
            userId,
            projectId,
            BkAuthPermission.CREATE,
            "用户($userId)在工程($projectId)下没有证书创建权限"
        )

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

    fun updateEnterprise(
        userId: String,
        projectId: String,
        certId: String,
        certRemark: String?,
        mpInputStream: InputStream?,
        mpDisposition: FormDataContentDisposition?
    ) {
        certPermissionService.validatePermission(
            userId,
            projectId,
            certId,
            BkAuthPermission.EDIT,
            "用户($userId)在工程($projectId)下没有证书编辑权限"
        )
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

    fun uploadAndroid(
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
        certPermissionService.validatePermission(
            userId,
            projectId,
            BkAuthPermission.CREATE,
            "用户($userId)在工程($projectId)下没有证书创建权限"
        )
        certPermissionService.validatePermission(
            userId = userId,
            projectId = projectId,
            resourceCode = credentialId,
            bkAuthPermission = BkAuthPermission.USE,
            message = "用户($userId)在工程($projectId)下没有凭据($credentialId)的使用权限"
        )
        certPermissionService.validatePermission(
            userId = userId,
            projectId = projectId,
            resourceCode = aliasCredentialId,
            bkAuthPermission = BkAuthPermission.USE,
            message = "用户($userId)在工程($projectId)下没有凭据($aliasCredentialId)的使用权限"
        )

        if (certDao.has(dslContext, projectId, certId)) {
            throw OperationException("证书${certId}已存在")
        }

        val jksFileContent = read(inputStream)
        if (jksFileContent.size > certMaxSize) {
            throw OperationException("JKS文件大小不能超过64k")
        }

        val credential = credentialService.serviceGet(projectId, credentialId)
        if (!certHelper.validJksPassword(jksFileContent, credential.v1)) {
            throw OperationException("证书密码错误")
        }

        val aliasCredential = credentialService.serviceGet(projectId, aliasCredentialId)
        if (!certHelper.validJksAlias(jksFileContent, credential.v1, alias, aliasCredential.v1)) {
            throw OperationException("证书别名或者别名密码错误")
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

    fun updateAndroid(
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
        certPermissionService.validatePermission(
            userId,
            projectId,
            certId,
            BkAuthPermission.EDIT,
            "用户($userId)在工程($projectId)下没有证书编辑权限"
        )
        val certRecord = certDao.getOrNull(dslContext, projectId, certId)
        certRecord ?: throw OperationException("证书${certId}不存在")

        certPermissionService.validatePermission(
            userId = userId,
            projectId = projectId,
            resourceCode = credentialId,
            bkAuthPermission = BkAuthPermission.USE,
            message = "用户($userId)在工程($projectId)下没有凭据($credentialId)的使用权限"
        )

        certPermissionService.validatePermission(
            userId = userId,
            projectId = projectId,
            resourceCode = aliasCredentialId,
            bkAuthPermission = BkAuthPermission.USE,
            message = "用户($userId)在工程($projectId)下没有凭据($aliasCredentialId)的使用权限"
        )

        val jksFileContent =
            if (inputStream != null) read(inputStream)
            else certHelper.decryptBytes(certRecord.certJksFileContent)!!
        if (jksFileContent.size > certMaxSize) {
            throw OperationException("JKS文件大小不能超过64k")
        }

        val credential = credentialService.serviceGet(projectId, credentialId)
        if (!certHelper.validJksPassword(jksFileContent, credential.v1)) {
            throw OperationException("证书密码错误")
        }

        val aliasCredential = credentialService.serviceGet(projectId, aliasCredentialId)
        if (!certHelper.validJksAlias(jksFileContent, credential.v1, alias, aliasCredential.v1)) {
            throw OperationException("证书别名或者别名密码错误")
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

    fun uploadTls(
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
        certPermissionService.validatePermission(
            userId,
            projectId,
            BkAuthPermission.CREATE,
            "用户($userId)在工程($projectId)下没有证书创建权限"
        )
        if (certDao.has(dslContext, projectId, certId)) {
            throw OperationException("证书${certId}已被他人使用")
        }
        if (certId.length > certIdMaxSize) {
            throw OperationException("证书名称不能超过32位")
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

    fun updateTls(
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
        certPermissionService.validatePermission(
            userId = userId,
            projectId = projectId,
            resourceCode = certId,
            bkAuthPermission = BkAuthPermission.EDIT,
            message = "用户($userId)在工程($projectId)下没有证书编辑权限"
        )
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

    fun delete(userId: String, projectId: String, certId: String) {
        certPermissionService.validatePermission(
            userId,
            projectId,
            certId,
            BkAuthPermission.DELETE,
            "用户($userId)在工程($projectId)下没有证书($certId)的删除权限"
        )

        certPermissionService.deleteResource(projectId, certId)
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            certDao.delete(transactionContext, projectId, certId)
            certTlsDao.delete(transactionContext, projectId, certId)
            certEnterpriseDao.delete(transactionContext, projectId, certId)
        }
    }

    fun list(
        userId: String,
        projectId: String,
        certType: String?,
        offset: Int,
        limit: Int
    ): SQLPage<CertWithPermission> {
        val permissionToListMap = certPermissionService.filterCerts(
            userId = userId,
            projectId = projectId,
            bkAuthPermissions = setOf(BkAuthPermission.LIST, BkAuthPermission.DELETE, BkAuthPermission.EDIT)
        )
        val hasListPermissionCertIdList = permissionToListMap[BkAuthPermission.LIST]!!
        val hasDeletePermissionCertIdList = permissionToListMap[BkAuthPermission.DELETE]!!
        val hasEditPermissionCertIdList = permissionToListMap[BkAuthPermission.EDIT]!!

        logger.info("$permissionToListMap $hasListPermissionCertIdList $hasDeletePermissionCertIdList")

        val count = certDao.countByProject(dslContext, projectId, certType, hasListPermissionCertIdList.toSet())
        val certRecordList =
            certDao.listByProject(dslContext, projectId, certType, hasListPermissionCertIdList.toSet(), offset, limit)
        val certList = certRecordList.map {
            val hasDeletePermission = hasDeletePermissionCertIdList.contains(it.certId)
            val hasEditPermission = hasEditPermissionCertIdList.contains(it.certId)
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
                permissions = CertPermissions(hasDeletePermission, hasEditPermission)
            )
        }
        return SQLPage(count, certList)
    }

    fun hasPermissionList(
        userId: String,
        projectId: String,
        certType: String?,
        bkAuthPermission: BkAuthPermission,
        offset: Int,
        limit: Int
    ): SQLPage<Cert> {
        val hasPermissionCertIdList = certPermissionService.filterCert(userId, projectId, bkAuthPermission)

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

    fun getIos(projectId: String, certId: String): CertIOSInfo {
        val certRecord = certDao.get(dslContext, projectId, certId)
        return CertIOSInfo(
            certId = certId,
            p12FileName = certRecord.certP12FileName,
            mobileProvisionFileName = certRecord.certMpFileName,
            credentialId = certRecord.credentialId,
            remark = certRecord.certRemark
        )
    }

    fun getEnterprise(projectId: String, certId: String): CertEnterpriseInfo {
        val certRecord = certDao.get(dslContext, projectId, certId)
        return CertEnterpriseInfo(
            certId = certId,
            mobileProvisionFileName = certRecord.certMpFileName,
            remark = certRecord.certRemark
        )
    }

    fun getAndroid(projectId: String, certId: String): CertAndroidInfo {
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

    fun getTls(projectId: String, certId: String): CertTlsInfo {
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

    fun queryIos(buildId: String, certId: String, publicKey: String): CertIOS {
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
        val p12FileContent = certHelper.decryptBytes(certRecord.certP12FileContent)!!
        val p12Base64Content = encryptCert(p12FileContent, publicKeyByteArray, serverPrivateKeyByteArray)

        val mpFileName = certRecord.certMpFileName
        val mpFileContent = certHelper.decryptBytes(certRecord.certMpFileContent)!!
        val mpBase64Content = encryptCert(mpFileContent, publicKeyByteArray, serverPrivateKeyByteArray)

        val credentialId = certRecord.credentialId

        return CertIOS(serverBase64PublicKey, p12FileName, p12Base64Content, mpFileName, mpBase64Content, credentialId)
    }

    fun queryEnterprise(buildId: String, certId: String, publicKey: String): CertEnterprise {
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
        val mpFileContent = certHelper.decryptBytes(certRecord.certMpFileContent)!!
        val mpBase64Content = encryptCert(mpFileContent, publicKeyByteArray, serverPrivateKeyByteArray)
        val mpFileSha1 = ShaUtils.sha1(mpFileContent)

        return CertEnterprise(serverBase64PublicKey, mpFileName, mpBase64Content, mpFileSha1)
    }

    fun queryEnterpriseByProject(projectId: String, certId: String, publicKey: String): CertEnterprise {
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

    fun queryAndroid(buildId: String, certId: String, publicKey: String): CertAndroid {
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

    fun queryAndroidByProject(projectId: String, certId: String, publicKey: String): CertAndroidWithCredential {
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

    fun queryTlsByProject(projectId: String, certId: String, publicKey: String): CertTls {
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
