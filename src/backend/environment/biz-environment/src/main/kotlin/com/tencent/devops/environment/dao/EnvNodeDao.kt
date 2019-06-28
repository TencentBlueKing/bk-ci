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

package com.tencent.devops.environment.dao

import com.tencent.devops.model.environment.tables.TEnvNode
import com.tencent.devops.model.environment.tables.records.TEnvNodeRecord
import org.jooq.DSLContext
import org.jooq.Record2
import org.jooq.impl.DSL.count
import org.springframework.stereotype.Repository

@Repository
class EnvNodeDao {
    fun list(dslContext: DSLContext, projectId: String, envIds: List<Long>): List<TEnvNodeRecord> {
        with(TEnvNode.T_ENV_NODE) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(ENV_ID.`in`(envIds))
                .orderBy(NODE_ID.desc())
                .fetch()
        }
    }

    fun count(dslContext: DSLContext, projectId: String, envId: Long): Int {
        with(TEnvNode.T_ENV_NODE) {
            return dslContext.selectCount()
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(ENV_ID.eq(envId))
                .fetchOne(0, Int::class.java)
        }
    }

    fun batchCount(dslContext: DSLContext, projectId: String, envIds: List<Long>): List<Record2<Long, Int>> {
        with(TEnvNode.T_ENV_NODE) {
            return dslContext.select(ENV_ID, count())
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .groupBy(ENV_ID)
                .fetch()
        }
    }

    fun batchStoreEnvNode(dslContext: DSLContext, envNodeList: List<TEnvNodeRecord>) {
        if (envNodeList.isEmpty()) {
            return
        }
        dslContext.batchStore(envNodeList).execute()
    }

    fun batchDeleteEnvNode(dslContext: DSLContext, projectId: String, envId: Long, nodeIds: List<Long>) {
        if (nodeIds.isEmpty()) {
            return
        }

        with(TEnvNode.T_ENV_NODE) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(ENV_ID.eq(envId))
                .and(NODE_ID.`in`(nodeIds))
                .execute()
        }
    }

    fun deleteByNodeIds(dslContext: DSLContext, nodeIds: List<Long>) {
        if (nodeIds.isEmpty()) {
            return
        }

        with(TEnvNode.T_ENV_NODE) {
            dslContext.deleteFrom(this)
                .where(NODE_ID.`in`(nodeIds))
                .execute()
        }
    }
}