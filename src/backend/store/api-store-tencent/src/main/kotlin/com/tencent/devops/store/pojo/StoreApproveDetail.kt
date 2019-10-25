package com.tencent.devops.store.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("审核信息详情")
data class StoreApproveDetail(
    @ApiModelProperty("ID", required = true)
    val approveId: String,
    @ApiModelProperty("审批内容", required = true)
    val content: String,
    @ApiModelProperty("申请人", required = true)
    val applicant: String,
    @ApiModelProperty("审批类型 ATOM_COLLABORATOR_APPLY:申请成为插件协作者", required = true)
    val approveType: String,
    @ApiModelProperty("审批状态 WAIT:待审批，PASS:通过，REFUSE:拒绝", required = true)
    val approveStatus: String,
    @ApiModelProperty("审批信息", required = false)
    val approveMsg: String?,
    @ApiModelProperty("store组件代码", required = true)
    val storeCode: String,
    @ApiModelProperty("store组件类别 ATOM:插件 TEMPLATE:模板", required = true)
    val storeType: String,
    @ApiModelProperty("审批业务的额外参数", required = false)
    var additionalParams: Map<String, String>? = null,
    @ApiModelProperty("创建人", required = true)
    val creator: String,
    @ApiModelProperty("修改人", required = true)
    val modifier: String,
    @ApiModelProperty("创建日期", required = true)
    val createTime: Long = 0,
    @ApiModelProperty("更新日期", required = true)
    val updateTime: Long = 0
)