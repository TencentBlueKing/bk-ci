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

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "安装agent的请求信息中的 host信息")
data class HostForInstallAgent(
    @get:Schema(title = "hostID")
    val bkHostId: Int?,
    @get:Schema(title = "管控区域ID")
    val bkCloudId: Int?,
    @get:Schema(title = "是否自动选择安装通道，true-自动选择，false-读取installChannelId字段", required = true)
    val isAutoChooseInstallChannelId: Boolean = true,
    @get:Schema(title = "安装通道ID")
    val installChannelId: Int?,
    @get:Schema(title = "内网IPV4地址，inner_ip和inner_ipv6必选其一")
    val innerIp: String?,
    @get:Schema(title = "操作系统，1：LINUX 2：WINDOWS 3：AIX 4：SOLARIS")
    val osType: String?,
    @get:Schema(title = "认证类型，1：PASSWORD，密码认证 2: KEY，秘钥认证 3：TJJ_PASSWORD，默认为密码认证")
    val authType: String?,
    @get:Schema(title = "账户")
    val account: String?,
    @get:Schema(title = "密码")
    val password: String?,
    @get:Schema(title = "密钥")
    val key: String?
)