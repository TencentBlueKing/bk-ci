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

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "安装agent的返回结果中的 主机信息")
data class AgentIpFilter(
    @get:Schema(title = "主机业务ID")
    @JsonProperty("bk_biz_id")
    val bkBizId: Int,
    @get:Schema(title = "主机业务名称")
    @JsonProperty("bk_biz_name")
    val bkBizName: String,
    @get:Schema(title = "IP地址")
    val ip: String,
    @get:Schema(title = "内网IPV4地址")
    @JsonProperty("inner_ip")
    val innerIp: String,
    @get:Schema(title = "内网IPV6地址")
    @JsonProperty("inner_ipv6")
    val innerIpv6: String,
    @get:Schema(title = "主机ID")
    @JsonProperty("bk_host_id")
    val bkHostId: Int,
    @get:Schema(title = "管控区域名称")
    @JsonProperty("bk_cloud_name")
    val bkCloudName: String,
    @get:Schema(title = "管控区域ID")
    @JsonProperty("bk_cloud_id")
    val bkCloudId: Int,
    @get:Schema(title = "执行状态")
    val status: String,
    @get:Schema(title = "作业ID")
    @JsonProperty("job_id")
    val jobId: Int,
    @get:Schema(title = "过滤原因")
    val exception: String,
    @get:Schema(title = "失败的具体信息")
    val msg: String
)