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
 * 蓝鲸企业版/开源版专用Url实现
 */
class BluekingAgentUrlServiceImpl constructor(
    private val commonConfig: CommonConfig
) : AgentUrlService {

    override fun genAgentInstallUrl(agentRecord: TEnvironmentThirdpartyAgentRecord): String {
        val gw = genGateway(agentRecord)
        val agentHashId = HashUtil.encodeLongId(agentRecord.id)
        return "$gw/ms/environment/api/external/thirdPartyAgent/$agentHashId/install"
    }

    override fun genAgentUrl(agentRecord: TEnvironmentThirdpartyAgentRecord): String {
        val gw = genGateway(agentRecord)
        val agentHashId = HashUtil.encodeLongId(agentRecord.id)
        return if (agentRecord.os == OS.WINDOWS.name) {
            // windows下不需要区分架构，删除arch
            "$gw/ms/environment/api/external/thirdPartyAgent/$agentHashId/agent"
        } else {
            "$gw/ms/environment/api/external/thirdPartyAgent/$agentHashId/agent?arch=\${ARCH}"
        }
    }

    override fun genAgentInstallScript(agentRecord: TEnvironmentThirdpartyAgentRecord): String {
        val installUrl = genAgentInstallUrl(agentRecord)
        return if (agentRecord.os != OS.WINDOWS.name) {
            "curl -H \"$AUTH_HEADER_DEVOPS_PROJECT_ID: ${agentRecord.projectId}\" $installUrl | bash"
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
            fixGateway(agentRecord.fileGateway)
        }
    }

    override fun fixGateway(gateway: String?): String {
        val gw = if (gateway.isNullOrBlank()) commonConfig.devopsBuildGateway else gateway
        return if (gw!!.startsWith("http")) {
            gw.removeSuffix("/")
        } else {
            "http://$gw"
        }
    }
}
