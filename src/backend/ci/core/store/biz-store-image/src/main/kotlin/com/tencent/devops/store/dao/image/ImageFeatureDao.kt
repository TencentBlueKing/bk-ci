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

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.model.store.tables.TImage
import com.tencent.devops.model.store.tables.TImageFeature
import com.tencent.devops.model.store.tables.TStoreProjectRel
import com.tencent.devops.model.store.tables.records.TImageFeatureRecord
import com.tencent.devops.store.constant.StoreMessageCode.USER_IMAGE_NOT_EXIST
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_CODE
import com.tencent.devops.store.pojo.common.enums.StoreProjectTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.image.enums.ImageRDTypeEnum
import com.tencent.devops.store.pojo.image.enums.ImageStatusEnum
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record1
import org.jooq.Result
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Suppress("ALL")
@Repository
class ImageFeatureDao {
    fun getImageFeature(dslContext: DSLContext, imageCode: String): TImageFeatureRecord {
        with(TImageFeature.T_IMAGE_FEATURE) {
            return dslContext.selectFrom(this)
                .where(IMAGE_CODE.eq(imageCode))
                .fetchOne()
                ?: throw ErrorCodeException(
                    statusCode = 400,
                    errorCode = USER_IMAGE_NOT_EXIST,
                    defaultMessage = "no imageFeature for imageCode=$imageCode",
                    params = arrayOf(imageCode)
                )
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
        rdType: ImageRDTypeEnum?,
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
            if (rdType != null) {
                baseQuery = baseQuery.set(IMAGE_TYPE, rdType.type.toByte())
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
     * 带offset与limit查询公共非调试镜像代码
     */
    fun getPublicImageCodes(
        dslContext: DSLContext,
        projectCode: String,
        imageStatusSet: Set<ImageStatusEnum>?,
        offset: Int? = 0,
        limit: Int? = -1
    ): Result<Record1<String>>? {
        val tStoreProjectRel = TStoreProjectRel.T_STORE_PROJECT_REL.`as`("tStoreProjectRel")
        val tImageFeature = TImageFeature.T_IMAGE_FEATURE.`as`("tImageFeature")
        val tImage = TImage.T_IMAGE.`as`("tImage")
        // 先查出项目的调试项目
        val debugImageCodes = dslContext.select(tStoreProjectRel.STORE_CODE).from(tStoreProjectRel)
            .where(tStoreProjectRel.PROJECT_CODE.eq(projectCode))
            .and(tStoreProjectRel.TYPE.eq(StoreProjectTypeEnum.TEST.type.toByte()))
            .and(tStoreProjectRel.STORE_TYPE.eq(StoreTypeEnum.IMAGE.type.toByte()))
            .fetch()
        val conditions = mutableListOf<Condition>()
        // 镜像
        conditions.add(tStoreProjectRel.STORE_TYPE.eq(StoreTypeEnum.IMAGE.type.toByte()))
        // 公共
        conditions.add(tImageFeature.PUBLIC_FLAG.eq(true))
        // 非调试
        conditions.add(tStoreProjectRel.STORE_CODE.notIn(debugImageCodes))
        if (imageStatusSet != null && imageStatusSet.isNotEmpty()) {
            conditions.add(tImage.IMAGE_STATUS.`in`(imageStatusSet.map { it.status.toByte() }))
        }
        val baseQuery =
            dslContext.selectDistinct(tImageFeature.IMAGE_CODE.`as`(KEY_IMAGE_CODE)).from(tImageFeature).join(tImage)
                .on(tImageFeature.IMAGE_CODE.eq(tImage.IMAGE_CODE))
                .join(tStoreProjectRel).on(tImageFeature.IMAGE_CODE.eq(tStoreProjectRel.STORE_CODE))
                .where(conditions)
        if (offset != null && offset >= 0) {
            baseQuery.offset(offset)
        }
        if (limit != null && limit > 0) {
            baseQuery.limit(limit)
        }
        return baseQuery.fetch()
    }

    fun countPublicImageCodes(
        dslContext: DSLContext,
        projectCode: String,
        imageStatusSet: Set<ImageStatusEnum>?
    ): Int {
        val tStoreProjectRel = TStoreProjectRel.T_STORE_PROJECT_REL.`as`("tStoreProjectRel")
        val tImageFeature = TImageFeature.T_IMAGE_FEATURE.`as`("tImageFeature")
        val tImage = TImage.T_IMAGE.`as`("tImage")
        // 先查出项目的调试项目
        val debugImageCodes = dslContext.select(tStoreProjectRel.STORE_CODE).from(tStoreProjectRel)
            .where(tStoreProjectRel.PROJECT_CODE.eq(projectCode))
            .and(tStoreProjectRel.TYPE.eq(StoreProjectTypeEnum.TEST.type.toByte()))
            .and(tStoreProjectRel.STORE_TYPE.eq(StoreTypeEnum.IMAGE.type.toByte()))
            .fetch()
        val conditions = mutableListOf<Condition>()
        // 镜像
        conditions.add(tStoreProjectRel.STORE_TYPE.eq(StoreTypeEnum.IMAGE.type.toByte()))
        // 公共
        conditions.add(tImageFeature.PUBLIC_FLAG.eq(true))
        // 非调试
        conditions.add(tStoreProjectRel.STORE_CODE.notIn(debugImageCodes))
        if (imageStatusSet != null && imageStatusSet.isNotEmpty()) {
            conditions.add(tImage.IMAGE_STATUS.`in`(imageStatusSet.map { it.status.toByte() }))
        }
        val baseQuery =
            dslContext.select(DSL.countDistinct(tImageFeature.IMAGE_CODE)).from(tImageFeature).join(tImage)
                .on(tImageFeature.IMAGE_CODE.eq(tImage.IMAGE_CODE))
                .join(tStoreProjectRel).on(tImageFeature.IMAGE_CODE.eq(tStoreProjectRel.STORE_CODE))
                .where(conditions)
        return baseQuery.fetchOne()!!.get(0, Int::class.java)
    }

    fun countByCode(dslContext: DSLContext, imageCode: String): Int {
        val tImageFeature = TImageFeature.T_IMAGE_FEATURE.`as`("tImageFeature")
        with(tImageFeature) {
            val baseQuery =
                dslContext.select(DSL.countDistinct(IMAGE_CODE)).from(this)
                    .where(IMAGE_CODE.eq(imageCode))
            return baseQuery.fetchOne()!!.get(0, Int::class.java)
        }
    }
}
