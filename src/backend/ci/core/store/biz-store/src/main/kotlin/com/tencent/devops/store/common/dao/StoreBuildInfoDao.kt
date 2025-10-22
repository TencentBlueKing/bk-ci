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

import com.tencent.devops.model.store.tables.TStoreBuildInfo
import com.tencent.devops.model.store.tables.records.TStoreBuildInfoRecord
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository

@Suppress("ALL")
@Repository
class StoreBuildInfoDao {

    fun list(dslContext: DSLContext, storeType: StoreTypeEnum): Result<TStoreBuildInfoRecord>? {
        with(TStoreBuildInfo.T_STORE_BUILD_INFO) {
            return dslContext.selectFrom(this)
                    .where(ENABLE.eq(true))
                    .and(STORE_TYPE.eq(storeType.type.toByte()))
                    .orderBy(CREATE_TIME.asc()).fetch()
        }
    }

    fun getStoreBuildInfoByLanguage(
        dslContext: DSLContext,
        language: String,
        storeType: StoreTypeEnum
    ): TStoreBuildInfoRecord? {
        return with(TStoreBuildInfo.T_STORE_BUILD_INFO) {
            dslContext.selectFrom(this)
                .where(LANGUAGE.eq(language))
                .and(STORE_TYPE.eq(storeType.type.toByte()))
                .fetchOne()
        }
    }
}
