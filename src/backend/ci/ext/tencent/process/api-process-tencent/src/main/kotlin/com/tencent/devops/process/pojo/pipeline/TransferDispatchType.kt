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

package com.tencent.devops.process.pojo.pipeline

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("迁移构建资源")
data class TransferDispatchType(
    @ApiModelProperty("要迁移的项目ID", required = true)
    val projectId: String,
    @ApiModelProperty("要迁移的流水线列表，为空表示所有流水线", required = false, hidden = true)
    val pipelineIds: List<String> = mutableListOf(),
    @ApiModelProperty("源Dispatch Type, 默认为DOCKER，表示从Docker On VM迁移", required = false, hidden = true)
    val sourceDispatchType: String = "DOCKER",
    @ApiModelProperty("目标Dispatch Type, 默认为PUBLIC_DEVCLOUD，表示迁移到DevCloud公共构建机", required = false, hidden = true)
    val targetDispatchType: String = "PUBLIC_DEVCLOUD",
    @ApiModelProperty("渠道号，默认为BS表示蓝盾上创建的流水线", required = false, hidden = true)
    val channelCode: String = "BS"
)
