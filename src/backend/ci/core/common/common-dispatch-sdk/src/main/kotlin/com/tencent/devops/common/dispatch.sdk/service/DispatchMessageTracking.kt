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

package com.tencent.devops.common.dispatch.sdk.service

import com.tencent.devops.dispatch.pojo.dto.DispatchMessageTrackingRecord
import com.tencent.devops.dispatch.pojo.DispatchMessageStatus
import com.tencent.devops.dispatch.pojo.dto.InitMessageTrackingRequest
import com.tencent.devops.process.pojo.mq.PipelineAgentStartupEvent

/**
 * Dispatch 消息追踪 Client 接口
 * 各 dispatch 模块通过此 Client 调用追踪服务
 */
interface DispatchMessageTracking {

    /**
     * 初始化消息追踪
     */
    fun initMessageTracking(
        initMessageTrackingRequest: InitMessageTrackingRequest
    ): Long

    /**
     * 更新消息状态
     */
    fun updateMessageStatus(
        buildId: String,
        vmSeqId: String,
        executeCount: Int,
        newStatus: DispatchMessageStatus,
        statusMsg: String? = null,
        errorCode: String? = null,
        errorMessage: String? = null,
        errorType: String? = null,
        operator: String? = null,
        remark: String? = null
    ): Boolean

    // ========== 便捷方法 ==========

    /**
     * 追踪消息处理失败
     */
    fun trackConsumeFailed(
        buildId: String,
        vmSeqId: String,
        executeCount: Int,
        errorCode: String,
        errorMessage: String,
        errorType: String = "SYSTEM",
        operator: String? = null
    ): Boolean

    /**
     * 追踪消息处理成功
     */
    fun trackConsumeSuccess(
        buildId: String,
        vmSeqId: String,
        executeCount: Int,
        operator: String? = null
    ): Boolean

    /**
     * 追踪排队等待
     */
    fun trackResourceQueue(
        buildId: String,
        vmSeqId: String,
        executeCount: Int,
        operator: String? = null
    ): Boolean

    /**
     * 追踪配额排队
     */
    fun trackQuotaQueue(
        buildId: String,
        vmSeqId: String,
        executeCount: Int,
        operator: String? = null
    ): Boolean

    /**
     * 追踪配额不足导致的失败
     */
    fun trackQuotaInSufficient(
        buildId: String,
        vmSeqId: String,
        executeCount: Int,
        operator: String? = null
    ): Boolean

    /**
     * 追踪资源准备中
     */
    fun trackResourcePreparing(
        buildId: String,
        vmSeqId: String,
        executeCount: Int,
        operator: String? = null
    ): Boolean

    /**
     * 追踪资源准备失败
     */
    fun trackResourcePreparingFailed(
        buildId: String,
        vmSeqId: String,
        executeCount: Int,
        operator: String? = null
    ): Boolean

    /**
     * 追踪资源交付中
     */
    fun trackResourceDelivering(
        buildId: String,
        vmSeqId: String,
        executeCount: Int,
        operator: String? = null
    ): Boolean

    /**
     * 追踪资源交付失败
     */
    fun trackResourceDeliveringFailed(
        buildId: String,
        vmSeqId: String,
        executeCount: Int,
        operator: String? = null
    ): Boolean

    /**
     * 追踪资源就绪
     */
    fun trackResourceReady(
        buildId: String,
        vmSeqId: String,
        executeCount: Int,
        operator: String? = null
    ): Boolean

    /**
     * 查询消息追踪记录
     */
    fun getMessageTrackingRecord(
        buildId: String,
        vmSeqId: String,
        executeCount: Int
    ): DispatchMessageTrackingRecord?
}

