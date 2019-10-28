package com.tencent.devops.support.model.code

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("返回码更新请求报文体")
data class UpdateMessageCodeRequest(
    @ApiModelProperty("中文简体描述信息", required = true)
    var messageDetailZhCn: String,
    @ApiModelProperty("中文繁体描述信息", required = true)
    var messageDetailZhTw: String,
    @ApiModelProperty("英文描述信息", required = true)
    var messageDetailEn: String
)