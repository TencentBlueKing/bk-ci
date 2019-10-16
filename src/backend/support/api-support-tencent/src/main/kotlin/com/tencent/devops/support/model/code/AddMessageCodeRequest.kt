package com.tencent.devops.support.model.code

import com.tencent.devops.common.api.enums.SystemModuleEnum
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("返回码新增请求报文体")
data class AddMessageCodeRequest(
    @ApiModelProperty("信息码", required = true)
    val messageCode: String,
    @ApiModelProperty("模块代码", required = true)
    val moduleCode: SystemModuleEnum,
    @ApiModelProperty("中文简体描述信息", required = true)
    var messageDetailZhCn: String,
    @ApiModelProperty("中文繁体描述信息", required = true)
    var messageDetailZhTw: String,
    @ApiModelProperty("英文描述信息", required = true)
    var messageDetailEn: String
)