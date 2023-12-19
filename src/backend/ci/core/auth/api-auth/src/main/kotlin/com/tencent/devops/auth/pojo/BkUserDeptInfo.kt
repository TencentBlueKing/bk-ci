package com.tencent.devops.auth.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("用户部门详细信息")
data class BkUserDeptInfo(
    @ApiModelProperty("id")
    val id: String?,
    @ApiModelProperty("部门名称")
    val name: String?,
    @ApiModelProperty("部门详细名称")
    @JsonProperty("full_name")
    val fullName: String?
)
