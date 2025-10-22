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
import com.tencent.devops.model.notify.tables.TNotifyWechat
import com.tencent.devops.model.notify.tables.records.TNotifyWechatRecord
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.ArrayList

@Repository
@Suppress("ALL")
class WechatNotifyDao @Autowired constructor(private val dslContext: DSLContext) {
    fun insertOrUpdateWechatNotifyRecord(
        success: Boolean?,
        source: EnumNotifySource?,
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
        dslContext.insertInto(TNotifyWechat.T_NOTIFY_WECHAT,
            TNotifyWechat.T_NOTIFY_WECHAT.ID,
            TNotifyWechat.T_NOTIFY_WECHAT.SOURCE,
            TNotifyWechat.T_NOTIFY_WECHAT.RETRY_COUNT,
            TNotifyWechat.T_NOTIFY_WECHAT.LAST_ERROR,
            TNotifyWechat.T_NOTIFY_WECHAT.BODY,
            TNotifyWechat.T_NOTIFY_WECHAT.UPDATED_TIME,
            TNotifyWechat.T_NOTIFY_WECHAT.CREATED_TIME,
            TNotifyWechat.T_NOTIFY_WECHAT.SENDER,
            TNotifyWechat.T_NOTIFY_WECHAT.RECEIVERS,
            TNotifyWechat.T_NOTIFY_WECHAT.PRIORITY,
            TNotifyWechat.T_NOTIFY_WECHAT.SUCCESS,
            TNotifyWechat.T_NOTIFY_WECHAT.CONTENT_MD5,
            TNotifyWechat.T_NOTIFY_WECHAT.FREQUENCY_LIMIT,
            TNotifyWechat.T_NOTIFY_WECHAT.TOF_SYS_ID,
            TNotifyWechat.T_NOTIFY_WECHAT.FROM_SYS_ID)
            .values(
                id,
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
            .set(TNotifyWechat.T_NOTIFY_WECHAT.SUCCESS, success)
            .set(TNotifyWechat.T_NOTIFY_WECHAT.UPDATED_TIME, now)
            .set(TNotifyWechat.T_NOTIFY_WECHAT.LAST_ERROR, lastErrorMessage)
            .set(TNotifyWechat.T_NOTIFY_WECHAT.RETRY_COUNT, retryCount)
            .set(TNotifyWechat.T_NOTIFY_WECHAT.TOF_SYS_ID, tofSysId)
            .execute()
    }

    fun getReceiversByContentMd5AndTime(contentMd5: String, timeBeforeSeconds: Long): List<String> {
        with(TNotifyWechat.T_NOTIFY_WECHAT) {
            return dslContext.selectFrom(this)
                .where(CONTENT_MD5.eq(contentMd5))
                .and(SUCCESS.eq(true))
                .and(CREATED_TIME.greaterThan(LocalDateTime.now().minusSeconds(timeBeforeSeconds)))
                .fetch(RECEIVERS)
        }
    }

    fun count(success: Boolean?, fromSysId: String?): Int {
        return dslContext.selectCount()
            .from(TNotifyWechat.T_NOTIFY_WECHAT)
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
    ): Result<TNotifyWechatRecord> {
        val sqlLimit = PageUtil.convertPageSizeToSQLLimit(page, pageSize)
        return dslContext.selectFrom(TNotifyWechat.T_NOTIFY_WECHAT)
            .where(getListConditions(success, fromSysId))
            .orderBy(if (createdTimeSortOrder != null && createdTimeSortOrder == "descend") {
                TNotifyWechat.T_NOTIFY_WECHAT.CREATED_TIME.desc()
            } else {
                TNotifyWechat.T_NOTIFY_WECHAT.CREATED_TIME.asc()
            })
            .limit(sqlLimit.offset, sqlLimit.limit)
            .fetch()
    }

    private fun getListConditions(success: Boolean?, fromSysId: String?): List<Condition> {
        val conditions = ArrayList<Condition>()
        if (success != null) {
            conditions.add(TNotifyWechat.T_NOTIFY_WECHAT.SUCCESS.eq(success))
        }
        if (!fromSysId.isNullOrEmpty()) {
            conditions.add(TNotifyWechat.T_NOTIFY_WECHAT.FROM_SYS_ID.eq(fromSysId))
        }
        return conditions
    }
}
