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

package com.tencent.devops.dispatch.pojo

import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.dispatch.pojo.enums.JobQuotaVmType
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("系统默认JOB配额")
data class JobQuotaSystem(
    @ApiModelProperty("构建机类型", required = true)
    val vmType: JobQuotaVmType,
    @ApiModelProperty("构建来源，默认BS", required = true)
    val channelCode: String = ChannelCode.BS.name,
    @ApiModelProperty("蓝盾系统最大并发JOB数，默认5000", required = false)
    val runningJobMaxSystem: Int,
    @ApiModelProperty("单项目默认最大并发JOB数， 默认500", required = false)
    val runningJobMaxProject: Int,
    @ApiModelProperty("系统默认所有单个JOB最大执行时间，默认24小时", required = false)
    val runningTimeJobMax: Int,
    @ApiModelProperty("默认单项目所有JOB最大执行时间，默认5000小时/月", required = false)
    val runningTimeJobMaxProject: Int,
    @ApiModelProperty("项目执行job数量告警阈值，百分比，默认80", required = false)
    val projectRunningJobThreshold: Int,
    @ApiModelProperty("项目执行job时间告警阈值，百分比，默认80", required = false)
    val projectRunningTimeThreshold: Int,
    @ApiModelProperty("系统执行job数量告警阈值，百分比，默认80", required = false)
    val systemRunningJobThreshold: Int,
    @ApiModelProperty("创建时间", required = false)
    val createdTime: String,
    @ApiModelProperty("修改时间", required = false)
    val updatedTime: String,
    @ApiModelProperty("操作人", required = false)
    val operator: String
)
