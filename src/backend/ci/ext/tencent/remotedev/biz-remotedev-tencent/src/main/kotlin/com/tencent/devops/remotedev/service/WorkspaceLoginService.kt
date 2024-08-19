package com.tencent.devops.remotedev.service

import com.tencent.devops.remotedev.dao.WorkspaceJoinDao
import com.tencent.devops.remotedev.dao.WorkspaceLoginDao
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class WorkspaceLoginService @Autowired constructor(
    private val dslContext: DSLContext,
    private val workspaceLoginDao: WorkspaceLoginDao,
    private val workspaceJoinDao: WorkspaceJoinDao
) {
    fun addUserLogin(
        userId: String,
        workspaceName: String
    ) {
        val workspace = workspaceJoinDao.fetchAnyWindowsWorkspace(dslContext, workspaceName)
        if (workspace == null) {
            logger.error("addUserLogin workspace $workspaceName is null")
            return
        }
        workspaceLoginDao.createOrUpdate(
            dslContext = dslContext,
            projectId = workspace.projectId,
            workspaceName = workspaceName,
            hostIp = workspace.hostIp ?: "",
            loginUser = userId,
            loginTime = LocalDateTime.now()
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(WorkspaceLoginService::class.java)
    }
}
