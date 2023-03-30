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

package com.tencent.devops.dispatch.codecc.dao

import com.tencent.devops.common.api.util.SecurityUtil
import com.tencent.devops.model.dispatch.codecc.tables.TDispatchPipelineDockerBuild
import com.tencent.devops.model.dispatch.codecc.tables.records.TDispatchPipelineDockerBuildRecord
import com.tencent.devops.dispatch.codecc.pojo.PipelineTaskStatus
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Result
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

@Repository
class PipelineDockerBuildDao {

    private val logger = LoggerFactory.getLogger(PipelineDockerBuildDao::class.java)

    fun startBuild(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        vmSeqId: Int,
        secretKey: String,
        status: PipelineTaskStatus,
        zone: String?,
        dockerIp: String,
        poolNo: Int
    ): Long {
        with(TDispatchPipelineDockerBuild.T_DISPATCH_PIPELINE_DOCKER_BUILD) {
            val now = LocalDateTime.now()
            val preRecord =
                dslContext.selectFrom(this).where(BUILD_ID.eq(buildId)).and(VM_SEQ_ID.eq(vmSeqId)).fetchAny()
            if (preRecord != null) { // 支持更新，让用户进行步骤重试时继续能使用
                dslContext.update(this).set(SECRET_KEY, SecurityUtil.encrypt(secretKey))
                    .set(STATUS, status.status)
                    .set(CREATED_TIME, now)
                    .set(UPDATED_TIME, now)
                    .set(ZONE, zone)
                    .set(DOCKER_IP, dockerIp)
                    .set(POOL_NO, poolNo)
                    .where(ID.eq(preRecord.id)).execute()
                return preRecord.id
            }
            return dslContext.insertInto(
                this,
                PROJECT_ID,
                PIPELINE_ID,
                BUILD_ID,
                VM_SEQ_ID,
                SECRET_KEY,
                STATUS,
                CREATED_TIME,
                UPDATED_TIME,
                ZONE,
                DOCKER_IP,
                POOL_NO
            )
                .values(
                    projectId,
                    pipelineId,
                    buildId,
                    vmSeqId,
                    SecurityUtil.encrypt(secretKey),
                    status.status,
                    now,
                    now,
                    zone,
                    dockerIp,
                    poolNo
                )
                .returning(ID)
                .fetchOne()!!.id
        }
    }

    fun updateStatus(
        dslContext: DSLContext,
        buildId: String,
        vmSeqId: Int,
        status: PipelineTaskStatus
    ): Boolean {
        with(TDispatchPipelineDockerBuild.T_DISPATCH_PIPELINE_DOCKER_BUILD) {
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
        containerId: String
    ): Boolean {
        with(TDispatchPipelineDockerBuild.T_DISPATCH_PIPELINE_DOCKER_BUILD) {
            return dslContext.update(this)
                .set(CONTAINER_ID, containerId)
                .set(UPDATED_TIME, LocalDateTime.now())
                .where(BUILD_ID.eq(buildId))
                .and(VM_SEQ_ID.eq(vmSeqId))
                .execute() == 1
        }
    }

    fun listBuilds(
        dslContext: DSLContext,
        buildId: String
    ): Result<TDispatchPipelineDockerBuildRecord> {
        with(TDispatchPipelineDockerBuild.T_DISPATCH_PIPELINE_DOCKER_BUILD) {
            return dslContext.selectFrom(this)
                .where(BUILD_ID.eq(buildId))
                .fetch()
        }
    }

    fun getBuild(
        dslContext: DSLContext,
        buildId: String,
        vmSeqId: Int
    ): TDispatchPipelineDockerBuildRecord? {
        with(TDispatchPipelineDockerBuild.T_DISPATCH_PIPELINE_DOCKER_BUILD) {
            return dslContext.selectFrom(this)
                .where(BUILD_ID.eq(buildId))
                .and(VM_SEQ_ID.eq(vmSeqId))
                .fetchOne()
        }
    }

    fun getLatestBuild(
        dslContext: DSLContext,
        pipelineId: String,
        vmSeqId: Int
    ): Result<TDispatchPipelineDockerBuildRecord> {
        with(TDispatchPipelineDockerBuild.T_DISPATCH_PIPELINE_DOCKER_BUILD) {
            return dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(VM_SEQ_ID.eq(vmSeqId))
                .orderBy(CREATED_TIME.desc())
                .fetch()
        }
    }

    fun getTimeOutBuild(dslContext: DSLContext): Result<TDispatchPipelineDockerBuildRecord> {
        with(TDispatchPipelineDockerBuild.T_DISPATCH_PIPELINE_DOCKER_BUILD) {
            return dslContext.selectFrom(this)
                .where(STATUS.eq(2))
                .and(UPDATED_TIME.lessOrEqual(timestampSubDay(2)))
                .limit(1000)
                .fetch()
        }
    }

    fun updateTimeOutBuild(dslContext: DSLContext, buildId: String): Boolean {
        with(TDispatchPipelineDockerBuild.T_DISPATCH_PIPELINE_DOCKER_BUILD) {
            return dslContext.update(this)
                .set(STATUS, PipelineTaskStatus.DONE.status)
                .set(UPDATED_TIME, LocalDateTime.now())
                .where(BUILD_ID.eq(buildId))
                .execute() == 1
        }
    }

    fun getBuildListByLimit(
        dslContext: DSLContext,
        startTime: String,
        endTime: String,
        limit: Int
    ): Result<TDispatchPipelineDockerBuildRecord> {
        with(TDispatchPipelineDockerBuild.T_DISPATCH_PIPELINE_DOCKER_BUILD) {
            val startTimeFormat: LocalDateTime = Instant.ofEpochMilli(startTime.toLong()).atZone(ZoneOffset.ofHours(8)).toLocalDateTime()
            val endTimeFormat: LocalDateTime = Instant.ofEpochMilli(endTime.toLong()).atZone(ZoneOffset.ofHours(8)).toLocalDateTime()
            logger.info("startTimeFormat: $startTimeFormat, endTimeFormat: $endTimeFormat")
            return dslContext.selectFrom(this)
                .where(UPDATED_TIME.between(startTimeFormat).and(endTimeFormat))
                .limit(limit)
                .fetch()
        }
    }

    fun timestampSubDay(day: Long): Field<LocalDateTime> {
        return DSL.field("date_sub(NOW(), interval $day day)",
            LocalDateTime::class.java)
    }
}

/*
ALTER TABLE `T_DISPATCH_PIPELINE_DOCKER_BUILD` ADD COLUMN `DOCKER_IP` VARCHAR(64) DEFAULT '' COMMENT '构建机IP';
ALTER TABLE `T_DISPATCH_PIPELINE_DOCKER_BUILD` ADD COLUMN `CONTAINER_ID` VARCHAR(128) DEFAULT '' COMMENT '构建容器ID';
ALTER TABLE `T_DISPATCH_PIPELINE_DOCKER_BUILD` ADD COLUMN `POOL_NO` INT(11) DEFAULT 0 COMMENT '构建容器池序号';
 */
