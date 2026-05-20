package com.tencent.devops.process.dao

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.pojo.PipelineAsCodeSettings
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.pojo.setting.BuildCancelPolicy
import com.tencent.devops.common.pipeline.pojo.setting.PipelineRunLockType
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.common.pipeline.pojo.setting.Subscription
import com.tencent.devops.model.process.Tables.T_PIPELINE_SETTING_DRAFT_VERSION
import com.tencent.devops.model.process.tables.TPipelineSettingDraftVersion
import com.tencent.devops.model.process.tables.records.TPipelineSettingDraftVersionRecord
import com.tencent.devops.process.pojo.setting.PipelineSettingDraftVersion
import org.jooq.DSLContext
import org.jooq.RecordMapper
import org.springframework.stereotype.Repository

@Repository
class PipelineSettingDraftVersionDao {

    fun create(
        dslContext: DSLContext,
        setting: PipelineSetting,
        version: Int,
        draftVersion: Int
    ) {
        val successSubscriptionList = setting.successSubscriptionList ?: emptyList()
        val failSubscriptionList = setting.failSubscriptionList ?: emptyList()
        with(TPipelineSettingDraftVersion.T_PIPELINE_SETTING_DRAFT_VERSION) {
            dslContext.insertInto(
                this,
                PROJECT_ID,
                PIPELINE_ID,
                VERSION,
                SETTING_VERSION,
                DRAFT_VERSION,
                NAME,
                DESC,
                LABELS,
                RUN_LOCK_TYPE,
                WAIT_QUEUE_TIME_SECOND,
                MAX_QUEUE_SIZE,
                BUILD_NUM_RULE,
                CONCURRENCY_GROUP,
                CONCURRENCY_CANCEL_IN_PROGRESS,
                SUCCESS_SUBSCRIPTION,
                FAILURE_SUBSCRIPTION,
                PIPELINE_AS_CODE_SETTINGS,
                MAX_CON_RUNNING_QUEUE_SIZE,
                FAIL_IF_VARIABLE_INVALID,
                BUILD_CANCEL_POLICY
            ).values(
                setting.projectId,
                setting.pipelineId,
                version,
                setting.version,
                draftVersion,
                setting.pipelineName,
                setting.desc,
                setting.labels.let { JsonUtil.toJson(it, false) },
                PipelineRunLockType.toValue(setting.runLockType),
                DateTimeUtil.minuteToSecond(setting.waitQueueTimeMinute), // 转换为秒
                setting.maxQueueSize,
                setting.buildNumRule,
                setting.concurrencyGroup,
                setting.concurrencyCancelInProgress,
                JsonUtil.toJson(successSubscriptionList, false),
                JsonUtil.toJson(failSubscriptionList, false),
                setting.pipelineAsCodeSettings?.let { JsonUtil.toJson(it, false) },
                setting.maxConRunningQueueSize,
                setting.failIfVariableInvalid,
                setting.buildCancelPolicy.value
            ).execute()
        }
    }

    fun get(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        version: Int,
        draftVersion: Int
    ): PipelineSettingDraftVersion? {
        with(T_PIPELINE_SETTING_DRAFT_VERSION) {
            return dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(PROJECT_ID.eq(projectId))
                .and(VERSION.eq(version))
                .and(DRAFT_VERSION.eq(draftVersion))
                .fetchOne(mapper)
        }
    }

    fun update(
        dslContext: DSLContext,
        setting: PipelineSetting,
        version: Int,
        draftVersion: Int
    ) {
        val successSubscriptionList = setting.successSubscriptionList ?: emptyList()
        val failSubscriptionList = setting.failSubscriptionList ?: emptyList()
        with(T_PIPELINE_SETTING_DRAFT_VERSION) {
            dslContext.update(this)
                .set(NAME, setting.pipelineName)
                .set(DESC, setting.desc)
                .set(LABELS, setting.labels.let { JsonUtil.toJson(it, false) })
                .set(RUN_LOCK_TYPE, PipelineRunLockType.toValue(setting.runLockType))
                .set(WAIT_QUEUE_TIME_SECOND, DateTimeUtil.minuteToSecond(setting.waitQueueTimeMinute))
                .set(MAX_QUEUE_SIZE, setting.maxQueueSize)
                .set(BUILD_NUM_RULE, setting.buildNumRule)
                .set(CONCURRENCY_GROUP, setting.concurrencyGroup)
                .set(CONCURRENCY_CANCEL_IN_PROGRESS, setting.concurrencyCancelInProgress)
                .set(SUCCESS_SUBSCRIPTION, JsonUtil.toJson(successSubscriptionList, false))
                .set(FAILURE_SUBSCRIPTION, JsonUtil.toJson(failSubscriptionList, false))
                .set(PIPELINE_AS_CODE_SETTINGS, setting.pipelineAsCodeSettings?.let { JsonUtil.toJson(it, false) })
                .set(MAX_CON_RUNNING_QUEUE_SIZE, setting.maxConRunningQueueSize)
                .set(FAIL_IF_VARIABLE_INVALID, setting.failIfVariableInvalid)
                .set(BUILD_CANCEL_POLICY, setting.buildCancelPolicy.value)
                .where(PROJECT_ID.eq(setting.projectId))
                .and(PIPELINE_ID.eq(setting.pipelineId))
                .and(VERSION.eq(version))
                .and(DRAFT_VERSION.eq(draftVersion))
                .execute()
        }
    }

    private class PipelineSettingDraftVersionJooqMapper :
        RecordMapper<TPipelineSettingDraftVersionRecord, PipelineSettingDraftVersion> {
        override fun map(record: TPipelineSettingDraftVersionRecord?): PipelineSettingDraftVersion? {
            return record?.let { r ->
                PipelineSettingDraftVersion(
                    projectId = r.projectId,
                    pipelineId = r.pipelineId,
                    version = r.version,
                    settingVersion = r.settingVersion,
                    draftVersion = r.draftVersion,
                    pipelineName = r.name,
                    desc = r.desc,
                    labels = r.labels?.let { self ->
                        JsonUtil.getObjectMapper().readValue(self) as List<String>
                    },
                    buildNumRule = r.buildNumRule,
                    successSubscriptionList = r.successSubscription?.let {
                        JsonUtil.to(it, object : TypeReference<List<Subscription>>() {})
                            .map { s -> s.fixWeworkGroupType() }
                    },
                    failSubscriptionList = r.failureSubscription?.let {
                        JsonUtil.to(it, object : TypeReference<List<Subscription>>() {})
                            .map { s -> s.fixWeworkGroupType() }
                    },
                    runLockType = r.runLockType?.let { PipelineRunLockType.valueOf(it) },
                    waitQueueTimeSecond = r.waitQueueTimeSecond,
                    maxQueueSize = r.maxQueueSize,
                    concurrencyGroup = r.concurrencyGroup,
                    concurrencyCancelInProgress = r.concurrencyCancelInProgress,
                    pipelineAsCodeSettings = r.pipelineAsCodeSettings?.let {
                        JsonUtil.to(it, PipelineAsCodeSettings::class.java)
                    },
                    maxConRunningQueueSize = r.maxConRunningQueueSize,
                    failIfVariableInvalid = r.failIfVariableInvalid,
                    buildCancelPolicy = BuildCancelPolicy.parse(r.buildCancelPolicy)
                )
            }
        }
    }

    companion object {
        private val mapper = PipelineSettingDraftVersionJooqMapper()
    }
}
