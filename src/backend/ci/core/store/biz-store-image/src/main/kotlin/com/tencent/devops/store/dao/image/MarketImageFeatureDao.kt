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

import com.tencent.devops.common.api.exception.DataConsistencyException
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.model.store.tables.TImageFeature
import com.tencent.devops.model.store.tables.records.TImageFeatureRecord
import com.tencent.devops.store.pojo.image.request.ImageFeatureCreateRequest
import com.tencent.devops.store.pojo.image.request.ImageFeatureUpdateRequest
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
@Suppress("ALL")
class MarketImageFeatureDao {

    /**
     * 添加镜像特性
     */
    fun addImageFeature(dslContext: DSLContext, userId: String, imageFeatureCreateRequest: ImageFeatureCreateRequest) {
        with(TImageFeature.T_IMAGE_FEATURE) {
            dslContext.insertInto(this,
                ID,
                IMAGE_CODE,
                CREATOR,
                MODIFIER
            )
                .values(
                    UUIDUtil.generate(),
                    imageFeatureCreateRequest.imageCode,
                    userId,
                    userId
                ).execute()
        }
    }

    /**
     * 获取镜像特性
     */
    fun getImageFeature(dslContext: DSLContext, imageCode: String): TImageFeatureRecord? {
        with(TImageFeature.T_IMAGE_FEATURE) {
            return dslContext.selectFrom(this)
                .where(IMAGE_CODE.eq(imageCode))
                .fetchOne()
        }
    }

    /**
     * 安全限制方法，仅应当使用存在的imageCode调用此方法
     * 获取镜像特性
     */
    fun getExistedImageFeature(dslContext: DSLContext, imageCode: String): TImageFeatureRecord {
        with(TImageFeature.T_IMAGE_FEATURE) {
            return dslContext.selectFrom(this)
                .where(IMAGE_CODE.eq(imageCode))
                .fetchOne()
                ?: throw DataConsistencyException(
                    "imageCode=$imageCode",
                    "TImageFeature",
                    "Not Exist"
                )
        }
    }

    /**
     * 更新镜像特性
     */
    fun updateImageFeature(
        dslContext: DSLContext,
        userId: String,
        imageFeatureUpdateRequest: ImageFeatureUpdateRequest
    ) {
        with(TImageFeature.T_IMAGE_FEATURE) {
            val baseStep = dslContext.update(this)
            val publicFlag = imageFeatureUpdateRequest.publicFlag
            if (null != publicFlag) {
                baseStep.set(PUBLIC_FLAG, publicFlag)
            }
            val recommendFlag = imageFeatureUpdateRequest.recommendFlag
            if (null != recommendFlag) {
                baseStep.set(RECOMMEND_FLAG, recommendFlag)
            }
            val certificationFlag = imageFeatureUpdateRequest.certificationFlag
            if (null != certificationFlag) {
                baseStep.set(CERTIFICATION_FLAG, certificationFlag)
            }
            val deleteFlag = imageFeatureUpdateRequest.deleteFlag
            if (null != deleteFlag) {
                baseStep.set(DELETE_FLAG, deleteFlag)
            }
            baseStep.set(MODIFIER, userId)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(IMAGE_CODE.eq(imageFeatureUpdateRequest.imageCode))
                .execute()
        }
    }

    fun daleteImageFeature(dslContext: DSLContext, imageCode: String) {
        with(TImageFeature.T_IMAGE_FEATURE) {
            dslContext.deleteFrom(this)
                .where(IMAGE_CODE.eq(imageCode))
                .execute()
        }
    }
}
