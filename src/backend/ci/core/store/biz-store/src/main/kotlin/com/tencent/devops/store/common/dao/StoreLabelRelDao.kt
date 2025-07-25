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

import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.db.utils.skipCheck
import com.tencent.devops.model.store.tables.TLabel
import com.tencent.devops.model.store.tables.TStoreBase
import com.tencent.devops.model.store.tables.TStoreLabelRel
import com.tencent.devops.store.pojo.common.KEY_CREATE_TIME
import com.tencent.devops.store.pojo.common.KEY_ID
import com.tencent.devops.store.pojo.common.KEY_LABEL_CODE
import com.tencent.devops.store.pojo.common.KEY_LABEL_ID
import com.tencent.devops.store.pojo.common.KEY_LABEL_NAME
import com.tencent.devops.store.pojo.common.KEY_LABEL_TYPE
import com.tencent.devops.store.pojo.common.KEY_UPDATE_TIME
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class StoreLabelRelDao {

    fun getLabelsByStoreIds(
        dslContext: DSLContext,
        storeIds: Set<String>
    ): Result<out Record>? {
        val tLabel = TLabel.T_LABEL
        val tStoreLabelRel = TStoreLabelRel.T_STORE_LABEL_REL
        return dslContext.select(
            tLabel.ID.`as`(KEY_LABEL_ID),
            tLabel.LABEL_CODE.`as`(KEY_LABEL_CODE),
            tLabel.LABEL_NAME.`as`(KEY_LABEL_NAME),
            tLabel.TYPE.`as`(KEY_LABEL_TYPE),
            tLabel.CREATE_TIME.`as`(KEY_CREATE_TIME),
            tLabel.UPDATE_TIME.`as`(KEY_UPDATE_TIME),
            tStoreLabelRel.STORE_ID.`as`(KEY_ID)
        ).from(tLabel).join(tStoreLabelRel).on(tLabel.ID.eq(tStoreLabelRel.LABEL_ID))
            .where(tStoreLabelRel.STORE_ID.`in`(storeIds))
            .skipCheck()
            .fetch()
    }

    fun deleteByStoreId(dslContext: DSLContext, storeId: String) {
        with(TStoreLabelRel.T_STORE_LABEL_REL) {
            dslContext.deleteFrom(this)
                .where(STORE_ID.eq(storeId))
                .execute()
        }
    }

    fun deleteByStoreCode(dslContext: DSLContext, storeCode: String, storeType: StoreTypeEnum) {
        val tStoreBase = TStoreBase.T_STORE_BASE
        val storeIds = dslContext.select(tStoreBase.ID).from(tStoreBase)
            .where(tStoreBase.STORE_CODE.eq(storeCode).and(tStoreBase.STORE_TYPE.eq(storeType.type.toByte()))).fetch()
        with(TStoreLabelRel.T_STORE_LABEL_REL) {
            dslContext.deleteFrom(this)
                .where(STORE_ID.`in`(storeIds))
                .execute()
        }
    }

    fun batchAdd(dslContext: DSLContext, userId: String, storeId: String, labelIdList: List<String>) {
        with(TStoreLabelRel.T_STORE_LABEL_REL) {
            val addStep = labelIdList.map {
                dslContext.insertInto(
                    this,
                    ID,
                    STORE_ID,
                    LABEL_ID,
                    CREATOR,
                    MODIFIER
                )
                    .values(
                        UUIDUtil.generate(),
                        storeId,
                        it,
                        userId,
                        userId
                    )
            }
            dslContext.batch(addStep).execute()
        }
    }

    fun batchDeleteByStoreId(dslContext: DSLContext, storeIds: List<String>) {
        with(TStoreLabelRel.T_STORE_LABEL_REL) {
            dslContext.deleteFrom(this)
                .where(STORE_ID.`in`(storeIds))
                .execute()
        }
    }
}
