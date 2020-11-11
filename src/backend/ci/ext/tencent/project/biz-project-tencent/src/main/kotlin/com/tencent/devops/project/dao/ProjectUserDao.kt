package com.tencent.devops.project.dao

import com.tencent.devops.model.project.tables.TUser
import com.tencent.devops.model.project.tables.records.TUserRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class ProjectUserDao {
    fun list(
        dslContext: DSLContext,
        limit: Int,
        offset: Int
    ): Result<TUserRecord>? {
        return with(TUser.T_USER) {
            dslContext.selectFrom(this).limit(limit).offset(offset).fetch()
        }
    }
}