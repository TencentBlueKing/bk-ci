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
import com.tencent.devops.common.db.utils.JooqUtils
import com.tencent.devops.common.pipeline.pojo.setting.PipelineRunLockType
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSubscriptionType
import com.tencent.devops.common.pipeline.pojo.setting.Subscription
import com.tencent.devops.model.process.tables.TPipelineSetting
import com.tencent.devops.model.process.tables.records.TPipelineSettingRecord
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record1
import org.jooq.Record4
import org.jooq.RecordMapper
import org.jooq.Result
import org.springframework.stereotype.Repository

@Suppress("ALL")
@Repository
class PipelineSettingDao {

    fun saveSetting(
        dslContext: DSLContext,
        setting: PipelineSetting,
        isTemplate: Boolean = false
    ): Int {
        with(TPipelineSetting.T_PIPELINE_SETTING) {
            // #6090 先查询存在情况再做刷新或插入
            val successSubscriptionList = setting.successSubscriptionList ?: emptyList()
            val failSubscriptionList = setting.failSubscriptionList ?: emptyList()
            val insert = dslContext.insertInto(
                this,
                PROJECT_ID,
                NAME,
                DESC,
                RUN_LOCK_TYPE,
                PIPELINE_ID,
                SUCCESS_RECEIVER,
                FAIL_RECEIVER,
                SUCCESS_GROUP,
                FAIL_GROUP,
                SUCCESS_TYPE,
                FAIL_TYPE,
                FAIL_WECHAT_GROUP_FLAG,
                FAIL_WECHAT_GROUP,
                FAIL_WECHAT_GROUP_MARKDOWN_FLAG,
                SUCCESS_WECHAT_GROUP_FLAG,
                SUCCESS_WECHAT_GROUP,
                SUCCESS_WECHAT_GROUP_MARKDOWN_FLAG,
                SUCCESS_DETAIL_FLAG,
                FAIL_DETAIL_FLAG,
                SUCCESS_CONTENT,
                FAIL_CONTENT,
                WAIT_QUEUE_TIME_SECOND,
                MAX_QUEUE_SIZE,
                IS_TEMPLATE,
                MAX_PIPELINE_RES_NUM,
                MAX_CON_RUNNING_QUEUE_SIZE,
                BUILD_NUM_RULE,
                CONCURRENCY_GROUP,
                CONCURRENCY_CANCEL_IN_PROGRESS,
                CLEAN_VARIABLES_WHEN_RETRY,
                SUCCESS_SUBSCRIPTION,
                FAILURE_SUBSCRIPTION,
                VERSION,
                PIPELINE_AS_CODE_SETTINGS,
                FAIL_IF_VARIABLE_INVALID
            ).values(
                setting.projectId,
                setting.pipelineName,
                setting.desc,
                PipelineRunLockType.toValue(setting.runLockType),
                setting.pipelineId,
                setting.successSubscription?.users,
                setting.failSubscription?.users,
                setting.successSubscription?.groups?.joinToString(","),
                setting.failSubscription?.groups?.joinToString(","),
                setting.successSubscription?.types?.joinToString(",") { it.name },
                setting.failSubscription?.types?.joinToString(",") { it.name },
                setting.failSubscription?.wechatGroupFlag ?: false,
                setting.failSubscription?.wechatGroup ?: "",
                setting.failSubscription?.wechatGroupMarkdownFlag ?: false,
                setting.successSubscription?.wechatGroupFlag ?: false,
                setting.successSubscription?.wechatGroup ?: "",
                setting.successSubscription?.wechatGroupMarkdownFlag ?: false,
                setting.successSubscription?.detailFlag ?: false,
                setting.failSubscription?.detailFlag ?: false,
                setting.successSubscription?.content,
                setting.failSubscription?.content,
                DateTimeUtil.minuteToSecond(setting.waitQueueTimeMinute),
                setting.maxQueueSize,
                isTemplate,
                setting.maxPipelineResNum,
                setting.maxConRunningQueueSize,
                setting.buildNumRule,
                setting.concurrencyGroup,
                setting.concurrencyCancelInProgress,
                setting.cleanVariablesWhenRetry,
                JsonUtil.toJson(successSubscriptionList, false),
                JsonUtil.toJson(failSubscriptionList, false),
                setting.version,
                setting.pipelineAsCodeSettings?.let { JsonUtil.toJson(it, false) },
                setting.failIfVariableInvalid
            ).onDuplicateKeyUpdate()
                .set(NAME, setting.pipelineName)
                .set(DESC, setting.desc)
                .set(RUN_LOCK_TYPE, PipelineRunLockType.toValue(setting.runLockType))
                .set(SUCCESS_RECEIVER, setting.successSubscription?.users)
                .set(FAIL_RECEIVER, setting.failSubscription?.users)
                .set(SUCCESS_GROUP, setting.successSubscription?.groups?.joinToString(","))
                .set(FAIL_GROUP, setting.failSubscription?.groups?.joinToString(","))
                .set(SUCCESS_TYPE, setting.successSubscription?.types?.joinToString(",") { it.name })
                .set(FAIL_TYPE, setting.failSubscription?.types?.joinToString(",") { it.name })
                .set(FAIL_WECHAT_GROUP_FLAG, setting.failSubscription?.wechatGroupFlag ?: false)
                .set(FAIL_WECHAT_GROUP, setting.failSubscription?.wechatGroup ?: "")
                .set(FAIL_WECHAT_GROUP_MARKDOWN_FLAG, setting.failSubscription?.wechatGroupMarkdownFlag ?: false)
                .set(SUCCESS_WECHAT_GROUP_FLAG, setting.successSubscription?.wechatGroupFlag ?: false)
                .set(SUCCESS_WECHAT_GROUP, setting.successSubscription?.wechatGroup ?: "")
                .set(SUCCESS_WECHAT_GROUP_MARKDOWN_FLAG, setting.successSubscription?.wechatGroupMarkdownFlag ?: false)
                .set(SUCCESS_DETAIL_FLAG, setting.successSubscription?.detailFlag ?: false)
                .set(FAIL_DETAIL_FLAG, setting.failSubscription?.detailFlag ?: false)
                .set(SUCCESS_CONTENT, setting.successSubscription?.content)
                .set(FAIL_CONTENT, setting.failSubscription?.content)
                .set(WAIT_QUEUE_TIME_SECOND, DateTimeUtil.minuteToSecond(setting.waitQueueTimeMinute))
                .set(MAX_QUEUE_SIZE, setting.maxQueueSize)
                .set(MAX_PIPELINE_RES_NUM, setting.maxPipelineResNum)
                .set(BUILD_NUM_RULE, setting.buildNumRule)
                .set(CONCURRENCY_GROUP, setting.concurrencyGroup)
                .set(CONCURRENCY_CANCEL_IN_PROGRESS, setting.concurrencyCancelInProgress)
                .set(CLEAN_VARIABLES_WHEN_RETRY, setting.cleanVariablesWhenRetry)
                .set(CLEAN_VARIABLES_WHEN_RETRY, setting.cleanVariablesWhenRetry)
                .set(SUCCESS_SUBSCRIPTION, JsonUtil.toJson(successSubscriptionList, false))
                .set(FAILURE_SUBSCRIPTION, JsonUtil.toJson(failSubscriptionList, false))
                .set(VERSION, setting.version)
                .set(MAX_CON_RUNNING_QUEUE_SIZE, setting.maxConRunningQueueSize)
                .set(FAIL_IF_VARIABLE_INVALID, setting.failIfVariableInvalid)
            // pipelineAsCodeSettings 默认传空不更新
            setting.pipelineAsCodeSettings?.let { self ->
                insert.set(PIPELINE_AS_CODE_SETTINGS, JsonUtil.toJson(self, false))
            }
            return insert.execute()
        }
    }

    fun getSetting(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String
    ): PipelineSetting? {
        with(TPipelineSetting.T_PIPELINE_SETTING) {
            return dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId)))
                .fetchOne(mapper)
        }
    }

    fun getPipelineAsCodeSettings(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String
    ): PipelineAsCodeSettings? {
        with(TPipelineSetting.T_PIPELINE_SETTING) {
            return dslContext.select(PIPELINE_AS_CODE_SETTINGS)
                .from(this)
                .where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId)))
                .fetchOne()?.component1()?.let { self ->
                    JsonUtil.to(self, PipelineAsCodeSettings::class.java)
                }
        }
    }

    fun getSettings(
        dslContext: DSLContext,
        pipelineIds: Set<String>,
        projectId: String? = null
    ): List<PipelineSetting> {
        with(TPipelineSetting.T_PIPELINE_SETTING) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PIPELINE_ID.`in`(pipelineIds))
            if (projectId != null) {
                conditions.add(PROJECT_ID.eq(projectId))
            }
            return dslContext.selectFrom(this)
                .where(conditions)
                .fetch(mapper)
        }
    }

    fun batchUpdate(dslContext: DSLContext, tPipelineSettingRecords: List<TPipelineSettingRecord>) {
        dslContext.batchUpdate(tPipelineSettingRecords).execute()
    }

    /**
     * 获取简单的数据(避免select大字段)
     *
     * @return PIPELINE_ID, DESC, RUN_LOCK_TYPE, BUILD_NUM_RULE
     */
    fun getSimpleSettings(
        dslContext: DSLContext,
        pipelineIds: Set<String>,
        projectId: String? = null
    ): Result<Record4<String, String, Int, String>> {
        with(TPipelineSetting.T_PIPELINE_SETTING) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PIPELINE_ID.`in`(pipelineIds))
            if (projectId != null) {
                conditions.add(PROJECT_ID.eq(projectId))
            }
            return dslContext.select(PIPELINE_ID, DESC, RUN_LOCK_TYPE, BUILD_NUM_RULE).from(this)
                .where(conditions)
                .fetch()
        }
    }

    fun getSetting(
        dslContext: DSLContext,
        projectId: String,
        name: String,
        pipelineId: String? = null,
        isTemplate: Boolean = false
    ): List<PipelineSetting> {
        with(TPipelineSetting.T_PIPELINE_SETTING) {
            val conditions = mutableListOf(
                PROJECT_ID.eq(projectId),
                NAME.eq(name),
                IS_TEMPLATE.eq(isTemplate)
            )
            if (!pipelineId.isNullOrBlank()) conditions.add(PIPELINE_ID.eq(pipelineId))
            return dslContext.selectFrom(this)
                .where(conditions)
                .fetch(mapper)
        }
    }

    /**
     * 更新模版引用的设置
     */
    fun updateSettingName(dslContext: DSLContext, pipelineIdList: List<String>, name: String) {
        with(TPipelineSetting.T_PIPELINE_SETTING) {
            dslContext.update(this)
                .set(NAME, name)
                .where(PIPELINE_ID.`in`(pipelineIdList))
                .execute()
        }
    }

    fun updateSetting(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        name: String,
        desc: String
    ): PipelineSetting? {
        with(TPipelineSetting.T_PIPELINE_SETTING) {
            val result = dslContext.update(this)
                .set(NAME, name)
                .set(DESC, desc)
                .where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId)))
                .returning().fetchOne()
            return mapper.map(result)
        }
    }

    fun getSettingByName(
        dslContext: DSLContext,
        name: String,
        projectId: String,
        pipelineId: String,
        isTemplate: Boolean = false
    ): Record1<Int>? {
        with(TPipelineSetting.T_PIPELINE_SETTING) {
            return dslContext.selectCount()
                .from(this)
                .where(
                    PROJECT_ID.eq(projectId).and(NAME.eq(name)).and(PIPELINE_ID.ne(pipelineId)).and(
                        IS_TEMPLATE.eq(
                            isTemplate
                        )
                    )
                )
                .fetchOne()
        }
    }

    fun delete(dslContext: DSLContext, projectId: String, pipelineId: String): Int {
        with(TPipelineSetting.T_PIPELINE_SETTING) {
            return dslContext.deleteFrom(this)
                .where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId)))
                .execute()
        }
    }

    fun updateMaxConRunningQueueSize(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        maxConRunningQueueSize: Int
    ): Int {
        with(TPipelineSetting.T_PIPELINE_SETTING) {
            return dslContext.update(this)
                .set(MAX_CON_RUNNING_QUEUE_SIZE, maxConRunningQueueSize)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(PROJECT_ID.eq(projectId))
                .execute()
        }
    }

    fun updatePipelineAsCodeSettings(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String?,
        pipelineAsCodeSettings: PipelineAsCodeSettings
    ): Int {
        with(TPipelineSetting.T_PIPELINE_SETTING) {
            val update = dslContext.update(this)
                .set(PIPELINE_AS_CODE_SETTINGS, JsonUtil.toJson(pipelineAsCodeSettings, false))
                .where(PROJECT_ID.eq(projectId))
            pipelineId?.let { self ->
                update.and(PIPELINE_ID.eq(self))
            }
            return update.execute()
        }
    }

    /**
     * 获取非继承项目的流水线列表
     */
    fun getNonInheritedPipelineIds(
        dslContext: DSLContext,
        projectId: String
    ): List<String> {
        with(TPipelineSetting.T_PIPELINE_SETTING) {
            var conditionsAnd = PIPELINE_AS_CODE_SETTINGS.isNotNull
            val inheritedDialectField =
                JooqUtils.jsonExtractAny<Boolean?>(PIPELINE_AS_CODE_SETTINGS, "$.inheritedDialect")
            // 不是继承项目的流水线列表
            conditionsAnd = conditionsAnd.and(inheritedDialectField.isNotNull).and(inheritedDialectField.isFalse)

            return dslContext.select(PIPELINE_ID).from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(conditionsAnd)
                .fetch(0, String::class.java)
        }
    }

    class PipelineSettingJooqMapper : RecordMapper<TPipelineSettingRecord, PipelineSetting> {
        override fun map(record: TPipelineSettingRecord?): PipelineSetting? {
            return record?.let { t ->
                val successType = t.successType?.split(",")?.filter { i -> i.isNotBlank() }
                    ?.map { type -> PipelineSubscriptionType.valueOf(type) }?.toMutableSet() ?: mutableSetOf()
                // 老数据兼容,老数据的启用企业微信群通知,转换成微信组通知类型
                if (t.successWechatGroupFlag == true) {
                    successType.add(PipelineSubscriptionType.WEWORK_GROUP)
                }
                val failType = t.failType?.split(",")?.filter { i -> i.isNotBlank() }
                    ?.map { type -> PipelineSubscriptionType.valueOf(type) }?.toMutableSet() ?: mutableSetOf()
                if (t.failWechatGroupFlag == true) {
                    failType.add(PipelineSubscriptionType.WEWORK_GROUP)
                }
                var oldSuccessSubscription = Subscription(
                    types = successType,
                    groups = t.successGroup?.split(",")?.toSet() ?: emptySet(),
                    users = t.successReceiver ?: "",
                    wechatGroupFlag = t.successWechatGroupFlag ?: false,
                    wechatGroup = t.successWechatGroup ?: "",
                    wechatGroupMarkdownFlag = t.successWechatGroupMarkdownFlag ?: false,
                    detailFlag = t.successDetailFlag ?: false,
                    content = t.successContent ?: ""
                ).takeIf { successType.isNotEmpty() }
                var oldFailSubscription = Subscription(
                    types = failType,
                    groups = t.failGroup?.split(",")?.toSet() ?: emptySet(),
                    users = t.failReceiver ?: "",
                    wechatGroupFlag = t.failWechatGroupFlag ?: false,
                    wechatGroup = t.failWechatGroup ?: "",
                    wechatGroupMarkdownFlag = t.failWechatGroupMarkdownFlag ?: false,
                    detailFlag = t.failDetailFlag ?: false,
                    content = t.failContent ?: ""
                ).takeIf { failType.isNotEmpty() }
                // 如果新数组有值，则老数据被替换为新数据的首个元素
                val successSubscriptionList = t.successSubscription?.let {
                    val list = JsonUtil.to(it, object : TypeReference<List<Subscription>>() {})
                    if (list.isNotEmpty()) {
                        oldSuccessSubscription = list.first()
                        list.map { s -> s.fixWeworkGroupType() }
                    } else null
                } ?: oldSuccessSubscription?.let { listOf(it) }
                val failSubscriptionList = t.failureSubscription?.let {
                    val list = JsonUtil.to(it, object : TypeReference<List<Subscription>>() {})
                    if (list.isNotEmpty()) {
                        oldFailSubscription = list.first()
                        list.map { s -> s.fixWeworkGroupType() }
                    } else null
                } ?: oldFailSubscription?.let { listOf(it) }
                PipelineSetting(
                    projectId = t.projectId,
                    pipelineId = t.pipelineId,
                    pipelineName = t.name,
                    desc = t.desc,
                    runLockType = PipelineRunLockType.valueOf(t.runLockType),
                    successSubscription = oldSuccessSubscription ?: Subscription(),
                    failSubscription = oldFailSubscription ?: Subscription(),
                    successSubscriptionList = successSubscriptionList,
                    failSubscriptionList = failSubscriptionList,
                    labels = emptyList(), // 标签不在本表保存，在写入和查询时需要通过 PipelineGroupService.kt
                    waitQueueTimeMinute = DateTimeUtil.secondToMinute(t.waitQueueTimeSecond?.toInt() ?: 600000),
                    maxQueueSize = t.maxQueueSize,
                    maxPipelineResNum = t.maxPipelineResNum,
                    maxConRunningQueueSize = t.maxConRunningQueueSize,
                    failIfVariableInvalid = t.failIfVariableInvalid,
                    buildNumRule = t.buildNumRule,
                    concurrencyCancelInProgress = t.concurrencyCancelInProgress,
                    concurrencyGroup = t.concurrencyGroup,
                    cleanVariablesWhenRetry = t.cleanVariablesWhenRetry,
                    pipelineAsCodeSettings = t.pipelineAsCodeSettings?.let { self ->
                        JsonUtil.to(self, PipelineAsCodeSettings::class.java)
                    },
                    version = t.version ?: 1
                )
            }
        }
    }

    companion object {
        private val mapper = PipelineSettingJooqMapper()
    }
}
