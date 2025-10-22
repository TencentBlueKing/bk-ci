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

package com.tencent.devops.common.log.pojo.message

import com.tencent.devops.common.log.pojo.enums.LogType
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * Powered By Tencent
 */
@Schema(title = "日志模型")
data class LogMessage(
    @get:Schema(title = "日志内容信息")
    var message: String,
    @get:Schema(title = "时间戳")
    val timestamp: Long,
    @get:Schema(title = "标签")
    val tag: String = "",
    @get:Schema(title = "step Id")
    val stepId: String = "",
    @get:Schema(title = "job id")
    val jobId: String = "",
    @get:Schema(title = "container Hash Id")
    val containerHashId: String = "",
    @get:Schema(title = "日志类型")
    val logType: LogType = LogType.LOG,
    @get:Schema(title = "执行次数")
    val executeCount: Int? = null,
    @get:Schema(title = "子标签")
    val subTag: String? = null
) {
    override fun toString(): String {
        return "LogMessage(tag='$tag', subTag='$subTag', jobId='$jobId', message='$message', " +
            "timestamp=$timestamp), logType=$logType, executeCount=$executeCount)"
    }
}
