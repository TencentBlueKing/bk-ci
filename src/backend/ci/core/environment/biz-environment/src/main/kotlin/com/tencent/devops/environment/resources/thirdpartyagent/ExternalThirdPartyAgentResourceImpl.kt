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

package com.tencent.devops.environment.resources.thirdpartyagent

import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.pojo.agent.AgentArchType
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.environment.api.thirdpartyagent.ExternalThirdPartyAgentResource
import com.tencent.devops.environment.service.thirdpartyagent.BatchInstallAgentService
import com.tencent.devops.environment.service.thirdpartyagent.DownloadAgentInstallService
import com.tencent.devops.environment.service.thirdpartyagent.ImportService
import org.springframework.beans.factory.annotation.Autowired
import javax.ws.rs.core.Response

@RestResource
class ExternalThirdPartyAgentResourceImpl @Autowired constructor(
    private val downloadAgentInstallService: DownloadAgentInstallService,
    private val importService: ImportService,
    private val batchInstallAgentService: BatchInstallAgentService
) : ExternalThirdPartyAgentResource {
    override fun downloadAgentInstallScript(agentId: String) =
        downloadAgentInstallService.downloadInstallScript(agentId, false)

    override fun downloadAgent(agentId: String, eTag: String?, arch: String?) =
        downloadAgentInstallService.downloadAgent(
            agentId = agentId,
            arch = when (arch) {
                "arm64" -> AgentArchType.ARM64
                "mips64" -> AgentArchType.MIPS64
                else -> null
            }
        )

    override fun downloadJRE(agentId: String, eTag: String?, arch: String?) =
        downloadAgentInstallService.downloadJre(
            agentId, eTag,
            arch = when (arch) {
                "arm64" -> AgentArchType.ARM64
                "mips64" -> AgentArchType.MIPS64
                else -> null
            }
        )

    override fun downloadNewInstallAgentBatchFile(agentHashId: String): Response {
        val newAgentId = importService.generateAgentByOtherAgentId(agentHashId)
        return downloadAgentInstallService.downloadInstallAgentBatchFile(newAgentId)
    }

    override fun batchDownloadAgentInstallScript(token: String, os: OS, zoneName: String?): Response {
        return batchInstallAgentService.genAgentInstallScript(
            token = token,
            os = os,
            zoneName = zoneName
        )
    }
}
