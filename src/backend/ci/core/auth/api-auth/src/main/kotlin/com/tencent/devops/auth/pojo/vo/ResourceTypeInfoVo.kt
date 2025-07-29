package com.tencent.devops.auth.pojo.vo

import com.tencent.devops.common.api.annotation.BkFieldI18n
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "资源类型")
data class ResourceTypeInfoVo(
    @get:Schema(title = "ID")
    val id: Int = 0,
    @get:Schema(title = "资源类型")
    val resourceType: String,
    @get:Schema(title = "资源类型名")
    @BkFieldI18n(keyPrefixName = "resourceType")
    val name: String,
    @get:Schema(title = "父类资源")
    val parent: String = "",
    @get:Schema(title = "所属系统")
    val system: String = "",
    @get:Schema(title = "授权交接失败成员列表")
    val memberIds: List<String> = emptyList()
)
