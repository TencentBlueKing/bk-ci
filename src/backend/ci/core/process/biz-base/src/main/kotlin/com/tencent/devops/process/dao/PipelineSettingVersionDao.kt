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

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.pojo.PipelineAsCodeSettings
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.pojo.setting.PipelineRunLockType
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.common.pipeline.pojo.setting.Subscription
import com.tencent.devops.model.process.tables.TPipelineSettingVersion
import com.tencent.devops.model.process.tables.records.TPipelineSettingVersionRecord
import com.tencent.devops.process.pojo.setting.PipelineSettingVersion
import org.jooq.DSLContext
import org.jooq.RecordMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository

@Suppress("LongParameterList")
@Repository
class PipelineSettingVersionDao {

    private val logger = LoggerFactory.getLogger(PipelineSettingVersionDao::class.java)

    fun saveSetting(
        dslContext: DSLContext,
        setting: PipelineSetting,
        version: Int,
        isTemplate: Boolean = false,
        id: Long? = null
    ): Int {
        val successSubscriptionList = setting.successSubscriptionList ?: emptyList()
        val failSubscriptionList = setting.failSubscriptionList ?: emptyList()
        with(TPipelineSettingVersion.T_PIPELINE_SETTING_VERSION) {
            return dslContext.insertInto(
                this,
                ID,
                PROJECT_ID,
                PIPELINE_ID,
                NAME,
                DESC,
                LABELS,
                RUN_LOCK_TYPE,
                WAIT_QUEUE_TIME_SECOND,
                MAX_QUEUE_SIZE,
                IS_TEMPLATE,
                BUILD_NUM_RULE,
                CONCURRENCY_GROUP,
                CONCURRENCY_CANCEL_IN_PROGRESS,
                SUCCESS_SUBSCRIPTION,
                FAILURE_SUBSCRIPTION,
                PIPELINE_AS_CODE_SETTINGS,
                VERSION,
                MAX_CON_RUNNING_QUEUE_SIZE,
                FAIL_IF_VARIABLE_INVALID
            ).values(
                id,
                setting.projectId,
                setting.pipelineId,
                setting.pipelineName,
                setting.desc,
                setting.labels.let { self ->
                    JsonUtil.toJson(self, false)
                },
                PipelineRunLockType.toValue(setting.runLockType),
                DateTimeUtil.minuteToSecond(setting.waitQueueTimeMinute),
                setting.maxQueueSize,
                isTemplate,
                setting.buildNumRule,
                setting.concurrencyGroup,
                setting.concurrencyCancelInProgress,
                JsonUtil.toJson(successSubscriptionList, false),
                JsonUtil.toJson(failSubscriptionList, false),
                setting.pipelineAsCodeSettings?.let { self ->
                    JsonUtil.toJson(self, false)
                },
                version,
                setting.maxConRunningQueueSize ?: -1,
                setting.failIfVariableInvalid
            ).onDuplicateKeyUpdate()
                .set(NAME, setting.pipelineName)
                .set(DESC, setting.desc)
                .set(LABELS, setting.labels.let { self -> JsonUtil.toJson(self, false) })
                .set(RUN_LOCK_TYPE, PipelineRunLockType.toValue(setting.runLockType))
                .set(WAIT_QUEUE_TIME_SECOND, DateTimeUtil.minuteToSecond(setting.waitQueueTimeMinute))
                .set(MAX_QUEUE_SIZE, setting.maxQueueSize)
                .set(BUILD_NUM_RULE, setting.buildNumRule)
                .set(CONCURRENCY_GROUP, setting.concurrencyGroup)
                .set(CONCURRENCY_CANCEL_IN_PROGRESS, setting.concurrencyCancelInProgress)
                .set(SUCCESS_SUBSCRIPTION, JsonUtil.toJson(successSubscriptionList, false))
                .set(FAILURE_SUBSCRIPTION, JsonUtil.toJson(failSubscriptionList, false))
                .set(MAX_CON_RUNNING_QUEUE_SIZE, setting.maxConRunningQueueSize ?: -1)
                .set(PIPELINE_AS_CODE_SETTINGS, setting.pipelineAsCodeSettings?.let { self ->
                    JsonUtil.toJson(self, false)
                })
                .set(FAIL_IF_VARIABLE_INVALID, setting.failIfVariableInvalid)
                .execute()
        }
    }

    fun getSettingVersion(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        version: Int
    ): PipelineSettingVersion? {
        with(TPipelineSettingVersion.T_PIPELINE_SETTING_VERSION) {
            return dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(VERSION.eq(version))
                .and(PROJECT_ID.eq(projectId))
                .fetchOne(mapper)
        }
    }

    fun getLatestSettingVersion(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String
    ): PipelineSettingVersion? {
        with(TPipelineSettingVersion.T_PIPELINE_SETTING_VERSION) {
            return dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(PROJECT_ID.eq(projectId))
                .orderBy(VERSION.desc()).limit(1)
                .fetchOne(mapper)
        }
    }

    fun getSettingByPipelineIds(
        dslContext: DSLContext,
        pipelineIds: List<String>
    ): List<PipelineSettingVersion> {
        with(TPipelineSettingVersion.T_PIPELINE_SETTING_VERSION) {
            return dslContext.selectFrom(this)
                .where(PIPELINE_ID.`in`(pipelineIds))
                .fetch(mapper)
        }
    }

    fun batchUpdate(dslContext: DSLContext, tPipelineSettingVersionRecords: List<TPipelineSettingVersionRecord>) {
        dslContext.batchUpdate(tPipelineSettingVersionRecords).execute()
    }

    fun updateSetting(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        version: Int,
        name: String,
        desc: String
    ) {
        with(TPipelineSettingVersion.T_PIPELINE_SETTING_VERSION) {
            dslContext.update(this)
                .set(NAME, name)
                .set(DESC, desc)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(PROJECT_ID.eq(projectId))
                .and(VERSION.eq(version))
                .execute()
        }
    }

    fun deleteAllVersion(dslContext: DSLContext, projectId: String, pipelineId: String): Int {
        with(TPipelineSettingVersion.T_PIPELINE_SETTING_VERSION) {
            return dslContext.deleteFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(PROJECT_ID.eq(projectId))
                .execute()
        }
    }

    fun deleteByVer(dslContext: DSLContext, projectId: String, pipelineId: String, version: Int): Int {
        with(TPipelineSettingVersion.T_PIPELINE_SETTING_VERSION) {
            return dslContext.deleteFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(VERSION.eq(version))
                .and(PROJECT_ID.eq(projectId))
                .execute()
        }
    }

    fun deleteEarlyVersion(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        currentVersion: Int,
        maxPipelineResNum: Int
    ): Int {
        return with(TPipelineSettingVersion.T_PIPELINE_SETTING_VERSION) {
            dslContext.deleteFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(VERSION.le(currentVersion - maxPipelineResNum))
                .and(PROJECT_ID.eq(projectId))
                .execute()
        }
    }

    class PipelineSettingVersionJooqMapper : RecordMapper<TPipelineSettingVersionRecord, PipelineSettingVersion> {
        override fun map(record: TPipelineSettingVersionRecord?): PipelineSettingVersion? {
            return record?.let { t ->
                PipelineSettingVersion(
                    projectId = t.projectId,
                    pipelineId = t.pipelineId,
                    pipelineName = t.name,
                    desc = t.desc,
                    runLockType = t.runLockType?.let { PipelineRunLockType.valueOf(it) },
                    successSubscriptionList = t.successSubscription?.let {
                        JsonUtil.to(it, object : TypeReference<List<Subscription>>() {})
                            .map { s -> s.fixWeworkGroupType() }
                    },
                    failSubscriptionList = t.failureSubscription?.let {
                        JsonUtil.to(it, object : TypeReference<List<Subscription>>() {})
                            .map { s -> s.fixWeworkGroupType() }
                    },
                    version = t.version,
                    labels = t.labels?.let { self ->
                        JsonUtil.getObjectMapper().readValue(self) as List<String>
                    },
                    waitQueueTimeMinute = DateTimeUtil.secondToMinute(t.waitQueueTimeSecond ?: 600000),
                    maxQueueSize = t.maxQueueSize,
                    buildNumRule = t.buildNumRule,
                    concurrencyCancelInProgress = t.concurrencyCancelInProgress,
                    concurrencyGroup = t.concurrencyGroup,
                    maxConRunningQueueSize = t.maxConRunningQueueSize,
                    pipelineAsCodeSettings = t.pipelineAsCodeSettings?.let { self ->
                        JsonUtil.to(self, PipelineAsCodeSettings::class.java)
                    },
                    failIfVariableInvalid = t.failIfVariableInvalid
                )
            }
        }
    }

    companion object {
        private val mapper = PipelineSettingVersionJooqMapper()
    }
}
