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

package com.tencent.devops.common.pipeline.enums

/**
 * [statusName] 状态中文名
 * [visible] 是否对用户可见
 */
@Suppress("TooManyFunctions")
enum class BuildStatus(val statusName: String, val visible: Boolean) {
    SUCCEED("成功", true), // 0 成功（最终态）
    FAILED("失败", true), // 1 失败（最终态）
    CANCELED("取消", true), // 2 取消（最终态）
    RUNNING("运行中", true), // 3 运行中（中间状态）
    TERMINATE("终止", true), // 4 终止（Task最终态）待作废
    REVIEWING("审核中", true), // 5 审核中（Task中间状态）
    REVIEW_ABORT("审核驳回", true), // 6 审核驳回（Task最终态）
    REVIEW_PROCESSED("审核通过", true), // 7 审核通过（Task最终态）
    HEARTBEAT_TIMEOUT("心跳超时", true), // 8 心跳超时（最终态）
    PREPARE_ENV("准备环境中", true), // 9 准备环境中（中间状态）
    UNEXEC("从未执行", false), // 10 从未执行（最终态）
    SKIP("跳过", true), // 11 跳过（最终态）
    QUALITY_CHECK_FAIL("质量红线检查失败", true), // 12 质量红线检查失败（最终态）
    QUEUE("排队", true), // 13 排队（初始状态）
    LOOP_WAITING("轮循等待", true), // 14 轮循等待中 互斥组抢锁轮循 （中间状态）
    CALL_WAITING("等待回调", true), // 15 等待回调 用于启动构建环境插件等待构建机回调启动结果（中间状态）
    TRY_FINALLY("补偿任务", false), // 16 不可见的后台状态（未使用）
    QUEUE_TIMEOUT("排队超时", true), // 17 排队超时（最终态）
    EXEC_TIMEOUT("执行超时", true), // 18 执行超时（最终态）
    QUEUE_CACHE("队列待处理", true), // 19 队列待处理，瞬态。只在启动和取消过程中存在（中间状态）
    RETRY("重试", true), // 20 重试（中间状态）
    PAUSE("暂停执行", true), // 21 暂停执行，等待事件 （Stage/Job/Task中间态）
    STAGE_SUCCESS("阶段性完成", true), // 22 当Stage人工审核取消运行时，成功（Stage/Pipeline最终态）
    QUOTA_FAILED("配额不够失败", true), // 23 失败 (未使用）
    DEPENDENT_WAITING("依赖等待", true), // 24 依赖等待 等待依赖的job完成才会进入准备环境（Job中间态）
    QUALITY_CHECK_PASS("质量红线检查通过", true), // 25 质量红线检查通过（准入准出中间态）
    QUALITY_CHECK_WAIT("质量红线等待把关", true), // 26 质量红线等待把关（准入准出中间态）
    TRIGGER_REVIEWING("构建触发待审核", true), // 27 构建触发待审核（入队列前中间态）
    UNKNOWN("未知状态", false); // 99

    fun isNeverRun(): Boolean = this == UNEXEC || this == TRIGGER_REVIEWING

    fun isFinish(): Boolean = isFailure() || isSuccess() || isCancel()

    fun isFailure(): Boolean = this == FAILED || isPassiveStop() || isTimeout() || this == QUOTA_FAILED

    fun isSuccess(): Boolean = this == SUCCEED || this == SKIP || this == REVIEW_PROCESSED || this == QUALITY_CHECK_PASS

    fun isCancel(): Boolean = this == CANCELED

    fun isSkip(): Boolean = this == SKIP

    fun isRunning(): Boolean =
        this == RUNNING ||
            this == LOOP_WAITING ||
            this == REVIEWING ||
            this == PREPARE_ENV ||
            this == CALL_WAITING ||
            this == PAUSE

    fun isReview(): Boolean = this == REVIEW_ABORT || this == REVIEW_PROCESSED

    fun isReadyToRun(): Boolean = this == QUEUE || this == QUEUE_CACHE || this == RETRY

    fun isPassiveStop(): Boolean = this == TERMINATE || this == REVIEW_ABORT || this == QUALITY_CHECK_FAIL

    fun isPause(): Boolean = this == PAUSE

    fun isTimeout(): Boolean = this == QUEUE_TIMEOUT || this == EXEC_TIMEOUT || this == HEARTBEAT_TIMEOUT

    companion object {

        fun parse(statusName: String?): BuildStatus {
            return try {
                if (statusName == null) UNKNOWN else valueOf(statusName)
            } catch (ignored: Exception) {
                UNKNOWN
            }
        }
        @Deprecated(replaceWith = ReplaceWith("isFailure"), message = "replace by isFailure")
        fun isFailure(status: BuildStatus) = status.isFailure()
        @Deprecated(replaceWith = ReplaceWith("isFinish"), message = "replace by isFinish")
        fun isFinish(status: BuildStatus) = status.isFinish()
        @Deprecated(replaceWith = ReplaceWith("isSuccess"), message = "replace by isSuccess")
        fun isSuccess(status: BuildStatus) = status.isSuccess()
        @Deprecated(replaceWith = ReplaceWith("isRunning"), message = "replace by isRunning")
        fun isRunning(status: BuildStatus) = status.isRunning()
        @Deprecated(replaceWith = ReplaceWith("isCancel"), message = "replace by isCancel")
        fun isCancel(status: BuildStatus) = status.isCancel()
        @Deprecated(replaceWith = ReplaceWith("isReadyToRun"), message = "replace by isReadyToRun")
        fun isReadyToRun(status: BuildStatus) = status.isReadyToRun()
    }
}
