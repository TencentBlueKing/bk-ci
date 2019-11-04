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

package com.tencent.devops.environment.service.node

import com.tencent.devops.common.environment.agent.client.BcsClient
import com.tencent.devops.common.environment.agent.pojo.agent.BcsVmNode
import com.tencent.devops.environment.pojo.enums.NodeType
import com.tencent.devops.model.environment.tables.records.TNodeRecord
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class DeleteBcsNodeActionImpl @Autowired constructor(private val bcsClient: BcsClient) : NodeAction {

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(DeleteBcsNodeActionImpl::class.java)!!
    }

    override fun type(): NodeActionFactory.Action {
        return NodeActionFactory.Action.DELETE
    }

    override fun action(nodeRecords: List<TNodeRecord>) {
        // 回收 BCSVM 机器
        val bcsVmNodeList = nodeRecords.filter { NodeType.BCSVM.name == it.nodeType }.map {
            BcsVmNode(it.nodeName, it.nodeClusterId, it.nodeNamespace, "", "", "")
        }

        if (bcsVmNodeList.isNotEmpty()) {
            try {
                bcsClient.deleteVm(bcsVmNodeList)
            } catch (e: Exception) {
                logger.error("delete bcs VM failed", e)
            }
        }
    }
}