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
import com.tencent.devops.common.db.utils.skipCheck
import com.tencent.devops.dispatch.pojo.enums.PipelineTaskStatus
import com.tencent.devops.model.dispatch.tables.TDispatchPipelineDockerDebug
import com.tencent.devops.model.dispatch.tables.records.TDispatchPipelineDockerDebugRecord
import org.jooq.DSLContext
import org.jooq.DatePart
import org.jooq.Field
import org.jooq.Result
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.LocalDateTime

@Repository@Suppress("ALL")
class PipelineDockerDebugDao {

    fun insertDebug(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        vmSeqId: String,
        poolNo: Int,
        status: PipelineTaskStatus,
        token: String,
        imageName: String,
        hostTag: String,
        containerId: String,
        buildEnv: String,
        registryUser: String?,
        registryPwd: String?,
        imageType: String?
    ) {
        with(TDispatchPipelineDockerDebug.T_DISPATCH_PIPELINE_DOCKER_DEBUG) {
            val now = LocalDateTime.now()
            dslContext.insertInto(this,
                PROJECT_ID,
                PIPELINE_ID,
                VM_SEQ_ID,
                POOL_NO,
                STATUS,
                TOKEN,
                IMAGE_NAME,
                HOST_TAG,
                CONTAINER_ID,
                CREATED_TIME,
                UPDATED_TIME,
                BUILD_ENV,
                REGISTRY_USER,
                REGISTRY_PWD,
                IMAGE_TYPE
            )
                .values(
                    projectId,
                    pipelineId,
                    vmSeqId,
                    poolNo,
                    status.status,
                    token,
                    imageName,
                    hostTag,
                    containerId,
                    now,
                    now,
                    buildEnv,
                    registryUser,
                    registryPwd,
                    imageType
                )
                .onDuplicateKeyUpdate()
                .set(POOL_NO, poolNo)
                .set(STATUS, status.status)
                .set(TOKEN, token)
                .set(IMAGE_NAME, imageName)
                .set(HOST_TAG, hostTag)
                .set(CONTAINER_ID, containerId)
                .set(UPDATED_TIME, now)
                .set(BUILD_ENV, buildEnv)
                .set(REGISTRY_USER, registryUser)
                .set(REGISTRY_PWD, registryPwd)
                .execute()
        }
    }

    fun updateStatus(
        dslContext: DSLContext,
        pipelineId: String,
        vmSeqId: String,
        status: PipelineTaskStatus
    ): Boolean {
        with(TDispatchPipelineDockerDebug.T_DISPATCH_PIPELINE_DOCKER_DEBUG) {
            return dslContext.update(this)
                .set(STATUS, status.status)
                .set(UPDATED_TIME, LocalDateTime.now())
                .where(PIPELINE_ID.eq(pipelineId))
                .and(VM_SEQ_ID.eq(vmSeqId))
                .execute() == 1
        }
    }

    fun updateContainerId(
        dslContext: DSLContext,
        pipelineId: String,
        vmSeqId: String,
        containerId: String
    ): Boolean {
        with(TDispatchPipelineDockerDebug.T_DISPATCH_PIPELINE_DOCKER_DEBUG) {
            return dslContext.update(this)
                .set(CONTAINER_ID, containerId)
                .set(UPDATED_TIME, LocalDateTime.now())
                .where(PIPELINE_ID.eq(pipelineId))
                .and(VM_SEQ_ID.eq(vmSeqId))
                .execute() == 1
        }
    }

    fun updateStatusAndTag(
        dslContext: DSLContext,
        pipelineId: String,
        vmSeqId: String,
        status: PipelineTaskStatus,
        hostTag: String
    ): Boolean {
        with(TDispatchPipelineDockerDebug.T_DISPATCH_PIPELINE_DOCKER_DEBUG) {
            return dslContext.update(this)
                .set(STATUS, status.status)
                .set(HOST_TAG, hostTag)
                .set(UPDATED_TIME, LocalDateTime.now())
                .where(PIPELINE_ID.eq(pipelineId))
                .and(VM_SEQ_ID.eq(vmSeqId))
                .execute() == 1
        }
    }

    fun listDebugs(
        dslContext: DSLContext,
        pipelineId: String,
        vmSeqId: String
    ): Result<TDispatchPipelineDockerDebugRecord> {
        with(TDispatchPipelineDockerDebug.T_DISPATCH_PIPELINE_DOCKER_DEBUG) {
            return dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(VM_SEQ_ID.eq(vmSeqId))
                .skipCheck()
                .fetch()
        }
    }

    fun getDebug(
        dslContext: DSLContext,
        pipelineId: String,
        vmSeqId: String
    ): TDispatchPipelineDockerDebugRecord? {
        with(TDispatchPipelineDockerDebug.T_DISPATCH_PIPELINE_DOCKER_DEBUG) {
            return dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(VM_SEQ_ID.eq(vmSeqId))
                .skipCheck()
                .fetchOne()
        }
    }

    fun getQueueDebugExcludeProj(
        dslContext: DSLContext,
        projectIds: Set<String>
    ): Result<TDispatchPipelineDockerDebugRecord> {
        with(TDispatchPipelineDockerDebug.T_DISPATCH_PIPELINE_DOCKER_DEBUG) {
            return dslContext.selectFrom(this)
                .where(STATUS.eq(PipelineTaskStatus.QUEUE.status))
                .and(HOST_TAG.eq("")).and(PROJECT_ID.notIn(projectIds))
                .orderBy(UPDATED_TIME.asc())
                .skipCheck()
                .fetch()
        }
    }

    fun getQueueDebugByProj(
        dslContext: DSLContext,
        projectIds: Set<String>
    ): Result<TDispatchPipelineDockerDebugRecord> {
        with(TDispatchPipelineDockerDebug.T_DISPATCH_PIPELINE_DOCKER_DEBUG) {
            return dslContext.selectFrom(this)
                .where(STATUS.eq(PipelineTaskStatus.QUEUE.status))
                .and(HOST_TAG.eq("")).and(PROJECT_ID.`in`(projectIds))
                .orderBy(UPDATED_TIME.asc())
                .skipCheck()
                .fetch()
        }
    }

    fun getQueueDebugExcludeProj(
        dslContext: DSLContext,
        projectIds: Set<String>,
        hostTag: String
    ): Result<TDispatchPipelineDockerDebugRecord> {
        with(TDispatchPipelineDockerDebug.T_DISPATCH_PIPELINE_DOCKER_DEBUG) {
            return dslContext.selectFrom(this)
                .where(STATUS.eq(PipelineTaskStatus.QUEUE.status))
                .and(HOST_TAG.eq(hostTag)).and(PROJECT_ID.notIn(projectIds))
                .orderBy(UPDATED_TIME.asc())
                .skipCheck()
                .fetch()
        }
    }

    fun getQueueDebugByProj(
        dslContext: DSLContext,
        projectIds: Set<String>,
        hostTag: String
    ): Result<TDispatchPipelineDockerDebugRecord> {
        with(TDispatchPipelineDockerDebug.T_DISPATCH_PIPELINE_DOCKER_DEBUG) {
            return dslContext.selectFrom(this)
                .where(STATUS.eq(PipelineTaskStatus.QUEUE.status))
                .and(HOST_TAG.eq(hostTag)).and(PROJECT_ID.`in`(projectIds))
                .orderBy(UPDATED_TIME.asc())
                .skipCheck()
                .fetch()
        }
    }

    fun getDoneDebug(
        dslContext: DSLContext,
        hostTag: String
    ): Result<TDispatchPipelineDockerDebugRecord> {
        with(TDispatchPipelineDockerDebug.T_DISPATCH_PIPELINE_DOCKER_DEBUG) {
            val statusCond = STATUS.eq(PipelineTaskStatus.DONE.status).or(STATUS.eq(PipelineTaskStatus.FAILURE.status))
            return dslContext.selectFrom(this)
                .where(statusCond)
                .and(HOST_TAG.eq(hostTag))
                .orderBy(UPDATED_TIME.asc())
                .skipCheck()
                .fetch()
        }
    }

    fun deleteDebug(
        dslContext: DSLContext,
        pipelineId: String,
        vmSeqId: String
    ) {
        with(TDispatchPipelineDockerDebug.T_DISPATCH_PIPELINE_DOCKER_DEBUG) {
            dslContext.deleteFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(VM_SEQ_ID.eq(vmSeqId))
                .execute()
        }
    }

    fun deleteDebug(dslContext: DSLContext, id: Long) {
        with(TDispatchPipelineDockerDebug.T_DISPATCH_PIPELINE_DOCKER_DEBUG) {
            dslContext.deleteFrom(this)
                .where(ID.eq(id))
                .execute()
        }
    }

    fun getTimeOutDebugTask(dslContext: DSLContext): Result<TDispatchPipelineDockerDebugRecord> {
        with(TDispatchPipelineDockerDebug.T_DISPATCH_PIPELINE_DOCKER_DEBUG) {
            return dslContext.selectFrom(this)
                .where(timestampDiff(DatePart.HOUR, UPDATED_TIME.cast(Timestamp::class.java)).greaterOrEqual(1))
                .skipCheck()
                .fetch()
        }
    }

    fun updateTimeOutDebugTask(dslContext: DSLContext): Boolean {
        with(TDispatchPipelineDockerDebug.T_DISPATCH_PIPELINE_DOCKER_DEBUG) {
            return dslContext.update(this)
                .set(STATUS, PipelineTaskStatus.DONE.status)
                .where(timestampDiff(DatePart.HOUR, UPDATED_TIME.cast(Timestamp::class.java)).greaterOrEqual(1))
                .execute() == 1
        }
    }

    fun getUnclaimedHostDebug(dslContext: DSLContext): Result<TDispatchPipelineDockerDebugRecord> {
        with(TDispatchPipelineDockerDebug.T_DISPATCH_PIPELINE_DOCKER_DEBUG) {
            return dslContext.selectFrom(this)
                .where(timestampDiff(DatePart.SECOND, UPDATED_TIME
                    .cast(java.sql.Timestamp::class.java)).greaterOrEqual(60))
                .and(STATUS.eq(PipelineTaskStatus.QUEUE.status))
                .and(HOST_TAG.isNotNull).and(HOST_TAG.notEqual(""))
                .skipCheck()
                .fetch()
        }
    }

    fun clearHostTagForUnclaimedHostDebug(dslContext: DSLContext): Boolean {
        with(TDispatchPipelineDockerDebug.T_DISPATCH_PIPELINE_DOCKER_DEBUG) {
            return dslContext.update(this)
                .set(HOST_TAG, "")
                .where(timestampDiff(DatePart.SECOND, UPDATED_TIME
                    .cast(java.sql.Timestamp::class.java)).greaterOrEqual(60))
                .and(STATUS.eq(PipelineTaskStatus.QUEUE.status))
                .and(HOST_TAG.isNotNull).and(HOST_TAG.notEqual(""))
                .execute() == 1
        }
    }

    fun getUnclaimedZoneDebug(dslContext: DSLContext): Result<TDispatchPipelineDockerDebugRecord> {
        with(TDispatchPipelineDockerDebug.T_DISPATCH_PIPELINE_DOCKER_DEBUG) {
            return dslContext.selectFrom(this)
                .where(timestampDiff(DatePart.SECOND, UPDATED_TIME
                    .cast(java.sql.Timestamp::class.java)).greaterOrEqual(100))
                .and(STATUS.eq(PipelineTaskStatus.QUEUE.status))
                .and(ZONE.isNotNull).and(ZONE.notEqual(""))
                .skipCheck()
                .fetch()
        }
    }

    fun resetZoneForUnclaimedZoneDebug(dslContext: DSLContext): Boolean {
        with(TDispatchPipelineDockerDebug.T_DISPATCH_PIPELINE_DOCKER_DEBUG) {
            return dslContext.update(this)
                .set(ZONE, Zone.SHENZHEN.name)
                .where(timestampDiff(DatePart.SECOND, UPDATED_TIME
                    .cast(java.sql.Timestamp::class.java)).greaterOrEqual(100))
                .and(STATUS.eq(PipelineTaskStatus.QUEUE.status))
                .and(ZONE.isNotNull).and(ZONE.notEqual("")).and(ZONE.notEqual(Zone.SHENZHEN.name))
                .execute() == 1
        }
    }

    fun timestampDiff(part: DatePart, t1: Field<Timestamp>): Field<Int> {
        return DSL.field("timestampdiff({0}, {1}, NOW())",
            Int::class.java, DSL.keyword(part.toSQL()), t1)
    }
}

/**

DROP TABLE IF EXISTS `T_DISPATCH_PIPELINE_DOCKER_DEBUG`;
CREATE TABLE `T_DISPATCH_PIPELINE_DOCKER_DEBUG` (
`ID` int(11) NOT NULL AUTO_INCREMENT,
`PROJECT_ID` varchar(64) NOT NULL,
`PIPELINE_ID` varchar(32) NOT NULL DEFAULT '',
`VM_SEQ_ID` varchar(32) NOT NULL,
`STATUS` int(11) NOT NULL,
`TOKEN` varchar(128) NULL,
`IMAGE_NAME` varchar(1024) NOT NULL,
`HOST_TAG` varchar(128) NULL,
`CONTAINER_ID` varchar(128) NULL,
`CREATED_TIME` datetime NOT NULL,
`UPDATED_TIME` datetime NOT NULL,
`BUILD_ENV` varchar(4096) NULL,
`REGISTRY_USER` varchar(128) NULL,
`REGISTRY_PWD` varchar(128) NULL,
COLUMN `IMAGE_TYPE` varchar(128) NULL,
PRIMARY KEY (`ID`),
UNIQUE KEY `PIPELINE_ID` (`PIPELINE_ID`,`VM_SEQ_ID`)
) ENGINE=InnoDB AUTO_INCREMENT=93 DEFAULT CHARSET=utf8;

ALTER TABLE T_DISPATCH_PIPELINE_DOCKER_DEBUG ADD COLUMN `BUILD_ENV` varchar(4096) NULL;

ALTER TABLE T_DISPATCH_PIPELINE_DOCKER_DEBUG ADD COLUMN `REGISTRY_USER` varchar(128) NULL;
ALTER TABLE T_DISPATCH_PIPELINE_DOCKER_DEBUG ADD COLUMN `REGISTRY_PWD` varchar(128) NULL;
ALTER TABLE T_DISPATCH_PIPELINE_DOCKER_DEBUG ADD COLUMN `IMAGE_TYPE` varchar(128) NULL;

ALTER TABLE T_DISPATCH_PIPELINE_DOCKER_DEBUG ADD COLUMN `POOL_NO` int(11) DEFAULT 0;

 * */
