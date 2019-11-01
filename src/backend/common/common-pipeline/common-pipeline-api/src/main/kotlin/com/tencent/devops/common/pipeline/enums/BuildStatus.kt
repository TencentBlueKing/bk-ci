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

package com.tencent.devops.common.pipeline.enums

import com.tencent.devops.common.api.pojo.IdValue

enum class BuildStatus(val statusName: String, val visiable: Boolean) {
    SUCCEED("成功", true), // 0 成功
    FAILED("失败", true), // 1 失败
    CANCELED("取消", true), // 2 取消
    RUNNING("运行中", true), // 3 运行中
    TERMINATE("终止", true), // 4 终止
    REVIEWING("审核中", true), // 5 审核中
    REVIEW_ABORT("审核驳回", true), // 6 审核驳回
    REVIEW_PROCESSED("审核通过", true), // 7 审核通过
    HEARTBEAT_TIMEOUT("心跳超时", true), // 8 心跳超时
    PREPARE_ENV("准备环境中", true), // 9 准备环境中
    UNEXEC("从未执行", false), // 10 从未执行（未使用）
    SKIP("跳过", true), // 11 跳过
    QUALITY_CHECK_FAIL("质量红线检查失败", true), // 12 质量红线检查失败
    QUEUE("排队", true), // 13 排队（新）
    LOOP_WAITING("轮循等待", true), // 14 轮循等待 --运行中状态（新）
    CALL_WAITING("等待回调", true), // 15 等待回调 --运行中状态（新）
    TRY_FINALLY("补偿任务", false), // 16 不可见的后台状态（新）为前面失败的任务做补偿的任务的原子状态，执行中如果前面有失败，则该种状态的任务才会执行。
    QUEUE_TIMEOUT("排队超时", true), // 17 排队超时
    EXEC_TIMEOUT("执行超时", true); // 18 执行超时

    companion object {
        fun isFailure(status: BuildStatus) = status == FAILED || isCancel(status) || isTimeout(status)

        fun isFinish(status: BuildStatus) = isFailure(status) || isSuccess(status)

        fun isSuccess(status: BuildStatus) =
            status == SUCCEED || status == SKIP || status == REVIEW_PROCESSED

        fun isRunning(status: BuildStatus) =
            isLoop(status) || status == REVIEWING || status == PREPARE_ENV || status == CALL_WAITING

        fun isCancel(status: BuildStatus) =
            status == TERMINATE || status == CANCELED || status == REVIEW_ABORT || status == QUALITY_CHECK_FAIL

        fun isReview(status: BuildStatus) = status == REVIEW_ABORT || status == REVIEW_PROCESSED

        fun isReadyToRun(status: BuildStatus) = status == QUEUE
        /**
         * 是否处于循环中： 正在运行中或循环等待都属于循环
         */
        fun isLoop(status: BuildStatus) = status == RUNNING || status == LOOP_WAITING

        /**
         * 能否重试执行
         */
//        fun isCanRetry(status: BuildStatus) = !isCancel(status) && (isFailure(status) || isReadyToRun(status))

        /**
         * 是否是用于补偿失败任务的后置任务状态
         */
//        fun isTryFinally(status: BuildStatus) = status == TRY_FINALLY

        /**
         * 是否超时
         */
        fun isTimeout(status: BuildStatus) =
            status == QUEUE_TIMEOUT || status == EXEC_TIMEOUT || status == HEARTBEAT_TIMEOUT

        /**
         * 获取Status列表
         */
        fun getStatusMap(): List<IdValue> {
            val result = mutableListOf<IdValue>()
            values().filter { it.visiable }.forEach {
                result.add(IdValue(it.name, it.statusName))
            }
            return result
        }
    }
}
