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
package com.tencent.devops.notify.dao

import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.notify.enums.EnumNotifySource
import com.tencent.devops.model.notify.tables.TNotifySms
import com.tencent.devops.model.notify.tables.records.TNotifySmsRecord
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.ArrayList

@Repository
@Suppress("ALL")
class SmsNotifyDao @Autowired constructor(private val dslContext: DSLContext) {
    fun insertOrUpdateSmsNotifyRecord(
        success: Boolean,
        source: EnumNotifySource?,
        batchId: String?,
        id: String?,
        retryCount: Int?,
        lastErrorMessage: String?,
        receivers: String?,
        sender: String?,
        body: String?,
        priority: Int?,
        contentMd5: String?,
        frequencyLimit: Int?,
        tofSysId: String?,
        fromSysId: String?
    ) {
        val now = LocalDateTime.now()
        dslContext.insertInto(TNotifySms.T_NOTIFY_SMS,
            TNotifySms.T_NOTIFY_SMS.ID,
            TNotifySms.T_NOTIFY_SMS.BATCH_ID,
            TNotifySms.T_NOTIFY_SMS.SOURCE,
            TNotifySms.T_NOTIFY_SMS.RETRY_COUNT,
            TNotifySms.T_NOTIFY_SMS.LAST_ERROR,
            TNotifySms.T_NOTIFY_SMS.BODY,
            TNotifySms.T_NOTIFY_SMS.UPDATED_TIME,
            TNotifySms.T_NOTIFY_SMS.CREATED_TIME,
            TNotifySms.T_NOTIFY_SMS.SENDER,
            TNotifySms.T_NOTIFY_SMS.RECEIVERS,
            TNotifySms.T_NOTIFY_SMS.PRIORITY,
            TNotifySms.T_NOTIFY_SMS.SUCCESS,
            TNotifySms.T_NOTIFY_SMS.CONTENT_MD5,
            TNotifySms.T_NOTIFY_SMS.FREQUENCY_LIMIT,
            TNotifySms.T_NOTIFY_SMS.TOF_SYS_ID,
            TNotifySms.T_NOTIFY_SMS.FROM_SYS_ID)
            .values(
                id,
                batchId,
                source?.name,
                retryCount,
                lastErrorMessage,
                body,
                now,
                now,
                sender,
                receivers,
                priority,
                success,
                contentMd5,
                frequencyLimit,
                tofSysId,
                fromSysId
            )
            .onDuplicateKeyUpdate()
            .set(TNotifySms.T_NOTIFY_SMS.SUCCESS, success)
            .set(TNotifySms.T_NOTIFY_SMS.UPDATED_TIME, now)
            .set(TNotifySms.T_NOTIFY_SMS.LAST_ERROR, lastErrorMessage)
            .set(TNotifySms.T_NOTIFY_SMS.RETRY_COUNT, retryCount)
            .set(TNotifySms.T_NOTIFY_SMS.TOF_SYS_ID, tofSysId)
            .execute()
    }

    fun getReceiversByContentMd5AndTime(contentMd5: String, timeBeforeSeconds: Long): List<String> {
        with(TNotifySms.T_NOTIFY_SMS) {
            return dslContext.selectFrom(this)
                .where(CONTENT_MD5.eq(contentMd5))
                .and(SUCCESS.eq(true))
                .and(CREATED_TIME.greaterThan(LocalDateTime.now().minusSeconds(timeBeforeSeconds)))
                .fetch(RECEIVERS)
        }
    }

    fun count(success: Boolean?, fromSysId: String?): Int {
        return dslContext.selectCount()
            .from(TNotifySms.T_NOTIFY_SMS)
            .where(getListConditions(success, fromSysId))
            .fetchOne()!!
            .value1()
    }

    fun list(
        page: Int,
        pageSize: Int,
        success: Boolean?,
        fromSysId: String?,
        createdTimeSortOrder: String?
    ): Result<TNotifySmsRecord> {
        val sqlLimit = PageUtil.convertPageSizeToSQLLimit(page, pageSize)
        return dslContext.selectFrom(TNotifySms.T_NOTIFY_SMS)
            .where(getListConditions(success, fromSysId))
            .orderBy(if (createdTimeSortOrder != null && createdTimeSortOrder == "descend") {
                TNotifySms.T_NOTIFY_SMS.CREATED_TIME.desc()
            } else {
                TNotifySms.T_NOTIFY_SMS.CREATED_TIME.asc()
            })
            .limit(sqlLimit.offset, sqlLimit.limit)
            .fetch()
    }

    private fun getListConditions(success: Boolean?, fromSysId: String?): List<Condition> {
        val conditions = ArrayList<Condition>()
        if (success != null) {
            conditions.add(TNotifySms.T_NOTIFY_SMS.SUCCESS.eq(success))
        }
        if (!fromSysId.isNullOrEmpty()) {
            conditions.add(TNotifySms.T_NOTIFY_SMS.FROM_SYS_ID.eq(fromSysId))
        }
        return conditions
    }
}
