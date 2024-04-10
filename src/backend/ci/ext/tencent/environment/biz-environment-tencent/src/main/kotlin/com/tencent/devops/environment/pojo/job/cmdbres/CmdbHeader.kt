package com.tencent.devops.environment.pojo.job.cmdbres

import io.swagger.v3.oas.annotations.media.Schema

data class CmdbHeader(
    @get:Schema(title = "实际返回的条目数", required = true)
    val returnRows: Int,
    @get:Schema(title = "总的条目数", required = true)
    val totalRows: Int
)