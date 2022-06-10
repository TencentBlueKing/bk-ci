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

package com.tencent.devops.dispatch.kubernetes.dao

import com.tencent.devops.model.dispatch.tables.TDispatchKubernetesBuild
import com.tencent.devops.model.dispatch.tables.records.TDispatchKubernetesBuildRecord
import org.jooq.DSLContext
import org.jooq.DatePart
import org.jooq.Field
import org.jooq.Result
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.LocalDateTime

@Repository
class BuildDao {

    private val logger = LoggerFactory.getLogger(BuildDao::class.java)

    fun createOrUpdate(
        dslContext: DSLContext,
        pipelineId: String,
        vmSeqId: String,
        poolNo: Int,
        projectId: String,
        containerName: String,
        image: String,
        status: Int,
        userId: String,
        cpu: Int,
        memory: String,
        disk: String
    ): Int {
        return with(TDispatchKubernetesBuild.T_DISPATCH_KUBERNETES_BUILD) {
            dslContext.insertInto(
                this,
                PIPELINE_ID,
                VM_SEQ_ID,
                POOL_NO,
                PROJECT_ID,
                CONTAINER_NAME,
                IMAGES,
                STATUS,
                CREATED_TIME,
                UPDATE_TIME,
                USER_ID,
                CPU,
                MEMORY,
                DISK
            ).values(
                pipelineId,
                vmSeqId,
                poolNo,
                projectId,
                containerName,
                image,
                status,
                LocalDateTime.now(),
                LocalDateTime.now(),
                userId,
                cpu,
                memory,
                disk
            ).onDuplicateKeyUpdate()
                .set(PIPELINE_ID, pipelineId)
                .set(VM_SEQ_ID, vmSeqId)
                .set(POOL_NO, poolNo)
                .set(PROJECT_ID, projectId)
                .set(CONTAINER_NAME, containerName)
                .set(IMAGES, image)
                .set(STATUS, status)
                .set(CPU, cpu)
                .set(MEMORY, memory)
                .set(DISK, disk)
                .set(UPDATE_TIME, LocalDateTime.now())
                .execute()
        }
    }

    fun create(
        dslContext: DSLContext,
        pipelineId: String,
        vmSeqId: String,
        poolNo: Int,
        projectId: String,
        containerName: String,
        image: String,
        status: Int,
        userId: String
    ) {
        with(TDispatchKubernetesBuild.T_DISPATCH_KUBERNETES_BUILD) {
            dslContext.insertInto(
                this,
                PIPELINE_ID,
                VM_SEQ_ID,
                POOL_NO,
                PROJECT_ID,
                CONTAINER_NAME,
                IMAGES,
                STATUS,
                CREATED_TIME,
                UPDATE_TIME,
                USER_ID
            ).values(
                pipelineId,
                vmSeqId,
                poolNo,
                projectId,
                containerName,
                image,
                status,
                LocalDateTime.now(),
                LocalDateTime.now(),
                userId
            ).execute()
        }
    }

    fun update(
        dslContext: DSLContext,
        pipelineId: String,
        vmSeqId: String,
        poolNo: Int,
        containerName: String,
        image: String,
        status: Int
    ) {
        with(TDispatchKubernetesBuild.T_DISPATCH_KUBERNETES_BUILD) {
            dslContext.update(this)
                .set(UPDATE_TIME, LocalDateTime.now())
                .set(IMAGES, image)
                .set(CONTAINER_NAME, containerName)
                .set(STATUS, status)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(VM_SEQ_ID.eq(vmSeqId))
                .and(POOL_NO.eq(poolNo))
                .execute()
        }
    }

    fun updateStatus(
        dslContext: DSLContext,
        pipelineId: String,
        vmSeqId: String?,
        poolNo: Int?,
        status: Int
    ) {
        with(TDispatchKubernetesBuild.T_DISPATCH_KUBERNETES_BUILD) {
            logger.info("$pipelineId|$vmSeqId|$poolNo update status: $status")
            val sql = dslContext.update(this)
                .set(UPDATE_TIME, LocalDateTime.now())
                .set(STATUS, status)
                .where(PIPELINE_ID.eq(pipelineId))
            if (null != vmSeqId) {
                sql.and(VM_SEQ_ID.eq(vmSeqId))
            }
            if (null != poolNo) {
                sql.and(POOL_NO.eq(poolNo))
            }
            sql.execute()
        }
    }

    fun updateDebugStatus(
        dslContext: DSLContext,
        pipelineId: String,
        vmSeqId: String?,
        containerName: String?,
        debugStatus: Boolean
    ) {
        with(TDispatchKubernetesBuild.T_DISPATCH_KUBERNETES_BUILD) {
            val sql = dslContext.update(this)
                .set(UPDATE_TIME, LocalDateTime.now())
                .set(DEBUG_TIME, LocalDateTime.now())
                .set(DEBUG_STATUS, debugStatus)
                .where(PIPELINE_ID.eq(pipelineId))
            if (null != vmSeqId) {
                sql.and(VM_SEQ_ID.eq(vmSeqId))
            }
            if (null != containerName) {
                sql.and(CONTAINER_NAME.eq(containerName))
            }
            sql.execute()
        }
    }

    fun get(
        dslContext: DSLContext,
        pipelineId: String,
        vmSeqId: String,
        poolNo: Int
    ): TDispatchKubernetesBuildRecord? {
        with(TDispatchKubernetesBuild.T_DISPATCH_KUBERNETES_BUILD) {
            return dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(VM_SEQ_ID.eq(vmSeqId))
                .and(POOL_NO.eq(poolNo))
                .fetchAny()
        }
    }

    fun getContainerStatus(
        dslContext: DSLContext,
        pipelineId: String,
        vmSeqId: String,
        containerName: String
    ): TDispatchKubernetesBuildRecord? {
        with(TDispatchKubernetesBuild.T_DISPATCH_KUBERNETES_BUILD) {
            return dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(VM_SEQ_ID.eq(vmSeqId))
                .and(CONTAINER_NAME.eq(containerName))
                .fetchAny()
        }
    }

    fun delete(
        dslContext: DSLContext,
        pipelineId: String,
        vmSeqId: String,
        poolNo: Int
    ): Int {
        return with(TDispatchKubernetesBuild.T_DISPATCH_KUBERNETES_BUILD) {
            dslContext.delete(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(VM_SEQ_ID.eq(vmSeqId))
                .and(POOL_NO.eq(poolNo))
                .execute()
        }
    }

    fun getTimeOutBusyContainer(dslContext: DSLContext): Result<TDispatchKubernetesBuildRecord> {
        with(TDispatchKubernetesBuild.T_DISPATCH_KUBERNETES_BUILD) {
            return dslContext.selectFrom(this)
                .where(timestampDiff(DatePart.DAY, UPDATE_TIME.cast(java.sql.Timestamp::class.java)).greaterOrEqual(2))
                .and(STATUS.eq(1))
                .limit(1000)
                .fetch()
        }
    }

    fun getNoUseIdleContainer(dslContext: DSLContext): Result<TDispatchKubernetesBuildRecord> {
        with(TDispatchKubernetesBuild.T_DISPATCH_KUBERNETES_BUILD) {
            return dslContext.selectFrom(this)
                .where(timestampDiff(DatePart.DAY, UPDATE_TIME.cast(java.sql.Timestamp::class.java)).greaterOrEqual(7))
                .and(STATUS.eq(0))
                .fetch()
        }
    }

    fun getTimeoutBusyDebugContainer(dslContext: DSLContext): Result<TDispatchKubernetesBuildRecord> {
        with(TDispatchKubernetesBuild.T_DISPATCH_KUBERNETES_BUILD) {
            return dslContext.selectFrom(this)
                .where(STATUS.eq(0))
                .and(DEBUG_STATUS.eq(true))
                .and(timestampDiff(DatePart.HOUR, DEBUG_TIME.cast(java.sql.Timestamp::class.java)).greaterOrEqual(1))
                .fetch()
        }
    }

    fun timestampDiff(part: DatePart, t1: Field<Timestamp>): Field<Int> {
        return DSL.field(
            "timestampdiff({0}, {1}, NOW())",
            Int::class.java, DSL.keyword(part.toSQL()), t1
        )
    }
}
