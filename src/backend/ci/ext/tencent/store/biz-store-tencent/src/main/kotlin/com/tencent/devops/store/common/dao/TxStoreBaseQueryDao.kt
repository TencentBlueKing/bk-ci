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

import com.tencent.devops.common.api.constant.INIT_VERSION
import com.tencent.devops.model.store.tables.TStoreBase
import com.tencent.devops.model.store.tables.TTemplate
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.jooq.DSLContext
import org.jooq.Record2
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class TxStoreBaseQueryDao {

    fun listStoreInitCreator(
        dslContext: DSLContext,
        storeTypeEnum: StoreTypeEnum,
        offset: Int,
        limit: Int
    ): Result<Record2<String, String>> {
        with(TStoreBase.T_STORE_BASE) {
            return dslContext.select(STORE_CODE, CREATOR)
                .from(this)
                .where(STORE_TYPE.eq(storeTypeEnum.type.toByte()))
                .and(VERSION.eq(INIT_VERSION))
                .groupBy(STORE_CODE)
                .limit(limit).offset(offset)
                .fetch()
        }
    }

    fun listTempLateInitCreator(
        dslContext: DSLContext,
        offset: Int,
        limit: Int
    ): Result<Record2<String, String>> {
        with(TTemplate.T_TEMPLATE) {
            return dslContext.select(TEMPLATE_CODE, CREATOR)
                .from(this)
                .where(LATEST_FLAG.eq(true))
                .groupBy(TEMPLATE_CODE)
                .limit(limit).offset(offset)
                .fetch()
        }
    }
}
