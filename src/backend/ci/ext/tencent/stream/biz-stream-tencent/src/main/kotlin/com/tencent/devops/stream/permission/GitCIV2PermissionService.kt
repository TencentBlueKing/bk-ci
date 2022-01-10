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

package com.tencent.devops.stream.permission

import com.tencent.devops.auth.api.service.ServicePermissionAuthResource
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.stream.v2.dao.StreamBasicSettingDao
import com.tencent.devops.stream.common.exception.GitCINoEnableException
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.ws.rs.core.Response

/**
 * 角色权限对接AUTH，操作类权限检查OAuth权限，和工蜂角色权限(工蜂30以上权限，所以纯查看接口不需要权限校验)
 * 开启权限，通过工蜂项目是否开启判断
 * 首先判断是否具有角色权限，在判断是否开启
 * 项目开启：419 无OAuth：418 无角色：403
 */
@Service
class GitCIV2PermissionService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val streamBasicSettingDao: StreamBasicSettingDao,
    private val tokenCheckService: ClientTokenService
) {
    // 校验只需要工蜂不需要OAuth的
    fun checkGitCIPermission(
        userId: String,
        projectId: String,
        permission: AuthPermission = AuthPermission.VIEW
    ) {
        checkPermissionAndOauth(userId, projectId, permission)
    }

    // 校验只需要OAuth的
    fun checkGitCIAndOAuth(
        userId: String,
        projectId: String,
        permission: AuthPermission = AuthPermission.EDIT
    ) {
        checkPermissionAndOauth(userId, projectId, permission)
    }

    fun checkWebPermission(userId: String, projectId: String): Boolean {
        logger.info("GitCIEnvironmentPermission user:$userId projectId: $projectId ")

        val result = client.get(ServicePermissionAuthResource::class).validateUserResourcePermission(
            userId = userId,
            token = tokenCheckService.getSystemToken(null) ?: "",
            action = AuthPermission.WEB_CHECK.value,
            projectCode = projectId,
            resourceCode = AuthResourceType.PIPELINE_DEFAULT.value
        ).data ?: false
        if (!result) {
            logger.warn("$projectId $userId checkWebPermission fail")
        }
        return result
    }

    fun checkGitCIAndOAuthAndEnable(
        userId: String,
        projectId: String,
        gitProjectId: Long,
        permission: AuthPermission = AuthPermission.EDIT
    ) {
        checkPermissionAndOauth(userId, projectId, permission)
        checkEnableGitCI(gitProjectId)
    }

    fun checkEnableGitCI(
        gitProjectId: Long
    ) {
        val setting = streamBasicSettingDao.getSetting(dslContext, gitProjectId)
        if (setting == null || !setting.enableCi) {
            throw GitCINoEnableException(setting?.name ?: gitProjectId.toString())
        }
    }

    private fun checkPermissionAndOauth(userId: String, projectId: String, permission: AuthPermission) {
        logger.info("GitCIEnvironmentPermission user:$userId projectId: $projectId ")
        val result = client.get(ServicePermissionAuthResource::class).validateUserResourcePermission(
            userId = userId,
            token = tokenCheckService.getSystemToken(null) ?: "",
            action = permission.value,
            projectCode = projectId,
            resourceCode = null
        ).data
        // 说明用户没有工蜂权限
        if (result == null || !result) {
            throw CustomException(
                Response.Status.FORBIDDEN,
                "Permission denied."
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GitCIV2PermissionService::class.java)
    }
}
