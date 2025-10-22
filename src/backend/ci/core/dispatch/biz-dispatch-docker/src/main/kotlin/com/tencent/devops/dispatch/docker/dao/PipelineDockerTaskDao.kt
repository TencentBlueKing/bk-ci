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

package com.tencent.devops.dispatch.docker.dao

import com.tencent.devops.common.api.pojo.Zone
import com.tencent.devops.dispatch.pojo.enums.PipelineTaskStatus
import com.tencent.devops.model.dispatch.tables.TDispatchPipelineDockerTask
import com.tencent.devops.model.dispatch.tables.records.TDispatchPipelineDockerTaskRecord
import com.tencent.devops.store.pojo.image.enums.ImageRDTypeEnum
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Result
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository@Suppress("ALL")
class PipelineDockerTaskDao {

    fun insertTask(
        dslContext: DSLContext,
        projectId: String,
        agentId: String,
        pipelineId: String,
        buildId: String,
        vmSeqId: Int,
        status: PipelineTaskStatus,
        secretKey: String,
        imageName: String,
        hostTag: String,
        channelCode: String,
        zone: String?,
        registryUser: String?,
        registryPwd: String?,
        imageType: String?,
        imagePublicFlag: Boolean?,
        imageRDType: ImageRDTypeEnum?,
        containerHashId: String?
    ): Long {
        with(TDispatchPipelineDockerTask.T_DISPATCH_PIPELINE_DOCKER_TASK) {
            val now = LocalDateTime.now()
            val preRecord = dslContext.selectFrom(this).where(BUILD_ID.eq(buildId))
                .and(VM_SEQ_ID.eq(vmSeqId)).fetchAny()
            if (preRecord != null) { // 支持更新，让用户进行步骤重试时继续能使用
                dslContext.update(this)
                    .set(AGENT_ID, agentId)
                    .set(STATUS, status.status)
                    .set(SECRET_KEY, secretKey)
                    .set(IMAGE_NAME, imageName)
                    .set(CHANNEL_CODE, channelCode)
                    .set(HOST_TAG, hostTag)
                    .set(CREATED_TIME, now)
                    .set(UPDATED_TIME, now)
                    .set(ZONE, zone)
                    .set(REGISTRY_USER, registryUser)
                    .set(REGISTRY_PWD, registryPwd)
                    .set(IMAGE_TYPE, imageType)
                    .set(CONTAINER_HASH_ID, containerHashId)
                    .where(ID.eq(preRecord.id)).execute()
                return preRecord.id
            }
            return dslContext.insertInto(this,
                PROJECT_ID,
                AGENT_ID,
                PIPELINE_ID,
                BUILD_ID,
                VM_SEQ_ID,
                STATUS,
                SECRET_KEY,
                IMAGE_NAME,
                CHANNEL_CODE,
                HOST_TAG,
                CREATED_TIME,
                UPDATED_TIME,
                ZONE,
                REGISTRY_USER,
                REGISTRY_PWD,
                IMAGE_TYPE,
                IMAGE_PUBLIC_FLAG,
                IMAGE_RD_TYPE,
                CONTAINER_HASH_ID
            ).values(
                projectId,
                agentId,
                pipelineId,
                buildId,
                vmSeqId,
                status.status,
                secretKey,
                imageName,
                channelCode,
                hostTag,
                now,
                now,
                zone,
                registryUser,
                registryPwd,
                imageType,
                imagePublicFlag,
                imageRDType?.type?.toByte(),
                containerHashId
            ).returning(ID).fetchOne()!!.id
        }
    }

    fun updateStatus(
        dslContext: DSLContext,
        buildId: String,
        vmSeqId: Int,
        status: PipelineTaskStatus
    ): Boolean {
        with(TDispatchPipelineDockerTask.T_DISPATCH_PIPELINE_DOCKER_TASK) {
            return dslContext.update(this)
                .set(STATUS, status.status)
                .set(UPDATED_TIME, LocalDateTime.now())
                .where(BUILD_ID.eq(buildId))
                .and(VM_SEQ_ID.eq(vmSeqId))
                .execute() == 1
        }
    }

    fun updateContainerId(
        dslContext: DSLContext,
        buildId: String,
        vmSeqId: Int,
        containerId: String,
        hostTag: String?
    ): Boolean {
        with(TDispatchPipelineDockerTask.T_DISPATCH_PIPELINE_DOCKER_TASK) {
            val update = dslContext.update(this)
                .set(CONTAINER_ID, containerId)
                .set(UPDATED_TIME, LocalDateTime.now())

            if (!hostTag.isNullOrBlank()) {
                update.set(HOST_TAG, hostTag)
            }

            return update.where(BUILD_ID.eq(buildId))
                .and(VM_SEQ_ID.eq(vmSeqId))
                .execute() == 1
        }
    }

    fun updateStatusAndTag(
        dslContext: DSLContext,
        buildId: String,
        vmSeqId: Int,
        status: PipelineTaskStatus,
        hostTag: String
    ): Boolean {
        with(TDispatchPipelineDockerTask.T_DISPATCH_PIPELINE_DOCKER_TASK) {
            return dslContext.update(this)
                .set(STATUS, status.status)
                .set(HOST_TAG, hostTag)
                .set(UPDATED_TIME, LocalDateTime.now())
                .where(BUILD_ID.eq(buildId))
                .and(VM_SEQ_ID.eq(vmSeqId))
                .execute() == 1
        }
    }

    fun listTasks(
        dslContext: DSLContext,
        buildId: String
    ): Result<TDispatchPipelineDockerTaskRecord> {
        with(TDispatchPipelineDockerTask.T_DISPATCH_PIPELINE_DOCKER_TASK) {
            return dslContext.selectFrom(this)
                .where(BUILD_ID.eq(buildId))
                .fetch()
        }
    }

    fun getTask(
        dslContext: DSLContext,
        buildId: String,
        vmSeqId: Int
    ): TDispatchPipelineDockerTaskRecord? {
        with(TDispatchPipelineDockerTask.T_DISPATCH_PIPELINE_DOCKER_TASK) {
            return dslContext.selectFrom(this)
                .where(BUILD_ID.eq(buildId))
                .and(VM_SEQ_ID.eq(vmSeqId))
                .fetchOne()
        }
    }

    fun getQueueTasksExcludeProj(
        dslContext: DSLContext,
        projectIds: Set<String>
    ): Result<TDispatchPipelineDockerTaskRecord> {
        with(TDispatchPipelineDockerTask.T_DISPATCH_PIPELINE_DOCKER_TASK) {
            return dslContext.selectFrom(this)
                .where(STATUS.eq(PipelineTaskStatus.QUEUE.status))
                .and(HOST_TAG.eq(""))
                .and(ZONE.eq("")).and(PROJECT_ID.notIn(projectIds))
                .orderBy(UPDATED_TIME.asc())
                .fetch()
        }
    }

    fun getQueueTasksByProj(
        dslContext: DSLContext,
        projectIds: Set<String>
    ): Result<TDispatchPipelineDockerTaskRecord> {
        with(TDispatchPipelineDockerTask.T_DISPATCH_PIPELINE_DOCKER_TASK) {
            return dslContext.selectFrom(this)
                .where(STATUS.eq(PipelineTaskStatus.QUEUE.status))
                .and(HOST_TAG.eq(""))
                .and(ZONE.eq("")).and(PROJECT_ID.`in`(projectIds))
                .orderBy(UPDATED_TIME.asc())
                .fetch()
        }
    }

    fun getQueueTasksExcludeProj(
        dslContext: DSLContext,
        projectIds: Set<String>,
        zone: Zone
    ): Result<TDispatchPipelineDockerTaskRecord> {
        with(TDispatchPipelineDockerTask.T_DISPATCH_PIPELINE_DOCKER_TASK) {
            return dslContext.selectFrom(this)
                .where(STATUS.eq(PipelineTaskStatus.QUEUE.status))
                .and(ZONE.eq(zone.name))
                .and(HOST_TAG.eq("")).and(PROJECT_ID.notIn(projectIds))
                .orderBy(UPDATED_TIME.asc())
                .fetch()
        }
    }

    fun getQueueTasksByProj(
        dslContext: DSLContext,
        projectIds: Set<String>,
        zone: Zone
    ): Result<TDispatchPipelineDockerTaskRecord> {
        with(TDispatchPipelineDockerTask.T_DISPATCH_PIPELINE_DOCKER_TASK) {
            return dslContext.selectFrom(this)
                .where(STATUS.eq(PipelineTaskStatus.QUEUE.status))
                .and(ZONE.eq(zone.name))
                .and(HOST_TAG.eq("")).and(PROJECT_ID.`in`(projectIds))
                .orderBy(UPDATED_TIME.asc())
                .fetch()
        }
    }

    fun getQueueTasksExcludeProj(
        dslContext: DSLContext,
        projectIds: Set<String>,
        hostTag: String
    ): Result<TDispatchPipelineDockerTaskRecord> {
        with(TDispatchPipelineDockerTask.T_DISPATCH_PIPELINE_DOCKER_TASK) {
            return dslContext.selectFrom(this)
                .where(STATUS.eq(PipelineTaskStatus.QUEUE.status))
                .and(HOST_TAG.eq(hostTag)).and(PROJECT_ID.notIn(projectIds))
                .orderBy(UPDATED_TIME.asc())
                .fetch()
        }
    }

    fun getQueueTasksByProj(
        dslContext: DSLContext,
        projectIds: Set<String>,
        hostTag: String
    ): Result<TDispatchPipelineDockerTaskRecord> {
        with(TDispatchPipelineDockerTask.T_DISPATCH_PIPELINE_DOCKER_TASK) {
            return dslContext.selectFrom(this)
                .where(STATUS.eq(PipelineTaskStatus.QUEUE.status))
                .and(HOST_TAG.eq(hostTag)).and(PROJECT_ID.`in`(projectIds))
                .orderBy(UPDATED_TIME.asc())
                .fetch()
        }
    }

    fun getDoneTasks(
        dslContext: DSLContext,
        hostTag: String
    ): Result<TDispatchPipelineDockerTaskRecord> {
        with(TDispatchPipelineDockerTask.T_DISPATCH_PIPELINE_DOCKER_TASK) {
            val statusCond = STATUS.eq(PipelineTaskStatus.DONE.status).or(STATUS.eq(PipelineTaskStatus.FAILURE.status))
            return dslContext.selectFrom(this)
                .where(statusCond)
                .and(HOST_TAG.eq(hostTag))
                .orderBy(UPDATED_TIME.asc())
                .fetch()
        }
    }

    fun deleteTask(dslContext: DSLContext, id: Long) {
        with(TDispatchPipelineDockerTask.T_DISPATCH_PIPELINE_DOCKER_TASK) {
            dslContext.deleteFrom(this)
                .where(ID.eq(id))
                .execute()
        }
    }

    fun getTimeOutTask(dslContext: DSLContext): Result<TDispatchPipelineDockerTaskRecord> {
        with(TDispatchPipelineDockerTask.T_DISPATCH_PIPELINE_DOCKER_TASK) {
            return dslContext.selectFrom(this)
                .where(UPDATED_TIME.lessOrEqual(timestampSubDay(2)))
                .fetch()
        }
    }

    fun updateTimeOutTask(dslContext: DSLContext): Boolean {
        with(TDispatchPipelineDockerTask.T_DISPATCH_PIPELINE_DOCKER_TASK) {
            return dslContext.update(this)
                .set(STATUS, com.tencent.devops.dispatch.pojo.enums.PipelineTaskStatus.FAILURE.status)
                .where(UPDATED_TIME.lessOrEqual(timestampSubDay(2)))
                .execute() == 1
        }
    }

    fun getUnclaimedHostTask(dslContext: DSLContext): Result<TDispatchPipelineDockerTaskRecord> {
        with(TDispatchPipelineDockerTask.T_DISPATCH_PIPELINE_DOCKER_TASK) {
            return dslContext.selectFrom(this)
                .where(UPDATED_TIME.lessOrEqual(timestampSubSecond(20)))
                .and(STATUS.eq(PipelineTaskStatus.QUEUE.status))
                .and(HOST_TAG.isNotNull).and(HOST_TAG.notEqual(""))
                .fetch()
        }
    }

    fun clearHostTagForUnclaimedHostTask(dslContext: DSLContext, buildId: String, vmSeqId: Int): Boolean {
        with(TDispatchPipelineDockerTask.T_DISPATCH_PIPELINE_DOCKER_TASK) {
            return dslContext.update(this)
                .set(HOST_TAG, "")
                .where(BUILD_ID.eq(buildId))
                .and(VM_SEQ_ID.eq(vmSeqId))
                .and(STATUS.eq(PipelineTaskStatus.QUEUE.status))
                .and(HOST_TAG.isNotNull).and(HOST_TAG.notEqual(""))
                .execute() > 0
        }
    }

    fun clearHostTagForUnclaimedHostTask(dslContext: DSLContext): Boolean {
        with(TDispatchPipelineDockerTask.T_DISPATCH_PIPELINE_DOCKER_TASK) {
            return dslContext.update(this)
                .set(HOST_TAG, "")
                .where(UPDATED_TIME.lessOrEqual(timestampSubSecond(20)))
                .and(STATUS.eq(PipelineTaskStatus.QUEUE.status))
                .and(HOST_TAG.isNotNull).and(HOST_TAG.notEqual(""))
                .execute() == 1
        }
    }

    fun getUnclaimedZoneTask(dslContext: DSLContext): Result<TDispatchPipelineDockerTaskRecord> {
        with(TDispatchPipelineDockerTask.T_DISPATCH_PIPELINE_DOCKER_TASK) {
            return dslContext.selectFrom(this)
                .where(UPDATED_TIME.lessOrEqual(timestampSubSecond(40)))
                .and(STATUS.eq(PipelineTaskStatus.QUEUE.status))
                .and(ZONE.isNotNull).and(ZONE.notEqual(""))
                .fetch()
        }
    }

    fun resetZoneForUnclaimedZoneTask(dslContext: DSLContext): Boolean {
        with(TDispatchPipelineDockerTask.T_DISPATCH_PIPELINE_DOCKER_TASK) {
            return dslContext.update(this)
                .set(ZONE, Zone.SHENZHEN.name)
                .where(UPDATED_TIME.lessOrEqual(timestampSubSecond(40)))
                .and(STATUS.eq(PipelineTaskStatus.QUEUE.status))
                .and(ZONE.isNotNull).and(ZONE.notEqual("")).and(ZONE.notEqual(Zone.SHENZHEN.name))
                .execute() == 1
        }
    }

    fun timestampSubDay(day: Long): Field<LocalDateTime> {
        return DSL.field("date_sub(NOW(), interval $day day)",
            LocalDateTime::class.java)
    }

    fun timestampSubSecond(second: Long): Field<LocalDateTime> {
        return DSL.field("date_sub(NOW(), interval $second second)",
            LocalDateTime::class.java)
    }
}
