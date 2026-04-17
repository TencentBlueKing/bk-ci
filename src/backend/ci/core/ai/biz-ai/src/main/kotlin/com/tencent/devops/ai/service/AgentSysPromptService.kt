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

package com.tencent.devops.ai.service

import com.tencent.devops.ai.dao.AgentSysPromptDao
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.regex.Matcher.quoteReplacement
import java.util.regex.Pattern

/**
 * 智能体系统提示词服务。
 *
 * 从数据库加载提示词模板，支持 `{{变量}}` 占位符替换。
 */
@Service
class AgentSysPromptService @Autowired constructor(
    private val dslContext: DSLContext,
    private val dao: AgentSysPromptDao
) {

    /**
     * 根据智能体名称获取对应的系统提示词模板。
     *
     * @return 模板内容；未配置或未启用时返回 null
     */
    fun getPromptTemplate(agentName: String): String? {
        val template = dao.getEnabledPrompt(dslContext, agentName)
        if (template != null) {
            logger.info(
                "[SysPrompt] Loaded template for agent={}",
                agentName
            )
        }
        return template
    }

    /**
     * 将模板中的 `{{varName}}` 占位符替换为实际值。
     * 未匹配到的占位符保持原样。
     */
    fun resolveTemplate(
        template: String,
        variables: Map<String, String>
    ): String {
        if (variables.isEmpty()) return template
        val matcher = PLACEHOLDER_PATTERN.matcher(template)
        val sb = StringBuffer()
        while (matcher.find()) {
            val varName = matcher.group(1).trim()
            val value = variables[varName]
            matcher.appendReplacement(
                sb, quoteReplacement(value ?: matcher.group())
            )
        }
        matcher.appendTail(sb)
        return sb.toString()
    }

    /**
     * 构建智能体的完整系统提示词。
     *
     * 流程：
     * 1. 从 DB 获取 agentName 对应的模板，若未配置则使用 defaultPrompt
     * 2. 追加全局通用后缀（AGENT_NAME='*'）
     * 3. 解析模板中的 {{变量}} 占位符
     *
     * @param agentName 智能体名称（如 "supervisor"、"auth_agent"）
     * @param defaultPrompt 未配置 DB 模板时的默认提示词
     * @param variables 变量映射，用于替换 {{变量}} 占位符
     * @return 完整的系统提示词
     */
    fun buildSysPrompt(
        agentName: String,
        defaultPrompt: String,
        variables: Map<String, String>
    ): String {
        val baseTemplate = getPromptTemplate(agentName) ?: defaultPrompt
        val suffix = getPromptTemplate(GLOBAL_AGENT_NAME)
        val fullTemplate = if (suffix.isNullOrBlank()) {
            baseTemplate
        } else {
            "$baseTemplate\n\n$suffix"
        }
        return resolveTemplate(fullTemplate, variables)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(
            AgentSysPromptService::class.java
        )
        /** 匹配 {{varName}} 格式的占位符 */
        private val PLACEHOLDER_PATTERN =
            Pattern.compile("\\{\\{([^}]+)}}")
        /** 全局通用后缀的 agentName */
        private const val GLOBAL_AGENT_NAME = "*"
    }
}
