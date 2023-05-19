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

package com.tencent.devops.common.pipeline.utils

import com.tencent.devops.common.pipeline.enums.BuildStatus

/**
 * 对[BuildStatus]进行状态间切换逻辑处理
 * 比如对A状态进行取消，应该切换成B状态等等
 */
object BuildStatusSwitcher {

    /**
     * 如果 buildStatus 为结束态：
     *  则返回[BuildStatus.UNEXEC]
     * 否则
     *  返回[BuildStatus.SKIP] 切换成为跳过
     */
    fun readyToSkipWhen(buildStatus: BuildStatus): BuildStatus {
        return if (buildStatus.isFailure() || buildStatus.isCancel()) {
            BuildStatus.UNEXEC
        } else {
            BuildStatus.SKIP
        }
    }

    val pipelineStatusMaker = PipelineBuildStatusMaker()

    val stageStatusMaker = StageBuildStatusMaker()

    val jobStatusMaker = JobBuildStatusMaker()

    val taskStatusMaker = TaskBuildStatusMaker()

    interface BuildStatusMaker {

        fun statusSet(): Set<BuildStatus>

        /**
         * 取消构建时，[currentBuildStatus]应该切换成什么状态
         * 当[BuildStatus.isReadyToRun]准备执行状态，则直接设置为[BuildStatus.CANCELED]
         * 当[BuildStatus.isRunning]执行中状态，则直接设置为[BuildStatus.CANCELED]
         * 当[BuildStatus.isFinish]结束状态，则直接原样返回[currentBuildStatus]
         * 其他未列入状态，暂定[BuildStatus.CANCELED]
         */
        fun cancel(currentBuildStatus: BuildStatus): BuildStatus {
            val canceled = BuildStatus.CANCELED
            return when {
                currentBuildStatus == BuildStatus.UNKNOWN -> canceled
                currentBuildStatus.isReadyToRun() -> canceled
                currentBuildStatus.isRunning() -> canceled
                currentBuildStatus.isFinish() -> if (statusSet().contains(currentBuildStatus)) {
                    currentBuildStatus
                } else {
                    canceled
                }

                else -> canceled // 其他状态暂定
            }
        }

        /**
         * 正常结束构建时，[currentBuildStatus]应该切换成什么状态。
         * 如已经结束，则直接返回[currentBuildStatus],否则返回[BuildStatus.SUCCEED]
         */
        fun finish(currentBuildStatus: BuildStatus): BuildStatus {
            return if (currentBuildStatus.isFinish() || currentBuildStatus == BuildStatus.STAGE_SUCCESS) {
                if (statusSet().contains(currentBuildStatus)) { // 在定义范围内的最终态直接返回
                    currentBuildStatus
                } else {
                    if (currentBuildStatus.isFailure()) { // 失败类统一收敛为FAILED状态
                        BuildStatus.FAILED
                    } else {
                        BuildStatus.SUCCEED
                    }
                }
            } else if (currentBuildStatus.isReadyToRun()) { // 排队状态->取消
                BuildStatus.CANCELED
            } else { // 运行中及其它状态 -> 成功
                BuildStatus.SUCCEED
            }
        }

        /**
         * 强制结束构建时，[currentBuildStatus]应该切换成什么状态。
         * 如已经结束并且[fastKill]=false，则直接返回[currentBuildStatus], 否则返回[BuildStatus.FAILED]，
         *
         */
        fun forceFinish(currentBuildStatus: BuildStatus, fastKill: Boolean = false): BuildStatus {
            return if (currentBuildStatus.isFinish() || currentBuildStatus == BuildStatus.STAGE_SUCCESS) {
                if (statusSet().contains(currentBuildStatus) && !fastKill) { // 在定义范围内并且fastKill=false直接返回
                    currentBuildStatus
                } else {
                    if (currentBuildStatus.isSuccess()) {
                        BuildStatus.SUCCEED
                    } else { // 失败类统一收敛为FAILED状态
                        BuildStatus.FAILED
                    }
                }
            } else { // 运行中及其它状态 -> 失败
                BuildStatus.FAILED
            }
        }

        fun switchByErrorCode(currentBuildStatus: BuildStatus, errorCode: Int?): BuildStatus = currentBuildStatus
    }

    class TaskBuildStatusMaker : BuildStatusMaker {

        companion object {
            private val timeoutCodeSet = setOf(2103006)
        }

        /**
         * 强制结束构建时，[currentBuildStatus]如果是[BuildStatus.isRunning]状态并且[fastKill] = false, 则为[BuildStatus.TERMINATE]
         * 如已经结束，则直接返回[currentBuildStatus],否则返回[BuildStatus.FAILED]
         */
        override fun forceFinish(currentBuildStatus: BuildStatus, fastKill: Boolean): BuildStatus {
            return if (currentBuildStatus.isFinish()) {
                if (statusSet().contains(currentBuildStatus)) { // 在定义范围内的最终态直接返回
                    currentBuildStatus
                } else {
                    if (currentBuildStatus.isSuccess()) {
                        BuildStatus.SUCCEED
                    } else { // 失败类统一收敛为FAILED状态
                        BuildStatus.FAILED
                    }
                }
            } else if (currentBuildStatus.isRunning() && !fastKill) {
                BuildStatus.TERMINATE // 运行中 -> 终止
            } else { // 其它状态 -> 失败
                BuildStatus.FAILED
            }
        }

        override fun switchByErrorCode(currentBuildStatus: BuildStatus, errorCode: Int?): BuildStatus {
            if (timeoutCodeSet.contains(errorCode)) {
                return BuildStatus.QUEUE_TIMEOUT
            }
            return currentBuildStatus
        }

        private val pipelineStatus = setOf(
            BuildStatus.QUEUE,
            BuildStatus.QUEUE_CACHE,
            BuildStatus.RETRY,
            BuildStatus.RUNNING,
            BuildStatus.CALL_WAITING,
            BuildStatus.REVIEWING,
            BuildStatus.REVIEW_ABORT,
            BuildStatus.REVIEW_PROCESSED,
            BuildStatus.PAUSE,
            BuildStatus.CANCELED,
            BuildStatus.SUCCEED,
            BuildStatus.FAILED,
            BuildStatus.TERMINATE,
            BuildStatus.SKIP,
            BuildStatus.UNEXEC,
            BuildStatus.QUEUE_TIMEOUT,
            BuildStatus.QUALITY_CHECK_FAIL
        )

        override fun statusSet(): Set<BuildStatus> = pipelineStatus
    }

    class PipelineBuildStatusMaker : BuildStatusMaker {

        private val pipelineStatus = setOf(
            BuildStatus.QUEUE,
            BuildStatus.QUEUE_CACHE,
            BuildStatus.RUNNING,
            BuildStatus.CANCELED,
            BuildStatus.SUCCEED,
            BuildStatus.FAILED,
            BuildStatus.TERMINATE,
            BuildStatus.QUEUE_TIMEOUT,
            BuildStatus.STAGE_SUCCESS
        )

        override fun statusSet(): Set<BuildStatus> = pipelineStatus
    }

    class StageBuildStatusMaker : BuildStatusMaker {

        private val pipelineStatus = setOf(
            BuildStatus.QUEUE,
            BuildStatus.QUEUE_CACHE,
            BuildStatus.RUNNING,
            BuildStatus.REVIEWING,
            BuildStatus.PAUSE,
            BuildStatus.CANCELED,
            BuildStatus.SUCCEED,
            BuildStatus.FAILED,
            BuildStatus.TERMINATE,
            BuildStatus.SKIP,
            BuildStatus.UNEXEC,
            BuildStatus.QUEUE_TIMEOUT,
            BuildStatus.STAGE_SUCCESS
        )

        override fun statusSet(): Set<BuildStatus> = pipelineStatus
    }

    class JobBuildStatusMaker : BuildStatusMaker {

        private val pipelineStatus = setOf(
            BuildStatus.QUEUE,
            BuildStatus.QUEUE_CACHE,
            BuildStatus.DEPENDENT_WAITING, // 依赖等待
            BuildStatus.LOOP_WAITING, // 互斥组抢锁轮循
            BuildStatus.PREPARE_ENV,
            BuildStatus.RUNNING,
            BuildStatus.CANCELED,
            BuildStatus.SUCCEED,
            BuildStatus.FAILED,
            BuildStatus.TERMINATE,
            BuildStatus.SKIP,
            BuildStatus.UNEXEC,
            BuildStatus.QUEUE_TIMEOUT,
            BuildStatus.HEARTBEAT_TIMEOUT
        )

        override fun statusSet(): Set<BuildStatus> = pipelineStatus
    }
}
