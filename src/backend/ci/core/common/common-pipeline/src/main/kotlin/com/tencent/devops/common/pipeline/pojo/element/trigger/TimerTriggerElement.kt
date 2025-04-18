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

package com.tencent.devops.common.pipeline.pojo.element.trigger

import com.cronutils.mapper.CronMapper
import com.cronutils.model.CronType
import com.cronutils.model.definition.CronDefinitionBuilder
import com.cronutils.parser.CronParser
import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.enums.TriggerRepositoryType
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.v3.oas.annotations.media.Schema
import org.slf4j.LoggerFactory

@Schema(title = "定时触发")
data class TimerTriggerElement(
    @get:Schema(title = "任务名称", required = true)
    override val name: String = "定时触发",
    @get:Schema(title = "id", required = false)
    override var id: String? = null,
    @get:Schema(title = "状态", required = false)
    override var status: String? = null,
    // express是老的接口数据， 后面要废弃掉
    @get:Schema(title = "定时表达式", required = false)
    @Deprecated(message = "@see advanceExpression")
    val expression: String? = null,
    @get:Schema(title = "改进后的表达式", required = false)
    val newExpression: List<String>? = null,
    @get:Schema(title = "高级定时表达式", required = false)
    val advanceExpression: List<String>? = null,
    @get:Schema(title = "源代码未更新则不触发构建", required = false)
    val noScm: Boolean? = false,
    @get:Schema(title = "指定代码库分支", required = false)
    val branches: List<String>? = null,
    @get:Schema(title = "代码库类型", required = false)
    val repositoryType: TriggerRepositoryType? = null,
    @get:Schema(title = "代码库HashId", required = false)
    val repoHashId: String? = null,
    @get:Schema(title = "指定代码库别名", required = false)
    val repoName: String? = null,
    @get:Schema(title = "定时启动参数,格式: [{key:'id',value:1},{key:'name',value:'xxx'}]", required = false)
    val startParams: String? = null
) : Element(name, id, status) {
    companion object {
        const val classType = "timerTrigger"
        val logger = LoggerFactory.getLogger(TimerTriggerElement::class.java)
    }

    override fun getClassType() = classType

    private fun isOldExpress() =
        (newExpression == null || newExpression.isEmpty()) &&
            (advanceExpression == null || advanceExpression.isEmpty())

    private fun convertExpression(expression: String): String {
        val unixDefinition = CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX)
        val parser = CronParser(unixDefinition)
        return try {
            val qaurtzCron = parser.parse(expression)
            val mapper = CronMapper.fromUnixToQuartz()
            mapper.map(qaurtzCron).asString()
        } catch (ignore: IllegalArgumentException) {
            // The old cron, just return it
            expression
        }
    }

    @SuppressWarnings("NestedBlockDepth")
    fun convertExpressions(params: Map<String, String>): Set<String> {
        return if (isOldExpress()) {
            if (expression != null) {
                setOf(convertExpression(expression))
            } else {
                setOf()
            }
        } else {
            val expressions = mutableSetOf<String>()
            if (newExpression != null && newExpression.isNotEmpty()) {
                newExpression.forEach { expression ->
                    expressions.add(convertExpression(checkAndSetSecond(expression)))
                }
            }
            if (advanceExpression != null && advanceExpression.isNotEmpty()) {
                advanceExpression.forEach { expression ->
                    EnvUtils.parseEnv(command = expression, data = params)
                        .split("\n")
                        .forEach { expr ->
                            expressions.add(convertExpression(expr))
                        }
                }
            }
            expressions
        }
    }

    private fun checkAndSetSecond(expression: String): String {
        val newExpression = expression.trim()
        val expressionParts = newExpression.split(" ")
        return if (expressionParts[0] != "0") {
            val newExpressionParts = expressionParts.toMutableList()
            newExpressionParts[0] = "0"
            newExpressionParts.joinToString(separator = " ")
        } else {
            newExpression
        }
    }

    override fun findFirstTaskIdByStartType(startType: StartType): String {
        return if (startType.name == StartType.TIME_TRIGGER.name) {
            this.id!!
        } else {
            super.findFirstTaskIdByStartType(startType)
        }
    }

    fun enableRepoConfig(): Boolean {
        return repositoryType == TriggerRepositoryType.SELF ||
                repositoryType == TriggerRepositoryType.ID && !repoHashId.isNullOrBlank() ||
                repositoryType == TriggerRepositoryType.NAME && !repoName.isNullOrBlank()
    }

    /**
     * 启动参数
     */
    fun convertStartParams() = if (startParams.isNullOrBlank()) {
        null
    } else {
        try {
            JsonUtil.to(startParams, object : TypeReference<List<Map<String, Any>>>() {})
        } catch (ignored: Exception) {
            // 跳过，存在异常启动参数
            logger.warn("startParams is not a valid json, skip it: $startParams")
            listOf()
        }.filter { it.containsKey("key") }
                .associate { it["key"].toString() to (it["value"]?.toString() ?: "") }
    }
}
