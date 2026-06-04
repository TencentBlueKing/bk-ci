package com.tencent.devops.environment.service

import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.environment.dao.thirdpartyagent.ThirdPartyAgentDao
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TencentNodeService @Autowired constructor(
    private val dslContext: DSLContext,
    private val thirdPartyAgentDao: ThirdPartyAgentDao,
    private val nodeService: NodeService
) {
    fun updateCreateNodeDisplay(
        userId: String,
        projectId: String,
        workspaceName: String,
        displayName: String
    ): Boolean {
        val record =
            thirdPartyAgentDao.getAgentByWorkspaceName(dslContext, projectId, listOf(workspaceName)).firstOrNull()
                ?: return false
        nodeService.updateDisplayName(userId, projectId, HashUtil.encodeLongId(record.nodeId), displayName)
        return true
    }
}