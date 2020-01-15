package com.tencent.devops.store.dao

import com.tencent.devops.model.store.tables.TExtensionService
import com.tencent.devops.store.dao.common.AbstractStoreCommonDao
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository(value = "SERVICE_COMMON_DAO")
class ServiceCommonDao : AbstractStoreCommonDao() {
    override fun getNewestStoreNameByCode(dslContext: DSLContext, storeCode: String): String? {
        return with(TExtensionService.T_EXTENSION_SERVICE) {
            dslContext.select(SERVICE_NAME).from(this)
                .where(SERVICE_CODE.eq(storeCode))
                .orderBy(CREATE_TIME.desc())
                .limit(1)
                .fetchOne(0, String::class.java)
        }
    }

    override fun getStoreCodeListByName(dslContext: DSLContext, storeName: String): Result<out Record>? {
        return with(TExtensionService.T_EXTENSION_SERVICE) {
            dslContext.select(SERVICE_CODE.`as`("storeCode")).from(this)
                .where(SERVICE_NAME.contains(storeName))
                .groupBy(SERVICE_CODE)
                .fetch()
        }
    }

    override fun getStoreNameById(dslContext: DSLContext, storeId: String): String? {
        return with(TExtensionService.T_EXTENSION_SERVICE) {
            dslContext.select(SERVICE_NAME).from(this).where(ID.eq(storeId)).fetchOne(0, String::class.java)
        }
    }
}