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
package com.tencent.devops.openapi.service.apigw

import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.quality.pojo.QualityRuleIntercept
import com.tencent.devops.process.api.user.UserPipelineResource
import com.tencent.devops.process.pojo.Permission
import com.tencent.devops.project.api.service.ServiceUserResource
import com.tencent.devops.quality.api.v2.ServiceQualityInterceptResource
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * @Description
 * @Date 2019/9/1
 * @Version 1.0
 */
@Service
class ApigwQualityService(private val client: Client) {

    companion object {
        private val logger = LoggerFactory.getLogger(ApigwQualityService::class.java)
    }

    fun getBuildQuality(
        userId: String,
        bgId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        checkUserPermission: Boolean,
        interfaceName: String? = "ApigwQualityService"
    ): List<QualityRuleIntercept>? {
        logger.info("$interfaceName:getBuildQuality:Input($userId,$projectId,$pipelineId,$buildId,$checkUserPermission)")
        // 权限校验
        val userDeptInfo = client.get(ServiceUserResource::class).getDetailFromCache(userId).data
        if (userDeptInfo == null || userDeptInfo.bgId.trim() != bgId.trim()) {
            logger.warn("$interfaceName:PermissionForbidden:userDeptInfo.bgId=${userDeptInfo?.bgId},bgId=$bgId,userId=$userId")
            throw PermissionForbiddenException(
                message = "$userId doesn't have perssion to access data of bg(bgId=$bgId)"
            )
        }
        if (checkUserPermission) {
            // 验证用户是否有流水线查看权限
            val permissionData = client.get(UserPipelineResource::class).hasPermission(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                permission = Permission.VIEW
            ).data
            if (permissionData == null || permissionData == false) {
                val message = "user($userId) does not have perssion to view pipeline($pipelineId)"
                logger.warn("$interfaceName:$message")
                throw PermissionForbiddenException(
                    message = message
                )
            }
        }
        return client.get(ServiceQualityInterceptResource::class).listHistory(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId
        ).data
    }
}
