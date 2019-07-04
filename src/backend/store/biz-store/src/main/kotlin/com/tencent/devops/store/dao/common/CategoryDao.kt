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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.store.dao.common

import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.model.store.tables.TCategory
import com.tencent.devops.model.store.tables.records.TCategoryRecord
import com.tencent.devops.store.pojo.common.Category
import com.tencent.devops.store.pojo.common.CategoryRequest
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class CategoryDao {

    fun add(dslContext: DSLContext, id: String, categoryRequest: CategoryRequest, type: Byte) {
        with(TCategory.T_CATEGORY) {
            dslContext.insertInto(
                this,
                ID,
                CATEGORY_CODE,
                CATEGORY_NAME,
                ICON_URL,
                TYPE
            )
                .values(
                    id,
                    categoryRequest.categoryCode,
                    categoryRequest.categoryName,
                    categoryRequest.iconUrl,
                    type
                ).execute()
        }
    }

    fun countByName(dslContext: DSLContext, categoryName: String, type: Byte): Int {
        with(TCategory.T_CATEGORY) {
            return dslContext.selectCount().from(this).where(CATEGORY_NAME.eq(categoryName).and(TYPE.eq(type)))
                .fetchOne(0, Int::class.java)
        }
    }

    fun countByCode(dslContext: DSLContext, categoryCode: String, type: Byte): Int {
        with(TCategory.T_CATEGORY) {
            return dslContext.selectCount().from(this).where(CATEGORY_CODE.eq(categoryCode).and(TYPE.eq(type)))
                .fetchOne(0, Int::class.java)
        }
    }

    fun delete(dslContext: DSLContext, id: String) {
        with(TCategory.T_CATEGORY) {
            dslContext.deleteFrom(this)
                .where(ID.eq(id))
                .execute()
        }
    }

    fun update(dslContext: DSLContext, id: String, categoryRequest: CategoryRequest) {
        with(TCategory.T_CATEGORY) {
            dslContext.update(this)
                .set(CATEGORY_CODE, categoryRequest.categoryCode)
                .set(CATEGORY_NAME, categoryRequest.categoryName)
                .set(ICON_URL, categoryRequest.iconUrl)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(ID.eq(id))
                .execute()
        }
    }

    fun getCategory(dslContext: DSLContext, id: String): TCategoryRecord? {
        with(TCategory.T_CATEGORY) {
            return dslContext.selectFrom(this)
                .where(ID.eq(id))
                .fetchOne()
        }
    }

    fun getAllCategory(dslContext: DSLContext, type: Byte): Result<TCategoryRecord>? {
        with(TCategory.T_CATEGORY) {
            return dslContext
                .selectFrom(this)
                .where(TYPE.eq(type))
                .orderBy(CREATE_TIME.desc())
                .fetch()
        }
    }

    fun convert(record: TCategoryRecord): Category {
        with(record) {
            return Category(
                id = id,
                categoryCode = categoryCode,
                categoryName = categoryName,
                iconUrl = iconUrl,
                categoryType = StoreTypeEnum.getStoreType(type.toInt()),
                createTime = createTime.timestampmilli(),
                updateTime = updateTime.timestampmilli()
            )
        }
    }
}