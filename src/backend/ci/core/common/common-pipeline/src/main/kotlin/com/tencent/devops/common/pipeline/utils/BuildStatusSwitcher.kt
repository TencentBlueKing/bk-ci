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
     * 取消构建时，[currentBuildStatus]应该切换成什么状态
     * 当[BuildStatus.isReadyToRun]准备执行状态，则直接设置为[BuildStatus.CANCELED]
     * 当[BuildStatus.isRunning]执行中状态，则直接设置为[BuildStatus.CANCELED]
     * 当[BuildStatus.isFinish]结束状态，则直接原样返回[currentBuildStatus]
     * 其他未列入状态，暂定[BuildStatus.CANCELED]
     */
    fun cancel(currentBuildStatus: BuildStatus): BuildStatus {
        return when {
            currentBuildStatus == BuildStatus.UNKNOWN -> BuildStatus.CANCELED
            currentBuildStatus.isReadyToRun() -> BuildStatus.CANCELED
            currentBuildStatus.isRunning() -> BuildStatus.CANCELED
            currentBuildStatus.isFinish() -> currentBuildStatus
            else -> BuildStatus.CANCELED // 其他状态暂定
        }
    }

    /**
     * 正常结束构建时，[currentBuildStatus]应该切换成什么状态。
     * 如已经结束，则直接返回[currentBuildStatus],否则返回[BuildStatus.SUCCEED]
     */
    fun finish(currentBuildStatus: BuildStatus): BuildStatus {
        return if (currentBuildStatus.isFinish() || currentBuildStatus == BuildStatus.STAGE_SUCCESS) {
            currentBuildStatus
        } else {
            BuildStatus.SUCCEED
        }
    }

    /**
     * 对于流水线状态来讲，成功要转换为SUCCEED, 失败统一为FAILED，CANCEL取消和STAGE_SUCCESS保持不变
     */
    fun fixPipelineFinish(currentBuildStatus: BuildStatus): BuildStatus {
        var buildStatus = finish(currentBuildStatus)
        if (buildStatus.isSuccess()) {
            buildStatus = BuildStatus.SUCCEED
        } else if (buildStatus.isFailure()) {
            buildStatus = BuildStatus.FAILED
        }
        return buildStatus
    }

    /**
     * 强制结束构建时，[currentBuildStatus]应该切换成什么状态。
     * 如已经结束，则直接返回[currentBuildStatus],否则返回[BuildStatus.FAILED]
     */
    fun forceFinish(currentBuildStatus: BuildStatus): BuildStatus {
        return if (currentBuildStatus.isFinish() || currentBuildStatus == BuildStatus.STAGE_SUCCESS) {
            currentBuildStatus
        } else {
            BuildStatus.FAILED
        }
    }

    /**
     * 一个[BuildStatus.isReadyToRun]状态的插件任务在切换成[BuildStatus.SKIP]前，
     * 如果 [containerFailure]容器状态已经出错：
     *  则返回[BuildStatus.UNEXEC]表示从未执行
     * 否则
     *  返回[BuildStatus.SKIP] 切换成为跳过
     */
    fun readyToSkipWhen(containerFailure: Boolean): BuildStatus {
        return if (containerFailure) { // 容器已经出错，排队变未执行
            BuildStatus.UNEXEC
        } else {
            BuildStatus.SKIP
        }
    }
}
