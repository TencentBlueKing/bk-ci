package com.tencent.devops.prebuild.dao

import com.tencent.devops.model.prebuild.tables.TPrebuildPersonalMachine
import com.tencent.devops.model.prebuild.tables.records.TPrebuildPersonalMachineRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class PrebuildPersonalMachineDao {

    fun create(
        dslContext: DSLContext,
        userId: String,
        hostName: String,
        ip: String,
        remark: String?
    ) {
        with(TPrebuildPersonalMachine.T_PREBUILD_PERSONAL_MACHINE) {
            dslContext.insertInto(
                this,
                OWNER,
                HOST_NAME,
                IP,
                REMARK,
                CREATED_TIME,
                UPDATE_TIME
            ).values(
                userId,
                hostName,
                ip,
                remark,
                LocalDateTime.now(),
                LocalDateTime.now()
            ).execute()
        }
    }

    fun get(
        dslContext: DSLContext,
        userId: String,
        hostName: String
    ): TPrebuildPersonalMachineRecord? {
        with(TPrebuildPersonalMachine.T_PREBUILD_PERSONAL_MACHINE) {
            return dslContext.selectFrom(this)
                    .where(OWNER.eq(userId))
                    .and(HOST_NAME.eq(hostName))
                    .fetchAny()
        }
    }
}
