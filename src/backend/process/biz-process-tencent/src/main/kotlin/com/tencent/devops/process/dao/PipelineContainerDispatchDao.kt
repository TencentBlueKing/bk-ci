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

package com.tencent.devops.process.dao

import com.tencent.devops.model.process.tables.TPipelineContainerDispatch
import com.tencent.devops.model.process.tables.TPipelineInfo
import com.tencent.devops.model.process.tables.records.TPipelineContainerDispatchRecord
import com.tencent.devops.process.pojo.PipelineContainerDispatchInfo
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record1
import org.jooq.Result
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class PipelineContainerDispatchDao {

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineContainerDispatchDao::class.java)
    }

    fun exists(dslContext: DSLContext, pipelineId: String, pipelineVersion: Int): Boolean {
        with(TPipelineContainerDispatch.T_PIPELINE_CONTAINER_DISPATCH) {
            val records = dslContext.selectOne().from(this).where(PIPELINE_ID.eq(pipelineId))
                .and(PIPELINE_VERSION.eq(pipelineVersion)).fetch()
            return records.size > 0
        }
    }

    fun delete(dslContext: DSLContext, pipelineId: String) {
        with(TPipelineContainerDispatch.T_PIPELINE_CONTAINER_DISPATCH) {
            dslContext.deleteFrom(this)
                    .where(PIPELINE_ID.eq(pipelineId))
                    .execute()
        }
    }

    fun create(
        dslContext: DSLContext,
        containerId: String,
        pipelineId: String,
        pipelineVersion: Int,
        projectId: String,
        dispatchBuildType: String,
        dispatchValue: String,
        dispatchImageType: String?,
        dispatchCredentialId: String?,
        dispatchWorkspace: String?,
        dispatchAgentType: String?
    ): Int {
        return with(TPipelineContainerDispatch.T_PIPELINE_CONTAINER_DISPATCH) {
            dslContext.insertInto(
                this,
                CONTAINER_ID,
                PIPELINE_ID,
                PIPELINE_VERSION,
                PROJECT_ID,
                DISPATCH_BUILD_TYPE,
                DISPATCH_VALUE,
                DISPATCH_IMAGE_TYPE,
                DISPATCH_CREDENTIAL_ID,
                DISPATCH_WORKSPACE,
                DISPATCH_AGENT_TYPE
            )
                .values(
                    containerId,
                    pipelineId,
                    pipelineVersion,
                    projectId,
                    dispatchBuildType,
                    dispatchValue,
                    dispatchImageType,
                    dispatchCredentialId,
                    dispatchWorkspace,
                    dispatchAgentType
                )
                .execute()
        }
    }

    fun batchCreate(
        dslContext: DSLContext,
        pipelineContainerDispatchInfoList: List<PipelineContainerDispatchInfo>
    ): IntArray {
        return with(TPipelineContainerDispatch.T_PIPELINE_CONTAINER_DISPATCH) {
            val now = LocalDateTime.now()
            val recordList = pipelineContainerDispatchInfoList.map {
                val record = TPipelineContainerDispatchRecord()
                record.set(ID, 0)
                record.set(CONTAINER_ID, it.containerId)
                record.set(PIPELINE_ID, it.pipelineId)
                record.set(PIPELINE_VERSION, it.pipelineVersion)
                record.set(PROJECT_ID, it.projectId)
                record.set(UPDATE_TIME, now)
                record.set(DISPATCH_BUILD_TYPE, it.dispatchBuildType)
                record.set(DISPATCH_VALUE, it.dispatchValue)
                record.set(DISPATCH_IMAGE_TYPE, it.dispatchImageType)
                record.set(DISPATCH_CREDENTIAL_ID, it.dispatchCredentialId)
                record.set(DISPATCH_WORKSPACE, it.dispatchWorkspace)
                record.set(DISPATCH_AGENT_TYPE, it.dispatchAgentType)
                record
            }
            dslContext.batchInsert(recordList).execute()
        }
    }

    fun listPipelineIds(
        dslContext: DSLContext,
        dispatchBuildType: String?,
        dispatchValue: String?,
        limit: Int?,
        offset: Int?
    ): Result<Record1<String>>? {
        // 先取最大版本再做条件过滤，历史版本中使用了镜像不算
        val a = TPipelineContainerDispatch.T_PIPELINE_CONTAINER_DISPATCH.`as`("a")
        val b = TPipelineInfo.T_PIPELINE_INFO.`as`("b")

        val baseQuery = dslContext.select(
                a.PIPELINE_ID.`as`("pipelineId")
        ).from(a).join(b).on(a.PIPELINE_ID.eq(b.PIPELINE_ID))
        val conditions = mutableListOf<Condition>()
        conditions.add(a.DISPATCH_BUILD_TYPE.eq(dispatchBuildType))
        conditions.add(a.PIPELINE_VERSION.eq(b.VERSION))
        if (!dispatchValue.isNullOrBlank()) {
            conditions.add(a.DISPATCH_VALUE.eq(dispatchValue))
        }
        val groupQuery = baseQuery
                .where(conditions)
                .groupBy(a.PIPELINE_ID)
        val result: Result<Record1<String>>?
        if (limit != null && limit > 0 && offset != null && offset >= 0) {
            result = groupQuery
                .limit(offset, limit)
                .fetch()
        } else {
            result = groupQuery.fetch()
        }
        return result
    }

    fun getPipelineNum(dslContext: DSLContext, dispatchBuildType: String?, dispatchValue: String?): Int {
        // 先取最大版本再做条件过滤，历史版本中使用了镜像不算
        val a = TPipelineContainerDispatch.T_PIPELINE_CONTAINER_DISPATCH.`as`("a")
        val b = TPipelineInfo.T_PIPELINE_INFO.`as`("b")

        val baseQuery = dslContext.select(
            a.PIPELINE_ID
        ).from(a).join(b).on(a.PIPELINE_ID.eq(b.PIPELINE_ID))
        val conditions = mutableListOf<Condition>()
        conditions.add(a.DISPATCH_BUILD_TYPE.eq(dispatchBuildType))
        conditions.add(a.PIPELINE_VERSION.eq(b.VERSION))
        if (!dispatchValue.isNullOrBlank()) {
            conditions.add(a.DISPATCH_VALUE.eq(dispatchValue))
        }
        val groupQuery = baseQuery
            .where(conditions)
            .groupBy(a.PIPELINE_ID)
        return groupQuery.fetch().size
    }
}