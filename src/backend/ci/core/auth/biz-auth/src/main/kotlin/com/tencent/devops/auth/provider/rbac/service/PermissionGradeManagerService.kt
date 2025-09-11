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

package com.tencent.devops.auth.provider.rbac.service

import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.bk.sdk.iam.dto.CallbackApplicationDTO
import com.tencent.bk.sdk.iam.dto.GradeManagerApplicationCreateDTO
import com.tencent.bk.sdk.iam.dto.GradeManagerApplicationUpdateDTO
import com.tencent.bk.sdk.iam.dto.manager.AuthorizationScopes
import com.tencent.bk.sdk.iam.dto.manager.ManagerScopes
import com.tencent.bk.sdk.iam.dto.manager.dto.CreateManagerDTO
import com.tencent.bk.sdk.iam.dto.manager.dto.UpdateManagerDTO
import com.tencent.bk.sdk.iam.service.v2.V2ManagerService
import com.tencent.devops.auth.constant.AuthI18nConstants.BK_CREATE_BKCI_PROJECT_APPLICATION
import com.tencent.devops.auth.constant.AuthI18nConstants.BK_REVISE_BKCI_PROJECT_APPLICATION
import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.dao.AuthItsmCallbackDao
import com.tencent.devops.auth.dao.AuthMonitorSpaceDao
import com.tencent.devops.auth.dao.AuthResourceGroupConfigDao
import com.tencent.devops.auth.pojo.ItsmCancelApplicationInfo
import com.tencent.devops.auth.provider.rbac.pojo.event.AuthResourceGroupCreateEvent
import com.tencent.devops.auth.provider.rbac.pojo.event.AuthResourceGroupModifyEvent
import com.tencent.devops.auth.service.AuthAuthorizationScopesService
import com.tencent.devops.auth.service.DeptService
import com.tencent.devops.auth.service.iam.PermissionResourceGroupService
import com.tencent.devops.auth.service.iam.PermissionResourceGroupSyncService
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.api.pojo.DefaultGroupType
import com.tencent.devops.common.auth.api.pojo.SubjectScopeInfo
import com.tencent.devops.common.auth.callback.AuthConstants.ALL_MEMBERS
import com.tencent.devops.common.auth.callback.AuthConstants.ALL_MEMBERS_NAME
import com.tencent.devops.common.auth.enums.GroupType
import com.tencent.devops.common.auth.enums.SubjectScopeType
import com.tencent.devops.common.auth.utils.IamGroupUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.trace.TraceEventDispatcher
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.project.api.service.ServiceProjectApprovalResource
import com.tencent.devops.project.pojo.ProjectApprovalInfo
import com.tencent.devops.project.pojo.enums.ProjectApproveStatus
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value

@Suppress("LongParameterList", "TooManyFunctions")
class PermissionGradeManagerService @Autowired constructor(
    private val client: Client,
    private val iamV2ManagerService: V2ManagerService,
    private val iamConfiguration: IamConfiguration,
    private val authMonitorSpaceDao: AuthMonitorSpaceDao,
    private val authItsmCallbackDao: AuthItsmCallbackDao,
    private val dslContext: DSLContext,
    private val authResourceService: AuthResourceService,
    private val authResourceGroupConfigDao: AuthResourceGroupConfigDao,
    private val traceEventDispatcher: TraceEventDispatcher,
    private val itsmService: ItsmService,
    private val authAuthorizationScopesService: AuthAuthorizationScopesService,
    private val permissionResourceGroupService: PermissionResourceGroupService,
    private val resourceGroupSyncService: PermissionResourceGroupSyncService,
    private val deptService: DeptService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(PermissionGradeManagerService::class.java)
        private const val DEPARTMENT = "department"
        private const val CANCEL_ITSM_APPLICATION_ACTION = "WITHDRAW"
        private const val REVOKE_ITSM_APPLICATION_ACTION = "REVOKED"
        private const val FINISH_ITSM_APPLICATION_ACTION = "FINISHED"
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
        val manageGroupConfig = authResourceGroupConfigDao.getByGroupCode(
            dslContext = dslContext,
            resourceType = resourceType,
            groupCode = DefaultGroupType.MANAGER.value
        ) ?: throw ErrorCodeException(
            errorCode = AuthMessageCode.ERROR_AUTH_RESOURCE_GROUP_CONFIG_NOT_EXIST,
            params = arrayOf(DefaultGroupType.MANAGER.value),
            defaultMessage = "${resourceType}_${DefaultGroupType.MANAGER.value} group config  not exist"
        )
        val description = manageGroupConfig.description
        var authorizationScopes = authAuthorizationScopesService.generateBkciAuthorizationScopes(
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
            logger.info("create grade manager|$name|$userId")
            // 若为不需要审批的项目，直接注册监控权限
            val monitorAuthorizationScopes = authAuthorizationScopesService.generateMonitorAuthorizationScopes(
                projectName = projectName,
                projectCode = projectCode,
                groupCode = BkAuthGroup.GRADE_ADMIN.value,
                userId = userId
            )
            authorizationScopes = authorizationScopes.plus(monitorAuthorizationScopes)

            logger.info("PermissionGradeManagerService|createGradeManager|$authorizationScopes")
            val createManagerDTO = CreateManagerDTO.builder()
                .system(iamConfiguration.systemId)
                .name(name)
                .description(description)
                .members(listOf(userId))
                .authorization_scopes(authorizationScopes)
                .subject_scopes(subjectScopes)
                .sync_perm(true)
                .syncSubjectTemplate(true)
                .groupName(manageGroupConfig.groupName)
                .build()
            val gradeManagerId = iamV2ManagerService.createManagerV2(createManagerDTO)
            logger.info("create iam grade manager success|$name|$projectCode|$userId|$gradeManagerId")
            gradeManagerId
        } else {
            val callbackId = UUIDUtil.generate()
            val itsmContentDTO = itsmService.buildGradeManagerItsmContentDTO(
                projectName = projectName,
                projectId = projectCode,
                desc = projectApprovalInfo.description ?: "",
                organization = getOrganizationStr(projectApprovalInfo),
                authSecrecy = projectApprovalInfo.authSecrecy,
                subjectScopes = projectApprovalInfo.subjectScopes ?: listOf(
                    SubjectScopeInfo(
                        id = ALL_MEMBERS,
                        type = ALL_MEMBERS,
                        name = ALL_MEMBERS_NAME
                    )
                ),
                productName = projectApprovalInfo.productName!!
            )
            val gradeManagerApplicationCreateDTO = GradeManagerApplicationCreateDTO
                .builder()
                .name(name)
                .description(description)
                .members(arrayListOf(userId))
                .authorizationScopes(authorizationScopes)
                .subjectScopes(subjectScopes)
                .syncPerm(true)
                .syncSubjectTemplate(true)
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
                        params = arrayOf(userId, projectName)
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
        projectName: String,
        /*该字段主要用于当创建项目审批回调时，需要修改分级管理员并注册监控权限资源，此时不走审批流程*/
        registerMonitorPermission: Boolean = false
    ): Boolean {
        val projectApprovalInfo = client.get(ServiceProjectApprovalResource::class).get(projectId = projectCode).data
            ?: throw ErrorCodeException(
                errorCode = AuthMessageCode.RELATED_RESOURCE_EMPTY,
                params = arrayOf(projectCode),
                defaultMessage = "the resource not exists, projectCode:$projectCode"
            )
        logger.info("modify grade manager:$projectApprovalInfo")
        val name = IamGroupUtils.buildGradeManagerName(
            projectName = projectName
        )
        val groupConfig = authResourceGroupConfigDao.getByGroupCode(
            dslContext = dslContext,
            resourceType = AuthResourceType.PROJECT.value,
            groupCode = DefaultGroupType.MANAGER.value
        ) ?: throw ErrorCodeException(
            errorCode = AuthMessageCode.ERROR_AUTH_RESOURCE_GROUP_CONFIG_NOT_EXIST,
            params = arrayOf(DefaultGroupType.MANAGER.value),
            defaultMessage = "group config ${DefaultGroupType.MANAGER.value} not exist"
        )
        val authorizationScopes = generateAuthorizationScopes(
            projectCode = projectCode,
            projectName = projectName,
            creator = projectApprovalInfo.creator!!,
            bkciManagerGroupConfig = groupConfig.authorizationScopes,
            registerMonitorPermission = registerMonitorPermission
        )
        logger.info("PermissionGradeManagerService|modifyGradeManager|$authorizationScopes")
        val subjectScopes = projectApprovalInfo.subjectScopes?.map {
            when (it.type) {
                SubjectScopeType.DEPARTMENT.value -> ManagerScopes(DEPARTMENT, it.id)
                SubjectScopeType.USER.value -> ManagerScopes(it.type, it.username)
                else -> ManagerScopes(it.type, it.id)
            }
        } ?: listOf(ManagerScopes(ALL_MEMBERS, ALL_MEMBERS))

        val gradeManagerDetail = iamV2ManagerService.getGradeManagerDetail(gradeManagerId)
        val finalMembers = gradeManagerDetail.members.filterNot {
            deptService.isUserDeparted(it)
        }
        val description = gradeManagerDetail.description
        // 创建项目审批通过后，会调用更新分级管理员接口去修改项目的授权范围，此时不走审批流程
        return if (projectApprovalInfo.approvalStatus == ProjectApproveStatus.APPROVED.status ||
            registerMonitorPermission) {
            val updateManagerDTO = UpdateManagerDTO.builder()
                .name(name)
                .members(finalMembers)
                .description(description)
                .authorizationScopes(authorizationScopes)
                .subjectScopes(subjectScopes)
                .syncPerm(true)
                .groupName(groupConfig.groupName)
                .build()
            logger.info("update grade manager|$name|$finalMembers")
            iamV2ManagerService.updateManagerV2(gradeManagerId, updateManagerDTO)
            true
        } else {
            val callbackId = UUIDUtil.generate()
            val itsmContentDTO = itsmService.buildGradeManagerItsmContentDTO(
                projectName = projectName,
                projectId = projectCode,
                desc = projectApprovalInfo.description ?: "",
                organization = getOrganizationStr(projectApprovalInfo),
                authSecrecy = projectApprovalInfo.authSecrecy,
                subjectScopes = projectApprovalInfo.subjectScopes ?: listOf(
                    SubjectScopeInfo(
                        id = ALL_MEMBERS,
                        type = ALL_MEMBERS,
                        name = ALL_MEMBERS_NAME
                    )
                ),
                productName = projectApprovalInfo.productName!!,
                isCreateProject = false
            )

            val gradeManagerApplicationUpdateDTO = GradeManagerApplicationUpdateDTO.builder()
                .name(name)
                .description(description)
                .authorizationScopes(authorizationScopes)
                .subjectScopes(subjectScopes)
                .syncPerm(true)
                .groupName(groupConfig.groupName)
                .applicant(projectApprovalInfo.updator)
                .members(finalMembers)
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
                        params = arrayOf(projectApprovalInfo.updator!!, projectName)
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

    private fun getOrganizationStr(projectApprovalInfo: ProjectApprovalInfo): String {
        return with(projectApprovalInfo) {
            listOf(
                bgName, businessLineName, deptName, centerName
            ).filter { !it.isNullOrBlank() }.joinToString("-")
        }
    }

    private fun generateAuthorizationScopes(
        projectCode: String,
        projectName: String,
        creator: String,
        bkciManagerGroupConfig: String,
        registerMonitorPermission: Boolean
    ): List<AuthorizationScopes> {
        val bkciAuthorizationScopes = authAuthorizationScopesService.generateBkciAuthorizationScopes(
            authorizationScopesStr = bkciManagerGroupConfig,
            projectCode = projectCode,
            projectName = projectName,
            iamResourceCode = projectCode,
            resourceName = projectName
        )
        val monitorSpaceInfo = authMonitorSpaceDao.get(
            dslContext = dslContext,
            projectCode = projectCode
        )
        // 对于正常修改项目流程时，仅对项目的分级管理员已具有监控的授权范围，才需要加上监控授权范围；否则，只需要蓝盾的授权范围。
        return if (monitorSpaceInfo != null || registerMonitorPermission) {
            val monitorAuthorizationScopes = authAuthorizationScopesService.generateMonitorAuthorizationScopes(
                projectName = projectName,
                projectCode = projectCode,
                groupCode = BkAuthGroup.GRADE_ADMIN.value,
                userId = creator
            )
            bkciAuthorizationScopes.plus(monitorAuthorizationScopes)
        } else {
            bkciAuthorizationScopes
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
        permissionResourceGroupService.syncManagerGroup(
            projectCode = projectCode,
            managerId = gradeManagerId,
            resourceType = AuthResourceType.PROJECT.value,
            resourceCode = projectCode,
            resourceName = projectName,
            iamResourceCode = projectCode
        )
        val defaultGroupConfigs = authResourceGroupConfigDao.get(
            dslContext = dslContext,
            resourceType = AuthResourceType.PROJECT.value,
            createMode = false,
            groupType = GroupType.DEFAULT.value
        )
        defaultGroupConfigs.forEach { groupConfig ->
            permissionResourceGroupService.createGroupAndPermissionsByGroupCode(
                projectId = projectCode,
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = projectCode,
                groupCode = groupConfig.groupCode
            )
        }
        // 分级管理员创建后,需要同步下组、成员和分级管理员
        resourceGroupSyncService.syncGroupAndMember(projectCode = projectCode)
    }

    fun modifyGradeDefaultGroup(
        gradeManagerId: Int,
        projectCode: String,
        projectName: String
    ) {
        permissionResourceGroupService.modifyManagerDefaultGroup(
            projectCode = projectCode,
            resourceType = AuthResourceType.PROJECT.value,
            resourceCode = projectCode,
            resourceName = projectName
        )
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
        // 若itsm还未结束，需要发起撤销
        if (!isItsmTicketFinished(callbackRecord.sn)) {
            itsmService.cancelItsmApplication(
                ItsmCancelApplicationInfo(
                    sn = callbackRecord.sn,
                    operator = userId,
                    actionType = CANCEL_ITSM_APPLICATION_ACTION
                )
            )
        }
        logger.info("cancel create gradle manager|${callbackRecord.callbackId}|${callbackRecord.sn}")
        iamV2ManagerService.cancelCallbackApplication(callbackRecord.callbackId)
        authItsmCallbackDao.updateCallbackBySn(
            dslContext = dslContext,
            sn = callbackRecord.sn,
            approver = userId,
            approveResult = false
        )
        return true
    }

    private fun isItsmTicketFinished(sn: String): Boolean {
        val itsmTicketStatus = itsmService.getItsmTicketStatus(sn)
        return itsmTicketStatus == REVOKE_ITSM_APPLICATION_ACTION ||
            itsmTicketStatus == FINISH_ITSM_APPLICATION_ACTION
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

        // 审批通过后，需要修改分级管理员，注册监控中心权限资源
        modifyGradeManager(
            gradeManagerId = gradeManagerId.toString(),
            projectCode = projectCode,
            projectName = projectName,
            registerMonitorPermission = true
        )

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
}
