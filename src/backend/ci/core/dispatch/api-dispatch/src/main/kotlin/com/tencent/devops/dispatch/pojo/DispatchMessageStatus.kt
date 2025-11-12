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

package com.tencent.devops.dispatch.pojo

enum class AgentStartDiagnoseStatus {
    UNKNOWN, // 未知（通常是还没被消费到或者是没有这个消息）
    IN_QUEUE, // 队列中， 看要不要细化？
    SCHEDULER_FAILURE,// 构建机调度失败
    DELIVERING, // 构建机交付中
    SUCCESS, // 构建机交付成功
    FAILURE, // 构建机交付失败
}


/**
 * Dispatch 消息消费通用状态
 */
enum class DispatchMessageStatus(
    val status: Int,
    val statusName: String,
    val description: String,
    val category: StatusCategory
) {
    // ========== 基础状态 (0-99) ==========
    NOT_RECEIVED(0, "未收到消息", "Message not received", StatusCategory.INITIAL),
    MESSAGE_RECEIVED(1, "消息已接收", "Message received", StatusCategory.PROCESSING),
    CONSUME_FAILED(2, "消息处理失败", "Message consumption failed", StatusCategory.FAILED),
    CONSUME_SUCCESS(3, "消息处理成功", "Message consumption succeeded", StatusCategory.SUCCESS),

    // ========== 排队相关状态 (100-199) ==========
    RESOURCE_QUEUE(100, "排队等待资源", "Queuing for resources", StatusCategory.PROCESSING),
    QUOTA_QUEUE(101, "排队等待配额", "Queuing for quota", StatusCategory.PROCESSING),
    QUOTA_INSUFFICIENT(102, "配额不足", "Quota insufficient", StatusCategory.FAILED),

    // ========== 资源准备状态 (200-299) ==========
    RESOURCE_PREPARING(200, "资源准备中", "Resource preparing", StatusCategory.PROCESSING),
    RESOURCE_DELIVERING(203, "资源交付中", "Resource delivering", StatusCategory.PROCESSING),
    RESOURCE_READY(204, "资源就绪", "Resource ready", StatusCategory.SUCCESS),
    RESOURCE_PREPARE_FAILED(205, "资源准备失败", "Resource preparation failed", StatusCategory.FAILED),
    RESOURCE_DELIVERY_FAILED(208, "资源交付失败", "Resource delivery failed", StatusCategory.FAILED),

    // ========== 构建执行状态 (300-399) ==========
    AGENT_CONNECTING(300, "Agent连接中", "Agent connecting", StatusCategory.PROCESSING),
    AGENT_CONNECTED(301, "Agent已连接", "Agent connected", StatusCategory.SUCCESS),
    AGENT_CONNECT_TIMEOUT(302, "Agent连接超时", "Agent connection timeout", StatusCategory.FAILED),
    BUILD_RUNNING(303, "构建执行中", "Build running", StatusCategory.PROCESSING),

    // ========== 清理状态 (400-499) ==========
    RESOURCE_CLEANING(400, "资源清理中", "Resource cleaning", StatusCategory.PROCESSING),
    RESOURCE_CLEANED(401, "资源已清理", "Resource cleaned", StatusCategory.SUCCESS),
    RESOURCE_CLEAN_FAILED(402, "资源清理失败", "Resource clean failed", StatusCategory.FAILED);

    enum class StatusCategory {
        INITIAL,      // 初始状态
        PROCESSING,   // 处理中
        SUCCESS,      // 成功
        FAILED        // 失败
    }

    companion object {
        fun valueOf(status: Int): DispatchMessageStatus? {
            return values().find { it.status == status }
        }

        fun isFinalStatus(status: DispatchMessageStatus): Boolean {
            return status.category == StatusCategory.SUCCESS ||
                   status.category == StatusCategory.FAILED
        }

        fun isSuccessStatus(status: DispatchMessageStatus): Boolean {
            return status.category == StatusCategory.SUCCESS
        }

        fun isFailedStatus(status: DispatchMessageStatus): Boolean {
            return status.category == StatusCategory.FAILED
        }

        fun isProcessingStatus(status: DispatchMessageStatus): Boolean {
            return status.category == StatusCategory.PROCESSING
        }
    }
}

