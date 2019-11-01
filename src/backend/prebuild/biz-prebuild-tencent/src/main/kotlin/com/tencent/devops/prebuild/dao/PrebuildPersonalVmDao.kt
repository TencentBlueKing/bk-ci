package com.tencent.devops.prebuild.dao

import com.tencent.devops.model.prebuild.tables.TPrebuildPersonalVm
import com.tencent.devops.model.prebuild.tables.records.TPrebuildPersonalVmRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class PrebuildPersonalVmDao {

    fun create(
        dslContext: DSLContext,
        userId: String,
        vmIp: String,
        vmName: String,
        rsyncPwd: String
    ) {
        with(TPrebuildPersonalVm.T_PREBUILD_PERSONAL_VM) {
            dslContext.insertInto(
                this,
                OWNER,
                VM_IP,
                VM_NAME,
                RSYNC_PWD,
                CREATED_TIME,
                UPDATE_TIME
            ).values(
                userId,
                vmIp,
                vmName,
                rsyncPwd,
                LocalDateTime.now(),
                LocalDateTime.now()
            )
                .execute()
        }
    }

    fun get(
        dslContext: DSLContext,
        userId: String
    ): TPrebuildPersonalVmRecord? {
        with(TPrebuildPersonalVm.T_PREBUILD_PERSONAL_VM) {
            return dslContext.selectFrom(this)
                .where(OWNER.eq(userId))
                .fetchAny()
        }
    }
}
