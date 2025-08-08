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

package com.tencent.devops.process.engine.dao

import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.model.process.tables.TPipelineOperationLog
import com.tencent.devops.model.process.tables.records.TPipelineOperationLogRecord
import com.tencent.devops.process.enums.OperationLogType
import com.tencent.devops.process.pojo.PipelineOperationLog
import org.jooq.DSLContext
import org.jooq.RecordMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository

@Repository
@Suppress("LongParameterList")
class PipelineOperationLogDao {

    fun add(
        dslContext: DSLContext,
        id: Long?,
        projectId: String,
        pipelineId: String,
        version: Int,
        operator: String,
        operationLogType: OperationLogType,
        params: String,
        description: String?
    ) {
        with(TPipelineOperationLog.T_PIPELINE_OPERATION_LOG) {
            dslContext.insertInto(
                this,
                ID,
                PROJECT_ID,
                PIPELINE_ID,
                VERSION,
                OPERATOR,
                OPERATION_TYPE,
                PARAMS,
                DESCRIPTION
            ).values(
                id,
                projectId,
                pipelineId,
                version,
                operator,
                operationLogType.name,
                params,
                description
            ).execute()
        }
    }

    fun batchAddLogs(
        dslContext: DSLContext,
        operationLogList: List<PipelineOperationLog>
    ) {
        with(TPipelineOperationLog.T_PIPELINE_OPERATION_LOG) {
            val addStep = operationLogList.map {
                dslContext.insertInto(
                    this,
                    ID,
                    PROJECT_ID,
                    PIPELINE_ID,
                    VERSION,
                    OPERATOR,
                    OPERATION_TYPE,
                    PARAMS,
                    DESCRIPTION
                ).values(
                    it.id,
                    it.pipelineId,
                    it.pipelineId,
                    it.version,
                    it.operator,
                    it.operationLogType.name,
                    it.params,
                    it.description
                )
            }
            dslContext.batch(addStep).execute()
        }
    }

    fun getCountByPipeline(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        creator: String?
    ): Int {
        return with(TPipelineOperationLog.T_PIPELINE_OPERATION_LOG) {
            val select = dslContext.selectCount().from(this)
                .where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId)))
            creator?.let { select.and(OPERATOR.like("%$creator%")) }
            select.fetchOne(0, Int::class.java)!!
        }
    }

    fun getListByPipeline(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        creator: String?,
        offset: Int,
        limit: Int
    ): List<PipelineOperationLog> {
        return with(TPipelineOperationLog.T_PIPELINE_OPERATION_LOG) {
            val select = dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId)))
            creator?.let { select.and(OPERATOR.like("%$creator%")) }
            select.orderBy(CREATE_TIME.desc())
                .limit(limit).offset(offset)
                .fetch(mapper)
        }
    }

    fun countOperator(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String
    ): Int {
        with(TPipelineOperationLog.T_PIPELINE_OPERATION_LOG) {
            val query = dslContext.selectDistinct(OPERATOR)
                .from(this)
                .where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId)))
            return query.count()
        }
    }

    fun getOperatorList(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String
    ): List<String> {
        with(TPipelineOperationLog.T_PIPELINE_OPERATION_LOG) {
            return dslContext.selectDistinct(OPERATOR)
                .from(this)
                .where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId)))
                .fetch().map { it.component1() }
        }
    }

    class PipelineOperationLogJooqMapper : RecordMapper<TPipelineOperationLogRecord, PipelineOperationLog> {
        override fun map(record: TPipelineOperationLogRecord?): PipelineOperationLog? {
            return record?.run {
                PipelineOperationLog(
                    id = id,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    version = version,
                    operator = operator,
                    operationLogType = OperationLogType.parseType(operationType),
                    params = params,
                    description = description,
                    operateTime = createTime.timestampmilli()
                )
            }
        }
    }

    companion object {
        private val mapper = PipelineOperationLogJooqMapper()
        private val logger = LoggerFactory.getLogger(PipelineOperationLogDao::class.java)
    }
}
