package com.tencent.devops.store.pojo.ideatom

import com.tencent.devops.store.pojo.ideatom.enums.IdeAtomTypeEnum
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("IDE插件发布请求报文体")
data class IdeAtomReleaseRequest(
    @ApiModelProperty("插件类型，SELF_DEVELOPED：自研 THIRD_PARTY：第三方开发", required = true)
    val atomType: IdeAtomTypeEnum,
    @ApiModelProperty("是否为公共插件 true：公共插件 false：普通插件", required = true)
    val publicFlag: Boolean,
    @ApiModelProperty("是否推荐， TRUE：是 FALSE：不是", required = true)
    val recommendFlag: Boolean,
    @ApiModelProperty("权重（数值越大代表权重越高）", required = false)
    val weight: Int?,
    @ApiModelProperty("插件安装包名称", required = true)
    val pkgName: String
)