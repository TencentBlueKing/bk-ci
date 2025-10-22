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

import com.tencent.devops.model.store.tables.TStoreBaseFeature
import com.tencent.devops.model.store.tables.records.TStoreBaseFeatureRecord
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class StoreBaseFeatureQueryDao {

    fun getBaseFeatureByCode(
        dslContext: DSLContext,
        storeCode: String,
        storeType: StoreTypeEnum
    ): TStoreBaseFeatureRecord? {
        return with(TStoreBaseFeature.T_STORE_BASE_FEATURE) {
            dslContext.selectFrom(this)
                .where(STORE_CODE.eq(storeCode).and(STORE_TYPE.eq(storeType.type.toByte())))
                .fetchOne()
        }
    }

    fun getComponentPublicFlagInfo(
        dslContext: DSLContext,
        storeCodes: List<String>,
        storeType: StoreTypeEnum
    ): Map<String, Boolean> {
        with(TStoreBaseFeature.T_STORE_BASE_FEATURE) {
            return dslContext.select(STORE_CODE, PUBLIC_FLAG)
                .from(this)
                .where(STORE_CODE.`in`(storeCodes).and(STORE_TYPE.eq(storeType.type.toByte())))
                .fetch().intoMap({ it.value1() }, { it.value2() as Boolean })
        }
    }

    fun getAllPublicComponent(
        dslContext: DSLContext,
        storeType: StoreTypeEnum
    ): List<String> {
        with(TStoreBaseFeature.T_STORE_BASE_FEATURE) {
            return dslContext.select(STORE_CODE)
                .from(this)
                .where(STORE_TYPE.eq(storeType.type.toByte()).and(PUBLIC_FLAG.eq(true)))
                .fetchInto(String::class.java)
        }
    }

    fun isPublic(
        dslContext: DSLContext,
        storeCode: String,
        storeType: StoreTypeEnum
    ): Boolean {
        with(TStoreBaseFeature.T_STORE_BASE_FEATURE) {
            return dslContext.select(PUBLIC_FLAG).from(this)
                .where(STORE_CODE.eq(storeCode).and(STORE_TYPE.eq(storeType.type.toByte())))
                .fetchOne(0, Boolean::class.java)!!
        }
    }
}
