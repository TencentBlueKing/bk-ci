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
import io.swagger.annotations.ApiModelProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class FilterHostInfo(
    @ApiModelProperty(value = "管控区域ID")
    val bkCloudId: Int?,
    @ApiModelProperty(value = "业务ID")
    val bkBizId: Int?,
    @ApiModelProperty(value = "主机ID")
    val bkHostId: Long?,
    @ApiModelProperty(value = "主机名")
    val bkHostName: String?,
    @ApiModelProperty(value = "Agent ID")
    val bkAgentId: String?,
    @ApiModelProperty(value = "寻址方式，1: 0，静态 2: 1，动态")
    val bkAddressing: String?,
    @ApiModelProperty(value = "操作系统，1：LINUX 2：WINDOWS 3：AIX 4：SOLARIS")
    val osType: String?,
    @ApiModelProperty(value = "主机内网IPV4地址")
    val innerIp: String?,
    @ApiModelProperty(value = "主机内网IPV6地址")
    val innerIpv6: String?,
    @ApiModelProperty(value = "外网IPv4地址")
    val outerIp: String?,
    @ApiModelProperty(value = "外网IPv6地址")
    val outerIpv6: String?,
    @ApiModelProperty(value = "接入点ID")
    val apId: Int?,
    @ApiModelProperty(value = "登录IP")
    val loginIp: String?,
    @ApiModelProperty(value = "数据IP")
    val dataIp: String?,
    @ApiModelProperty(value = "任务执行状态")
    val status: String?,
    @ApiModelProperty(value = "版本")
    val version: String?,
    @ApiModelProperty(value = "创建时间")
    val createdAt: String?,
    @ApiModelProperty(value = "更新时间")
    val updatedAt: String?,
    @ApiModelProperty(value = "是否手动模式")
    val isManual: Boolean?,
    @ApiModelProperty(value = "额外信息")
    val extraData: ExtraData?,
    @ApiModelProperty(value = "任务执行状态名称")
    val statusDisplay: String?,
    @ApiModelProperty(value = "管控区域名称")
    val bkCloudName: String?,
    @ApiModelProperty(value = "安装通道名称")
    val installChannelName: String?,
    @ApiModelProperty(value = "业务名称")
    val bkBizName: String?,
    @ApiModelProperty(value = "鉴权信息")
    val identityInfo: IdentityInfo?,
    @ApiModelProperty(value = "")
    val jobResult: JobResultForFilterHostInfo?,
    @ApiModelProperty(value = "拓扑信息")
    val topology: List<String>?,
    @ApiModelProperty(value = "是否具有操作权限")
    val operatePermission: Boolean?
)