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
 */

package com.tencent.devops.dispatch.docker.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "容器信息模型")
data class ContainerInfo(
    @Schema(name = "项目id")
    val projectId: String,
    @Schema(name = "流水线id")
    val pipelineId: String,
    @Schema(name = "构建序列号")
    val vmSeqId: String,
    @Schema(name = "资源池序号")
    val poolNo: Int,
    @Schema(name = "状态")
    val status: Int,
    @Schema(name = "镜像名称")
    val imageName: String,
    @Schema(name = "容器id")
    val containerId: String,
    @Schema(name = "容器地址")
    val address: String,
    @Schema(name = "容器token")
    val token: String,
    @Schema(name = "构建环境")
    val buildEnv: String?,
    @Schema(name = "仓库用户名")
    val registryUser: String?,
    @Schema(name = "仓库密码")
    val registryPwd: String?,
    @Schema(name = "镜像类型")
    val imageType: String?
)
