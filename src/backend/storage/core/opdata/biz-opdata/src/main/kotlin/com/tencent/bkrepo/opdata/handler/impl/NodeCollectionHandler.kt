/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.opdata.handler.impl

import com.tencent.bkrepo.opdata.constant.OPDATA_GRAFANA_NUMBER
import com.tencent.bkrepo.opdata.constant.OPDATA_GRAFANA_STRING
import com.tencent.bkrepo.opdata.constant.OPDATA_NODE_NUM
import com.tencent.bkrepo.opdata.handler.QueryHandler
import com.tencent.bkrepo.opdata.model.NodeCollectionModel
import com.tencent.bkrepo.opdata.pojo.Columns
import com.tencent.bkrepo.opdata.pojo.QueryResult
import com.tencent.bkrepo.opdata.pojo.Target
import com.tencent.bkrepo.opdata.pojo.enums.Metrics
import org.springframework.stereotype.Component

/**
 * Node分表数据量统计
 */
@Component
class NodeCollectionHandler(
    private val nodeCollectionModel: NodeCollectionModel
) : QueryHandler {

    override val metric: Metrics get() = Metrics.NODECOLLECTION

    override fun handle(target: Target, result: MutableList<Any>) {
        val rows = mutableListOf<List<Any>>()
        val columns = mutableListOf<Columns>()
        val resultMap = nodeCollectionModel.statNodeNum()

        columns.add(Columns("collectionName", OPDATA_GRAFANA_STRING))
        columns.add(Columns(OPDATA_NODE_NUM, OPDATA_GRAFANA_NUMBER))
        resultMap.toList().forEach {
            rows.add(listOf(it.first, it.second))
        }
        val data = QueryResult(columns, rows, target.type)
        result.add(data)
    }
}
