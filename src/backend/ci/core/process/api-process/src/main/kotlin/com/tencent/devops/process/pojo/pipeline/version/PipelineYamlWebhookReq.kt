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

package com.tencent.devops.process.pojo.pipeline.version

import com.tencent.devops.process.pojo.pipeline.PipelineYamlFileInfo
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线yaml文件推送请求")
data class PipelineYamlWebhookReq(
    @get:Schema(title = "模板YAML", required = true)
    val yaml: String,
    @get:Schema(title = "yaml文件名", required = true)
    val yamlFileName: String,
    @get:Schema(title = "分支名", required = true)
    val branchName: String,
    @get:Schema(title = "是否默认分支", required = true)
    val isDefaultBranch: Boolean,
    @get:Schema(title = "描述", required = true)
    val description: String? = null,
    @get:Schema(title = "yaml文件信息", required = true)
    val yamlFileInfo: PipelineYamlFileInfo? = null,
    @get:Schema(title = "合并请求ID", required = true)
    val pullRequestId: Long? = null,
    @get:Schema(title = "合并请求链接", required = true)
    val pullRequestUrl: String? = null
) : PipelineVersionCreateReq
