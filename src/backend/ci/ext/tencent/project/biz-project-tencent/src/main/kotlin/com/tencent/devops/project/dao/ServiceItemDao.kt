package com.tencent.devops.project.dao

import com.tencent.devops.model.project.tables.TServiceItem
import com.tencent.devops.model.project.tables.records.TServiceItemRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class ServiceItemDao {

    fun getAllServiceItem(dslContext: DSLContext): Result<TServiceItemRecord>? {
        return with(TServiceItem.T_SERVICE_ITEM) {
            dslContext.selectFrom(this)
                .orderBy(CREATE_TIME.desc())
                .fetch()
        }
    }
}