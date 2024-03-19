package com.tencent.devops.remotedev.dao

import com.tencent.devops.model.remotedev.tables.TWorkspaceLogin
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class WorkspaceLoginDao {
    fun createOrUpdate(
        dslContext: DSLContext,
        projectId: String,
        workspaceName: String,
        hostIp: String,
        loginUser: String,
        loginTime: LocalDateTime
    ) {
        with(TWorkspaceLogin.T_WORKSPACE_LOGIN) {
            dslContext.insertInto(
                this,
                PROJECT_ID,
                WORKSPACE_NAME,
                HOST_IP,
                LAST_LOGIN_USER,
                LAST_LOGIN_TIME
            ).values(
                projectId,
                workspaceName,
                hostIp,
                loginUser,
                loginTime
            ).onDuplicateKeyUpdate()
                .set(LAST_LOGIN_USER, loginUser)
                .set(LAST_LOGIN_TIME, loginTime)
                .execute()
        }
    }
}
