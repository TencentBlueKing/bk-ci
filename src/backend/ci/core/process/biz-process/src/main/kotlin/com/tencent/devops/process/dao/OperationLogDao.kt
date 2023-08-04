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

package com.tencent.devops.process.dao

import com.tencent.devops.model.process.tables.TPipelineOperationLog
import com.tencent.devops.process.enums.OperationLogType
import com.tencent.devops.process.pojo.PipelineOperationLog
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
@Suppress("LongParameterList")
class OperationLogDao {

    fun add(
        dslContext: DSLContext,
        id: Int,
        projectId: String,
        pipelineId: String,
        operator: String,
        operationLogType: OperationLogType,
        description: String?
    ) {
        with(TPipelineOperationLog.T_PIPELINE_OPERATION_LOG) {
            dslContext.insertInto(
                this,
                ID,
                PROJECT_ID,
                PIPELINE_ID,
                OPERATOR,
                OPERATION_TYPE,
                PARAMS,
                DESCRIPTION
            ).values(
                id,
                projectId,
                pipelineId,
                operator,
                operator,
                operationLogType.name,
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
                    OPERATOR,
                    OPERATION_TYPE,
                    PARAMS,
                    DESCRIPTION
                )
                    .values(
                        it.id,
                        it.pipelineId,
                        it.pipelineId,
                        it.operator,
                        it.operationLogType.name,
                        it.params,
                        it.description
                    )
            }
            dslContext.batch(addStep).execute()
        }
    }
}
