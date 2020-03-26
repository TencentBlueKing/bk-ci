package com.tencent.devops.store.dao.common

import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.model.store.tables.TStoreMediaInfo
import com.tencent.devops.model.store.tables.records.TStoreMediaInfoRecord
import com.tencent.devops.store.pojo.common.StoreMediaInfo
import com.tencent.devops.store.pojo.common.StoreMediaInfoRequest
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class StoreMediaInfoDao {
    fun add(dslContext: DSLContext, id: String, userId: String, storeMediaInfoReq: StoreMediaInfoRequest, type: Byte) {
        with(TStoreMediaInfo.T_STORE_MEDIA_INFO) {
            dslContext.insertInto(
                this,
                ID,
                STORE_CODE,
                MEDIA_URL,
                MEDIA_TYPE,
                STORE_TYPE,
                CREATOR,
                MODIFIER,
                CREATE_TIME,
                UPDATE_TIME
            )
                .values(
                    id,
                    storeMediaInfoReq.storeCode,
                    storeMediaInfoReq.mediaUrl,
                    storeMediaInfoReq.mediaType,
                    type,
                    userId,
                    userId,
                    LocalDateTime.now(),
                    LocalDateTime.now()
                ).execute()
        }
    }

    fun updateById(dslContext: DSLContext, id: String, userId: String, storeMediaInfoReq: StoreMediaInfoRequest) {
        with(TStoreMediaInfo.T_STORE_MEDIA_INFO) {
            dslContext.update(this)
                .set(MEDIA_URL, storeMediaInfoReq.mediaUrl)
                .set(MEDIA_TYPE, storeMediaInfoReq.mediaType)
                .set(UPDATE_TIME, LocalDateTime.now())
                .set(MODIFIER, userId)
                .where(ID.eq(id))
                .execute()
        }
    }

    fun getMediaInfo(dslContext: DSLContext, id: String): TStoreMediaInfoRecord? {
        with(TStoreMediaInfo.T_STORE_MEDIA_INFO) {
            return dslContext.selectFrom(this)
                .where(ID.eq(id))
                .fetchOne()
        }
    }

    fun getMediaInfoByStoreCode(dslContext: DSLContext, type: Byte, storeCode: String): Result<TStoreMediaInfoRecord>? {
        with(TStoreMediaInfo.T_STORE_MEDIA_INFO) {
            return dslContext
                .selectFrom(this)
                .where(STORE_TYPE.eq(type).and(STORE_CODE.eq(storeCode)))
                .fetch()
        }
    }

    fun convert(record: TStoreMediaInfoRecord): StoreMediaInfo {
        with(record) {
            return StoreMediaInfo(
                id = id,
                storeCode = storeCode,
                mediaUrl = mediaUrl,
                mediaType = mediaType,
                create = creator,
                modifier = modifier,
                createTime = DateTimeUtil.toDateTime(createTime),
                updateTime = DateTimeUtil.toDateTime(updateTime)
            )
        }
    }
}