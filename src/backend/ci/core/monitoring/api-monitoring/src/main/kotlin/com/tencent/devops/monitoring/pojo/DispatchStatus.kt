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
package com.tencent.devops.monitoring.pojo

import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.api.annotation.InfluxTag
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("dispatch状态上报")
data class DispatchStatus(
    @ApiModelProperty("蓝盾项目ID", required = true)
    val projectId: String,
    @ApiModelProperty("流水线ID", required = true)
    val pipelineId: String,
    @ApiModelProperty("构建ID", required = true)
    val buildId: String,
    @ApiModelProperty("vmSeqId", required = true)
    val vmSeqId: String,
    @ApiModelProperty("actionType", required = true)
    val actionType: String,
    @ApiModelProperty("retryCount", required = false)
    val retryCount: Long? = 0,
    @ApiModelProperty("channelCode", required = false)
    val channelCode: ChannelCode?,
    @ApiModelProperty("开机时间", required = true)
    val startTime: Long,
    @ApiModelProperty("关机时间", required = false)
    val stopTime: Long?,
    @ApiModelProperty("蓝盾错误码", required = true)
    val errorCode: String,
    @ApiModelProperty("失败原因", required = false)
    val errorMsg: String?,
    @ApiModelProperty("错误类型", required = false)
    val errorType: String? = ErrorType.SYSTEM.name,
    @InfluxTag
    @ApiModelProperty("BuildType", required = false)
    val buildType: String
)
