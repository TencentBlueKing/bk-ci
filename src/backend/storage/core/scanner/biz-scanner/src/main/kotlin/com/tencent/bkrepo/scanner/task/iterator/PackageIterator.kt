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

package com.tencent.bkrepo.scanner.task.iterator

import com.tencent.bkrepo.common.api.constant.DEFAULT_PAGE_NUMBER
import com.tencent.bkrepo.common.api.constant.DEFAULT_PAGE_SIZE
import com.tencent.bkrepo.common.api.exception.NotFoundException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.query.enums.OperationType
import com.tencent.bkrepo.common.query.model.PageLimit
import com.tencent.bkrepo.common.query.model.QueryModel
import com.tencent.bkrepo.common.query.model.Rule
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.api.PackageClient
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.packages.PackageSummary
import com.tencent.bkrepo.scanner.pojo.Node
import com.tencent.bkrepo.scanner.pojo.rule.RuleArtifact
import com.tencent.bkrepo.scanner.utils.Request
import com.tencent.bkrepo.scanner.utils.RuleMatcher
import kotlin.math.min

/**
 * 依赖包迭代器
 */
class PackageIterator(
    private val packageClient: PackageClient,
    private val nodeClient: NodeClient,
    override val position: PackageIteratePosition
) : PageableIterator<Node>() {
    override fun nextPageData(page: Int, pageSize: Int): List<Node> {
        if (position.packages.isEmpty() || position.packageIndex >= position.packages.size - 1) {
            position.packages = requestPackage(page, pageSize)
            position.packageIndex = INITIAL_INDEX
        }

        if (position.packages.isEmpty()) {
            return emptyList()
        }

        // packages填充版本后列表大小会超过pageSize，需要拆分列表，剩余的packages留在下次遍历
        val fromIndex = position.packageIndex + 1
        val toIndex = min(position.packages.size, position.packageIndex + position.pageSize + 1)
        val populatedPackages = position.packages
            .subList(fromIndex, toIndex)
            .asSequence()
            .map { pkg -> populatePackage(pkg) }
            .filter { it.fullPath != null }
            .toList()
        position.packageIndex = position.packageIndex + (toIndex - fromIndex)
        return requestNode(populatedPackages)
    }

    /**
     * 请求package数据
     */
    private fun requestPackage(page: Int, pageSize: Int): List<Package> {
        // 拉取待扫描的package
        val packageQueryModel = QueryModel(
            PageLimit(page, pageSize), null, packageSelect, packageSummaryRule(position.rule)
        )

        val records = Request.request { packageClient.searchPackage(packageQueryModel) }!!.records
        return if (records.isEmpty()) {
            emptyList()
        } else {
            records.flatMap {
                val pkg = parse(it)
                // 获取与rule匹配的版本
                pkg.historyVersion
                    .ifEmpty { listOf(pkg.latestVersion) }
                    .asSequence()
                    .filter { version ->
                        RuleMatcher.nameVersionMatch(pkg.artifactName, version, position.rule as Rule.NestedRule)
                    }
                    .map { version -> pkg.copy(packageVersion = version) }
                    .toList()
            }
        }
    }

    /**
     * 解析package数据
     */
    @Suppress("UNCHECKED_CAST")
    private fun parse(packageSummary: Map<*, *>) = Package(
        projectId = packageSummary[PackageSummary::projectId.name] as String,
        repoName = packageSummary[PackageSummary::repoName.name] as String,
        artifactName = packageSummary[PackageSummary::name.name] as String,
        packageKey = packageSummary[PackageSummary::key.name] as String,
        latestVersion = packageSummary[PackageSummary::latest.name] as String,
        historyVersion = packageSummary[PackageSummary::historyVersion.name] as List<String>? ?: emptyList()
    )

    /**
     * 获取packageSummary查询规则
     *
     * @param rule 初始规则
     *
     * @return 过滤了version字段规则后的rule
     */
    private fun packageSummaryRule(rule: Rule): Rule {
        require(rule is Rule.NestedRule && rule.relation == Rule.NestedRule.RelationType.AND)
        return filterAndNestedRuleField(rule, listOf(RuleArtifact::version.name))
    }

    /**
     * 过滤AND类型的NestedRule中[fields]相关的rule
     */
    private fun filterAndNestedRuleField(rule: Rule.NestedRule, fields: List<String>): Rule.NestedRule {
        require(rule.relation == Rule.NestedRule.RelationType.AND)
        val rules = ArrayList<Rule>(rule.rules.size)
        rule.rules.forEach {
            if (it is Rule.QueryRule && it.field !in fields) {
                rules.add(it)
            }

            if (it is Rule.NestedRule && it.relation == Rule.NestedRule.RelationType.AND) {
                rules.addAll(filterAndNestedRuleField(it, fields).rules)
            }

            if (it is Rule.NestedRule && it.relation == Rule.NestedRule.RelationType.OR) {
                val filteredRule = filterOrNestedRuleField(it, fields)
                if (filteredRule.rules.isNotEmpty()) {
                    rules.add(filteredRule)
                }
            }
        }

        return Rule.NestedRule(rules, Rule.NestedRule.RelationType.AND)
    }

    /**
     * 过滤OR类型的NestedRule中[fields]相关的rule
     */
    private fun filterOrNestedRuleField(rule: Rule.NestedRule, fields: List<String>): Rule.NestedRule {
        require(rule.relation == Rule.NestedRule.RelationType.OR)
        val rules = ArrayList<Rule>(rule.rules.size)

        rule.rules.forEach {
            if (it is Rule.QueryRule && it.field !in fields) {
                rules.add(it)
            }

            if (it is Rule.NestedRule && it.relation == Rule.NestedRule.RelationType.AND) {
                val filteredRule = filterAndNestedRuleField(it, fields)
                if (filteredRule.rules.isNotEmpty()) {
                    rules.add(filteredRule)
                }
            }

            if (it is Rule.NestedRule && it.relation == Rule.NestedRule.RelationType.OR) {
                rules.addAll(filterOrNestedRuleField(it, fields).rules)
            }
        }

        return Rule.NestedRule(rules, Rule.NestedRule.RelationType.OR)
    }

    /**
     * 填充包的fullPath字段
     * TODO 实现批量获取package完整数据接口后移除此方法
     */
    private fun populatePackage(pkg: Package): Package {
        require(pkg.packageVersion != null)
        with(pkg) {
            val packageVersion = Request.request {
                packageClient.findVersionByName(projectId, repoName, packageKey, packageVersion!!)
            } ?: throw NotFoundException(CommonMessageCode.RESOURCE_NOT_FOUND, packageKey, packageVersion!!)
            pkg.fullPath = packageVersion.contentPath
        }
        return pkg
    }

    /**
     * 请求[packages]对应的node数据
     */
    private fun requestNode(packages: List<Package>): List<Node> {
        if (packages.isEmpty()) {
            return emptyList()
        }
        val nodeQueryRule = nodeQueryRule(packages)
        val nodes = Request.requestNodes(nodeClient, nodeQueryRule, DEFAULT_PAGE_NUMBER, position.pageSize)
        val packageMap = packages.associateBy { it.fullPath }
        nodes.forEach {
            val pkg = packageMap[it.fullPath]!!
            it.packageKey = pkg.packageKey
            it.packageVersion = pkg.packageVersion
        }
        return nodes
    }

    /**
     * 获取查询[packages]对应node信息的rule
     */
    private fun nodeQueryRule(packages: List<Package>): Rule {
        require(packages.isNotEmpty())
        val projectIdRule = (position.rule as Rule.NestedRule)
            .rules
            .first { it is Rule.QueryRule && it.field == NodeDetail::projectId.name }

        val rules = mutableListOf(projectIdRule)
        val packagesRules = packages.map {
            val packageRules = mutableListOf<Rule>(
                Rule.QueryRule(NodeDetail::repoName.name, it.repoName, OperationType.EQ),
                Rule.QueryRule(NodeDetail::fullPath.name, it.fullPath!!, OperationType.EQ)
            )
            Rule.NestedRule(packageRules, Rule.NestedRule.RelationType.AND)
        }
        rules.add(Rule.NestedRule(packagesRules.toMutableList(), Rule.NestedRule.RelationType.OR))

        return Rule.NestedRule(rules, Rule.NestedRule.RelationType.AND)
    }

    companion object {
        private val packageSelect = listOf(
            PackageSummary::projectId.name,
            PackageSummary::repoName.name,
            PackageSummary::key.name,
            PackageSummary::name.name,
            PackageSummary::latest.name,
            PackageSummary::historyVersion.name
        )
    }

    data class Package(
        val projectId: String,
        val repoName: String,
        val artifactName: String,
        val packageKey: String,
        val latestVersion: String,
        val historyVersion: List<String> = emptyList(),
        var packageVersion: String? = null,
        var fullPath: String? = null
    )

    /**
     * 当前遍历到的位置
     */
    data class PackageIteratePosition(
        /**
         * 需要遍历的依赖包匹配规则
         * rule结构如下
         * {
         *     "rules": [
         *         {
         *            "field": "projectId",
         *            "value": "test",
         *            "operation": "EQ"
         *         },
         *         {
         *            "field": "repoName",
         *            "value": "test",
         *            "operation": "EQ"
         *         },
         *         {
         *             "rules": [
         *                 {
         *                     "field": "name",
         *                     "value": "test",
         *                     "operation": "EQ"
         *                 },
         *                 {
         *                     rules: [
         *                         {
         *                             "field": "name",
         *                             "value": "test",
         *                             "operation": "EQ"
         *                         },
         *                         {
         *                             "field": "version",
         *                             "value": "test",
         *                             "operation": "EQ"
         *                         },
         *                     ],
         *                     relation: "AND"
         *                 }
         *             ],
         *             "relation": "OR"
         *         }
         *     ],
         *     "relation": "AND"
         * }
         */
        val rule: Rule,
        var packages: List<Package> = emptyList(),
        var packageIndex: Int = INITIAL_INDEX,
        override var page: Int = INITIAL_PAGE,
        override var pageSize: Int = DEFAULT_PAGE_SIZE,
        override var index: Int = INITIAL_INDEX
    ) : PageIteratePosition(page = page, pageSize = pageSize, index = index)
}
