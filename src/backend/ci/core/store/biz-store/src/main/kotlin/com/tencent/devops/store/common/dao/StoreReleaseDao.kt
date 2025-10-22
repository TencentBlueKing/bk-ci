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

import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.model.store.tables.TStoreRelease
import com.tencent.devops.store.pojo.common.publication.StoreReleaseCreateRequest
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Suppress("ALL")
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

    fun deleteStoreReleaseInfo(dslContext: DSLContext, storeCode: String, storeType: Byte) {
        with(TStoreRelease.T_STORE_RELEASE) {
            dslContext.deleteFrom(this)
                .where(STORE_CODE.eq(storeCode))
                .and(STORE_TYPE.eq(storeType))
                .execute()
        }
    }
}
