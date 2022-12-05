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

package com.tencent.devops.lambda.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("构建详情")
data class DataPlatBuildDetail(
    @ApiModelProperty("清洗时间", required = false)
    val washTime: String,
    @ApiModelProperty("构建ID", required = true)
    val buildId: String,
    @ApiModelProperty("模板ID", required = true)
    val templateId: String,
    @ApiModelProperty("事业群名称", required = true)
    val bgName: String,
    @ApiModelProperty("部门名称", required = true)
    val deptName: String,
    @ApiModelProperty("中心名称", required = true)
    val centerName: String,
    @ApiModelProperty("项目ID", required = true)
    val projectId: String,
    @ApiModelProperty("流水线ID", required = true)
    val pipelineId: String,
    @ApiModelProperty("构建号", required = false)
    val buildNum: Int?,
    @ApiModelProperty("是否保密项目", required = false)
    val isSecrecy: Boolean?,
    @ApiModelProperty("构建详情", required = true)
    val model: String,
    @ApiModelProperty("构建触发人", required = false)
    val startUser: String?,
    @ApiModelProperty("出发方式", required = false)
    val trigger: String?,
    @ApiModelProperty("启动时间", required = false)
    val startTime: String,
    @ApiModelProperty("结束时间", required = false)
    val endTime: String?,
    @ApiModelProperty("构建状态", required = false)
    val status: String?,
    @ApiModelProperty("事业群ID", required = false)
    val bgId: String,
    @ApiModelProperty("部门ID", required = false)
    val deptId: String,
    @ApiModelProperty("中心ID", required = false)
    val centerId: String
)
