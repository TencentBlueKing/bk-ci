package com.tencent.devops.auth.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("搜索用户组实体")
data class SearchGroupInfo(
    @ApiModelProperty("分级管理员是否继承查询二级管理员的用户组")
    var inherit: Boolean? = true,
    @ApiModelProperty("操作id筛选")
    val actionId: String?,
    @ApiModelProperty("资源类型筛选")
    val resourceType: String?,
    @ApiModelProperty("资源实例筛选")
    val iamResourceCode: String?,
    @ApiModelProperty("用户组名称")
    val name: String?,
    @ApiModelProperty("用户组描述")
    val description: String?,
    @ApiModelProperty("用户组id")
    val groupId: Int?,
    @ApiModelProperty("page")
    val page: Int,
    @ApiModelProperty("pageSize")
    val pageSize: Int
)
