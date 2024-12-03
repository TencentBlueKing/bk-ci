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

package com.tencent.devops.environment.service.job

import com.tencent.devops.common.api.exception.ResourceNotMatchException
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.environment.constant.EnvironmentMessageCode.ERROR_NODE_NOT_EXISTS
import com.tencent.devops.environment.dao.job.CmdbNodeDao
import com.tencent.devops.environment.pojo.job.agentreq.ApiGwInstallAgentReq
import com.tencent.devops.environment.pojo.job.agentreq.HostForInstallAgent
import com.tencent.devops.environment.pojo.job.agentreq.InstallAgentReq
import com.tencent.devops.environment.pojo.job.agentres.AgentResult
import com.tencent.devops.environment.pojo.job.agentres.InstallAgentResult
import com.tencent.devops.environment.pojo.job.agentres.ObtainManualCommandResult
import com.tencent.devops.environment.service.gseagent.GSEAgentService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service("ApiGwAgentService")
class ApiGwAgentService @Autowired constructor(
    private val cmdbNodeDao: CmdbNodeDao,
    private val gseAgentService: GSEAgentService
) {

    fun installAgent(
        userId: String,
        projectId: String,
        apiGwInstallAgentReq: ApiGwInstallAgentReq,
        hostList: List<Long>
    ): AgentResult<InstallAgentResult> {

        val installAgentReq = apiGwInstallAgentReq.let {
            InstallAgentReq(
                hosts = listOf(
                    HostForInstallAgent(
                        bkHostId = hostList[0].toInt(),
                        bkCloudId = it.bkCloudId,
                        bkAddressing = null,
                        isAutoChooseInstallChannelId = it.isAutoChooseInstallChannelId,
                        apId = null,
                        installChannelId = null,
                        innerIp = it.innerIp,
                        loginIp = null,
                        innerIpv6 = null,
                        osType = it.osType,
                        authType = null,
                        account = null,
                        password = null,
                        key = null,
                        port = null,
                        isManual = true,
                        peerExchangeSwitchForAgent = null,
                        enableCompression = null,
                    )
                ),
                replaceHostId = null,
                isInstallLatestPlugins = null,
            )
        }
        return gseAgentService.installAgent(userId, null, installAgentReq)
    }

    fun getInstallCommand(
        projectId: String,
        jobId: Int,
        cloudAreaId: Int,
        innerIp: String
    ): AgentResult<ObtainManualCommandResult> {
        val hostList = cmdbNodeDao.getNodeHostIdByCloudIp(projectId = null, cloudAreaId = cloudAreaId, ip = innerIp)
        if (hostList.isEmpty()) {
            throw ResourceNotMatchException(
                errorCode = ERROR_NODE_NOT_EXISTS,
                message = I18nUtil.getCodeLanMessage(ERROR_NODE_NOT_EXISTS),
                params = arrayOf("[$projectId]$cloudAreaId:$innerIp")
            )
        }
        return gseAgentService.obtainManualInstallationCommand(jobId, hostList[0])
    }

    fun getNodeHostIdByCloudIp(projectId: String?, cloudAreaId: Int, ip: String): List<Long> {
        return cmdbNodeDao.getNodeHostIdByCloudIp(projectId, cloudAreaId, ip)
    }
}
