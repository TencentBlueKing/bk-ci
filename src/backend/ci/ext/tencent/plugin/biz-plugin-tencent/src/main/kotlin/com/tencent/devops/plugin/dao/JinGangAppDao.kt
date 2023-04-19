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

package com.tencent.devops.plugin.dao

import com.tencent.devops.model.plugin.tables.TPluginJingang
import com.tencent.devops.model.plugin.tables.TPluginJingangResult
import com.tencent.devops.model.plugin.tables.records.TPluginJingangRecord
import com.tencent.devops.model.plugin.tables.records.TPluginJingangResultRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date

@Repository
class JinGangAppDao {

    companion object {
        private val logger = LoggerFactory.getLogger(JinGangAppDao::class.java)
    }

    fun createTask(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        buildNo: Int,
        userId: String,
        path: String,
        md5: String,
        size: Long,
        createTime: Long,
        updateTime: Long,
        version: String,
        type: Int
    ): Long {

        with(TPluginJingang.T_PLUGIN_JINGANG) {
            val data = dslContext.insertInto(this,
                    PROJECT_ID,
                    PIPELINE_ID,
                    BUILD_ID,
                    BUILD_NO,
                    USER_ID,
                    FILE_PATH,
                    FILE_MD5,
                    FILE_SIZE,
                    CREATE_TIME,
                    UPDATE_TIME,
                    VERSION,
                    TYPE)
                    .values(projectId,
                            pipelineId,
                            buildId,
                            buildNo,
                            userId,
                            path,
                            md5,
                            size,
                            LocalDateTime.ofInstant(Date(createTime).toInstant(), ZoneId.systemDefault()),
                            LocalDateTime.ofInstant(Date(updateTime).toInstant(), ZoneId.systemDefault()),
                            version,
                            type)
                    .returning(ID)
                    .fetchOne()!!
            return data.id
        }
    }

    fun updateTask(
        dslContext: DSLContext,
        buildId: String,
        md5: String,
        status: Int,
        taskId: Long,
        scanUrl: String,
        result: String
    ) {
        with(TPluginJingangResult.T_PLUGIN_JINGANG_RESULT) {
            dslContext.insertInto(this,
                    BUILD_ID,
                    FILE_MD5,
                    TASK_ID,
                    RESULT)
                    .values(
                            buildId,
                            md5,
                            taskId,
                            result
                    )
                    .execute()
        }

        with(TPluginJingang.T_PLUGIN_JINGANG) {
            dslContext.update(this)
                    .set(UPDATE_TIME, LocalDateTime.now())
                    .set(STATUS, status)
                    .set(SCAN_URL, scanUrl)
                    .where(ID.eq(taskId))
                    .execute()
        }
    }

    fun getList(
        dslContext: DSLContext,
        projectId: String,
        page: Int,
        pageSize: Int
    ): Result<TPluginJingangRecord>? {
        with(TPluginJingang.T_PLUGIN_JINGANG) {
            return dslContext.selectFrom(this)
                    .where(PROJECT_ID.eq(projectId))
                    .orderBy(CREATE_TIME.desc())
                    .limit(pageSize).offset((page - 1) * pageSize)
                    .fetch()
        }
    }

    fun getTaskList(
        dslContext: DSLContext,
        projectIds: Set<String>
    ): Result<TPluginJingangRecord>? {

        with(TPluginJingang.T_PLUGIN_JINGANG) {
            val conditions = if (projectIds.isNotEmpty()) {
                listOf(PROJECT_ID.`in`(projectIds))
            } else {
                listOf()
            }

            return dslContext.selectFrom(this)
                    .where(conditions)
                    .fetch()
        }
    }

    fun getResultList(
        dslContext: DSLContext,
        taskIds: List<Long>
    ): Result<TPluginJingangResultRecord>? {

        return with(TPluginJingangResult.T_PLUGIN_JINGANG_RESULT) {
            dslContext.selectFrom(this)
                    .where(TASK_ID.`in`(taskIds))
                    .fetch()
        }
    }

    fun getCount(
        dslContext: DSLContext,
        projectId: String
    ): Int {
        with(TPluginJingang.T_PLUGIN_JINGANG) {
            return dslContext.selectCount().from(this)
                    .where(PROJECT_ID.eq(projectId))
                    .fetchOne()!!.get(0) as Int
        }
    }

    fun getTask(
        dslContext: DSLContext,
        taskId: Long
    ): TPluginJingangRecord? {
        with(TPluginJingang.T_PLUGIN_JINGANG) {
            return dslContext.selectFrom(this)
                    .where(ID.eq(taskId))
                    .fetchOne()
        }
    }

    fun getTaskResult(
        dslContext: DSLContext,
        taskId: Long
    ): TPluginJingangResultRecord? {
        with(TPluginJingangResult.T_PLUGIN_JINGANG_RESULT) {
            return dslContext.selectFrom(this)
                    .where(TASK_ID.eq(taskId))
                    .fetchOne()
        }
    }

    fun deleteTask(
        dslContext: DSLContext,
        taskId: Long
    ) {
        with(TPluginJingang.T_PLUGIN_JINGANG) {
            dslContext.deleteFrom(this)
                    .where(ID.eq(taskId))
                    .execute()
        }
    }

    fun timeOutJob(dslContext: DSLContext) {

        with(TPluginJingang.T_PLUGIN_JINGANG) {

            val ids = dslContext.selectFrom(this)
                    .where(CREATE_TIME.lt(LocalDateTime.now().minusDays(1)).and(STATUS.isNull))
                    .fetch()

            logger.info("timeout jod id(s): $ids")
            // 更新为超时
            dslContext.update(this)
                    .set(STATUS, -1)
                    .set(UPDATE_TIME, LocalDateTime.now())
                    .where(ID.`in`(ids.map { it.id }))
                    .execute()

            // 更新失败理由
//            dslContext.update(TPluginJingangResult.T_PLUGIN_JINGANG_RESULT)
//                    .set(TPluginJingangResult.T_PLUGIN_JINGANG_RESULT.RESULT, "time out")
//                    .where(TPluginJingangResult.T_PLUGIN_JINGANG_RESULT.TASK_ID.`in`(ids))
//                    .execute()
        }
    }
}
