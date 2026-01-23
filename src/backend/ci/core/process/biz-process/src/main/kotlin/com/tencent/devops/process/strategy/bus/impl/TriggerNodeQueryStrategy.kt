/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.process.strategy.bus.impl

import com.tencent.devops.common.api.pojo.IdValue
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.environment.api.ServiceNodeResource
import com.tencent.devops.environment.pojo.NodeWithPermission
import com.tencent.devops.environment.pojo.enums.NodeType
import com.tencent.devops.process.strategy.pojo.HistoryConditionQueryRequest
import com.tencent.devops.process.strategy.bus.IHistoryConditionQueryStrategy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * 触发节点查询策略
 */
@Component
class TriggerNodeQueryStrategy @Autowired(
    required = false
) constructor(
    private val client: Client
) : IHistoryConditionQueryStrategy {

    override fun query(
        request: HistoryConditionQueryRequest
    ): Page<IdValue> {
        // 调用远程接口获取节点数据
        val result: Result<Page<NodeWithPermission>> =
            client.get(ServiceNodeResource::class).fetchNodes(
                userId = request.userId,
                projectId = request.projectId,
                page = request.page,
                pageSize = request.pageSize,
                displayName = request.keyword, // displayName对应接口的keyword参数
                nodeType = NodeType.CREATE
            )
        // 处理结果
        val nodePage = result.data
        if (result.isNotOk() || nodePage == null) {
            return Page(
                page = request.page,
                pageSize = request.pageSize,
                count = 0L,
                records = emptyList()
            )
        }
        // 转换为IdValue列表
        val idValues = nodePage.records.map { node ->
            // 使用节点的agentHashId作为id，displayName作为显示值
            IdValue(
                id = node.agentHashId ?: "",
                value = node.displayName ?: node.agentHashId ?: ""
            )
        }
        return Page(
            page = nodePage.page,
            pageSize = nodePage.pageSize,
            count = nodePage.count,
            records = idValues
        )
    }
}

