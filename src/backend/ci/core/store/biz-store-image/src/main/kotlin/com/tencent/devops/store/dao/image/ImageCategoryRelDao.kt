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
package com.tencent.devops.store.dao.image

import com.tencent.devops.model.store.tables.TCategory
import com.tencent.devops.model.store.tables.TImageCategoryRel
import com.tencent.devops.store.constant.StoreMessageCode.USER_IMAGE_UNKNOWN_IMAGE_CATEGORY
import com.tencent.devops.store.exception.image.CategoryNotExistException
import com.tencent.devops.store.pojo.common.KEY_CATEGORY_CODE
import com.tencent.devops.store.pojo.common.KEY_CATEGORY_ICON_URL
import com.tencent.devops.store.pojo.common.KEY_CATEGORY_ID
import com.tencent.devops.store.pojo.common.KEY_CATEGORY_NAME
import com.tencent.devops.store.pojo.common.KEY_CATEGORY_TYPE
import com.tencent.devops.store.pojo.common.KEY_CREATE_TIME
import com.tencent.devops.store.pojo.common.KEY_UPDATE_TIME
import com.tencent.devops.store.pojo.image.enums.CategoryTypeEnum
import org.jooq.DSLContext
import org.jooq.Record1
import org.jooq.Record7
import org.jooq.Result
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ImageCategoryRelDao {
    fun getCategoryCodeByImageId(dslContext: DSLContext, imageId: String): Result<Record1<String>>? {
        val tImageCategoryRel = TImageCategoryRel.T_IMAGE_CATEGORY_REL.`as`("tImageCategoryRel")
        val tCategory = TCategory.T_CATEGORY.`as`("tCategory")
        return dslContext.select(
            tCategory.CATEGORY_CODE.`as`(KEY_CATEGORY_CODE)
        ).from(tImageCategoryRel).join(tCategory)
            .on(tImageCategoryRel.CATEGORY_ID.eq(tCategory.ID))
            .where(tImageCategoryRel.IMAGE_ID.eq(imageId))
            .fetch()
    }

    fun getImageIdsByCategoryIds(
        dslContext: DSLContext,
        categoryIds: Set<String>
    ): Result<Record1<String>>? {
        with(TImageCategoryRel.T_IMAGE_CATEGORY_REL) {
            return dslContext.select(IMAGE_ID).from(this)
                .where(CATEGORY_ID.`in`(categoryIds))
                .fetch()
        }
    }

    fun getCategorysByImageId(
        dslContext: DSLContext,
        imageId: String
    ): Result<Record7<String, String, String, String, Byte, LocalDateTime, LocalDateTime>>? {
        val a = TCategory.T_CATEGORY.`as`("a")
        val b = TImageCategoryRel.T_IMAGE_CATEGORY_REL.`as`("b")
        return dslContext.select(
            a.ID.`as`(KEY_CATEGORY_ID),
            a.CATEGORY_CODE.`as`(KEY_CATEGORY_CODE),
            a.CATEGORY_NAME.`as`(KEY_CATEGORY_NAME),
            a.ICON_URL.`as`(KEY_CATEGORY_ICON_URL),
            a.TYPE.`as`(KEY_CATEGORY_TYPE),
            a.CREATE_TIME.`as`(KEY_CREATE_TIME),
            a.UPDATE_TIME.`as`(KEY_UPDATE_TIME)
        ).from(a).join(b).on(a.ID.eq(b.CATEGORY_ID))
            .where(b.IMAGE_ID.eq(imageId))
            .fetch()
    }

    fun deleteByImageId(dslContext: DSLContext, imageId: String) {
        with(TImageCategoryRel.T_IMAGE_CATEGORY_REL) {
            dslContext.deleteFrom(this)
                .where(IMAGE_ID.eq(imageId))
                .execute()
        }
    }

    fun batchDeleteByImageId(dslContext: DSLContext, imageIds: List<String>) {
        with(TImageCategoryRel.T_IMAGE_CATEGORY_REL) {
            dslContext.deleteFrom(this)
                .where(IMAGE_ID.`in`(imageIds))
                .execute()
        }
    }

    fun batchAdd(dslContext: DSLContext, userId: String, imageId: String, categoryIdList: List<String>) {
        with(TImageCategoryRel.T_IMAGE_CATEGORY_REL) {
            val addStep = categoryIdList.map {
                dslContext.insertInto(
                    this,
                    ID,
                    IMAGE_ID,
                    CATEGORY_ID,
                    CREATOR,
                    MODIFIER
                )
                    .values(
                        com.tencent.devops.common.api.util.UUIDUtil.generate(),
                        imageId,
                        it,
                        userId,
                        userId
                    )
            }
            dslContext.batch(addStep).execute()
        }
    }

    fun updateCategory(dslContext: DSLContext, userId: String, imageId: String, categoryCode: String?) {
        if (!categoryCode.isNullOrBlank()) {
            dslContext.transaction { configuration ->
                val context = DSL.using(configuration)
                // 根据categoryCode查出对应的ID
                val tCategory = TCategory.T_CATEGORY.`as`("tCategory")
                val categoryIdRecords = context.select(tCategory.ID).from(tCategory).where(
                    tCategory.CATEGORY_CODE.eq(categoryCode)
                        .and(tCategory.TYPE.eq(CategoryTypeEnum.IMAGE.type.toByte()))
                ).fetch()
                if (categoryIdRecords.size == 0) {
                    throw CategoryNotExistException(
                        message = "category not exist,categoryCode=$categoryCode",
                        errorCode = USER_IMAGE_UNKNOWN_IMAGE_CATEGORY,
                        params = arrayOf(categoryCode ?: "")
                    )
                }
                val categoryId = categoryIdRecords[0].get(0) as String
                deleteByImageId(context, imageId)
                batchAdd(context, userId, imageId, listOf(categoryId))
            }
        }
    }
}
