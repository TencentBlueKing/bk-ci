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
package com.tencent.devops.openapi.resources.apigw.v4

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.consul.ConsulConstants.PROJECT_TAG_REDIS_KEY
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.BkTag
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.v4.ApigwProjectResourceV4
import com.tencent.devops.openapi.service.OpenapiPermissionService
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.project.pojo.ProjectCreateInfo
import com.tencent.devops.project.pojo.ProjectCreateUserInfo
import com.tencent.devops.project.pojo.ProjectUpdateInfo
import com.tencent.devops.project.pojo.ProjectVO
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.pojo.enums.ProjectValidateType
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value

@RestResource
class ApigwProjectResourceV4Impl @Autowired constructor(
    private val client: Client,
    private val redisOperation: RedisOperation,
    private val bkTag: BkTag,
    private val openapiPermissionService: OpenapiPermissionService
) : ApigwProjectResourceV4 {
    companion object {
        private val logger = LoggerFactory.getLogger(ApigwProjectResourceV4Impl::class.java)
    }

    @Value("\${project.route.tag:#{null}}")
    private val projectRouteTag: String? = ""

    override fun create(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectCreateInfo: ProjectCreateInfo,
        accessToken: String?
    ): Result<Boolean> {
        logger.info("OPENAPI_PROJECT_V4|$userId|create|$projectCreateInfo|$accessToken|$projectRouteTag")

        // 创建项目需要指定对接的主集群。 不同集群可能共用同一个套集群
        if (!projectRouteTag.isNullOrEmpty()) {
            bkTag.setGatewayTag(projectRouteTag)
        }

        return client.get(ServiceProjectResource::class).create(
            userId = userId,
            projectCreateInfo = projectCreateInfo,
            accessToken = accessToken
        )
    }

    override fun update(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        projectUpdateInfo: ProjectUpdateInfo,
        accessToken: String?
    ): Result<Boolean> {
        logger.info("OPENAPI_PROJECT_V4|$userId|update|$projectId|$projectUpdateInfo|$accessToken")
        return client.get(ServiceProjectResource::class).update(
            userId = userId,
            projectId = projectId,
            projectUpdateInfo = projectUpdateInfo,
            accessToken = accessToken
        )
    }

    override fun get(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        accessToken: String?
    ): Result<ProjectVO?> {
        logger.info("OPENAPI_PROJECT_V4|$userId|get|$projectId")
        return client.get(ServiceProjectResource::class).get(
            englishName = projectId
        )
    }

    override fun list(
        appCode: String?,
        apigwType: String?,
        userId: String,
        accessToken: String?
    ): Result<List<ProjectVO>> {
        logger.info("OPENAPI_PROJECT_V4|$userId|list")
        return client.get(ServiceProjectResource::class).list(
            userId = userId
        )
    }

    override fun validate(
        appCode: String?,
        apigwType: String?,
        userId: String?,
        validateType: ProjectValidateType,
        name: String,
        projectId: String?
    ): Result<Boolean> {
        logger.info("OPENAPI_PROJECT_V4|$userId|validate|$validateType|$name|$projectId")
        return client.get(ServiceProjectResource::class).validate(
            validateType = validateType,
            name = name,
            projectId = projectId
        )
    }

    override fun createProjectUser(
        appCode: String?,
        apigwType: String?,
        userId: String?,
        projectId: String,
        createInfo: ProjectCreateUserInfo
    ): Result<Boolean?> {
        logger.info("createProjectUser v4 |$userId|$projectId|$createInfo|")
        openapiPermissionService.validProjectManagerPermission(appCode, apigwType, userId, projectId)
        val projectConsulTag = redisOperation.hget(PROJECT_TAG_REDIS_KEY, projectId)
        if (!projectConsulTag.isNullOrEmpty()) {
            bkTag.setGatewayTag(projectConsulTag)
        }
        return client.get(ServiceProjectResource::class).createProjectUser(
            projectId = projectId,
            createInfo = createInfo
        )
    }
}
