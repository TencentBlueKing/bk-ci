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

package com.tencent.devops.store.pojo.common.statistic

import com.tencent.devops.common.api.util.DateTimeUtil
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.util.Date

@Schema(title = "每日统计信息请求报文")
data class StoreDailyStatisticRequest(
    @get:Schema(title = "总下载量")
    var totalDownloads: Int? = null,
    @get:Schema(title = "每日下载量")
    var dailyDownloads: Int? = null,
    @get:Schema(title = "每日执行成功数")
    val dailySuccessNum: Int? = null,
    @get:Schema(title = "每日执行失败数")
    val dailyFailNum: Int? = null,
    @get:Schema(title = "每日执行失败详情")
    val dailyFailDetail: Map<String, Any>? = null,
    @get:Schema(title = "每日活跃时长，单位：小时")
    val dailyActiveDuration: Double? = null,
    @get:Schema(title = "统计时间")
    val statisticsTime: LocalDateTime = DateTimeUtil.convertDateToFormatLocalDateTime(Date(), "yyyy-MM-dd")
)
