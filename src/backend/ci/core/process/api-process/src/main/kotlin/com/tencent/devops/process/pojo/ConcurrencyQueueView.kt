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

package com.tencent.devops.process.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "互斥组排队视图")
data class ConcurrencyQueueView(
    @get:Schema(title = "当前构建是否处于互斥组排队，为 false 时不展示面板")
    val visible: Boolean,
    @get:Schema(title = "服务器当前时间戳（毫秒），用于计算等待和运行时长")
    val serverTime: Long,
    @get:Schema(title = "已解析的互斥组名。visible 为 false 时为空")
    val concurrencyGroup: String? = null,
    @get:Schema(title = "当前构建入队时间（毫秒）。已等待 = serverTime - queueTime")
    val queueTime: Long? = null,
    @get:Schema(title = "正在占用互斥组的构建，按开始时间从早到晚，最多 10 条")
    val running: List<ConcurrencyQueueBuild> = emptyList(),
    @get:Schema(title = "排在当前构建前面的构建，按出队顺序，最多 20 条")
    val ahead: List<ConcurrencyQueueBuild> = emptyList(),
    @get:Schema(title = "前方排队总数，可能大于 ahead 的条数")
    val aheadCount: Int = 0
) {
    companion object {
        const val AHEAD_LIMIT = 20
        const val RUNNING_LIMIT = 10
    }
}

@Schema(title = "互斥组排队中的一条构建")
data class ConcurrencyQueueBuild(
    @get:Schema(title = "构建ID")
    val buildId: String,
    @get:Schema(title = "流水线ID")
    val pipelineId: String,
    @get:Schema(title = "流水线名称。无查看权限时为空")
    val pipelineName: String? = null,
    @get:Schema(title = "构建号")
    val buildNum: Int,
    @get:Schema(title = "状态枚举名，如 RUNNING、QUEUE、QUEUE_CACHE")
    val status: String,
    @get:Schema(title = "触发人。无查看权限时为空串")
    val userId: String,
    @get:Schema(title = "触发方式原始值，如 MANUAL、WEB_HOOK。无查看权限时为空串")
    val trigger: String,
    @get:Schema(title = "构建信息，对应 BK_CI_BUILD_MSG，用作卡片副标题。无查看权限时为空")
    val buildMsg: String? = null,
    @get:Schema(title = "入队时间（毫秒）")
    val queueTime: Long,
    @get:Schema(title = "开始执行时间（毫秒）。尚未开始时为空")
    val startTime: Long? = null,
    @get:Schema(title = "是否调试构建")
    val debug: Boolean = false,
    @get:Schema(title = "当前用户是否可查看该流水线。为 false 时不要跳转详情，也不要展示名称、触发人和构建信息")
    val detailVisible: Boolean = true
)
