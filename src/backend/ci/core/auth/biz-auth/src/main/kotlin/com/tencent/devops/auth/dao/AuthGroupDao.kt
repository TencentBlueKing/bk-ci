package com.tencent.devops.auth.dao

import com.tencent.devops.auth.entity.GroupCreateInfo
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.model.auth.tables.TAuthGroup
import com.tencent.devops.model.auth.tables.records.TAuthGroupRecord
import org.jooq.DSLContext
import org.jooq.Select
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.UUID

@Repository
class AuthGroupDao {

    fun createGroup(dslContext: DSLContext, groupCreateInfo: GroupCreateInfo): String {
        val id = UUIDUtil.generate()
        with(TAuthGroup.T_AUTH_GROUP) {
           dslContext.insertInto(
                TAuthGroup.T_AUTH_GROUP,
                ID,
                GROUP_NAME,
                GROUP_CODE,
                PROJECT_CODE,
                CREATE_USER,
                CREATE_TIME,
                UPDATE_USER,
                UPDATE_TIME
            ).values(
               id.toString(),
               groupCreateInfo.groupName,
               groupCreateInfo.groupCode,
               groupCreateInfo.projectCode,
               groupCreateInfo.user,
               LocalDateTime.now(),
               null,
               null
           ).execute()
        }
        return id.toString()
    }

    fun getGroup(dslContext: DSLContext, projectCode: String, groupCode: String) : TAuthGroupRecord? {
        with(TAuthGroup.T_AUTH_GROUP) {
            return dslContext.selectFrom(this)
                .where(PROJECT_CODE.eq(projectCode).and(GROUP_CODE.eq(groupCode))).fetchOne()
        }
    }

    fun getGroupById(dslContext: DSLContext, groupId: String) : TAuthGroupRecord? {
        with(TAuthGroup.T_AUTH_GROUP) {
            return dslContext.selectFrom(this)
                .where(ID.eq(groupId)).fetchOne()
        }
    }
}

