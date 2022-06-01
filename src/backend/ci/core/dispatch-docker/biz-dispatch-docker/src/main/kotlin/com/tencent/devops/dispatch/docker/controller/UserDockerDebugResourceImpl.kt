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
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.docker.api.user.UserDockerDebugResource
import com.tencent.devops.dispatch.docker.dao.PipelineDockerDebugDao
import com.tencent.devops.dispatch.docker.pojo.DebugStartParam
import com.tencent.devops.dispatch.docker.service.debug.DebugServiceEnum
import com.tencent.devops.dispatch.docker.service.debug.impl.DockerHostDebugServiceImpl
import com.tencent.devops.dispatch.docker.service.debug.ExtDebugService
import com.tencent.devops.common.service.BkTag
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.util.stream.Collectors

@RestResource
class UserDockerDebugResourceImpl @Autowired constructor(
    private val dockerHostDebugService: DockerHostDebugServiceImpl,
    private val pipelineDockerDebugDao: PipelineDockerDebugDao,
    private val extDebugService: ExtDebugService,
    private val dslContext: DSLContext,
    private val bkTag: BkTag
) : UserDockerDebugResource {

    override fun startDebug(userId: String, debugStartParam: DebugStartParam): Result<String>? {
        logger.info("[$userId]| start debug, debugStartParam: $debugStartParam")
        // dispatchType不在枚举工厂类内时默认委ext debug服务
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

            return Result(debugUrl)
        }

        return Result(DebugServiceEnum.valueOf(debugStartParam.dispatchType).instance().startDebug(
            userId = userId,
            projectId = debugStartParam.projectId,
            pipelineId = debugStartParam.pipelineId,
            vmSeqId = debugStartParam.vmSeqId,
            buildId = debugStartParam.buildId
        ))
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

        return Result(DebugServiceEnum.valueOf(dispatchType!!).instance().stopDebug(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            vmSeqId = vmSeqId,
            containerName = containerName ?: ""
        ))
    }

    companion object {
        private val logger = LoggerFactory.getLogger(UserDockerDebugResourceImpl::class.java)
    }
}
