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

package com.tencent.devops.store.dao.common

import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.model.store.tables.TStorePipelineRel
import com.tencent.devops.model.store.tables.records.TStorePipelineRelRecord
import com.tencent.devops.store.pojo.common.enums.StorePipelineBusTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class StorePipelineRelDao {

    fun add(
        dslContext: DSLContext,
        storeCode: String,
        storeType: StoreTypeEnum,
        pipelineId: String,
        busType: StorePipelineBusTypeEnum = StorePipelineBusTypeEnum.BUILD
    ) {
        with(TStorePipelineRel.T_STORE_PIPELINE_REL) {
            dslContext.insertInto(
                this,
                ID,
                STORE_CODE,
                STORE_TYPE,
                PIPELINE_ID,
                BUS_TYPE
            )
                .values(
                    UUIDUtil.generate(),
                    storeCode,
                    storeType.type.toByte(),
                    pipelineId,
                    busType.name
                ).execute()
        }
    }

    fun getStorePipelineRel(
        dslContext: DSLContext,
        storeCode: String,
        storeType: StoreTypeEnum,
        busType: StorePipelineBusTypeEnum = StorePipelineBusTypeEnum.BUILD
    ): TStorePipelineRelRecord? {
        with(TStorePipelineRel.T_STORE_PIPELINE_REL) {
            return dslContext.selectFrom(this)
                .where(STORE_CODE.eq(storeCode))
                .and(STORE_TYPE.eq(storeType.type.toByte()))
                .and(BUS_TYPE.eq(busType.name))
                .fetchOne()
        }
    }

    fun getStorePipelineRelByPipelineId(dslContext: DSLContext, pipelineId: String): TStorePipelineRelRecord? {
        with(TStorePipelineRel.T_STORE_PIPELINE_REL) {
            return dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .fetchOne()
        }
    }

    fun deleteStorePipelineRel(dslContext: DSLContext, storeCode: String, storeType: Byte) {
        with(TStorePipelineRel.T_STORE_PIPELINE_REL) {
            dslContext.deleteFrom(this)
                .where(STORE_CODE.eq(storeCode))
                .and(STORE_TYPE.eq(storeType))
                .execute()
        }
    }
}
