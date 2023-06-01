package com.tencent.devops.experience.pojo.group

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("版本体验-体验组提交")
data class GroupCommit(
        @ApiModelProperty("体验组ID,不传为新建,传值为更新", required = false)
        val groupHashId: String? = null,
        @ApiModelProperty("体验组名称", required = true)
        val name: String,
        @ApiModelProperty("描述", required = true)
        val remark: String,
        @ApiModelProperty("成员列表", required = true)
        val members: List<Member>
) {
    @ApiModel("版本体验--体验组提交--成员信息")
    data class Member(
            @ApiModelProperty("成员ID")
            val name: String,
            @ApiModelProperty("类别,1--内部人员,2--外部人员,3--内部组织", required = true)
            val type: Int
    )
}
