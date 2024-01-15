package com.tencent.devops.experience.pojo.group

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "版本体验-体验组信息-V2")
data class GroupV2(
    @Schema(description = "体验组HashID", required = true)
    val groupHashId: String,
    @Schema(description = "体验组名称", required = true)
    val name: String,
    @Schema(description = "描述")
    val remark: String,
    @Schema(description = "成员列表")
    val members: List<Member>
) {
    @Schema(description = "版本体验-体验组-成员信息")
    data class Member(
        @Schema(description = "ID", required = true)
        val id: String,
        @Schema(description = "名称", required = true)
        val name: String,
        @Schema(description = "类别,1--内部人员,2--外部人员,3--内部组织", required = true)
        val type: Int,
        @Schema(description = "组织架构", required = true)
        val deptFullName: String
    )
}
