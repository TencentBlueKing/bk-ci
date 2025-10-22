/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.store.common.dao

import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.model.store.tables.TStoreMediaInfo
import com.tencent.devops.model.store.tables.records.TStoreMediaInfoRecord
import com.tencent.devops.store.pojo.common.media.StoreMediaInfo
import com.tencent.devops.store.pojo.common.media.StoreMediaInfoRequest
import com.tencent.devops.store.common.service.action.StoreDecorateFactory
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

    fun deleteByStoreCode(dslContext: DSLContext, storeCode: String, storeType: Byte) {
        with(TStoreMediaInfo.T_STORE_MEDIA_INFO) {
            dslContext.delete(this).where(
                STORE_CODE.eq(storeCode).and(STORE_TYPE.eq(storeType))
            ).execute()
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
                .where(STORE_TYPE.eq(type).and(STORE_CODE.eq(storeCode))).orderBy(MEDIA_TYPE.desc())
                .fetch()
        }
    }

    fun convert(record: TStoreMediaInfoRecord): StoreMediaInfo {
        with(record) {
            return StoreMediaInfo(
                id = id,
                storeCode = storeCode,
                mediaUrl = mediaUrl?.let {
                    StoreDecorateFactory.get(StoreDecorateFactory.Kind.HOST)?.decorate(it) as? String
                } ?: "",
                mediaType = mediaType,
                create = creator,
                modifier = modifier,
                createTime = DateTimeUtil.toDateTime(createTime),
                updateTime = DateTimeUtil.toDateTime(updateTime)
            )
        }
    }
}
