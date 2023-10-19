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
import com.tencent.devops.common.api.pojo.PipelineAsCodeSettings
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.notify.enums.NotifyType
import com.tencent.devops.common.pipeline.pojo.setting.PipelineRunLockType
import com.tencent.devops.model.process.tables.TPipelineSettingVersion
import com.tencent.devops.model.process.tables.records.TPipelineSettingVersionRecord
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.process.pojo.setting.PipelineSettingVersion
import com.tencent.devops.common.pipeline.pojo.setting.Subscription
import org.jooq.DSLContext
import org.jooq.RecordMapper
import org.springframework.stereotype.Repository

@Suppress("LongParameterList")
@Repository
class PipelineSettingVersionDao {

    // 新流水线创建的时候，设置默认的通知配置。
    fun insertNewSetting(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        pipelineName: String,
        settingVersion: Int,
        isTemplate: Boolean = false,
        successNotifyTypes: String = "",
        failNotifyTypes: String = "${NotifyType.EMAIL.name},${NotifyType.RTX.name}",
        id: Long? = null
    ): Int {
        with(TPipelineSettingVersion.T_PIPELINE_SETTING_VERSION) {
            return dslContext.insertInto(
                this,
                PROJECT_ID,
                PIPELINE_ID,
                NAME,
                IS_TEMPLATE,
                VERSION,
                ID
            )
                .values(
                    projectId,
                    pipelineId,
                    pipelineName,
                    isTemplate,
                    settingVersion,
                    id
                )
                .execute()
        }
    }

    fun saveSetting(
        dslContext: DSLContext,
        setting: PipelineSetting,
        version: Int,
        isTemplate: Boolean = false,
        id: Long? = null
    ): Int {
        val successSubscriptionList = setting.successSubscriptionList ?: listOf(setting.successSubscription)
        val failSubscriptionList = setting.failSubscriptionList ?: listOf(setting.failSubscription)
        with(TPipelineSettingVersion.T_PIPELINE_SETTING_VERSION) {
            return dslContext.insertInto(
                this,
                PROJECT_ID,
                PIPELINE_ID,
                NAME,
                IS_TEMPLATE,
                VERSION,
                ID,
                SUCCESS_SUBSCRIPTION,
                FAILURE_SUBSCRIPTION
            ).values(
                setting.projectId,
                setting.pipelineId,
                setting.pipelineName,
                isTemplate,
                version,
                id,
                JsonUtil.toJson(successSubscriptionList, false),
                JsonUtil.toJson(failSubscriptionList, false)
            ).onDuplicateKeyUpdate()
                .set(SUCCESS_SUBSCRIPTION, JsonUtil.toJson(successSubscriptionList, false))
                .set(FAILURE_SUBSCRIPTION, JsonUtil.toJson(failSubscriptionList, false))
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
                val successSubscriptionList = t.successSubscription?.let {
                    JsonUtil.to(it, object : TypeReference<List<Subscription>>() {})
                }
                val failSubscriptionList = t.failureSubscription?.let {
                    JsonUtil.to(it, object : TypeReference<List<Subscription>>() {})
                }
                PipelineSettingVersion(
                    projectId = t.projectId,
                    pipelineId = t.pipelineId,
                    pipelineName = t.name,
                    desc = t.desc,
                    runLockType = PipelineRunLockType.valueOf(t.runLockType),
                    failSubscriptionList = failSubscriptionList,
                    labels = emptyList(),
                    waitQueueTimeMinute = DateTimeUtil.secondToMinute(t.waitQueueTimeSecond ?: 600000),
                    maxQueueSize = t.maxQueueSize,
                    buildNumRule = t.buildNumRule,
                    concurrencyCancelInProgress = t.concurrencyCancelInProgress,
                    concurrencyGroup = t.concurrencyGroup,
                    pipelineAsCodeSettings = t.pipelineAsCodeSettings?.let { self ->
                        JsonUtil.to(self, PipelineAsCodeSettings::class.java)
                    }
                )
            }
        }
    }

    companion object {
        private val mapper = PipelineSettingVersionJooqMapper()
    }
}
