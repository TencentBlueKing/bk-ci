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

package com.tencent.devops.store.dao.common

import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.model.store.tables.TStoreCodecc
import com.tencent.devops.store.pojo.common.StoreCodeccInfo
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class TxStoreCodeccDao {

    /**
     * 计算评分
     */
    fun calScore(dslContext: DSLContext, calSql: String, thousandCcnIndex: Double): Double {
        return dslContext.fetchOne(calSql, thousandCcnIndex).get(0, Double::class.java)
    }

    fun addStoreCodeccScore(dslContext: DSLContext, userId: String, storeCodeccInfo: StoreCodeccInfo) {
        with(TStoreCodecc.T_STORE_CODECC) {
            dslContext.insertInto(
                this,
                ID,
                STORE_ID,
                STORE_CODE,
                STORE_TYPE,
                BUILD_ID,
                TASK_ID,
                CODE_STYLE_SCORE,
                CODE_SECURITY_SCORE,
                CODE_MEASURE_SCORE,
                MODIFIER,
                CREATOR
            )
                .values(
                    UUIDUtil.generate(),
                    storeCodeccInfo.storeId,
                    storeCodeccInfo.storeCode,
                    storeCodeccInfo.storeType.type.toByte(),
                    storeCodeccInfo.buildId,
                    storeCodeccInfo.taskId,
                    storeCodeccInfo.codeStyleScore,
                    storeCodeccInfo.codeSecurityScore,
                    storeCodeccInfo.codeMeasureScore,
                    userId,
                    userId
                ).execute()
        }
    }
}