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

package com.tencent.devops.process.engine.dao

import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.BuildNo
import com.tencent.devops.model.process.Tables.T_PIPELINE_INFO
import com.tencent.devops.model.process.Tables.T_PIPELINE_INFO_VERSION
import com.tencent.devops.model.process.tables.records.TPipelineInfoVersionRecord
import com.tencent.devops.process.engine.pojo.PipelineInfo
import org.jooq.DSLContext
import org.jooq.Result
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class PipelineInfoVersionDao {

    fun create(
        dslContext: DSLContext,
        pipelineId: String,
        projectId: String,
        version: Int,
        pipelineName: String,
        userId: String,
        channelCode: ChannelCode,
        manualStartup: Boolean,
        canElementSkip: Boolean,
        taskCount: Int
    ): Int {
        val count = with(T_PIPELINE_INFO_VERSION) {
            dslContext.insertInto(
                    this,
                    PIPELINE_ID,
                    PROJECT_ID,
                    VERSION,
                    PIPELINE_NAME,
                    PIPELINE_DESC,
                    CREATE_TIME,
                    UPDATE_TIME,
                    CHANNEL,
                    CREATOR,
                    LAST_MODIFY_USER,
                    MANUAL_STARTUP,
                    ELEMENT_SKIP, TASK_COUNT
            )
                    .values(
                            pipelineId,
                            projectId,
                            version,
                            pipelineName,
                            pipelineName,
                            LocalDateTime.now(),
                            LocalDateTime.now(),
                            channelCode.name, userId, userId,
                            if (manualStartup) 1 else 0,
                            if (canElementSkip) 1 else 0,
                            taskCount
                    )
                    .execute()
        }
        logger.info("Create the pipeline $pipelineId result=${count == 1}")
        return version
    }

    fun create(
        dslContext: DSLContext,
        pipelineId: String,
        projectId: String,
        version: Int,
        pipelineName: String,
        userId: String,
        channelCode: String?,
        manualStartup: Int?,
        canElementSkip: Int?,
        taskCount: Int?
    ): Int {
        val count = with(T_PIPELINE_INFO_VERSION) {
            dslContext.insertInto(
                    this,
                    PIPELINE_ID,
                    PROJECT_ID,
                    VERSION,
                    PIPELINE_NAME,
                    PIPELINE_DESC,
                    CREATE_TIME,
                    UPDATE_TIME,
                    CHANNEL,
                    CREATOR,
                    LAST_MODIFY_USER,
                    MANUAL_STARTUP,
                    ELEMENT_SKIP, TASK_COUNT
            )
                    .values(
                            pipelineId,
                            projectId,
                            version,
                            pipelineName,
                            pipelineName,
                            LocalDateTime.now(),
                            LocalDateTime.now(),
                            channelCode,
                            userId,
                            userId,
                            manualStartup,
                            canElementSkip,
                            taskCount
                    )
                    .execute()
        }
        logger.info("Create the pipeline $pipelineId result=${count == 1}")
        return version
    }

    fun update(
        dslContext: DSLContext,
        pipelineId: String,
        userId: String,
        updateVersion: Boolean = true,
        pipelineName: String? = null,
        pipelineDesc: String? = null,
        manualStartup: Boolean? = null,
        canElementSkip: Boolean? = null,
        buildNo: BuildNo? = null,
        taskCount: Int = 0
    ): Int {
        val count = with(T_PIPELINE_INFO_VERSION) {

            val update = dslContext.update(this)

            if (updateVersion) { // 刷新版本号，每次递增1
                update.set(VERSION, VERSION + 1)
            }

            if (!pipelineName.isNullOrBlank()) {
                update.set(PIPELINE_NAME, pipelineName)
            }
            if (!pipelineDesc.isNullOrBlank()) {
                update.set(PIPELINE_DESC, pipelineDesc)
            }
            if (manualStartup != null) {
                update.set(MANUAL_STARTUP, if (manualStartup) 1 else 0)
            }
            if (canElementSkip != null) {
                update.set(ELEMENT_SKIP, if (canElementSkip) 1 else 0)
            }
            if (taskCount > 0) {
                update.set(TASK_COUNT, taskCount)
            }

            update.set(UPDATE_TIME, LocalDateTime.now())
                .set(LAST_MODIFY_USER, userId)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(VERSION.eq(VERSION - 1))
                .execute()
        }

        val version = with(T_PIPELINE_INFO) {
            dslContext.select(VERSION)
                .from(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .fetchOne(0, Int::class.java)
        }
        logger.info("Update the pipeline $pipelineId add new version($version) and result=${count == 1}")
        return version
    }

    fun listPipelineVersion(dslContext: DSLContext, projectId: String, pipelineId: String): Result<TPipelineInfoVersionRecord>? {
        return with(T_PIPELINE_INFO_VERSION) {
            dslContext.selectFrom(this)
                    .where(PROJECT_ID.eq(projectId))
                    .and(PIPELINE_ID.eq(pipelineId))
                    .and(DELETE.eq(false))
                    .fetch()
        }
    }

    fun getPipelineInfo(
        dslContext: DSLContext,
        pipelineId: String,
        channelCode: ChannelCode? = null
    ): TPipelineInfoVersionRecord? {
        return with(T_PIPELINE_INFO_VERSION) {
            val query = dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))

            if (channelCode != null) {
                query.and(CHANNEL.eq(channelCode.name))
            }
            query.and(DELETE.eq(false)).fetchAny()
        }
    }

    fun getPipelineInfo(
        dslContext: DSLContext,
        projectId: String?,
        pipelineId: String,
        version: Int,
        channelCode: ChannelCode? = null
    ): TPipelineInfoVersionRecord? {
        return with(T_PIPELINE_INFO_VERSION) {
            val query = if (!projectId.isNullOrBlank()) {
                dslContext.selectFrom(this).where(PROJECT_ID.eq(projectId))
                        .and(PIPELINE_ID.eq(pipelineId)).and(VERSION.equal(version))
            } else {
                dslContext.selectFrom(this).where(PIPELINE_ID.eq(pipelineId)).and(VERSION.equal(version))
            }

            if (channelCode != null) {
                query.and(CHANNEL.eq(channelCode.name))
            }
            query.and(DELETE.eq(false)).fetchAny()
        }
    }

    fun getPipelineInfo(
        dslContext: DSLContext,
        projectId: String?,
        pipelineId: String,
        channelCode: ChannelCode? = null,
        delete: Boolean = false
    ): TPipelineInfoVersionRecord? {
        return with(T_PIPELINE_INFO_VERSION) {
            val query = if (!projectId.isNullOrBlank()) {
                dslContext.selectFrom(this).where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
            } else {
                dslContext.selectFrom(this).where(PIPELINE_ID.eq(pipelineId))
            }

            if (channelCode != null) {
                query.and(CHANNEL.eq(channelCode.name))
            }
            query.and(DELETE.eq(delete)).fetchAny()
        }
    }

    fun getPipelineInfo(
        dslContext: DSLContext,
        projectId: String?,
        pipelineId: String,
        version: Int,
        channelCode: ChannelCode? = null,
        delete: Boolean = false
    ): TPipelineInfoVersionRecord? {
        return with(T_PIPELINE_INFO_VERSION) {
            val query = if (!projectId.isNullOrBlank()) {
                dslContext.selectFrom(this).where(PROJECT_ID.eq(projectId))
                        .and(PIPELINE_ID.eq(pipelineId)).and(VERSION.equal(version))
            } else {
                dslContext.selectFrom(this).where(PIPELINE_ID.eq(pipelineId)).and(VERSION.equal(version))
            }

            if (channelCode != null) {
                query.and(CHANNEL.eq(channelCode.name))
            }
            query.and(DELETE.eq(delete)).fetchAny()
        }
    }

    fun softDelete(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        changePipelineName: String,
        userId: String,
        version: Int,
        channelCode: ChannelCode?
    ): Int {
        return with(T_PIPELINE_INFO_VERSION) {
            val update = dslContext.update(this)
                .set(DELETE, true)
                .set(UPDATE_TIME, LocalDateTime.now())
                .set(LAST_MODIFY_USER, userId)
                .set(PIPELINE_NAME, changePipelineName)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(VERSION.eq(version))
            if (channelCode != null) {
                update.and(CHANNEL.eq(channelCode.name))
            }
            update.execute()
        }
    }

    fun delete(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String
    ): Int {
        return with(T_PIPELINE_INFO_VERSION) {
            dslContext.delete(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .execute()
        }
    }

    fun convert(t: TPipelineInfoVersionRecord?, templateId: String?): PipelineInfo? {
        return if (t != null) {
            with(t) {
                PipelineInfo(
                        projectId,
                        pipelineId,
                        templateId,
                        pipelineName,
                        pipelineDesc,
                        version,
                        createTime.timestampmilli(),
                        updateTime.timestampmilli(),
                        creator,
                        lastModifyUser,
                        ChannelCode.valueOf(channel),
                        manualStartup == 1,
                        elementSkip == 1,
                        taskCount
                )
            }
        } else {
            null
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineInfoVersionDao::class.java)
    }
}
