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

package com.tencent.devops.environment.pojo.cmdb.resp

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.environment.pojo.CmdbNode
import io.swagger.v3.oas.annotations.media.Schema

data class NewCmdbServer(
    @get:Schema(title = "服务器ID")
    val serverId: Long,
    @get:Schema(title = "主负责人")
    val maintainer: String?,
    @get:Schema(title = "备份负责人(多个人用;分隔)")
    val maintainerBak: String?,
    @get:Schema(title = "运维部门ID")
    @JsonProperty("maintenanceDepartmentId")
    val departmentId: Int?,
    @get:Schema(title = "主机名称")
    val hostName: String?,
    @get:Schema(title = "操作系统名称")
    val osName: String?,
    @get:Schema(title = "服务器的内网Ipv4地址列表")
    val innerServerIpv4: List<NewCmdbInnerServerIpv4>?
) {

    fun getFirstIp(): String? {
        return innerServerIpv4?.get(0)?.ip
    }

    private fun getDisplayIp(): String? {
        if (innerServerIpv4 == null) {
            return null
        }
        return innerServerIpv4.joinToString(";")
    }

    fun toCmdbNode(): CmdbNode {
        return CmdbNode(
            name = hostName ?: "",
            serverId = serverId,
            operator = maintainer ?: "",
            bakOperator = maintainerBak ?: "",
            ip = getFirstIp() ?: "",
            displayIp = getDisplayIp() ?: "",
            osName = osName ?: ""
        )
    }
}
