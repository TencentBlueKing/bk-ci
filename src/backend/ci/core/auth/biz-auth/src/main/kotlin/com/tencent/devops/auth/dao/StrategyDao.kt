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

package com.tencent.devops.auth.dao

import com.tencent.devops.auth.entity.StrategyInfo
import com.tencent.devops.common.db.utils.skipCheck
import com.tencent.devops.model.auth.tables.TAuthStrategy
import com.tencent.devops.model.auth.tables.records.TAuthStrategyRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class StrategyDao {

    fun create(dslContext: DSLContext, userId: String, strategyInfo: StrategyInfo): Int {
        with(TAuthStrategy.T_AUTH_STRATEGY) {
            return dslContext.insertInto(
                this,
                STRATEGY_NAME,
                STRATEGY_BODY,
                IS_DELETE,
                CREATE_USER,
                UPDATE_USER
            ).values(
                strategyInfo.name,
                strategyInfo.strategy,
                0,
                userId,
                ""
            ).execute()
        }
    }

    fun update(dslContext: DSLContext, id: Int, strategyInfo: StrategyInfo, userId: String) {
        with(TAuthStrategy.T_AUTH_STRATEGY) {
            val update = dslContext.update(this)

            if (strategyInfo.name != null) {
                update.set(STRATEGY_NAME, strategyInfo.name)
            }
            update.set(STRATEGY_BODY, strategyInfo.strategy)
                .set(UPDATE_USER, userId)
                .where(ID.eq(id))
                .execute()
        }
    }

    fun delete(dslContext: DSLContext, id: Int, userId: String) {
        with(TAuthStrategy.T_AUTH_STRATEGY) {
            dslContext.update(this).set(IS_DELETE, 1)
                .set(UPDATE_USER, userId)
                .where(ID.eq(id))
                .execute()
        }
    }

    fun get(dslContext: DSLContext, id: Int): TAuthStrategyRecord? {
        with(TAuthStrategy.T_AUTH_STRATEGY) {
            return dslContext.selectFrom(this).where(ID.eq(id).and(IS_DELETE.eq(0))).fetchOne()
        }
    }

    fun list(dslContext: DSLContext): Result<TAuthStrategyRecord>? {
        with(TAuthStrategy.T_AUTH_STRATEGY) {
            return dslContext.selectFrom(this)
                .where((IS_DELETE.eq(0)))
                .orderBy(CREATE_TIME.desc())
                .skipCheck()
                .fetch()
        }
    }

    fun getByName(dslContext: DSLContext, strategyName: String): TAuthStrategyRecord? {
        with(TAuthStrategy.T_AUTH_STRATEGY) {
            return dslContext.selectFrom(this).where(STRATEGY_NAME.eq(strategyName).and(IS_DELETE.eq(0))).fetchOne()
        }
    }
}
