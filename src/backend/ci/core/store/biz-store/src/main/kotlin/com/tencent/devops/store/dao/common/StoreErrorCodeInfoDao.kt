/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.devops.store.dao.common

import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.model.store.tables.TStoreErrorCodeInfo
import com.tencent.devops.store.pojo.common.StoreErrorCodeInfo
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class StoreErrorCodeInfoDao {

    fun batchUpdateErrorCodeInfo(dslContext: DSLContext, userId: String, storeErrorCodeInfo: StoreErrorCodeInfo) {
        dslContext.batch(storeErrorCodeInfo.errorCodes.map { errorCode ->
            with(TStoreErrorCodeInfo.T_STORE_ERROR_CODE_INFO) {
                dslContext.insertInto(this)
                    .set(ID, UUIDUtil.generate())
                    .set(STORE_CODE, storeErrorCodeInfo.storeCode)
                    .set(STORE_TYPE, storeErrorCodeInfo.storeType?.type?.toByte())
                    .set(ERROR_CODE, errorCode)
                    .set(CREATOR, userId)
                    .set(MODIFIER, userId)
                    .set(CREATE_TIME, LocalDateTime.now())
                    .set(UPDATE_TIME, LocalDateTime.now())
                    .onDuplicateKeyUpdate()
                    .set(MODIFIER, userId)
                    .set(UPDATE_TIME, LocalDateTime.now())
            }
        }).execute()
    }

    fun getStoreErrorCode(
        dslContext: DSLContext,
        storeCode: String,
        storeType: StoreTypeEnum,
        errorCode: Int
    ): Result<Record> {
        with(TStoreErrorCodeInfo.T_STORE_ERROR_CODE_INFO) {
            return dslContext.select()
                .from(this)
                .where(STORE_CODE.eq(storeCode))
                .and(STORE_TYPE.eq(storeType.type.toByte()))
                .and(ERROR_CODE.eq(errorCode))
                .fetch()
        }
    }

    fun getStoreErrorCodes(
        dslContext: DSLContext,
        storeCode: String,
        storeType: StoreTypeEnum
    ): Set<Int> {
        with(TStoreErrorCodeInfo.T_STORE_ERROR_CODE_INFO) {
            return dslContext.select(
                ERROR_CODE
            ).from(this)
                .where(STORE_CODE.eq(storeCode))
                .and(STORE_TYPE.eq(storeType.type.toByte()))
                .fetchSet(ERROR_CODE)
        }
    }

    fun batchDeleteErrorCodeInfo(
        dslContext: DSLContext,
        storeCode: String,
        storeType: StoreTypeEnum,
        errorCodes: Set<Int>
    ) {
        with(TStoreErrorCodeInfo.T_STORE_ERROR_CODE_INFO) {
            dslContext.deleteFrom(this)
                .where(STORE_CODE.eq(storeCode))
                .and(STORE_TYPE.eq(storeType.type.toByte()))
                .and(ERROR_CODE.`in`(errorCodes))
                .execute()
        }
    }
}
