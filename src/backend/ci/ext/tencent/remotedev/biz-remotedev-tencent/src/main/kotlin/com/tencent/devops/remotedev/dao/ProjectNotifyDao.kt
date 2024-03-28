package com.tencent.devops.remotedev.dao

import com.tencent.devops.common.api.model.SQLLimit
import com.tencent.devops.common.db.utils.skipCheck
import com.tencent.devops.model.remotedev.tables.TWorkspaceNotify
import com.tencent.devops.model.remotedev.tables.records.TWorkspaceNotifyRecord
import com.tencent.devops.remotedev.pojo.op.WorkspaceNotifyData
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ProjectNotifyDao {

    fun add(
        dslContext: DSLContext,
        userId: String?,
        notifyData: WorkspaceNotifyData
    ) {
        with(TWorkspaceNotify.T_WORKSPACE_NOTIFY) {
            dslContext.insertInto(
                this,
                OPERATOR,
                PROJECT_IDS,
                IPS,
                TITLE,
                DESC,
                CREATED_TIME
            ).values(
                userId,
                notifyData.projectId.toString(),
                notifyData.ip.toString(),
                notifyData.title,
                notifyData.desc ?: "",
                LocalDateTime.now()
            ).execute()
        }
    }

    fun fetch(
        dslContext: DSLContext,
        sqlLimit: SQLLimit
    ): List<TWorkspaceNotifyRecord> {
        with(TWorkspaceNotify.T_WORKSPACE_NOTIFY) {
            return dslContext.selectFrom(this).offset(sqlLimit.offset).limit(sqlLimit.limit).skipCheck().fetch()
        }
    }
}
