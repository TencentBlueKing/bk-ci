/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.process.api.op

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.event.CallBackEvent
import com.tencent.devops.common.pipeline.event.CallBackNetWorkRegionType
import com.tencent.devops.common.pipeline.event.CallbackConstants
import com.tencent.devops.common.pipeline.event.ProjectPipelineCallBack
import com.tencent.devops.common.pipeline.pojo.secret.ISecretParam
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.engine.service.ProjectPipelineCallBackService
import com.tencent.devops.process.pojo.CreateCallBackResult
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpPipelineCallbackResourceImpl @Autowired constructor(
    private val projectPipelineCallBackService: ProjectPipelineCallBackService
) : OpPipelineCallbackResource {

    override fun enableCallbackByIds(projectId: String, callbackIds: String): Result<Boolean> {
        if (projectId.isEmpty()) {
            throw ParamBlankException("Invalid projectId")
        }
        if (callbackIds.isEmpty()) {
            throw ParamBlankException("Invalid callbackIds")
        }
        projectPipelineCallBackService.enableByIds(
            projectId = projectId,
            callbackIds = callbackIds
        )
        return Result(true)
    }

    override fun enableCallbackByUrl(projectId: String?, url: String): Result<Boolean> {
        if (url.isEmpty()) {
            throw ParamBlankException("Invalid url")
        }
        logger.info("OpPipelineCallbackResource:begin enableCallback-----------")
        try {
            val limit = 1000
            var offset = 0
            do {
                val disableCallbackList = projectPipelineCallBackService.getDisableCallbackList(
                    limit = limit,
                    offset = offset,
                    projectId = projectId,
                    url = url
                )
                val pageSize = disableCallbackList.size
                disableCallbackList.forEach {
                    projectPipelineCallBackService.enable(it)
                }
                offset += limit
                // 一秒休眠时间
                Thread.sleep(1 * 1000)
            } while (pageSize == limit)
        } catch (e: Exception) {
            logger.warn("OpPipelineCallbackResource：enableCallback failed | $e ")
        }
        logger.info("OpPipelineCallbackResource:end enableCallback-----------")
        return Result(true)
    }

    override fun disableCallbackByIds(projectId: String, callbackIds: String): Result<Boolean> {
        if (projectId.isEmpty()) {
            throw ParamBlankException("Invalid projectId")
        }
        if (callbackIds.isEmpty()) {
            throw ParamBlankException("Invalid callbackIds")
        }
        projectPipelineCallBackService.batchDisable(projectId = projectId, callbackIds = callbackIds)
        return Result(true)
    }

    override fun createProjectCallback(
        userId: String,
        eventType: CallBackEvent,
        url: String,
        region: CallBackNetWorkRegionType?,
        secretParam: ISecretParam
    ): Result<CreateCallBackResult> {
        logger.info("start create project callback|$userId|$eventType|$region|$url")
        return Result(
            projectPipelineCallBackService.createCallBack(
                userId = userId,
                projectId = CallbackConstants.DEVOPS_ALL_PROJECT,
                region = region,
                event = eventType.name,
                secretParam = secretParam,
                secretToken = null,
                needCheckPermission = false,
                url = url
            )
        )
    }

    override fun delete(userId: String, id: Long): Result<Boolean> {
        projectPipelineCallBackService.delete(
            userId = userId,
            projectId = CallbackConstants.DEVOPS_ALL_PROJECT,
            id = id,
            needCheckPermission = false
        )
        return Result(true)
    }

    override fun list(userId: String, event: CallBackEvent): Result<List<ProjectPipelineCallBack>> {
        return Result(
            projectPipelineCallBackService.listProjectCallBack(
                projectId = CallbackConstants.DEVOPS_ALL_PROJECT,
                events = event.name
            )
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(OpPipelineCallbackResourceImpl::class.java)
    }
}
