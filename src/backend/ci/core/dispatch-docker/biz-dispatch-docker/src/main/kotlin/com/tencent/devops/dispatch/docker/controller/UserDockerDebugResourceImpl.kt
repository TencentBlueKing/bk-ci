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

package com.tencent.devops.dispatch.docker.controller

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.type.BuildType
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.docker.api.user.UserDockerDebugResource
import com.tencent.devops.dispatch.docker.pojo.DebugResponse
import com.tencent.devops.dispatch.docker.pojo.DebugStartParam
import com.tencent.devops.dispatch.docker.pojo.enums.DockerRoutingType
import com.tencent.devops.dispatch.docker.service.DockerRoutingService
import com.tencent.devops.dispatch.docker.service.debug.DebugServiceEnum
import com.tencent.devops.dispatch.docker.service.debug.ExtDebugService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.util.stream.Collectors

@RestResource
class UserDockerDebugResourceImpl @Autowired constructor(
    private val dockerRoutingService: DockerRoutingService,
    private val extDebugService: ExtDebugService
) : UserDockerDebugResource {

    override fun startDebug(userId: String, debugStartParam: DebugStartParam): Result<DebugResponse>? {
        logger.info("[$userId]| start debug, debugStartParam: $debugStartParam")
        // dispatchType不在枚举工厂类内时默认为ext debug服务
        if (!DebugServiceEnum
                .values().toList()
                .stream().map { it.name }.collect(Collectors.toList()).contains(debugStartParam.dispatchType)) {
            val debugUrl = extDebugService.startDebug(
                userId = userId,
                projectId = debugStartParam.projectId,
                pipelineId = debugStartParam.pipelineId,
                buildId = debugStartParam.buildId,
                vmSeqId = debugStartParam.vmSeqId
            ) ?: throw ErrorCodeException(
                errorCode = "2103503",
                defaultMessage = "Can not found debug container.",
                params = arrayOf(debugStartParam.pipelineId)
            )

            return Result(
                DebugResponse(
                    websocketUrl = debugUrl,
                    containerName = null,
                    dispatchType = BuildType.PUBLIC_DEVCLOUD.name
                )
            )
        }

        val formatDispatchType = formatDispatchType(debugStartParam.projectId)
        val debugUrl = DebugServiceEnum.valueOf(formatDispatchType.name).instance().startDebug(
            userId = userId,
            projectId = debugStartParam.projectId,
            pipelineId = debugStartParam.pipelineId,
            vmSeqId = debugStartParam.vmSeqId,
            buildId = debugStartParam.buildId
        )
        return Result(
            DebugResponse(
            websocketUrl = debugUrl,
            containerName = null,
            dispatchType = formatDispatchType.name
        )
        )
    }

    override fun stopDebug(
        userId: String,
        projectId: String,
        pipelineId: String,
        vmSeqId: String,
        containerName: String?,
        dispatchType: String?
    ): Result<Boolean>? {
        if (!DebugServiceEnum
                .values().toList()
                .stream().map { it.name }.collect(Collectors.toList()).contains(dispatchType)) {
            val result = extDebugService.stopDebug(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                vmSeqId = vmSeqId,
                containerName = containerName ?: ""
            )

            return Result(result)
        }

        val formatDispatchType = formatDispatchType(projectId)
        return Result(DebugServiceEnum.valueOf(formatDispatchType.name!!).instance().stopDebug(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            vmSeqId = vmSeqId,
            containerName = containerName ?: ""
        ))
    }

    /**
     * BCS和VM构建类型在前端统一表现为VM类型，通过白名单控制BCS构建类型路由
     */
    private fun formatDispatchType(projectId: String): BuildType {
        return when (dockerRoutingService.getDockerRoutingType(projectId)) {
            DockerRoutingType.VM -> BuildType.DOCKER
            DockerRoutingType.BCS -> BuildType.PUBLIC_BCS
            else -> BuildType.DOCKER
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(UserDockerDebugResourceImpl::class.java)
    }
}
