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

package com.tencent.devops.environment.pojo.job.agentres

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "查询agent任务状态的接口的返回结果")
data class QueryAgentTaskStatusResult(
    @get:Schema(title = "作业任务ID", required = true)
    val jobId: Int,
    @get:Schema(title = "创建者", required = true)
    val createdBy: String,
    @get:Schema(title = "作业类型", required = true)
    val jobType: String,
    @get:Schema(title = "作业类型名称", required = true)
    val jobTypeDisplay: String,
    @get:Schema(title = "过滤的IP列表", required = true)
    val ipFilterList: List<String>,
    @get:Schema(title = "实例记录数量总和")
    val total: Int?,
    @get:Schema(title = "过滤的主机详细信息列表")
    val list: List<HostDetail>?,
    @get:Schema(title = "任务统计信息", required = true)
    val statistics: Statistics,
    @get:Schema(title = "执行状态", required = true)
    val status: String,
    @get:Schema(title = "完成时间")
    val endTime: String?,
    @get:Schema(title = "启动时间时间", required = true)
    val startTime: String,
    @get:Schema(title = "执行耗时", required = true)
    val costTime: String,
    @get:Schema(title = "执行任务元数据信息", required = true)
    val meta: Meta
)