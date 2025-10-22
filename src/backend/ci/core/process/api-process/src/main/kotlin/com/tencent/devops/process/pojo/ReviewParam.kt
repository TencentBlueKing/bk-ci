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

package com.tencent.devops.process.pojo

import com.tencent.devops.common.pipeline.enums.ManualReviewAction
import com.tencent.devops.common.pipeline.pojo.element.atom.ManualReviewParam
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "人工审核插件-审核信息")
data class ReviewParam(
//    userId: String, projectId: String, pipelineId: String, buildId: String, elementId: String
//    @get:Schema(title = "主键ID", required = false)
//    var id: Long,
    @get:Schema(title = "项目Id", required = true)
    var projectId: String = "",
    @get:Schema(title = "流水线Id", required = true)
    var pipelineId: String = "",
    @get:Schema(title = "构建Id", required = true)
    var buildId: String = "",
    @get:Schema(title = "审核人", required = true)
    var reviewUsers: MutableList<String> = mutableListOf(),
    @get:Schema(title = "审核结果", required = false)
    var status: ManualReviewAction? = null,
    @get:Schema(title = "描述", required = false)
    var desc: String? = "",
    @get:Schema(title = "审核意见", required = false)
    var suggest: String? = "",
    @get:Schema(title = "参数列表", required = false)
    var params: MutableList<ManualReviewParam> = mutableListOf()
)
