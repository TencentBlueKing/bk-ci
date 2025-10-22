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

package com.tencent.devops.dispatch.docker.pojo

import com.tencent.devops.common.pipeline.type.BuildType
import io.swagger.v3.oas.annotations.media.Schema

/**
 * DebugStartParam
 */
@Schema(title = "启动webConsole调试请求参数")
data class DebugStartParam(
    @get:Schema(title = "项目id", required = true)
    val projectId: String,
    @get:Schema(title = "流水线Id", required = true)
    val pipelineId: String,
    @get:Schema(title = "vmSeqId", required = true)
    val vmSeqId: String,
    @get:Schema(title = "imageType为BKSTORE时的镜像编码", required = false)
    val imageCode: String? = null,
    @get:Schema(title = "imageType为BKSTORE时的镜像版本", required = false)
    val imageVersion: String? = null,
    @get:Schema(title = "镜像名称", required = false)
    val imageName: String? = null,
    @get:Schema(title = "环境变量", required = true)
    val buildEnv: Map<String, String>?,
    @get:Schema(title = "镜像类型(BKDEVOPS或THIRD或BKSTORE)", required = false)
    val imageType: String? = null,
    @get:Schema(title = "镜像仓库凭证ID", required = false)
    val credentialId: String? = null,
    @get:Schema(title = "启动命令", required = false)
    val cmd: String? = "/bin/sh",
    @get:Schema(title = "镜像信息", required = false)
    val containerPool: String? = null,
    @get:Schema(title = "buildId", required = false)
    val buildId: String? = null,
    @get:Schema(title = "dispatchType", required = false)
    val dispatchType: String = BuildType.DOCKER.name
)
