package com.tencent.devops.auth.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "搜索用户组实体")
data class SearchGroupInfo(
    @Schema(title = "分级管理员是否继承查询二级管理员的用户组")
    var inherit: Boolean? = true,
    @Schema(title = "操作id筛选")
    val actionId: String? = null,
    @Schema(title = "资源类型筛选")
    val resourceType: String? = null,
    @Schema(title = "资源实例筛选")
    val iamResourceCode: String? = null,
    @Schema(title = "用户组名称")
    val name: String? = null,
    @Schema(title = "用户组描述")
    val description: String? = null,
    @Schema(title = "用户组id")
    val groupId: Int? = null,
    @Schema(title = "page")
    val page: Int,
    @Schema(title = "pageSize")
    val pageSize: Int
)
