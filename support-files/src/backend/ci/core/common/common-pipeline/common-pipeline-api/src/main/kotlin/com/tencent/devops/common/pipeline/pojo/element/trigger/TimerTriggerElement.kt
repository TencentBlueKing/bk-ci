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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
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
import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("定时触发")
data class TimerTriggerElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "定时触发",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    // express是老的接口数据， 后面要废弃掉
    @ApiModelProperty("定时表达式", required = false)
    val expression: String?,
    @ApiModelProperty("改进后的表达式", required = false)
    val newExpression: List<String>? = null,
    @ApiModelProperty("高级定时表达式", required = false)
    val advanceExpression: List<String>? = null,
    @ApiModelProperty("源代码未更新则不触发构建", required = false)
    val noScm: Boolean? = false
) : Element(name, id, status) {
    companion object {
        const val classType = "timerTrigger"
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
        } catch (e: IllegalArgumentException) {
            // The old cron, just return it
            expression
        }
    }

    fun convertExpressions(): Set<String> {
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
                    expressions.add(convertExpression(expression))
                }
            }
            if (advanceExpression != null && advanceExpression.isNotEmpty()) {
                advanceExpression.forEach { expression ->
                    expressions.add(convertExpression(expression))
                }
            }
            expressions
        }
    }
}