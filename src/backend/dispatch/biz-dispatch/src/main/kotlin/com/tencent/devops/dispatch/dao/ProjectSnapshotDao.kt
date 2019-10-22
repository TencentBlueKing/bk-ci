package com.tencent.devops.dispatch.dao

import com.tencent.devops.model.dispatch.tables.TDispatchProjectSnapshot
import com.tencent.devops.model.dispatch.tables.records.TDispatchProjectSnapshotRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class ProjectSnapshotDao {

    fun findSnapshot(dslContext: DSLContext, projectId: String): TDispatchProjectSnapshotRecord? {
        return dslContext.selectFrom(TDispatchProjectSnapshot.T_DISPATCH_PROJECT_SNAPSHOT)
                .where(TDispatchProjectSnapshot.T_DISPATCH_PROJECT_SNAPSHOT.PROJECT_ID.eq(projectId))
                .fetchOne()
    }
}