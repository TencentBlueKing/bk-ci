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

data class AgentFilterHostInfo(
    @get:Schema(title = "管控区域ID")
    @JsonProperty("bk_cloud_id")
    val bkCloudId: Int?,
    @get:Schema(title = "业务ID")
    @JsonProperty("bk_biz_id")
    val bkBizId: Int?,
    @get:Schema(title = "主机ID")
    @JsonProperty("bk_host_id")
    val bkHostId: Long?,
    @get:Schema(title = "主机名")
    @JsonProperty("bk_host_name")
    val bkHostName: String?,
    @get:Schema(title = "Agent ID")
    @JsonProperty("bk_agent_id")
    val bkAgentId: String?,
    @get:Schema(title = "寻址方式，1: 0，静态 2: 1，动态")
    @JsonProperty("bk_addressing")
    val bkAddressing: String?,
    @get:Schema(title = "操作系统，1：LINUX 2：WINDOWS 3：AIX 4：SOLARIS")
    @JsonProperty("os_type")
    val osType: String?,
    @get:Schema(title = "主机内网IPV4地址")
    @JsonProperty("inner_ip")
    val innerIp: String?,
    @get:Schema(title = "主机内网IPV6地址")
    @JsonProperty("inner_ipv6")
    val innerIpv6: String?,
    @get:Schema(title = "外网IPv4地址")
    @JsonProperty("outer_ip")
    val outerIp: String?,
    @get:Schema(title = "外网IPv6地址")
    @JsonProperty("outer_ipv6")
    val outerIpv6: String?,
    @get:Schema(title = "接入点ID")
    @JsonProperty("ap_id")
    val apId: Int?,
    @get:Schema(title = "安装通道ID")
    @JsonProperty("install_channel_id")
    val installChannelId: Int?,
    @get:Schema(title = "登录IP")
    @JsonProperty("login_ip")
    val loginIp: String?,
    @get:Schema(title = "数据IP")
    @JsonProperty("data_ip")
    val dataIp: String?,
    @get:Schema(title = "任务执行状态")
    val status: String?,
    @get:Schema(title = "版本")
    val version: String?,
    @get:Schema(title = "创建时间")
    @JsonProperty("created_at")
    val createdAt: String?,
    @get:Schema(title = "更新时间")
    @JsonProperty("updated_at")
    val updatedAt: String?,
    @get:Schema(title = "是否手动模式")
    @JsonProperty("is_manual")
    val isManual: Boolean?,
    @get:Schema(title = "额外信息")
    @JsonProperty("extra_data")
    val extraData: AgentExtraData?,
    @get:Schema(title = "任务执行状态名称")
    @JsonProperty("status_display")
    val statusDisplay: String?,
    @get:Schema(title = "管控区域名称")
    @JsonProperty("bk_cloud_name")
    val bkCloudName: String?,
    @get:Schema(title = "安装通道名称")
    @JsonProperty("install_channel_name")
    val installChannelName: String?,
    @get:Schema(title = "业务名称")
    @JsonProperty("bk_biz_name")
    val bkBizName: String?,
    @get:Schema(title = "鉴权信息")
    @JsonProperty("identity_info")
    val identityInfo: AgentIdentityInfo?,
    @get:Schema(title = "")
    @JsonProperty("job_result")
    val jobResult: AgentJobResultForFilterHostInfo?,
    @get:Schema(title = "拓扑信息")
    val topology: List<String>?,
    @get:Schema(title = "是否具有操作权限")
    @JsonProperty("operate_permission")
    val operatePermission: Boolean?
)