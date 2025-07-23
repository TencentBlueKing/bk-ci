package com.tencent.devops.store.pojo.common.honor

import io.swagger.v3.oas.annotations.media.Schema


@Schema(title = "荣誉国际化信息")
class HonorI18nDTO (
    @get:Schema(title = "荣誉头衔", required = true)
    val honorTitle: String,
    @get:Schema(title = "荣誉名称", required = true)
    val honorName: String,
    @get:Schema(title = "荣誉头衔国际化信息", required = true)
    val honorTitleI18n:String,
    @get:Schema(title = "荣誉名称国际化信息", required = true)
    val honorNameI18n:String,
    @get:Schema(title = "对应语言", required = true)
    val language: String,
)