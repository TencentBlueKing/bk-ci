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

package com.tencent.devops.common.event.pojo.measure

import com.tencent.devops.common.api.pojo.ErrorInfo
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("构建结束后流水线指标数据")
data class BuildEndPipelineMetricsData(
    @ApiModelProperty("统计时间", required = true)
    val statisticsTime: String,
    @ApiModelProperty("项目ID", required = true)
    val projectId: String,
    @ApiModelProperty("流水线ID", required = true)
    val pipelineId: String,
    @ApiModelProperty("流水线名称", required = true)
    val pipelineName: String,
    @ApiModelProperty("构建ID", required = true)
    val buildId: String,
    @ApiModelProperty("构建序号", required = true)
    val buildNum: Int,
    @ApiModelProperty("代码库地址", required = false)
    val repoUrl: String? = null,
    @ApiModelProperty("代码库分支", required = false)
    val branch: String? = null,
    @ApiModelProperty("启动用户", required = true)
    val startUser: String,
    @ApiModelProperty("执行开始时间", required = true)
    val startTime: String? = null,
    @ApiModelProperty("执行结束时间", required = true)
    val endTime: String? = null,
    @ApiModelProperty("流水线构建耗时", required = true)
    val costTime: Long,
    @ApiModelProperty("是否执行成功", required = true)
    val successFlag: Boolean,
    @ApiModelProperty("错误信息列表", required = false)
    var errorInfos: List<ErrorInfo>? = null,
    @ApiModelProperty("stage指标数据列表", required = true)
    val stages: List<BuildEndStageMetricsData>,
    @ApiModelProperty("渠道代码", required = true)
    val channelCode: String
)
