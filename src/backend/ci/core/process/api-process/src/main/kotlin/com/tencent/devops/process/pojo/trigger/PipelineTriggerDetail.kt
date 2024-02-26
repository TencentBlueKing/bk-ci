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
 *
 */

package com.tencent.devops.process.pojo.trigger

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线触发详情")
data class PipelineTriggerDetail(
    var detailId: Long? = null,
    @get:Schema(title = "蓝盾项目ID")
    val projectId: String,
    @get:Schema(title = "事件ID")
    val eventId: Long,
    @get:Schema(title = "触发状态")
    var status: String,
    @get:Schema(title = "流水线Id")
    var pipelineId: String? = null,
    @get:Schema(title = "流水线名称")
    var pipelineName: String? = null,
    @get:Schema(title = "构建Id")
    var buildId: String? = null,
    @get:Schema(title = "构建编号")
    var buildNum: String? = null,
    @get:Schema(title = "原因")
    var reason: String? = null,
    @get:Schema(title = "原因详情", required = false)
    var reasonDetailList: List<String>? = null,
    @get:Schema(title = "创建时间", required = false)
    val createTime: Long? = null
)
