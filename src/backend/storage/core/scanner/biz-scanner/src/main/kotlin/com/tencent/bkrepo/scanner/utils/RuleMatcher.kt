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

package com.tencent.bkrepo.scanner.utils

import com.tencent.bkrepo.common.api.util.readJsonString
import com.tencent.bkrepo.common.query.enums.OperationType
import com.tencent.bkrepo.common.query.model.Rule
import com.tencent.bkrepo.common.query.util.MongoEscapeUtils
import com.tencent.bkrepo.scanner.model.TScanPlan
import com.tencent.bkrepo.scanner.pojo.request.MatchPlanSingleScanRequest
import com.tencent.bkrepo.scanner.pojo.rule.RuleArtifact
import java.io.File

object RuleMatcher {
    fun match(request: MatchPlanSingleScanRequest, plan: TScanPlan): Boolean {
        return with(request) {
            if (fullPath != null) {
                matchFullPath(fullPath!!, plan.rule.readJsonString())
            } else {
                nameVersionMatch(packageName!!, version!!, plan.rule.readJsonString())
            }
        }
    }

    private fun matchFullPath(fullPath: String, rule: Rule): Boolean {
        require(rule is Rule.NestedRule)
        val name = File(fullPath).name
        return nameVersionMatch(name, null, rule)
    }

    /**
     * 判断制品[name]和[version]是否匹配[rule]
     *
     * @param name 制品名
     * @param version 制品版本
     * @param rule 用于匹配的规则
     *
     * @return true 匹配， false 不匹配， [rule]中没有限制[name]和[version]相关规则时候返回true
     */
    fun nameVersionMatch(name: String?, version: String?, rule: Rule.NestedRule): Boolean {
        if (rule.relation == Rule.NestedRule.RelationType.AND && rule.rules.isNotEmpty()) {
            return matchAnd(name, version, rule)
        }

        if (rule.relation == Rule.NestedRule.RelationType.OR && rule.rules.isNotEmpty()) {
            return rule.rules.any { it is Rule.NestedRule && nameVersionMatch(name, version, it) }
        }

        return true
    }

    private fun matchAnd(name: String?, version: String?, rule: Rule.NestedRule): Boolean {
        require(rule.relation == Rule.NestedRule.RelationType.AND)

        // 获取nameRule
        val nameRule = getQueryRule(rule, RuleArtifact::name.name)

        // 获取versionRule
        val versionRule = getQueryRule(rule, RuleArtifact::version.name)

        // nameRule或versionRule不存在的时候才继续遍历
        if (nameRule == null && versionRule == null) {
            return rule.rules
                .asSequence()
                .filterIsInstance<Rule.NestedRule>()
                .all { nameVersionMatch(name, version, it) }
        }

        val nameMatchFailed = name == null && nameRule != null
        val versionMatchFailed = version == null && versionRule != null
        if (nameMatchFailed || versionMatchFailed) {
            return false
        }


        // 制品名匹配
        return if (name != null && nameRule != null) {
            val matched = match(name, nameRule)
            // 制品名不匹配时直接返回匹配失败，制品版本与制品版本规则都存在时候才进行版本匹配，否则直接返回制品名匹配结果
            matched && (version == null || versionRule == null || match(version, versionRule))
        } else {
            // 不存在制品名或不存在制品名相关规则时，直接匹配版本
            match(version!!, versionRule!!)
        }
    }

    private fun getQueryRule(rule: Rule.NestedRule, field: String): Rule.QueryRule? {
        // 获取nameRule
        return rule.rules.firstOrNull {
            it is Rule.QueryRule && it.field == field
        } as Rule.QueryRule?
    }

    /**
     * 判断[value]是否匹配[rule]
     * 目前仅支持EQ,MATCH
     */
    private fun match(value: String, rule: Rule.QueryRule): Boolean {
        return when (rule.operation) {
            OperationType.EQ -> value == rule.value
            OperationType.MATCH -> {
                val escapedValue = MongoEscapeUtils.escapeRegexExceptWildcard(rule.value.toString())
                val regexPattern = escapedValue.replace("*", ".*")
                "^$regexPattern$".toRegex().matches(value)
            }
            else -> false
        }
    }
}
