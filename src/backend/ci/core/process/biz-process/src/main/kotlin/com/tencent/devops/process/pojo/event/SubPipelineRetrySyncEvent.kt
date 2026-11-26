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

package com.tencent.devops.process.pojo.event

import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.event.pojo.IEvent
import com.tencent.devops.common.stream.constants.StreamBinding

/**
 * #8609 子流水线同步模式重试后异步联动父流水线单步重试事件
 *
 * 设计目的：
 * 1. 与子构建重试主流程与 PipelineBuildQueueBroadCastEvent 广播消费解耦，绝不阻塞子构建启动或持有父子双锁；
 * 2. 支持通过 delayMills 做非阻塞锁竞争退避重投（抢不到父 BuildIdLock / StageIdLock 时立即释放消费线程）；
 * 3. 支持跨项目调用经父集群接口落入父集群本地队列异步消费（fromRemoteForward = true）。
 */
@Event(StreamBinding.PIPELINE_SUB_PIPELINE_RETRY_SYNC)
data class SubPipelineRetrySyncEvent(
    val parentProjectId: String,
    val parentPipelineId: String,
    val parentBuildId: String,
    val parentTaskId: String,
    val subProjectId: String,
    val subPipelineId: String,
    val subPipelineName: String = "",
    val subBuildId: String,
    val subBuildNum: Int? = null,
    val subExecuteCount: Int,
    val operator: String? = null,
    val visitedBuildIds: List<String> = emptyList(),
    val lockRetryCount: Int = 0,
    val fromRemoteForward: Boolean = false,
    override var delayMills: Int = 0,
    override var retryTime: Int = 1
) : IEvent(delayMills = delayMills, retryTime = retryTime)
