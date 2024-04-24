package com.tencent.devops.remotedev.dao

import com.tencent.devops.common.api.model.SQLLimit
import com.tencent.devops.common.db.utils.skipCheck
import com.tencent.devops.model.remotedev.tables.TProjectTcloudCfs
import com.tencent.devops.model.remotedev.tables.records.TProjectTcloudCfsRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class ProjectTCloudCfsDao {
    fun add(
        dslContext: DSLContext,
        projectId: String,
        cfsId: String,
        region: String,
        pgId: String
    ) {
        with(TProjectTcloudCfs.T_PROJECT_TCLOUD_CFS) {
            dslContext.insertInto(
                this,
                PROJECT_ID,
                CFS_ID,
                REGION,
                PG_ID
            ).values(
                projectId,
                cfsId,
                region,
                pgId
            ).execute()
        }
    }

    fun updatePGId(
        dslContext: DSLContext,
        projectId: String,
        cfsId: String,
        pgId: String
    ) {
        with(TProjectTcloudCfs.T_PROJECT_TCLOUD_CFS) {
            dslContext.update(this)
                .set(PG_ID, pgId)
                .where(PROJECT_ID.eq(projectId)).and(CFS_ID.eq(cfsId))
                .execute()
        }
    }

    fun fetch(
        dslContext: DSLContext,
        sqlLimit: SQLLimit
    ): List<TProjectTcloudCfsRecord> {
        with(TProjectTcloudCfs.T_PROJECT_TCLOUD_CFS) {
            return dslContext.selectFrom(this).offset(sqlLimit.offset).limit(sqlLimit.limit).skipCheck().fetch()
        }
    }

    fun fetchAny(
        dslContext: DSLContext,
        projectId: String
    ): TProjectTcloudCfsRecord? {
        with(TProjectTcloudCfs.T_PROJECT_TCLOUD_CFS) {
            return dslContext.selectFrom(this).where(PROJECT_ID.eq(projectId)).fetchAny()
        }
    }

    fun delete(
        dslContext: DSLContext,
        projectId: String,
        cfsId: String
    ) {
        with(TProjectTcloudCfs.T_PROJECT_TCLOUD_CFS) {
            dslContext.deleteFrom(this).where(PROJECT_ID.eq(projectId)).and(CFS_ID.eq(cfsId)).execute()
        }
    }
}
