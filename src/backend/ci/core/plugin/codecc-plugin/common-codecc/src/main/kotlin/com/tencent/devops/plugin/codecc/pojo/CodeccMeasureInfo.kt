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

package com.tencent.devops.plugin.codecc.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("codecc度量信息")
data class CodeccMeasureInfo(
    @ApiModelProperty("项目ID", required = true)
    val projectId: String,
    @ApiModelProperty("任务ID", required = false)
    val taskId: Long?,
    @ApiModelProperty("代码库提交ID", required = false)
    val commitId: String?,
    @ApiModelProperty("代码库url地址", required = false)
    val repoUrl: String?,
    @ApiModelProperty("codecc扫描详情url地址", required = false)
    val codeccUrl: String?,
    @ApiModelProperty("规范得分", required = false)
    val codeStyleScore: Double?,
    @ApiModelProperty("安全得分", required = false)
    val codeSecurityScore: Double?,
    @ApiModelProperty("度量得分", required = false)
    val codeMeasureScore: Double?,
    @ApiModelProperty("最近一次分析触发时间", required = false)
    val lastAnalysisTime: Long?,
    @ApiModelProperty("任务状态(失败：1， 执行中: 3， 成功：0, 未执行：2)", required = false)
    val status: Int?,
    @ApiModelProperty("工具执行信息", required = false)
    val lastAnalysisResultList: List<CodeccToolAnalysisInfo>?,
    @ApiModelProperty("是否合格", required = false)
    var qualifiedFlag: Boolean? = null
)