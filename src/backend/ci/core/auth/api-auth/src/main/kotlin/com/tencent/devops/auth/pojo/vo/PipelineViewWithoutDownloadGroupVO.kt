package com.tencent.devops.auth.pojo.vo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "仅有流水线查看权限且没有下载权限的项目级用户组")
data class PipelineViewWithoutDownloadGroupVO(
    @get:Schema(title = "项目ID")
    val projectCode: String,
    @get:Schema(title = "用户组ID")
    val iamGroupId: Int,
    @get:Schema(title = "用户组名称")
    val groupName: String,
    @get:Schema(title = "有效成员数")
    val memberCount: Int
)
