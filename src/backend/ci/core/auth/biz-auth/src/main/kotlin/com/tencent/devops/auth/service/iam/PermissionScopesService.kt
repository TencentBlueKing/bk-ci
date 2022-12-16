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

package com.tencent.devops.auth.service.iam

import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.bk.sdk.iam.dto.manager.Action
import com.tencent.bk.sdk.iam.dto.manager.AuthorizationScopes
import com.tencent.bk.sdk.iam.dto.manager.ManagerPath
import com.tencent.bk.sdk.iam.dto.manager.ManagerResources
import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.service.StrategyService
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.service.utils.MessageCodeUtil
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 可授权范围操作
 */
@Service
class PermissionScopesService(
    private val iamConfiguration: IamConfiguration,
    private val strategyService: StrategyService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(PermissionScopesService::class.java)
    }

    /**
     * 构建分级管理员或分级管理员用户组授权范围
     */
    fun buildGradeManagerAuthorizationScopes(
        strategyName: String,
        projectCode: String,
        projectName: String
    ): AuthorizationScopes {
        val (projectStrategyList, resourceStrategyMap) = getGroupStrategy(strategyName)
        val actions = mutableListOf<Action>()
        val resources = mutableListOf<ManagerResources>()
        // 所有的创建都直接挂在项目下
        projectStrategyList.forEach { createAction ->
            actions.add(Action(createAction))
            val managerPath = mutableListOf<ManagerPath>()
            val projectPath = ManagerPath(
                iamConfiguration.systemId,
                AuthResourceType.PROJECT.value,
                projectCode,
                projectName
            )
            managerPath.add(projectPath)
            val paths = mutableListOf<List<ManagerPath>>()
            paths.add(managerPath)
            resources.add(
                ManagerResources.builder()
                    .system(iamConfiguration.systemId)
                    .type(AuthResourceType.PROJECT.value)
                    .paths(paths).build()
            )
        }
        resourceStrategyMap.forEach { (resourceType, resourceActions) ->
            resourceActions.forEach { resourceAction ->
                actions.add(Action(resourceAction))
                val managerPath = mutableListOf<ManagerPath>()
                val projectPath = ManagerPath(
                    iamConfiguration.systemId,
                    AuthResourceType.PROJECT.value,
                    projectCode,
                    projectName
                )
                val resourcePath = ManagerPath(
                    iamConfiguration.systemId,
                    resourceType,
                    "*",
                    ""
                )
                managerPath.add(projectPath)
                managerPath.add(resourcePath)
                val paths = mutableListOf<List<ManagerPath>>()
                paths.add(managerPath)
                resources.add(
                    ManagerResources.builder()
                        .system(iamConfiguration.systemId)
                        .type(resourceType)
                        .paths(paths).build()
                )
            }
        }
        logger.info("build gradle manager authorization scopes action:$actions, resources:$resources")
        return AuthorizationScopes.builder()
            .system(iamConfiguration.systemId)
            .actions(actions)
            .resources(resources)
            .build()
    }

    @SuppressWarnings("LongParameterList")
    fun buildSubsetManagerAuthorizationScopes(
        strategyName: String,
        projectCode: String,
        projectName: String,
        resourceType: String,
        resourceCode: String,
        resourceName: String
    ): AuthorizationScopes {
        val resourceStrategyMap = getGroupStrategy(strategyName).second
        val actions = mutableListOf<Action>()
        val resources = mutableListOf<ManagerResources>()
        resourceStrategyMap.forEach { (strategyResourceType, resourceActions) ->
            resourceActions.forEach { resourceAction ->
                actions.add(Action(resourceAction))
                val managerPath = mutableListOf<ManagerPath>()
                val projectPath = ManagerPath(
                    iamConfiguration.systemId,
                    AuthResourceType.PROJECT.value,
                    projectCode,
                    projectName
                )
                managerPath.add(projectPath)
                // 如果二级管理员对应的资源类型与策略的资源类型不相同,说明二级管理员资源类型是策略的父资源类型
                if (resourceType != strategyResourceType) {
                    val parentResourcePath = ManagerPath(
                        iamConfiguration.systemId,
                        strategyResourceType,
                        resourceCode,
                        resourceName
                    )
                    val resourcePath = ManagerPath(
                        iamConfiguration.systemId,
                        resourceType,
                        "*",
                        ""
                    )
                    managerPath.add(parentResourcePath)
                    managerPath.add(resourcePath)
                } else {
                    val resourcePath = ManagerPath(
                        iamConfiguration.systemId,
                        strategyResourceType,
                        resourceCode,
                        resourceName
                    )
                    managerPath.add(resourcePath)
                }
                val paths = mutableListOf<List<ManagerPath>>()
                paths.add(managerPath)
                resources.add(
                    ManagerResources.builder()
                        .system(iamConfiguration.systemId)
                        .type(strategyResourceType)
                        .paths(paths).build()
                )
            }
        }
        logger.info("build subset manager authorization scopes action:$actions, resources:$resources")
        return AuthorizationScopes.builder()
            .system(iamConfiguration.systemId)
            .actions(actions)
            .resources(resources)
            .build()
    }

    private fun getGroupStrategy(groupName: String): Pair<List<String>, Map<String, List<String>>> {
        val strategyInfo = strategyService.getStrategyByName(groupName)
            ?: throw ErrorCodeException(
                errorCode = AuthMessageCode.STRATEGT_NAME_NOT_EXIST,
                defaultMessage = MessageCodeUtil.getCodeMessage(
                    messageCode = AuthMessageCode.STRATEGT_NAME_NOT_EXIST,
                    params = arrayOf(groupName)
                ))
        logger.info("getGroupStrategy ${strategyInfo.strategy}")
        val projectStrategyList = mutableListOf<String>()
        val resourceStrategyMap = mutableMapOf<String, List<String>>()
        strategyInfo.strategy.forEach { (resource, list) ->
            val actionData = buildAction(resource, list)
            projectStrategyList.addAll(actionData.first)
            resourceStrategyMap.putAll(actionData.second)
        }
        return Pair(projectStrategyList, resourceStrategyMap)
    }

    private fun buildAction(resource: String, actionList: List<String>): Pair<List<String>, Map<String, List<String>>> {
        val projectStrategyList = mutableListOf<String>()
        val resourceStrategyMap = mutableMapOf<String, List<String>>()
        val resourceStrategyList = mutableListOf<String>()
        // 如果是project相关的资源, 直接拼接action
        if (resource == AuthResourceType.PROJECT.value) {
            actionList.forEach { projectAction ->
                projectStrategyList.add(resource + "_" + projectAction)
            }
        } else {
            actionList.forEach {
                // 如果是非project资源。 若action是create,需挂在project下,因create相关的资源都是绑定在项目下。
                if (it == AuthPermission.CREATE.value) {
                    projectStrategyList.add(resource + "_" + it)
                } else {
                    resourceStrategyList.add(resource + "_" + it)
                }
            }
            resourceStrategyMap[resource] = resourceStrategyList
            logger.info("$resource $resourceStrategyList")
        }
        return Pair(projectStrategyList, resourceStrategyMap)
    }

}
