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

package com.tencent.devops.stream.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "蓝盾stream流水线列表")
data class StreamGitProjectPipeline(
    @get:Schema(title = "git项目ID", required = true)
    val gitProjectId: Long,
    @get:Schema(title = "流水线名称", required = true)
    var displayName: String,
    @get:Schema(title = "蓝盾流水线ID", required = true)
    var pipelineId: String,
    @get:Schema(title = "文件路径", required = true)
    val filePath: String,
    @get:Schema(title = "是否启用", required = true)
    val enabled: Boolean,
    @get:Schema(title = "创建人", required = false)
    val creator: String?,
    @get:Schema(title = "自己一次构建分支", required = false)
    val latestBuildBranch: String?,
    @get:Schema(title = "git yaml文件链接", required = false)
    val yamlLink: String? = "",
    @get:Schema(title = "最后一次更新分支", required = false)
    val lastUpdateBranch: String? = ""
)

fun StreamGitProjectPipeline.isExist(): Boolean {
    return pipelineId.isNotBlank()
}
