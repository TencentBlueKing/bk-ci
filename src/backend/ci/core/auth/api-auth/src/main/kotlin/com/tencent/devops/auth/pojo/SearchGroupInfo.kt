package com.tencent.devops.auth.pojo

import com.tencent.devops.auth.pojo.enum.GroupLevel
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "搜索用户组实体")
data class SearchGroupInfo(
    @get:Schema(title = "用户组级别")
    var groupLevel: GroupLevel? = GroupLevel.PROJECT,
    @get:Schema(title = "操作id筛选")
    val actionId: String? = null,
    @get:Schema(title = "资源类型筛选")
    val resourceType: String? = null,
    @get:Schema(title = "资源实例筛选")
    val iamResourceCode: String? = null,
    @get:Schema(title = "用户组名称")
    val name: String? = null,
    @get:Schema(title = "用户组描述")
    val description: String? = null,
    @get:Schema(title = "用户组id")
    val groupId: Int? = null,
    @get:Schema(title = "page")
    val page: Int,
    @get:Schema(title = "pageSize")
    val pageSize: Int
)
