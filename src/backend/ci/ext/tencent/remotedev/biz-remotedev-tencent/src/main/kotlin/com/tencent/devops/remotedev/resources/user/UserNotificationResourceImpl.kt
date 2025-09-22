package com.tencent.devops.remotedev.resources.user

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.remotedev.api.user.UserNotificationResource
import com.tencent.devops.remotedev.pojo.UserNotifyInfo
import com.tencent.devops.remotedev.pojo.notification.ClearNotifyRequest
import com.tencent.devops.remotedev.pojo.notification.MarkReadRequest
import com.tencent.devops.remotedev.service.NotificationCenterService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserNotificationResourceImpl @Autowired constructor(
    private val notificationCenterService: NotificationCenterService
) : UserNotificationResource {

    override fun getUserNotifyList(
        userId: String,
        page: Int,
        pageSize: Int,
        category: String?,
        read: Boolean?
    ): Result<Page<UserNotifyInfo>> {
        require(page > 0 && pageSize > 0) { "page and pageSize must be positive" }
        return Result(
            notificationCenterService.getUserNotifications(
                NotificationCenterService.NotificationQueryCondition(
                    userId = userId,
                    page = page,
                    pageSize = pageSize,
                    category = category,
                    isRead = read
                )
            )
        )
    }

    override fun markNotifyAsRead(userId: String, request: MarkReadRequest): Result<Boolean> {
        if (request.notifyIds.isEmpty()) {
            throw ErrorCodeException(errorCode = "INVALID_PARAM", defaultMessage = "notifyIds cannot be empty")
        }
        val updated = notificationCenterService.markAsRead(userId, request.notifyIds)
        return Result(updated)
    }

    override fun clearAllNotify(userId: String, request: ClearNotifyRequest): Result<Boolean> {
        val cleared = notificationCenterService.clearAllNotifications(userId, request.category)
        return Result(cleared)
    }

    override fun getUnreadCount(userId: String): Result<Int> {
        return Result(notificationCenterService.getUnreadCount(userId))
    }
}
