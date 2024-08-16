package com.tencent.devops.experience.pojo.group

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "版本体验-体验组提交")
data class GroupCommit(
    @get:Schema(title = "体验组ID,不传为新建,传值为更新", required = false)
    val groupHashId: String? = null,
    @get:Schema(title = "体验组名称", required = true)
    val name: String,
    @get:Schema(title = "描述", required = true)
    val remark: String,
    @get:Schema(title = "成员列表", required = true)
    val members: List<Member>
) {
    @Schema(title = "版本体验--体验组提交--成员信息")
    data class Member(
        @get:Schema(title = "成员ID")
        val id: String,
        @get:Schema(title = "成员名称")
        val name: String,
        @get:Schema(title = "类别,1--内部人员,2--外部人员,3--内部组织", required = true)
        val type: Int
    )
}
