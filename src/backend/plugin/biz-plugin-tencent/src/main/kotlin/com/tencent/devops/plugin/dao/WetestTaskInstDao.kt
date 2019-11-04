/*
 * Tencent is pleased to support the open source community by making BK-REPO 蓝鲸制品库 available.
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

package com.tencent.devops.plugin.dao

import com.tencent.devops.model.plugin.tables.TPluginWetestTaskInst
import com.tencent.devops.model.plugin.tables.records.TPluginWetestTaskInstRecord
import com.tencent.devops.plugin.pojo.wetest.WetestInstStatus
import com.tencent.devops.plugin.pojo.wetest.WetestTaskInst
import com.tencent.devops.plugin.service.WetestTaskInstService
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Result
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date

@Repository
class WetestTaskInstDao {

    companion object {
        private val logger = LoggerFactory.getLogger(WetestTaskInstService::class.java)
    }

    fun insert(
        dslContext: DSLContext,
        wetestTaskInst: WetestTaskInst
    ): Int {
        with(TPluginWetestTaskInst.T_PLUGIN_WETEST_TASK_INST) {
            return dslContext.insertInto(this,
                    TEST_ID,
                    PROJECT_ID,
                    PIPELINE_ID,
                    BUILD_ID,
                    BUILD_NO,
                    NAME,
                    VERSION,
                    PASSING_RATE,
                    TASK_ID,
                    TEST_TYPE,
                    SCRIPT_TYPE,
                    IS_SYNC,
                    SOURCE_PATH,
                    ACCOUNT_PATH,
                    SCRIPT_PATH,
                    SOURCE_TYPE,
                    SCRIPT_PATH_TYPE,
                    ACCOUNT_PATH_TYPE,
                    IS_PRIVATE_CLOUD,
                    START_USER,
                    BEGIN_TIME,
                    STATUS,
                    EMAIL_GROUP_ID)
                    .values(
                            wetestTaskInst.testId,
                            wetestTaskInst.projectId,
                            wetestTaskInst.pipelineId,
                            wetestTaskInst.buildId,
                            wetestTaskInst.buildNo,
                            wetestTaskInst.name,
                            wetestTaskInst.version,
                            wetestTaskInst.passingRate,
                            wetestTaskInst.taskId.toInt(),
                            wetestTaskInst.testType,
                            wetestTaskInst.scriptType,
                            wetestTaskInst.synchronized,
                            wetestTaskInst.sourcePath,
                            wetestTaskInst.scriptPath,
                            wetestTaskInst.accountFile,
                            wetestTaskInst.sourceType,
                            wetestTaskInst.scriptSourceType,
                            wetestTaskInst.accountSourceType,
                            wetestTaskInst.privateCloud,
                            wetestTaskInst.startUserId,
                            LocalDateTime.ofInstant(Date(wetestTaskInst.beginTime).toInstant(), ZoneId.systemDefault()),
                            WetestInstStatus.RUNNING.name,
                            wetestTaskInst.emailGroupId)
                    .execute()
        }
    }

    fun updateTaskInstStatus(dslContext: DSLContext, testId: String, status: WetestInstStatus, passRate: String?): Int {
        with(TPluginWetestTaskInst.T_PLUGIN_WETEST_TASK_INST) {
            return dslContext.update(this)
                    .set(END_TIME, LocalDateTime.now())
                    .set(STATUS, status.name)
                    .set(PASSING_RATE, passRate ?: "")
                    .where(TEST_ID.eq(testId))
                    .execute()
        }
    }

    fun getUnfinishTask(dslContext: DSLContext): Result<TPluginWetestTaskInstRecord>? {
        with(TPluginWetestTaskInst.T_PLUGIN_WETEST_TASK_INST) {
            return dslContext.selectFrom(this)
                    .where(STATUS.eq(WetestInstStatus.RUNNING.name).and(IS_SYNC.eq("0")))
                    .fetch()
        }
    }

    // 默认不分页
    fun getTaskInst(dslContext: DSLContext, projectId: String, pipelineIds: Set<String>? = null, versions: Set<String>? = null, offset: Int? = null, limit: Int? = null): Result<TPluginWetestTaskInstRecord>? {
        with(TPluginWetestTaskInst.T_PLUGIN_WETEST_TASK_INST) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PROJECT_ID.eq(projectId))
            if (pipelineIds != null && pipelineIds.isNotEmpty()) {
                conditions.add(PIPELINE_ID.`in`(pipelineIds))
            }
            if (versions != null && versions.isNotEmpty()) {
                conditions.add(VERSION.`in`(versions))
            }
            val sqlExec = dslContext.selectFrom(this)
                    .where(conditions)
            if (limit != null && offset != null) {
                sqlExec.limit(limit).offset(offset)
            }
            sqlExec.orderBy(BEGIN_TIME.desc())
            return sqlExec.fetch()
        }
    }

    fun getTaskInstCount(dslContext: DSLContext, projectId: String, pipelineIds: Set<String>? = null, versions: Set<String>? = null): Int {
        with(TPluginWetestTaskInst.T_PLUGIN_WETEST_TASK_INST) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PROJECT_ID.eq(projectId))
            if (pipelineIds != null && pipelineIds.isNotEmpty()) {
                conditions.add(PIPELINE_ID.`in`(pipelineIds))
            }
            if (versions != null && versions.isNotEmpty()) {
                conditions.add(VERSION.`in`(versions))
            }
            return dslContext.selectCount().from(this)
                    .where(conditions)
                    .fetchOne().value1()
        }
    }

    // 通过pipelineId与buildId查询执行信息,默认不分页
    fun getTaskInstByBuildId(dslContext: DSLContext, projectId: String, pipelineId: String? = null, buildId: String? = null, offset: Int? = null, limit: Int? = null): Result<TPluginWetestTaskInstRecord>? {
        with(TPluginWetestTaskInst.T_PLUGIN_WETEST_TASK_INST) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PROJECT_ID.eq(projectId))
            if (pipelineId != null && pipelineId.isNotEmpty()) {
                conditions.add(PIPELINE_ID.eq(pipelineId))
            }
            if (buildId != null && buildId.isNotEmpty()) {
                conditions.add(BUILD_ID.eq(buildId))
            }
            val sqlExec = dslContext.selectFrom(this)
                    .where(conditions)
            if (limit != null && offset != null) {
                sqlExec.limit(limit).offset(offset)
            }
            sqlExec.orderBy(BEGIN_TIME.desc())
            return sqlExec.fetch()
        }
    }

    fun getTaskInstCountByBuildId(dslContext: DSLContext, projectId: String, pipelineId: String? = null, buildId: String? = null): Int {
        with(TPluginWetestTaskInst.T_PLUGIN_WETEST_TASK_INST) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PROJECT_ID.eq(projectId))
            if (pipelineId != null && pipelineId.isNotEmpty()) {
                conditions.add(PIPELINE_ID.eq(pipelineId))
            }
            if (buildId != null && buildId.isNotEmpty()) {
                conditions.add(BUILD_ID.eq(buildId))
            }
            return dslContext.selectCount().from(this)
                    .where(conditions)
                    .fetchOne().value1()
        }
    }
}

/**

ALTER TABLE T_PLUGIN_WETEST_TASK_INST ADD COLUMN `EMAIL_GROUP_ID` INT(11) NULL;

**/