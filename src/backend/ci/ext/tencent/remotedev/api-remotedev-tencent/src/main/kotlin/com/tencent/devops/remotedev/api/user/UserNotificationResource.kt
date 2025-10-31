package com.tencent.devops.remotedev.api.user

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.remotedev.pojo.UserNotifyInfo
import com.tencent.devops.remotedev.pojo.notification.ClearNotifyRequest
import com.tencent.devops.remotedev.pojo.notification.MarkReadRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "USER_NOTIFICATION", description = "用户-通知中心相关")
@Path("/{apiType:user|desktop}/notification")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserNotificationResource {

    @Operation(summary = "查询用户通知列表")
    @GET
    @Path("/list")
    fun getUserNotifyList(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "页码", required = false)
        @QueryParam("page")
        page: Int = 1,
        @Parameter(description = "每页数量", required = false)
        @QueryParam("pageSize")
        pageSize: Int = 20,
        @Parameter(description = "分类筛选", required = false)
        @QueryParam("category")
        category: String?,
        @Parameter(description = "分类筛选", required = false)
        @QueryParam("read")
        read: Boolean?
    ): Result<Page<UserNotifyInfo>>

    @Operation(summary = "标记指定通知为已读")
    @POST
    @Path("/markRead")
    fun markNotifyAsRead(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        request: MarkReadRequest
    ): Result<Boolean>

    @Operation(summary = "清除用户所有未读通知")
    @POST
    @Path("/clearAll")
    fun clearAllNotify(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        request: ClearNotifyRequest
    ): Result<Boolean>

    @Operation(summary = "获取未读数量")
    @GET
    @Path("/unreadCount")
    fun getUnreadCount(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String
    ): Result<Int>
}
