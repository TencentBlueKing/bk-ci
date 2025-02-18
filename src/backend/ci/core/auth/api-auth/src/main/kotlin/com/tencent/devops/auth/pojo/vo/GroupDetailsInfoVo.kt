package com.tencent.devops.auth.pojo.vo

import com.tencent.devops.auth.pojo.enum.JoinedType
import com.tencent.devops.auth.pojo.enum.MemberType
import com.tencent.devops.auth.pojo.enum.RemoveMemberButtonControl
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "用户组详细信息")
data class GroupDetailsInfoVo(
    @get:Schema(title = "资源实例code")
    val resourceCode: String,
    @get:Schema(title = "资源实例名称")
    val resourceName: String,
    @get:Schema(title = "资源类型")
    val resourceType: String,
    @get:Schema(title = "用户组ID")
    val groupId: Int,
    @get:Schema(title = "用户组名称")
    val groupName: String,
    @get:Schema(title = "用户组描述")
    val groupDesc: String? = null,
    @get:Schema(title = "有效期，天")
    val expiredAtDisplay: String,
    @get:Schema(title = "过期时间戳，毫秒")
    val expiredAt: Long,
    @get:Schema(title = "加入时间")
    val joinedTime: Long,
    @get:Schema(title = "移除成员按钮控制")
    val removeMemberButtonControl: RemoveMemberButtonControl,
    @get:Schema(title = "加入方式")
    val joinedType: JoinedType,
    @get:Schema(title = "操作人")
    val operator: String,
    @get:Schema(title = "是否正在交接")
    val beingHandedOver: Boolean? = null,
    @get:Schema(title = "交接单号")
    val flowNo: String? = null,
    @get:Schema(title = "组成员类型")
    val memberType: MemberType? = null
)
