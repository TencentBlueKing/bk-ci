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

package com.tencent.devops.common.api.pojo

import com.tencent.devops.common.api.annotation.InfluxTag
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "插件监控数据")
data class AtomMonitorData(
    @get:Schema(title = "插件执行错误码", required = true)
    @InfluxTag
    val errorCode: Int,
    @get:Schema(title = "插件执行错误信息", required = false)
    val errorMsg: String? = null,
    @get:Schema(title = "插件执行错误类型", required = false)
    @InfluxTag
    val errorType: String? = null,
    @get:Schema(title = "插件代码", required = true)
    @InfluxTag
    val atomCode: String,
    @get:Schema(title = "插件版本", required = true)
    val version: String,
    @get:Schema(title = "项目ID", required = true)
    val projectId: String,
    @get:Schema(title = "流水线ID", required = true)
    val pipelineId: String,
    @get:Schema(title = "构建ID", required = true)
    val buildId: String,
    @get:Schema(title = "构建环境ID", required = true)
    val vmSeqId: String,
    @get:Schema(title = "执行开始时间", required = false)
    @InfluxTag
    val startTime: Long?,
    @get:Schema(title = "执行结束时间", required = false)
    @InfluxTag
    val endTime: Long?,
    @get:Schema(title = "执行耗时时间", required = false)
    val elapseTime: Long?,
    @get:Schema(title = "来源渠道", required = false)
    val channel: String? = null,
    @get:Schema(title = "执行人", required = true)
    val starter: String,
    @get:Schema(title = "组织架构详细信息", required = true)
    val organizationDetailInfo: OrganizationDetailInfo,
    @get:Schema(title = "扩展数据", required = false)
    val extData: Map<String, Any>? = null
)
