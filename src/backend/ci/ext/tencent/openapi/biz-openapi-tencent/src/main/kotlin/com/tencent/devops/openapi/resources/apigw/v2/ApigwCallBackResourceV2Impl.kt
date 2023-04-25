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
package com.tencent.devops.openapi.resources.apigw.v2

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.event.CallBackEvent
import com.tencent.devops.common.pipeline.event.CallBackNetWorkRegionType
import com.tencent.devops.common.pipeline.event.ProjectPipelineCallBack
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.v2.ApigwCallBackResourceV2
import com.tencent.devops.process.api.service.ServiceCallBackResource
import com.tencent.devops.process.pojo.ProjectPipelineCallBackHistory
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwCallBackResourceV2Impl @Autowired constructor(
    private val client: Client
) : ApigwCallBackResourceV2 {
    override fun create(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        url: String,
        region: CallBackNetWorkRegionType,
        event: CallBackEvent,
        secretToken: String?
    ): Result<Boolean> {
        return client.get(ServiceCallBackResource::class).create(
            userId = userId,
            projectId = projectId,
            url = url,
            region = region,
            event = event,
            secretToken = secretToken
        )
    }

    override fun list(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        page: Int?,
        pageSize: Int?
    ): Result<Page<ProjectPipelineCallBack>> {
        return client.get(ServiceCallBackResource::class).list(
            userId = userId,
            projectId = projectId,
            page = page,
            pageSize = pageSize
        )
    }

    override fun remove(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        id: Long
    ): Result<Boolean> {
        return client.get(ServiceCallBackResource::class).remove(
            userId = userId,
            projectId = projectId,
            id = id
        )
    }

    override fun listHistory(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        url: String,
        event: CallBackEvent,
        startTime: String?,
        endTime: String?,
        page: Int?,
        pageSize: Int?
    ): Result<Page<ProjectPipelineCallBackHistory>> {
        return client.get(ServiceCallBackResource::class).listHistory(
            userId = userId,
            projectId = projectId,
            url = url,
            event = event,
            startTime = if (startTime == null) {
                null
            } else {
                DateTimeUtil.stringToLocalDateTime(startTime).timestamp()
            },
            endTime = if (endTime == null) {
                null
            } else {
                DateTimeUtil.stringToLocalDateTime(endTime).timestamp()
            },
            page = page,
            pageSize = pageSize
        )
    }

    override fun retry(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        id: Long
    ): Result<Boolean> {
        return client.get(ServiceCallBackResource::class).retry(
            userId = userId,
            projectId = projectId,
            id = id
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ApigwCallBackResourceV2Impl::class.java)
    }
}
