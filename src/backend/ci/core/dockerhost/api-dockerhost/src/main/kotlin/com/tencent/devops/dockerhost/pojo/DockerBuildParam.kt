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

package com.tencent.devops.dockerhost.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "DockerBuild")
data class DockerBuildParam(
    @get:Schema(title = "镜像名称", required = true)
    val imageName: String,
    @get:Schema(title = "镜像TAG", required = true)
    val imageTag: String,
    @get:Schema(title = "构建目录", required = false)
    val buildDir: String? = ".",
    @get:Schema(title = "Dockerfile", required = false)
    val dockerFile: String? = "Dockerfile",
    @get:Schema(title = "repoAddr", required = true)
    val repoAddr: String,
    @get:Schema(title = "userName", required = true)
    val userName: String,
    @get:Schema(title = "password", required = true)
    val password: String,
    @get:Schema(title = "基础镜像凭证", required = true)
    val ticket: List<Triple<String, String, String>> = emptyList(),
    @get:Schema(title = "构建的参数", required = true)
    val args: List<String> = emptyList(),
    @get:Schema(title = "host配置", required = true)
    val host: List<String> = emptyList(),
    @get:Schema(title = "并发构建池序号", required = false)
    val poolNo: String? = "0",
    @get:Schema(title = "镜像tag列表", required = false)
    val imageTagList: List<String> = emptyList(),
    @get:Schema(title = "pipelineTaskId", required = false)
    val pipelineTaskId: String = "",
    @get:Schema(title = "流水线触发用户", required = false)
    val userId: String = ""
)
