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

import com.tencent.devops.model.store.tables.TStoreBaseExt
import com.tencent.devops.model.store.tables.records.TStoreBaseExtRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class StoreBaseExtQueryDao {

    fun getBaseExtByIds(dslContext: DSLContext, storeIds: List<String>): Result<TStoreBaseExtRecord> {
        with(TStoreBaseExt.T_STORE_BASE_EXT) {
            return dslContext.selectFrom(this)
                .where(STORE_ID.`in`(storeIds))
                .fetch()
        }
    }

    fun getBaseExtByStoreId(
        dslContext: DSLContext,
        storeId: String,
        fieldName: String? = null
    ): Result<TStoreBaseExtRecord> {
        return with(TStoreBaseExt.T_STORE_BASE_EXT) {
            val conditions = mutableListOf(STORE_ID.eq(storeId))
            if (!fieldName.isNullOrBlank()) {
                conditions.add(FIELD_NAME.eq(fieldName))
            }
            dslContext.selectFrom(this).where(conditions).fetch()
        }
    }
}
