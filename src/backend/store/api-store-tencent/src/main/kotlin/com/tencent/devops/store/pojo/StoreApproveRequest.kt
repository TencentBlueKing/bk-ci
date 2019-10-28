package com.tencent.devops.store.pojo

import com.tencent.devops.store.pojo.enums.ApproveStatusEnum
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("store审批信息请求报文体")
data class StoreApproveRequest(
    @ApiModelProperty("审批原因", required = true)
    val approveMsg: String,
    @ApiModelProperty("审批状态", required = true)
    val approveStatus: ApproveStatusEnum
)