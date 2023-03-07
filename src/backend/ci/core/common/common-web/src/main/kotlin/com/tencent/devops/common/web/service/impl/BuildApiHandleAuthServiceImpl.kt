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

package com.tencent.devops.common.web.service.impl

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_BUILD_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PROJECT_ID
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.common.web.service.BuildApiHandleService
import com.tencent.devops.common.web.service.ServiceBuildApiPermissionResource
import org.slf4j.LoggerFactory
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@Suppress("ComplexCondition")
class BuildApiHandleAuthServiceImpl : BuildApiHandleService {

    companion object {
        private val logger = LoggerFactory.getLogger(BuildApiHandleAuthServiceImpl::class.java)
    }

    override fun handleBuildApiService(parameterNames: Array<String>, parameterValue: Array<Any>) {
        val request = (RequestContextHolder.getRequestAttributes() as ServletRequestAttributes).request
        val authBuildId = request.getHeader(AUTH_HEADER_DEVOPS_BUILD_ID)
        val authProjectId = request.getHeader(AUTH_HEADER_DEVOPS_PROJECT_ID)
        if (!parameterNames.contains("projectId") || !parameterNames.contains("pipelineId")) return
        var projectId: String? = null
        var pipelineId: String? = null
        for (index in parameterValue.indices) {
            when (parameterNames[index]) {
                "projectId" -> {
                    projectId = parameterValue[index].toString()
                }
                "pipelineId" -> {
                    pipelineId = parameterValue[index].toString()
                }
            }
        }
        logger.info("Build ProjectId[$authProjectId], BuildID[$authBuildId],user project param[$projectId], " +
                "user pipeline param[$pipelineId]")
        if (projectId != null && pipelineId != null && authProjectId != null && authBuildId != null) {
            val client = SpringContextUtil.getBean(Client::class.java)
            val buildStartUser = client.get(ServiceBuildApiPermissionResource::class)
                .getStartUser(authProjectId, authBuildId).data!!
            logger.info("verify that user [$buildStartUser] has permission to access information " +
                    "in pipeline [$pipelineId] under project [$projectId].")
            val checkPipelinePermissionResult = client.get(ServiceBuildApiPermissionResource::class).verifyApi(
                userId = buildStartUser,
                projectId = projectId,
                pipelineId = pipelineId
            ).data!!
            if (!checkPipelinePermissionResult) {
                logger.info("The user [$buildStartUser] does not have permission to access " +
                        "the project [$projectId] pipeline [$pipelineId] build info")
            }
        } else {
            logger.warn(
                "The parameter of this request is abnormal {" +
                    "Build ProjectId[$authProjectId], BuildID[$authBuildId],user project param[$projectId], " +
                    "user pipeline param[$pipelineId]}"
            )
        }
    }
}
