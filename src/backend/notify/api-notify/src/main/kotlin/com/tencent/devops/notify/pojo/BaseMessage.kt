package com.tencent.devops.notify.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("基础消息类型")
open class BaseMessage {

    @ApiModelProperty("频率限制，单位分钟，即 frequencyLimit 分钟内限制不重发相同内容的消息")
    var frequencyLimit: Int = 0

    @ApiModelProperty("源系统id")
    var fromSysId: String = ""

    @ApiModelProperty("tof系统id")
    var tofSysId: String = ""
}