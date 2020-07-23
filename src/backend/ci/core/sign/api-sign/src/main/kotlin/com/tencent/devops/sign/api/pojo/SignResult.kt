package com.tencent.devops.sign.api.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("签名状态查询结果")
data class SignResult(
    @ApiModelProperty("签名ID", required = true)
    val resignId: String,
    @ApiModelProperty("是否完成", required = true)
    val finished: Boolean,
    @ApiModelProperty("重签IPA的下载链接", required = false)
    val fileDownloadUrl: String?
)