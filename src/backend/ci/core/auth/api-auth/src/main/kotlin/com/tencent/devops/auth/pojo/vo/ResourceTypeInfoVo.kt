package com.tencent.devops.auth.pojo.vo

import com.tencent.devops.common.api.annotation.BkFieldI18n
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("资源类型")
data class ResourceTypeInfoVo(
    @ApiModelProperty("ID")
    val id: Int,
    @ApiModelProperty("资源类型")
    val resourceType: String,
    @ApiModelProperty("资源类型名")
    @BkFieldI18n(keyPrefixName = "resourceType")
    val name: String,
    @ApiModelProperty("父类资源")
    val parent: String,
    @ApiModelProperty("所属系统")
    val system: String
)
