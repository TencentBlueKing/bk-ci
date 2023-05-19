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

package com.tencent.devops.process.api.service

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.pipeline.event.CallBackEvent
import com.tencent.devops.common.pipeline.event.CallBackNetWorkRegionType
import com.tencent.devops.common.pipeline.event.PipelineCallbackEvent
import com.tencent.devops.common.pipeline.event.ProjectPipelineCallBack
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_CALLBACK_SAVE_FAIL
import com.tencent.devops.process.engine.service.ProjectPipelineCallBackService
import com.tencent.devops.process.pojo.CreateCallBackResult
import com.tencent.devops.process.pojo.ProjectPipelineCallBackHistory
import org.springframework.beans.factory.annotation.Autowired

@Suppress("ALL")
@RestResource
class ServiceCallBackResourceImpl @Autowired constructor(
    val projectPipelineCallBackService: ProjectPipelineCallBackService
) : ServiceCallBackResource {

    override fun create(
        userId: String,
        projectId: String,
        url: String,
        region: CallBackNetWorkRegionType?,
        event: CallBackEvent,
        secretToken: String?
    ): Result<Boolean> {
        val result = projectPipelineCallBackService.createCallBack(
            userId = userId,
            projectId = projectId,
            url = url,
            region = region,
            event = event.name,
            secretToken = secretToken
        )
        return if (result.failureEvents.isNotEmpty()) {
            throw ErrorCodeException(
                errorCode = ERROR_CALLBACK_SAVE_FAIL,
                params = arrayOf(result.failureEvents[event.name] ?: "")
            )
        } else {
            Result(true)
        }
    }

    override fun batchCreate(
        userId: String,
        projectId: String,
        url: String,
        region: CallBackNetWorkRegionType?,
        event: String,
        secretToken: String?
    ): Result<CreateCallBackResult> {
        return Result(
            projectPipelineCallBackService.createCallBack(
                userId = userId,
                projectId = projectId,
                url = url,
                region = region,
                event = event,
                secretToken = secretToken
            )
        )
    }

    override fun list(
        userId: String,
        projectId: String,
        page: Int?,
        pageSize: Int?
    ): Result<Page<ProjectPipelineCallBack>> {
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: 20
        val limit = PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull)
        val result = projectPipelineCallBackService.listByPage(
            userId = userId,
            projectId = projectId,
            offset = limit.offset,
            limit = limit.limit
        )
        return Result(Page(pageNotNull, pageSizeNotNull, result.count, result.records))
    }

    override fun remove(userId: String, projectId: String, id: Long): Result<Boolean> {
        projectPipelineCallBackService.delete(
            userId = userId,
            projectId = projectId,
            id = id
        )
        return Result(true)
    }

    override fun listHistory(
        userId: String,
        projectId: String,
        url: String,
        event: CallBackEvent,
        startTime: Long?,
        endTime: Long?,
        page: Int?,
        pageSize: Int?
    ): Result<Page<ProjectPipelineCallBackHistory>> {
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize?.coerceAtMost(20) ?: 20
        val limit = PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull)
        val result = projectPipelineCallBackService.listHistory(
            userId = userId,
            projectId = projectId,
            callBackUrl = url,
            events = event.name,
            startTime = startTime,
            endTime = endTime,
            offset = limit.offset,
            limit = limit.limit
        )
        return Result(Page(pageNotNull, pageSizeNotNull, result.count, result.records))
    }

    override fun retry(userId: String, projectId: String, id: Long): Result<Boolean> {
        projectPipelineCallBackService.retry(
            userId = userId,
            projectId = projectId,
            id = id
        )
        return Result(true)
    }

    override fun createPipelineCallBack(
        userId: String,
        projectId: String,
        pipelineId: String,
        callbackInfo: PipelineCallbackEvent
    ): Result<Boolean> {
        projectPipelineCallBackService.bindPipelineCallBack(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            callbackInfo = callbackInfo
        )
        return Result(true)
    }
}
