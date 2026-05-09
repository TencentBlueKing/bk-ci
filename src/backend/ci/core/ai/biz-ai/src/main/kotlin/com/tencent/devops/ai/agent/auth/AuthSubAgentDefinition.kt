/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.ai.agent.auth

import com.tencent.devops.ai.agent.CommonTools
import com.tencent.devops.ai.agent.SubAgentDefinition
import com.tencent.devops.ai.pojo.ChatContextDTO
import com.tencent.devops.auth.api.service.ServiceProjectAuthResource
import com.tencent.devops.common.client.Client
import io.agentscope.core.ReActAgent
import io.agentscope.core.hook.Hook
import io.agentscope.core.memory.autocontext.AutoContextConfig
import io.agentscope.core.memory.autocontext.AutoContextMemory
import io.agentscope.core.model.Model
import io.agentscope.core.tool.Toolkit
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * 权限管理子智能体定义。
 *
 * 注册多组权限场景工具类（用户组/成员查询、权限洞察、管理员变更、成员自助），
 * 提供基于 IAM RBAC 的权限查询与管理能力。
 * 创建时自动解析当前用户在目标项目中的角色，以约束操作范围。
 */
@Component
class AuthSubAgentDefinition @Autowired constructor(
    private val client: Client
) : SubAgentDefinition {

    override fun toolName(): String = "auth_agent"

    override fun description(): String =
        "权限管理智能体，负责项目权限全生命周期管理。" +
                "包括分析权限、查询资源类型/操作权限、" +
                "查询用户组/权限详情/成员列表、" +
                "添加/删除/续期/移除/交接用户组成员、" +
                "智能推荐用户组、申请权限、" +
                "将用户移出项目等。" +
                "当用户询问权限、用户组、成员、授权、" +
                "续期、交接、移除权限、" +
                "分析权限、申请权限等相关问题时使用。"

    override fun defaultSysPrompt(): String = authSubAgentOperationGuideMarkdown()

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
        toolkit.registerTool(AuthGroupMemberQueryTools(client) { userId })
        toolkit.registerTool(AuthPermissionInsightTools(client) { userId })
        toolkit.registerTool(AuthAdminMutationTools(client) { userId })
        toolkit.registerTool(AuthMemberSelfServiceTools(client) { userId })

        val projectId = chatContext.projectId
        val userRole = resolveUserRole(userId, projectId)

        val resolvedPrompt = sysPrompt
            .replace("{{userId}}", userId)
            .replace("{{projectId}}", projectId ?: "未知")
            .replace("{{userRole}}", userRole)

        return ReActAgent.builder()
            .name("权限管理助手")
            .sysPrompt(resolvedPrompt)
            .model(model)
            .toolkit(toolkit)
            .memory(AutoContextMemory(autoContextConfig, model))
            .hooks(hooks)
            .build()
    }

    /** 通过 Auth 服务校验是否为管理员，异常视为普通成员。 */
    private fun resolveUserRole(
        userId: String,
        projectId: String?
    ): String {
        if (projectId.isNullOrBlank()) return ROLE_UNDETERMINED
        return try {
            client.get(ServiceProjectAuthResource::class)
                .checkProjectManagerAndMessage(
                    userId = userId,
                    projectId = projectId
                )
            "管理员"
        } catch (e: Exception) {
            logger.debug(
                "[AuthAgent] User {} is not manager of {}",
                userId, projectId
            )
            "普通成员"
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(
            AuthSubAgentDefinition::class.java
        )
        const val ROLE_UNDETERMINED = "待确定"
    }
}
