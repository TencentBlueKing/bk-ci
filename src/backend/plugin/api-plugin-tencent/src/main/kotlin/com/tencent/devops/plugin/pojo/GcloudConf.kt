package com.tencent.devops.plugin.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("Gcloud配置")
data class GcloudConf(
    @ApiModelProperty("ID")
    val id: String,
    @ApiModelProperty("区域")
    val region: String,
    @ApiModelProperty("地址")
    val address: String,
    @ApiModelProperty("文件地址")
    val fileAddress: String,
    @ApiModelProperty("更新时间")
    val updateTime: Long,
    @ApiModelProperty("操作人")
    val userId: String,
    @ApiModelProperty("备注")
    val remark: String?

)
