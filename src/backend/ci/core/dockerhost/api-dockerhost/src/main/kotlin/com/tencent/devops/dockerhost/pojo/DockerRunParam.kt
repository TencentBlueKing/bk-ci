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

@Schema(title = "DockerRun")
data class DockerRunParam(
    @get:Schema(title = "镜像名称，包括tag", required = true)
    val imageName: String,
    @get:Schema(title = "镜像仓库用户名", required = true)
    val registryUser: String?,
    @get:Schema(title = "镜像仓库密码", required = true)
    val registryPwd: String?,
    @get:Schema(title = "命令行", required = false)
    val command: List<String>,
    @get:Schema(title = "环境变量", required = false)
    val env: Map<String, String?>?,
    @get:Schema(title = "并发构建池序号", required = false)
    val poolNo: String? = "0",
    @get:Schema(title = "映射端口列表", required = false)
    val portList: List<Int>? = emptyList()
)
