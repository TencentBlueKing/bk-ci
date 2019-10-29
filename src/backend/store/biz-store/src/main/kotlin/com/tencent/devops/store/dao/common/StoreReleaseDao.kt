package com.tencent.devops.store.dao.common

import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.model.store.tables.TStoreRelease
import com.tencent.devops.store.pojo.common.StoreReleaseCreateRequest
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class StoreReleaseDao {

    fun addStoreReleaseInfo(
        dslContext: DSLContext,
        userId: String,
        storeReleaseCreateRequest: StoreReleaseCreateRequest
    ) {
        with(TStoreRelease.T_STORE_RELEASE) {
            val storeCode = storeReleaseCreateRequest.storeCode
            val storeType = storeReleaseCreateRequest.storeType
            val record = dslContext.selectFrom(this)
                .where(STORE_CODE.eq(storeCode)).and(STORE_TYPE.eq(storeType.type.toByte()))
                .fetchOne()
            if (null == record) {
                dslContext.insertInto(
                    this,
                    ID,
                    STORE_CODE,
                    STORE_TYPE,
                    FIRST_PUB_CREATOR,
                    FIRST_PUB_TIME,
                    LATEST_UPGRADER,
                    LATEST_UPGRADE_TIME,
                    CREATOR,
                    MODIFIER
                ).values(
                    UUIDUtil.generate(),
                    storeReleaseCreateRequest.storeCode,
                    storeReleaseCreateRequest.storeType.type.toByte(),
                    storeReleaseCreateRequest.latestUpgrader,
                    storeReleaseCreateRequest.latestUpgradeTime,
                    storeReleaseCreateRequest.latestUpgrader,
                    storeReleaseCreateRequest.latestUpgradeTime,
                    userId,
                    userId
                ).execute()
            } else {
                dslContext.update(this)
                    .set(LATEST_UPGRADER, storeReleaseCreateRequest.latestUpgrader)
                    .set(LATEST_UPGRADE_TIME, storeReleaseCreateRequest.latestUpgradeTime)
                    .set(MODIFIER, userId)
                    .set(UPDATE_TIME, LocalDateTime.now())
                    .where(STORE_CODE.eq(storeCode)).and(STORE_TYPE.eq(storeType.type.toByte()))
                    .execute()
            }
        }
    }
}