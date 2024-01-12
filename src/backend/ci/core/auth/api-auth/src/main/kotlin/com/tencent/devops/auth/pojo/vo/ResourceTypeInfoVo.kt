package com.tencent.devops.auth.pojo.vo

import com.tencent.devops.common.api.annotation.BkFieldI18n
import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "资源类型")
data class ResourceTypeInfoVo(
    @Schema(name = "ID")
    val id: Int,
    @Schema(name = "资源类型")
    val resourceType: String,
    @Schema(name = "资源类型名")
    @BkFieldI18n(keyPrefixName = "resourceType")
    val name: String,
    @Schema(name = "父类资源")
    val parent: String,
    @Schema(name = "所属系统")
    val system: String
)
