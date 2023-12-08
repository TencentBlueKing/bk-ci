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
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("安装agent的请求信息中的 host信息")
data class AgentHostForInstallAgent(
    @ApiModelProperty(value = "业务ID", required = true)
    @JsonProperty("bk_biz_id")
    val bkBizId: Int,
    @ApiModelProperty(value = "管控区域ID", required = true)
    @JsonProperty("bk_cloud_id")
    val bkCloudId: Int,
    @ApiModelProperty(value = "主机ID")
    @JsonProperty("bk_host_id")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val bkHostId: Int?,
    @ApiModelProperty(value = "寻址方式，1: 0，静态 2: 1，动态")
    @JsonProperty("bk_addressing")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val bkAddressing: Int?,
    @ApiModelProperty(value = "接入点ID")
    @JsonProperty("ap_id")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val apId: Int?,
    @ApiModelProperty(value = "安装通道ID")
    @JsonProperty("install_channel_id")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val installChannelId: Int?,
    @ApiModelProperty(value = "内网IPV4地址，inner_ip和inner_ipv6必选其一")
    @JsonProperty("inner_ip")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val innerIp: String?,
    @ApiModelProperty(value = "外网IP")
    @JsonProperty("outer_ip")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val outerIp: String?,
    @ApiModelProperty(value = "登录IP")
    @JsonProperty("login_ip")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val loginIp: String?,
    @ApiModelProperty(value = "数据IP")
    @JsonProperty("data_ip")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val dataIp: String?,
    @ApiModelProperty(value = "内网IPv6")
    @JsonProperty("inner_ipv6")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val innerIpv6: String?,
    @ApiModelProperty(value = "外网IPv6")
    @JsonProperty("outer_ipv6")
    val outerIpv6: String?,
    @ApiModelProperty(value = "操作系统，1：LINUX 2：WINDOWS 3：AIX 4：SOLARIS")
    @JsonProperty("os_type")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val osType: String?,
    @ApiModelProperty(value = "认证类型，1：PASSWORD，密码认证 2: KEY，秘钥认证 3：TJJ_PASSWORD，默认为密码认证")
    @JsonProperty("auth_type")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val authType: String?,
    @ApiModelProperty(value = "账户")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val account: String?,
    @ApiModelProperty(value = "密码")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val password: String?,
    @ApiModelProperty(value = "端口")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val port: String?,
    @ApiModelProperty(value = "密钥")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val key: String?,
    @ApiModelProperty(value = "是否手动模式")
    @JsonProperty("is_manual")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val isManual: Boolean?,
    @ApiModelProperty(value = "密码保留天数，默认保留一天")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val retention: Int?,
    @ApiModelProperty(value = "加速设置，默认为0")
    @JsonProperty("peer_exchange_switch_for_agent")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val peerExchangeSwitchForAgent: Int?,
    @ApiModelProperty(value = "传输限速")
    @JsonProperty("bt_speed_limit")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val btSpeedLimit: String?,
    @ApiModelProperty(value = "数据压缩开关，默认是关闭")
    @JsonProperty("enable_compression")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val enableCompression: Boolean?,
    @ApiModelProperty(value = "数据文件路径")
    @JsonProperty("data_path")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val dataPath: String?
)