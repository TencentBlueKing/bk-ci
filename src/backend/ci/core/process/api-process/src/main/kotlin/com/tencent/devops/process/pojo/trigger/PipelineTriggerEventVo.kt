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
 *
 */

package com.tencent.devops.process.pojo.trigger

import io.swagger.annotations.ApiModelProperty

data class PipelineTriggerEventVo(
    @ApiModelProperty("事件明细ID")
    var detailId: Long,
    @ApiModelProperty("蓝盾项目ID")
    val projectId: String,
    @ApiModelProperty("事件ID")
    val eventId: Long,
    @ApiModelProperty("触发类型")
    val triggerType: String,
    @ApiModelProperty("事件触发源,代码库触发-代码库ID", required = false)
    val eventSource: String? = "",
    @ApiModelProperty("事件类型")
    val eventType: String,
    @ApiModelProperty("触发人")
    val triggerUser: String,
    @ApiModelProperty("事件描述")
    var eventDesc: String,
    @ApiModelProperty("事件时间")
    val eventTime: Long,
    @ApiModelProperty("触发状态")
    var status: String,
    @ApiModelProperty("流水线Id")
    var pipelineId: String? = null,
    @ApiModelProperty("流水线名称")
    var pipelineName: String? = null,
    @ApiModelProperty("构建Id")
    var buildId: String? = null,
    @ApiModelProperty("构建编号")
    var buildNum: String? = null,
    @ApiModelProperty("原因")
    var reason: String? = null,
    @ApiModelProperty("失败原因详情", required = false)
    var reasonDetailList: List<String>? = null,
    @ApiModelProperty("失败原因", required = false)
    var failReason: String = ""
)
