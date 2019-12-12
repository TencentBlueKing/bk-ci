package com.tencent.devops.store.dao.image

import com.tencent.devops.model.store.tables.TImageVersionLog
import com.tencent.devops.model.store.tables.records.TImageVersionLogRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class ImageVersionLogDao {
    fun getLatestImageVersionLogByImageId(dslContext: DSLContext, imageId: String): Result<TImageVersionLogRecord>? {
        with(TImageVersionLog.T_IMAGE_VERSION_LOG) {
            val result = dslContext.selectFrom(this)
                .where(IMAGE_ID.eq(imageId))
                .orderBy(UPDATE_TIME.desc())
                .limit(1)
                .fetch()
            if (result.size == 0) return null
            return result
        }
    }

    fun deleteByImageId(dslContext: DSLContext, imageId: String) {
        with(TImageVersionLog.T_IMAGE_VERSION_LOG) {
            dslContext.deleteFrom(this)
                .where(IMAGE_ID.eq(imageId))
                .execute()
        }
    }
}