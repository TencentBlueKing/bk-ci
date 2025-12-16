package com.tencent.devops.store.pojo.common

import com.tencent.devops.store.pojo.atom.enums.JobTypeEnum
import com.tencent.devops.store.pojo.common.enums.ServiceScopeEnum
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "服务范围配置")
data class ServiceScopeConfig(
    @get:Schema(title = "服务范围", required = true)
    val serviceScope: ServiceScopeEnum,

    @get:Schema(title = "所属插件分类代码", required = true)
    val classifyCode: String,

    @get:Schema(title = "适用Job类型", required = true)
    val jobType: JobTypeEnum,

    @get:Schema(title = "插件标签列表", required = false)
    val labelIdList: List<String>? = null
)