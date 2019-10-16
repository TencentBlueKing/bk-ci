package com.tencent.devops.environment.service

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PROJECT_ID
import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.environment.service.slave.SlaveGatewayService
import com.tencent.devops.model.environment.tables.records.TEnvironmentThirdpartyAgentRecord
import org.springframework.beans.factory.annotation.Autowired

/**
 * 腾讯内部旧版专用Agent下载链接生成服务
 */
class TencentAgentUrlServiceImpl @Autowired constructor(
    private val slaveGatewayService: SlaveGatewayService
) : AgentUrlService {

    override fun genAgentInstallUrl(agentRecord: TEnvironmentThirdpartyAgentRecord): String {
        val gw = slaveGatewayService.fixGateway(agentRecord.gateway)
        val agentHashId = HashUtil.encodeLongId(agentRecord.id)
        return "$gw/external/agents/$agentHashId/install"
    }

    override fun genAgentUrl(agentRecord: TEnvironmentThirdpartyAgentRecord): String {
        val gw = slaveGatewayService.fixGateway(agentRecord.gateway)
        val agentHashId = HashUtil.encodeLongId(agentRecord.id)
        return "$gw/external/agents/$agentHashId/agent"
    }

    override fun genAgentInstallScript(agentRecord: TEnvironmentThirdpartyAgentRecord): String {
        val url = genAgentInstallUrl(agentRecord)
        return if (agentRecord.os != OS.WINDOWS.name) {
            "curl -H \"$AUTH_HEADER_DEVOPS_PROJECT_ID: ${agentRecord.projectId}\" $url | bash"
        } else {
            ""
        }
    }
}