package com.tencent.devops.remotedev.service

import com.tencent.devops.remotedev.dao.WorkspaceDao
import com.tencent.devops.remotedev.dao.WorkspaceLoginDao
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class WorkspaceLoginService @Autowired constructor(
    private val dslContext: DSLContext,
    private val workspaceLoginDao: WorkspaceLoginDao,
    private val workspaceDao: WorkspaceDao
) {
    fun addUserLogin(
        userId: String,
        workspaceName: String
    ) {
        val workspace = workspaceDao.fetchWorkspaces(dslContext, setOf(workspaceName)).firstOrNull()
        if (workspace == null) {
            logger.error("addUserLogin workspace $workspaceName is null")
            return
        }
        workspaceLoginDao.createOrUpdate(
            dslContext = dslContext,
            projectId = workspace.projectId,
            workspaceName = workspaceName,
            hostIp = workspace.hostName ?: "",
            loginUser = userId,
            loginTime = LocalDateTime.now()
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(WorkspaceLoginService::class.java)
    }
}
