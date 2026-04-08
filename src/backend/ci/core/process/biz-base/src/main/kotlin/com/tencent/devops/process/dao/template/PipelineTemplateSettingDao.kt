package com.tencent.devops.process.dao.template

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.pojo.PipelineAsCodeSettings
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.toLocalDateTimeOrDefault
import com.tencent.devops.common.pipeline.pojo.setting.BuildCancelPolicy
import com.tencent.devops.common.pipeline.pojo.setting.PipelineRunLockType
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.common.pipeline.pojo.setting.Subscription
import com.tencent.devops.common.pipeline.utils.PIPELINE_RES_NUM_MIN
import com.tencent.devops.model.process.tables.TPipelineTemplateSettingVersion
import com.tencent.devops.model.process.tables.records.TPipelineTemplateSettingVersionRecord
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateSettingCommonCondition
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateSettingUpdateInfo
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class PipelineTemplateSettingDao {
    fun createOrUpdate(
        dslContext: DSLContext,
        record: PipelineSetting
    ) {
        val successSubscriptionList = record.successSubscriptionList ?: emptyList()
        val failSubscriptionList = record.failSubscriptionList ?: emptyList()
        val waitQueueTimeSecond = DateTimeUtil.minuteToSecond(record.waitQueueTimeMinute)
        val labelStr = JsonUtil.toJson(record.labels)
        val pipelineAsCodeSettings = record.pipelineAsCodeSettings?.let { self -> JsonUtil.toJson(self, false) }
        with(TPipelineTemplateSettingVersion.T_PIPELINE_TEMPLATE_SETTING_VERSION) {
            dslContext.insertInto(
                this,
                PROJECT_ID,
                TEMPLATE_ID,
                SETTING_VERSION,
                NAME,
                DESC,
                LABELS,
                WAIT_QUEUE_TIME_SECOND,
                MAX_QUEUE_SIZE,
                BUILD_NUM_RULE,
                CONCURRENCY_GROUP,
                CONCURRENCY_CANCEL_IN_PROGRESS,
                PIPELINE_AS_CODE_SETTINGS,
                SUCCESS_SUBSCRIPTION,
                FAILURE_SUBSCRIPTION,
                RUN_LOCK_TYPE,
                MAX_CON_RUNNING_QUEUE_SIZE,
                FAIL_IF_VARIABLE_INVALID,
                BUILD_CANCEL_POLICY,
                CREATOR,
                UPDATER,
                CREATED_TIME,
                UPDATE_TIME
            ).values(
                record.projectId,
                record.pipelineId,
                record.version,
                record.pipelineName,
                record.desc,
                labelStr,
                waitQueueTimeSecond,
                record.maxQueueSize,
                record.buildNumRule,
                record.concurrencyGroup,
                record.concurrencyCancelInProgress,
                pipelineAsCodeSettings,
                JsonUtil.toJson(successSubscriptionList),
                JsonUtil.toJson(failSubscriptionList),
                PipelineRunLockType.toValue(record.runLockType),
                record.maxConRunningQueueSize ?: 50,
                record.failIfVariableInvalid,
                record.buildCancelPolicy.value,
                record.creator,
                record.updater,
                record.createdTime.toLocalDateTimeOrDefault(),
                record.updateTime.toLocalDateTimeOrDefault()
            ).onDuplicateKeyUpdate()
                .set(NAME, record.pipelineName)
                .set(DESC, record.desc)
                .set(WAIT_QUEUE_TIME_SECOND, waitQueueTimeSecond)
                .set(LABELS, labelStr)
                .set(MAX_QUEUE_SIZE, record.maxQueueSize)
                .set(BUILD_NUM_RULE, record.buildNumRule)
                .set(CONCURRENCY_GROUP, record.concurrencyGroup)
                .set(CONCURRENCY_CANCEL_IN_PROGRESS, record.concurrencyCancelInProgress)
                .set(PIPELINE_AS_CODE_SETTINGS, pipelineAsCodeSettings)
                .set(SUCCESS_SUBSCRIPTION, JsonUtil.toJson(successSubscriptionList, false))
                .set(FAILURE_SUBSCRIPTION, JsonUtil.toJson(failSubscriptionList, false))
                .set(RUN_LOCK_TYPE, PipelineRunLockType.toValue(record.runLockType))
                .set(MAX_CON_RUNNING_QUEUE_SIZE, record.maxConRunningQueueSize)
                .set(FAIL_IF_VARIABLE_INVALID, record.failIfVariableInvalid)
                .set(BUILD_CANCEL_POLICY, record.buildCancelPolicy.value)
                .set(UPDATER, record.updater)
                .set(UPDATE_TIME, record.updateTime.toLocalDateTimeOrDefault())
                .execute()
        }
    }

    fun update(
        dslContext: DSLContext,
        record: PipelineTemplateSettingUpdateInfo,
        commonCondition: PipelineTemplateSettingCommonCondition
    ) {
        with(TPipelineTemplateSettingVersion.T_PIPELINE_TEMPLATE_SETTING_VERSION) {
            val now = LocalDateTime.now()
            dslContext.update(this)
                .apply {
                    record.labels?.let { set(LABELS, JsonUtil.toJson(it)) }
                    record.waitQueueTimeMinute?.let { set(WAIT_QUEUE_TIME_SECOND, DateTimeUtil.minuteToSecond(it)) }
                    record.maxQueueSize?.let { set(MAX_QUEUE_SIZE, it) }
                    record.buildNumRule?.let { set(BUILD_NUM_RULE, it) }
                    record.concurrencyGroup?.let { set(CONCURRENCY_GROUP, it) }
                    record.concurrencyCancelInProgress?.let { set(CONCURRENCY_CANCEL_IN_PROGRESS, it) }
                    record.pipelineAsCodeSettings?.let { set(PIPELINE_AS_CODE_SETTINGS, JsonUtil.toJson(it)) }
                    record.runLockType?.let { set(RUN_LOCK_TYPE, PipelineRunLockType.toValue(record.runLockType)) }
                    record.maxConRunningQueueSize?.let { set(MAX_CON_RUNNING_QUEUE_SIZE, it) }
                    record.failIfVariableInvalid?.let { set(FAIL_IF_VARIABLE_INVALID, it) }
                    record.buildCancelPolicy?.let { set(BUILD_CANCEL_POLICY, it.value) }
                    if (!record.successSubscriptionList.isNullOrEmpty()) {
                        set(SUCCESS_SUBSCRIPTION, JsonUtil.toJson(record.successSubscriptionList!!, false))
                    }
                    if (!record.failSubscriptionList.isNullOrEmpty()) {
                        set(FAILURE_SUBSCRIPTION, JsonUtil.toJson(record.failSubscriptionList!!, false))
                    }
                    if (!record.name.isNullOrBlank()) {
                        set(NAME, record.name)
                    }
                    if (!record.desc.isNullOrBlank()) {
                        set(DESC, record.desc)
                    }
                    record.updater?.let { set(UPDATER, record.updater) }
                }
                .set(UPDATE_TIME, now)
                .where(buildQueryCondition(commonCondition))
                .execute()
        }
    }

    fun list(
        dslContext: DSLContext,
        commonCondition: PipelineTemplateSettingCommonCondition,
        limit: Int? = null,
        offset: Int? = null
    ): List<PipelineSetting> {
        return with(TPipelineTemplateSettingVersion.T_PIPELINE_TEMPLATE_SETTING_VERSION) {
            dslContext.selectFrom(this)
                .where(buildQueryCondition(commonCondition))
                .let { if (limit != null && offset != null) it.limit(limit).offset(offset) else it }
                .fetch().map { it.convert() }
        }
    }

    fun get(
        dslContext: DSLContext,
        projectId: String,
        templateId: String,
        settingVersion: Int
    ): PipelineSetting? {
        return with(TPipelineTemplateSettingVersion.T_PIPELINE_TEMPLATE_SETTING_VERSION) {
            dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(TEMPLATE_ID.eq(templateId))
                .and(SETTING_VERSION.eq(settingVersion))
                .fetchOne()?.convert()
        }
    }

    fun get(
        dslContext: DSLContext,
        commonCondition: PipelineTemplateSettingCommonCondition
    ): PipelineSetting? {
        return with(TPipelineTemplateSettingVersion.T_PIPELINE_TEMPLATE_SETTING_VERSION) {
            dslContext.selectFrom(this)
                .where(buildQueryCondition(commonCondition))
                .fetchOne()?.convert()
        }
    }

    fun count(
        dslContext: DSLContext,
        commonCondition: PipelineTemplateSettingCommonCondition
    ): Int {
        return with(TPipelineTemplateSettingVersion.T_PIPELINE_TEMPLATE_SETTING_VERSION) {
            dslContext.selectCount().from(this)
                .where(buildQueryCondition(commonCondition))
                .fetchOne(0, Int::class.java)!!
        }
    }

    fun delete(
        dslContext: DSLContext,
        commonCondition: PipelineTemplateSettingCommonCondition
    ) {
        return with(TPipelineTemplateSettingVersion.T_PIPELINE_TEMPLATE_SETTING_VERSION) {
            dslContext.deleteFrom(this)
                .where(buildQueryCondition(commonCondition))
                .execute()
        }
    }

    @Suppress("NestedBlockDepth")
    fun buildQueryCondition(
        commonCondition: PipelineTemplateSettingCommonCondition
    ): MutableList<Condition> {
        return with(TPipelineTemplateSettingVersion.T_PIPELINE_TEMPLATE_SETTING_VERSION) {
            with(commonCondition) {
                val conditions = mutableListOf<Condition>()
                conditions.add(PROJECT_ID.eq(projectId))
                if (!name.isNullOrBlank()) conditions.add(NAME.eq(name))
                // 优先处理批量查询条件
                if (!templateVersionPairs.isNullOrEmpty()) {
                    val pairConditions = templateVersionPairs!!.map { pair ->
                        DSL.row(TEMPLATE_ID, SETTING_VERSION).eq(DSL.row(pair.templateId, pair.version))
                    }
                    conditions.add(DSL.or(pairConditions))
                } else {
                    // 如果没有批量查询条件，则处理单个 templateId 和 settingVersion 条件
                    if (templateId != null) conditions.add(TEMPLATE_ID.eq(templateId))
                    if (settingVersion != null) conditions.add(SETTING_VERSION.eq(settingVersion))
                }
                if (creator != null) conditions.add(CREATOR.eq(creator))
                if (updater != null) conditions.add(UPDATER.eq(updater))
                conditions
            }
        }
    }

    fun pruneLatestVersions(
        dslContext: DSLContext,
        projectId: String,
        templateId: String,
        limit: Int
    ) {
        if (limit <= 0) {
            return
        }
        with(TPipelineTemplateSettingVersion.T_PIPELINE_TEMPLATE_SETTING_VERSION) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId).and(TEMPLATE_ID.eq(templateId)))
                .orderBy(SETTING_VERSION.desc()) // 按版本号降序
                .limit(limit) // 限制数量
                .execute()
        }
    }

    fun TPipelineTemplateSettingVersionRecord.convert(): PipelineSetting {
        val successSubscriptionList = this.successSubscription?.let {
            JsonUtil.to(it, object : TypeReference<List<Subscription>>() {})
        }
        val failSubscriptionList = this.failureSubscription?.let {
            JsonUtil.to(it, object : TypeReference<List<Subscription>>() {})
        }
        return PipelineSetting(
            projectId = this.projectId,
            pipelineId = this.templateId,
            pipelineName = this.name,
            desc = this.desc,
            runLockType = this.runLockType?.let { PipelineRunLockType.valueOf(it) } ?: PipelineRunLockType.SINGLE_LOCK,
            successSubscriptionList = successSubscriptionList,
            failSubscriptionList = failSubscriptionList,
            version = this.settingVersion,
            labels = this.labels?.let {
                JsonUtil.to(it, object : TypeReference<List<String>>() {})
            } ?: emptyList(),
            waitQueueTimeMinute = DateTimeUtil.secondToMinute(this.waitQueueTimeSecond ?: 600000),
            maxQueueSize = this.maxQueueSize,
            buildNumRule = this.buildNumRule,
            concurrencyCancelInProgress = this.concurrencyCancelInProgress,
            concurrencyGroup = this.concurrencyGroup,
            maxConRunningQueueSize = this.maxConRunningQueueSize,
            failIfVariableInvalid = this.failIfVariableInvalid,
            pipelineAsCodeSettings = this.pipelineAsCodeSettings?.let { self ->
                JsonUtil.to(self, PipelineAsCodeSettings::class.java)
            },
            buildCancelPolicy = BuildCancelPolicy.parse(this.buildCancelPolicy),
            maxPipelineResNum = PIPELINE_RES_NUM_MIN,
            creator = creator,
            updater = updater
        )
    }
}
