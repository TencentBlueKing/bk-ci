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

package com.tencent.devops.project.dao

import com.tencent.devops.model.project.tables.TNotice
import com.tencent.devops.model.project.tables.records.TNoticeRecord
import com.tencent.devops.project.pojo.NoticeRequest
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.sql.Timestamp

@Repository
class NoticeDao {

    fun getValidNotice(dslContext: DSLContext): TNoticeRecord? {
        val currentTimestamp = System.currentTimeMillis()
        with(TNotice.T_NOTICE) {
            val conditions = mutableListOf<Condition>()
            conditions.add(EFFECT_DATE.le(Timestamp(currentTimestamp).toLocalDateTime()))
            conditions.add(INVALID_DATE.ge(Timestamp(currentTimestamp).toLocalDateTime()))
            val notices = dslContext
                    .selectFrom(this)
                    .where(conditions)
                    .orderBy(ID.desc())
                    .fetch()
            return if (notices.size > 0) {
                notices[0]
            } else {
                null
            }
        }
    }

    fun getAllNotice(dslContext: DSLContext): Result<TNoticeRecord>? {
        with(TNotice.T_NOTICE) {
            return dslContext
                    .selectFrom(this)
                    .orderBy(ID.desc())
                    .fetch()
        }
    }

    fun getNotice(dslContext: DSLContext, id: Long): TNoticeRecord? {
        with(TNotice.T_NOTICE) {
            return dslContext.selectFrom(this)
                    .where(ID.eq(id))
                    .fetchOne()
        }
    }
    fun deleteNotice(dslContext: DSLContext, id: Long): Int {
        with(TNotice.T_NOTICE) {
            return dslContext.deleteFrom(this)
                    .where(ID.eq(id))
                    .execute()
        }
    }
    fun handleNotice(dslContext: DSLContext, id: Long?, noticeRequest: NoticeRequest): Int {
        val currentTimestamp = System.currentTimeMillis()
        with(TNotice.T_NOTICE) {
            val effectDate = Timestamp(noticeRequest.effectDate).toLocalDateTime()
            val invalidDate = Timestamp(noticeRequest.invalidDate).toLocalDateTime()
            return if (id != null && exist(dslContext, id)) {
                // 存在的时候，更新
                dslContext.update(this)
                        .set(NOTICE_TITLE, noticeRequest.noticeTitle)
                        .set(EFFECT_DATE, effectDate)
                        .set(INVALID_DATE, invalidDate)
                        .set(UPDATE_DATE, Timestamp(currentTimestamp).toLocalDateTime())
                        .set(NOTICE_CONTENT, noticeRequest.noticeContent)
                        .set(REDIRECT_URL, noticeRequest.redirectUrl)
                        .set(SERVICE_NAME, noticeRequest.noticeService?.joinToString(","))
                        .set(NOTICE_TYPE, noticeRequest.noticeType.toByte())
                        .where(ID.eq(id))
                        .execute()
            } else {
                // 不存在的时候,插入
                dslContext.insertInto(this,
                        NOTICE_TITLE,
                        EFFECT_DATE,
                        INVALID_DATE,
                        NOTICE_CONTENT,
                        REDIRECT_URL,
                        NOTICE_TYPE,
                        SERVICE_NAME
                ).values(
                        noticeRequest.noticeTitle,
                        effectDate,
                        invalidDate,
                        noticeRequest.noticeContent,
                        noticeRequest.redirectUrl,
                        noticeRequest.noticeType.toByte(),
                    noticeRequest.noticeService?.joinToString(",")
                ).execute()
            }
        }
    }

    fun exist(dslContext: DSLContext, id: Long): Boolean {
        with(TNotice.T_NOTICE) {
            return dslContext.selectFrom(this).where(ID.eq(id)).fetchOne() != null
        }
    }
}
