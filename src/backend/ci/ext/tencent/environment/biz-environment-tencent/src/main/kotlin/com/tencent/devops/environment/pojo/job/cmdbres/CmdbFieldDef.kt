package com.tencent.devops.environment.pojo.job.cmdbres

import io.swagger.annotations.ApiModelProperty

data class CmdbFieldDef(
    @ApiModelProperty(value = "属性类型", required = true)
    val dataType: String,
    @ApiModelProperty(value = "属性英文ID", required = true)
    val id: String,
    @ApiModelProperty(value = "属性名称", required = true)
    val name: String
)