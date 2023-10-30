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

package com.tencent.devops.process.permission.template

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.api.AuthResourceApi
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.code.PipelineAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.model.process.tables.TTemplate
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import com.tencent.devops.project.api.service.ServiceProjectResource
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Result
import org.slf4j.LoggerFactory

@Suppress("LongParameterList")
class RbacPipelineTemplatePermissionService constructor(
    val authPermissionApi: AuthPermissionApi,
    authProjectApi: AuthProjectApi,
    pipelineAuthServiceCode: PipelineAuthServiceCode,
    val dslContext: DSLContext,
    val pipelineInfoDao: PipelineInfoDao,
    val client: Client,
    val authResourceApi: AuthResourceApi
) : AbstractPipelineTemplatePermissionService(
    authProjectApi = authProjectApi,
    pipelineAuthServiceCode = pipelineAuthServiceCode
) {
    override fun checkPipelineTemplatePermission(
        userId: String,
        projectId: String,
        permission: AuthPermission
    ): Boolean {
        // todo 待校验，若该项目未迁移模板资源，去校验模板权限，是否会报错？
        if (!authPermissionApi.validateUserResourcePermission(
                user = userId,
                serviceCode = pipelineAuthServiceCode,
                resourceType = resourceType,
                permission = permission,
                projectCode = projectId
            )) {
            logger.warn("user($userId) has no $permission permission under project($projectId)")
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_NO_PERMISSION_OPERATION_TEMPLATE,
                defaultMessage = "user($userId) has no $permission permission under project($projectId)!"
            )
        }
        return true
    }

    override fun checkPipelineTemplatePermission(
        userId: String,
        projectId: String,
        templateId: String,
        permission: AuthPermission
    ): Boolean {
        // todo 待校验，若该项目未迁移模板资源，去校验模板权限，是否会报错？
        if (!authPermissionApi.validateUserResourcePermission(
                user = userId,
                serviceCode = pipelineAuthServiceCode,
                resourceType = resourceType,
                projectCode = projectId,
                resourceCode = templateId,
                permission = permission
            )) {
            logger.warn(
                "The user($userId) does not have permission to " +
                    "${permission.value} the template under this project($projectId)"
            )
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_NO_PERMISSION_OPERATION_TEMPLATE,
                defaultMessage = "The user($userId) does not have permission to " +
                    "${permission.value} the template under this project($projectId)"
            )
        }
        return true
    }

    override fun getResourcesByPermission(
        userId: String,
        projectId: String,
        permissions: Set<AuthPermission>,
        templateRecords: Result<out Record>?
    ): Map<AuthPermission, Result<out Record>> {
        if (templateRecords == null) return emptyMap()
        // 是否开启模板权限管理
        val enableTemplatePermissionManage = enableTemplatePermissionManage(projectId)
        return permissions.associateWith { permission ->
            if (!enableTemplatePermissionManage) {
                handleTemplateWithoutPermissionManage(
                    permission = permission,
                    templateRecords = templateRecords,
                    userId = userId,
                    projectId = projectId
                )
            } else {
                handleTemplateWithPermissionManage(
                    permission = permission,
                    templateRecords = templateRecords,
                    userId = userId,
                    projectId = projectId
                )
            }
        }
    }

    override fun createResource(
        userId: String,
        projectId: String,
        templateId: String,
        templateName: String
    ) {
        if (enableTemplatePermissionManage(projectId)) {
            authResourceApi.createResource(
                user = userId,
                projectCode = projectId,
                serviceCode = pipelineAuthServiceCode,
                resourceType = resourceType,
                resourceCode = templateId,
                resourceName = templateName
            )
        }
    }

    override fun modifyResource(
        userId: String,
        projectId: String,
        templateId: String,
        templateName: String
    ) {
        if (enableTemplatePermissionManage(projectId)) {
            authResourceApi.modifyResource(
                projectCode = projectId,
                serviceCode = pipelineAuthServiceCode,
                resourceType = resourceType,
                resourceCode = templateId,
                resourceName = templateName
            )
        }
    }

    override fun deleteResource(projectId: String, templateId: String) {
        if (enableTemplatePermissionManage(projectId)) {
            authResourceApi.deleteResource(
                serviceCode = pipelineAuthServiceCode,
                resourceType = resourceType,
                projectCode = projectId,
                resourceCode = templateId
            )
        }
    }

    private fun enableTemplatePermissionManage(projectId: String): Boolean {
        val projectInfo = client.get(ServiceProjectResource::class).get(englishName = projectId).data
        return projectInfo != null && projectInfo.properties?.enableTemplatePermissionManage == true
    }

    private fun handleTemplateWithPermissionManage(
        permission: AuthPermission,
        templateRecords: Result<out Record>,
        userId: String,
        projectId: String
    ): Result<out Record> {
        val resources = authPermissionApi.getUserResourceByPermission(
            user = userId,
            serviceCode = pipelineAuthServiceCode,
            resourceType = resourceType,
            projectCode = projectId,
            permission = permission,
            supplier = null
        )
        return templateRecords.filter { record ->
            val tTemplate = TTemplate.T_TEMPLATE
            val templateId = record[tTemplate.ID]
            resources.contains(templateId)
        } as Result<out Record>
    }

    companion object {
        private val resourceType = AuthResourceType.PIPELINE_TEMPLATE
        private val logger = LoggerFactory.getLogger(RbacPipelineTemplatePermissionService::class.java)
    }
}
