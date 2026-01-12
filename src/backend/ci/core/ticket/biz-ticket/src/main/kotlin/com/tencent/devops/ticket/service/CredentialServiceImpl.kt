/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

import com.tencent.bk.audit.annotations.ActionAuditRecord
import com.tencent.bk.audit.annotations.AuditAttribute
import com.tencent.bk.audit.annotations.AuditEntry
import com.tencent.bk.audit.annotations.AuditInstanceRecord
import com.tencent.bk.audit.context.ActionAuditContext
import com.tencent.devops.common.api.constant.TEMPLATE_ACROSS_INFO_ID
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.util.DHUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.audit.ActionAuditContent
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.ResourceTypeId
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.model.ticket.tables.records.TCredentialRecord
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.api.service.ServiceTemplateAcrossResource
import com.tencent.devops.process.api.service.ServiceVarResource
import com.tencent.devops.process.pojo.BuildTemplateAcrossInfo
import com.tencent.devops.process.pojo.TemplateAcrossInfoType
import com.tencent.devops.ticket.constant.TicketMessageCode
import com.tencent.devops.ticket.constant.TicketMessageCode.USER_NO_ENGINEERING_CREDENTIAL_OPERATE_PERMISSIONS
import com.tencent.devops.ticket.dao.CredentialDao
import com.tencent.devops.ticket.pojo.Credential
import com.tencent.devops.ticket.pojo.CredentialCreate
import com.tencent.devops.ticket.pojo.CredentialInfo
import com.tencent.devops.ticket.pojo.CredentialItemVo
import com.tencent.devops.ticket.pojo.CredentialPermissions
import com.tencent.devops.ticket.pojo.CredentialSettingUpdate
import com.tencent.devops.ticket.pojo.CredentialUpdate
import com.tencent.devops.ticket.pojo.CredentialWithPermission
import com.tencent.devops.ticket.pojo.enums.CredentialType
import com.tencent.devops.ticket.pojo.item.AccessTokenCredentialItem
import com.tencent.devops.ticket.pojo.item.OauthTokenCredentialItem
import com.tencent.devops.ticket.pojo.item.PasswordCredentialItem
import com.tencent.devops.ticket.pojo.item.SshPrivateKeyCredentialItem
import com.tencent.devops.ticket.pojo.item.TokenSshPrivateKeyCredentialItem
import com.tencent.devops.ticket.pojo.item.TokenUserPassCredentialItem
import com.tencent.devops.ticket.pojo.item.UserPassCredentialItem
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.Base64
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.core.Response

@Suppress("ALL")
@Service
class CredentialServiceImpl @Autowired constructor(
    private val credentialHelper: CredentialHelper,
    private val credentialPermissionService: CredentialPermissionService,
    private val dslContext: DSLContext,
    private val client: Client,
    private val credentialDao: CredentialDao
) : CredentialService {

    override fun serviceEdit(userId: String?, projectId: String, credentialId: String, credential: CredentialUpdate) {
        if (!credentialDao.has(dslContext, projectId, credentialId)) {
            throw ErrorCodeException(
                errorCode = TicketMessageCode.CREDENTIAL_NOT_FOUND,
                params = arrayOf(credentialId)
            )
        }
        if (!credentialHelper.isValid(credential)) {
            throw ErrorCodeException(
                errorCode = TicketMessageCode.CREDENTIAL_FORMAT_INVALID
            )
        }
        if (!credential.credentialName.isNullOrBlank()) {
            if (credential.credentialName!!.length > CREDENTIAL_NAME_MAX_SIZE) {
                throw ErrorCodeException(
                    errorCode = TicketMessageCode.CREDENTIAL_NAME_TOO_LONG
                )
            }
            if (!CREDENTIAL_NAME_REGEX.matches(credential.credentialName!!)) {
                throw ErrorCodeException(
                    errorCode = TicketMessageCode.CREDENTIAL_NAME_ILLEGAL
                )
            }
        }

        logger.info("$userId edit credential $credentialId")
        credentialDao.updateIgnoreNull(
            dslContext = dslContext,
            projectId = projectId,
            credentialId = credentialId,
            credentialV1 = credentialHelper.encryptCredential(credential.v1),
            credentialV2 = credentialHelper.encryptCredential(credential.v2),
            credentialV3 = credentialHelper.encryptCredential(credential.v3),
            credentialV4 = credentialHelper.encryptCredential(credential.v4),
            credentialRemark = credential.credentialRemark,
            credentialName = if (credential.credentialName.isNullOrBlank()) {
                credentialId
            } else {
                credential.credentialName!!
            },
            updateUser = userId
        )
    }

    @ActionAuditRecord(
        actionId = ActionId.CREDENTIAL_CREATE,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.CREDENTIAL,
            instanceIds = "#credential?.credentialId",
            instanceNames = "#credential?.credentialName"
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.CREDENTIAL_CREATE_CONTENT
    )
    override fun userCreate(
        userId: String,
        projectId: String,
        credential: CredentialCreate,
        authGroupList: List<BkAuthGroup>?
    ) {
        val create = AuthPermission.CREATE
        credentialPermissionService.validatePermission(
            userId = userId,
            projectId = projectId,
            authPermission = create,
            message = MessageUtil.getMessageByLocale(
                USER_NO_ENGINEERING_CREDENTIAL_OPERATE_PERMISSIONS,
                I18nUtil.getLanguage(userId),
                arrayOf(
                    userId,
                    projectId,
                    "",
                    create.getI18n(I18nUtil.getLanguage(userId))
                )
            )
        )
        if (credentialDao.has(dslContext, projectId, credential.credentialId)) {
            throw ErrorCodeException(
                errorCode = TicketMessageCode.CREDENTIAL_EXIST,
                params = arrayOf(credential.credentialId)
            )
        }
        if (!credentialHelper.isValid(credential)) {
            throw ErrorCodeException(
                errorCode = TicketMessageCode.CREDENTIAL_FORMAT_INVALID
            )
        }
        if (credential.credentialId.length > CREDENTIAL_ID_MAX_SIZE) {
            throw ErrorCodeException(
                errorCode = TicketMessageCode.CREDENTIAL_ID_TOO_LONG
            )
        }
        if (!CREDENTIAL_ID_REGEX.matches(credential.credentialId)) {
            throw ErrorCodeException(
                errorCode = TicketMessageCode.CREDENTIAL_ID_ILLEGAL
            )
        }
        if (!credential.credentialName.isNullOrBlank()) {
            if (credential.credentialName!!.length > CREDENTIAL_NAME_MAX_SIZE) {
                throw ErrorCodeException(
                    errorCode = TicketMessageCode.CREDENTIAL_NAME_TOO_LONG
                )
            }
            if (!CREDENTIAL_NAME_REGEX.matches(credential.credentialName!!)) {
                throw ErrorCodeException(
                    errorCode = TicketMessageCode.CREDENTIAL_NAME_ILLEGAL
                )
            }
        }

        logger.info("$userId create credential ${credential.credentialId}")
        credentialDao.create(
            dslContext = dslContext,
            projectId = projectId,
            credentialUserId = userId,
            credentialId = credential.credentialId,
            credentialName = if (credential.credentialName.isNullOrBlank()) {
                credential.credentialId
            } else {
                credential.credentialName!!
            },
            credentialType = credential.credentialType.name,
            credentialV1 = credentialHelper.encryptCredential(credential.v1)!!,
            credentialV2 = credentialHelper.encryptCredential(credential.v2),
            credentialV3 = credentialHelper.encryptCredential(credential.v3),
            credentialV4 = credentialHelper.encryptCredential(credential.v4),
            credentialRemark = credential.credentialRemark
        )
        credentialPermissionService.createResource(userId, projectId, credential.credentialId, authGroupList)
    }

    @ActionAuditRecord(
        actionId = ActionId.CREDENTIAL_EDIT,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.CREDENTIAL
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.CREDENTIAL_EDIT_CONTENT
    )
    override fun userEdit(
        userId: String,
        projectId: String,
        credentialId: String,
        credential: CredentialUpdate
    ) {
        val edit = AuthPermission.EDIT
        credentialPermissionService.validatePermission(
            userId = userId,
            projectId = projectId,
            resourceCode = credentialId,
            authPermission = edit,
            message = MessageUtil.getMessageByLocale(
                USER_NO_ENGINEERING_CREDENTIAL_OPERATE_PERMISSIONS,
                I18nUtil.getLanguage(userId),
                arrayOf(
                    userId,
                    projectId,
                    credentialId,
                    edit.getI18n(I18nUtil.getLanguage(userId))
                )
            )
        )
        ActionAuditContext.current()
            .setInstanceId(credentialId)
            .setInstanceName(credential.credentialName)
            .setInstance(credential)
        serviceEdit(
            userId = userId,
            projectId = projectId,
            credentialId = credentialId,
            credential = credential
        )
    }

    @ActionAuditRecord(
        actionId = ActionId.CREDENTIAL_EDIT,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.CREDENTIAL,
            instanceIds = "#credentialId",
            instanceNames = "#credentialId"
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.CREDENTIAL_EDIT_SETTING_CONTENT
    )
    override fun userSettingEdit(
        userId: String,
        projectId: String,
        credentialId: String,
        credentialSetting: CredentialSettingUpdate
    ): Boolean {
        val edit = AuthPermission.EDIT
        credentialPermissionService.validatePermission(
            userId = userId,
            projectId = projectId,
            resourceCode = credentialId,
            authPermission = edit,
            message = MessageUtil.getMessageByLocale(
                USER_NO_ENGINEERING_CREDENTIAL_OPERATE_PERMISSIONS,
                I18nUtil.getLanguage(userId),
                arrayOf(
                    userId,
                    projectId,
                    credentialId,
                    edit.getI18n(I18nUtil.getLanguage(userId))
                )
            )
        )

        ActionAuditContext.current()
            .setInstanceId(credentialId)
            .setInstanceName(credentialId)
            .setInstance(credentialSetting)
        return credentialDao.updateSetting(
            dslContext = dslContext,
            projectId = projectId,
            credentialId = credentialId,
            allowAcrossProject = credentialSetting.allowAcrossProject
        ) > 0
    }

    @ActionAuditRecord(
        actionId = ActionId.CREDENTIAL_DELETE,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.CREDENTIAL,
            instanceIds = "#credentialId",
            instanceNames = "#credentialId"
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.CREDENTIAL_DELETE_CONTENT
    )
    override fun userDelete(userId: String, projectId: String, credentialId: String) {
        val delete = AuthPermission.DELETE
        credentialPermissionService.validatePermission(
            userId = userId,
            projectId = projectId,
            resourceCode = credentialId,
            authPermission = delete,
            message = MessageUtil.getMessageByLocale(
                USER_NO_ENGINEERING_CREDENTIAL_OPERATE_PERMISSIONS,
                I18nUtil.getLanguage(userId),
                arrayOf(
                    userId,
                    projectId,
                    credentialId,
                    delete.getI18n(I18nUtil.getLanguage(userId))
                )
            )
        )
        logger.info("$userId delete credential $credentialId")
        credentialPermissionService.deleteResource(projectId, credentialId)
        credentialDao.delete(dslContext, projectId, credentialId)
    }

    override fun userList(
        userId: String,
        projectId: String,
        credentialTypes: List<CredentialType>?,
        offset: Int,
        limit: Int,
        keyword: String?
    ): SQLPage<CredentialWithPermission> {
        val permissionToListMap = credentialPermissionService.filterCredentials(
            userId = userId,
            projectId = projectId,
            authPermissions = setOf(
                AuthPermission.LIST,
                AuthPermission.DELETE,
                AuthPermission.VIEW,
                AuthPermission.EDIT,
                AuthPermission.USE
            )
        )
        if (permissionToListMap.isNullOrEmpty()) {
            return SQLPage(0, emptyList())
        }
        val hasListPermissionCredentialIdList = permissionToListMap[AuthPermission.LIST]!!
        val hasDeletePermissionCredentialIdList = permissionToListMap[AuthPermission.DELETE]!!
        val hasViewPermissionCredentialIdList = permissionToListMap[AuthPermission.VIEW]!!
        val hasEditPermissionCredentialIdList = permissionToListMap[AuthPermission.EDIT]!!
        val hasUsePermissionCredentialIdList = permissionToListMap[AuthPermission.USE]!!

        val count = credentialDao.countByProject(
            dslContext,
            projectId,
            credentialTypes?.toSet(),
            hasListPermissionCredentialIdList.toSet()
        )
        val credentialRecordList = credentialDao.listByProject(
            dslContext,
            projectId,
            credentialTypes?.toSet(),
            hasListPermissionCredentialIdList.toSet(),
            offset,
            limit,
            keyword
        )
        val credentialList = credentialRecordList.map {
            val hasDeletePermission = hasDeletePermissionCredentialIdList.contains(it.credentialId)
            val hasViewPermission = hasViewPermissionCredentialIdList.contains(it.credentialId)
            val hasEditPermission = hasEditPermissionCredentialIdList.contains(it.credentialId)
            val hasUsePermission = hasUsePermissionCredentialIdList.contains(it.credentialId)
            CredentialWithPermission(
                credentialId = it.credentialId,
                credentialName = it.credentialName ?: it.credentialId,
                credentialType = CredentialType.valueOf(it.credentialType),
                credentialRemark = it.credentialRemark,
                updatedTime = it.updatedTime.timestamp(),
                v1 = credentialHelper.credentialMixer,
                v2 = credentialHelper.credentialMixer,
                v3 = credentialHelper.credentialMixer,
                v4 = credentialHelper.credentialMixer,
                permissions = CredentialPermissions(
                    delete = hasDeletePermission,
                    view = hasViewPermission,
                    edit = hasEditPermission,
                    use = hasUsePermission
                ),
                updateUser = it.updateUser,
                allowAcrossProject = it.allowAcrossProject,
                createTime = it.createdTime.timestamp(),
                createUser = it.credentialUserId
            )
        }
        return SQLPage(count, credentialList)
    }

    override fun hasPermissionList(
        userId: String,
        projectId: String,
        credentialTypes: List<CredentialType>?,
        authPermission: AuthPermission,
        offset: Int?,
        limit: Int?,
        keyword: String?
    ): SQLPage<Credential> {
        val hasPermissionList = credentialPermissionService.filterCredential(userId, projectId, authPermission)
        logger.info("hasPermissionList $hasPermissionList")
        val count =
            credentialDao.countByProject(dslContext, projectId, credentialTypes?.toSet(), hasPermissionList.toSet())
        val credentialRecordList = credentialDao.listByProject(
            dslContext,
            projectId,
            credentialTypes?.toSet(),
            hasPermissionList.toSet(),
            offset,
            limit,
            keyword
        )
        logger.info("credentialRecordList $credentialRecordList")
        val credentialList = credentialRecordList.map {
            Credential(
                credentialId = it.credentialId,
                credentialName = it.credentialName ?: it.credentialId,
                credentialType = CredentialType.valueOf(it.credentialType),
                credentialRemark = it.credentialRemark,
                updatedTime = it.createdTime.timestamp(),
                v1 = credentialHelper.credentialMixer,
                v2 = credentialHelper.credentialMixer,
                v3 = credentialHelper.credentialMixer,
                v4 = credentialHelper.credentialMixer,
                updateUser = it.updateUser
            )
        }
        return SQLPage(count, credentialList)
    }

    override fun serviceList(projectId: String, offset: Int, limit: Int): SQLPage<Credential> {
        val count = credentialDao.countByProject(dslContext, projectId)
        val credentialRecords = credentialDao.listByProject(dslContext, projectId, offset, limit)
        val result = credentialRecords.map {
            Credential(
                credentialId = it.credentialId,
                credentialName = it.credentialName ?: it.credentialId,
                credentialType = CredentialType.valueOf(it.credentialType),
                credentialRemark = it.credentialRemark,
                updatedTime = it.createdTime.timestamp(),
                v1 = credentialHelper.credentialMixer,
                v2 = credentialHelper.credentialMixer,
                v3 = credentialHelper.credentialMixer,
                v4 = credentialHelper.credentialMixer,
                updateUser = it.updateUser
            )
        }
        return SQLPage(count, result)
    }

    override fun serviceCheck(projectId: String, credentialId: String) {
        if (!credentialDao.has(dslContext, projectId, credentialId)) {
            throw NotFoundException("Credential $credentialId does not exists")
        }
    }

    @ActionAuditRecord(
        actionId = ActionId.CREDENTIAL_VIEW,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.CREDENTIAL
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.CREDENTIAL_VIEW_CONTENT
    )
    override fun userShow(userId: String, projectId: String, credentialId: String): CredentialWithPermission {
        val view = AuthPermission.VIEW
        credentialPermissionService.validatePermission(
            userId = userId,
            projectId = projectId,
            resourceCode = credentialId,
            authPermission = view,
            message = MessageUtil.getMessageByLocale(
                USER_NO_ENGINEERING_CREDENTIAL_OPERATE_PERMISSIONS,
                I18nUtil.getLanguage(userId),
                arrayOf(
                    userId,
                    projectId,
                    credentialId,
                    view.getI18n(I18nUtil.getLanguage(userId))
                )
            )
        )
        val hasViewPermission = true
        val hasDeletePermission =
            credentialPermissionService.validatePermission(userId, projectId, credentialId, AuthPermission.DELETE)
        val hasEditPermission =
            credentialPermissionService.validatePermission(userId, projectId, credentialId, AuthPermission.EDIT)

        val credentialRecord = credentialDao.get(dslContext, projectId, credentialId)

        ActionAuditContext.current()
            .setInstanceName(credentialRecord.credentialName)
            .setInstanceId(credentialRecord.credentialId)
        return CredentialWithPermission(
            credentialId = credentialId,
            credentialName = credentialRecord.credentialName ?: credentialId,
            credentialType = CredentialType.valueOf(credentialRecord.credentialType),
            credentialRemark = credentialRecord.credentialRemark,
            updatedTime = credentialRecord.updatedTime.timestamp(),
            v1 = credentialHelper.decryptCredential(credentialRecord.credentialV1)!!,
            v2 = credentialHelper.decryptCredential(credentialRecord.credentialV2),
            v3 = credentialHelper.decryptCredential(credentialRecord.credentialV3),
            v4 = credentialHelper.decryptCredential(credentialRecord.credentialV4),
            permissions = CredentialPermissions(
                hasDeletePermission,
                hasViewPermission,
                hasEditPermission
            ),
            updateUser = credentialRecord.updateUser,
            createTime = credentialRecord.createdTime.timestamp(),
            createUser = credentialRecord.credentialUserId
        )
    }

    @ActionAuditRecord(
        actionId = ActionId.CREDENTIAL_VIEW,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.CREDENTIAL
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.CREDENTIAL_VIEW_CONTENT
    )
    override fun userGet(userId: String, projectId: String, credentialId: String): CredentialWithPermission {
        val view = AuthPermission.VIEW
        credentialPermissionService.validatePermission(
            userId,
            projectId,
            credentialId,
            view,
            MessageUtil.getMessageByLocale(
                USER_NO_ENGINEERING_CREDENTIAL_OPERATE_PERMISSIONS,
                I18nUtil.getLanguage(userId),
                arrayOf(
                    userId,
                    projectId,
                    credentialId,
                    view.getI18n(I18nUtil.getLanguage(userId))
                )
            )
        )
        val hasViewPermission = true
        val hasDeletePermission =
            credentialPermissionService.validatePermission(userId, projectId, credentialId, AuthPermission.DELETE)
        val hasEditPermission =
            credentialPermissionService.validatePermission(userId, projectId, credentialId, AuthPermission.EDIT)

        val credentialRecord = credentialDao.get(dslContext, projectId, credentialId)

        ActionAuditContext.current()
            .setInstanceId(credentialRecord.credentialId)
            .setInstanceName(credentialRecord.credentialName)
        return CredentialWithPermission(
            credentialId = credentialId,
            credentialName = credentialRecord.credentialName ?: credentialId,
            credentialType = CredentialType.valueOf(credentialRecord.credentialType),
            credentialRemark = credentialRecord.credentialRemark,
            updatedTime = credentialRecord.updatedTime.timestamp(),
            v1 = credentialHelper.credentialMixer,
            v2 = credentialHelper.credentialMixer,
            v3 = credentialHelper.credentialMixer,
            v4 = credentialHelper.credentialMixer,
            permissions = CredentialPermissions(
                hasDeletePermission,
                hasViewPermission,
                hasEditPermission
            ),
            updateUser = credentialRecord.updateUser,
            allowAcrossProject = credentialRecord.allowAcrossProject,
            createTime = credentialRecord.createdTime.timestamp(),
            createUser = credentialRecord.credentialUserId
        )
    }

    @ActionAuditRecord(
        actionId = ActionId.CREDENTIAL_VIEW,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.CREDENTIAL,
            instanceNames = "#credentialId",
            instanceIds = "#credentialId"
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.CREDENTIAL_VIEW_CONTENT
    )
    override fun buildGet(
        projectId: String,
        buildId: String,
        credentialId: String,
        publicKey: String,
        taskId: String?,
        padding: Boolean
    ): CredentialInfo? {
        val buildBasicInfoResult = client.get(ServiceBuildResource::class).serviceBasic(projectId, buildId)
        if (buildBasicInfoResult.isNotOk()) {
            throw RemoteServiceException("Failed to build the basic information based on the buildId")
        }
        val buildBasicInfo = buildBasicInfoResult.data
            ?: throw RemoteServiceException("Failed to build the basic information based on the buildId")
        return serviceGet(
            projectId = buildBasicInfo.projectId,
            credentialId = credentialId,
            publicKey = publicKey,
            padding = padding
        ) ?: run {
            // 获取跨项目凭证
            val acrossInfo = getAcrossInfo(projectId, buildId, taskId) ?: return null
            serviceGet(
                projectId = acrossInfo.targetProjectId,
                credentialId = credentialId,
                publicKey = publicKey,
                padding = padding
            )
        }
    }

    override fun buildGetAcrossProject(
        projectId: String,
        targetProjectId: String,
        buildId: String,
        credentialId: String,
        publicKey: String,
        padding: Boolean
    ): CredentialInfo? {
        val buildBasicInfoResult = client.get(ServiceBuildResource::class).serviceBasic(projectId, buildId)
        if (buildBasicInfoResult.isNotOk()) {
            throw RemoteServiceException("Failed to build the basic information based on the buildId")
        }
        buildBasicInfoResult.data
            ?: throw RemoteServiceException("Failed to build the basic information based on the buildId")
        return serviceGetAcrossProject(
            targetProjectId = targetProjectId,
            credentialId = credentialId,
            publicKey = publicKey,
            padding = padding
        )
    }

    override fun buildGetDetail(
        projectId: String,
        buildId: String,
        taskId: String?,
        credentialId: String
    ): Map<String, String> {
        val buildBasicInfoResult = client.get(ServiceBuildResource::class).serviceBasic(projectId, buildId)
        if (buildBasicInfoResult.isNotOk()) {
            throw RemoteServiceException("Failed to build the basic information based on the buildId")
        }
        val buildBasicInfo = buildBasicInfoResult.data
            ?: throw RemoteServiceException("Failed to build the basic information based on the buildId")

        val credentialInfo = try {
            serviceGet(buildBasicInfo.projectId, credentialId)
        } catch (ignore: Exception) {
            // 如果没有跨项目信息则直接抛出异常
            val acrossInfo = getAcrossInfo(projectId, buildId, taskId) ?: throw ignore
            serviceAcrossGet(acrossInfo.targetProjectId, credentialId)
        }

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

    @ActionAuditRecord(
        actionId = ActionId.CREDENTIAL_VIEW,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.CREDENTIAL,
            instanceNames = "#credentialId",
            instanceIds = "#credentialId"
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.CREDENTIAL_VIEW_CONTENT
    )
    override fun serviceGet(
        projectId: String,
        credentialId: String,
        publicKey: String,
        padding: Boolean
    ): CredentialInfo? {
        val credentialRecord = credentialDao.getOrNull(dslContext, projectId, credentialId) ?: return null

        return credentialInfo(publicKey = publicKey, credentialRecord = credentialRecord, padding = padding)
    }

    @ActionAuditRecord(
        actionId = ActionId.CREDENTIAL_VIEW,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.CREDENTIAL,
            instanceNames = "#credentialId",
            instanceIds = "#credentialId"
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#targetProjectId")],
        scopeId = "#targetProjectId",
        content = ActionAuditContent.CREDENTIAL_VIEW_CONTENT
    )
    override fun serviceGetAcrossProject(
        targetProjectId: String,
        credentialId: String,
        publicKey: String,
        padding: Boolean
    ): CredentialInfo? {
        val credentialRecord = credentialDao.getOrNull(dslContext, targetProjectId, credentialId)?.let {
            if (!it.allowAcrossProject) {
                throw CustomException(Response.Status.FORBIDDEN, "credential not allow across project")
            } else {
                it
            }
        } ?: return null

        return credentialInfo(publicKey = publicKey, credentialRecord = credentialRecord, padding = padding)
    }

    private fun credentialInfo(
        publicKey: String,
        credentialRecord: TCredentialRecord,
        padding: Boolean
    ): CredentialInfo {
        val publicKeyByteArray = Base64.getDecoder().decode(publicKey)
        val serverDHKeyPair = DHUtil.initKey(publicKeyByteArray)
        val serverPublicKeyByteArray = serverDHKeyPair.publicKey
        val serverPrivateKeyByteArray = serverDHKeyPair.privateKey
        val serverBase64PublicKey = String(Base64.getEncoder().encode(serverPublicKeyByteArray))

        val credentialV1 = credentialHelper.encryptCredential(
            aesEncryptedCredential = credentialRecord.credentialV1,
            publicKeyByteArray = publicKeyByteArray,
            serverPrivateKeyByteArray = serverPrivateKeyByteArray,
            padding = padding
        )!!
        val credentialV2 = credentialHelper.encryptCredential(
            aesEncryptedCredential = credentialRecord.credentialV2,
            publicKeyByteArray = publicKeyByteArray,
            serverPrivateKeyByteArray = serverPrivateKeyByteArray,
            padding = padding
        )
        val credentialV3 = credentialHelper.encryptCredential(
            aesEncryptedCredential = credentialRecord.credentialV3,
            publicKeyByteArray = publicKeyByteArray,
            serverPrivateKeyByteArray = serverPrivateKeyByteArray,
            padding = padding
        )
        val credentialV4 = credentialHelper.encryptCredential(
            aesEncryptedCredential = credentialRecord.credentialV4,
            publicKeyByteArray = publicKeyByteArray,
            serverPrivateKeyByteArray = serverPrivateKeyByteArray,
            padding = padding
        )

        return CredentialInfo(
            publicKey = serverBase64PublicKey,
            credentialType = CredentialType.valueOf(credentialRecord.credentialType),
            v1 = credentialV1,
            v2 = credentialV2,
            v3 = credentialV3,
            v4 = credentialV4,
            allowAcrossProject = credentialRecord.allowAcrossProject
        )
    }

    @ActionAuditRecord(
        actionId = ActionId.CREDENTIAL_VIEW,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.CREDENTIAL,
            instanceNames = "#credentialId",
            instanceIds = "#credentialId"
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.CREDENTIAL_VIEW_CONTENT
    )
    override fun serviceGet(projectId: String, credentialId: String): Credential {
        val record = credentialDao.get(dslContext, projectId, credentialId)

        return Credential(
            credentialId = record.credentialId,
            credentialName = record.credentialName ?: record.credentialId,
            credentialType = CredentialType.valueOf(record.credentialType),
            credentialRemark = record.credentialRemark,
            updatedTime = record.updatedTime.timestamp(),
            v1 = credentialHelper.decryptCredential(record.credentialV1)!!,
            v2 = credentialHelper.decryptCredential(record.credentialV2),
            v3 = credentialHelper.decryptCredential(record.credentialV3),
            v4 = credentialHelper.decryptCredential(record.credentialV4),
            updateUser = record.updateUser
        )
    }

    private fun serviceAcrossGet(projectId: String, credentialId: String): Credential {
        val record = credentialDao.get(dslContext, projectId, credentialId).also {
            if (!it.allowAcrossProject) {
                throw CustomException(Response.Status.FORBIDDEN, "credential not allow across project")
            }
        }

        return Credential(
            credentialId = record.credentialId,
            credentialName = record.credentialName ?: record.credentialId,
            credentialType = CredentialType.valueOf(record.credentialType),
            credentialRemark = record.credentialRemark,
            updatedTime = record.updatedTime.timestamp(),
            v1 = credentialHelper.decryptCredential(record.credentialV1)!!,
            v2 = credentialHelper.decryptCredential(record.credentialV2),
            v3 = credentialHelper.decryptCredential(record.credentialV3),
            v4 = credentialHelper.decryptCredential(record.credentialV4),
            updateUser = record.updateUser
        )
    }

    @AuditEntry(actionId = ActionId.CREDENTIAL_VIEW)
    @ActionAuditRecord(
        actionId = ActionId.CREDENTIAL_VIEW,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.CREDENTIAL
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.CREDENTIAL_VIEW_CONTENT
    )
    override fun getCredentialByIds(projectId: String?, credentialIds: Set<String>): List<Credential>? {
        val records = credentialDao.listByProject(
            dslContext = dslContext,
            credentialIds = credentialIds,
            credentialTypes = null,
            projectId = projectId,
            limit = null,
            offset = null,
            keyword = null
        )
        return records.map {
            ActionAuditContext.current().addInstanceInfo(
                it.credentialId,
                it.credentialName,
                null,
                null
            )
            Credential(
                credentialId = it.credentialId,
                credentialName = it.credentialName ?: it.credentialId,
                credentialType = CredentialType.valueOf(it.credentialType),
                credentialRemark = it.credentialRemark,
                updatedTime = it.createdTime.timestamp(),
                v1 = credentialHelper.credentialMixer,
                v2 = credentialHelper.credentialMixer,
                v3 = credentialHelper.credentialMixer,
                v4 = credentialHelper.credentialMixer,
                updateUser = it.updateUser,
                createUser = it.credentialUserId
            )
        }
    }

    override fun searchByCredentialId(
        projectId: String,
        offset: Int,
        limit: Int,
        credentialId: String
    ): SQLPage<Credential> {
        val count = credentialDao.countByIdLike(dslContext, projectId, credentialId)
        val credentialRecords = credentialDao.searchByIdLike(
            dslContext = dslContext,
            projectId = projectId,
            offset = offset,
            limit = limit,
            credentialId = credentialId
        )
        val result = credentialRecords.map {
            Credential(
                credentialId = it.credentialId,
                credentialName = it.credentialName ?: it.credentialId,
                credentialType = CredentialType.valueOf(it.credentialType),
                credentialRemark = it.credentialRemark,
                updatedTime = it.createdTime.timestamp(),
                v1 = credentialHelper.credentialMixer,
                v2 = credentialHelper.credentialMixer,
                v3 = credentialHelper.credentialMixer,
                v4 = credentialHelper.credentialMixer,
                updateUser = it.updateUser
            )
        }
        return SQLPage(count, result)
    }

    override fun getCredentialItem(
        projectId: String,
        credentialId: String,
        publicKey: String,
        padding: Boolean
    ): CredentialItemVo? {
        return serviceGet(
            projectId = projectId,
            credentialId = credentialId,
            publicKey = publicKey,
            padding = padding
        )?.convertCredentialItem()
    }

    private fun getAcrossInfo(
        projectId: String,
        buildId: String,
        taskId: String?
    ): BuildTemplateAcrossInfo? {
        if (taskId.isNullOrBlank()) {
            return null
        }
        val templateId = client.get(ServiceVarResource::class).getBuildVar(
            projectId = projectId,
            pipelineId = null,
            buildId = buildId,
            varName = TEMPLATE_ACROSS_INFO_ID
        ).data?.get(TEMPLATE_ACROSS_INFO_ID)
        if (templateId.isNullOrBlank()) {
            return null
        }

        val acrossInfoList = client.get(ServiceTemplateAcrossResource::class).getBuildAcrossTemplateInfo(
            projectId = projectId,
            templateId = templateId
        ).data?.ifEmpty { return null } ?: return null

        return acrossInfoList.firstOrNull {
            it.templateType == TemplateAcrossInfoType.STEP &&
                it.templateInstancesIds.contains(taskId)
        }
    }

    private fun CredentialInfo.convertCredentialItem(): CredentialItemVo {
        val credentialItem = when (credentialType) {
            CredentialType.PASSWORD ->
                PasswordCredentialItem(password = v1)

            CredentialType.ACCESSTOKEN ->
                AccessTokenCredentialItem(accessToken = v1)

            CredentialType.OAUTHTOKEN ->
                OauthTokenCredentialItem(oauthToken = v1)

            CredentialType.USERNAME_PASSWORD ->
                UserPassCredentialItem(username = v1, password = v2!!)

            CredentialType.SSH_PRIVATEKEY ->
                SshPrivateKeyCredentialItem(privateKey = v1, passphrase = v2)

            CredentialType.TOKEN_SSH_PRIVATEKEY ->
                TokenSshPrivateKeyCredentialItem(token = v1, privateKey = v2!!, passphrase = v3)

            CredentialType.TOKEN_USERNAME_PASSWORD ->
                TokenUserPassCredentialItem(token = v1, username = v2!!, password = v3!!)

            else ->
                throw IllegalArgumentException("unsupported credentialType $credentialType")
        }
        return CredentialItemVo(publicKey = publicKey, credentialItem = credentialItem)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CredentialServiceImpl::class.java)
        private const val CREDENTIAL_ID_MAX_SIZE = 40
        private const val CREDENTIAL_NAME_MAX_SIZE = 64
        private val CREDENTIAL_ID_REGEX = Regex("^[0-9a-zA-Z_]+$")
        private val CREDENTIAL_NAME_REGEX = Regex("^[a-zA-Z0-9_\u4e00-\u9fa5-.]+$")
    }
}
