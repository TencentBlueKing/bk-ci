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

import com.tencent.devops.common.client.Client
import com.tencent.devops.dispatch.pojo.dto.DispatchMessageTrackingRecord
import com.tencent.devops.dispatch.pojo.dto.InitMessageTrackingRequest
import com.tencent.devops.dispatch.pojo.dto.UpdateMessageStatusRequest
import com.tencent.devops.dispatch.api.ServiceDispatchMessageTrackingResource
import com.tencent.devops.dispatch.pojo.DispatchMessageStatus
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * Dispatch 消息追踪实现
 */
@Component
class DispatchMessageTracking @Autowired constructor(
    private val client: Client
) {

    companion object {
        private val logger = LoggerFactory.getLogger(DispatchMessageTracking::class.java)
    }

    fun initMessageTracking(
        initMessageTrackingRequest: InitMessageTrackingRequest
    ): Long {
        return try {
            client.get(ServiceDispatchMessageTrackingResource::class)
                .initMessageTracking(initMessageTrackingRequest).data ?: 0L
        } catch (e: Exception) {
            logger.warn(
                "[${initMessageTrackingRequest.buildId}|${initMessageTrackingRequest.vmSeqId}] " +
                        "Failed to init message tracking",
                e
            )
            0L
        }
    }

    fun updateMessageStatus(
        buildId: String,
        vmSeqId: String,
        executeCount: Int,
        newStatus: DispatchMessageStatus,
        statusMsg: String?,
        errorCode: String? = "",
        errorMessage: String? = "",
        errorType: String? = "",
        operator: String? = "",
        remark: String? = ""
    ): Boolean {
        return try {
            val request = UpdateMessageStatusRequest(
                buildId = buildId,
                vmSeqId = vmSeqId.toInt(),
                executeCount = executeCount,
                newStatus = newStatus.status,
                statusMsg = statusMsg,
                errorCode = errorCode,
                errorMessage = errorMessage,
                errorType = errorType,
                operator = operator,
                remark = remark
            )
            client.get(ServiceDispatchMessageTrackingResource::class).updateMessageStatus(request).data ?: false
        } catch (e: Exception) {
            logger.warn("[$buildId|$vmSeqId] Failed to update message status", e)
            false
        }
    }

    fun trackConsumeFailed(
        buildId: String,
        vmSeqId: String,
        executeCount: Int,
        errorCode: String,
        errorMessage: String,
        errorType: String,
        operator: String? = ""
    ) = updateMessageStatus(
        buildId = buildId,
        vmSeqId = vmSeqId,
        executeCount = executeCount,
        newStatus = DispatchMessageStatus.CONSUME_FAILED,
        statusMsg = DispatchMessageStatus.CONSUME_SUCCESS.description,
        errorCode = errorCode,
        errorMessage = errorMessage,
        errorType = errorType,
        operator = operator,
        remark = "Message consumption failed: $errorMessage"
    )

    fun trackResourceQueue(
        buildId: String,
        vmSeqId: String,
        executeCount: Int,
        operator: String? = ""
    ) = updateMessageStatus(
        buildId = buildId,
        vmSeqId = vmSeqId,
        executeCount = executeCount,
        newStatus = DispatchMessageStatus.RESOURCE_QUEUE,
        statusMsg = DispatchMessageStatus.RESOURCE_QUEUE.description,
        operator = operator,
        remark = "Queuing for resources"
    )

    fun trackQuotaQueue(
        buildId: String,
        vmSeqId: String,
        executeCount: Int,
        operator: String? = ""
    ) = updateMessageStatus(
        buildId = buildId,
        vmSeqId = vmSeqId,
        executeCount = executeCount,
        newStatus = DispatchMessageStatus.QUOTA_QUEUE,
        statusMsg = DispatchMessageStatus.QUOTA_QUEUE.description,
        operator = operator
    )

    fun trackQuotaInSufficient(
        buildId: String,
        vmSeqId: String,
        executeCount: Int,
        operator: String? = ""
    ) = updateMessageStatus(
        buildId = buildId,
        vmSeqId = vmSeqId,
        executeCount = executeCount,
        newStatus = DispatchMessageStatus.QUOTA_INSUFFICIENT,
        statusMsg = DispatchMessageStatus.QUOTA_INSUFFICIENT.description,
        operator = operator
    )

    fun trackResourcePreparing(
        buildId: String,
        vmSeqId: String,
        executeCount: Int,
        operator: String? = ""
    ) = updateMessageStatus(
        buildId = buildId,
        vmSeqId = vmSeqId,
        executeCount = executeCount,
        newStatus = DispatchMessageStatus.RESOURCE_PREPARING,
        statusMsg = DispatchMessageStatus.RESOURCE_PREPARING.description,
        operator = operator
    )

    fun trackResourcePreparingFailed(
        buildId: String,
        vmSeqId: String,
        executeCount: Int,
        operator: String? = ""
    ) = updateMessageStatus(
        buildId = buildId,
        vmSeqId = vmSeqId,
        executeCount = executeCount,
        newStatus = DispatchMessageStatus.RESOURCE_PREPARE_FAILED,
        statusMsg = DispatchMessageStatus.RESOURCE_PREPARE_FAILED.description,
        operator = operator
    )

    fun trackResourceDelivering(
        buildId: String,
        vmSeqId: String,
        executeCount: Int,
        operator: String? = ""
    ) = updateMessageStatus(
        buildId = buildId,
        vmSeqId = vmSeqId,
        executeCount = executeCount,
        newStatus = DispatchMessageStatus.RESOURCE_DELIVERING,
        statusMsg = DispatchMessageStatus.RESOURCE_DELIVERING.description,
        operator = operator
    )

    fun trackResourceDeliveringFailed(
        buildId: String,
        vmSeqId: String,
        executeCount: Int,
        operator: String? = ""
    ) = updateMessageStatus(
        buildId = buildId,
        vmSeqId = vmSeqId,
        executeCount = executeCount,
        newStatus = DispatchMessageStatus.RESOURCE_DELIVERY_FAILED,
        statusMsg = DispatchMessageStatus.RESOURCE_DELIVERY_FAILED.description,
        operator = operator
    )

    fun trackResourceReady(
        buildId: String,
        vmSeqId: String,
        executeCount: Int,
        operator: String? = ""
    ) = updateMessageStatus(
        buildId = buildId,
        vmSeqId = vmSeqId,
        executeCount = executeCount,
        newStatus = DispatchMessageStatus.RESOURCE_READY,
        statusMsg = DispatchMessageStatus.RESOURCE_READY.description,
        operator = operator
    )

    fun trackConsumeSuccess(
        buildId: String,
        vmSeqId: String,
        executeCount: Int,
        operator: String? = ""
    ) = updateMessageStatus(
        buildId = buildId,
        vmSeqId = vmSeqId,
        executeCount = executeCount,
        newStatus = DispatchMessageStatus.CONSUME_SUCCESS,
        statusMsg = DispatchMessageStatus.CONSUME_SUCCESS.description,
        operator = operator,
        remark = "Message consumption completed successfully"
    )

    fun getMessageTrackingRecord(
        buildId: String,
        vmSeqId: String,
        executeCount: Int
    ): DispatchMessageTrackingRecord? {
        return try {
            val result = client.get(ServiceDispatchMessageTrackingResource::class).getMessageTrackingRecord(
                buildId = buildId,
                vmSeqId = vmSeqId.toInt(),
                executeCount = executeCount
            ).data
            result
        } catch (e: Exception) {
            logger.error("[$buildId|$vmSeqId|$executeCount] Failed to get message tracking record", e)
            null
        }
    }
}