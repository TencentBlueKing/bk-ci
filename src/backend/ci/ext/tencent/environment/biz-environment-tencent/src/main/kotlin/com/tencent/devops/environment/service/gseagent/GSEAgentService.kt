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

package com.tencent.devops.environment.service.gseagent

import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.environment.config.EnvironmentProperties
import com.tencent.devops.environment.constant.EnvironmentMessageCode
import com.tencent.devops.environment.pojo.job.agentreq.AgentHostForInstallAgent
import com.tencent.devops.environment.pojo.job.agentreq.AgentInstallAgentReq
import com.tencent.devops.environment.pojo.job.agentreq.InstallAgentReq
import com.tencent.devops.environment.pojo.job.agentreq.RetryAgentInstallTaskNodeManReq
import com.tencent.devops.environment.pojo.job.agentreq.RetryAgentInstallTaskReq
import com.tencent.devops.environment.pojo.job.agentreq.TerminateAgentInstallTaskNodeManReq
import com.tencent.devops.environment.pojo.job.agentreq.TerminateAgentInstallTaskReq
import com.tencent.devops.environment.pojo.job.agentres.AgentInstallChannel
import com.tencent.devops.environment.pojo.job.agentres.AgentOriginalResult
import com.tencent.devops.environment.pojo.job.agentres.AgentResult
import com.tencent.devops.environment.pojo.job.agentres.AgentTerminalAgentInstallTaskResult
import com.tencent.devops.environment.pojo.job.agentres.Content
import com.tencent.devops.environment.pojo.job.agentres.InstallAgentChannel
import com.tencent.devops.environment.pojo.job.agentres.InstallAgentResult
import com.tencent.devops.environment.pojo.job.agentres.IpFilter
import com.tencent.devops.environment.pojo.job.agentres.ManualInstallCommand
import com.tencent.devops.environment.pojo.job.agentres.ObtainManualCommandResult
import com.tencent.devops.environment.pojo.job.agentres.QueryAgentInstallChannelResult
import com.tencent.devops.environment.pojo.job.agentres.RetryAgentInstallTaskResp
import com.tencent.devops.environment.pojo.job.agentres.RetryAgentInstallTaskResult
import com.tencent.devops.environment.pojo.job.agentres.Step
import com.tencent.devops.environment.pojo.job.agentres.TerminalAgentInstallTaskResult
import com.tencent.devops.environment.service.job.ChooseAgentInstallChannelIdService
import com.tencent.devops.environment.service.job.NodeManApi
import com.tencent.devops.environment.service.cc.TencentCCService
import com.tencent.devops.environment.utils.FileUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.InputStream
import javax.ws.rs.core.Response

@Service("GSEAgentService")
data class GSEAgentService @Autowired constructor(
    private val nodeManApi: NodeManApi,
    private val chooseAgentInstallChannelIdService: ChooseAgentInstallChannelIdService,
    private val tencentQueryFromCCService: TencentCCService,
    private val environmentProperties: EnvironmentProperties,
    private val installTaskService: InstallTaskService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(GSEAgentService::class.java)

        private const val DEFAULT_INSTALL_AGENT_JOB_TYPE = "REINSTALL_AGENT"

        // 节点管理预发布/正式环境 apId均固定为1
        private const val DEFAULT_INSTALL_AGENT_AP_ID = 1
        private const val DEFAULT_IS_MANUAL = false
        private const val DEFAULT_CLOUD_ID = 0
        private const val DEFAULT_NOT_INSTALL_LATEST_PLUGINS = false

        const val AGENT_ABNORMAL_NODE_STATUS = 0
        const val AGENT_NORMAL_NODE_STATUS = 1
        const val AGENT_NOT_INSTALLED_TAG = false

        const val NODEMAN_COMMAND_NOT_READY_CODE = 3800015
        const val NODEMAN_LOG_NOT_READY_CODE = 3800007
    }

    /**
     * 安装GSE Agent
     */
    fun installAgent(
        userId: String,
        keyFile: InputStream?,
        installAgentReq: InstallAgentReq
    ): AgentResult<InstallAgentResult> {
        NodeManApi.setNodemanOperationName(::installAgent.name)
        val hostIdList = installAgentReq.hosts.mapNotNull { it.bkHostId }
        val hostIdToBizIdMap = buildHostToBizIdMap(hostIdList)
        val installAgentRequest = AgentInstallAgentReq(
            jobType = DEFAULT_INSTALL_AGENT_JOB_TYPE,
            hosts = installAgentReq.hosts.map {
                AgentHostForInstallAgent(
                    bkBizId = getBizIdOfHost(hostIdToBizIdMap, it.bkHostId),
                    bkCloudId = it.bkCloudId ?: DEFAULT_CLOUD_ID,
                    bkHostId = it.bkHostId,
                    bkAddressing = it.bkAddressing,
                    apId = if (it.apId != null) it.apId else DEFAULT_INSTALL_AGENT_AP_ID,
                    installChannelId = it.installChannelId,
                    innerIp = it.innerIp,
                    outerIp = null,
                    loginIp = it.loginIp,
                    dataIp = null,
                    innerIpv6 = it.innerIpv6,
                    outerIpv6 = null,
                    osType = it.osType,
                    authType = it.authType,
                    account = it.account,
                    password = if (null == it.password && "PASSWORD" == it.authType) {
                        throw ParamBlankException("The password cannot be empty.")
                    } else it.password,
                    port = it.port,
                    key = if ("KEY" == it.authType) FileUtils.convertFileContentToString(keyFile) else it.key,
                    isManual = if (true == it.isManual) it.isManual else DEFAULT_IS_MANUAL,
                    retention = null,
                    peerExchangeSwitchForAgent = it.peerExchangeSwitchForAgent,
                    btSpeedLimit = null,
                    enableCompression = it.enableCompression,
                    dataPath = null
                )
            },
            replaceHostId = installAgentReq.replaceHostId,
            isInstallLatestPlugins = DEFAULT_NOT_INSTALL_LATEST_PLUGINS
        )
        val agentInstallAgentRes = nodeManApi.installAgent(installAgentRequest)
        val installAgentRes: AgentResult<InstallAgentResult> = AgentResult(
            code = agentInstallAgentRes.code,
            result = agentInstallAgentRes.result,
            message = agentInstallAgentRes.message,
            errors = agentInstallAgentRes.errors,
            data = InstallAgentResult(
                jobId = agentInstallAgentRes.data?.jobId,
                jobUrl = agentInstallAgentRes.data?.jobUrl,
                ipFilter = agentInstallAgentRes.data?.ipFilter?.map {
                    IpFilter(
                        bkBizId = it.bkBizId,
                        bkBizName = it.bkBizName,
                        ip = it.ip,
                        innerIp = it.innerIp,
                        innerIpv6 = it.innerIpv6,
                        bkHostId = it.bkHostId,
                        bkCloudName = it.bkCloudName,
                        bkCloudId = it.bkCloudId,
                        status = it.status,
                        jobId = it.jobId,
                        exception = it.exception,
                        msg = it.msg
                    )
                }
            )
        )
        checkInstallAgentTaskCreated(installAgentRes)
        installTaskService.startCheckInstallStatusTask(
            jobId = installAgentRes.data?.jobId!!,
            ipList = installAgentReq.hosts.mapNotNull { it.innerIp }
        )
        return installAgentRes
    }

    private fun checkInstallAgentTaskCreated(installAgentRes: AgentResult<InstallAgentResult>) {
        if (null == installAgentRes.data?.jobId) {
            throw CustomException(
                Response.Status.BAD_REQUEST,
                I18nUtil.getCodeLanMessage(
                    messageCode = EnvironmentMessageCode.ERROR_FAIL_TO_CREATE_AGENT_INSTALL_TASK,
                    params = arrayOf(installAgentRes.message ?: "")
                )
            )
        }
    }

    private fun buildHostToBizIdMap(hostIdList: List<Int>): Map<Int, Int> {
        val hostBizRelationList = tencentQueryFromCCService.queryCCFindHostBizRelations(
            hostIdList
        ).data
        return hostBizRelationList?.associate {
            it.bkHostId to it.bkBizId
        } ?: emptyMap()
    }

    private fun getBizIdOfHost(hostIdToBizIdMap: Map<Int, Int>, hostId: Int?): Int {
        if (hostId == null) {
            return 0
        }
        return hostIdToBizIdMap[hostId] ?: environmentProperties.cc.bkBizScopeId
    }

    /**
     * 终止agent安装任务
     */
    fun terminalAgentInstallTask(
        jobId: Int,
        terminateAgentInstallTaskReq: TerminateAgentInstallTaskReq
    ): AgentResult<TerminalAgentInstallTaskResult> {
        NodeManApi.setNodemanOperationName(::terminalAgentInstallTask.name)
        val terminateAgentInstallTaskNodeManReq = TerminateAgentInstallTaskNodeManReq(
            instanceIdList = terminateAgentInstallTaskReq.instanceIdList
        )
        val agentTrmAgentInstallTaskRes: AgentOriginalResult<AgentTerminalAgentInstallTaskResult> =
            nodeManApi.terminateAgentInstallTask(jobId, terminateAgentInstallTaskNodeManReq)
        val termAgentInstallTaskRes: AgentResult<TerminalAgentInstallTaskResult> = AgentResult(
            code = agentTrmAgentInstallTaskRes.code,
            result = agentTrmAgentInstallTaskRes.result,
            message = agentTrmAgentInstallTaskRes.message,
            errors = agentTrmAgentInstallTaskRes.errors,
            data = agentTrmAgentInstallTaskRes.data?.let {
                TerminalAgentInstallTaskResult(
                    taskIdList = it.taskIdList
                )
            }
        )
        return termAgentInstallTaskRes
    }

    /**
     * 重试agent安装任务
     */
    fun retryAgentInstallTask(
        jobId: Int,
        retryAgentInstallTaskReq: RetryAgentInstallTaskReq
    ): AgentResult<RetryAgentInstallTaskResult> {
        NodeManApi.setNodemanOperationName(::retryAgentInstallTask.name)
        val retryAgentInstallTaskNodeManReq = RetryAgentInstallTaskNodeManReq(
            instanceIdList = retryAgentInstallTaskReq.instanceIdList
        )
        val agentRetryAgentInstallTaskRes: AgentOriginalResult<RetryAgentInstallTaskResp> =
            nodeManApi.retryAgentInstallTask(jobId, retryAgentInstallTaskNodeManReq)
        val retryAgentInstallTaskRes: AgentResult<RetryAgentInstallTaskResult> = AgentResult(
            code = agentRetryAgentInstallTaskRes.code,
            result = agentRetryAgentInstallTaskRes.result,
            message = agentRetryAgentInstallTaskRes.message,
            errors = agentRetryAgentInstallTaskRes.errors,
            data = agentRetryAgentInstallTaskRes.data?.let {
                RetryAgentInstallTaskResult(
                    taskIdList = it.taskIdList
                )
            }
        )
        return retryAgentInstallTaskRes
    }

    fun queryAgentInstallChannel(withHidden: Boolean): AgentResult<QueryAgentInstallChannelResult> {
        NodeManApi.setNodemanOperationName(::queryAgentInstallChannel.name)
        val agentInstallChannelResp: AgentOriginalResult<Array<AgentInstallChannel>> =
            nodeManApi.queryAgentInstallChannel(withHidden)
        val queryAgentInsChannelRes: AgentResult<QueryAgentInstallChannelResult> = AgentResult(
            code = agentInstallChannelResp.code,
            result = agentInstallChannelResp.result,
            message = agentInstallChannelResp.message,
            errors = agentInstallChannelResp.errors,
            data = agentInstallChannelResp.data?.let {
                QueryAgentInstallChannelResult(
                    installChannelList = it.map { installChannel ->
                        InstallAgentChannel(
                            id = installChannel.id,
                            name = installChannel.name,
                            bkCloudId = installChannel.bkCloudId,
                            hidden = installChannel.hidden
                        )
                    }
                )
            }
        )
        return queryAgentInsChannelRes
    }

    fun obtainManualInstallationCommand(jobId: Int, hostId: Long): AgentResult<ObtainManualCommandResult> {
        NodeManApi.setNodemanOperationName(::obtainManualInstallationCommand.name)
        val manualInstallCommandResp: AgentOriginalResult<ManualInstallCommand> = try {
            nodeManApi.getJobCommands(
                jobId = jobId,
                hostId = hostId
            )
        } catch (e: RemoteServiceException) { // 最初未获取到安装命令，节点管理抛出的"订阅任务未准备好"异常，该情况可重试，后台不抛出异常
            if (logger.isDebugEnabled)
                logger.debug("e.errorCode: ${e.errorCode}, isEq: ${NODEMAN_COMMAND_NOT_READY_CODE == e.errorCode}")
            if (NODEMAN_COMMAND_NOT_READY_CODE == e.errorCode) {
                AgentOriginalResult(
                    code = NODEMAN_COMMAND_NOT_READY_CODE,
                    result = false,
                    message = "Nodeman command is not ready.",
                    errors = null,
                    data = null
                )
            } else {
                throw e
            }
        }
        val obtainManualCommandRes: AgentResult<ObtainManualCommandResult> = AgentResult(
            code = manualInstallCommandResp.code,
            result = manualInstallCommandResp.result,
            message = manualInstallCommandResp.message,
            errors = manualInstallCommandResp.errors,
            data = if (manualInstallCommandResp.data?.solutions.isNullOrEmpty()) {
                ObtainManualCommandResult(
                    status = manualInstallCommandResp.data?.status,
                    networkPolicyDocLink = getNetworkPolicyDocLink()
                )
            } else {
                manualInstallCommandResp.data?.solutions?.get(0)?.let {
                    ObtainManualCommandResult(
                        type = it.type,
                        description = it.description,
                        steps = it.steps.map { step ->
                            Step(
                                type = step.type,
                                description = step.description,
                                contents = step.contents.map { content ->
                                    Content(
                                        text = content.text,
                                        description = content.description
                                    )
                                }
                            )
                        },
                        networkPolicyDocLink = getNetworkPolicyDocLink()
                    )
                }
            }
        )
        return obtainManualCommandRes
    }

    fun getNetworkPolicyDocLink(): String? {
        return environmentProperties.nodeman.networkPolicyDocLink
    }
}
