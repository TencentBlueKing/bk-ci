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
import com.tencent.devops.model.store.tables.TStoreOptLog
import com.tencent.devops.store.pojo.common.OperationLogCreateRequest
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Suppress("ALL")
@Repository
class OperationLogDao {

    fun add(
        dslContext: DSLContext,
        id: String,
        storeCode: String,
        storeType: Byte,
        optType: String,
        optUser: String,
        optDesc: String
    ) {
        with(TStoreOptLog.T_STORE_OPT_LOG) {
            dslContext.insertInto(
                this,
                ID,
                STORE_CODE,
                STORE_TYPE,
                OPT_TYPE,
                OPT_DESC,
                OPT_USER,
                CREATOR
            )
                .values(
                    id,
                    storeCode,
                    storeType,
                    optType,
                    optDesc,
                    optUser,
                    optUser
                ).execute()
        }
    }

    fun batchAddLogs(
        dslContext: DSLContext,
        userId: String,
        operationLogList: List<OperationLogCreateRequest>
    ) {
        with(TStoreOptLog.T_STORE_OPT_LOG) {
            val addStep = operationLogList.map {
                dslContext.insertInto(
                    this,
                    ID,
                    STORE_CODE,
                    STORE_TYPE,
                    OPT_TYPE,
                    OPT_DESC,
                    OPT_USER,
                    CREATOR
                )
                    .values(
                        UUIDUtil.generate(),
                        it.storeCode,
                        it.storeType,
                        it.optType,
                        it.optDesc,
                        it.optUser,
                        userId
                    )
            }
            dslContext.batch(addStep).execute()
        }
    }

    fun deleteOperationLog(dslContext: DSLContext, storeCode: String, storeType: Byte) {
        with(TStoreOptLog.T_STORE_OPT_LOG) {
            dslContext.deleteFrom(this)
                .where(STORE_CODE.eq(storeCode).and(STORE_TYPE.eq(storeType)))
                .execute()
        }
    }
}
