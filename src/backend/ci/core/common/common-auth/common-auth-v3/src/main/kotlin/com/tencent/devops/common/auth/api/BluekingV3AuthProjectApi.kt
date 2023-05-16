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

package com.tencent.devops.common.auth.api

import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.bk.sdk.iam.constants.ExpressionOperationEnum
import com.tencent.bk.sdk.iam.dto.InstanceDTO
import com.tencent.bk.sdk.iam.dto.action.ActionDTO
import com.tencent.bk.sdk.iam.helper.AuthHelper
import com.tencent.bk.sdk.iam.service.PolicyService
import com.tencent.devops.common.auth.api.pojo.BKAuthProjectRolesResources
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.api.pojo.BkAuthGroupAndUserList
import com.tencent.devops.common.auth.api.pojo.BkAuthProjectInfoResources
import com.tencent.devops.common.auth.code.AuthServiceCode
import com.tencent.devops.common.auth.service.IamEsbService
import com.tencent.devops.common.auth.utils.ActionUtils
import com.tencent.devops.common.auth.utils.AuthUtils
import org.slf4j.LoggerFactory

@Suppress("TooManyFunctions")
class BluekingV3AuthProjectApi constructor(
    private val policyService: PolicyService,
    private val authHelper: AuthHelper,
    private val iamConfiguration: IamConfiguration,
    private val iamEsbService: IamEsbService
) : AuthProjectApi {

    override fun validateUserProjectPermission(
        user: String,
        serviceCode: AuthServiceCode,
        projectCode: String,
        permission: AuthPermission
    ): Boolean {
        // v3没有project_enable权限,启用/禁用只有管理员才有权限
        return if (permission == AuthPermission.MANAGE || permission == AuthPermission.ENABLE) {
            checkProjectManager(userId = user, serviceCode = serviceCode, projectCode = projectCode)
        } else {
            checkProjectUser(user = user, serviceCode = serviceCode, projectCode = projectCode)
        }
    }

    override fun getProjectUsers(serviceCode: AuthServiceCode, projectCode: String, group: BkAuthGroup?): List<String> {
        logger.info("v3 getProjectUsers serviceCode[$serviceCode] projectCode[$projectCode] group[$group]")
        return emptyList()
    }

    override fun isProjectUser(
        user: String,
        serviceCode: AuthServiceCode,
        projectCode: String,
        group: BkAuthGroup?
    ): Boolean {
        if (group != null && group == BkAuthGroup.MANAGER) {
            return checkProjectManager(user, serviceCode, projectCode)
        }
        return checkProjectUser(user, serviceCode, projectCode)
    }

    override fun checkProjectUser(user: String, serviceCode: AuthServiceCode, projectCode: String): Boolean {
        val actionType = ActionUtils.buildAction(AuthResourceType.PROJECT, AuthPermission.VIEW)
        val checkAction = checkAction(projectCode, actionType, user)
        logger.info("isProjectUser checkAction:$checkAction")
        return checkAction
    }

    override fun checkProjectManager(userId: String, serviceCode: AuthServiceCode, projectCode: String): Boolean {
        val actionType = ActionUtils.buildAction(AuthResourceType.PROJECT, AuthPermission.MANAGE)
        val checkAction = checkAction(projectCode, actionType, userId)
        logger.info("isProjectManager checkAction:$checkAction")
        return checkAction
    }

    override fun getProjectGroupAndUserList(
        serviceCode: AuthServiceCode,
        projectCode: String
    ): List<BkAuthGroupAndUserList> {
        logger.info("v3 getProjectGroupAndUserList serviceCode[$serviceCode] projectCode[$projectCode] ")
        return emptyList()
    }

    override fun getUserProjects(
        serviceCode: AuthServiceCode,
        userId: String,
        supplier: (() -> List<String>)?
    ): List<String> {
        logger.info("v3 getUserProjects user[$userId] serviceCode[$serviceCode] supplier[$supplier] ")
        val actionType = ActionUtils.buildAction(AuthResourceType.PROJECT, AuthPermission.VIEW)
        val actionDtos = mutableListOf<ActionDTO>()
        val actionDto = ActionDTO()
        actionDto.id = actionType
        actionDtos.add(actionDto)
        val actionPolicyDTOs = policyService.batchGetPolicyByActionList(userId, actionDtos, null) ?: return emptyList()
        logger.info("getUserProjects actionPolicyDTOs $actionPolicyDTOs")
        val actionPolicy = actionPolicyDTOs[0]
        val projectList = AuthUtils.getProjects(actionPolicy.condition)
        logger.info("getUserProjects user[$userId],projects$projectList")
        return projectList
    }

    override fun getUserProjectsByPermission(
        serviceCode: AuthServiceCode,
        userId: String,
        permission: AuthPermission,
        supplier: (() -> List<String>)?
    ): List<String> {
        return emptyList()
    }

    override fun getUserProjectsAvailable(
        serviceCode: AuthServiceCode,
        userId: String,
        supplier: (() -> List<String>)?
    ): Map<String, String> {
        logger.info("v3 getUserProjectsAvailable user[$userId] serviceCode[$serviceCode] supplier[$supplier] ")
        val actionType = ActionUtils.buildAction(AuthResourceType.PROJECT, AuthPermission.VIEW)
        val actionDtos = mutableListOf<ActionDTO>()
        val actionDto = ActionDTO()
        actionDto.id = actionType
        actionDtos.add(actionDto)

        val actionPolicyDTOs = policyService.batchGetPolicyByActionList(userId, actionDtos, null) ?: return emptyMap()
        val actionPolicy = actionPolicyDTOs[0]
        val content = actionPolicy.condition
        if (content.field != "project.id") {
            return emptyMap()
        }
        val projectList = mutableListOf<String>()
        when (content.operator) {
            ExpressionOperationEnum.ANY -> projectList.add("*")
            ExpressionOperationEnum.IN -> projectList.addAll(content.value.toString().splitToSequence(","))
            ExpressionOperationEnum.EQUAL -> projectList.add(content.value.toString())
        }
        logger.info("getUserProjects user[$userId],projects[$projectList]")

        // 此处为兼容接口实现，并没有projectName,统一都是projectCode
        val projectCode2Code = mutableMapOf<String, String>()
        projectList.forEach {
            projectCode2Code[it] = it
        }
        return projectCode2Code
    }

    override fun createProjectUser(
        user: String,
        serviceCode: AuthServiceCode,
        projectCode: String,
        role: String
    ): Boolean {
        return true
    }

    override fun getProjectRoles(
        serviceCode: AuthServiceCode,
        projectCode: String,
        projectId: String
    ): List<BKAuthProjectRolesResources> {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun getProjectInfo(serviceCode: AuthServiceCode, projectId: String): BkAuthProjectInfoResources? {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    private fun checkAction(projectCode: String, actionType: String, userId: String): Boolean {
        val instance = InstanceDTO()
        instance.id = projectCode
        instance.system = iamConfiguration.systemId
        instance.type = AuthResourceType.PROJECT.value
        logger.info("v3 isProjectUser actionType[$actionType] instance[$instance]")
        return authHelper.isAllowed(userId, actionType, instance)
    }

    companion object {
        val logger = LoggerFactory.getLogger(BluekingV3AuthProjectApi::class.java)
    }
}
