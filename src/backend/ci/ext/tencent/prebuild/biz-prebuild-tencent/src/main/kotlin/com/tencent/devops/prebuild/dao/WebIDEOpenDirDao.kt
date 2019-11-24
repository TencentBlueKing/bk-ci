package com.tencent.devops.prebuild.dao

import com.tencent.devops.model.prebuild.tables.TWebideOpendir
import com.tencent.devops.model.prebuild.tables.records.TWebideOpendirRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class WebIDEOpenDirDao {
    fun get(dslContext: DSLContext, owner: String, ip: String): TWebideOpendirRecord? {
        return with(TWebideOpendir.T_WEBIDE_OPENDIR) {
            dslContext.selectFrom(this)
                    .where(OWNER.eq(owner))
                    .and(IP.eq(ip))
                    .fetchAny()
        }
    }

    fun update(dslContext: DSLContext, owner: String, ip: String, path: String) {
        with(TWebideOpendir.T_WEBIDE_OPENDIR) {
            /*dslContext.update(this)
                    .set(PATH, path)
                    .set(LAST_UPDATE, System.currentTimeMillis())
                    .where(OWNER.eq(owner))
                    .and(IP.eq(ip))
                    .execute()*/
            dslContext.insertInto(this)
                    .values(owner, ip, path, System.currentTimeMillis())
                    .onDuplicateKeyUpdate()
                    .set(PATH, path)
                    .set(LAST_UPDATE, System.currentTimeMillis())
                    .execute()
        }
    }
}