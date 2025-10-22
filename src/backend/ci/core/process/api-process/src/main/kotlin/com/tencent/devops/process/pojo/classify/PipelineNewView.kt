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

package com.tencent.devops.process.pojo.classify

import com.tencent.devops.process.pojo.classify.enums.Logic
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线视图")
data class PipelineNewView(
    @get:Schema(title = "流水线id", required = false)
    val id: String,
    @get:Schema(title = "项目id", required = false)
    val projectId: String,
    @get:Schema(title = "名称", required = false)
    val name: String,
    @get:Schema(title = "是否项目", required = false)
    val projected: Boolean,
    @get:Schema(title = "创建时间", required = false)
    val createTime: Long,
    @get:Schema(title = "更新时间", required = false)
    val updateTime: Long,
    @get:Schema(title = "创建者", required = false)
    val creator: String,
    @get:Schema(title = "逻辑符", required = false)
    val logic: Logic,
    @get:Schema(title = "流水线视图过滤器列表", required = false)
    val filters: List<PipelineViewFilter>,
    @get:Schema(title = "视图类型", required = true)
    val viewType: Int,
    @get:Schema(title = "流水线ID列表", required = true)
    val pipelineIds: List<String>
)
