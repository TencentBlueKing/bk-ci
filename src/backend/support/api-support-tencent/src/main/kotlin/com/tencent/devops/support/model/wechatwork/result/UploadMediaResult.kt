package com.tencent.devops.support.model.wechatwork.result

import com.tencent.devops.support.model.wechatwork.enums.UploadMediaType
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("上传临时素材返回结果")
data class UploadMediaResult(
    @ApiModelProperty("临时素材的类型")
    val mediaType: UploadMediaType,
    @ApiModelProperty("临时素材的ID")
    val mediaID: String
)