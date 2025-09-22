package com.tencent.devops.remotedev.dao

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.model.remotedev.tables.TWorkspaceNotifyHistory
import com.tencent.devops.model.remotedev.tables.TWorkspaceNotifyReadStatus
import com.tencent.devops.remotedev.pojo.NotifyCategory
import com.tencent.devops.remotedev.pojo.UserNotifyInfo
import com.tencent.devops.remotedev.pojo.common.RemoteDevNotifyType
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

@Repository
class WorkspaceNotifyReadStatusDao {

    fun createReadStatus(
        dslContext: DSLContext,
        userId: String,
        notifyId: Long
    ): Boolean {
        with(TWorkspaceNotifyReadStatus.T_WORKSPACE_NOTIFY_READ_STATUS) {
            val affected = dslContext.insertInto(this)
                .columns(USER_ID, NOTIFY_ID, IS_READ)
                .values(userId, notifyId, 0.toByte())
                .onDuplicateKeyIgnore()
                .execute()
            return affected > 0
        }
    }

    fun markAsRead(
        dslContext: DSLContext,
        userId: String,
        notifyIds: List<Long>
    ): Int {
        if (notifyIds.isEmpty()) return 0
        with(TWorkspaceNotifyReadStatus.T_WORKSPACE_NOTIFY_READ_STATUS) {
            return dslContext.update(this)
                .set(IS_READ, 1.toByte())
                .set(READ_TIME, DSL.currentLocalDateTime())
                .where(USER_ID.eq(userId))
                .and(NOTIFY_ID.`in`(notifyIds))
                .and(IS_READ.eq(0.toByte()))
                .execute()
        }
    }

    fun getUnreadCount(
        dslContext: DSLContext,
        userId: String
    ): Int {
        with(TWorkspaceNotifyReadStatus.T_WORKSPACE_NOTIFY_READ_STATUS) {
            return dslContext.selectCount()
                .from(this)
                .where(USER_ID.eq(userId))
                .and(IS_READ.eq(0.toByte()))
                .fetchOne(0, Int::class.java) ?: 0
        }
    }

    fun clearAllUnread(
        dslContext: DSLContext,
        userId: String,
        category: String?
    ): Int {
        with(TWorkspaceNotifyReadStatus.T_WORKSPACE_NOTIFY_READ_STATUS) {
            val baseCondition = USER_ID.eq(userId).and(IS_READ.eq(0.toByte()))
            val condition = if (category.isNullOrBlank()) {
                baseCondition
            } else {
                with(TWorkspaceNotifyHistory.T_WORKSPACE_NOTIFY_HISTORY) {
                    baseCondition.and(
                        NOTIFY_ID.`in`(
                            dslContext.select(ID).from(this).where(TYPE.eq(category))
                        )
                    )
                }
            }
            return dslContext.update(this)
                .set(IS_READ, 1.toByte())
                .set(READ_TIME, DSL.currentLocalDateTime())
                .where(condition)
                .execute()
        }
    }

    fun getUserNotifyListWithReadStatus(
        dslContext: DSLContext,
        userId: String,
        page: Int,
        pageSize: Int,
        category: String?
    ): List<UserNotifyInfo> {
        val readStatusTable = TWorkspaceNotifyReadStatus.T_WORKSPACE_NOTIFY_READ_STATUS
        val historyTable = TWorkspaceNotifyHistory.T_WORKSPACE_NOTIFY_HISTORY

        val baseCondition = readStatusTable.USER_ID.eq(userId)
        val condition = if (category.isNullOrBlank()) {
            baseCondition
        } else {
            baseCondition.and(historyTable.TYPE.eq(category))
        }

        val records = dslContext.select(
            historyTable.ID,
            historyTable.BODY_PARAMS,
            historyTable.TYPE,
            historyTable.OPERATOR,
            historyTable.CREATED_TIME,
            readStatusTable.IS_READ,
            readStatusTable.READ_TIME
        ).from(historyTable)
            .leftJoin(readStatusTable).on(
                readStatusTable.NOTIFY_ID.eq(historyTable.ID)
                    .and(readStatusTable.USER_ID.eq(userId))
            )
            .where(condition)
            .orderBy(historyTable.ID.desc())
            .limit(pageSize)
            .offset((page - 1) * pageSize)
            .fetch()

        return records.map {
            val body = JsonUtil.to<Map<String, String>>(it.get(historyTable.BODY_PARAMS))
            UserNotifyInfo(
                id = it.get(historyTable.ID)!!,
                title = body[UserNotifyInfo::title.name] ?: "",
                content = body[UserNotifyInfo::content.name],
                notifyType = RemoteDevNotifyType.valueOf(it.get(historyTable.TYPE)!!),
                operatorName = it.get(historyTable.OPERATOR) ?: "",
                createTime = it.get(historyTable.CREATED_TIME)!!,
                isRead = ((it.get(readStatusTable.IS_READ) ?: 0.toByte()) == 1.toByte()),
                readTime = it.get(readStatusTable.READ_TIME),
                category = NotifyCategory.fromValue(body[UserNotifyInfo::category.name])
            )
        }
    }
}
