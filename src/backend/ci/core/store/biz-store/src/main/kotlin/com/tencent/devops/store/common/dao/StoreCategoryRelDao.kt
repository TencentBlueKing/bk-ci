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
import com.tencent.devops.common.db.utils.skipCheck
import com.tencent.devops.model.store.tables.TCategory
import com.tencent.devops.model.store.tables.TStoreBase
import com.tencent.devops.model.store.tables.TStoreCategoryRel
import com.tencent.devops.model.store.tables.records.TCategoryRecord
import com.tencent.devops.store.pojo.common.KEY_CATEGORY_CODE
import com.tencent.devops.store.pojo.common.KEY_CATEGORY_ICON_URL
import com.tencent.devops.store.pojo.common.KEY_CATEGORY_ID
import com.tencent.devops.store.pojo.common.KEY_CATEGORY_NAME
import com.tencent.devops.store.pojo.common.KEY_CATEGORY_TYPE
import com.tencent.devops.store.pojo.common.KEY_CREATE_TIME
import com.tencent.devops.store.pojo.common.KEY_STORE_ID
import com.tencent.devops.store.pojo.common.KEY_UPDATE_TIME
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class StoreCategoryRelDao {

    fun getCategoriesByStoreId(
        dslContext: DSLContext,
        storeIds: Set<String>
    ): Result<out Record>? {
        val tCategory = TCategory.T_CATEGORY
        val tStoreCategoryRel = TStoreCategoryRel.T_STORE_CATEGORY_REL
        return dslContext.select(
            tCategory.ID.`as`(KEY_CATEGORY_ID),
            tCategory.CATEGORY_CODE.`as`(KEY_CATEGORY_CODE),
            tCategory.CATEGORY_NAME.`as`(KEY_CATEGORY_NAME),
            tCategory.ICON_URL.`as`(KEY_CATEGORY_ICON_URL),
            tCategory.TYPE.`as`(KEY_CATEGORY_TYPE),
            tCategory.CREATE_TIME.`as`(KEY_CREATE_TIME),
            tCategory.UPDATE_TIME.`as`(KEY_UPDATE_TIME)
        ).from(tCategory).join(tStoreCategoryRel).on(tCategory.ID.eq(tStoreCategoryRel.CATEGORY_ID))
            .where(tStoreCategoryRel.STORE_ID.`in`(storeIds))
            .skipCheck()
            .fetch()
    }

    fun deleteByStoreId(dslContext: DSLContext, storeId: String) {
        with(TStoreCategoryRel.T_STORE_CATEGORY_REL) {
            dslContext.deleteFrom(this)
                .where(STORE_ID.eq(storeId))
                .execute()
        }
    }

    fun deleteByStoreCode(dslContext: DSLContext, storeCode: String, storeType: StoreTypeEnum) {
        val tsb = TStoreBase.T_STORE_BASE
        val storeIds = dslContext.select(tsb.ID).from(tsb)
            .where(tsb.STORE_CODE.eq(storeCode).and(tsb.STORE_TYPE.eq(storeType.type.toByte()))).fetch()
        with(TStoreCategoryRel.T_STORE_CATEGORY_REL) {
            dslContext.deleteFrom(this)
                .where(STORE_ID.`in`(storeIds))
                .execute()
        }
    }

    fun batchAdd(dslContext: DSLContext, userId: String, storeId: String, categoryIdList: List<String>) {
        with(TStoreCategoryRel.T_STORE_CATEGORY_REL) {
            val addStep = categoryIdList.map {
                dslContext.insertInto(
                    this,
                    ID,
                    STORE_ID,
                    CATEGORY_ID,
                    CREATOR,
                    MODIFIER
                )
                    .values(
                        UUIDUtil.generate(),
                        storeId,
                        it,
                        userId,
                        userId
                    )
            }
            dslContext.batch(addStep).execute()
        }
    }

    fun getByStoreId(dslContext: DSLContext, storeId: String): List<TCategoryRecord> {
        val tStoreCategoryRel = TStoreCategoryRel.T_STORE_CATEGORY_REL
        with(TCategory.T_CATEGORY) {
            return dslContext.select(
                ID,
                CATEGORY_CODE,
                CATEGORY_NAME,
                ICON_URL,
                TYPE,
                CREATE_TIME,
                UPDATE_TIME,
                CREATOR,
                MODIFIER
            )
                .from(this)
                .join(tStoreCategoryRel)
                .on(ID.eq(tStoreCategoryRel.CATEGORY_ID))
                .where(tStoreCategoryRel.STORE_ID.eq(storeId))
                .skipCheck()
                .fetchInto(TCategoryRecord::class.java)
        }
    }

    fun batchQueryByStoreIds(dslContext: DSLContext, storeIds: List<String>): Result<out Record>? {
        val tStoreCategoryRel = TStoreCategoryRel.T_STORE_CATEGORY_REL
        with(TCategory.T_CATEGORY) {
            return dslContext.select(
                ID,
                tStoreCategoryRel.STORE_ID.`as`(KEY_STORE_ID),
                CATEGORY_CODE,
                CATEGORY_NAME,
                ICON_URL,
                TYPE,
                CREATE_TIME,
                UPDATE_TIME,
                CREATOR,
                MODIFIER
            )
                .from(this)
                .join(tStoreCategoryRel)
                .on(ID.eq(tStoreCategoryRel.CATEGORY_ID))
                .where(tStoreCategoryRel.STORE_ID.`in`(storeIds))
                .skipCheck()
                .fetch()
        }
    }
}
