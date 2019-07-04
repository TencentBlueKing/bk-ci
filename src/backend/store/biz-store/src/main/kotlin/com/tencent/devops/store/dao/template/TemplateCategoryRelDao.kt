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

package com.tencent.devops.store.dao.template

import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.model.store.tables.TCategory
import com.tencent.devops.model.store.tables.TTemplateCategoryRel
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class TemplateCategoryRelDao {

    fun getCategorysByTemplateId(
        dslContext: DSLContext,
        templateId: String
    ): Result<out Record>? {
        val a = TCategory.T_CATEGORY.`as`("a")
        val b = TTemplateCategoryRel.T_TEMPLATE_CATEGORY_REL.`as`("b")
        return dslContext.select(
            a.ID.`as`("id"),
            a.CATEGORY_CODE.`as`("categoryCode"),
            a.CATEGORY_NAME.`as`("categoryName"),
            a.ICON_URL.`as`("iconUrl"),
            a.TYPE.`as`("categoryType"),
            a.CREATE_TIME.`as`("createTime"),
            a.UPDATE_TIME.`as`("updateTime")
        ).from(a).join(b).on(a.ID.eq(b.CATEGORY_ID))
            .where(b.TEMPLATE_ID.eq(templateId))
            .fetch()
    }

    fun deleteByTemplateId(dslContext: DSLContext, templateId: String) {
        with(TTemplateCategoryRel.T_TEMPLATE_CATEGORY_REL) {
            dslContext.deleteFrom(this)
                .where(TEMPLATE_ID.eq(templateId))
                .execute()
        }
    }

    fun batchAdd(dslContext: DSLContext, userId: String, templateId: String, categoryIdList: List<String>) {
        with(TTemplateCategoryRel.T_TEMPLATE_CATEGORY_REL) {
            val addStep = categoryIdList.map {
                dslContext.insertInto(
                    this,
                    ID,
                    TEMPLATE_ID,
                    CATEGORY_ID,
                    CREATOR,
                    MODIFIER
                )
                    .values(
                        UUIDUtil.generate(),
                        templateId,
                        it,
                        userId,
                        userId
                    )
            }
            dslContext.batch(addStep).execute()
        }
    }
}