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

package com.tencent.devops.process.permission

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.api.AuthResourceApiStr
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.code.BSPipelineAuthServiceCode
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import javax.ws.rs.core.Response

class V3PipelinePermissionServiceImpl @Autowired constructor(
    val authPermissionApi: AuthPermissionApi,
    val authProjectApi: AuthProjectApi,
    val bsPipelineAuthServiceCode: BSPipelineAuthServiceCode,
    val dslContext: DSLContext,
    val pipelineInfoDao: PipelineInfoDao,
    val authResourceApi: AuthResourceApiStr
) : PipelinePermissionService {
    // TODO: 为解决pipelineId较长的问题，此处校验时需将pipelineId转为Id, 获取实例时,需将ID转化为pipelineId

    // 一般为校验用户是否有某项目下的创建逻辑
    override fun checkPipelinePermission(
        userId: String,
        projectId: String,
        permission: AuthPermission
    ): Boolean {
        return authPermissionApi.validateUserResourcePermission(
            user = userId,
            serviceCode = bsPipelineAuthServiceCode,
            resourceType = AuthResourceType.PIPELINE_DEFAULT,
            permission = permission,
            projectCode = projectId
        )
    }

    // 校验用户是否有某项目下某流水线的指定权限
    override fun checkPipelinePermission(
        userId: String,
        projectId: String,
        pipelineId: String,
        permission: AuthPermission
    ): Boolean {
        if (pipelineId == "*") {
            return checkPipelinePermission(
                userId = userId,
                projectId = projectId,
                permission = permission
            )
        }
        val iamId = findInstanceId(projectId, pipelineId)

        return authPermissionApi.validateUserResourcePermission(
            user = userId,
            projectCode = projectId,
            resourceCode = iamId,
            permission = permission,
            resourceType = AuthResourceType.PIPELINE_DEFAULT,
            serviceCode = bsPipelineAuthServiceCode
        )
    }

    override fun validPipelinePermission(
        userId: String,
        projectId: String,
        pipelineId: String,
        permission: AuthPermission,
        message: String?
    ) {
        if (pipelineId == "*") {
            if (!checkPipelinePermission(
                    userId = userId,
                    projectId = projectId,
                    permission = permission
                )) {
                throw PermissionForbiddenException(message)
            }
            return
        }

        val iamId = findInstanceId(projectId, pipelineId)
        if (iamId.isNullOrEmpty()) {
            throw PermissionForbiddenException("流水线不存在")
        }
        val permissionCheck = authPermissionApi.validateUserResourcePermission(
            user = userId,
            projectCode = projectId,
            resourceCode = iamId,
            permission = permission,
            resourceType = AuthResourceType.PIPELINE_DEFAULT,
            serviceCode = bsPipelineAuthServiceCode
        )
        if (!permissionCheck) {
            throw PermissionForbiddenException(message)
        }
    }

    override fun getResourceByPermission(userId: String, projectId: String, permission: AuthPermission): List<String> {

        val iamInstanceList = authPermissionApi.getUserResourceByPermission(
            user = userId,
            serviceCode = bsPipelineAuthServiceCode,
            projectCode = projectId,
            permission = permission,
            supplier = null,
            resourceType = AuthResourceType.PIPELINE_DEFAULT
        )

        val pipelineIds = mutableListOf<String>()
        if (iamInstanceList.contains("*")) {
            pipelineInfoDao.searchByProject(dslContext, projectId)?.map { pipelineIds.add(it.pipelineId) }
        } else {
            val ids = iamInstanceList.map { it.toLong() }
            pipelineInfoDao.getPipelineByAutoId(dslContext, ids, projectId).map { pipelineIds.add(it.pipelineId) }
        }
        return pipelineIds
    }

    override fun filterPipelines(
        userId: String,
        projectId: String,
        authPermissions: Set<AuthPermission>,
        pipelineIds: List<String>
    ): Map<AuthPermission, List<String>> {
        return authPermissions.associateWith {
            pipelineIds
        }
    }

    override fun createResource(
        userId: String,
        projectId: String,
        pipelineId: String,
        pipelineName: String
    ) {
        val pipelineAutoId = findInstanceId(projectId, pipelineId)
        return authResourceApi.createResource(
            user = userId,
            projectCode = projectId,
            serviceCode = bsPipelineAuthServiceCode.id(),
            resourceType = AuthResourceType.PIPELINE_DEFAULT.value,
            resourceCode = pipelineAutoId,
            resourceName = pipelineName
        )
    }

    override fun modifyResource(projectId: String, pipelineId: String, pipelineName: String) {
        return
    }

    override fun deleteResource(projectId: String, pipelineId: String) {
        return
    }

    override fun isProjectUser(userId: String, projectId: String, group: BkAuthGroup?): Boolean {
        return authProjectApi.isProjectUser(
            user = userId,
            projectCode = projectId,
            group = group,
            serviceCode = bsPipelineAuthServiceCode
        )
    }

    override fun checkProjectManager(userId: String, projectId: String): Boolean {
        return authProjectApi.checkProjectManager(userId, bsPipelineAuthServiceCode, projectId)
    }

    private fun findInstanceId(projectId: String, pipelineId: String): String {
        val pipelineInfo = pipelineInfoDao.getPipelineInfo(dslContext, projectId, pipelineId)
            ?: throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_PIPELINE_MODEL_NOT_EXISTS,
                defaultMessage = "流水线编排不存在"
            )
        return pipelineInfo.id.toString()
    }
}
