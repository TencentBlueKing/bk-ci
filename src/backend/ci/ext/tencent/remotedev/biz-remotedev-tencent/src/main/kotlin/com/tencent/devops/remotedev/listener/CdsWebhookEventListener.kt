package com.tencent.devops.remotedev.listener

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.process.api.service.ServiceMarketEventResource
import com.tencent.devops.remotedev.dao.WorkspaceJoinDao
import com.tencent.devops.remotedev.dispatch.kubernetes.dao.DispatchWorkspaceDao
import com.tencent.devops.remotedev.listener.event.CdsWebhookEvent
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CdsWebhookEventListener @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val dispatchWorkspaceDao: DispatchWorkspaceDao,
    private val workspaceJoinDao: WorkspaceJoinDao
) {

    fun execute(event: CdsWebhookEvent) {
        val workspaceName = event.workspaceName
            ?: dispatchWorkspaceDao.getWorkspaceNameByEnvId(event.envId, dslContext)
            ?: run {
                logger.warn("CdsWebhookEventListener|${event}|workspace not found")
                return
            }
        val ws = workspaceJoinDao.fetchAnyWindowsWorkspace(dslContext, workspaceName) ?: run {
            logger.warn("CdsWebhookEventListener|${event}|windows workspace not found")
            return
        }
        client.get(ServiceMarketEventResource::class).cdsWebhook(
            userId = event.userId,
            projectId = ws.projectId,
            workspaceName = workspaceName,
            cdsIp = ws.hostIp ?: "",
            eventType = event.type.name.lowercase(),
            eventCode = "CDS-${event.type.name}",
            body = event.body
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CdsWebhookEventListener::class.java)
    }
}
