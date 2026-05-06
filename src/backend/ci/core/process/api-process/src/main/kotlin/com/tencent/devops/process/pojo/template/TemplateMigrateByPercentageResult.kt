package com.tencent.devops.process.pojo.template

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "按比例灰度迁移模板执行结果")
data class TemplateMigrateByPercentageResult(
    @get:Schema(title = "是否为预览模式")
    val dryRun: Boolean,

    @get:Schema(title = "目标百分比")
    val targetPercent: Int,

    @get:Schema(title = "项目全集总数")
    val totalProjectCount: Int,

    @get:Schema(title = "本次命中的目标项目数（哈希命中，已扣除黑名单）")
    val targetCount: Int,

    @get:Schema(title = "已完成迁移的项目数（幂等跳过数）")
    val alreadyDoneCount: Int,

    @get:Schema(
        title = "本次已提交迁移任务数（execute 模式下为已提交异步任务数，dry-run 时等于 targetCount - alreadyDoneCount）"
    )
    val migratedCount: Int,

    @get:Schema(title = "迁移失败数")
    val failedCount: Int,

    @get:Schema(title = "迁移失败的项目列表")
    val failedProjects: List<String>
)
