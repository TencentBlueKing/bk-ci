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

package com.tencent.devops.project.service.iam

import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.bk.sdk.iam.dto.manager.Action
import com.tencent.bk.sdk.iam.dto.manager.AuthorizationScopes
import com.tencent.bk.sdk.iam.dto.manager.ManagerPath
import com.tencent.bk.sdk.iam.dto.manager.ManagerResources
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.utils.TActionUtils

object AuthorizationUtils {

    fun buildManagerResources(
        projectId: String,
        projectName: String,
        iamConfiguration: IamConfiguration
    ): List<AuthorizationScopes> {
        val authorizationScopes = mutableListOf<AuthorizationScopes>()
        authorizationScopes.add(buildProject(projectId, projectName, iamConfiguration))
        authorizationScopes.add(buildResource(
            projectId = projectId,
            projectName = projectName,
            iamConfiguration = iamConfiguration,
            actions = pipelineAction.split(","),
            resourceType = AuthResourceType.PIPELINE_DEFAULT.value
        ))
        authorizationScopes.add(buildResource(
            projectId = projectId,
            projectName = projectName,
            iamConfiguration = iamConfiguration,
            actions = repertoryAction.split(","),
            resourceType = AuthResourceType.CODE_REPERTORY.value
        ))
        authorizationScopes.add(buildResource(
            projectId = projectId,
            projectName = projectName,
            iamConfiguration = iamConfiguration,
            actions = environmentAction.split(","),
            resourceType = AuthResourceType.ENVIRONMENT_ENVIRONMENT.value
        ))
        authorizationScopes.add(buildResource(
            projectId = projectId,
            projectName = projectName,
            iamConfiguration = iamConfiguration,
            actions = envNodeAction.split(","),
            resourceType = AuthResourceType.ENVIRONMENT_ENV_NODE.value
        ))
        authorizationScopes.add(buildResource(
            projectId = projectId,
            projectName = projectName,
            iamConfiguration = iamConfiguration,
            actions = certAction.split(","),
            resourceType = AuthResourceType.TICKET_CERT.value
        ))
        authorizationScopes.add(buildResource(
            projectId = projectId,
            projectName = projectName,
            iamConfiguration = iamConfiguration,
            actions = credentialAction.split(","),
            resourceType = AuthResourceType.TICKET_CREDENTIAL.value
        ))
        authorizationScopes.add(buildResource(
            projectId = projectId,
            projectName = projectName,
            iamConfiguration = iamConfiguration,
            actions = experienceGroupAction.split(","),
            resourceType = TActionUtils.extResourceType(AuthResourceType.EXPERIENCE_GROUP)
        ))
        authorizationScopes.add(buildResource(
            projectId = projectId,
            projectName = projectName,
            iamConfiguration = iamConfiguration,
            actions = experienceTaskAction.split(","),
            resourceType = TActionUtils.extResourceType(AuthResourceType.EXPERIENCE_TASK)
        ))
        authorizationScopes.add(buildResource(
            projectId = projectId,
            projectName = projectName,
            iamConfiguration = iamConfiguration,
            actions = groupAction.split(","),
            resourceType = TActionUtils.extResourceType(AuthResourceType.QUALITY_GROUP)
        ))
        authorizationScopes.add(buildResource(
            projectId = projectId,
            projectName = projectName,
            iamConfiguration = iamConfiguration,
            actions = ruleAction.split(","),
            resourceType = TActionUtils.extResourceType(AuthResourceType.QUALITY_RULE)
        ))
        return authorizationScopes
    }

    private fun buildProject(
        projectId: String,
        projectName: String,
        iamConfiguration: IamConfiguration
    ): AuthorizationScopes {
        val actions = projectAction.split(",")
        // TODO: 添加project相关action
        val managerPath = ManagerPath(iamConfiguration.systemId, AuthResourceType.PROJECT.value, projectId, projectName)
        val managerPaths = mutableListOf<ManagerPath>()
        managerPaths.add(managerPath)
        val paths = mutableListOf<List<ManagerPath>>()
        paths.add(managerPaths)
        val resource = ManagerResources.builder()
            .system(iamConfiguration.systemId)
            .paths(paths)
            .type(AuthResourceType.PROJECT.value)
            .build()
        val resources = mutableListOf<ManagerResources>()
        resources.add(resource)
        val action = mutableListOf<Action>()
        actions.forEach {
            action.add(Action(it))
        }
        return AuthorizationScopes
            .builder()
            .system(iamConfiguration.systemId)
            .actions(action)
            .resources(resources)
            .build()
    }

    private fun buildResource(
        projectId: String,
        projectName: String,
        iamConfiguration: IamConfiguration,
        actions: List<String>,
        resourceType: String
    ): AuthorizationScopes {
        val projectManagerPath = ManagerPath(
            iamConfiguration.systemId,
            AuthResourceType.PROJECT.value,
            projectId,
            projectName
        )
//        val resourceManagerPath = ManagerPath(iamConfiguration.systemId, resourceType, "*", "")
        val managerPaths = mutableListOf<ManagerPath>()
        managerPaths.add(projectManagerPath)
//        managerPaths.add(resourceManagerPath)
        val paths = mutableListOf<List<ManagerPath>>()
        paths.add(managerPaths)
        val resource = ManagerResources.builder()
            .system(iamConfiguration.systemId)
            .type(resourceType)
            .paths(paths)
            .build()
        val resources = mutableListOf<ManagerResources>()
        resources.add(resource)
        val action = mutableListOf<Action>()
        actions.forEach {
            action.add(Action(it))
        }
        return AuthorizationScopes
            .builder()
            .system(iamConfiguration.systemId)
            .actions(action)
            .resources(resources)
            .build()
    }

    private const val projectAction = "project_view,all_action," +
        "pipeline_create,project_views_manager," +
        "repertory_create,credential_create," +
        "environment_create,env_node_create," +
        "cert_create,rule_create,quality_group_create"
    private const val pipelineAction = "pipeline_view,pipeline_edit,pipeline_download," +
        "pipeline_delete,pipeline_share,pipeline_execute"
    private const val repertoryAction = "repertory_view,repertory_edit,repertory_delete,repertory_use"
    private const val credentialAction = "credential_view,credential_edit,credential_delete,credential_use"
    private const val environmentAction = "environment_view,environment_edit,environment_delete,environment_use"
    private const val envNodeAction = "env_node_view,env_node_edit,env_node_delete,env_node_use"
    private const val certAction = "cert_view,cert_edit,cert_delete,cert_use"
    private const val groupAction = "quality_group_delete,quality_group_edit"
    private const val ruleAction = "rule_delete,rule_edit,rule_enable"
    private const val experienceGroupAction = "experience_group_delete,experience_group_edit"
    private const val experienceTaskAction = "experience_task_delete,experience_task_edit"
}
