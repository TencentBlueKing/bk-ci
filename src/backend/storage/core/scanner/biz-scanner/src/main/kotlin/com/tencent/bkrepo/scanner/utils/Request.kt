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

import com.tencent.bkrepo.common.api.exception.SystemErrorException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.mongo.dao.AbstractMongoDao
import com.tencent.bkrepo.common.query.model.PageLimit
import com.tencent.bkrepo.common.query.model.QueryModel
import com.tencent.bkrepo.common.query.model.Rule
import com.tencent.bkrepo.common.query.model.Sort
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.scanner.pojo.Node
import java.lang.ClassCastException

object Request {
    private val nodeSelected = listOf(
        NodeDetail::sha256.name,
        NodeDetail::size.name,
        NodeDetail::fullPath.name,
        NodeDetail::projectId.name,
        NodeDetail::repoName.name,
        NodeDetail::name.name
    )

    /**
     * 发起请求返回[Response]的data字段，并在请求失败时抛出[SystemErrorException]异常
     */
    fun <R> request(reqAction: () -> Response<R>): R? {
        val res = reqAction.invoke()
        if (res.isNotOk()) {
            throw SystemErrorException(CommonMessageCode.SYSTEM_ERROR, res.message ?: "")
        }
        return res.data
    }

    /**
     * 请求node数据并解析成[Node]
     */
    fun requestNodes(nodeClient: NodeClient, rule: Rule, page: Int, pageSize: Int): List<Node> {
        val queryModel = QueryModel(
            PageLimit(page, pageSize),
            Sort(listOf(AbstractMongoDao.ID), Sort.Direction.ASC),
            nodeSelected,
            rule
        )
        return request { nodeClient.search(queryModel) }!!.records.map {
            val projectId = it[NodeDetail::projectId.name]!! as String
            val repoName = it[NodeDetail::repoName.name]!! as String
            val sha256 = it[NodeDetail::sha256.name]!! as String
            val size = toLong(it[NodeDetail::size.name]!!)
            val fullPath = it[NodeDetail::fullPath.name]!! as String
            val name = it[NodeDetail::name.name]!! as String
            Node(projectId, repoName, fullPath, name, null, null, sha256, size)
        }
    }

    private fun toLong(value: Any): Long {
        return when (value) {
            is Int -> value.toLong()
            is Long -> value
            else -> throw ClassCastException()
        }
    }
}
