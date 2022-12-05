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

package com.tencent.bkrepo.common.query.matcher

import com.tencent.bkrepo.common.api.constant.CharPool.DOT
import com.tencent.bkrepo.common.query.enums.OperationType
import com.tencent.bkrepo.common.query.exception.QueryModelException
import com.tencent.bkrepo.common.query.matcher.impl.AfterMatcher
import com.tencent.bkrepo.common.query.matcher.impl.BeforeMatcher
import com.tencent.bkrepo.common.query.matcher.impl.EqualMatcher
import com.tencent.bkrepo.common.query.matcher.impl.GreaterThanMatcher
import com.tencent.bkrepo.common.query.matcher.impl.GreaterThanOrEqualMatcher
import com.tencent.bkrepo.common.query.matcher.impl.InMatcher
import com.tencent.bkrepo.common.query.matcher.impl.LessThanMatcher
import com.tencent.bkrepo.common.query.matcher.impl.LessThanOrEqualMatcher
import com.tencent.bkrepo.common.query.matcher.impl.MatchIMatcher
import com.tencent.bkrepo.common.query.matcher.impl.MatchMatcher
import com.tencent.bkrepo.common.query.matcher.impl.NinMatcher
import com.tencent.bkrepo.common.query.matcher.impl.NotEqualMatcher
import com.tencent.bkrepo.common.query.matcher.impl.NotNullMatcher
import com.tencent.bkrepo.common.query.matcher.impl.NullMatcher
import com.tencent.bkrepo.common.query.matcher.impl.PrefixMatcher
import com.tencent.bkrepo.common.query.matcher.impl.RegexMatcher
import com.tencent.bkrepo.common.query.matcher.impl.SuffixMatcher
import com.tencent.bkrepo.common.query.model.Rule
import com.tencent.bkrepo.common.query.model.Rule.NestedRule.RelationType.AND
import com.tencent.bkrepo.common.query.model.Rule.NestedRule.RelationType.NOR
import com.tencent.bkrepo.common.query.model.Rule.NestedRule.RelationType.OR
import java.lang.reflect.Field

/**
 * 规则匹配器
 */
abstract class RuleMatcher {
    /**
     * 支持的操作类型
     */
    abstract fun supportOperationType(): OperationType

    /**
     * 是否支持[rule]的操作类型
     */
    protected open fun isSupportOperationType(rule: Rule.QueryRule): Boolean {
        return rule.operation == supportOperationType()
    }

    /**
     * 是否支持[valueToMatch]数据类型
     */
    protected open fun isSupportValueType(rule: Rule.QueryRule, valueToMatch: Any?): Boolean {
        return supportOperationType().valueType.isInstance(rule.value)
            && supportOperationType().valueType.isInstance(valueToMatch)
    }

    /**
     * 判断给定的值与规则是否匹配
     *
     * @param rule 待匹配的规则
     * @param valueToMatch 待匹配的值
     *
     * @return 是否匹配成功
     */
    fun match(rule: Rule.QueryRule, valueToMatch: Any?): Boolean {
        if (!isSupportOperationType(rule)) {
            throw IllegalArgumentException("unsupported operation, rule[$rule]")
        }

        if (!isSupportValueType(rule, valueToMatch)) {
            // 待匹配的值不存在时返回false
            val valueType = valueToMatch?.javaClass?.name ?: return false
            // 待匹配的值存在但是类型不符合要求
            throw IllegalArgumentException(
                "unsupported value type, value[$valueToMatch], type[${valueType}], " +
                    "ruleValue[${rule.value}, ruleValueType[${rule.value.javaClass.name}]]"
            )
        }

        return doMatch(rule, valueToMatch)
    }

    protected abstract fun doMatch(rule: Rule.QueryRule, valueToMatch: Any?): Boolean

    companion object {
        private val defaultQueryRuleMatcherMap = mapOf(
            OperationType.EQ to EqualMatcher(),
            OperationType.NE to NotEqualMatcher(),
            OperationType.LT to LessThanMatcher(),
            OperationType.LTE to LessThanOrEqualMatcher(),
            OperationType.GT to GreaterThanMatcher(),
            OperationType.GTE to GreaterThanOrEqualMatcher(),
            OperationType.BEFORE to BeforeMatcher(),
            OperationType.AFTER to AfterMatcher(),
            OperationType.IN to InMatcher(),
            OperationType.NIN to NinMatcher(),
            OperationType.PREFIX to PrefixMatcher(),
            OperationType.SUFFIX to SuffixMatcher(),
            OperationType.MATCH to MatchMatcher(),
            OperationType.MATCH_I to MatchIMatcher(),
            OperationType.REGEX to RegexMatcher(),
            OperationType.NULL to NullMatcher(),
            OperationType.NOT_NULL to NotNullMatcher()
        )

        /**
         * 规则匹配
         *
         * @param rule 需要匹配的规则
         * @param valuesToMatch 用于匹配的值
         * @param successOnMiss 当[rule]中指定的字段在[valuesToMatch]中不存在时是否匹配成功
         *
         * @return true 匹配成功，false 匹配失败
         */
        fun match(rule: Rule, valuesToMatch: Map<String, Any>, successOnMiss: Boolean = false): Boolean {
            return when (rule) {
                is Rule.QueryRule -> match(rule, valuesToMatch, successOnMiss)
                is Rule.FixedRule -> match(rule.wrapperRule, valuesToMatch, successOnMiss)
                is Rule.NestedRule -> {
                    when (rule.relation) {
                        AND -> matchAnd(rule.rules, valuesToMatch, successOnMiss)
                        OR -> matchOr(rule.rules, valuesToMatch, successOnMiss)
                        NOR -> matchNor(rule.rules, valuesToMatch, successOnMiss)
                    }
                }
            }
        }

        private fun match(
            rule: Rule.QueryRule,
            valuesToMatch: Map<String, Any>,
            successOnMiss: Boolean
        ): Boolean {
            val valueToMatch = getValueToMatch(rule, valuesToMatch)

            if (valueToMatch == null && successOnMiss) {
                return true
            }

            return defaultQueryRuleMatcherMap[rule.operation]?.match(rule, valueToMatch)
                ?: throw QueryModelException("Unsupported operation [${rule.operation}].")
        }

        private fun matchAnd(
            rules: List<Rule>,
            valuesToMatch: Map<String, Any>,
            successOnMiss: Boolean
        ): Boolean {
            return rules.all { match(it, valuesToMatch, successOnMiss) }
        }

        private fun matchOr(
            rules: List<Rule>,
            valuesToMatch: Map<String, Any>,
            successOnMiss: Boolean
        ): Boolean {
            if (rules.isEmpty()) {
                return true
            }

            rules.forEach {
                if (match(it, valuesToMatch, successOnMiss)) {
                    return true
                }
            }

            return false
        }

        private fun matchNor(
            rules: List<Rule>,
            valuesToMatch: Map<String, Any>,
            successOnMiss: Boolean
        ): Boolean {
            return rules.all { !match(it, valuesToMatch, successOnMiss) }
        }

        /**
         * 从[valuesToMatch]中获取[rule]指定的字段值
         */
        private fun getValueToMatch(rule: Rule.QueryRule, valuesToMatch: Map<String, Any>): Any? {
            if (rule.field.isEmpty()) {
                throw IllegalArgumentException("invalid rule[$rule]")
            }

            val fields = rule.field.split(DOT)
            var valueToMatch: Any? = valuesToMatch[fields[0]]
            for (i in 1 until fields.size) {
                if (valueToMatch == null) {
                    return null
                }

                val field = fields[i]

                valueToMatch = if (valueToMatch is Map<*, *>) {
                    valueToMatch[field]
                } else {
                    getDeclaredField(valueToMatch::class.java, field)?.let {
                        it.isAccessible = true
                        it.get(valueToMatch)
                    }
                }
            }

            return valueToMatch
        }

        /**
         * 在[clazz]与其所有父类中查找[fieldName]字段
         */
        private fun getDeclaredField(clazz: Class<*>?, fieldName: String): Field? {
            if (clazz == null) {
                return null
            }
            return try {
                clazz.getDeclaredField(fieldName)
            } catch (e: NoSuchFieldException) {
                getDeclaredField(clazz.superclass, fieldName)
            }
        }
    }
}
