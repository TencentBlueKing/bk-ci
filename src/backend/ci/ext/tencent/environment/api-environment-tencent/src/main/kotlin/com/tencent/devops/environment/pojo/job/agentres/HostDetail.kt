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

package com.tencent.devops.environment.pojo.job.agentres

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.v3.oas.annotations.media.Schema

@JsonIgnoreProperties(ignoreUnknown = true)
data class HostDetail(
    @get:Schema(title = "是否存在过滤的主机")
    val filterHost: Boolean?,
    @get:Schema(title = "主机ID")
    val bkHostId: Int?,
    @get:Schema(title = "主机IP地址")
    val ip: String,
    @get:Schema(title = "主机内网IPV4地址")
    val innerIp: String?,
    @get:Schema(title = "实例ID")
    val instanceId: String?,
    @get:Schema(title = "主机内网IPV6地址")
    val innerIpv6: String?,
    @get:Schema(title = "管控区域ID")
    val bkCloudId: Int?,
    @get:Schema(title = "管控区域名称")
    val bkCloudName: String?,
    @get:Schema(title = "业务ID")
    val bkBizId: Int?,
    @get:Schema(title = "业务名称")
    val bkBizName: String?,
    @get:Schema(title = "任务ID")
    val jobId: Int?,
    @get:Schema(title = "任务执行状态")
    val status: String?,
    @get:Schema(title = "任务执行状态名称")
    val statusDisplay: String?
)