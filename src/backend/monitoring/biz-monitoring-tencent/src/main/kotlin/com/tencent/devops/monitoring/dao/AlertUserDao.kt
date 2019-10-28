package com.tencent.devops.monitoring.dao

import com.tencent.devops.model.monitoring.tables.TAlertUser
import com.tencent.devops.model.monitoring.tables.records.TAlertUserRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class AlertUserDao {

    fun list(dslContext: DSLContext): List<TAlertUserRecord> {
        with(TAlertUser.T_ALERT_USER) {
            return dslContext.selectFrom(this)
                    .fetch()
        }
    }
}