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
 *
 */

package com.tencent.devops.auth.service.migrate

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.bk.sdk.iam.dto.manager.Action
import com.tencent.bk.sdk.iam.dto.manager.AuthorizationScopes
import com.tencent.bk.sdk.iam.dto.manager.ManagerMember
import com.tencent.bk.sdk.iam.dto.manager.ManagerPath
import com.tencent.bk.sdk.iam.dto.manager.ManagerResources
import com.tencent.bk.sdk.iam.dto.manager.ManagerRoleGroup
import com.tencent.bk.sdk.iam.dto.manager.RoleGroupMemberInfo
import com.tencent.bk.sdk.iam.dto.manager.dto.ManagerMemberGroupDTO
import com.tencent.bk.sdk.iam.dto.manager.dto.ManagerRoleGroupDTO
import com.tencent.bk.sdk.iam.dto.response.ResponseDTO
import com.tencent.bk.sdk.iam.service.v2.impl.V2ManagerServiceImpl
import com.tencent.devops.auth.common.Constants
import com.tencent.devops.auth.constant.AuthMessageCode.ERROR_AUTH_GROUP_NOT_EXIST
import com.tencent.devops.auth.dao.AuthResourceGroupConfigDao
import com.tencent.devops.auth.dao.AuthResourceGroupDao
import com.tencent.devops.auth.pojo.migrate.MigrateTaskDataResp
import com.tencent.devops.auth.pojo.migrate.MigrateTaskDataResult
import com.tencent.devops.auth.pojo.migrate.MigrateTaskResp
import com.tencent.devops.auth.service.AuthResourceCodeConverter
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.DefaultGroupType
import com.tencent.devops.common.auth.utils.RbacAuthUtils
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value

/**
 * v3权限策略迁移到rbac
 *
 * 权限策略包含：
 * 1. 蓝盾创建的默认用户组权限
 *     1.1. 包含all_action，直接加入管理员
 *     1.2. project_views_manager忽略权限
 * 2. 用户在权限中心创建的用户组权限
 * 3. 用户自定义权限
 */
@Suppress("LongParameterList")
class MigrateV3PolicyService constructor(
    private val v2ManagerService: V2ManagerServiceImpl,
    private val iamConfiguration: IamConfiguration,
    private val dslContext: DSLContext,
    private val authResourceGroupDao: AuthResourceGroupDao,
    private val authResourceGroupConfigDao: AuthResourceGroupConfigDao,
    private val migrateResourceCodeConverter: MigrateResourceCodeConverter,
    private val authResourceCodeConverter: AuthResourceCodeConverter
) {

    companion object {
        // 启动iam迁移任务
        private const val IAM_MIGRATE_TASK = "api/v2/open/migration/bkci/task"

        // 获取iam迁移数据
        private const val IAM_GET_MIGRATE_DATA = "api/v2/open/migration/bkci/data/"
        private const val SUCCESSFUL_IAM_MIGRATE_TASK_SUCCESS = "SUCCESS"
        // 轮询获取iam迁移状态睡眠时间
        private const val SLEEP_LOOP_IAM_GET_MIGRATE_TASK = 30000L
        // ci通过iam接口创建的用户组
        private const val GROUP_API_POLICY = "group_api_policy"
        // 用户在iam界面创建的用户组
        private const val GROUP_WEB_POLICY = "group_web_policy"
        // 用户创建用户组group_code
        private const val CUSTOM_GROUP_CODE = "custom"
        private val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        private val logger = LoggerFactory.getLogger(MigrateV3PolicyService::class.java)
    }


    // iam迁移的token
    @Value("\${migrate.iamToken:#{null}}")
    private val migrateIamToken: String = ""

    @Value("\${auth.url:}")
    private val iamBaseUrl = ""

    /**
     * 启动迁移任务
     *
     * @param v3GradeManagerId v3分级管理员ID
     */
    fun startMigrateTask(v3GradeManagerId: String) {
        val data = JsonUtil.toJson(listOf(v3GradeManagerId)).toRequestBody(mediaType)
        val request = Request.Builder()
            .url("$iamBaseUrl/$IAM_MIGRATE_TASK?token=$migrateIamToken")
            .post(data)
            .build()
        getBody(operation = "failed to start migrate task", request = request)
    }

    fun migrateGroupPolicy(projectCode: String, projectName: String, gradeManagerId: Int) {
        loopTaskStatus()
        val managerGroupId = authResourceGroupDao.get(
            dslContext = dslContext,
            projectCode = projectCode,
            resourceType = AuthResourceType.PROJECT.value,
            resourceCode = projectCode,
            groupCode = DefaultGroupType.MANAGER.value
        )?.relationId?.toInt() ?: throw ErrorCodeException(
            errorCode = ERROR_AUTH_GROUP_NOT_EXIST,
            params = arrayOf(DefaultGroupType.MANAGER.value)
        )
        // 蓝盾创建的默认用户组权限
        val groupApiPolicyResult = getGroupPolicyData(projectCode = projectCode, type = GROUP_API_POLICY)
        migrateGroup(
            projectCode = projectCode,
            projectName = projectName,
            gradeManagerId = gradeManagerId,
            managerGroupId = managerGroupId,
            results = groupApiPolicyResult
        )
        // 用户在权限中心创建的用户组权限
        val groupWebPolicyResult = getGroupPolicyData(projectCode = projectCode, type = GROUP_WEB_POLICY)
        migrateGroup(
            projectCode = projectCode,
            projectName = projectName,
            gradeManagerId = gradeManagerId,
            managerGroupId = managerGroupId,
            results = groupWebPolicyResult
        )
    }

    fun migrateUserCustomPolicy(projectCode: String, gradeManagerId: Int) {

    }

    fun comparePolicy(projectCode: String): Boolean {
        return true
    }

    /**
     * 轮询获取迁移任务状态,如果状态没有成功,等待30s再重试
     */
    private fun loopTaskStatus() {
        val request = Request.Builder()
            .url("$iamBaseUrl/$IAM_MIGRATE_TASK?token=$migrateIamToken")
            .get()
            .build()
        val migrateTaskResp = JsonUtil.to(
            getBody("get iam migrate task status", request),
            object : TypeReference<ResponseDTO<MigrateTaskResp>>() {}
        ).data
        if (migrateTaskResp.status != SUCCESSFUL_IAM_MIGRATE_TASK_SUCCESS) {
            Thread.sleep(SLEEP_LOOP_IAM_GET_MIGRATE_TASK)
            loopTaskStatus()
        }
    }

    /**
     * 获取ci调用iam接口创建的用户组
     */
    private fun getGroupPolicyData(projectCode: String, type: String): List<MigrateTaskDataResult> {
        val request = Request.Builder()
            .url(
                "$iamBaseUrl/$IAM_GET_MIGRATE_DATA?" +
                    "token=$migrateIamToken&project_id=$projectCode&type=$type"
            )
            .get()
            .build()
        return JsonUtil.to(
            getBody("get iam migrate data", request),
            object : TypeReference<ResponseDTO<MigrateTaskDataResp>>() {}
        ).data.results
    }

    private fun migrateGroup(
        projectCode: String,
        projectName: String,
        gradeManagerId: Int,
        managerGroupId: Int,
        results: List<MigrateTaskDataResult>
    ) {
        results.forEach result@{ result ->
            logger.info("migrate group|${result.projectId}|${result.subject.name}|${result.subject.id}")
            val rbacAuthorizationScopeList = buildRbacAuthorizationScopeList(
                projectCode = projectCode,
                managerGroupId = managerGroupId,
                result = result
            )

            if (rbacAuthorizationScopeList.isEmpty()) {
                return@result
            }

            // 创建用户组
            val groupName = result.subject.name.substringAfter("${result.projectId}-")
            val groupId = authResourceGroupDao.getByGroupName(
                dslContext = dslContext,
                projectCode = projectCode,
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = projectCode,
                groupName = groupName
            )?.relationId?.toInt() ?: run {
                createRbacGroup(
                    groupName = groupName,
                    gradeManagerId = gradeManagerId,
                    projectCode = projectCode,
                    projectName = projectName
                )
            }

            // 用户组授权
            rbacAuthorizationScopeList.forEach { authorizationScope ->
                v2ManagerService.grantRoleGroupV2(groupId, authorizationScope)
            }
            // 往用户组添加成员
            batchAddGroupMember(groupId = groupId, members = result.members)
        }
    }

    private fun createRbacGroup(
        groupName: String,
        gradeManagerId: Int,
        projectCode: String,
        projectName: String
    ): Int {
        val managerRoleGroup = ManagerRoleGroup()
        managerRoleGroup.name = groupName
        val managerRoleGroupDTO = ManagerRoleGroupDTO.builder().groups(listOf(managerRoleGroup)).build()
        val groupId = v2ManagerService.batchCreateRoleGroupV2(gradeManagerId, managerRoleGroupDTO)
        val groupConfig = authResourceGroupConfigDao.getByName(
            dslContext = dslContext,
            resourceType = AuthResourceType.PROJECT.value,
            groupName = groupName
        )
        authResourceGroupDao.create(
            dslContext = dslContext,
            projectCode = projectCode,
            resourceType = AuthResourceType.PROJECT.value,
            resourceCode = projectCode,
            resourceName = projectName,
            iamResourceCode = projectCode,
            groupCode = groupConfig?.groupCode ?: CUSTOM_GROUP_CODE,
            groupName = groupName,
            defaultGroup = groupConfig != null,
            relationId = groupId.toString()
        )
        return groupId
    }

    private fun buildRbacAuthorizationScopeList(
        projectCode: String,
        managerGroupId: Int,
        result: MigrateTaskDataResult
    ): List<AuthorizationScopes> {
        return result.permissions.map permission@{ permission ->
            val rbacActions = buildRbacActions(
                managerGroupId = managerGroupId,
                permission = permission,
                members = result.members
            )
            // 操作为空,可能是有all_action,已经添加到管理员组
            if (rbacActions.isEmpty()) {
                return emptyList()
            }
            val rbacResources = buildRbacManagerResources(projectCode = projectCode, permission = permission)
            AuthorizationScopes.builder()
                .system(iamConfiguration.systemId)
                .actions(rbacActions)
                .resources(rbacResources)
                .build()
        }
    }

    private fun buildRbacActions(
        managerGroupId: Int,
        permission: AuthorizationScopes,
        members: List<RoleGroupMemberInfo>
    ): List<Action> {
        val rbacActions = mutableListOf<Action>()
        permission.actions.forEach action@{ action ->
            when (action.id) {
                // 如果包含all_action,则直接添加到管理员组
                Constants.ALL_ACTION -> {
                    logger.info("match all_action,member add to manager group $managerGroupId")
                    batchAddGroupMember(groupId = managerGroupId, members = members)
                    return emptyList()
                }
                "project_views_manager" -> {
                    logger.info("skip project_views_manager action")
                }
                else -> {
                    rbacActions.add(action)
                }
            }
        }
        // 所有用户组都添加project_visit权限
        rbacActions.add(Action(RbacAuthUtils.buildAction(AuthPermission.VISIT, AuthResourceType.PROJECT)))
        return rbacActions
    }

    private fun buildRbacManagerResources(
        projectCode: String,
        permission: AuthorizationScopes
    ): List<ManagerResources> {
        return permission.resources.map { resource ->
            val rbacManagerPaths = resource.paths.map { managerPaths ->
                managerPaths.map { managerPath ->
                    // 先将v3资源code转换成rbac资源code
                    val resourceCode = migrateResourceCodeConverter.v3ToRbacResourceCode(
                        resourceType = managerPath.type,
                        resourceCode = managerPath.id
                    )
                    // 获取rbac资源code对应的iam资源code
                    val iamResourceCode = authResourceCodeConverter.code2IamCode(
                        projectCode = projectCode,
                        resourceType = managerPath.type,
                        resourceCode = resourceCode
                    )
                    val rbacManagerPath = ManagerPath()
                    rbacManagerPath.system = iamConfiguration.systemId
                    rbacManagerPath.id = iamResourceCode
                    rbacManagerPath.type = managerPath.type
                    rbacManagerPath
                }
            }
            ManagerResources.builder()
                .system(iamConfiguration.systemId)
                .type(resource.type)
                .paths(rbacManagerPaths)
                .build()
        }
    }

    private fun batchAddGroupMember(groupId: Int, members: List<RoleGroupMemberInfo>) {
        members.forEach member@{ member ->
            val managerMember = ManagerMember(member.type, member.id)
            val managerMemberGroupDTO = ManagerMemberGroupDTO.builder()
                .members(listOf(managerMember))
                .expiredAt(member.expiredAt)
                .build()
            v2ManagerService.createRoleGroupMemberV2(groupId, managerMemberGroupDTO)
        }
    }

    private fun getBody(operation: String, request: Request): String {
        OkhttpUtils.doHttp(request).use { response ->
            if (!response.isSuccessful) {
                throw RemoteServiceException(operation)
            }
            return response.body!!.string()
        }
    }
}
