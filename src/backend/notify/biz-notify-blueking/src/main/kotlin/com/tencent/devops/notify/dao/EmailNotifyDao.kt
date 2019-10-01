package com.tencent.devops.notify.dao

import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.model.notify.tables.TNotifyEmail
import com.tencent.devops.model.notify.tables.records.TNotifyEmailRecord
import com.tencent.devops.common.notify.enums.EnumNotifySource
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.ArrayList

@Repository
class EmailNotifyDao @Autowired constructor(private val dslContext: DSLContext) {
    fun insertOrUpdateEmailNotifyRecord(
        success: Boolean,
        source: EnumNotifySource?,
        id: String?,
        retryCount: Int,
        lastErrorMessage: String?,
        to: String?,
        cc: String?,
        bcc: String?,
        sender: String?,
        title: String?,
        body: String?,
        type: Int?,
        format: Int?,
        priority: Int?,
        contentMd5: String?,
        frequencyLimit: Int?,
        tofSysId: String?,
        fromSysId: String?
    ) {
        val now = LocalDateTime.now()
        dslContext.insertInto(TNotifyEmail.T_NOTIFY_EMAIL,
                TNotifyEmail.T_NOTIFY_EMAIL.ID,
                TNotifyEmail.T_NOTIFY_EMAIL.SOURCE,
                TNotifyEmail.T_NOTIFY_EMAIL.RETRY_COUNT,
                TNotifyEmail.T_NOTIFY_EMAIL.LAST_ERROR,
                TNotifyEmail.T_NOTIFY_EMAIL.TITLE,
                TNotifyEmail.T_NOTIFY_EMAIL.BODY,
                TNotifyEmail.T_NOTIFY_EMAIL.UPDATED_TIME,
                TNotifyEmail.T_NOTIFY_EMAIL.CREATED_TIME,
                TNotifyEmail.T_NOTIFY_EMAIL.SENDER,
                TNotifyEmail.T_NOTIFY_EMAIL.TO,
                TNotifyEmail.T_NOTIFY_EMAIL.CC,
                TNotifyEmail.T_NOTIFY_EMAIL.BCC,
                TNotifyEmail.T_NOTIFY_EMAIL.TYPE,
                TNotifyEmail.T_NOTIFY_EMAIL.FORMAT,
                TNotifyEmail.T_NOTIFY_EMAIL.PRIORITY,
                TNotifyEmail.T_NOTIFY_EMAIL.SUCCESS,
                TNotifyEmail.T_NOTIFY_EMAIL.CONTENT_MD5,
                TNotifyEmail.T_NOTIFY_EMAIL.FREQUENCY_LIMIT,
                TNotifyEmail.T_NOTIFY_EMAIL.TOF_SYS_ID,
                TNotifyEmail.T_NOTIFY_EMAIL.FROM_SYS_ID)
                .values(
                        id,
                        source?.name,
                        retryCount,
                        lastErrorMessage,
                        title,
                        body,
                        now,
                        now,
                        sender,
                        to,
                        cc,
                        bcc,
                        type,
                        format,
                        priority,
                        success,
                        contentMd5,
                        frequencyLimit,
                        tofSysId,
                        fromSysId
                )
                .onDuplicateKeyUpdate()
                .set(TNotifyEmail.T_NOTIFY_EMAIL.SUCCESS, success)
                .set(TNotifyEmail.T_NOTIFY_EMAIL.UPDATED_TIME, now)
                .set(TNotifyEmail.T_NOTIFY_EMAIL.LAST_ERROR, lastErrorMessage)
                .set(TNotifyEmail.T_NOTIFY_EMAIL.RETRY_COUNT, retryCount)
                .set(TNotifyEmail.T_NOTIFY_EMAIL.TOF_SYS_ID, tofSysId)
                .execute()
    }

    fun getTosByContentMd5AndTime(contentMd5: String, timeBeforeSeconds: Long): List<String> {
        return dslContext.selectFrom(TNotifyEmail.T_NOTIFY_EMAIL)
                .where(TNotifyEmail.T_NOTIFY_EMAIL.CONTENT_MD5.eq(contentMd5))
                .and(TNotifyEmail.T_NOTIFY_EMAIL.SUCCESS.eq(true))
                .and(TNotifyEmail.T_NOTIFY_EMAIL.CREATED_TIME.greaterThan(LocalDateTime.now().minusSeconds(timeBeforeSeconds)))
                .fetch(TNotifyEmail.T_NOTIFY_EMAIL.TO)
    }

    fun count(success: Boolean?, fromSysId: String?): Int {
        return dslContext.selectCount()
                .from(TNotifyEmail.T_NOTIFY_EMAIL)
                .where(getListConditions(success, fromSysId))
                .fetchOne()
                .value1()
    }

    fun list(
        page: Int,
        pageSize: Int,
        success: Boolean?,
        fromSysId: String?,
        createdTimeSortOrder: String?
    ): Result<TNotifyEmailRecord> {
        val sqlLimit = PageUtil.convertPageSizeToSQLLimit(page, pageSize)
        return dslContext.selectFrom(TNotifyEmail.T_NOTIFY_EMAIL)
                .where(getListConditions(success, fromSysId))
                .orderBy(if (createdTimeSortOrder != null && createdTimeSortOrder == "descend")
                    TNotifyEmail.T_NOTIFY_EMAIL.CREATED_TIME.desc()
                else
                    TNotifyEmail.T_NOTIFY_EMAIL.CREATED_TIME.asc())
                .limit(sqlLimit.offset, sqlLimit.limit)
                .fetch()
    }

    private fun getListConditions(success: Boolean?, fromSysId: String?): List<Condition> {
        val conditions = ArrayList<Condition>()
        if (success != null) {
            conditions.add(TNotifyEmail.T_NOTIFY_EMAIL.SUCCESS.eq(success))
        }
        if (!fromSysId.isNullOrEmpty()) {
            conditions.add(TNotifyEmail.T_NOTIFY_EMAIL.FROM_SYS_ID.eq(fromSysId))
        }
        return conditions
    }
}