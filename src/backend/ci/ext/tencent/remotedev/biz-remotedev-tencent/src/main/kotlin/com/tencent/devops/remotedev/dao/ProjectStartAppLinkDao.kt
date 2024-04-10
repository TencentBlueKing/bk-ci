package com.tencent.devops.remotedev.dao

import com.tencent.devops.common.db.utils.skipCheck
import com.tencent.devops.model.remotedev.tables.TProjectStartAppLink
import com.tencent.devops.model.remotedev.tables.records.TProjectStartAppLinkRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class ProjectStartAppLinkDao {

    fun addLink(
        dslContext: DSLContext,
        appName: String,
        detail: String,
        appId: Long
    ): Boolean {
        with(TProjectStartAppLink.T_PROJECT_START_APP_LINK) {
            return dslContext.insertInto(
                this,
                APPNAME,
                DETAIL,
                APPID
            ).values(
                appName,
                detail,
                appId
            ).onDuplicateKeyUpdate()
                .set(APPID, appId)
                .execute() > 0
        }
    }

    fun fetchAll(
        dslContext: DSLContext
    ): List<TProjectStartAppLinkRecord> {
        with(TProjectStartAppLink.T_PROJECT_START_APP_LINK) {
            return dslContext.selectFrom(this).skipCheck().fetch()
        }
    }
}
