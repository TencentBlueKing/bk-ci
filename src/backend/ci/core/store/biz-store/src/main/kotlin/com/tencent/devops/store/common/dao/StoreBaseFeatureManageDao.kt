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
import com.tencent.devops.store.pojo.common.publication.StoreBaseFeatureDataPO
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

@Repository
class StoreBaseFeatureManageDao {

    fun saveStoreBaseFeatureData(
        dslContext: DSLContext,
        storeBaseFeatureDataPO: StoreBaseFeatureDataPO
    ) {
        with(TStoreBaseFeature.T_STORE_BASE_FEATURE) {
            val publicFlag = storeBaseFeatureDataPO.publicFlag
            val recommendFlag = storeBaseFeatureDataPO.recommendFlag
            val certificationFlag = storeBaseFeatureDataPO.certificationFlag
            val showFlag = storeBaseFeatureDataPO.showFlag
            val type = storeBaseFeatureDataPO.type
            val rdType = storeBaseFeatureDataPO.rdType
            val weight = storeBaseFeatureDataPO.weight
            dslContext.insertInto(
                this,
                ID,
                STORE_CODE,
                STORE_TYPE,
                PUBLIC_FLAG,
                RECOMMEND_FLAG,
                CERTIFICATION_FLAG,
                SHOW_FLAG,
                TYPE,
                RD_TYPE,
                WEIGHT,
                CREATOR,
                MODIFIER,
                UPDATE_TIME,
                CREATE_TIME
            ).values(
                storeBaseFeatureDataPO.id,
                storeBaseFeatureDataPO.storeCode,
                storeBaseFeatureDataPO.storeType.type.toByte(),
                publicFlag ?: false,
                recommendFlag ?: true,
                certificationFlag ?: false,
                showFlag ?: true,
                type,
                rdType,
                weight,
                storeBaseFeatureDataPO.creator,
                storeBaseFeatureDataPO.modifier,
                storeBaseFeatureDataPO.updateTime,
                storeBaseFeatureDataPO.createTime
            )
                .onDuplicateKeyUpdate()
                .set(PUBLIC_FLAG, DSL.`when`(DSL.condition(publicFlag != null), publicFlag).otherwise(PUBLIC_FLAG))
                .set(
                    RECOMMEND_FLAG,
                    DSL.`when`(DSL.condition(recommendFlag != null), recommendFlag).otherwise(RECOMMEND_FLAG)
                )
                .set(
                    CERTIFICATION_FLAG,
                    DSL.`when`(DSL.condition(certificationFlag != null), certificationFlag)
                        .otherwise(CERTIFICATION_FLAG)
                )
                .set(SHOW_FLAG, DSL.`when`(DSL.condition(showFlag != null), showFlag).otherwise(SHOW_FLAG))
                .set(TYPE, DSL.`when`(DSL.condition(type != null), type).otherwise(TYPE))
                .set(RD_TYPE, DSL.`when`(DSL.condition(rdType != null), rdType).otherwise(RD_TYPE))
                .set(WEIGHT, DSL.`when`(DSL.condition(weight != null), weight).otherwise(WEIGHT))
                .set(MODIFIER, storeBaseFeatureDataPO.modifier)
                .set(UPDATE_TIME, LocalDateTime.now())
                .execute()
        }
    }

    fun deleteStoreBaseFeature(dslContext: DSLContext, storeCode: String, storeType: Byte) {
        with(TStoreBaseFeature.T_STORE_BASE_FEATURE) {
            dslContext.deleteFrom(this)
                .where(STORE_CODE.eq(storeCode))
                .and(STORE_TYPE.eq(storeType))
                .execute()
        }
    }
}
