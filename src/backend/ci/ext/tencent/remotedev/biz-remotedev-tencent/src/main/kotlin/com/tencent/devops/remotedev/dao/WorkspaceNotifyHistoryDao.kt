package com.tencent.devops.remotedev.dao

import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.model.remotedev.tables.TWorkspaceNotifyHistory
import com.tencent.devops.model.remotedev.tables.records.TWorkspaceNotifyHistoryRecord
import com.tencent.devops.remotedev.pojo.common.RemoteDevNotifyType
import org.jooq.DSLContext
import org.slf4j.MDC
import org.springframework.stereotype.Repository

@Repository
class WorkspaceNotifyHistoryDao {

    fun add(
        dslContext: DSLContext,
        operator: String,
        userIds: String,
        type: RemoteDevNotifyType,
        status: RemoteDevNotifyType.Status,
        bodyParams: String
    ): Long {
        val bizId = MDC.get(TraceTag.BIZID) ?: TraceTag.buildBiz().also {
            MDC.put(TraceTag.BIZID, it)
        }
        with(TWorkspaceNotifyHistory.T_WORKSPACE_NOTIFY_HISTORY) {
            return dslContext.insertInto(
                this,
                BIZ_ID,
                OPERATOR,
                USER_IDS,
                TYPE,
                STATUS,
                BODY_PARAMS
            ).values(
                bizId,
                operator,
                userIds,
                type.name,
                status.name,
                bodyParams
            ).returning(ID).fetchOne()!!.id
        }
    }

    fun updateStatus(
        dslContext: DSLContext,
        id: Long,
        status: RemoteDevNotifyType.Status
    ): Int {
        with(TWorkspaceNotifyHistory.T_WORKSPACE_NOTIFY_HISTORY) {
            return dslContext.update(this)
                .set(STATUS, status.name)
                .where(ID.eq(id))
                .execute()
        }
    }

    fun fetchFailMessage(
        dslContext: DSLContext,
        userId: String,
        type: RemoteDevNotifyType
    ): List<TWorkspaceNotifyHistoryRecord> {
        with(TWorkspaceNotifyHistory.T_WORKSPACE_NOTIFY_HISTORY) {
            return dslContext.selectFrom(this)
                .where(USER_IDS.eq(userId))
                .and(TYPE.eq(type.name))
                .orderBy(CREATED_TIME.asc())
                .fetch()
        }
    }
}
