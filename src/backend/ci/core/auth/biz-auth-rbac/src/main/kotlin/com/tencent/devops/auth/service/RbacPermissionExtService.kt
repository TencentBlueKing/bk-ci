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

package com.tencent.devops.auth.service

import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.bk.sdk.iam.dto.manager.Action
import com.tencent.bk.sdk.iam.dto.manager.AuthorizationScopes
import com.tencent.bk.sdk.iam.dto.manager.ManagerPath
import com.tencent.bk.sdk.iam.dto.manager.ManagerResources
import com.tencent.bk.sdk.iam.dto.manager.dto.CreateSubsetManagerDTO
import com.tencent.bk.sdk.iam.service.v2.V2ManagerService
import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.service.iam.PermissionExtService
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.enums.SubsetGroupType
import com.tencent.devops.common.auth.utils.RbacAuthUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.project.constant.ProjectMessageCode
import com.tencent.devops.project.pojo.ProjectVO
import org.slf4j.LoggerFactory

class RbacPermissionExtService(
    private val client: Client,
    private val iamConfiguration: IamConfiguration,
    private val v2ManagerService: V2ManagerService,
    private val strategyService: StrategyService,
    private val authResourceService: AuthResourceService
) : PermissionExtService {

    companion object {
        private val logger = LoggerFactory.getLogger(RbacPermissionExtService::class.java)
    }

    override fun resourceCreateRelation(
        userId: String,
        projectCode: String,
        resourceType: String,
        resourceCode: String,
        resourceName: String
    ): Boolean {
        logger.info("resourceCreateRelation $userId $projectCode $resourceCode $resourceName $resourceType")
        val projectInfo =
            client.get(ServiceProjectResource::class).get(englishName = projectCode).data ?: throw ErrorCodeException(
                errorCode = ProjectMessageCode.PROJECT_NOT_EXIST,
                params = arrayOf(projectCode),
                defaultMessage = "项目[$projectCode]不存在"
            )
        val gradeManagerId = projectInfo.relationId ?: throw ErrorCodeException(
            errorCode = AuthMessageCode.PROJECT_NOT_FIND_RELATION,
            params = arrayOf(projectCode),
            defaultMessage = "项目${projectCode}没有关联系统分级管理员"
        )
       /* val createSubSetManagerDTO = buildCreateSubSetManagerDTO(
            userId = userId,
            projectCode = projectCode,
            resourceType = resourceType,
            resourceCode = resourceCode,
            resourceName = resourceName,
            projectInfo = projectInfo
        )
        val subsetGradeManagerId = v2ManagerService.createSubsetManager(
            gradeManagerId,
            createSubSetManagerDTO
        )*/
        authResourceService.create(
            userId = userId,
            projectCode = projectCode,
            resourceType = resourceType,
            resourceCode = resourceCode,
            resourceName = resourceName,
            relationId = "0"
        )
        return true
    }

    @SuppressWarnings("LongParameterList")
    private fun buildCreateSubSetManagerDTO(
        userId: String,
        projectCode: String,
        resourceType: String,
        resourceCode: String,
        resourceName: String,
        projectInfo: ProjectVO,
    ): CreateSubsetManagerDTO {
        val strategyName = RbacAuthUtils.getSubsetStrategyName(resourceType, SubsetGroupType.OWNER.value)
        val strategyEntity = strategyService.getStrategyByName(strategyName = strategyName) ?: throw ErrorCodeException(
            errorCode = AuthMessageCode.STRATEGT_NOT_EXIST,
            params = arrayOf(strategyName),
            defaultMessage = "策略${strategyName}不存在"
        )
        val actions = mutableListOf<Action>()
        val resources = mutableListOf<ManagerResources>()
        val projectPath = ManagerPath(
            iamConfiguration.systemId,
            AuthResourceType.PROJECT.value,
            projectCode,
            projectInfo.projectName
        )
        val resourcePath = ManagerPath(
            iamConfiguration.systemId,
            resourceType,
            resourceCode,
            resourceName
        )
        val resource = ManagerResources.builder()
            .system(iamConfiguration.systemId)
            .type(resourceType)
            .paths(listOf(listOf(projectPath, resourcePath)))
            .build()
        strategyEntity.strategy[resourceType]?.forEach {
            val action = Action(
                RbacAuthUtils.buildAction(
                    authPermission = AuthPermission.get(it),
                    authResourceType = RbacAuthUtils.getResourceTypeByStr(resourceType)
                )
            )
            actions.add(action)
            resources.add(resource)
        }
        val authorizationScopes =
            AuthorizationScopes.builder().system(iamConfiguration.systemId).actions(actions).resources(resources)
                .build()
        return CreateSubsetManagerDTO.builder()
            .name(resourceName)
            .members(listOf(userId))
            .authorizationScopes(listOf(authorizationScopes))
            .build()
    }
}
