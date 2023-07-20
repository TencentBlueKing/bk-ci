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

package com.tencent.devops.auth.service

import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.bk.sdk.iam.dto.CallbackApplicationDTO
import com.tencent.bk.sdk.iam.dto.GradeManagerApplicationCreateDTO
import com.tencent.bk.sdk.iam.dto.GradeManagerApplicationUpdateDTO
import com.tencent.bk.sdk.iam.dto.V2PageInfoDTO
import com.tencent.bk.sdk.iam.dto.itsm.ItsmAttrs
import com.tencent.bk.sdk.iam.dto.itsm.ItsmColumn
import com.tencent.bk.sdk.iam.dto.itsm.ItsmContentDTO
import com.tencent.bk.sdk.iam.dto.itsm.ItsmScheme
import com.tencent.bk.sdk.iam.dto.itsm.ItsmStyle
import com.tencent.bk.sdk.iam.dto.itsm.ItsmValue
import com.tencent.bk.sdk.iam.dto.manager.ManagerRoleGroup
import com.tencent.bk.sdk.iam.dto.manager.ManagerScopes
import com.tencent.bk.sdk.iam.dto.manager.V2ManagerRoleGroupInfo
import com.tencent.bk.sdk.iam.dto.manager.dto.CreateManagerDTO
import com.tencent.bk.sdk.iam.dto.manager.dto.ManagerRoleGroupDTO
import com.tencent.bk.sdk.iam.dto.manager.dto.SearchGroupDTO
import com.tencent.bk.sdk.iam.dto.manager.dto.UpdateManagerDTO
import com.tencent.bk.sdk.iam.service.v2.V2ManagerService
import com.tencent.devops.auth.constant.AuthI18nConstants.BK_AUTH_SECRECY
import com.tencent.devops.auth.constant.AuthI18nConstants.BK_CREATE_BKCI_PROJECT_APPLICATION
import com.tencent.devops.auth.constant.AuthI18nConstants.BK_CREATE_PROJECT_APPROVAL
import com.tencent.devops.auth.constant.AuthI18nConstants.BK_ORGANIZATION
import com.tencent.devops.auth.constant.AuthI18nConstants.BK_PROJECT_DESC
import com.tencent.devops.auth.constant.AuthI18nConstants.BK_PROJECT_ID
import com.tencent.devops.auth.constant.AuthI18nConstants.BK_PROJECT_NAME
import com.tencent.devops.auth.constant.AuthI18nConstants.BK_REVISE_BKCI_PROJECT_APPLICATION
import com.tencent.devops.auth.constant.AuthI18nConstants.BK_SUBJECT_SCOPES
import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.dao.AuthItsmCallbackDao
import com.tencent.devops.auth.dao.AuthResourceGroupConfigDao
import com.tencent.devops.auth.dao.AuthResourceGroupDao
import com.tencent.devops.auth.pojo.ItsmCancelApplicationInfo
import com.tencent.devops.auth.pojo.event.AuthResourceGroupCreateEvent
import com.tencent.devops.auth.pojo.event.AuthResourceGroupModifyEvent
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.DefaultGroupType
import com.tencent.devops.common.auth.api.pojo.SubjectScopeInfo
import com.tencent.devops.common.auth.callback.AuthConstants.ALL_MEMBERS
import com.tencent.devops.common.auth.callback.AuthConstants.ALL_MEMBERS_NAME
import com.tencent.devops.common.auth.enums.SubjectScopeType
import com.tencent.devops.common.auth.utils.IamGroupUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.trace.TraceEventDispatcher
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.project.api.service.ServiceProjectApprovalResource
import com.tencent.devops.project.pojo.enums.ProjectApproveStatus
import com.tencent.devops.project.pojo.enums.ProjectAuthSecrecyStatus
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import java.util.Arrays

@Suppress("LongParameterList", "TooManyFunctions")
class PermissionGradeManagerService @Autowired constructor(
    private val client: Client,
    private val permissionGroupPoliciesService: PermissionGroupPoliciesService,
    private val iamV2ManagerService: V2ManagerService,
    private val iamConfiguration: IamConfiguration,
    private val authItsmCallbackDao: AuthItsmCallbackDao,
    private val dslContext: DSLContext,
    private val authResourceService: AuthResourceService,
    private val authResourceGroupDao: AuthResourceGroupDao,
    private val authResourceGroupConfigDao: AuthResourceGroupConfigDao,
    private val traceEventDispatcher: TraceEventDispatcher,
    private val itsmService: ItsmService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(PermissionGradeManagerService::class.java)
        private const val DEPARTMENT = "department"
        private const val CANCEL_ITSM_APPLICATION_ACTION = "WITHDRAW"
    }

    @Value("\${itsm.callback.update.url:#{null}}")
    private val itsmUpdateCallBackUrl: String = ""

    @Value("\${itsm.callback.create.url:#{null}}")
    private val itsmCreateCallBackUrl: String = ""

    /**
     * 创建分级管理员
     */
    @SuppressWarnings("LongParameterList", "LongMethod")
    fun createGradeManager(
        userId: String,
        projectCode: String,
        projectName: String,
        resourceType: String,
        resourceCode: String,
        resourceName: String
    ): Int {
        val projectApprovalInfo = client.get(ServiceProjectApprovalResource::class).get(projectId = projectCode).data
            ?: throw ErrorCodeException(
                errorCode = AuthMessageCode.RELATED_RESOURCE_EMPTY,
                params = arrayOf(projectCode),
                defaultMessage = "the resource not exists, projectCode:$projectCode"
            )
        val name = IamGroupUtils.buildGradeManagerName(
            projectName = resourceName
        )
        val manageGroupConfig = authResourceGroupConfigDao.get(
            dslContext = dslContext,
            resourceType = resourceType,
            groupCode = DefaultGroupType.MANAGER.value
        ) ?: throw ErrorCodeException(
            errorCode = AuthMessageCode.ERROR_AUTH_RESOURCE_GROUP_CONFIG_NOT_EXIST,
            params = arrayOf(DefaultGroupType.MANAGER.value),
            defaultMessage = "${resourceType}_${DefaultGroupType.MANAGER.value} group config  not exist"
        )
        val description = manageGroupConfig.description
        val authorizationScopes = permissionGroupPoliciesService.buildAuthorizationScopes(
            authorizationScopesStr = manageGroupConfig.authorizationScopes,
            projectCode = projectCode,
            projectName = projectName,
            iamResourceCode = projectCode,
            resourceName = projectName
        )
        val subjectScopes = projectApprovalInfo.subjectScopes?.map {
            when (it.type) {
                SubjectScopeType.DEPARTMENT.value -> ManagerScopes(DEPARTMENT, it.id)
                SubjectScopeType.USER.value -> ManagerScopes(it.type, it.username)
                else -> ManagerScopes(it.type, it.id)
            }
        } ?: listOf(ManagerScopes(ALL_MEMBERS, ALL_MEMBERS))
        return if (projectApprovalInfo.approvalStatus == ProjectApproveStatus.APPROVED.status) {
            val createManagerDTO = CreateManagerDTO.builder()
                .system(iamConfiguration.systemId)
                .name(name)
                .description(description)
                .members(listOf(userId))
                .authorization_scopes(authorizationScopes)
                .subject_scopes(subjectScopes)
                .sync_perm(true)
                .groupName(manageGroupConfig.groupName)
                .build()
            logger.info("create grade manager|$name|$userId")
            val gradeManagerId = iamV2ManagerService.createManagerV2(createManagerDTO)
            gradeManagerId
        } else {
            val callbackId = UUIDUtil.generate()
            val itsmContentDTO = buildItsmContentDTO(
                projectName = projectName,
                projectId = projectCode,
                desc = projectApprovalInfo.description ?: "",
                organization =
                "${projectApprovalInfo.bgName}-${projectApprovalInfo.deptName}-${projectApprovalInfo.deptName}",
                authSecrecy = projectApprovalInfo.authSecrecy,
                subjectScopes = projectApprovalInfo.subjectScopes ?: listOf(
                    SubjectScopeInfo(
                        id = ALL_MEMBERS,
                        type = ALL_MEMBERS,
                        name = ALL_MEMBERS_NAME
                    )
                )
            )
            val gradeManagerApplicationCreateDTO = GradeManagerApplicationCreateDTO
                .builder()
                .name(name)
                .description(description)
                .members(arrayListOf(userId))
                .authorizationScopes(authorizationScopes)
                .subjectScopes(subjectScopes)
                .syncPerm(true)
                .groupName(manageGroupConfig.groupName)
                .applicant(userId)
                .reason(
                    IamGroupUtils.buildItsmDefaultReason(
                        projectName = projectName,
                        userId = userId,
                        isCreate = true,
                        language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
                    )
                )
                .callbackId(callbackId)
                .callbackUrl(itsmCreateCallBackUrl)
                .content(itsmContentDTO)
                .title(
                    I18nUtil.getCodeLanMessage(
                        messageCode = BK_CREATE_BKCI_PROJECT_APPLICATION,
                        params = arrayOf(projectName)
                    )
                )
                .build()
            logger.info("create grade manager application|$projectCode|$name|$callbackId|$itsmCreateCallBackUrl")
            val createGradeManagerApplication =
                iamV2ManagerService.createGradeManagerApplication(gradeManagerApplicationCreateDTO)
            authItsmCallbackDao.create(
                dslContext = dslContext,
                applyId = createGradeManagerApplication.id,
                sn = createGradeManagerApplication.sn,
                englishName = projectCode,
                callbackId = callbackId,
                applicant = userId
            )
            0
        }
    }

    /**
     * 修改分级管理员
     */
    @SuppressWarnings("LongParameterList", "LongMethod")
    fun modifyGradeManager(
        gradeManagerId: String,
        projectCode: String,
        projectName: String
    ): Boolean {
        val projectApprovalInfo = client.get(ServiceProjectApprovalResource::class).get(projectId = projectCode).data
            ?: throw ErrorCodeException(
                errorCode = AuthMessageCode.RELATED_RESOURCE_EMPTY,
                params = arrayOf(projectCode),
                defaultMessage = "the resource not exists, projectCode:$projectCode"
            )
        val name = IamGroupUtils.buildGradeManagerName(
            projectName = projectName
        )
        val groupConfig = authResourceGroupConfigDao.get(
            dslContext = dslContext,
            resourceType = AuthResourceType.PROJECT.value,
            groupCode = DefaultGroupType.MANAGER.value
        ) ?: throw ErrorCodeException(
            errorCode = AuthMessageCode.ERROR_AUTH_RESOURCE_GROUP_CONFIG_NOT_EXIST,
            params = arrayOf(DefaultGroupType.MANAGER.value),
            defaultMessage = "group config ${DefaultGroupType.MANAGER.value} not exist"
        )
        val authorizationScopes = permissionGroupPoliciesService.buildAuthorizationScopes(
            authorizationScopesStr = groupConfig.authorizationScopes,
            projectCode = projectCode,
            projectName = projectName,
            iamResourceCode = projectCode,
            resourceName = projectName
        )
        val subjectScopes = projectApprovalInfo.subjectScopes?.map {
            when (it.type) {
                SubjectScopeType.DEPARTMENT.value -> ManagerScopes(DEPARTMENT, it.id)
                SubjectScopeType.USER.value -> ManagerScopes(it.type, it.username)
                else -> ManagerScopes(it.type, it.id)
            }
        } ?: listOf(ManagerScopes(ALL_MEMBERS, ALL_MEMBERS))
        return if (projectApprovalInfo.approvalStatus == ProjectApproveStatus.APPROVED.status) {
            val gradeManagerDetail = iamV2ManagerService.getGradeManagerDetail(gradeManagerId)
            val updateManagerDTO = UpdateManagerDTO.builder()
                .name(name)
                .members(gradeManagerDetail.members)
                .description(gradeManagerDetail.description)
                .authorizationScopes(authorizationScopes)
                .subjectScopes(subjectScopes)
                .syncPerm(true)
                .groupName(groupConfig.groupName)
                .build()
            logger.info("update grade manager|$name|${gradeManagerDetail.members}")
            iamV2ManagerService.updateManagerV2(gradeManagerId, updateManagerDTO)
            true
        } else {
            val callbackId = UUIDUtil.generate()
            val itsmContentDTO = buildItsmContentDTO(
                projectName = projectName,
                projectId = projectCode,
                desc = projectApprovalInfo.description ?: "",
                organization =
                "${projectApprovalInfo.bgName}-${projectApprovalInfo.deptName}-${projectApprovalInfo.deptName}",
                authSecrecy = projectApprovalInfo.authSecrecy,
                subjectScopes = projectApprovalInfo.subjectScopes ?: listOf(
                    SubjectScopeInfo(
                        id = ALL_MEMBERS,
                        type = ALL_MEMBERS,
                        name = ALL_MEMBERS_NAME
                    )
                )
            )
            val gradeManagerDetail = iamV2ManagerService.getGradeManagerDetail(gradeManagerId)
            val gradeManagerApplicationUpdateDTO = GradeManagerApplicationUpdateDTO.builder()
                .name(name)
                .description(gradeManagerDetail.description)
                .authorizationScopes(authorizationScopes)
                .subjectScopes(subjectScopes)
                .syncPerm(true)
                .groupName(groupConfig.groupName)
                .applicant(projectApprovalInfo.updator)
                .members(gradeManagerDetail.members)
                .reason(
                    IamGroupUtils.buildItsmDefaultReason(
                        projectName = projectCode,
                        userId = projectApprovalInfo.updator!!,
                        isCreate = false,
                        language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
                    )
                )
                .callbackId(callbackId)
                .callbackUrl(itsmUpdateCallBackUrl)
                .content(itsmContentDTO)
                .title(
                    I18nUtil.getCodeLanMessage(
                        messageCode = BK_REVISE_BKCI_PROJECT_APPLICATION,
                        params = arrayOf(projectName)
                    )
                )
                .build()
            logger.info("update grade manager application|$projectCode|$name|$callbackId|$itsmUpdateCallBackUrl")
            val updateGradeManagerApplication =
                iamV2ManagerService.updateGradeManagerApplication(gradeManagerId, gradeManagerApplicationUpdateDTO)
            authItsmCallbackDao.create(
                dslContext = dslContext,
                applyId = updateGradeManagerApplication.id,
                sn = updateGradeManagerApplication.sn,
                englishName = projectCode,
                callbackId = callbackId,
                applicant = projectApprovalInfo.updator!!
            )
            false
        }
    }

    /**
     * 创建分级管理员默认组
     */
    fun createGradeDefaultGroup(
        gradeManagerId: Int,
        userId: String,
        projectCode: String,
        projectName: String
    ) {
        syncGradeManagerGroup(gradeManagerId = gradeManagerId, projectCode = projectCode, projectName = projectName)
        val defaultGroupConfigs = authResourceGroupConfigDao.get(
            dslContext = dslContext,
            resourceType = AuthResourceType.PROJECT.value,
            createMode = false
        )
        defaultGroupConfigs.forEach { groupConfig ->
            val resourceGroupInfo = authResourceGroupDao.get(
                dslContext = dslContext,
                projectCode = projectCode,
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = projectCode,
                groupCode = groupConfig.groupCode
            )
            if (resourceGroupInfo != null) {
                return@forEach
            }
            val name = groupConfig.groupName
            val description = groupConfig.description
            val managerRoleGroup = ManagerRoleGroup(name, description, false)
            val managerRoleGroupDTO = ManagerRoleGroupDTO.builder().groups(listOf(managerRoleGroup)).build()
            val iamGroupId = iamV2ManagerService.batchCreateRoleGroupV2(gradeManagerId, managerRoleGroupDTO)
            authResourceGroupDao.create(
                dslContext = dslContext,
                projectCode = projectCode,
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = projectCode,
                resourceName = projectName,
                iamResourceCode = projectCode,
                groupCode = groupConfig.groupCode,
                groupName = name,
                defaultGroup = true,
                relationId = iamGroupId.toString()
            )
            permissionGroupPoliciesService.grantGroupPermission(
                authorizationScopesStr = groupConfig.authorizationScopes,
                projectCode = projectCode,
                projectName = projectName,
                iamResourceCode = projectCode,
                resourceName = projectName,
                iamGroupId = iamGroupId
            )
        }
    }

    fun modifyGradeDefaultGroup(
        gradeManagerId: Int,
        projectCode: String,
        projectName: String
    ) {
        val defaultGroupConfigs = authResourceGroupConfigDao.get(
            dslContext = dslContext,
            resourceType = AuthResourceType.PROJECT.value
        )
        defaultGroupConfigs.forEach { groupConfig ->
            authResourceGroupDao.update(
                dslContext = dslContext,
                projectCode = projectCode,
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = projectCode,
                resourceName = projectName,
                groupCode = groupConfig.groupCode,
                groupName = groupConfig.groupName
            )
        }
    }

    /**
     * 同步创建分级管理员时自动创建的组
     */
    private fun syncGradeManagerGroup(
        gradeManagerId: Int,
        projectCode: String,
        projectName: String
    ) {
        val resourceManageGroupInfo = authResourceGroupDao.get(
            dslContext = dslContext,
            projectCode = projectCode,
            resourceType = AuthResourceType.PROJECT.value,
            resourceCode = projectCode,
            groupCode = DefaultGroupType.MANAGER.value
        )
        if (resourceManageGroupInfo != null) {
            return
        }
        val pageInfoDTO = V2PageInfoDTO()
        pageInfoDTO.page = PageUtil.DEFAULT_PAGE
        pageInfoDTO.pageSize = PageUtil.DEFAULT_PAGE_SIZE
        val searchGroupDTO = SearchGroupDTO.builder().inherit(false).build()
        val iamGroupInfoList = iamV2ManagerService.getGradeManagerRoleGroupV2(
            gradeManagerId.toString(),
            searchGroupDTO,
            pageInfoDTO
        )
        iamGroupInfoList.results.forEach { iamGroupInfo ->
            authResourceGroupDao.create(
                dslContext = dslContext,
                projectCode = projectCode,
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = projectCode,
                resourceName = projectName,
                iamResourceCode = projectCode,
                groupCode = DefaultGroupType.MANAGER.value,
                groupName = iamGroupInfo.name,
                defaultGroup = true,
                relationId = iamGroupInfo.id.toString()
            )
        }
    }

    fun deleteGradeManager(gradeManagerId: String) {
        iamV2ManagerService.deleteManagerV2(gradeManagerId)
    }

    /**
     * 驳回取消申请
     */
    fun rejectCancelApplication(callBackId: String): Boolean {
        return iamV2ManagerService.cancelCallbackApplication(callBackId)
    }

    /**
     * 用户主动取消申请
     */
    fun userCancelApplication(
        userId: String,
        projectCode: String
    ): Boolean {
        val callbackRecord =
            authItsmCallbackDao.getCallbackByEnglishName(dslContext = dslContext, projectCode = projectCode)
        // 审批单不存在或者已经结束
        if (callbackRecord == null || callbackRecord.approveResult != null) {
            logger.warn("itsm application has ended, no need to cancel|projectCode:$projectCode")
            return true
        }
        itsmService.cancelItsmApplication(
            ItsmCancelApplicationInfo(
                sn = callbackRecord.sn,
                operator = userId,
                actionType = CANCEL_ITSM_APPLICATION_ACTION
            )
        )
            logger.info("cancel create gradle manager|${callbackRecord.callbackId}|${callbackRecord.sn}")
        return iamV2ManagerService.cancelCallbackApplication(callbackRecord.callbackId)
    }

    fun listGroup(
        gradeManagerId: String,
        page: Int,
        pageSize: Int
    ): List<V2ManagerRoleGroupInfo> {
        val pageInfoDTO = V2PageInfoDTO()
        pageInfoDTO.page = page
        pageInfoDTO.pageSize = pageSize
        val searchGroupDTO = SearchGroupDTO.builder().inherit(false).build()
        val iamGroupInfoList = iamV2ManagerService.getGradeManagerRoleGroupV2(
            gradeManagerId,
            searchGroupDTO,
            pageInfoDTO
        )
        return iamGroupInfoList.results
    }

    fun handleItsmCreateCallback(
        userId: String,
        projectCode: String,
        projectName: String,
        sn: String,
        callBackId: String,
        currentStatus: String
    ): Int {
        logger.info("handle itsm create callback|$userId|$projectCode|$sn|$callBackId|$currentStatus")
        val callbackApplicationDTO = CallbackApplicationDTO
            .builder()
            .sn(sn)
            .currentStatus(currentStatus)
            .approveResult(true).build()
        val gradeManagerId =
            iamV2ManagerService.handleCallbackApplication(callBackId, callbackApplicationDTO).roleId
        authResourceService.create(
            userId = userId,
            projectCode = projectCode,
            resourceType = AuthResourceType.PROJECT.value,
            resourceCode = projectCode,
            resourceName = projectName,
            iamResourceCode = projectCode,
            // 项目默认开启权限管理
            enable = true,
            relationId = gradeManagerId.toString()
        )
        traceEventDispatcher.dispatch(
            AuthResourceGroupCreateEvent(
                managerId = gradeManagerId,
                userId = userId,
                projectCode = projectCode,
                projectName = projectName,
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = projectCode,
                resourceName = projectName,
                iamResourceCode = projectCode
            )
        )
        return gradeManagerId
    }

    fun handleItsmUpdateCallback(
        userId: String,
        projectCode: String,
        projectName: String,
        sn: String,
        callBackId: String,
        currentStatus: String
    ) {
        logger.info("handle itsm update callback|$userId|$projectCode|$sn|$callBackId|$currentStatus")
        val resourceInfo = authResourceService.get(
            projectCode = projectCode,
            resourceType = AuthResourceType.PROJECT.value,
            resourceCode = projectCode
        )
        val callbackApplicationDTO = CallbackApplicationDTO
            .builder()
            .sn(sn)
            .currentStatus(currentStatus)
            .approveResult(true).build()
        iamV2ManagerService.handleCallbackApplication(callBackId, callbackApplicationDTO).roleId
        authResourceService.update(
            projectCode = projectCode,
            resourceType = AuthResourceType.PROJECT.value,
            resourceCode = projectCode,
            resourceName = projectName
        )
        traceEventDispatcher.dispatch(
            AuthResourceGroupModifyEvent(
                managerId = resourceInfo.relationId.toInt(),
                projectCode = projectCode,
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = projectCode,
                resourceName = projectName
            )
        )
    }

    @Suppress("LongParameterList")
    private fun buildItsmContentDTO(
        projectName: String,
        projectId: String,
        desc: String,
        organization: String,
        authSecrecy: Int,
        subjectScopes: List<SubjectScopeInfo>
    ): ItsmContentDTO {
        val itsmColumns = listOf(
            ItsmColumn.builder().key("projectName")
                .name(I18nUtil.getCodeLanMessage(BK_PROJECT_NAME)).type("text").build(),
            ItsmColumn.builder().key("projectId").name(I18nUtil.getCodeLanMessage(BK_PROJECT_ID)).type("text").build(),
            ItsmColumn.builder().key("desc").name(I18nUtil.getCodeLanMessage(BK_PROJECT_DESC)).type("text").build(),
            ItsmColumn.builder().key("organization")
                .name(I18nUtil.getCodeLanMessage(BK_ORGANIZATION)).type("text").build(),
            ItsmColumn.builder().key("authSecrecy")
                .name(I18nUtil.getCodeLanMessage(BK_AUTH_SECRECY)).type("text").build(),
            ItsmColumn.builder().key("subjectScopes")
                .name(I18nUtil.getCodeLanMessage(BK_SUBJECT_SCOPES)).type("text").build()
        )
        val itsmAttrs = ItsmAttrs.builder().column(itsmColumns).build()
        val itsmScheme = ItsmScheme.builder().attrs(itsmAttrs).type("table").build()
        val scheme = HashMap<String, ItsmScheme>()
        scheme["content_table"] = itsmScheme
        val value = HashMap<String, ItsmStyle>()
        value["projectName"] = ItsmStyle.builder().value(projectName).build()
        value["projectId"] = ItsmStyle.builder().value(projectId).build()
        value["desc"] = ItsmStyle.builder().value(desc).build()
        value["organization"] = ItsmStyle.builder().value(organization).build()
        value["authSecrecy"] =
            ItsmStyle.builder().value(ProjectAuthSecrecyStatus.getStatus(authSecrecy)?.desc ?: "").build()
        value["subjectScopes"] = ItsmStyle.builder().value(subjectScopes.joinToString(",") { it.name }).build()
        val itsmValue = ItsmValue.builder()
            .scheme("content_table")
            .lable(
                I18nUtil.getCodeLanMessage(BK_CREATE_PROJECT_APPROVAL)
            )
            .value(listOf(value))
            .build()
        return ItsmContentDTO.builder().formData(Arrays.asList(itsmValue)).schemes(scheme).build()
    }
}
