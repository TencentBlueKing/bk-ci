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

import com.fasterxml.jackson.annotation.JsonProperty

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "查询agent任务状态的接口的返回结果")
data class AgentQueryAgentTaskStatusResult(
    @get:Schema(title = "", required = true)
    @JsonProperty("job_id")
    val jobId: Int,
    @get:Schema(title = "", required = true)
    @JsonProperty("created_by")
    val createdBy: String,
    @get:Schema(title = "", required = true)
    @JsonProperty("job_type")
    val jobType: String,
    @get:Schema(title = "", required = true)
    @JsonProperty("job_type_display")
    val jobTypeDisplay: String,
    @get:Schema(title = "", required = true)
    @JsonProperty("ip_filter_list")
    val ipFilterList: List<String>,
    @get:Schema(title = "")
    val total: Int?,
    @get:Schema(title = "")
    val list: List<AgentHostDetail>?,
    @get:Schema(title = "", required = true)
    val statistics: AgentStatistics,
    @get:Schema(title = "", required = true)
    val status: String,
    @get:Schema(title = "")
    @JsonProperty("end_time")
    val endTime: String?,
    @get:Schema(title = "", required = true)
    @JsonProperty("start_time")
    val startTime: String,
    @get:Schema(title = "", required = true)
    @JsonProperty("cost_time")
    val costTime: String,
    @get:Schema(title = "", required = true)
    val meta: AgentMeta
)