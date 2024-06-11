package com.tencent.devops.remotedev.service

import com.tencent.devops.remotedev.dao.ProjectStartAppLinkDao
import com.tencent.devops.remotedev.dao.WorkspaceWindowsDao
import com.tencent.devops.remotedev.service.client.BkRepoClient
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class WorkspaceRecordService @Autowired constructor(
    private val dslContext: DSLContext,
    private val workspaceWindowsDao: WorkspaceWindowsDao,
    private val startAppLinkDao: ProjectStartAppLinkDao,
    private val bkRepoClient: BkRepoClient
) {

    fun enableRecord(
        workspaceName: String,
        enable: Boolean
    ) {
        workspaceWindowsDao.updateRecord(dslContext = dslContext, workspaceName = workspaceName, enable = enable)
    }

    /**
     * @return <enable, address>
     */
    fun checkRecordAndAddress(
        appId: Long,
        ip: String,
        userId: String
    ): Pair<Boolean, String?> {
        val projectId = startAppLinkDao.getAppName(dslContext, appId) ?: return Pair(false, null)
        val (workspaceName, enable) = workspaceWindowsDao.fetchRecordByProjectIp(dslContext, projectId, ip)
            ?: return Pair(false, null)
        if (!enable) {
            return Pair(false, null)
        }

        return Pair(
            true,
            bkRepoClient.repoStreamCreate(
                projectId = projectId,
                workspaceName = workspaceName,
                userId = userId
            )
        )
    }
}