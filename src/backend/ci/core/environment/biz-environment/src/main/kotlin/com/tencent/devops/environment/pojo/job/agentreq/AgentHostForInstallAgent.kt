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

package com.tencent.devops.environment.pojo.job.agentreq

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "安装agent的请求信息中的 host信息")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class AgentHostForInstallAgent(
    @get:Schema(title = "业务ID", required = true)
    @JsonProperty("bk_biz_id")
    val bkBizId: Int,
    @get:Schema(title = "管控区域ID", required = true)
    @JsonProperty("bk_cloud_id")
    val bkCloudId: Int,
    @get:Schema(title = "主机ID")
    @JsonProperty("bk_host_id")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val bkHostId: Int?,
    @get:Schema(title = "寻址方式，1: 0，静态 2: 1，动态")
    @JsonProperty("bk_addressing")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val bkAddressing: String?,
    @get:Schema(title = "接入点ID", required = true)
    @JsonProperty("ap_id")
    val apId: Int,
    @get:Schema(title = "安装通道ID")
    @JsonProperty("install_channel_id")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val installChannelId: Int?,
    @get:Schema(title = "内网IPV4地址，inner_ip和inner_ipv6必选其一")
    @JsonProperty("inner_ip")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val innerIp: String?,
    @get:Schema(title = "外网IP")
    @JsonProperty("outer_ip")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val outerIp: String?,
    @get:Schema(title = "登录IP")
    @JsonProperty("login_ip")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val loginIp: String?,
    @get:Schema(title = "数据IP")
    @JsonProperty("data_ip")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val dataIp: String?,
    @get:Schema(title = "内网IPv6")
    @JsonProperty("inner_ipv6")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val innerIpv6: String?,
    @get:Schema(title = "外网IPv6")
    @JsonProperty("outer_ipv6")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val outerIpv6: String?,
    @get:Schema(title = "操作系统，1：LINUX 2：WINDOWS 3：AIX 4：SOLARIS", required = true)
    @JsonProperty("os_type")
    val osType: String?,
    @get:Schema(title = "认证类型，1：PASSWORD，密码认证 2: KEY，秘钥认证 3：TJJ_PASSWORD，默认为密码认证")
    @JsonProperty("auth_type")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val authType: String?,
    @get:Schema(title = "账户")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val account: String?,
    @get:Schema(title = "密码")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val password: String?,
    @get:Schema(title = "端口")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val port: String?,
    @get:Schema(title = "密钥")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val key: String?,
    @get:Schema(title = "是否手动模式")
    @JsonProperty("is_manual")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val isManual: Boolean?,
    @get:Schema(title = "密码保留天数，默认保留一天")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val retention: Int?,
    @get:Schema(title = "加速设置，默认为0")
    @JsonProperty("peer_exchange_switch_for_agent")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val peerExchangeSwitchForAgent: Int?,
    @get:Schema(title = "传输限速")
    @JsonProperty("bt_speed_limit")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val btSpeedLimit: String?,
    @get:Schema(title = "数据压缩开关，默认是关闭")
    @JsonProperty("enable_compression")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val enableCompression: Boolean?,
    @get:Schema(title = "数据文件路径")
    @JsonProperty("data_path")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val dataPath: String?
)