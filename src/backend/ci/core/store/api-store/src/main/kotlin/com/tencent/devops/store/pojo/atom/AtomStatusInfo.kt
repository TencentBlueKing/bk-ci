package com.tencent.devops.store.pojo.atom

import com.tencent.devops.common.api.annotation.BkFieldI18n
import com.tencent.devops.common.api.enums.I18nSourceEnum
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "插件状态信息")
data class AtomStatusInfo(
    @get:Schema(title = "插件标识")
    val atomCode: String,
    @get:Schema(title = "名称")
    @BkFieldI18n(source = I18nSourceEnum.DB)
    val name: String,
    @get:Schema(title = "版本号")
    val version: String,
    @get:Schema(title = "插件状态")
    val atomStatus: Byte
)
