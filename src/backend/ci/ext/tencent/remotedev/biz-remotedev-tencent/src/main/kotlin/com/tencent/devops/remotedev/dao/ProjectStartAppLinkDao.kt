package com.tencent.devops.remotedev.dao

import com.tencent.devops.model.remotedev.tables.TProjectStartAppLink
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
            ).execute() > 0
        }
    }
}
