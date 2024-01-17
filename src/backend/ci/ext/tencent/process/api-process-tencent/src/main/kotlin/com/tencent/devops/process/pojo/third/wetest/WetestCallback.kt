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

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "wetest 回调模型")
data class WetestCallback(
    @Schema(title = "相当于projectId", required = true)
    val productID: String,
    @Schema(title = "wetest的jobID", required = true)
    val jobID: String,
    @Schema(title = "buildID", required = true)
    val buildID: String,
    @Schema(title = "wetest的taskID", required = true)
    val taskID: String,
    @Schema(title = "wetest的sodaID，相当于pipelineId,", required = true)
    val sodaId: String,
    @Schema(title = "result_quality", required = false)
    val resultQuality: String,
    @Schema(title = "result_devnum", required = false)
    val resultDevNum: String,
    @Schema(title = "result_Rate", required = false)
    val resultRate: String,
    @Schema(title = "result_Problems", required = false)
    val resultProblems: String,
    @Schema(title = "result_Serious", required = false)
    val resultSerious: String,
    @Schema(title = "starttime", required = false)
    val startTime: String,
    @Schema(title = "endtime", required = false)
    val endTime: String
)
