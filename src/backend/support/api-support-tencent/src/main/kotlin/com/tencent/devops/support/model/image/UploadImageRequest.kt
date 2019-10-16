package com.tencent.devops.support.model.image

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("上传图片请求报文体")
data class UploadImageRequest(
    @ApiModelProperty("图片类型")
    val imageType: String,
    @ApiModelProperty("图片内容")
    val imageContentStr: String
)