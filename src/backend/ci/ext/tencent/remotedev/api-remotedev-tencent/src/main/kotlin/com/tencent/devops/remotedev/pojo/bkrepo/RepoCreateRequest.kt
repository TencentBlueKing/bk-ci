package com.tencent.devops.remotedev.pojo.bkrepo

import io.swagger.v3.oas.annotations.media.Schema

/**
 * BkRepo仓库创建请求
 */
@Schema(title = "BkRepo仓库创建请求")
data class RepoCreateRequest(
    @get:Schema(title = "项目ID", required = true)
    val projectId: String,
    @get:Schema(title = "仓库类型", required = true)
    val type: String,
    @get:Schema(title = "仓库名称", required = true)
    val name: String,
    @get:Schema(title = "是否公开", required = false)
    val public: Boolean = false,
    @get:Schema(title = "是否显示", required = false)
    val display: Boolean = false,
    @get:Schema(title = "仓库描述", required = false)
    val description: String = "",
    @get:Schema(title = "仓库类别", required = true)
    val category: String,
    @get:Schema(title = "仓库配置", required = true)
    val configuration: RepoConfiguration
)

/**
 * 仓库配置
 */
@Schema(title = "仓库配置")
data class RepoConfiguration(
    @get:Schema(title = "配置类型", required = false)
    val type: String = "local",
    @get:Schema(title = "仓库设置", required = true)
    val settings: RepoSettings
)

/**
 * 仓库设置
 */
@Schema(title = "仓库设置")
data class RepoSettings(
    @get:Schema(title = "是否系统仓库", required = false)
    val system: Boolean = false,
    @get:Schema(title = "自动索引配置", required = true)
    val autoIndex: AutoIndexConfig
)

/**
 * 自动索引配置
 */
@Schema(title = "自动索引配置")
data class AutoIndexConfig(
    @get:Schema(title = "是否启用", required = false)
    val enabled: Boolean = false
)
