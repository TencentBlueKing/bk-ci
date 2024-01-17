package com.tencent.devops.experience.pojo.group

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "版本体验-体验组信息-V2")
data class GroupV2(
    @Schema(title = "体验组HashID", required = true)
    val groupHashId: String,
    @Schema(title = "体验组名称", required = true)
    val name: String,
    @Schema(title = "描述")
    val remark: String,
    @Schema(title = "成员列表")
    val members: List<Member>
) {
    @Schema(title = "版本体验-体验组-成员信息")
    data class Member(
        @Schema(title = "ID", required = true)
        val id: String,
        @Schema(title = "名称", required = true)
        val name: String,
        @Schema(title = "类别,1--内部人员,2--外部人员,3--内部组织", required = true)
        val type: Int,
        @Schema(title = "组织架构", required = true)
        val deptFullName: String
    )
}
