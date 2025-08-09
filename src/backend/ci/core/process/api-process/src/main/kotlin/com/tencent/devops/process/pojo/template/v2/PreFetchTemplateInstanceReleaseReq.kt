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

package com.tencent.devops.process.pojo.template.v2

import com.tencent.devops.common.pipeline.enums.CodeTargetAction
import com.tencent.devops.process.pojo.PipelineIdAndName
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线模版实例化发布预览请求")
data class PreFetchTemplateInstanceReleaseReq(
    @get:Schema(title = "流水线ID和名称", required = true)
    val pipelineIdAndNames: List<PipelineIdAndName>,
    @get:Schema(title = "启用PAC", required = true)
    val enablePac: Boolean = false,
    @get:Schema(title = "提交动作", required = true)
    val targetAction: CodeTargetAction? = null,
    @get:Schema(title = "代码库hashId", required = true)
    val repoHashId: String? = null,
    @get:Schema(title = "指定提交的分支", required = true)
    val targetBranch: String? = null
)
