package com.tencent.devops.support.model.wechatwork.message

import com.tencent.devops.support.model.wechatwork.base.Image
import com.tencent.devops.support.model.wechatwork.base.Receiver
import com.tencent.devops.support.model.wechatwork.enums.MsgType
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("图片消息")
data class ImageMessage(
    @ApiModelProperty("接收者")
    val receiver: Receiver,
    @ApiModelProperty("消息类型")
    val msgtype: MsgType = MsgType.image,
    @ApiModelProperty("文本")
    val image: Image
)