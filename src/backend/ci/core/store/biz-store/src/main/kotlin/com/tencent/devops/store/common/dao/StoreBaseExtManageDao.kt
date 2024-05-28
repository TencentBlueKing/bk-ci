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

package com.tencent.devops.store.common.dao

import com.tencent.devops.model.store.tables.TStoreBaseExt
import com.tencent.devops.store.pojo.common.publication.StoreBaseExtDataPO
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.jooq.util.mysql.MySQLDSL
import org.springframework.stereotype.Repository

@Repository
class StoreBaseExtManageDao {

    fun batchSave(
        dslContext: DSLContext,
        storeBaseExtDataPOs: List<StoreBaseExtDataPO>
    ) {
        with(TStoreBaseExt.T_STORE_BASE_EXT) {
            dslContext.insertInto(
                this,
                ID,
                STORE_ID,
                STORE_CODE,
                STORE_TYPE,
                FIELD_NAME,
                FIELD_VALUE,
                CREATOR,
                MODIFIER,
                UPDATE_TIME,
                CREATE_TIME
            ).also { insert ->
                storeBaseExtDataPOs.forEach { storeBaseExtDataPO ->
                    insert.values(
                        storeBaseExtDataPO.id,
                        storeBaseExtDataPO.storeId,
                        storeBaseExtDataPO.storeCode,
                        storeBaseExtDataPO.storeType.type.toByte(),
                        storeBaseExtDataPO.fieldName,
                        storeBaseExtDataPO.fieldValue,
                        storeBaseExtDataPO.creator,
                        storeBaseExtDataPO.modifier,
                        storeBaseExtDataPO.updateTime,
                        storeBaseExtDataPO.createTime
                    )
                }
            }
                .onDuplicateKeyUpdate()
                .set(FIELD_NAME, MySQLDSL.values(FIELD_NAME))
                .set(FIELD_VALUE, MySQLDSL.values(FIELD_VALUE))
                .set(MODIFIER, MySQLDSL.values(MODIFIER))
                .set(UPDATE_TIME, LocalDateTime.now())
                .execute()
        }
    }

    fun deleteStoreBaseExtInfo(dslContext: DSLContext, storeId: String) {
        with(TStoreBaseExt.T_STORE_BASE_EXT) {
            dslContext.deleteFrom(this)
                .where(STORE_ID.eq(storeId))
                .execute()
        }
    }

    fun batchDeleteStoreBaseExtInfo(dslContext: DSLContext, storeIds: List<String>) {
        with(TStoreBaseExt.T_STORE_BASE_EXT) {
            dslContext.deleteFrom(this)
                .where(STORE_ID.`in`(storeIds))
                .execute()
        }
    }
}
