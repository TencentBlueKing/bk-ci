package com.tencent.devops.support.model.wechatwork.enums

import com.fasterxml.jackson.annotation.JsonValue
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("消息类型")
enum class MsgType(private val type: String) {
    @ApiModelProperty("文本")
    text("text"),
    @ApiModelProperty("图片")
    image("image"),
    @ApiModelProperty("文件")
    file("file"),
    @ApiModelProperty("富文本")
    rich_text("rich_text");

    @JsonValue
    fun jsonValue(): String {
        return type
    }
}