package com.tencent.devops.ai.agent

import com.tencent.devops.ai.pojo.ChatContextDTO
import io.agentscope.core.ReActAgent
import io.agentscope.core.hook.Hook
import io.agentscope.core.memory.autocontext.AutoContextConfig
import io.agentscope.core.model.Model
import io.agentscope.core.tool.Toolkit

/**
 * 子智能体注册契约。
 *
 * 各领域模块实现此接口并注册为 Spring Bean，Supervisor 启动时
 * 自动发现所有 [SubAgentDefinition] Bean，通过
 * `Toolkit.registration().subAgent()` 注册为工具。
 *
 * MCP 工具会由 Supervisor 工厂根据数据库 BIND_AGENT 字段
 * 自动加载到 [toolkit] 中，子智能体无需关心 MCP 注册。
 */
interface SubAgentDefinition {

    /** Supervisor 调用此子智能体时使用的唯一工具名。 */
    fun toolName(): String

    /** 供 LLM 判断何时调用此子智能体的描述信息。 */
    fun description(): String

    /**
     * 是否将此子智能体作为工具注册到 Supervisor 上。
     *
     * 返回 `true`（默认）时，Supervisor 可通过 LLM 决策自动调度；
     * 只能通过服务间调用或用户接口独立使用。
     */
    fun bindToSupervisor(): Boolean = true

    /** 硬编码的默认系统提示词，当数据库未配置时使用。 */
    fun defaultSysPrompt(): String

    /**
     * 工厂方法，每次调用创建一个新的 [ReActAgent] 实例。
     * @param model 使用的 LLM 模型
     * @param userId 来自 X-DEVOPS-UID 的认证用户
     * @param toolkit 由 Supervisor 工厂预装了 BIND_AGENT
     *        匹配 [toolName] 的 MCP 工具，子智能体在此基础上
     *        添加自己的专属工具即可
     * @param hooks 由 Supervisor 工厂根据 BIND_AGENT 匹配的
     *        Skill 构建的 Hook 列表（含 SkillHook、AutoContextHook 等），
     *        子智能体应传递给 ReActAgent.builder().hooks()
     * @param sysPrompt 从数据库加载的系统提示词，为 null 时
     *        使用子智能体自身的硬编码默认值
     * @param chatContext 由 Supervisor 从前端请求解析的结构化
     *        上下文，子智能体可直接读取 projectId 等字段，
     *        无需依赖 ThreadLocal
     * @param autoContextConfig 自动上下文记忆配置，非 null 时
     *        子智能体应使用 [AutoContextMemory] 进行上下文压缩管理
     */
    fun createAgent(
        model: Model,
        userId: String,
        toolkit: Toolkit,
        hooks: List<Hook> = emptyList(),
        sysPrompt: String,
        chatContext: ChatContextDTO = ChatContextDTO(),
        autoContextConfig: AutoContextConfig
    ): ReActAgent
}
