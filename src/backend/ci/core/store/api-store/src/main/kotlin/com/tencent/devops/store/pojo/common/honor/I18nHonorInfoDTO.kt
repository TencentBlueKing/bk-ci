package com.tencent.devops.store.pojo.common.honor

import com.tencent.devops.common.web.annotation.BkField
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "荣誉信息")
class I18nHonorInfoDTO(
    @get:Schema(title = "荣誉头衔", required = true)
    @BkField(maxLength = 4)
    val honorTitle: String,
    @get:Schema(title = "荣誉名称", required = true)
    @BkField(maxLength = 40)
    val honorName: String,
    @get:Schema(title = "荣誉头衔国际化信息", required = false)
    val honorTitleI18n: String? = null,
    @get:Schema(title = "荣誉名称国际化信息", required = false)
    val honorNameI18n: String? = null,
    @get:Schema(title = "对应语言", required = true)
    val language: String
)