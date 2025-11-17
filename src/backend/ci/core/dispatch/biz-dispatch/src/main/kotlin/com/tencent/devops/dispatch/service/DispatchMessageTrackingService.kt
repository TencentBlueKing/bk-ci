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

package com.tencent.devops.dispatch.service

import com.tencent.devops.dispatch.dao.DispatchMessageConsumeRecordDao
import com.tencent.devops.dispatch.pojo.DispatchMessageStatus
import com.tencent.devops.dispatch.pojo.dto.DispatchMessageTrackingRecord
import com.tencent.devops.dispatch.pojo.dto.InitMessageTrackingRequest
import com.tencent.devops.dispatch.pojo.dto.UpdateMessageStatusRequest
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter

/**
 * Dispatch 消息追踪服务
 */
@Service
class DispatchMessageTrackingService @Autowired constructor(
    private val dslContext: DSLContext,
    private val messageConsumeRecordDao: DispatchMessageConsumeRecordDao
) {
    companion object {
        private val logger = LoggerFactory.getLogger(DispatchMessageTrackingService::class.java)
    }

    /**
     * 初始化消息追踪
     */
    fun initMessageTracking(request: InitMessageTrackingRequest): Long {
        logger.info("[${request.buildId}|${request.vmSeqId}|${request.executeCount}] " +
            "Init message tracking for module: ${request.dispatchType}")

        return try {
            messageConsumeRecordDao.create(
                dslContext = dslContext,
                projectId = request.projectId,
                pipelineId = request.pipelineId,
                buildId = request.buildId,
                vmSeqId = request.vmSeqId,
                executeCount = request.executeCount,
                dispatchType = request.dispatchType
            )
        } catch (e: Exception) {
            logger.error("[${request.buildId}|${request.vmSeqId}|${request.executeCount}] " +
                "Failed to init message tracking", e)
            throw e
        }
    }

    /**
     * 更新消息状态
     */
    fun updateMessageStatus(request: UpdateMessageStatusRequest): Boolean {
        val newStatus = DispatchMessageStatus.valueOf(request.newStatus)
            ?: throw IllegalArgumentException("Invalid status: ${request.newStatus}")

        logger.info("[${request.buildId}|${request.vmSeqId}|${request.executeCount}] " +
            "Update status to: ${newStatus.statusName}")

        return try {
            val currentRecord = messageConsumeRecordDao.get(
                dslContext, request.buildId, request.vmSeqId, request.executeCount
            )
            val oldStatus = currentRecord?.consumeStatus?.let { DispatchMessageStatus.valueOf(it) }

            val updated = messageConsumeRecordDao.updateStatus(
                dslContext = dslContext,
                buildId = request.buildId,
                vmSeqId = request.vmSeqId,
                executeCount = request.executeCount,
                newStatus = newStatus,
                statusMsg = request.statusMsg,
                errorCode = request.errorCode,
                errorMessage = request.errorMessage,
                errorType = request.errorType
            )

            // 记录状态变更日志
            if (updated && currentRecord != null) {
                val elapsedTime = currentRecord.updatedTime?.let {
                    java.time.Duration.between(it, java.time.LocalDateTime.now()).toMillis()
                }
                
                logger.info(
                    "[${request.buildId}|${request.vmSeqId}|${request.executeCount}] " +
                    "Status changed: [${oldStatus?.statusName ?: "INIT"}] -> [${newStatus.statusName}], " +
                    "elapsed: ${elapsedTime}ms, operator: ${request.operator ?: "system"}, " +
                    "remark: ${request.remark ?: "N/A"}"
                )
                
                // 如果有错误信息，单独记录
                if (request.errorCode != null || request.errorMessage != null) {
                    logger.warn(
                        "[${request.buildId}|${request.vmSeqId}|${request.executeCount}] " +
                        "Error occurred - code: ${request.errorCode}, type: ${request.errorType}, " +
                        "message: ${request.errorMessage}"
                    )
                }
            }

            updated
        } catch (e: Exception) {
            logger.error("[${request.buildId}|${request.vmSeqId}|${request.executeCount}] " +
                "Failed to update status", e)
            false
        }
    }

    /**
     * 查询消息追踪记录
     */
    fun getMessageTrackingRecord(
        buildId: String,
        vmSeqId: Int,
        executeCount: Int
    ): DispatchMessageTrackingRecord? {
        return try {
            val record = messageConsumeRecordDao.get(dslContext, buildId, vmSeqId, executeCount)
                ?: return null

            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

            DispatchMessageTrackingRecord(
                projectId = record.projectId,
                pipelineId = record.pipelineId,
                buildId = record.buildId,
                vmSeqId = record.vmSeqId,
                executeCount = record.executeCount,
                dispatchType = record.dispatchType,
                consumeStatus = record.consumeStatus,
                consumeStatusMsg = DispatchMessageStatus.valueOf(record.consumeStatus)?.description,
                errorCode = record.errorCode,
                errorMessage = record.errorMessage,
                errorType = record.errorType,
                retryCount = record.retryCount,
                queueTimeCost = record.queueTimeCost,
                resourcePrepareTimeCost = record.resourcePrepareTimeCost,
                totalTimeCost = record.totalTimeCost,
                startTime = record.startTime?.format(formatter),
                endTime = record.endTime?.format(formatter),
                createdTime = record.createdTime.format(formatter),
                updatedTime = record.updatedTime.format(formatter)
            )
        } catch (e: Exception) {
            logger.error("[$buildId|$vmSeqId|$executeCount] Failed to get message tracking record", e)
            null
        }
    }
}

