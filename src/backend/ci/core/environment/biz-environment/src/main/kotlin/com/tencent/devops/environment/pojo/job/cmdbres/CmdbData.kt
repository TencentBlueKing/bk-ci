package com.tencent.devops.environment.pojo.job.cmdbres

import io.swagger.annotations.ApiModelProperty

data class CmdbData(
    @ApiModelProperty(value = "数据属性字段")
    val fieldDef: List<CmdbFieldDef>,
    @ApiModelProperty(value = "数据实例")
    val data: List<CmdbDataIns>?,
    @ApiModelProperty(value = "返回数据数量信息")
    val header: CmdbHeader?
)