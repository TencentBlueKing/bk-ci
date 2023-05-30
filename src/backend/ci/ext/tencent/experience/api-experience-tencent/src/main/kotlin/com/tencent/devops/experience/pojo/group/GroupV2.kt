package com.tencent.devops.experience.pojo.group

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("版本体验-体验组信息-V2")
data class GroupV2(
    @ApiModelProperty("体验组HashID", required = true)
    val groupHashId: String,
    @ApiModelProperty("体验组名称", required = true)
    val name: String,
    @ApiModelProperty("描述")
    val remark: String,
    @ApiModelProperty("成员列表")
    val members: List<Member>,
) {
    @ApiModel("版本体验-体验组-成员信息")
    data class Member(
        @ApiModelProperty("名称", required = true)
        val name: String,
        @ApiModelProperty("类别,1--内部人员,2--外部人员,3--内部组织", required = true)
        val type: Int,
        @ApiModelProperty("组织架构", required = true)
        val deptFullName: String
    )
}
