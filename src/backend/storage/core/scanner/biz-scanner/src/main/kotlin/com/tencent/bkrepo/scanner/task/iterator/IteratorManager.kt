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

import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.query.enums.OperationType
import com.tencent.bkrepo.common.query.model.Rule
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.api.PackageClient
import com.tencent.bkrepo.repository.api.RepositoryClient
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.node.NodeInfo
import com.tencent.bkrepo.scanner.pojo.Node
import com.tencent.bkrepo.scanner.pojo.ScanPlan
import com.tencent.bkrepo.scanner.pojo.ScanTask
import com.tencent.bkrepo.scanner.utils.Request
import org.springframework.stereotype.Component

/**
 * 迭代器管理器
 */
@Component
class IteratorManager(
    private val nodeClient: NodeClient,
    private val repositoryClient: RepositoryClient,
    private val packageClient: PackageClient
) {
    /**
     * 创建待扫描文件迭代器
     *
     * @param scanTask 扫描任务
     * @param resume 是否从之前的扫描进度恢复
     */
    fun createNodeIterator(scanTask: ScanTask, resume: Boolean = false): Iterator<Node> {
        val rule = scanTask.scanPlan
            ?.let { modifyRule(it, scanTask) }
            ?: scanTask.rule

        // TODO projectClient添加分页获取project接口后这边再取消rule需要projectId条件的限制
        require(rule is Rule.NestedRule)
        val projectIds = projectIdsFromRule(rule)
        val projectIdIterator = projectIds.iterator()

        return if (scanTask.scanPlan != null && scanTask.scanPlan!!.type != RepositoryType.GENERIC.name) {
            PackageIterator(packageClient, nodeClient, PackageIterator.PackageIteratePosition(rule))
        } else {
            NodeIterator(projectIdIterator, nodeClient, NodeIterator.NodeIteratePosition(rule))
        }
    }

    private fun modifyRule(scanPlan: ScanPlan, scanTask: ScanTask): Rule {
        val rule = scanTask.rule ?: scanPlan.rule!!
        if (scanPlan.type == RepositoryType.GENERIC.name) {
            if (scanPlan.repoNames.isNullOrEmpty()) {
                addRepoNames(rule as Rule.NestedRule, scanPlan.projectId!!)
            }
            return addMobilePackageRule(rule)
        }
        return rule
    }

    /**
     * 添加ipa和apk文件过滤规则，不放到ScanPlan中，文件名后缀限制可能被移除或修改
     */
    private fun addMobilePackageRule(rule: Rule): Rule {
        val mobilePackageRule = Rule.NestedRule(
            mutableListOf(
                Rule.QueryRule(NodeDetail::fullPath.name, ".apk", OperationType.SUFFIX),
                Rule.QueryRule(NodeDetail::fullPath.name, ".apks", OperationType.SUFFIX),
                Rule.QueryRule(NodeDetail::fullPath.name, ".aab", OperationType.SUFFIX),
                Rule.QueryRule(NodeDetail::fullPath.name, ".exe", OperationType.SUFFIX),
                Rule.QueryRule(NodeDetail::fullPath.name, ".so", OperationType.SUFFIX),
                Rule.QueryRule(NodeDetail::fullPath.name, ".ipa", OperationType.SUFFIX),
                Rule.QueryRule(NodeDetail::fullPath.name, ".dmg", OperationType.SUFFIX),
                Rule.QueryRule(NodeDetail::fullPath.name, ".jar", OperationType.SUFFIX)
            ),
            Rule.NestedRule.RelationType.OR
        )
        if (rule is Rule.NestedRule && rule.relation == Rule.NestedRule.RelationType.AND) {
            rule.rules.add(mobilePackageRule)
            return rule
        }
        return Rule.NestedRule(mutableListOf(rule, mobilePackageRule), Rule.NestedRule.RelationType.AND)
    }

    /**
     * 如果指定要扫描的projectId，必须relation为AND，在nestedRule里面的第一层rule包含projectId的匹配条件
     */
    @Suppress("UNCHECKED_CAST")
    private fun projectIdsFromRule(rule: Rule.NestedRule): List<String> {
        val projectIds = ArrayList<String>()
        if (rule.relation != Rule.NestedRule.RelationType.AND) {
            return emptyList()
        } else {
            rule.rules
                .asSequence()
                .filterIsInstance(Rule.QueryRule::class.java)
                .filter { it.field == NodeDetail::projectId.name }
                .forEach {
                    if (it.operation == OperationType.EQ) {
                        projectIds.add(it.value as String)
                    } else if (it.operation == OperationType.IN) {
                        projectIds.addAll(it.value as Collection<String>)
                    }
                }
        }
        return projectIds
    }

    private fun addRepoNames(rule: Rule, projectId: String): Rule {
        val repoNames = Request
            .request { repositoryClient.listRepo(projectId, null, RepositoryType.GENERIC.name) }
            ?.map { it.name }
            ?: return rule

        val repoRule = Rule.QueryRule(NodeInfo::repoName.name, repoNames, OperationType.IN)
        return if (rule is Rule.NestedRule && rule.relation == Rule.NestedRule.RelationType.AND) {
            rule.rules.add(repoRule)
            rule
        } else {
            Rule.NestedRule(mutableListOf(rule, repoRule))
        }
    }
}
