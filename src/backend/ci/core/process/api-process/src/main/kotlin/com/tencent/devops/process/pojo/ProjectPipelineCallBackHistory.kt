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

package com.tencent.devops.process.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("项目的流水线回调历史")
data class ProjectPipelineCallBackHistory(
    @ApiModelProperty("流水线id", required = false)
    val id: Long? = null,
    @ApiModelProperty("项目id", required = false)
    val projectId: String,
    @ApiModelProperty("回调url地址", required = false)
    val callBackUrl: String,
    @ApiModelProperty("事件", required = false)
    val events: String,
    @ApiModelProperty("状态", required = false)
    val status: String,
    @ApiModelProperty("请求header", required = false)
    val requestHeaders: List<CallBackHeader>? = null,
    @ApiModelProperty("请求body", required = false)
    val requestBody: String,
    @ApiModelProperty("响应状态码", required = false)
    val responseCode: Int? = null,
    @ApiModelProperty("响应body", required = false)
    val responseBody: String? = null,
    @ApiModelProperty("错误信息", required = false)
    val errorMsg: String? = null,
    @ApiModelProperty("创建时间", required = false)
    val createdTime: Long? = null,
    @ApiModelProperty("开始时间", required = false)
    val startTime: Long,
    @ApiModelProperty("结束时间", required = false)
    val endTime: Long
)

@ApiModel("回调header 模型")
data class CallBackHeader(
    @ApiModelProperty("名字", required = false)
    val name: String,
    @ApiModelProperty("值", required = false)
    val value: String
)
