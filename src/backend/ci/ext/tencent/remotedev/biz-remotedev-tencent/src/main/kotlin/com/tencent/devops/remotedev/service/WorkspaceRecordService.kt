package com.tencent.devops.remotedev.service

import com.tencent.devops.remotedev.dao.ProjectStartAppLinkDao
import com.tencent.devops.remotedev.dao.WorkspaceWindowsDao
import com.tencent.devops.remotedev.service.client.RemotedevBkRepoClient
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class WorkspaceRecordService @Autowired constructor(
    private val dslContext: DSLContext,
    private val workspaceWindowsDao: WorkspaceWindowsDao,
    private val startAppLinkDao: ProjectStartAppLinkDao,
    private val remotedevBkRepoClient: RemotedevBkRepoClient
) {

    fun enableRecord(
        workspaceName: String,
        enableUser: String?
    ) {
        workspaceWindowsDao.updateRecord(
            dslContext = dslContext,
            workspaceName = workspaceName,
            enableUser = enableUser
        )
    }

    /**
     * @return <enable, address>
     */
    fun checkRecordAndAddress(
        appId: Long,
        ip: String
    ): Pair<Boolean, String?> {
        val projectId = startAppLinkDao.getAppName(dslContext, appId) ?: return Pair(false, null)
        val (workspaceName, enableUser) = workspaceWindowsDao.fetchRecordByProjectIp(dslContext, projectId, ip)
            ?: return Pair(false, null)
        if (enableUser.isNullOrBlank()) {
            return Pair(false, null)
        }

        return Pair(
            true,
            remotedevBkRepoClient.repoStreamCreate(
                projectId = projectId,
                workspaceName = workspaceName,
                userId = enableUser
            )
        )
    }
}