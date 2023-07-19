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

package com.tencent.devops.process.permission

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.api.AuthResourceApi
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.code.PipelineAuthServiceCode
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.ProcessMessageCode
import javax.ws.rs.core.Response

/**
 * Pipeline专用权限校验接口
 */
abstract class AbstractPipelinePermissionService constructor(
    private val authProjectApi: AuthProjectApi,
    private val authResourceApi: AuthResourceApi,
    protected open val authPermissionApi: AuthPermissionApi,
    protected open val pipelineAuthServiceCode: PipelineAuthServiceCode
) : PipelinePermissionService {

    private val resourceType = AuthResourceType.PIPELINE_DEFAULT

    /**
     * 校验是否有任意流水线存在指定的权限
     * @param userId userId
     * @param projectId projectId
     * @param permission 权限
     * @return 有权限返回true
     */
    override fun checkPipelinePermission(userId: String, projectId: String, permission: AuthPermission): Boolean {
        return checkPipelinePermission(
            userId = userId,
            projectId = projectId,
            pipelineId = "*",
            permission = permission
        )
    }

    /**
     * 校验pipeline是否有指定权限
     * @param userId userId
     * @param projectId projectId
     * @param pipelineId pipelineId
     * @param permission 权限
     * @return 有权限返回true
     */
    override fun checkPipelinePermission(
        userId: String,
        projectId: String,
        pipelineId: String,
        permission: AuthPermission
    ): Boolean {

        return authPermissionApi.validateUserResourcePermission(
            user = userId,
            serviceCode = pipelineAuthServiceCode,
            resourceType = resourceType,
            projectCode = projectId,
            resourceCode = pipelineId,
            permission = permission
        )
    }

    override fun validPipelinePermission(
        userId: String,
        projectId: String,
        pipelineId: String,
        permission: AuthPermission,
        message: String?
    ) {
        if (!authPermissionApi.validateUserResourcePermission(
                user = userId,
                serviceCode = pipelineAuthServiceCode,
                resourceType = resourceType,
                projectCode = projectId,
                resourceCode = pipelineId,
                permission = permission
            )
        ) {
            val alias = permission.getI18n(I18nUtil.getLanguage(userId))
            throw ErrorCodeException(
                statusCode = Response.Status.FORBIDDEN.statusCode,
                errorCode = ProcessMessageCode.USER_NEED_PIPELINE_X_PERMISSION,
                defaultMessage = message,
                params = arrayOf(alias)
            )
        }
    }

    /**
     * 获取用户所拥有指定权限下的流水线ID列表
     * @param userId 用户ID
     * @param projectId projectCode英文id
     * @param permission 权限类型
     * @return 返回资源code列表
     */
    override fun getResourceByPermission(
        userId: String,
        projectId: String,
        permission: AuthPermission
    ): List<String> =
        authPermissionApi.getUserResourceByPermission(
            user = userId,
            serviceCode = pipelineAuthServiceCode,
            resourceType = resourceType,
            projectCode = projectId,
            permission = permission,
            supplier = supplierForFakePermission(projectId)
        )

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

    /**
     * 当权限为空时提供数据的接口
     */
    abstract fun supplierForFakePermission(projectId: String): () -> MutableList<String>

    /**
     * 注册流水线到权限中心与权限关联
     * @param userId userId
     * @param projectId projectId
     * @param pipelineId pipelineId
     * @param pipelineName pipelineName
     */
    override fun createResource(userId: String, projectId: String, pipelineId: String, pipelineName: String) {
        authResourceApi.createResource(
            user = userId,
            serviceCode = pipelineAuthServiceCode,
            resourceType = resourceType,
            projectCode = projectId,
            resourceCode = pipelineId,
            resourceName = pipelineName
        )
    }

    /**
     * 修改流水线在权限中心中的资源属性
     * @param projectId projectId
     * @param pipelineId pipelineId
     * @param pipelineName pipelineName
     */
    override fun modifyResource(projectId: String, pipelineId: String, pipelineName: String) {
        authResourceApi.modifyResource(
            serviceCode = pipelineAuthServiceCode,
            resourceType = resourceType,
            projectCode = projectId,
            resourceCode = pipelineId,
            resourceName = pipelineName
        )
    }

    /**
     * 从权限中心删除流水线资源
     * @param projectId projectId
     * @param pipelineId pipelineId
     */
    override fun deleteResource(projectId: String, pipelineId: String) {
        try {
            authResourceApi.deleteResource(
                serviceCode = pipelineAuthServiceCode,
                resourceType = resourceType,
                projectCode = projectId,
                resourceCode = pipelineId
            )
        } catch (ignored: Throwable) {
        }
    }

    /**
     * 判断是否某个项目中某个组角色的成员
     * @param userId 用户id
     * @param projectId projectId
     * @param group 项目组角色
     */
    override fun isProjectUser(userId: String, projectId: String, group: BkAuthGroup?): Boolean {
        if (group != null && group == BkAuthGroup.MANAGER) {
            return checkProjectManager(userId, projectId)
        }
        return authProjectApi.isProjectUser(
            user = userId,
            serviceCode = pipelineAuthServiceCode,
            projectCode = projectId,
            group = group
        )
    }

    override fun checkProjectManager(userId: String, projectId: String): Boolean {
        return authProjectApi.checkProjectManager(
            userId = userId,
            projectCode = projectId,
            serviceCode = pipelineAuthServiceCode
        )
    }
}
