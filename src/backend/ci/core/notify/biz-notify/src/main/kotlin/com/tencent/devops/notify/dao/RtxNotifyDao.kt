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
import com.tencent.devops.model.notify.tables.TNotifyRtx
import com.tencent.devops.model.notify.tables.records.TNotifyRtxRecord
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.ArrayList

/**
 * Powered By Tencent
 */
@Repository
@Suppress("ALL")
class RtxNotifyDao @Autowired constructor(
    private val dslContext: DSLContext
) {
    fun insertOrUpdateRtxNotifyRecord(
        success: Boolean,
        source: EnumNotifySource?,
        batchId: String,
        id: String,
        retryCount: Int,
        lastErrorMessage: String?,
        receivers: String?,
        sender: String?,
        title: String?,
        body: String?,
        priority: Int?,
        contentMd5: String?,
        frequencyLimit: Int?,
        tofSysId: String?,
        fromSysId: String?
    ) {
        val now = LocalDateTime.now()
        dslContext.insertInto(TNotifyRtx.T_NOTIFY_RTX,
            TNotifyRtx.T_NOTIFY_RTX.ID,
            TNotifyRtx.T_NOTIFY_RTX.BATCH_ID,
            TNotifyRtx.T_NOTIFY_RTX.SOURCE,
            TNotifyRtx.T_NOTIFY_RTX.RETRY_COUNT,
            TNotifyRtx.T_NOTIFY_RTX.LAST_ERROR,
            TNotifyRtx.T_NOTIFY_RTX.TITLE,
            TNotifyRtx.T_NOTIFY_RTX.BODY,
            TNotifyRtx.T_NOTIFY_RTX.UPDATED_TIME,
            TNotifyRtx.T_NOTIFY_RTX.CREATED_TIME,
            TNotifyRtx.T_NOTIFY_RTX.SENDER,
            TNotifyRtx.T_NOTIFY_RTX.RECEIVERS,
            TNotifyRtx.T_NOTIFY_RTX.PRIORITY,
            TNotifyRtx.T_NOTIFY_RTX.SUCCESS,
            TNotifyRtx.T_NOTIFY_RTX.CONTENT_MD5,
            TNotifyRtx.T_NOTIFY_RTX.FREQUENCY_LIMIT,
            TNotifyRtx.T_NOTIFY_RTX.TOF_SYS_ID,
            TNotifyRtx.T_NOTIFY_RTX.FROM_SYS_ID
        )
            .values(
                id,
                batchId,
                source?.name,
                retryCount,
                lastErrorMessage,
                title,
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
            .set(TNotifyRtx.T_NOTIFY_RTX.SUCCESS, success)
            .set(TNotifyRtx.T_NOTIFY_RTX.UPDATED_TIME, now)
            .set(TNotifyRtx.T_NOTIFY_RTX.LAST_ERROR, lastErrorMessage)
            .set(TNotifyRtx.T_NOTIFY_RTX.RETRY_COUNT, retryCount)
            .set(TNotifyRtx.T_NOTIFY_RTX.TOF_SYS_ID, tofSysId)
            .execute()
    }

    fun getReceiversByContentMd5AndTime(contentMd5: String, timeBeforeSeconds: Long): List<String> {
        return dslContext.selectFrom(TNotifyRtx.T_NOTIFY_RTX)
            .where(TNotifyRtx.T_NOTIFY_RTX.CONTENT_MD5.eq(contentMd5))
            .and(TNotifyRtx.T_NOTIFY_RTX.SUCCESS.eq(true))
            .and(TNotifyRtx.T_NOTIFY_RTX.CREATED_TIME.greaterThan(LocalDateTime.now().minusSeconds(timeBeforeSeconds)))
            .fetch(TNotifyRtx.T_NOTIFY_RTX.RECEIVERS)
    }

    fun count(success: Boolean?, fromSysId: String?): Int {
        return dslContext.selectCount()
            .from(TNotifyRtx.T_NOTIFY_RTX)
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
    ): Result<TNotifyRtxRecord> {
        val sqlLimit = PageUtil.convertPageSizeToSQLLimit(page, pageSize)
        return dslContext.selectFrom(TNotifyRtx.T_NOTIFY_RTX)
            .where(getListConditions(success, fromSysId))
            .orderBy(if (createdTimeSortOrder != null && createdTimeSortOrder == "descend") {
                TNotifyRtx.T_NOTIFY_RTX.CREATED_TIME.desc()
            } else {
                TNotifyRtx.T_NOTIFY_RTX.CREATED_TIME.asc()
            })
            .limit(sqlLimit.offset, sqlLimit.limit)
            .fetch()
    }

    private fun getListConditions(success: Boolean?, fromSysId: String?): List<Condition> {
        val conditions = ArrayList<Condition>()
        if (success != null) {
            conditions.add(TNotifyRtx.T_NOTIFY_RTX.SUCCESS.eq(success))
        }
        if (!fromSysId.isNullOrEmpty()) {
            conditions.add(TNotifyRtx.T_NOTIFY_RTX.FROM_SYS_ID.eq(fromSysId))
        }
        return conditions
    }
}
