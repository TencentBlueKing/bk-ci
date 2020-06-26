package com.tencent.devops.auth.dao

import com.tencent.devops.model.auth.tables.TAuthGroupUser
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.UUID

@Repository
class AuthGroupUserDao {

    fun create(dslContext: DSLContext, userId: String, groupId: String): Int {
        with(TAuthGroupUser.T_AUTH_GROUP_USER) {
            return dslContext.insertInto(
                this,
                ID,
                USER_ID,
                GROUP_ID,
                CREATE_USER,
                CREATE_TIME
            ).values(
                    UUID.randomUUID().toString(),
                    userId,
                    groupId,
                    userId,
                    LocalDateTime.now()
                ).execute()
        }
    }
}