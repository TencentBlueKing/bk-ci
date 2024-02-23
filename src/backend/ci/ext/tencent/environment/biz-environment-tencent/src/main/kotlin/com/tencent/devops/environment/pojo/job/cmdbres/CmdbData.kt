package com.tencent.devops.environment.pojo.job.cmdbres

import io.swagger.v3.oas.annotations.media.Schema

data class CmdbData(
    @get:Schema(title = "数据属性字段")
    val fieldDef: List<CmdbFieldDef>,
    @get:Schema(title = "数据实例")
    val data: List<CmdbDataIns>?,
    @get:Schema(title = "返回数据数量信息")
    val header: CmdbHeader?
)