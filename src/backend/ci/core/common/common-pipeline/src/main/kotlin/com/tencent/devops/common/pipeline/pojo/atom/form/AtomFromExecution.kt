package com.tencent.devops.common.pipeline.pojo.atom.form

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 分组信息
 */
@Schema(title = "执行命令入口信息")
data class AtomFromExecution(
    @get:Schema(title = "开发语言")
    val language: String,
    @get:Schema(title = "执行依赖")
    val demands: List<String>,
    @get:Schema(title = "执行入口")
    val target: String
)
