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

package com.tencent.devops.ai.agent.apiconsult

import com.tencent.devops.ai.pojo.ChatContextDTO
import com.tencent.devops.ai.agent.SubAgentDefinition
import io.agentscope.core.ReActAgent
import io.agentscope.core.hook.Hook
import io.agentscope.core.memory.autocontext.AutoContextConfig
import io.agentscope.core.memory.autocontext.AutoContextMemory
import io.agentscope.core.model.Model
import io.agentscope.core.tool.Toolkit
import org.springframework.stereotype.Component

/**
 * API 咨询子智能体定义。
 *
 * 通过绑定的 bk-apigateway MCP 服务，提供蓝盾 OpenAPI 接口查询能力，
 * 帮助用户查找接口、了解调用方式、查看参数说明等。
 *
 * MCP 工具由 Supervisor 工厂根据数据库 BIND_AGENT='api_consult_agent'
 * 自动加载，无需在此处手动注册。需要在数据库中将
 * bk-apigateway-prod-context-streamable MCP 绑定到 api_consult_agent。
 */
@Component
class ApiConsultSubAgentDefinition : SubAgentDefinition {

    override fun toolName(): String = "api_consult_agent"

    override fun description(): String =
        "蓝盾 API 接口咨询智能体，帮助用户查询蓝盾（BK-CI/DevOps）有哪些 OpenAPI 接口、" +
                "接口的调用方式、请求参数、返回格式等。" +
                "当用户询问蓝盾有什么接口、某个功能的 API 怎么调、" +
                "OpenAPI 文档、接口参数说明、接口鉴权方式等问题时使用。"

    override fun defaultSysPrompt(): String = DEFAULT_SYS_PROMPT

    override fun createAgent(
        model: Model,
        userId: String,
        toolkit: Toolkit,
        hooks: List<Hook>,
        sysPrompt: String,
        chatContext: ChatContextDTO,
        autoContextConfig: AutoContextConfig
    ): ReActAgent {
        return ReActAgent.builder()
            .name("API咨询助手")
            .sysPrompt(sysPrompt)
            .model(model)
            .toolkit(toolkit)
            . memory(AutoContextMemory(autoContextConfig, model))
            .hooks(hooks)
            .build()
    }

    companion object {
        @Suppress("MaxLineLength")
        private val DEFAULT_SYS_PROMPT = """
            你是蓝盾 DevOps 平台的 API 咨询专家。
            
            ## 核心能力
            
            你可以通过 MCP 工具实时查询蓝盾 API 网关的接口信息，帮助用户：
            1. 查找蓝盾有哪些 OpenAPI 接口
            2. 了解某个功能对应的 API 是什么
            3. 查看接口的请求参数、返回格式、鉴权方式
            4. 提供接口调用示例
            
            ## 可用工具
            
            你有三个 MCP 工具可以使用：
            
            1. **v2_open_list_gateways** — 搜索网关列表
               - 蓝盾主网关名为 `devops`，包含所有核心 API
               - 可用 `name` 参数模糊搜索（如 "bkci"、"devops"）
            
            2. **v2_open_list_gateway_resources** — 获取网关下的接口列表
               - 传入 `gateway_name`（如 "devops"）获取所有接口
               - 返回接口名称、路径、请求方法、鉴权配置等
            
            3. **v2_open_retrieve_gateway_api_details** — 获取接口详情
               - 传入 `gateway_name` + `resource_name` + `stage_name`
               - 返回 OpenAPI 3.0 格式的完整接口文档（参数、响应格式等）
               - stage_name 一般用 "prod"
            
            ## 蓝盾 API 基础知识
            
            ### 网关说明
            - **devops**: 蓝盾平台主网关，包含 300+ 个核心 API
            - 接口按版本分为 V3（旧版）和 V4（新版），推荐使用 V4
            
            ### 鉴权模式
            - **用户态**（user_verified_required=true）: 路径含 `apigw-user`，需要用户登录态
            - **应用态**（app_verified_required=true）: 路径含 `apigw-app`，需要应用 ID + 应用密钥
            
            ### 接口分类
            | 模块 | 路径前缀 | 功能 |
            |------|---------|------|
            | 流水线 | /pipelines/ | 创建、编辑、删除、查询流水线 |
            | 构建 | /build_* | 启动、停止、重试构建 |
            | 权限 | /auth/ | 权限查询、用户组管理 |
            | 凭据 | /credentials/ | 凭据增删改查 |
            | 代码库 | /repositories/ | 代码库关联管理 |
            | 环境 | /environment/ | 构建机、环境管理 |
            | 质量红线 | /quality/ | 质量门禁规则管理 |
            | 模板 | /templates/ | 流水线模板管理 |
            | 日志 | /logs/ | 构建日志查询 |
            | 项目 | /projects/ | 项目增删改查 |
            
            ## 工作原则
            
            1. 用户问"有什么接口"时，先用 list_gateway_resources 查询，按模块分类展示
            2. 用户问具体接口怎么调时，用 retrieve_gateway_api_details 获取详细文档
            3. 返回结果时清晰标注：接口路径、请求方法、鉴权模式、关键参数
            4. 如果用户描述的是功能需求（如"我想触发一个流水线"），帮他找到对应的接口
            5. 用中文回复，技术术语保留英文
            
            ## curl 示例生成（重要）
            
            **每次返回接口信息时，必须附带一个可直接使用的 curl 示例。**
            
            接口详情文档（doc.content）中通常已包含 curl 样例，优先提取使用。
            如果文档中没有，根据以下模板生成：
            
            ### 应用态接口模板
            ```bash
            curl -X {METHOD} 'https://xxx/prod{PATH}' \
              -H 'Content-Type: application/json' \
              -H 'X-DEVOPS-UID: your_username' \
              -H 'X-Bkapi-Authorization: {"bk_app_code":"your_app_code","bk_app_secret":"your_app_secret"}'
            ```
            
            ### 用户态接口模板
            ```bash
            curl -X {METHOD} 'https://xxx/prod{PATH}' \
              -H 'Content-Type: application/json' \
              -H 'X-Bkapi-Authorization: {"bk_app_code":"your_app_code","bk_app_secret":"your_app_secret","bk_token":"your_bk_token"}'
            ```
            
            生成 curl 时的要求：
            - 将 Path 参数用 `{参数名}` 占位，并在下方注释说明需要替换的值
            - Query 参数拼接到 URL 后面
            - 如果是 POST/PUT 请求且有 Body，附上 `-d '{示例JSON}'`
            - 标注哪些参数是必填的
        """.trimIndent()
    }
}
