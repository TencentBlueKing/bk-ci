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

import com.tencent.devops.monitoring.pojo.annotions.InfluxTag
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("scm调用工蜂addCommitCheckStatus接口的状态上报")
data class AddCommitCheckStatus(
    @ApiModelProperty("请求时间(时间戳，毫秒)", required = true)
    val requestTime: Long,
    @ApiModelProperty("响应时间(时间戳，毫秒)", required = true)
    val responseTime: Long,
    @ApiModelProperty("耗时(毫秒)", required = true)
    val elapseTime: Long,
    @ApiModelProperty("http状态码", required = false)
    val statusCode: String?,
    @ApiModelProperty("状态码对应的错误信息", required = false)
    val statusMessage: String?,
    @ApiModelProperty("错误类型", required = true)
    val errorType: String? = null,
    @ApiModelProperty("蓝盾错误码", required = true)
    @InfluxTag
    val errorCode: String,
    @ApiModelProperty("错误信息", required = false)
    val errorMsg: String?,
    @ApiModelProperty("工蜂项目名", required = false)
    val projectName: String,
    @ApiModelProperty("commitId", required = false)
    val commitId: String,
    @ApiModelProperty("block", required = false)
    val block: Boolean? = null,
    @ApiModelProperty("详情url", required = false)
    val targetUrl: String? = null,
    @ApiModelProperty("渠道", required = false)
    val channel: String? = null
)
