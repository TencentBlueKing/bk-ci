package com.tencent.devops.lambda.dao.store

import com.tencent.devops.model.store.tables.TStoreDockingPlatform
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class LambdaStoreDao {
    fun getPlatformName(
        dslContext: DSLContext,
        platformCode: String?
    ): String? {
        with(TStoreDockingPlatform.T_STORE_DOCKING_PLATFORM) {
            return dslContext.select(PLATFORM_NAME).from(this)
                .where(PLATFORM_CODE.eq(platformCode))
                .fetchOne(0, String::class.java)
        }
    }
}