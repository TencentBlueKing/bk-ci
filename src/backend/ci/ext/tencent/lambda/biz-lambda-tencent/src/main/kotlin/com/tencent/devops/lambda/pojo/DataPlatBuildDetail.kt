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

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "构建详情")
data class DataPlatBuildDetail(
    @Schema(title = "清洗时间", required = false)
    val washTime: String,
    @Schema(title = "构建ID", required = true)
    val buildId: String,
    @Schema(title = "模板ID", required = true)
    val templateId: String,
    @Schema(title = "事业群名称", required = true)
    val bgName: String,
    @Schema(title = "部门名称", required = true)
    val deptName: String,
    @Schema(title = "中心名称", required = true)
    val centerName: String,
    @Schema(title = "项目ID", required = true)
    val projectId: String,
    @Schema(title = "流水线ID", required = true)
    val pipelineId: String,
    @Schema(title = "构建号", required = false)
    val buildNum: Int?,
    @Schema(title = "是否保密项目", required = false)
    val isSecrecy: Boolean?,
    @Schema(title = "构建详情", required = true)
    val model: String,
    @Schema(title = "构建触发人", required = false)
    val startUser: String?,
    @Schema(title = "出发方式", required = false)
    val trigger: String?,
    @Schema(title = "启动时间", required = false)
    val startTime: String,
    @Schema(title = "结束时间", required = false)
    val endTime: String?,
    @Schema(title = "构建状态", required = false)
    val status: String?,
    @Schema(title = "事业群ID", required = false)
    val bgId: String,
    @Schema(title = "部门ID", required = false)
    val deptId: String,
    @Schema(title = "中心ID", required = false)
    val centerId: String
)
