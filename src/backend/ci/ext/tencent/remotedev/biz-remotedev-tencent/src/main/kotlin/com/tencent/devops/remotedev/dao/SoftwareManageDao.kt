package com.tencent.devops.remotedev.dao

import com.tencent.devops.model.remotedev.tables.TProjectSoftwares
import com.tencent.devops.model.remotedev.tables.records.TProjectSoftwaresRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class SoftwareManageDao {

    // 新增模板
    fun querySoftwareList(
        projectId: String,
        dslContext: DSLContext
    ): Result<TProjectSoftwaresRecord> {
        return with(TProjectSoftwares.T_PROJECT_SOFTWARES) {
            dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .fetch()
        }
    }
}
