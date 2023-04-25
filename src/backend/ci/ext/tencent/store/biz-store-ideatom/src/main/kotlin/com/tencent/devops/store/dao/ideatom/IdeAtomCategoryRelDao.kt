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

package com.tencent.devops.store.dao.ideatom

import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.model.store.tables.TCategory
import com.tencent.devops.model.store.tables.TIdeAtomCategoryRel
import com.tencent.devops.store.pojo.common.KEY_CATEGORY_CODE
import com.tencent.devops.store.pojo.common.KEY_CATEGORY_ICON_URL
import com.tencent.devops.store.pojo.common.KEY_CATEGORY_ID
import com.tencent.devops.store.pojo.common.KEY_CATEGORY_NAME
import com.tencent.devops.store.pojo.common.KEY_CATEGORY_TYPE
import com.tencent.devops.store.pojo.common.KEY_CREATE_TIME
import com.tencent.devops.store.pojo.common.KEY_UPDATE_TIME
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class IdeAtomCategoryRelDao {

    fun getCategorysByIdeAtomId(
        dslContext: DSLContext,
        atomId: String
    ): Result<out Record>? {
        val a = TCategory.T_CATEGORY.`as`("a")
        val b = TIdeAtomCategoryRel.T_IDE_ATOM_CATEGORY_REL.`as`("b")
        return dslContext.select(
            a.ID.`as`(KEY_CATEGORY_ID),
            a.CATEGORY_CODE.`as`(KEY_CATEGORY_CODE),
            a.CATEGORY_NAME.`as`(KEY_CATEGORY_NAME),
            a.ICON_URL.`as`(KEY_CATEGORY_ICON_URL),
            a.TYPE.`as`(KEY_CATEGORY_TYPE),
            a.CREATE_TIME.`as`(KEY_CREATE_TIME),
            a.UPDATE_TIME.`as`(KEY_UPDATE_TIME)
        ).from(a).join(b).on(a.ID.eq(b.CATEGORY_ID))
            .where(b.ATOM_ID.eq(atomId))
            .fetch()
    }

    fun deleteByIdeAtomId(dslContext: DSLContext, atomId: String) {
        with(TIdeAtomCategoryRel.T_IDE_ATOM_CATEGORY_REL) {
            dslContext.deleteFrom(this)
                .where(ATOM_ID.eq(atomId))
                .execute()
        }
    }

    fun batchAdd(dslContext: DSLContext, userId: String, atomId: String, categoryIdList: List<String>) {
        with(TIdeAtomCategoryRel.T_IDE_ATOM_CATEGORY_REL) {
            val addStep = categoryIdList.map {
                dslContext.insertInto(this,
                    ID,
                    ATOM_ID,
                    CATEGORY_ID,
                    CREATOR,
                    MODIFIER
                )
                    .values(
                        UUIDUtil.generate(),
                        atomId,
                        it,
                        userId,
                        userId
                    )
            }
            dslContext.batch(addStep).execute()
        }
    }
}
