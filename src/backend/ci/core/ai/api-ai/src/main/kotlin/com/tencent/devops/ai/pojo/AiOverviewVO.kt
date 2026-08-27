package com.tencent.devops.ai.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "运营-智能体管理概览")
data class AiOverviewVO(
    @get:Schema(title = "资源计数")
    val counts: AiOverviewCountsVO,
    @get:Schema(title = "已开通 AI 的项目数")
    val projectCount: Int
)

@Schema(title = "运营-智能体管理资源计数")
data class AiOverviewCountsVO(
    val sysPrompts: Int,
    val welcomeGuides: Int,
    val hotQuestions: Int,
    val mcpServers: Int,
    val systemMcpServers: Int,
    val skills: Int,
    val systemSkills: Int,
    val kbSources: Int,
    val kbEntries: Int,
    val projectKbSources: Int,
    val externalAgents: Int,
    val userPrompts: Int,
    val userLlmConfigs: Int
)
