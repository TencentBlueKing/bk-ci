/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
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

package com.tencent.devops.environment.utils

import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.environment.pojo.NodeBaseInfo
import com.tencent.devops.environment.pojo.enums.NodeType
import com.tencent.devops.model.environment.tables.records.TNodeRecord

object NodeStringIdUtils {
    fun getNodeStringId(it: TNodeRecord): String {
        return when (it.nodeType) {
            NodeType.CMDB.name -> "CMDB-${HashUtil.encodeLongId(it.nodeId)}-${it.nodeId}"
            NodeType.OTHER.name -> "OTHER-${HashUtil.encodeLongId(it.nodeId)}-${it.nodeId}"
            NodeType.THIRDPARTY.name -> "BUILD-${HashUtil.encodeLongId(it.nodeId)}-${it.nodeId}"
            else -> it.nodeStringId ?: ""
        }
    }

    fun getRefineDisplayName(nodeStringId: String, displayName: String): String {
        return if (displayName.isBlank()) {
            nodeStringId
        } else {
            displayName
        }
    }

    fun getNodeBaseInfo(nodeRecord: TNodeRecord): NodeBaseInfo {
        val nodeStringId = getNodeStringId(nodeRecord)
        return NodeBaseInfo(
            nodeHashId = HashUtil.encodeLongId(nodeRecord.nodeId),
            nodeId = nodeStringId,
            name = nodeRecord.nodeName,
            ip = nodeRecord.nodeIp,
            nodeStatus = nodeRecord.nodeStatus,
            agentStatus = AgentStatusUtils.getAgentStatus(nodeRecord),
            nodeType = nodeRecord.nodeType,
            osName = nodeRecord.osName,
            createdUser = nodeRecord.createdUser,
            operator = nodeRecord.operator,
            bakOperator = nodeRecord.bakOperator,
            gateway = "",
            displayName = getRefineDisplayName(nodeStringId, nodeRecord.displayName),
            envEnableNode = null,
            lastModifyTime = (nodeRecord.lastModifyTime ?: nodeRecord.createdTime).timestampmilli()
        )
    }
}
