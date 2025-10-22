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

package com.tencent.devops.dispatch.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "项目的JOB状态")
data class JobQuotaStatus(
    @get:Schema(title = "项目JOB数量配额", required = true)
    val jobQuota: Int,
    @get:Schema(title = "已运行JOB数量", required = true)
    val runningJobCount: Int,
    @get:Schema(title = "JOB告警阈值，百分比0-100", required = true)
    val jobThreshold: Int,
    @get:Schema(title = "项目job时间配额，单位：小时", required = true)
    val timeQuota: Long,
    @get:Schema(title = "当月已运行JOB时间, 单位：毫秒", required = true)
    val runningJobTime: Long,
    @get:Schema(title = "时间告警阈值，百分比0-100", required = true)
    val timeThreshold: Int
)
