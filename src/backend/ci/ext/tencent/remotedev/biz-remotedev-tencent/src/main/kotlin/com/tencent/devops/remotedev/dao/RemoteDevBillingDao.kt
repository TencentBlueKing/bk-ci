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

package com.tencent.devops.remotedev.dao

import com.tencent.devops.model.remotedev.tables.TRemoteDevBilling
import com.tencent.devops.model.remotedev.tables.TRemoteDevSettings
import com.tencent.devops.model.remotedev.tables.records.TRemoteDevBillingRecord
import org.jooq.DSLContext
import org.jooq.DatePart
import org.jooq.Field
import org.jooq.Record2
import org.jooq.Result
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.Duration
import java.time.LocalDateTime

@Repository
class RemoteDevBillingDao {

    companion object {
        private val logger = LoggerFactory.getLogger(RemoteDevBillingDao::class.java)
    }

    /**
     * 工作空间启动时创建billing
     */
    fun newBilling(
        dslContext: DSLContext,
        workspaceName: String,
        userId: String
    ) {
        val res = with(TRemoteDevBilling.T_REMOTE_DEV_BILLING) {
            dslContext.selectCount().from(this).where(
                WORKSPACE_NAME.eq(workspaceName)
            ).and(USER.eq(userId)).and(END_TIME.isNull).fetchOne(DSL.count())!! > 0
        }
        if (res) {
            logger.info("newBilling fail, task exist|$workspaceName|$userId")
            return
        }
        return with(TRemoteDevBilling.T_REMOTE_DEV_BILLING) {
            dslContext.insertInto(
                this,
                WORKSPACE_NAME,
                USER,
                START_TIME
            ).values(
                workspaceName,
                userId,
                LocalDateTime.now()
            ).execute()
        }
    }

    /**
     * 工作空间结束时调用
     */
    fun endBilling(
        dslContext: DSLContext,
        workspaceName: String
    ) {
        val now = LocalDateTime.now()
        val res = with(TRemoteDevBilling.T_REMOTE_DEV_BILLING) {
            dslContext.selectFrom(this)
                .where(WORKSPACE_NAME.eq(workspaceName)).and(END_TIME.isNull)
                .fetch()
        }
        res.forEach { record ->
            val add = Duration.between(record.startTime, now).seconds.toInt()
            with(TRemoteDevBilling.T_REMOTE_DEV_BILLING) {
                dslContext.update(this)
                    .set(END_TIME, now)
                    .set(USAGE_TIME, add)
                    .set(UPDATE_TIME, now)
                    .where(ID.eq(record.id))
                    .execute()
            }
            with(TRemoteDevSettings.T_REMOTE_DEV_SETTINGS) {
                dslContext.update(this)
                    .set(CUMULATIVE_USAGE_TIME, CUMULATIVE_USAGE_TIME + add)
                    .where(USER_ID.eq(record.user))
                    .execute()
            }
        }
    }

    fun fetchNotEndBilling(dslContext: DSLContext, userId: String): Result<TRemoteDevBillingRecord> {
        with(TRemoteDevBilling.T_REMOTE_DEV_BILLING) {
            return dslContext.selectFrom(this).where(USER.eq(userId)).and(END_TIME.isNull).fetch()
        }
    }

    fun monthlyInit(dslContext: DSLContext, freeTime: Int) {
        val res = with(TRemoteDevBilling.T_REMOTE_DEV_BILLING) {
            dslContext.select(WORKSPACE_NAME, USER).from(this)
                .where(END_TIME.isNull)
                .fetch()
        }
        /**
         * 每月结束还没有结束的task，并创建一个新的task。这样做便于按月区分粒度
         */
        res.forEach { record2: Record2<String/*WORKSPACE_NAME*/, String/*USER*/> ->
            endBilling(dslContext, record2.value1())
            newBilling(dslContext, record2.value1(), record2.value2())
        }
        /**
         * 初始化每个人的计费数据
         */
        with(TRemoteDevSettings.T_REMOTE_DEV_SETTINGS) {
            dslContext.update(this)
                .set(CUMULATIVE_USAGE_TIME, 0)
                .set(
                    CUMULATIVE_BILLING_TIME,
                    CUMULATIVE_BILLING_TIME + DSL.iif(
                        CUMULATIVE_BILLING_TIME.gt(freeTime),
                        CUMULATIVE_BILLING_TIME - freeTime,
                        0
                    )
                )
                .where(CUMULATIVE_USAGE_TIME.gt(0))
                .execute()
        }
    }
}
