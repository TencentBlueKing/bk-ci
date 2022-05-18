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

import com.tencent.bkrepo.common.api.constant.DEFAULT_PAGE_SIZE
import com.tencent.bkrepo.common.query.enums.OperationType
import com.tencent.bkrepo.common.query.model.Rule
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.scanner.pojo.Node
import com.tencent.bkrepo.scanner.utils.Request

/**
 * 文件迭代器
 *
 * @param projectIdIterator 用于提供需要遍历的Node所属的项目
 * @param nodeClient nodeClient
 * @param position 当前文件遍历到的位置
 */
class NodeIterator(
    private val projectIdIterator: Iterator<String>,
    private val nodeClient: NodeClient,
    override val position: NodeIteratePosition = NodeIteratePosition()
) : PageableIterator<Node>(position) {

    /**
     * 需要遍历的文件匹配规则
     */
    private val rule: Rule?

    init {
        this.rule = removeProjectIdRule(position.rule)
    }

    /**
     * 由于切换查询的projectId时page会变，因此不使用参数传入的page和pageSize
     */
    override fun nextPageData(page: Int, pageSize: Int): List<Node> {
        with(position) {
            var nodes: List<Node>
            do {
                nodes = requestNodes(projectId, this@NodeIterator.rule, this.page + 1, this.pageSize)

                // 当前project没有数据，且没有其他需要遍历的project时表示遍历完成
                if (nodes.isNotEmpty() || !projectIdIterator.hasNext()) {
                    break
                }

                // 当前project不存在需要扫描的文件，获取下一个要扫描的project
                projectId = projectIdIterator.next()
                this.page = INITIAL_PAGE
                this.index = INITIAL_INDEX
            } while (nodes.isEmpty())
            return nodes
        }
    }

    private fun requestNodes(projectId: String?, rule: Rule?, page: Int, pageSize: Int): List<Node> {
        if (projectId == null) {
            return emptyList()
        }

        val projectIdRule = modifyRule(projectId, rule)
        // 获取下一页需要扫描的文件
        return Request.requestNodes(nodeClient, projectIdRule, page, pageSize)
    }

    /**
     * 创建项目Id规则，最外层不存在projectId时候表示扫描所有
     *
     * @param projectId 设置规则匹配的项目
     * @param rule 匹配规则
     */
    private fun modifyRule(projectId: String, rule: Rule?): Rule {
        val rules = ArrayList<Rule>(2)
        rules.add(Rule.QueryRule(NodeDetail::projectId.name, projectId, OperationType.EQ))
        rules.add(Rule.QueryRule(NodeDetail::folder.name, false, OperationType.EQ))
        if (rule is Rule.NestedRule && rule.relation == Rule.NestedRule.RelationType.AND) {
            rules.addAll(rule.rules)
        } else {
            rule?.let { rules.add(it) }
        }
        return Rule.NestedRule(rules)
    }

    /**
     * 移除规则内所有和projectId有关的条件
     */
    private fun removeProjectIdRule(rule: Rule?): Rule? {
        when (rule) {
            is Rule.NestedRule -> {
                val rules = ArrayList<Rule>(rule.rules.size)
                rule.rules.forEach {
                    removeProjectIdRule(it)?.let { processedRule -> rules.add(processedRule) }
                }
                return rule.copy(rules = rules)
            }
            is Rule.QueryRule -> {
                if (rule.field == NodeDetail::projectId.name) {
                    return null
                }
                return rule
            }
            else -> {
                return rule
            }
        }
    }

    /**
     * 当前文件遍历到的位置
     */
    data class NodeIteratePosition(
        /**
         * 需要遍历的文件匹配规则，规则中的所有projectId相关条件会被移除
         */
        val rule: Rule? = null,
        /**
         * 当前正在查询的projectId
         */
        var projectId: String? = null,
        override var page: Int = INITIAL_PAGE,
        override var pageSize: Int = DEFAULT_PAGE_SIZE,
        override var index: Int = INITIAL_INDEX
    ) : PageIteratePosition(page = page, pageSize = pageSize, index = index)
}
