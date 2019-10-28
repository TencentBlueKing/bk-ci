package com.tencent.devops.store.pojo.ideatom

import com.tencent.devops.store.pojo.ideatom.enums.IdeAtomTypeEnum
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("IDE插件市场-IDE插件特性信息请求报文体")
data class IdeAtomFeatureRequest(
    @ApiModelProperty("IDE插件代码", required = true)
    val atomCode: String,
    @ApiModelProperty("插件类型，SELF_DEVELOPED：自研 THIRD_PARTY：第三方开发", required = false)
    val atomType: IdeAtomTypeEnum? = null,
    @ApiModelProperty("是否为公共IDE插件， TRUE：是 FALSE：不是", required = false)
    val publicFlag: Boolean? = null,
    @ApiModelProperty("是否推荐标识 true：推荐，false：不推荐", required = false)
    val recommendFlag: Boolean? = null,
    @ApiModelProperty(value = "代码库地址", required = false)
    var codeSrc: String? = null,
    @ApiModelProperty(value = "代码库命名空间", required = false)
    var nameSpacePath: String? = null,
    @ApiModelProperty(value = "权重（数值越大代表权重越高）", required = false)
    var weight: Int? = null
)