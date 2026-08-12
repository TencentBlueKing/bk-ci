package com.tencent.devops.process.yaml.transfer

import org.springframework.stereotype.Service

/**
 * 定时触发 node 字段在 YAML(workspaceName) 与 Model(agentHashId) 之间的互转扩展点。
 *
 * 默认空实现，具体调用 environment/CDS 接口的逻辑由对应通道覆盖。
 */
@Service
open class WorkspaceAgentTransferService {

    /**
     * agentHashId -> workspaceName（Model/UI 转 YAML 时使用）
     */
    open fun getWorkspaceNameByAgent(
        userId: String,
        projectId: String,
        agentHashId: String
    ): String? = null

    /**
     * workspaceName -> agentHashId（YAML 转 Model/UI 时使用）
     */
    open fun getAgentByWorkspaceName(
        userId: String,
        projectId: String,
        workspaceName: String
    ): String? = null
}
