package com.tencent.devops.store.pojo.image.request

import com.tencent.devops.store.pojo.image.enums.ImageRDTypeEnum
import io.swagger.annotations.ApiModelProperty

data class ApproveImageReq(
    @ApiModelProperty("镜像标识")
    val imageCode: String,
    @ApiModelProperty("是否为公共镜像")
    val publicFlag: Boolean,
    @ApiModelProperty("是否推荐")
    val recommendFlag: Boolean,
    @ApiModelProperty("是否为官方认证")
    val certificationFlag: Boolean,
    @ApiModelProperty("研发来源 SELF_DEVELOPED：自研 THIRD_PARTY：第三方", required = false)
    val rdType: ImageRDTypeEnum?,
    @ApiModelProperty("权重（数值越大代表权重越高）")
    val weight: Int?,
    @ApiModelProperty("审核结果：PASS：通过|REJECT：驳回")
    val result: String,
    @ApiModelProperty("审核结果说明")
    val message: String
)