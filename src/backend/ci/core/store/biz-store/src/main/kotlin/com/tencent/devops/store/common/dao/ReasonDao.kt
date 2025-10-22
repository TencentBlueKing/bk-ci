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

import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.model.store.tables.TReason
import com.tencent.devops.model.store.tables.records.TReasonRecord
import com.tencent.devops.store.pojo.common.reason.Reason
import com.tencent.devops.store.pojo.common.reason.ReasonReq
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ReasonDao {

    fun add(dslContext: DSLContext, id: String, userId: String, type: String, reasonReq: ReasonReq) {
        with(TReason.T_REASON) {
            dslContext.insertInto(
                this,
                ID,
                TYPE,
                CONTENT,
                MODIFIER,
                CREATOR,
                ORDER
            )
                .values(
                    id,
                    type,
                    reasonReq.content,
                    userId,
                    userId,
                    reasonReq.order
                ).execute()
        }
    }

    fun update(dslContext: DSLContext, id: String, userId: String, reasonReq: ReasonReq) {
        with(TReason.T_REASON) {
            dslContext.update(this)
                .set(CONTENT, reasonReq.content)
                .set(MODIFIER, userId)
                .set(UPDATE_TIME, LocalDateTime.now())
                .set(ORDER, reasonReq.order)
                .where(ID.eq(id))
                .execute()
        }
    }

    fun enable(dslContext: DSLContext, id: String, userId: String, enable: Boolean) {
        with(TReason.T_REASON) {
            dslContext.update(this)
                .set(ENABLE, enable)
                .set(MODIFIER, userId)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(ID.eq(id))
                .execute()
        }
    }

    fun delete(dslContext: DSLContext, id: String) {
        with(TReason.T_REASON) {
            dslContext.deleteFrom(this).where(ID.eq(id)).execute()
        }
    }

    fun get(dslContext: DSLContext, id: String): TReasonRecord? {
        with(TReason.T_REASON) {
            return dslContext.selectFrom(this).where(ID.eq(id)).fetchOne()
        }
    }

    fun list(dslContext: DSLContext, type: String, enable: Boolean?): Result<TReasonRecord> {
        with(TReason.T_REASON) {
            val condition = mutableListOf<Condition>()
            condition.add(TYPE.eq(type))
            if (enable != null) {
                condition.add(ENABLE.eq(enable))
            }
            return dslContext.selectFrom(this)
                .where(condition)
                .orderBy(ORDER.asc())
                .fetch()
        }
    }

    fun convert(record: TReasonRecord): Reason {
        with(record) {
            return Reason(
                id = id,
                type = type,
                content = content,
                modifier = modifier,
                creator = creator,
                createTime = DateTimeUtil.toDateTime(createTime),
                updateTime = DateTimeUtil.toDateTime(createTime),
                enable = enable,
                order = order
            )
        }
    }
}
