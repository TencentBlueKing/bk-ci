/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2022 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.repository.search.common

import com.tencent.bkrepo.common.query.enums.OperationType
import com.tencent.bkrepo.common.query.interceptor.QueryContext
import com.tencent.bkrepo.common.query.interceptor.QueryRuleInterceptor
import com.tencent.bkrepo.common.query.model.Rule
import com.tencent.bkrepo.repository.model.TNode
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * 拦截TNode LocalDatetime类型的字段查询，将字符串转换为LocalDatetime
 */
@Component
class LocalDatetimeRuleInterceptor : QueryRuleInterceptor {
    override fun match(rule: Rule): Boolean {
        return rule is Rule.QueryRule && isSupportRule(rule)
    }

    @Suppress("IMPLICIT_CAST_TO_ANY", "UNCHECKED_CAST")
    override fun intercept(rule: Rule, context: QueryContext): Criteria {
        require(rule is Rule.QueryRule)

        // 已经检查过类型，强转是安全的
        val newValue = try {
            if (rule.operation == OperationType.IN || rule.operation == OperationType.NIN) {
                (rule.value as List<CharSequence>).map { LocalDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME) }
            } else {
                LocalDateTime.parse(rule.value as CharSequence, DateTimeFormatter.ISO_DATE_TIME)
            }
        } catch (e: DateTimeParseException) {
            logger.error("localDatetime parse failed, rule[$rule]", e)
            // 解析失败，兼容旧逻辑，返回原rule
            return context.interpreter.resolveRule(rule.toFixed(), context)
        }
        val newRule = rule.copy(value = newValue).toFixed()

        return context.interpreter.resolveRule(newRule, context)
    }

    /**
     * 检查是否为支持的规则
     *
     * @param rule 待检查的规则
     *
     * @return true 支持， false 不支持
     */
    private fun isSupportRule(rule: Rule.QueryRule): Boolean {
        with(rule) {
            if (field !in LOCAL_DATETIME_FIELDS || operation !in SUPPORT_OPERATIONS) {
                return false
            }

            return if (operation == OperationType.IN || operation == OperationType.NIN) {
                value is List<*> && (value as List<*>).firstOrNull() is CharSequence
            } else {
                value is CharSequence
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(LocalDatetimeRuleInterceptor::class.java)
        private val LOCAL_DATETIME_FIELDS = arrayOf(
            TNode::createdDate.name,
            TNode::lastModifiedDate.name,
            TNode::deleted.name,
            TNode::expireDate.name
        )

        private val SUPPORT_OPERATIONS = arrayOf(
            OperationType.EQ,
            OperationType.NE,
            OperationType.IN,
            OperationType.NIN,
            OperationType.BEFORE,
            OperationType.AFTER
        )
    }
}
