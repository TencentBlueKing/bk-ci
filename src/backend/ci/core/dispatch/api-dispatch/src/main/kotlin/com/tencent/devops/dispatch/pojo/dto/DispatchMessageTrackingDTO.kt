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

package com.tencent.devops.dispatch.pojo.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 初始化消息追踪请求
 */
@Schema(title = "初始化消息追踪请求")
data class InitMessageTrackingRequest(
    @get:Schema(title = "项目ID", required = true)
    val projectId: String,
    @get:Schema(title = "流水线ID", required = true)
    val pipelineId: String,
    @get:Schema(title = "构建ID", required = true)
    val buildId: String,
    @get:Schema(title = "VM序列ID", required = true)
    val vmSeqId: Int,
    @get:Schema(title = "执行次数", required = true)
    val executeCount: Int,
    @get:Schema(title = "Dispatch类型", required = true)
    val dispatchType: String
)

/**
 * 更新消息状态请求
 */
@Schema(title = "更新消息状态请求")
data class UpdateMessageStatusRequest(
    @get:Schema(title = "构建ID", required = true)
    val buildId: String,
    @get:Schema(title = "VM序列ID", required = true)
    val vmSeqId: Int,
    @get:Schema(title = "执行次数", required = true)
    val executeCount: Int,
    @get:Schema(title = "新状态", required = true)
    val newStatus: Int,
    @get:Schema(title = "状态描述")
    val statusMsg: String? = null,
    @get:Schema(title = "错误码")
    val errorCode: String? = null,
    @get:Schema(title = "错误信息")
    val errorMessage: String? = null,
    @get:Schema(title = "错误类型")
    val errorType: String? = null,
    @get:Schema(title = "操作来源")
    val operator: String? = null,
    @get:Schema(title = "备注")
    val remark: String? = null
)

/**
 * 更新性能指标请求
 */
@Schema(title = "更新性能指标请求")
data class UpdatePerformanceRequest(
    @get:Schema(title = "构建ID", required = true)
    val buildId: String,
    @get:Schema(title = "VM序列ID", required = true)
    val vmSeqId: Int,
    @get:Schema(title = "执行次数", required = true)
    val executeCount: Int,
    @get:Schema(title = "队列耗时（毫秒）")
    val queueTimeCost: Long? = null,
    @get:Schema(title = "资源准备耗时（毫秒）")
    val resourcePrepareTimeCost: Long? = null
)

/**
 * 消息追踪记录响应
 */
@Schema(title = "消息追踪记录响应")
data class DispatchMessageTrackingRecord(
    @get:Schema(title = "项目ID")
    val projectId: String,
    @get:Schema(title = "流水线ID")
    val pipelineId: String,
    @get:Schema(title = "构建ID")
    val buildId: String,
    @get:Schema(title = "VM序列ID")
    val vmSeqId: Int,
    @get:Schema(title = "执行次数")
    val executeCount: Int,
    @get:Schema(title = "Dispatch类型")
    val dispatchType: String,
    @get:Schema(title = "消费状态")
    val consumeStatus: Int,
    @get:Schema(title = "状态描述")
    val consumeStatusMsg: String? = null,
    @get:Schema(title = "错误码")
    val errorCode: String? = null,
    @get:Schema(title = "错误信息")
    val errorMessage: String? = null,
    @get:Schema(title = "错误类型")
    val errorType: String? = null,
    @get:Schema(title = "重试次数")
    val retryCount: Int,
    @get:Schema(title = "队列耗时（毫秒）")
    val queueTimeCost: Long? = null,
    @get:Schema(title = "资源准备耗时（毫秒）")
    val resourcePrepareTimeCost: Long? = null,
    @get:Schema(title = "总耗时（毫秒）")
    val totalTimeCost: Long? = null,
    @get:Schema(title = "开始时间")
    val startTime: String? = null,
    @get:Schema(title = "结束时间")
    val endTime: String? = null,
    @get:Schema(title = "创建时间")
    val createdTime: String,
    @get:Schema(title = "更新时间")
    val updatedTime: String
)