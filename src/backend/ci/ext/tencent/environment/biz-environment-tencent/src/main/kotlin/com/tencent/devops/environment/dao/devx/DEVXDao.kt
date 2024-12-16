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

package com.tencent.devops.environment.dao.devx

import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.environment.pojo.enums.NodeStatus
import com.tencent.devops.environment.pojo.enums.TXEnvType
import com.tencent.devops.environment.pojo.enums.TXNodeType
import com.tencent.devops.model.environment.tables.TEnv
import com.tencent.devops.model.environment.tables.TNode
import com.tencent.devops.model.environment.tables.records.TEnvRecord
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

@Repository
class DEVXDao {
    fun addNode(
        dslContext: DSLContext,
        projectId: String,
        ip: String,
        name: String,
        userId: String,
        size: String
    ): Long {
        var nodeId = 0L
        with(TNode.T_NODE) {
            dslContext.transaction { configuration ->
                val transactionContext = DSL.using(configuration)
                nodeId = transactionContext.insertInto(
                    this,
                    PROJECT_ID,
                    NODE_IP,
                    DISPLAY_NAME,
                    NODE_NAME,
                    OS_NAME,
                    NODE_STATUS,
                    NODE_TYPE,
                    CREATED_USER,
                    CREATED_TIME,
                    LAST_MODIFY_USER,
                    LAST_MODIFY_TIME,
                    SIZE
                )
                    .values(
                        projectId,
                        ip,
                        name,
                        name,
                        OS.WINDOWS.name,
                        NodeStatus.NORMAL.name,
                        TXNodeType.DEVX.nodeType().name,
                        userId,
                        LocalDateTime.now(),
                        userId,
                        LocalDateTime.now(),
                        size
                    )
                    .returning(NODE_ID)
                    .fetchOne()!!.nodeId
                val hashId = HashUtil.encodeLongId(nodeId)
                transactionContext.update(this)
                    .set(NODE_HASH_ID, hashId)
                    .where(NODE_ID.eq(nodeId))
                    .execute()
            }
        }
        return nodeId
    }

    fun listEnv(dslContext: DSLContext, projectId: Set<String>): List<TEnvRecord> {
        with(TEnv.T_ENV) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.`in`(projectId))
                .and(IS_DELETED.eq(false))
                .and(ENV_TYPE.eq(TXEnvType.DEVX.name))
                .orderBy(ENV_ID.desc())
                .fetch()
        }
    }
}
