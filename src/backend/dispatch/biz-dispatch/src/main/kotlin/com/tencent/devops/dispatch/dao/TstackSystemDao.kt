package com.tencent.devops.dispatch.dao

import com.tencent.devops.model.dispatch.tables.TDispatchTstackFloatingIp
import com.tencent.devops.model.dispatch.tables.TDispatchTstackSystem
import com.tencent.devops.model.dispatch.tables.records.TDispatchTstackFloatingIpRecord
import com.tencent.devops.model.dispatch.tables.records.TDispatchTstackSystemRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class TstackSystemDao {
    fun getSystemConfig(dslContext: DSLContext): TDispatchTstackSystemRecord {
        with(TDispatchTstackSystem.T_DISPATCH_TSTACK_SYSTEM) {
            return dslContext.selectFrom(this)
                    // .where(ID.eq(1L))
                    .fetchOne()
        }
    }

    fun getFloatingIpList(dslContext: DSLContext): Result<TDispatchTstackFloatingIpRecord> {
        with(TDispatchTstackFloatingIp.T_DISPATCH_TSTACK_FLOATING_IP) {
            return dslContext.selectFrom(this).fetch()
        }
    }
}