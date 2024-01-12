package com.tencent.devops.auth.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "搜索用户组实体")
data class SearchGroupInfo(
    @Schema(description = "分级管理员是否继承查询二级管理员的用户组")
    var inherit: Boolean? = true,
    @Schema(description = "操作id筛选")
    val actionId: String? = null,
    @Schema(description = "资源类型筛选")
    val resourceType: String? = null,
    @Schema(description = "资源实例筛选")
    val iamResourceCode: String? = null,
    @Schema(description = "用户组名称")
    val name: String? = null,
    @Schema(description = "用户组描述")
    val description: String? = null,
    @Schema(description = "用户组id")
    val groupId: Int? = null,
    @Schema(description = "page")
    val page: Int,
    @Schema(description = "pageSize")
    val pageSize: Int
)
