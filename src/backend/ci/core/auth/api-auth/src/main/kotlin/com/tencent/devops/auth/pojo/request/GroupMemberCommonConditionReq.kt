package com.tencent.devops.auth.pojo.request

import com.tencent.devops.auth.pojo.ResourceMemberInfo
import com.tencent.devops.auth.pojo.dto.MemberGroupJoinedDTO
import com.tencent.devops.auth.pojo.enum.OperateChannel
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "用户组成员处理公共请求体")
open class GroupMemberCommonConditionReq(
    @get:Schema(title = "组IDs")
    open val groupIds: List<MemberGroupJoinedDTO> = emptyList(),
    @get:Schema(title = "全选的资源类型")
    open val resourceTypes: List<String> = emptyList(),
    @get:Schema(title = "全量选择")
    open val allSelection: Boolean = false,
    @get:Schema(title = "目标对象")
    open val targetMember: ResourceMemberInfo,
    @get:Schema(title = "操作渠道")
    open val operateChannel: OperateChannel = OperateChannel.MANAGER
) {
    override fun toString(): String {
        return "GroupMemberCommonConditionReq(groupIds=$groupIds,resourceTypes=$resourceTypes," +
            "allSelection=$allSelection,targetMember=$targetMember,operateChannel=$operateChannel)"
    }
}
