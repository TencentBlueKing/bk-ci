package com.tencent.devops.remotedev.service

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.websocket.dispatch.WebSocketDispatcher
import com.tencent.devops.remotedev.dao.WorkspaceNotifyReadStatusDao
import com.tencent.devops.remotedev.pojo.UserNotifyInfo
import com.tencent.devops.remotedev.pojo.WebSocketActionType
import com.tencent.devops.remotedev.websocket.page.WorkspacePageBuild
import com.tencent.devops.remotedev.websocket.push.WorkspaceWebsocketPush
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class NotificationCenterService @Autowired constructor(
    private val webSocketDispatcher: WebSocketDispatcher,
    private val redisOperation: RedisOperation,
    private val workspaceNotifyReadStatusDao: WorkspaceNotifyReadStatusDao,
    private val dslContext: DSLContext
) {
    data class NotificationQueryCondition(
        val userId: String,
        val page: Int,
        val pageSize: Int,
        val category: String? = null,
        val isRead: Boolean? = null
    )

    /**
     * 未读数量消息
     */
    data class UnreadCountMessage(
        val actionType: WebSocketActionType,
        val userId: String,
        val unreadCount: Int,
        val timestamp: Long = System.currentTimeMillis()
    )

    fun createNotification(userId: String, notifyId: Long): Boolean {
        workspaceNotifyReadStatusDao.createReadStatus(dslContext, userId, notifyId)
        // invalidate cache
        redisOperation.delete(buildUnreadCacheKey(userId))
        // compute and push
        pushUnreadCountUpdate(userId)
        return true
    }

    fun getUserNotifications(condition: NotificationQueryCondition): Page<UserNotifyInfo> {
        val list = workspaceNotifyReadStatusDao.getUserNotifyListWithReadStatus(
            dslContext = dslContext,
            userId = condition.userId,
            page = condition.page,
            pageSize = condition.pageSize,
            category = condition.category
        )
        return Page(
            count = list.size.toLong(),
            page = condition.page,
            pageSize = condition.pageSize,
            records = list
        )
    }

    fun markAsRead(userId: String, notifyIds: List<Long>): Boolean {
        val updated = workspaceNotifyReadStatusDao.markAsRead(dslContext, userId, notifyIds)
        if (updated > 0) {
            redisOperation.delete(buildUnreadCacheKey(userId))
            pushUnreadCountUpdate(userId)
        }
        return updated > 0
    }

    fun clearAllNotifications(userId: String, category: String?): Boolean {
        val cleared = workspaceNotifyReadStatusDao.clearAllUnread(dslContext, userId, category)
        if (cleared > 0) {
            redisOperation.delete(buildUnreadCacheKey(userId))
            pushUnreadCountUpdate(userId)
        }
        return cleared > 0
    }

    fun getUnreadCount(userId: String): Int {
        val key = buildUnreadCacheKey(userId)
        val cached = redisOperation.get(key)
        if (!cached.isNullOrBlank()) {
            return cached.toIntOrNull() ?: 0
        }
        val count = workspaceNotifyReadStatusDao.getUnreadCount(dslContext, userId)
        redisOperation.set(key, count.toString(), 600L) // simple TTL 60s per plan guidance
        return count
    }

    fun pushUnreadCountUpdate(userId: String) {
        val cnt = getUnreadCount(userId)
        pushUnreadCountUpdate(userId, cnt)
    }

    /**
     * 推送未读数量更新到客户端
     */
    fun pushUnreadCountUpdate(userId: String, unreadCount: Int) {
        val message = UnreadCountMessage(
            actionType = WebSocketActionType.WORKSPACE_NOTIFY_UNREAD_COUNT,
            userId = userId,
            unreadCount = unreadCount,
            timestamp = System.currentTimeMillis()
        )
        val push = WorkspaceWebsocketPush(
            type = WebSocketActionType.WORKSPACE_NOTIFY_UNREAD_COUNT,
            status = true,
            anyMessage = message,
            projectId = "",
            userId = userId,
            redisOperation = redisOperation,
            page = WorkspacePageBuild.buildPage(userId)
        )
        webSocketDispatcher.dispatch(push)
    }

    private fun buildUnreadCacheKey(userId: String) = "remotedev:notify:unread:$userId"
}
