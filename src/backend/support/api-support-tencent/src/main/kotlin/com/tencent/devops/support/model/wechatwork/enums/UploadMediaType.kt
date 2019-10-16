package com.tencent.devops.support.model.wechatwork.enums

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("素材类型")
enum class UploadMediaType(private val type: String) {
    @ApiModelProperty("图片")
    image("image"),
    @ApiModelProperty("语音")
    voice("voice"),
    @ApiModelProperty("视频")
    video("video"),
    @ApiModelProperty("普通文件")
    file("file")
}