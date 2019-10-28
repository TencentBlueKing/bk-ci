package com.tencent.devops.environment.service

import com.tencent.devops.model.environment.tables.records.TEnvironmentThirdpartyAgentRecord

interface AgentUrlService {

    fun genAgentInstallUrl(agentRecord: TEnvironmentThirdpartyAgentRecord): String
    /**
     *生成Agent URL
     */
    fun genAgentUrl(agentRecord: TEnvironmentThirdpartyAgentRecord): String

    /**
     * 生成构建机脚本下载链接
     */
    fun genAgentInstallScript(agentRecord: TEnvironmentThirdpartyAgentRecord): String
}