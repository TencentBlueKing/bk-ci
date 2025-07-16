package com.tencent.devops.store.pojo.common.honor

import com.tencent.devops.common.web.annotation.BkField
import com.tencent.devops.store.pojo.common.enums.BkLanguageEnum
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "荣誉信息")
class i18nHonorInfoDTO(
    @get:Schema(title = "荣誉头衔", required = true)
    @BkField(maxLength = 4)
    val honorTitle: String,
    @get:Schema(title = "荣誉名称", required = true)
    @BkField(maxLength = 40)
    val honorName: String,
    @get:Schema(title = "对应语言", required = true)
    val language: BkLanguageEnum
)