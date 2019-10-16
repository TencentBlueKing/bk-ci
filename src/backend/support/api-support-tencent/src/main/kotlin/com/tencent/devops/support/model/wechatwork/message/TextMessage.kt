package com.tencent.devops.support.model.wechatwork.message

import com.tencent.devops.support.model.wechatwork.base.Receiver
import com.tencent.devops.support.model.wechatwork.base.Text
import com.tencent.devops.support.model.wechatwork.enums.MsgType
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("文本消息")
data class TextMessage(
    @ApiModelProperty("接收者")
    val receiver: Receiver,
    @ApiModelProperty("消息类型")
    val msgtype: MsgType = MsgType.text,
    @ApiModelProperty("文本")
    val text: Text
)