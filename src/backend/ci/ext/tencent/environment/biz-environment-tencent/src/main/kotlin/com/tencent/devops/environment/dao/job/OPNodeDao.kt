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

package com.tencent.devops.environment.dao.job

import com.tencent.devops.environment.pojo.enums.NodeType
import com.tencent.devops.model.environment.tables.TEnvironmentThirdpartyAgent
import com.tencent.devops.model.environment.tables.TNode
import com.tencent.devops.model.environment.tables.records.TEnvironmentThirdpartyAgentRecord
import com.tencent.devops.model.environment.tables.records.TNodeRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class OPNodeDao {

    fun listAllNodes(
        dslContext: DSLContext,
        nodeType: NodeType,
        limit: Int,
        offset: Int,
    ): Result<TNodeRecord> {
        with(TNode.T_NODE) {
            return dslContext.selectFrom(this)
                .where(NODE_TYPE.eq(nodeType.name))
                .orderBy(NODE_ID.asc())
                .limit(limit).offset(offset)
                .fetch()
        }
    }

    fun getAgentsByNodeIds(
        dslContext: DSLContext,
        nodeIds: Collection<Long>
    ): Result<TEnvironmentThirdpartyAgentRecord> {
        with(TEnvironmentThirdpartyAgent.T_ENVIRONMENT_THIRDPARTY_AGENT) {
            return dslContext.selectFrom(this)
                .where(NODE_ID.`in`(nodeIds))
                .fetch()
        }
    }

    fun updateLastBuildTime(dslContext: DSLContext, pipelineId: String, nodeId: Long) {
        with(TNode.T_NODE) {
            dslContext.update(this)
                .set(LAST_BUILD_PIPELINE_ID, pipelineId)
                .where(NODE_ID.eq(nodeId))
                .execute()
        }
    }
}
