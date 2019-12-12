package com.tencent.devops.store.dao.image

import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.model.store.tables.TImageVersionLog
import com.tencent.devops.model.store.tables.records.TImageVersionLogRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class MarketImageVersionLogDao {

    fun addMarketImageVersion(dslContext: DSLContext, userId: String, imageId: String, releaseType: Byte, versionContent: String) {
        with(TImageVersionLog.T_IMAGE_VERSION_LOG) {
            dslContext.insertInto(this,
                ID,
                IMAGE_ID,
                RELEASE_TYPE,
                CONTENT,
                CREATOR,
                MODIFIER
            )
                .values(
                    UUIDUtil.generate(),
                    imageId,
                    releaseType,
                    versionContent,
                    userId,
                    userId
                )
                .onDuplicateKeyUpdate()
                .set(RELEASE_TYPE, releaseType)
                .set(CONTENT, versionContent)
                .set(MODIFIER, userId)
                .set(UPDATE_TIME, LocalDateTime.now())
                .execute()
        }
    }

    fun getImageVersion(dslContext: DSLContext, imageId: String): TImageVersionLogRecord {
        with(TImageVersionLog.T_IMAGE_VERSION_LOG) {
            return dslContext.selectFrom(this)
                .where(IMAGE_ID.eq(imageId))
                .fetchOne()
        }
    }
}