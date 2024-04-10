package com.tencent.devops.environment.pojo.job.cmdbres

import io.swagger.v3.oas.annotations.media.Schema

data class CmdbFieldDef(
    @get:Schema(title = "属性类型", required = true)
    val dataType: String,
    @get:Schema(title = "属性英文ID", required = true)
    val id: String,
    @get:Schema(title = "属性名称", required = true)
    val name: String
)