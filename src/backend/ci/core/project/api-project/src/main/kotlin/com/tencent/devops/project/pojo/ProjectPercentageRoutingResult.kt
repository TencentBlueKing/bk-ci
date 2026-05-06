package com.tencent.devops.project.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "按比例放量路由执行结果")
data class ProjectPercentageRoutingResult(
    @get:Schema(title = "是否为预览模式")
    val dryRun: Boolean,

    @get:Schema(title = "目标百分比")
    val targetPercent: Int,

    @get:Schema(title = "项目全集总数")
    val totalProjectCount: Int,

    @get:Schema(title = "本次实际目标数（哈希命中，已扣除黑名单）")
    val targetCount: Int,

    @get:Schema(title = "已在目标 tag（幂等跳过数）")
    val alreadyDoneCount: Int,

    @get:Schema(
        title = "本次实际切换数（execute 模式下有效，dry-run 时等于 targetCount - alreadyDoneCount）"
    )
    val switchedCount: Int,

    @get:Schema(title = "切换失败数")
    val failedCount: Int,

    @get:Schema(title = "切换失败的项目列表")
    val failedProjects: List<String>
)
