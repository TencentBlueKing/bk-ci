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

package com.tencent.devops.auth.service.iam.impl

import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.bk.sdk.iam.dto.manager.Action
import com.tencent.bk.sdk.iam.dto.manager.AuthorizationScopes
import com.tencent.bk.sdk.iam.dto.manager.ManagerPath
import com.tencent.bk.sdk.iam.dto.manager.ManagerResources
import com.tencent.bk.sdk.iam.dto.manager.ManagerRoleGroup
import com.tencent.bk.sdk.iam.dto.manager.dto.ManagerRoleGroupDTO
import com.tencent.bk.sdk.iam.dto.manager.vo.ManagerRoleGroupVO
import com.tencent.bk.sdk.iam.service.ManagerService
import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.pojo.dto.ProjectRoleDTO
import com.tencent.devops.auth.service.iam.PermissionGradeService
import com.tencent.devops.auth.service.iam.PermissionRoleService
import com.tencent.devops.common.auth.utils.IamUtils
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.service.utils.MessageCodeUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import java.lang.RuntimeException

open class AbsPermissionRoleServiceImpl @Autowired constructor(
    open val iamManagerService: ManagerService,
    private val permissionGradeService: PermissionGradeService,
    private val iamConfiguration: IamConfiguration
) : PermissionRoleService {

    @Value("\${project.role.default:#{null}}")
    val defaultRole: String = ""

    override fun createPermissionRole(
        userId: String,
        projectId: Int,
        projectCode: String,
        groupInfo: ProjectRoleDTO
    ): Int {
        // 校验操作人是否有项目分级管理员权限
        permissionGradeService.checkGradeManagerUser(userId, projectId)

        // 校验用户组名称
        checkRoleName(groupInfo.name, groupInfo.defaultGroup!!)

        val groupName = IamUtils.buildIamGroup(projectCode, groupInfo.name)

        // 添加项目下用户组
        val managerRoleGroup = ManagerRoleGroup(groupName, groupInfo.description)
        val roleGroups = mutableListOf<ManagerRoleGroup>()
        roleGroups.add(managerRoleGroup)
        val groups = ManagerRoleGroupDTO.builder().groups(roleGroups).build()
        val roleId = iamManagerService.createManagerRoleGroup(projectId, groups)

        // 默认分组需要分配默认权限
        if (groupInfo.defaultGroup!!) {
            when (groupInfo.name) {
                BkAuthGroup.DEVELOPER.value -> addDevelopPermission(roleId, projectCode)
                BkAuthGroup.MAINTAINER.value -> addMaintainerPermission(roleId, projectCode)
                BkAuthGroup.TESTER.value -> addTestPermission(roleId, projectCode)
                BkAuthGroup.QC.value -> addQCPermission(roleId, projectCode)
                BkAuthGroup.PM.value -> addPMPermission(roleId, projectCode)
            }
        }
        return roleId
    }

    override fun renamePermissionRole(userId: String, projectId: Int, roleId: String, groupInfo: ManagerRoleGroup) {
        permissionGradeService.checkGradeManagerUser(userId, projectId)
        // 校验用户组名称
        checkRoleName(groupInfo.name, false)
        iamManagerService.updateManagerRoleGroup(projectId, groupInfo)
    }

    override fun getPermissionRole(projectId: Int): ManagerRoleGroupVO {
        return iamManagerService.getManagerRoleGroup(projectId)
    }

    private fun checkRoleName(name: String, defaultGroup: Boolean) {
        // 校验用户组名称
        if (defaultGroup) {
            // 若为默认分组,需校验提供用户组是否在默认分组内。
            if (!defaultRole.contains(name)) {
                // 不在默认分组内则直接报错
                throw RuntimeException(MessageCodeUtil.getCodeLanMessage(AuthMessageCode.DEFAULT_GROUP_ERROR))
            }
        } else {
            // 非默认分组,不能使用默认分组组名
            if (defaultRole.contains(name)) {
                throw RuntimeException(MessageCodeUtil.getCodeLanMessage(AuthMessageCode.UN_DEFAULT_GROUP_ERROR))
            }
        }
    }

    private fun addDevelopPermission(roleId: Int, projectCode: String) {
        val actions = mutableListOf<String>()
        actions.add(PIPELINEACTION)
        actions.add(CREDENTIALACTION)
        actions.add(CERTACTION)
        actions.add(REPERTORYACTION)
        actions.add(ENVIRONMENTACTION)
        actions.add(NODEACTION)
        actions.add(REPORTACTION)
        val authorizationScopes = buildCreateAuthorizationScopes(actions, projectCode)
        iamManagerService.createRolePermission(roleId, authorizationScopes)
    }

    private fun addTestPermission(roleId: Int, projectCode: String) {
        val actions = mutableListOf<String>()
        actions.add(PIPELINEACTION)
        actions.add(CREDENTIALACTION)
        actions.add(REPERTORYACTION)
        actions.add(ENVIRONMENTACTION)
        actions.add(NODEACTION)
        val authorizationScopes = buildCreateAuthorizationScopes(actions, projectCode)
        iamManagerService.createRolePermission(roleId, authorizationScopes)
    }

    private fun addPMPermission(roleId: Int, projectCode: String) {
        val actions = mutableListOf<String>()
        actions.add(CREDENTIALACTION)
        actions.add(REPERTORYACTION)
        val authorizationScopes = buildCreateAuthorizationScopes(actions, projectCode)
        iamManagerService.createRolePermission(roleId, authorizationScopes)
    }

    private fun addQCPermission(roleId: Int, projectCode: String) {
        val createActions = mutableListOf<String>()
        createActions.add(CREDENTIALACTION)
        createActions.add(REPERTORYACTION)
        createActions.add(RULECREATEACTION)
        createActions.add(GROUPCREATEACTION)
        val createAuthorizationScopes = buildCreateAuthorizationScopes(createActions, projectCode)
        iamManagerService.createRolePermission(roleId, createAuthorizationScopes)
        val ruleAction = RULEACTION.split(",")
        val ruleAuthorizationScopes = buildOtherAuthorizationScopes(ruleAction, projectCode)
        iamManagerService.createRolePermission(roleId, ruleAuthorizationScopes)
        val groupAction = GROUPACTION.split(",")
        val groupAuthorizationScopes = buildOtherAuthorizationScopes(groupAction, projectCode)
        iamManagerService.createRolePermission(roleId, groupAuthorizationScopes)
    }

    private fun addMaintainerPermission(roleId: Int, projectCode: String) {
        val actions = mutableListOf<String>()
        actions.add(PIPELINEACTION)
        actions.add(CREDENTIALACTION)
        actions.add(REPERTORYACTION)
        actions.add(ENVIRONMENTACTION)
        actions.add(NODEACTION)
        val authorizationScopes = buildCreateAuthorizationScopes(actions, projectCode)
        iamManagerService.createRolePermission(roleId, authorizationScopes)
    }

    private fun buildCreateAuthorizationScopes(actions: List<String>, projectCode: String): AuthorizationScopes {
        val managerResources = mutableListOf<ManagerResources>()
        val managerPath = mutableListOf<ManagerPath>()
        val projectPath = ManagerPath(
            iamConfiguration.systemId,
            AuthResourceType.PROJECT.value,
            projectCode,
            ""
        )
        managerPath.add(projectPath)
        val paths = mutableListOf<List<ManagerPath>>()
        paths.add(managerPath)
        managerResources.add(
            ManagerResources.builder()
                .system(iamConfiguration.systemId)
                .type(AuthResourceType.PROJECT.value)
                .paths(paths).build()
        )
        val action = mutableListOf<Action>()
        actions.forEach {
            action.add(Action(it))
        }
        return AuthorizationScopes.builder()
            .system(iamConfiguration.systemId)
            .actions(action)
            .resources(managerResources)
            .build()
    }

    private fun buildOtherAuthorizationScopes(actions: List<String>, projectCode: String): AuthorizationScopes? {
        val resourceTypes = mutableSetOf<String>()
        var type = ""
        actions.forEach {
            resourceTypes.add(it.substringBeforeLast("_"))
            type = it.substringBeforeLast("_")
        }

        if (resourceTypes.size > 1) {
            logger.warn("buildOtherAuthorizationScopes not same resourceType:$resourceTypes")
            return null
        }
        val managerResources = mutableListOf<ManagerResources>()
        val managerPath = mutableListOf<ManagerPath>()
        val projectPath = ManagerPath(
            iamConfiguration.systemId,
            AuthResourceType.PROJECT.value,
            projectCode,
            ""
        )
        val resourcePath = ManagerPath(
            iamConfiguration.systemId,
            AuthResourceType.valueOf(type).value,
            "*",
            ""
        )
        managerPath.add(projectPath)
        managerPath.add(resourcePath)
        val paths = mutableListOf<List<ManagerPath>>()
        paths.add(managerPath)
        managerResources.add(
            ManagerResources.builder()
                .system(iamConfiguration.systemId)
                .type(AuthResourceType.valueOf(type).value)
                .paths(paths).build()
        )
        val action = mutableListOf<Action>()
        actions.forEach {
            action.add(Action(it))
        }
        return AuthorizationScopes.builder()
            .system(iamConfiguration.systemId)
            .actions(action)
            .resources(managerResources)
            .build()
    }

    companion object {
        val logger = LoggerFactory.getLogger(AbsPermissionRoleMemberImpl::class.java)
        const val PIPELINEACTION = "pipeline_create"
        const val REPORTACTION = "list_repo"
        const val CREDENTIALACTION = "credential_create"
        const val CERTACTION = "cert_create"
        const val REPERTORYACTION = "repertory_create"
        const val ENVIRONMENTACTION = "environment_create"
        const val NODEACTION = "env_node_create"
        const val RULECREATEACTION = "rule_create"
        const val GROUPCREATEACTION = "group_create"
        const val RULEACTION = "rule_delete,rule_update,rule_use"
        const val GROUPACTION = "group_delete,group_update,group_use"
    }
}
