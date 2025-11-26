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

package com.tencent.devops.environment.pojo.thirdpartyagent

import io.swagger.v3.oas.annotations.media.Schema
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Schema(title = "Agent活动（上下线）")
data class ThirdPartyAgentAction(
    @get:Schema(title = "Agent Hash Id", required = true)
    val agentId: String,
    @get:Schema(title = "项目ID", required = true)
    val projectId: String,
    @get:Schema(title = "活动", required = true)
    val action: String,
    @get:Schema(title = "活动时间", required = true)
    val actionTime: Long
)

data class OfflinePeriod(
    val offlineTime: Long, // 离线开始时间
    val onlineTime: Long,        // 上线时间（离线结束时间）
    val duration: Long           // 持续时长（秒）
) {
    fun getOfflineTimeStr(): String {
        return formatTime(offlineTime)
    }

    fun getOnlineTimeStr(): String {
        return formatTime(onlineTime)
    }

    fun getDurationStr(): String {
        val days = duration / (24 * 3600)
        val hours = (duration % (24 * 3600)) / 3600
        val minutes = (duration % 3600) / 60

        return buildString {
            if (days > 0) append("${days}天")
            if (hours > 0) append("${hours}时")
            if (minutes > 0 || (days == 0L && hours == 0L)) append("${minutes}分")
        }
    }

    private fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp * 1000))
    }
}
