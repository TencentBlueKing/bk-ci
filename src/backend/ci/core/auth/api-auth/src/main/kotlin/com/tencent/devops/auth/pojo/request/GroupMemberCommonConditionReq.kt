package com.tencent.devops.auth.pojo.request

import com.tencent.devops.auth.pojo.ResourceMemberInfo
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "用户组成员处理公共请求体")
open class GroupMemberCommonConditionReq(
    @get:Schema(title = "组IDs")
    open val groupIds: List<Int> = emptyList(),
    @get:Schema(title = "全选的资源类型")
    open val resourceTypes: List<String> = emptyList(),
    @get:Schema(title = "全量选择")
    open val allSelection: Boolean = false,
    @get:Schema(title = "是否排除唯一管理员组")
    open var excludedUniqueManagerGroup: Boolean = false,
    @get:Schema(title = "目标对象")
    open val targetMember: ResourceMemberInfo
) {
    override fun toString(): String {
        return "GroupMemberCommonConditionReq(groupIds=$groupIds,resourceTypes=$resourceTypes," +
            "allSelection=$allSelection,excludedUniqueManagerGroup=$excludedUniqueManagerGroup," +
            "targetMember=$targetMember)"
    }
}
