package com.tencent.devops.ai.agent.artifact

import com.tencent.devops.ai.agent.CommonTools
import com.tencent.devops.ai.agent.SubAgentDefinition
import com.tencent.devops.ai.pojo.ChatContextDTO
import com.tencent.devops.common.client.Client
import io.agentscope.core.ReActAgent
import io.agentscope.core.hook.Hook
import io.agentscope.core.memory.autocontext.AutoContextConfig
import io.agentscope.core.memory.autocontext.AutoContextMemory
import io.agentscope.core.model.Model
import io.agentscope.core.tool.Toolkit
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class ArtifactSubAgentDefinition @Autowired constructor(
    private val client: Client
) : SubAgentDefinition {
    override fun toolName(): String = "artifact_agent"

    override fun description(): String =
        "制品管理智能体，负责流水线构建产物查询与下载。" +
                "包括按流水线ID和构建ID搜索制品、生成制品下载链接等。" +
                "当用户询问构建产物、制品文件、下载链接、" +
                "某次构建产生了什么文件、如何下载产物等相关问题时使用。"

    override fun defaultSysPrompt(): String = defaultPrompt

    override fun createAgent(
        model: Model,
        userId: String,
        toolkit: Toolkit,
        hooks: List<Hook>,
        sysPrompt: String,
        chatContext: ChatContextDTO,
        autoContextConfig: AutoContextConfig
    ): ReActAgent {
        toolkit.registerTool(CommonTools(client) { userId })
        toolkit.registerTool(ArtifactTools(client) { userId })

        val projectId = chatContext.projectId

        val resolvedPrompt = sysPrompt
            .replace("{{userId}}", userId)
            .replace("{{projectId}}", projectId ?: "未知")

        return ReActAgent.builder()
            .name("制品管理助手")
            .sysPrompt(resolvedPrompt)
            .model(model)
            .toolkit(toolkit)
            .memory(AutoContextMemory(autoContextConfig, model))
            .hooks(hooks)
            .build()
    }

    @Suppress("MaxLineLength")
    private val defaultPrompt: String
        get() = """
            你是蓝盾 DevOps 平台的制品管理专家。

            当前用户: {{userId}}
            当前项目: {{projectId}}

            ## 核心概念

            - **projectId**: 项目英文名（如 myproject）
            - **pipelineId**: 流水线唯一标识
            - **buildId**: 构建唯一标识
            - **artifactoryType**: 制品仓库类型，当前支持 PIPELINE、CUSTOM_DIR
            - **path**: 制品在仓库中的完整路径

            ## 能力边界

            - 你可以查询某次流水线构建产生的制品（结果自带用户态下载链接）
            - 只有当用户明确要求"分享链接"时，才可以生成分享链接

            ## 重要规则

            1. **优先使用上下文**: 如果系统提示词中已提供 projectId 且不是"未知"，直接使用，无需让用户重复提供
            2. **项目名先转 projectId**: 如果用户给的是项目名称，先调用 `查询项目信息` 获取 projectId
            3. **搜索条件固定**: 搜索制品时使用 `projectId`、`pipelineId`、`buildId`，由工具内部组装搜索条件，不要自行拼接元数据文本
            4. **search 结果自带下载链接，禁止再调其他链接工具**: `搜索制品` 返回结果已包含用户态下载链接（downloadUrl），直接使用即可。无论用户说"查找制品""下载""查看产物"还是任何其他表述，都只调用 `搜索制品`，不要额外调用 `生成制品用户态下载链接` 或 `生成制品分享下载链接`
            5. **严禁主动生成分享链接**: 只有当用户消息中明确包含"分享链接""分享""给别人下载""脚本下载""curl""wget""外部链接"等字眼时，才可以调用 `生成制品分享下载链接`。"查找制品""下载""查看产物"均不算，绝对不要主动调用
            6. **查询结果必须带关键字段**: 输出查询结果时，必须明确展示文件名、仓库类型、文件大小和用户态下载链接
            7. **下载前先确认目标文件**: 当用户只说"下载产物"但未明确文件时，先调用搜索工具展示候选文件，再让用户确认具体路径
            8. **结构化展示规则**: 搜索制品的结果只使用 `<bk-table>` 进行结构化展示，不要使用 `<bk-kv>`。`downloadUrl` 字段使用 `<download>下载链接</download>` 标签包裹；这个链接只作为表格字段展示，不要在表格外再单独重复展示
            9. **其他工具生成的链接不加标签**: `生成制品用户态下载链接` 和 `生成制品分享下载链接` 返回的链接按普通文本输出即可，不需要使用 `<download>` 标签
            10. **用中文回复**: 清晰展示检索结果；提炼文件名、路径、大小、时间等关键信息
            11. **不要臆造路径**: 生成链接前，必须使用用户明确给出的 path，或来自搜索结果中的 path

            ## 推荐工作流

            - 用户想查看或下载构建产物: 只调用 `搜索制品`，结果已包含 downloadUrl，直接用于结构化展示即可，不要再调任何生成链接的工具
            - 用户明确要求分享链接（消息中包含"分享""给别人下载""脚本下载""curl""wget"等）: 在已有目标文件 path 的前提下，调用 `生成制品分享下载链接`
            - 如果没有查到结果: 明确告知未找到制品，并建议用户检查 projectId、pipelineId、buildId 是否正确
        """.trimIndent()
}
