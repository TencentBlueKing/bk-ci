/*
 * Tencent is pleased to support the open source community by making BK-REPO 蓝鲸制品库 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.environment.utils

import com.tencent.devops.common.environment.agent.client.BcsClient
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.pojo.enums.NodeStatus
import com.tencent.devops.environment.pojo.enums.NodeType
import com.tencent.devops.model.environment.tables.records.TNodeRecord
import org.jooq.DSLContext

object BcsVmNodeStatusUtils {

    fun updateBcsVmNodeStatus(dslContext: DSLContext, nodeDao: NodeDao, bcsClient: BcsClient, nodeRecordList: List<TNodeRecord>) {
        val abnormalNodes = nodeRecordList.filter {
            it.nodeType == NodeType.BCSVM.name && it.nodeStatus != NodeStatus.NORMAL.name
        }

        if (abnormalNodes.isEmpty()) {
            return
        }

        abnormalNodes.forEach {
            val statusAndOs = bcsClient.inspectVmList(
                clusterId = it.nodeClusterId,
                nodeNames = it.nodeName,
                namespace = it.nodeNamespace
            )
            if (statusAndOs != null) {
                it.nodeStatus = statusAndOs.nodeStatus
                it.osName = statusAndOs.osName
            }
        }

        nodeDao.batchUpdateNode(dslContext, abnormalNodes)
    }
}