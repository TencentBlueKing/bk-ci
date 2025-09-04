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

import com.tencent.devops.common.pipeline.enums.ChannelCode
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "project接口，users接口的状态上报")
data class UsersStatus(
    @get:Schema(title = "蓝盾项目ID", required = false)
    val projectId: String?,
    @get:Schema(title = "流水线ID", required = false)
    val pipelineId: String?,
    @get:Schema(title = "构建ID", required = false)
    val buildId: String?,
    @get:Schema(title = "vmSeqId", required = false)
    val vmSeqId: String?,
    @get:Schema(title = "channelCode", required = false)
    val channelCode: ChannelCode?,
    @get:Schema(title = "请求时间(时间戳，毫秒)", required = true)
    val requestTime: Long,
    @get:Schema(title = "响应时间(时间戳，毫秒)", required = true)
    val responseTime: Long,
    @get:Schema(title = "耗时(毫秒)", required = true)
    val elapseTime: Long,
    @get:Schema(title = "Http状态码", required = false)
    val statusCode: String?,
    @get:Schema(title = "状态码对应的错误信息", required = false)
    val statusMessage: String?,
    @get:Schema(title = "蓝盾错误码", required = true)
    val errorCode: String,
    @get:Schema(title = "错误信息", required = false)
    val errorMsg: String?
)
