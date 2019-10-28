package com.tencent.devops.store.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel
data class VisibleAuditInfo(
    @ApiModelProperty("审核记录的ID")
    val id: String,
    @ApiModelProperty("组件名称")
    val storeName: String,
    @ApiModelProperty("机构名称")
    val deptId: Int,
    @ApiModelProperty("机构名称")
    val deptName: String,
    @ApiModelProperty("审核状态")
    val status: Byte,
    @ApiModelProperty("批注")
    val comment: String?,
    @ApiModelProperty("修改人")
    val modifier: String,
    @ApiModelProperty("组件类型")
    val storeType: Byte,
    @ApiModelProperty("修改时间")
    val modifierTime: Long
)