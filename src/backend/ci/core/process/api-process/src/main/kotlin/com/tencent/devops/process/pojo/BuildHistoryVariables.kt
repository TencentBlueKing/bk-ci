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

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("历史构建变量")
data class BuildHistoryVariables(
    @ApiModelProperty("构建ID", required = true)
    val id: String,
    @ApiModelProperty("启动用户", required = true)
    val userId: String,
    @ApiModelProperty("触发条件", required = true)
    val trigger: String,
    @ApiModelProperty("流水线名称", required = true)
    val pipelineName: String,
    @ApiModelProperty("构建号", required = true)
    val buildNum: Int,
    @ApiModelProperty("编排文件版本号", required = true)
    val pipelineVersion: Int,
    @ApiModelProperty("开始时间", required = true)
    val startTime: Long,
    @ApiModelProperty("结束时间", required = true)
    val endTime: Long?,
    @ApiModelProperty("状态", required = true)
    val status: String,
    @ApiModelProperty("构建变量集合", required = true)
    val variables: Map<String, String>
)