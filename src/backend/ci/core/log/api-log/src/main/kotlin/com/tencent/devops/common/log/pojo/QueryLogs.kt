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

package com.tencent.devops.common.log.pojo

import com.tencent.devops.common.log.pojo.enums.LogStatus
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * Powered By Tencent
 */
@Schema(title = "日志查询模型")
data class QueryLogs(
    @get:Schema(title = "构建ID", required = true)
    val buildId: String,
    @get:Schema(title = "是否结束", required = true)
    var finished: Boolean,
    @get:Schema(title = "是否有后续日志", required = false)
    var hasMore: Boolean? = false,
    @get:Schema(title = "日志列表", required = true)
    var logs: MutableList<LogLine> = mutableListOf(),
    @get:Schema(title = "所用时间", required = false)
    var timeUsed: Long = 0,
    @get:Schema(title = "日志查询状态", required = false)
    var status: Int = LogStatus.SUCCEED.status,
    @get:Schema(title = "日志子tag列表", required = false)
    var subTags: List<String>? = null,
    @get:Schema(title = "错误信息", required = false)
    var message: String? = null
)
