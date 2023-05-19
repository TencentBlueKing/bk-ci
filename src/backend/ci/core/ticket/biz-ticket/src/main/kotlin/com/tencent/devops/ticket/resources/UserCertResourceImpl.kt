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

package com.tencent.devops.ticket.resources

import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.ticket.api.UserCertResource
import com.tencent.devops.ticket.constant.TicketMessageCode
import com.tencent.devops.ticket.constant.TicketMessageCode.INVALID_CERT_ID
import com.tencent.devops.ticket.constant.TicketMessageCode.CERT_FILE_MUST_BE
import com.tencent.devops.ticket.constant.TicketMessageCode.CERT_FILE_TYPE_ERROR
import com.tencent.devops.ticket.constant.TicketMessageCode.DESCRIPTION_FILE_TYPE_ERROR
import com.tencent.devops.ticket.constant.TicketMessageCode.KEY_FILE_MUST_BE
import com.tencent.devops.ticket.pojo.Cert
import com.tencent.devops.ticket.pojo.CertAndroidInfo
import com.tencent.devops.ticket.pojo.CertEnterpriseInfo
import com.tencent.devops.ticket.pojo.CertIOSInfo
import com.tencent.devops.ticket.pojo.CertTlsInfo
import com.tencent.devops.ticket.pojo.CertWithPermission
import com.tencent.devops.ticket.pojo.enums.Permission
import com.tencent.devops.ticket.service.CertPermissionService
import com.tencent.devops.ticket.service.CertService
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.springframework.beans.factory.annotation.Autowired
import java.io.InputStream

@Suppress("ALL")
@RestResource
class UserCertResourceImpl @Autowired constructor(
    private val certService: CertService,
    private val certPermissionService: CertPermissionService
) : UserCertResource {

    override fun hasCreatePermission(userId: String, projectId: String): Result<Boolean> {
        return Result(certPermissionService.validatePermission(userId, projectId, AuthPermission.CREATE))
    }

    override fun getIos(userId: String, projectId: String, certId: String): Result<CertIOSInfo> {
        return Result(certService.getIos(userId, projectId, certId))
    }

    override fun uploadIos(
        userId: String,
        projectId: String,
        certId: String,
        certRemark: String?,
        credentialId: String?,
        p12InputStream: InputStream,
        p12Disposition: FormDataContentDisposition,
        mpInputStream: InputStream,
        mpDisposition: FormDataContentDisposition
    ): Result<Boolean> {
        checkParams(userId, projectId, certId)
        if (!p12Disposition.fileName.endsWith(".p12")) {
            throw IllegalArgumentException(
                MessageUtil.getMessageByLocale(CERT_FILE_TYPE_ERROR, I18nUtil.getLanguage(userId), arrayOf(".p12"))
            )
        }
        if (!mpDisposition.fileName.endsWith(".mobileprovision")) {
            throw IllegalArgumentException(
                MessageUtil.getMessageByLocale(
                    DESCRIPTION_FILE_TYPE_ERROR,
                    I18nUtil.getLanguage(userId),
                    arrayOf(".mobileprovision")
                )
            )
        }
        certService.uploadIos(
            userId,
            projectId,
            certId,
            certRemark,
            credentialId,
            p12InputStream,
            p12Disposition,
            mpInputStream,
            mpDisposition
        )
        return Result(true)
    }

    override fun updateIos(
        userId: String,
        projectId: String,
        certId: String,
        certRemark: String?,
        credentialId: String?,
        p12InputStream: InputStream?,
        p12Disposition: FormDataContentDisposition?,
        mpInputStream: InputStream?,
        mpDisposition: FormDataContentDisposition?
    ): Result<Boolean> {
        checkParams(userId, projectId, certId)
        certService.updateIos(
            userId,
            projectId,
            certId,
            certRemark,
            credentialId,
            p12InputStream,
            p12Disposition,
            mpInputStream,
            mpDisposition
        )
        return Result(true)
    }

    override fun getAndroid(userId: String, projectId: String, certId: String): Result<CertAndroidInfo> {
        return Result(certService.getAndroid(userId, projectId, certId))
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
    ): Result<Boolean> {
        checkParams(userId, projectId, certId)
        if (credentialId.isBlank()) {
            throw ParamBlankException("Invalid credentialId")
        }
        if (alias.isBlank()) {
            throw ParamBlankException("Invalid alias")
        }
        if (aliasCredentialId.isBlank()) {
            throw ParamBlankException("Invalid aliasCredentialId")
        }
        if (!disposition.fileName.endsWith(".jks") && !disposition.fileName.endsWith(".keystore")) {
            throw IllegalArgumentException(
                MessageUtil.getMessageByLocale(
                    CERT_FILE_MUST_BE,
                    I18nUtil.getLanguage(userId),
                    arrayOf(".jks||.keystore")
                )
            )
        }
        certService.uploadAndroid(
            userId,
            projectId,
            certId,
            certRemark,
            credentialId,
            alias,
            aliasCredentialId,
            inputStream,
            disposition
        )
        return Result(true)
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
    ): Result<Boolean> {
        checkParams(userId, projectId, certId)
        certService.updateAndroid(
            userId,
            projectId,
            certId,
            certRemark,
            credentialId,
            alias,
            aliasCredentialId,
            inputStream,
            disposition
        )
        return Result(true)
    }

    override fun getTls(userId: String, projectId: String, certId: String): Result<CertTlsInfo> {
        return Result(certService.getTls(projectId, certId))
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
    ): Result<Boolean> {
        checkParams(userId, projectId, certId)
        if (!serverCrtDisposition.fileName.endsWith(".crt")) {
            throw IllegalArgumentException(
                MessageUtil.getMessageByLocale(CERT_FILE_MUST_BE, I18nUtil.getLanguage(userId), arrayOf(".crt"))
            )
        }
        if (!serverKeyDisposition.fileName.endsWith(".key")) {
            throw IllegalArgumentException(
                MessageUtil.getMessageByLocale(KEY_FILE_MUST_BE, I18nUtil.getLanguage(userId), arrayOf(".key"))
            )
        }
        if (clientCrtDisposition != null && !clientCrtDisposition.fileName.endsWith(".crt")) {
            throw IllegalArgumentException(
                MessageUtil.getMessageByLocale(CERT_FILE_MUST_BE, I18nUtil.getLanguage(userId), arrayOf(".crt"))
            )
        }
        if (clientKeyDisposition != null && !clientKeyDisposition.fileName.endsWith(".key")) {
            throw IllegalArgumentException(
                MessageUtil.getMessageByLocale(KEY_FILE_MUST_BE, I18nUtil.getLanguage(userId), arrayOf(".key"))
            )
        }
        certService.uploadTls(
            userId,
            projectId,
            certId,
            certRemark,
            serverCrtInputStream,
            serverCrtDisposition,
            serverKeyInputStream,
            serverKeyDisposition,
            clientCrtInputStream,
            clientCrtDisposition,
            clientKeyInputStream,
            clientKeyDisposition
        )
        return Result(true)
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
    ): Result<Boolean> {
        checkParams(userId, projectId, certId)
        certService.updateTls(
            userId,
            projectId,
            certId,
            certRemark,
            serverCrtInputStream,
            serverCrtDisposition,
            serverKeyInputStream,
            serverKeyDisposition,
            clientCrtInputStream,
            clientCrtDisposition,
            clientKeyInputStream,
            clientKeyDisposition
        )
        return Result(true)
    }

    override fun list(
        userId: String,
        projectId: String,
        certType: String?,
        page: Int?,
        pageSize: Int?
    ): Result<Page<CertWithPermission>> {
        checkParams(userId, projectId)
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: 20
        val limit = PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull)
        val result = certService.list(userId, projectId, certType, limit.offset, limit.limit)
        return Result(Page(pageNotNull, pageSizeNotNull, result.count, result.records))
    }

    override fun hasPermissionList(
        userId: String,
        projectId: String,
        certType: String?,
        permission: Permission,
        page: Int?,
        pageSize: Int?
//            ,isCommonEnterprise: Boolean?
    ): Result<Page<Cert>> {
        checkParams(userId, projectId)
        val bkAuthPermission = when (permission) {
            Permission.CREATE -> AuthPermission.CREATE
            Permission.DELETE -> AuthPermission.DELETE
            Permission.LIST -> AuthPermission.LIST
            Permission.VIEW -> AuthPermission.VIEW
            Permission.EDIT -> AuthPermission.EDIT
            Permission.USE -> AuthPermission.USE
        }
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: 20
        val limit = PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull)
        val result =
            certService.hasPermissionList(userId, projectId, certType, bkAuthPermission, limit.offset, limit.limit)
        return Result(Page(pageNotNull, pageSizeNotNull, result.count, result.records))
    }

    override fun delete(userId: String, projectId: String, certId: String): Result<Boolean> {
        checkParams(userId, projectId, certId)
        certService.delete(userId, projectId, certId)
        return Result(true)
    }

    private fun checkParams(userId: String, projectId: String, certId: String? = null) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        if (certId != null && certId.isBlank()) {
            throw ParamBlankException(
                MessageUtil.getMessageByLocale(INVALID_CERT_ID, I18nUtil.getLanguage(userId))
            )
        }
        if (certId != null && certId!!.length > 128) {
            throw OperationException(
                I18nUtil.getCodeLanMessage(
                    TicketMessageCode.CERT_ID_TOO_LONG,
                    language = I18nUtil.getLanguage(userId)
                )
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
    ): Result<Boolean> {
        checkParams(userId, projectId, certId)
        certService.updateEnterprise(
            userId,
            projectId,
            certId,
            certRemark,
            mpInputStream,
            mpDisposition
        )
        return Result(true)
    }

    override fun uploadEnterprise(
        userId: String,
        projectId: String,
        certId: String,
        certRemark: String?,
        mpInputStream: InputStream,
        mpDisposition: FormDataContentDisposition
    ): Result<Boolean> {
        checkParams(userId, projectId, certId)
        if (!mpDisposition.fileName.endsWith(".mobileprovision")) {
            throw IllegalArgumentException(
                MessageUtil.getMessageByLocale(DESCRIPTION_FILE_TYPE_ERROR,
                    I18nUtil.getLanguage(userId),
                    arrayOf(".mobileprovision")
                )
            )
        }
        certService.uploadEnterprise(
            userId,
            projectId,
            certId,
            certRemark,
            mpInputStream,
            mpDisposition
        )
        return Result(true)
    }

    override fun getEnterprise(
        userId: String,
        projectId: String,
        certId: String
    ): Result<CertEnterpriseInfo> {
        checkParams(userId, projectId, certId)
        return Result(certService.getEnterprise(projectId, certId))
    }
}
