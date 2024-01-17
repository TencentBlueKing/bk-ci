package com.tencent.devops.common.api.pojo.agent

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "需要升级的项")
data class UpgradeItem(
    @Schema(title = "升级go agent")
    val agent: Boolean,
    @Schema(title = "升级worker")
    val worker: Boolean,
    @Schema(title = "升级jdk")
    val jdk: Boolean,
    @Schema(title = "升级docker init 脚本")
    val dockerInitFile: Boolean
)
