package com.tencent.devops.store.dao.common

import com.tencent.devops.model.atom.tables.TReasonRel
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class ReasonRelDao {

    fun add(
        dslContext: DSLContext,
        id: String,
        userId: String,
        storeCode: String,
        reasonId: String,
        note: String?,
        type: String
    ) {
        with(TReasonRel.T_REASON_REL) {
            dslContext.insertInto(
                this,
                ID,
                REASON_ID,
                NOTE,
                STORE_CODE,
                TYPE,
                CREATOR
            )
                .values(
                    id,
                    reasonId,
                    note,
                    storeCode,
                    type,
                    userId
                ).execute()
        }
    }

    fun isUsed(
        dslContext: DSLContext,
        id: String
    ): Boolean {
        with(TReasonRel.T_REASON_REL) {
            return dslContext.selectCount()
                .from(this)
                .where(REASON_ID.eq(id))
                .fetchOne(0, Long::class.java) != 0L
        }
    }
}