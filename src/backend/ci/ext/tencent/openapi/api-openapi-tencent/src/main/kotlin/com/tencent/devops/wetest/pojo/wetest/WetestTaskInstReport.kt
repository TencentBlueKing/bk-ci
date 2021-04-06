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

package com.tencent.devops.wetest.pojo.wetest

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("WeTest测试实例")
data class WetestTaskInstReport(
    @ApiModelProperty("test id")
    val testId: String,
    @ApiModelProperty("项目id")
    val projectId: String,
    @ApiModelProperty("流水线id")
    val pipelineId: String,
    @ApiModelProperty("流水线名称")
    val pipelineName: String,
    @ApiModelProperty("构建id")
    val buildId: String,
    @ApiModelProperty("构建号")
    val buildNo: Int,
    @ApiModelProperty("名称")
    val name: String,
    @ApiModelProperty("包版本")
    val version: String,
    @ApiModelProperty("通过率")
    val passingRate: String,
    @ApiModelProperty("对应的任务设置id")
    val taskId: String,
    @ApiModelProperty("测试类型")
    val testType: String,
    @ApiModelProperty("脚本类型")
    val scriptType: String,
    @ApiModelProperty("是否是同步: 0-异步 1-同步")
    val synchronized: String,
    @ApiModelProperty("凭证id")
    val ticketId: String?,
    @ApiModelProperty("启动用户")
    val startUserId: String,
    @ApiModelProperty("状态")
    val status: String,
    @ApiModelProperty("开始时间")
    val beginTime: Long,
    @ApiModelProperty("结束时间")
    val endTime: Long? = null
)
