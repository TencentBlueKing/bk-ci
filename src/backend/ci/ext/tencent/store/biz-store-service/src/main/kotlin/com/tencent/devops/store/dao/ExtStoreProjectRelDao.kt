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

package com.tencent.devops.store.dao

import com.tencent.devops.model.store.tables.TStoreProjectRel
import com.tencent.devops.model.store.tables.records.TStoreProjectRelRecord
import com.tencent.devops.store.pojo.common.enums.StoreProjectTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@Repository
class ExtStoreProjectRelDao {

    /**
     * 获取项目的调试组件
     */
    fun getStoreInstall(
        dslContext: DSLContext,
        storeCode: String,
        storeType: StoreTypeEnum,
        startTime: Long
    ): Result<TStoreProjectRelRecord>? {
        return with(TStoreProjectRel.T_STORE_PROJECT_REL) {
            val where = dslContext.selectFrom(this).where(
                STORE_TYPE.eq(storeType.type.toByte()).and(STORE_CODE.eq(storeCode).and(TYPE.eq(StoreProjectTypeEnum.COMMON.type.toByte())))
            )
            if (startTime > 0) {
                where.and(
                    CREATE_TIME.ge(
                        LocalDateTime.ofInstant(
                            Instant.ofEpochSecond(startTime),
                            ZoneId.systemDefault()
                        )
                    )
                )
            }
            where.orderBy(CREATE_TIME.desc())
                .fetch()
        }
    }
}
