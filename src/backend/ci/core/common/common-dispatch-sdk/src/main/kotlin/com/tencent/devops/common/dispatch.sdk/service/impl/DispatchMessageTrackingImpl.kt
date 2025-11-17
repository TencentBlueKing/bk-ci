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

package com.tencent.devops.common.dispatch.sdk.service.impl

import com.tencent.devops.common.client.Client
import com.tencent.devops.dispatch.pojo.dto.DispatchMessageTrackingRecord
import com.tencent.devops.dispatch.pojo.dto.InitMessageTrackingRequest
import com.tencent.devops.dispatch.pojo.dto.UpdateMessageStatusRequest
import com.tencent.devops.common.dispatch.sdk.service.DispatchMessageTracking
import com.tencent.devops.dispatch.api.ServiceDispatchMessageTrackingResource
import com.tencent.devops.dispatch.pojo.DispatchMessageStatus
import com.tencent.devops.process.pojo.mq.PipelineAgentStartupEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * Dispatch 消息追踪实现
 */
@Component
class DispatchMessageTrackingImpl @Autowired constructor(
    private val client: Client
) : DispatchMessageTracking {

    companion object {
        private val logger = LoggerFactory.getLogger(DispatchMessageTrackingImpl::class.java)
    }

    override fun initMessageTracking(
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

    override fun updateMessageStatus(
        buildId: String,
        vmSeqId: String,
        executeCount: Int,
        newStatus: DispatchMessageStatus,
        statusMsg: String?,
        errorCode: String?,
        errorMessage: String?,
        errorType: String?,
        operator: String?,
        remark: String?
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

    override fun trackConsumeFailed(
        buildId: String,
        vmSeqId: String,
        executeCount: Int,
        errorCode: String,
        errorMessage: String,
        errorType: String,
        operator: String?
    ) = updateMessageStatus(
        buildId, vmSeqId, executeCount,
        DispatchMessageStatus.CONSUME_FAILED,
        errorCode = errorCode,
        errorMessage = errorMessage,
        errorType = errorType,
        operator = operator,
        remark = "Message consumption failed: $errorMessage"
    )

    override fun trackResourceQueue(
        buildId: String,
        vmSeqId: String,
        executeCount: Int,
        operator: String?
    ) = updateMessageStatus(
        buildId, vmSeqId, executeCount,
        DispatchMessageStatus.RESOURCE_QUEUE,
        operator = operator,
        remark = "Queuing for resources"
    )

    override fun trackQuotaQueue(
        buildId: String,
        vmSeqId: String,
        executeCount: Int,
        operator: String?
    ) = updateMessageStatus(
        buildId, vmSeqId, executeCount,
        DispatchMessageStatus.QUOTA_QUEUE,
        operator = operator
    )

    override fun trackQuotaInSufficient(
        buildId: String,
        vmSeqId: String,
        executeCount: Int,
        operator: String?
    ) = updateMessageStatus(
        buildId, vmSeqId, executeCount,
        DispatchMessageStatus.QUOTA_INSUFFICIENT,
        operator = operator
    )

    override fun trackResourcePreparing(
        buildId: String,
        vmSeqId: String,
        executeCount: Int,
        operator: String?
    ) = updateMessageStatus(
        buildId = buildId,
        vmSeqId = vmSeqId,
        executeCount = executeCount,
        newStatus = DispatchMessageStatus.RESOURCE_PREPARING,
        operator = operator
    )

    override fun trackResourcePreparingFailed(
        buildId: String,
        vmSeqId: String,
        executeCount: Int,
        operator: String?
    ) = updateMessageStatus(
        buildId = buildId,
        vmSeqId = vmSeqId,
        executeCount = executeCount,
        newStatus = DispatchMessageStatus.RESOURCE_PREPARE_FAILED,
        operator = operator
    )

    override fun trackResourceDelivering(
        buildId: String,
        vmSeqId: String,
        executeCount: Int,
        operator: String?
    ) = updateMessageStatus(
        buildId = buildId,
        vmSeqId = vmSeqId,
        executeCount = executeCount,
        newStatus = DispatchMessageStatus.RESOURCE_DELIVERING,
        operator = operator
    )

    override fun trackResourceDeliveringFailed(
        buildId: String,
        vmSeqId: String,
        executeCount: Int,
        operator: String?
    ) = updateMessageStatus(
        buildId = buildId,
        vmSeqId = vmSeqId,
        executeCount = executeCount,
        newStatus = DispatchMessageStatus.RESOURCE_DELIVERY_FAILED,
        operator = operator
    )

    override fun trackResourceReady(
        buildId: String,
        vmSeqId: String,
        executeCount: Int,
        operator: String?
    ) = updateMessageStatus(
        buildId = buildId,
        vmSeqId = vmSeqId,
        executeCount = executeCount,
        newStatus = DispatchMessageStatus.RESOURCE_READY,
        operator = operator
    )

    override fun trackConsumeSuccess(
        buildId: String,
        vmSeqId: String,
        executeCount: Int,
        operator: String?
    ) = updateMessageStatus(
        buildId = buildId,
        vmSeqId = vmSeqId,
        executeCount = executeCount,
        newStatus = DispatchMessageStatus.CONSUME_SUCCESS,
        operator = operator,
        remark = "Message consumption completed successfully"
    )

    override fun getMessageTrackingRecord(
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

