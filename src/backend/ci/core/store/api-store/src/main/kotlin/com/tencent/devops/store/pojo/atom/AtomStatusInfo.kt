package com.tencent.devops.store.pojo.atom

import com.tencent.devops.common.api.annotation.BkFieldI18n
import com.tencent.devops.common.api.enums.I18nSourceEnum
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("插件状态信息")
data class AtomStatusInfo(
    @ApiModelProperty("插件标识")
    val atomCode: String,
    @ApiModelProperty("名称")
    @BkFieldI18n(source = I18nSourceEnum.DB)
    val name: String,
    @ApiModelProperty("版本号")
    val version: String,
    @ApiModelProperty("插件状态")
    val atomStatus: Byte
)
