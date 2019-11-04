/*
 * Tencent is pleased to support the open source community by making BK-REPO 蓝鲸制品库 available.
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
package com.tencent.devops.store.dao.image

import com.tencent.devops.model.store.tables.TImageFeature
import com.tencent.devops.model.store.tables.records.TImageFeatureRecord
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_CODE
import org.jooq.DSLContext
import org.jooq.Record1
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ImageFeatureDao {
    fun getImageFeature(dslContext: DSLContext, imageCode: String): TImageFeatureRecord? {
        with(TImageFeature.T_IMAGE_FEATURE) {
            return dslContext.selectFrom(this)
                .where(IMAGE_CODE.eq(imageCode))
                .fetchOne()
        }
    }

    fun deleteAll(dslContext: DSLContext, imageCode: String): Int {
        with(TImageFeature.T_IMAGE_FEATURE) {
            return dslContext.deleteFrom(this)
                .where(IMAGE_CODE.eq(imageCode))
                .execute()
        }
    }

    /**
     * 根据imageCode更新ImageFeature表中其余字段
     */
    fun update(
        dslContext: DSLContext,
        imageCode: String,
        publicFlag: Boolean?,
        recommendFlag: Boolean?,
        certificationFlag: Boolean?,
        modifier: String?,
        weight: Int? = null
    ): Int {
        with(TImageFeature.T_IMAGE_FEATURE) {
            var baseQuery = dslContext.update(this).set(UPDATE_TIME, LocalDateTime.now())
            if (publicFlag != null) {
                baseQuery = baseQuery.set(PUBLIC_FLAG, publicFlag)
            }
            if (recommendFlag != null) {
                baseQuery = baseQuery.set(RECOMMEND_FLAG, recommendFlag)
            }
            if (certificationFlag != null) {
                baseQuery = baseQuery.set(CERTIFICATION_FLAG, certificationFlag)
            }
            if (!modifier.isNullOrBlank()) {
                baseQuery = baseQuery.set(MODIFIER, modifier)
            }
            if (weight != null) {
                baseQuery = baseQuery.set(WEIGHT, weight)
            }
            return baseQuery.where(IMAGE_CODE.eq(imageCode)).execute()
        }
    }

    /**
     * 带offset与limit查询公共镜像代码
     */
    fun getPublicImageCodes(
        dslContext: DSLContext,
        offset: Int? = 0,
        limit: Int? = -1
    ): Result<Record1<String>>? {
        with(TImageFeature.T_IMAGE_FEATURE) {
            val baseQuery = dslContext.select(IMAGE_CODE.`as`(KEY_IMAGE_CODE)).from(this)
                .where(PUBLIC_FLAG.eq(true))
            if (offset != null && offset >= 0) {
                baseQuery.offset(offset)
            }
            if (limit != null && limit > 0) {
                baseQuery.limit(limit)
            }
            return baseQuery.fetch()
        }
    }

    fun countPublicImageCodes(
        dslContext: DSLContext
    ): Int {
        with(TImageFeature.T_IMAGE_FEATURE) {
            val baseQuery = dslContext.selectCount().from(this)
                .where(PUBLIC_FLAG.eq(true))
            return baseQuery.fetchOne().get(0, Int::class.java)
        }
    }
}