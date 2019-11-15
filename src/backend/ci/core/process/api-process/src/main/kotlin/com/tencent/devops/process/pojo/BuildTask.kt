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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.pojo

import com.tencent.devops.common.pipeline.enums.BuildTaskStatus
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("流水线模型-构建任务")
data class BuildTask(
    @ApiModelProperty("构建ID", required = true)
    val buildId: String,
    @ApiModelProperty("构建环境ID", required = true)
    val vmSeqId: String,
    @ApiModelProperty("任务状态", required = true)
    val status: BuildTaskStatus,
    @ApiModelProperty("任务ID", required = true)
    val taskId: String? = null,
    @ApiModelProperty("插件ID", required = true)
    val elementId: String? = null,
    @ApiModelProperty("插件名字", required = true)
    val elementName: String? = null,
    @ApiModelProperty("任务类型", required = false)
    val type: String? = null,
    @ApiModelProperty("任务参数", required = false)
    val params: Map<String, String>? = null,
    @ApiModelProperty("环境参数", required = false)
    val buildVariable: Map<String, String>? = null
)