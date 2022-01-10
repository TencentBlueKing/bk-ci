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

package com.tencent.devops.process.pojo.third.wetest

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("wetest 回调模型")
data class WetestCallback(
    @ApiModelProperty(value = "相当于projectId", required = true)
    val productID: String,
    @ApiModelProperty(value = "wetest的jobID", required = true)
    val jobID: String,
    @ApiModelProperty(value = "buildID", required = true)
    val buildID: String,
    @ApiModelProperty(value = "wetest的taskID", required = true)
    val taskID: String,
    @ApiModelProperty(value = "wetest的sodaID，相当于pipelineId,", required = true)
    val sodaId: String,
    @ApiModelProperty(value = "result_quality", required = false)
    val resultQuality: String,
    @ApiModelProperty(value = "result_devnum", required = false)
    val resultDevNum: String,
    @ApiModelProperty(value = "result_Rate", required = false)
    val resultRate: String,
    @ApiModelProperty(value = "result_Problems", required = false)
    val resultProblems: String,
    @ApiModelProperty(value = "result_Serious", required = false)
    val resultSerious: String,
    @ApiModelProperty(value = "starttime", required = false)
    val startTime: String,
    @ApiModelProperty(value = "endtime", required = false)
    val endTime: String
)
