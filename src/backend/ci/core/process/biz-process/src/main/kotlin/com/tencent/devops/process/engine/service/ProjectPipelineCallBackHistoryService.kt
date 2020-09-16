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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.engine.service

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_CALLBACK_HISTORY_NOT_FOUND
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_CALLBACK_REPLY_FAIL
import com.tencent.devops.process.dao.ProjectPipelineCallbackHistoryDao
import com.tencent.devops.process.pojo.ProjectPipelineCallBackHistory
import com.tencent.devops.process.pojo.RequestHeader
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ProjectPipelineCallBackHistoryService @Autowired constructor(
    private val dslContext: DSLContext,
    private val projectPipelineCallbackHistoryDao: ProjectPipelineCallbackHistoryDao
) {

    companion object {
        private val logger = LoggerFactory.getLogger(ProjectPipelineCallBackHistoryService::class.java)
        private val JSON = MediaType.parse("application/json;charset=utf-8")
    }

    fun create(
        projectPipelineCallBackHistory: ProjectPipelineCallBackHistory
    ) {
        with(projectPipelineCallBackHistory) {
            projectPipelineCallbackHistoryDao.create(
                dslContext,
                projectId,
                callBackUrl,
                events,
                status,
                JsonUtil.toJson(requestHeader),
                requestBody,
                response,
                errorMsg
            )
        }
    }

    fun get(
        userId: String,
        projectId: String,
        id: Long
    ): ProjectPipelineCallBackHistory? {
        val record = projectPipelineCallbackHistoryDao.get(dslContext, id)
        return record?.let {
            ProjectPipelineCallBackHistory(
                it.id,
                it.projectId,
                it.callbackUrl,
                it.events,
                it.status,
                it.createdTime.timestamp(),
                JsonUtil.to(it.requestHeader, object : TypeReference<List<RequestHeader>>() {}),
                it.requestBody,
                it.response,
                it.errorMsg
            )
        }
    }

    fun list(
        userId: String,
        projectId: String,
        startTime: Long?,
        endTime: Long?,
        offset: Int,
        limit: Int
    ): SQLPage<ProjectPipelineCallBackHistory> {
        val count = projectPipelineCallbackHistoryDao.count(dslContext, projectId, startTime, endTime)
        val records = projectPipelineCallbackHistoryDao.list(dslContext, projectId, startTime, endTime, offset, limit)
        return SQLPage(
            count,
            records.map {
                ProjectPipelineCallBackHistory(
                    it.id,
                    it.projectId,
                    it.callbackUrl,
                    it.events,
                    it.status,
                    it.createdTime.timestamp(),
                    JsonUtil.to(it.requestHeader, object : TypeReference<List<RequestHeader>>() {}),
                    it.requestBody,
                    it.response,
                    it.errorMsg
                )
            }
        )
    }

    fun retry(
        userId: String,
        projectId: String,
        id: Long
    ) {
        val record = get(userId, projectId, id) ?: throw ErrorCodeException(
            errorCode = ERROR_CALLBACK_HISTORY_NOT_FOUND,
            defaultMessage = "重试的回调历史记录($id)不存在",
            params = arrayOf(id.toString())
        )

        val requestBuilder = Request.Builder()
            .url(record.callBackUrl)
            .post(RequestBody.create(JSON, record.requestBody))
        record.requestHeader.forEach {
            requestBuilder.addHeader(it.name, it.value)
        }
        val request = requestBuilder.build()

        OkhttpUtils.doHttp(request).use { response ->
            if (response.code() != 200) {
                logger.warn("[${record.projectId}]|CALL_BACK|url=${record.callBackUrl}| code=${response.code()}")
                throw ErrorCodeException(
                    statusCode = response.code(),
                    errorCode = ERROR_CALLBACK_REPLY_FAIL,
                    defaultMessage = "回调重试失败"
                )
            } else {
                logger.info("[${record.projectId}]|CALL_BACK|url=${record.callBackUrl}| code=${response.code()}")
            }
        }
    }
}