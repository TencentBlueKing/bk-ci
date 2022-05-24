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

import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.query.enums.OperationType
import com.tencent.bkrepo.common.query.model.Rule
import com.tencent.bkrepo.common.query.model.Rule.NestedRule
import com.tencent.bkrepo.common.query.model.Rule.NestedRule.RelationType.AND
import com.tencent.bkrepo.common.query.model.Rule.NestedRule.RelationType.OR
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.packages.PackageSummary
import com.tencent.bkrepo.scanner.pojo.rule.ArtifactRule
import com.tencent.bkrepo.scanner.pojo.rule.RuleArtifact
import com.tencent.bkrepo.scanner.pojo.rule.RuleType

object RuleConverter {

    fun convert(projectId: String, repoNames: List<String>, rules: List<ArtifactRule>, repoType: String? = null): Rule {
        val rule = createProjectIdAdnRepoRule(projectId, repoNames, repoType)

        if (rules.isEmpty()) {
            return rule
        } else if (rules.size == 1) {
            val nameAndVersionRule = rule(rules[0])
            if (nameAndVersionRule is NestedRule) {
                rule.rules.addAll(nameAndVersionRule.rules)
            } else {
                rule.rules.add(nameAndVersionRule)
            }
        } else {
            rules
                .asSequence()
                .filter { it.versionRule != null || it.nameRule != null }
                .map { artifactRule -> rule(artifactRule) }
                .let { rule.rules.add(NestedRule(it.toMutableList(), OR)) }
        }

        return rule
    }

    fun convert(rule: Rule): List<ArtifactRule> {
        require(rule is NestedRule)

        rule.rules.forEach { innerRule ->
            val isArtifactRule = isArtifactRule(innerRule)

            if (isArtifactRule && innerRule is Rule.QueryRule) {
                return listOf(artifactRule(innerRule))
            }

            if (isArtifactRule && innerRule is NestedRule && innerRule.relation == AND) {
                return listOf(artifactRule(innerRule))
            }

            if (isArtifactRule && innerRule is NestedRule && innerRule.relation == OR) {
                return innerRule.rules.map { artifactRule(it) }
            }
        }

        return emptyList()
    }

    fun convert(projectId: String, repoName: String, fullPath: String): Rule {
        val rule = createProjectIdAdnRepoRule(projectId, listOf(repoName))
        rule.rules.add(Rule.QueryRule(NodeDetail::fullPath.name, fullPath, OperationType.EQ))
        return rule
    }

    fun convert(projectId: String, repoName: String, packageKey: String, version: String): Rule {
        val rule = createProjectIdAdnRepoRule(projectId, listOf(repoName))
        rule.rules.add(Rule.QueryRule(PackageSummary::key.name, packageKey, OperationType.EQ))
        rule.rules.add(Rule.QueryRule(RuleArtifact::version.name, version, OperationType.EQ))
        return rule
    }

    private fun rule(artifactRule: ArtifactRule): Rule {
        val nameRule = artifactRule.nameRule?.let { convertRule(RuleArtifact::name.name, it) }
        val versionRule = artifactRule.versionRule?.let { convertRule(RuleArtifact::version.name, it) }
        return rule(nameRule, versionRule)
    }

    private fun convertRule(field: String, rule: com.tencent.bkrepo.scanner.pojo.rule.Rule): Rule.QueryRule {
        return when (rule.type) {
            RuleType.EQ -> Rule.QueryRule(field, rule.value, OperationType.EQ)
            RuleType.IN -> Rule.QueryRule(field, "*${rule.value}*", OperationType.MATCH)
            RuleType.REGEX -> Rule.QueryRule(field, rule.value, OperationType.REGEX)
        }
    }

    /**
     * 添加projectId和repoName规则
     */
    private fun createProjectIdAdnRepoRule(
        projectId: String,
        repoNames: List<String>,
        repoType: String? = null
    ): NestedRule {
        val rules = mutableListOf<Rule>(
            Rule.QueryRule(NodeDetail::projectId.name, projectId, OperationType.EQ)
        )

        if (repoType != null && repoType != RepositoryType.GENERIC.name) {
            rules.add(Rule.QueryRule(PackageSummary::type.name, repoType, OperationType.EQ))
        }

        if (repoNames.isNotEmpty()) {
            rules.add(Rule.QueryRule(NodeDetail::repoName.name, repoNames, OperationType.IN))
        }

        return NestedRule(rules, AND)
    }

    private fun rule(nameRule: Rule?, versionRule: Rule?): Rule {
        require(nameRule != null || versionRule != null)
        if (nameRule == null) {
            return versionRule!!
        }

        if (versionRule == null) {
            return nameRule
        }

        return NestedRule(mutableListOf(nameRule, versionRule), AND)
    }

    private fun isArtifactRule(rule: Rule): Boolean {
        with(rule) {
            // 只存在一个artifactRule，version和name只存在一种
            if (this is Rule.QueryRule) {
                return field == RuleArtifact::name.name || field == RuleArtifact::version.name
            }

            // 只存在一个artifactRule，version和name两种rule都存在
            if (this is NestedRule && this.relation == AND) {
                val artifactRule = artifactRule(this)
                return artifactRule.nameRule != null && artifactRule.versionRule != null
            }

            // 存在多个artifactRule的情况
            if (this is NestedRule && this.relation == OR) {
                val artifactRule = artifactRule(this.rules.first())
                return artifactRule.nameRule != null || artifactRule.versionRule != null
            }

            return false
        }
    }

    private fun artifactRule(rule: Rule): ArtifactRule {
        require(rule is Rule.QueryRule || rule is NestedRule && rule.relation == AND)

        return ArtifactRule(
            findRuleFrom(rule, RuleArtifact::name.name),
            findRuleFrom(rule, RuleArtifact::version.name)
        )
    }

    private fun findRuleFrom(rule: Rule, filed: String): com.tencent.bkrepo.scanner.pojo.rule.Rule? {
        require(rule is Rule.QueryRule || rule is NestedRule && rule.relation == AND)

        return if (rule is Rule.QueryRule && rule.field == filed) {
            convertRule(rule)
        } else if (rule is NestedRule && rule.relation == AND) {
            rule.rules
                .firstOrNull { it is Rule.QueryRule && it.field == filed }
                ?.let { convertRule(it) }
        } else {
            null
        }
    }

    private fun convertRule(rule: Rule): com.tencent.bkrepo.scanner.pojo.rule.Rule {
        require(rule is Rule.QueryRule)

        val value = rule.value.toString()
        val ruleValue = if (rule.operation == OperationType.MATCH && value.length > 2) {
            // MATCH匹配规则的value为‘*someValue*’,需要移除头尾的'*'
            value.substring(1, value.length - 1)
        } else {
            value
        }

        return com.tencent.bkrepo.scanner.pojo.rule.Rule(
            convertRuleOperationType(rule.operation),
            ruleValue
        )
    }

    private fun convertRuleOperationType(type: OperationType): RuleType {
        return when (type) {
            OperationType.EQ -> RuleType.EQ
            OperationType.REGEX -> RuleType.REGEX
            OperationType.MATCH -> RuleType.IN
            else -> throw ErrorCodeException(CommonMessageCode.PARAMETER_INVALID, type)
        }
    }
}
