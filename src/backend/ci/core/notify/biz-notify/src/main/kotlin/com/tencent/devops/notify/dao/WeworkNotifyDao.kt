package com.tencent.devops.notify.dao

import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.model.notify.tables.TNotifyWework
import com.tencent.devops.model.notify.tables.records.TNotifyWeworkRecord
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class WeworkNotifyDao(
    private val dslContext: DSLContext
) {

    fun insertOrUpdateWeworkNotifyRecord(
        success: Boolean,
        lastErrorMessage: String?,
        receivers: String?,
        body: String?
    ) {
        val now = LocalDateTime.now()
        dslContext.insertInto(
            TNotifyWework.T_NOTIFY_WEWORK,
            TNotifyWework.T_NOTIFY_WEWORK.LAST_ERROR,
            TNotifyWework.T_NOTIFY_WEWORK.BODY,
            TNotifyWework.T_NOTIFY_WEWORK.UPDATED_TIME,
            TNotifyWework.T_NOTIFY_WEWORK.CREATED_TIME,
            TNotifyWework.T_NOTIFY_WEWORK.RECEIVERS,
            TNotifyWework.T_NOTIFY_WEWORK.SUCCESS
        )
            .values(
                lastErrorMessage,
                body,
                now,
                now,
                receivers,
                success
            )
            .execute()
    }

    fun count(success: Boolean?, fromSysId: String?): Int {
        return dslContext.selectCount()
            .from(TNotifyWework.T_NOTIFY_WEWORK)
            .where(getListConditions(success))
            .fetchOne()!!
            .value1()
    }

    fun list(
        page: Int,
        pageSize: Int,
        success: Boolean?,
        fromSysId: String?,
        createdTimeSortOrder: String?
    ): Result<TNotifyWeworkRecord> {
        val sqlLimit = PageUtil.convertPageSizeToSQLLimit(page, pageSize)
        return dslContext.selectFrom(TNotifyWework.T_NOTIFY_WEWORK)
            .where(getListConditions(success))
            .orderBy(
                if (createdTimeSortOrder != null && createdTimeSortOrder == "descend") {
                    TNotifyWework.T_NOTIFY_WEWORK.CREATED_TIME.desc()
                } else {
                    TNotifyWework.T_NOTIFY_WEWORK.CREATED_TIME.asc()
                }
            )
            .limit(sqlLimit.offset, sqlLimit.limit)
            .fetch()
    }

    private fun getListConditions(success: Boolean?): List<Condition> {
        val conditions = ArrayList<Condition>()
        if (success != null) {
            conditions.add(TNotifyWework.T_NOTIFY_WEWORK.SUCCESS.eq(success))
        }
        return conditions
    }
}
