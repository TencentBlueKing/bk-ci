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

import com.tencent.devops.model.store.tables.TImage
import com.tencent.devops.model.store.tables.TImageFeature
import com.tencent.devops.store.dao.common.AbstractStoreCommonDao
import com.tencent.devops.store.pojo.common.StoreBaseInfo
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository(value = "IMAGE_COMMON_DAO")
class ImageCommonDao : AbstractStoreCommonDao() {

    override fun getNewestStoreNameByCode(dslContext: DSLContext, storeCode: String): String? {
        return with(TImage.T_IMAGE) {
            dslContext.select(IMAGE_NAME).from(this)
                .where(IMAGE_CODE.eq(storeCode))
                .orderBy(CREATE_TIME.desc())
                .limit(1)
                .fetchOne(0, String::class.java)
        }
    }

    override fun getStorePublicFlagByCode(dslContext: DSLContext, storeCode: String): Boolean {
        return with(TImageFeature.T_IMAGE_FEATURE) {
            dslContext.select(PUBLIC_FLAG).from(this)
                .where(IMAGE_CODE.eq(storeCode))
                .fetchOne(0, Boolean::class.java)!!
        }
    }

    override fun getStoreCodeListByName(dslContext: DSLContext, storeName: String): Result<out Record>? {
        return with(TImage.T_IMAGE) {
            dslContext.select(IMAGE_CODE.`as`("storeCode")).from(this)
                .where(IMAGE_NAME.contains(storeName))
                .groupBy(IMAGE_CODE)
                .fetch()
        }
    }

    override fun getStoreNameById(dslContext: DSLContext, storeId: String): String? {
        return with(TImage.T_IMAGE) {
            dslContext.select(IMAGE_NAME).from(this).where(ID.eq(storeId)).fetchOne(0, String::class.java)
        }
    }

    override fun getStoreNameByCode(dslContext: DSLContext, storeCode: String): String? {
        return with(TImage.T_IMAGE) {
            dslContext.select(IMAGE_NAME)
                .from(this)
                .where(IMAGE_CODE.eq(storeCode).and(LATEST_FLAG.eq(true)))
                .fetchOne(0, String::class.java)
        }
    }

    override fun getLatestStoreInfoListByCodes(
        dslContext: DSLContext,
        storeCodeList: List<String>
    ): Result<out Record>? {
        return with(TImage.T_IMAGE) {
            dslContext.select(
                IMAGE_CODE.`as`("storeCode"),
                VERSION.`as`("version")
            ).from(this)
                .where(IMAGE_CODE.`in`(storeCodeList))
                .and(LATEST_FLAG.eq(true))
                .fetch()
        }
    }

    override fun getStoreDevLanguages(dslContext: DSLContext, storeCode: String): List<String>? {
        return null
    }

    override fun getNewestStoreBaseInfoByCode(
        dslContext: DSLContext,
        storeCode: String,
        storeStatus: Byte?
    ): StoreBaseInfo? {
        val ti = TImage.T_IMAGE
        val tif = TImageFeature.T_IMAGE_FEATURE
        val conditions = mutableListOf<Condition>()
        conditions.add(ti.IMAGE_CODE.eq(storeCode))
        if (storeStatus != null) {
            conditions.add(ti.IMAGE_STATUS.eq(storeStatus))
        }
        val imageRecord = dslContext.selectFrom(ti)
            .where(conditions)
            .orderBy(ti.CREATE_TIME.desc())
            .limit(1)
            .fetchOne()
        return if (imageRecord != null) {
            val publicFlag = dslContext.select(tif.PUBLIC_FLAG).from(tif)
                .where(tif.IMAGE_CODE.eq(storeCode))
                .fetchOne(0, Boolean::class.java)!!
            StoreBaseInfo(
                storeId = imageRecord.id,
                storeCode = imageRecord.imageCode,
                storeName = imageRecord.imageName,
                version = imageRecord.version,
                publicFlag = publicFlag
            )
        } else {
            null
        }
    }
}
