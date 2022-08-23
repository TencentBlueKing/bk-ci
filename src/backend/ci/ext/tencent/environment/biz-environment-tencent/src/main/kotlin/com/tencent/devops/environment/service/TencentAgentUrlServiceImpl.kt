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

package com.tencent.devops.environment.service

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PROJECT_ID
import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.model.environment.tables.records.TEnvironmentThirdpartyAgentRecord

/**
 * 腾讯内部旧版专用Agent下载链接生成服务
 */
open class TencentAgentUrlServiceImpl constructor(
    private val commonConfig: CommonConfig
) : AgentUrlService {

    override fun genAgentInstallUrl(agentRecord: TEnvironmentThirdpartyAgentRecord): String {
        val gw = genGateway(agentRecord)
        val agentHashId = HashUtil.encodeLongId(agentRecord.id)
        return "http://$gw/external/agents/$agentHashId/install"
    }

    override fun genAgentUrl(agentRecord: TEnvironmentThirdpartyAgentRecord): String {
        val gw = genGateway(agentRecord)
        val agentHashId = HashUtil.encodeLongId(agentRecord.id)
        // --story=869226483 蓝盾网站启用woa及https  原因： woa切换后的过渡的临时方案
        // 1、目前已经全面https + woa，但windows需要在页面上下载，如果非https，则会被浏览器限制不安全内容而无法下载
        // 2、公司devnet区域有的Linux/Mac构建机无法访问 https协议 的域名, 等解决。。。
        return if (agentRecord.os == OS.WINDOWS.name) {
            val wUrl = gw.replace(".oa.", ".woa.")
            "https://$wUrl/external/agents/$agentHashId/agent?arch=\${ARCH}"
        } else {
            "http://$gw/external/agents/$agentHashId/agent?arch=\${ARCH}"
        }
    }

    override fun genAgentInstallScript(agentRecord: TEnvironmentThirdpartyAgentRecord): String {
        val url = genAgentInstallUrl(agentRecord)
        return if (agentRecord.os != OS.WINDOWS.name) {
            "curl -H \"$AUTH_HEADER_DEVOPS_PROJECT_ID: ${agentRecord.projectId}\" $url | bash"
        } else {
            ""
        }
    }

    override fun genGateway(agentRecord: TEnvironmentThirdpartyAgentRecord): String {
        return fixGateway(agentRecord.gateway)
    }

    override fun genFileGateway(agentRecord: TEnvironmentThirdpartyAgentRecord): String {
        return if (agentRecord.fileGateway.isNullOrBlank()) {
            genGateway(agentRecord)
        } else {
            agentRecord.fileGateway.removePrefix("https://").removePrefix("http://").removeSuffix("/")
        }
    }

    override fun fixGateway(gateway: String?): String {
        val gw = if (gateway.isNullOrBlank()) commonConfig.devopsBuildGateway!! else gateway!!
        return gw.removePrefix("https://").removePrefix("http://").removeSuffix("/")
    }
}
