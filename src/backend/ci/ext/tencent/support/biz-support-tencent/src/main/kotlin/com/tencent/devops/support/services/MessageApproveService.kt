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

package com.tencent.devops.support.services

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.support.model.approval.CompleteMoaWorkItemRequest
import com.tencent.devops.support.model.approval.CreateEsbMoaApproveParam
import com.tencent.devops.support.model.approval.CreateEsbMoaCompleteParam
import com.tencent.devops.support.model.approval.CreateEsbMoaWorkItem
import com.tencent.devops.support.model.approval.CreateMoaApproveRequest
import com.tencent.devops.support.model.approval.MoaWorkItemElement
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Date

@Service
class MessageApproveService @Autowired constructor() {

    @Value("\${gateway.appCode}")
    private lateinit var appCode: String

    @Value("\${gateway.appSecret}")
    private lateinit var appSecret: String

    @Value("\${gateway.urlPrefix}")
    private lateinit var urlPrefix: String

    @Value("\${gateway.moa.completeUrl}")
    private lateinit var moaCompleteUrl: String

    @Value("\${gateway.moa.pushDataUrl}")
    private lateinit var moaPushDataUrl: String

    @Value("\${gateway.moa.pushWorkItemUrl}")
    private lateinit var moaPushWorkItemUrl: String

    @Value("\${gateway.moa.completeWorkItemUrl}")
    private lateinit var moaCompleteWorkItemUrl: String

    fun createMoaMessageApproval(userId: String, createMoaApproveRequest: CreateMoaApproveRequest): Result<Boolean> {
        logger.info("createMoaMessageApproval userId is :$userId, createMoaApproveRequest is :$createMoaApproveRequest")
        val createEsbMoaApproveParam = CreateEsbMoaApproveParam(
            appCode = appCode,
            appSecret = appSecret,
            operator = "DevOps",
            verifier = createMoaApproveRequest.verifier,
            title = createMoaApproveRequest.title,
            taskId = createMoaApproveRequest.taskId,
            startDate = DateTimeUtil.formatDate(Date()),
            backUrl = createMoaApproveRequest.backUrl,
            sysUrl = createMoaApproveRequest.sysUrl
        )
        val requestBody = JsonUtil.toJson(createEsbMoaApproveParam)
        val request = Request.Builder()
            .url(urlPrefix + moaPushDataUrl)
            .post(RequestBody.create("application/json;charset=utf-8".toMediaTypeOrNull(), requestBody))
            .build()
        OkhttpUtils.doHttp(request).use { res ->
            val data = res.body!!.string()
            logger.info("the response>> $data")
            if (!res.isSuccessful) return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.SYSTEM_ERROR)
            val response: Map<String, Any> = JsonUtil.toMap(data)
            val code = response["code"]
            if (code != "00") {
                return Result(status = code.toString().toInt(), message = response["message"] as String) // 发送失败抛出错误提示
            }
        }
        return Result(true)
    }

    fun createMoaWorkItem(moaWorkItemElementList: List<MoaWorkItemElement>): Result<Boolean> {
        val createEsbMoaWorkItem = CreateEsbMoaWorkItem(
            appCode = appCode,
            appSecret = appSecret,
            operator = "DevOps",
            workItems = moaWorkItemElementList
        )
        val requestBody = JsonUtil.toJson(createEsbMoaWorkItem)
        val request = Request.Builder()
            .url(urlPrefix + moaPushWorkItemUrl)
            .post(RequestBody.create("application/json;charset=utf-8".toMediaTypeOrNull(), requestBody))
            .build()
        OkhttpUtils.doHttp(request).use { res ->
            val data = res.body!!.string()
            logger.info("the response>> $data")
            if (!res.isSuccessful) return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.SYSTEM_ERROR)
            val response: Map<String, Any> = JsonUtil.toMap(data)
            val code = response["code"]
            if (code != "00") {
                return Result(status = code.toString().toInt(), message = response["message"] as String) // 发送失败抛出错误提示
            }
        }
        return Result(true)
    }

    fun completeMoaWorkItem(completeMoaWorkItemRequest: CompleteMoaWorkItemRequest): Result<Boolean> {
        val completeEsbMoaWorkItem = CompleteMoaWorkItemRequest(
            appCode = appCode,
            appSecret = appSecret,
            operator = "DevOps",
            activity = completeMoaWorkItemRequest.activity,
            category = completeMoaWorkItemRequest.category,
            handler = completeMoaWorkItemRequest.handler,
            processInstId = completeMoaWorkItemRequest.processInstId,
            processName = completeMoaWorkItemRequest.processName
        )
        val requestBody = JsonUtil.toJson(completeEsbMoaWorkItem)
        val request = Request.Builder()
            .url(urlPrefix + moaCompleteWorkItemUrl)
            .post(RequestBody.create("application/json;charset=utf-8".toMediaTypeOrNull(), requestBody))
            .build()
        OkhttpUtils.doHttp(request).use { res ->
            val data = res.body!!.string()
            logger.info("the response>> $data")
            if (!res.isSuccessful) return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.SYSTEM_ERROR)
            val response: Map<String, Any> = JsonUtil.toMap(data)
            val code = response["code"]
            if (code != "00") {
                return Result(status = code.toString().toInt(), message = response["message"] as String) // 发送失败抛出错误提示
            }
        }
        return Result(true)
    }

    fun moaComplete(taskId: String): Result<Boolean> {
        logger.info("moaComplete taskId is :$taskId")
        val createEsbMoaCompleteParam = CreateEsbMoaCompleteParam(
            appCode = appCode,
            appSecret = appSecret,
            operator = "DevOps",
            taskId = taskId
        )
        val requestBody = JsonUtil.toJson(createEsbMoaCompleteParam)
        val request = Request.Builder()
            .url(urlPrefix + moaCompleteUrl)
            .post(RequestBody.create("application/json;charset=utf-8".toMediaTypeOrNull(), requestBody))
            .build()
        OkhttpUtils.doHttp(request).use { res ->
            val data = res.body!!.string()
            logger.info("the response>> $data")
            if (!res.isSuccessful) return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.SYSTEM_ERROR)
            val response: Map<String, Any> = JsonUtil.toMap(data)
            val code = response["code"]
            if (code != "00") {
                return Result(status = code.toString().toInt(), message = response["message"] as String) // 发送失败抛出错误提示
            }
        }
        return Result(true)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MessageApproveService::class.java)
    }
}
