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

import com.tencent.devops.common.api.pojo.PipelineAsCodeSettings
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.notify.enums.NotifyType
import com.tencent.devops.model.process.tables.TPipelineSetting
import com.tencent.devops.model.process.tables.records.TPipelineSettingRecord
import com.tencent.devops.process.pojo.setting.PipelineRunLockType
import com.tencent.devops.process.pojo.setting.PipelineSetting
import com.tencent.devops.process.util.NotifyTemplateUtils
import com.tencent.devops.process.utils.PIPELINE_RES_NUM_MIN
import com.tencent.devops.process.utils.PIPELINE_SETTING_MAX_QUEUE_SIZE_DEFAULT
import com.tencent.devops.process.utils.PIPELINE_SETTING_WAIT_QUEUE_TIME_MINUTE_DEFAULT
import com.tencent.devops.process.utils.PIPELINE_START_USER_NAME
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record1
import org.jooq.Record4
import org.jooq.Result
import org.springframework.stereotype.Repository

@Suppress("ALL")
@Repository
class PipelineSettingDao {

    // 新流水线创建的时候，设置默认的通知配置。
    fun insertNewSetting(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        pipelineName: String,
        isTemplate: Boolean = false,
        successNotifyTypes: String = "",
        failNotifyTypes: String = "${NotifyType.EMAIL.name},${NotifyType.RTX.name}",
        maxPipelineResNum: Int? = PIPELINE_RES_NUM_MIN,
        pipelineAsCodeSettings: PipelineAsCodeSettings?
    ): Int {
        with(TPipelineSetting.T_PIPELINE_SETTING) {
            return dslContext.insertInto(
                this,
                PROJECT_ID,
                PIPELINE_ID,
                NAME,
                RUN_LOCK_TYPE,
                DESC,
                SUCCESS_RECEIVER,
                FAIL_RECEIVER,
                SUCCESS_GROUP,
                FAIL_GROUP,
                SUCCESS_TYPE,
                FAIL_TYPE,
                SUCCESS_CONTENT,
                FAIL_CONTENT,
                WAIT_QUEUE_TIME_SECOND,
                MAX_QUEUE_SIZE,
                IS_TEMPLATE,
                MAX_PIPELINE_RES_NUM,
                PIPELINE_AS_CODE_SETTINGS
            )
                .values(
                    projectId,
                    pipelineId,
                    pipelineName,
                    PipelineRunLockType.toValue(PipelineRunLockType.MULTIPLE),
                    "",
                    "\${$PIPELINE_START_USER_NAME}",
                    "\${$PIPELINE_START_USER_NAME}",
                    "",
                    "",
                    successNotifyTypes,
                    failNotifyTypes,
                    NotifyTemplateUtils.getCommonShutdownSuccessContent(),
                    NotifyTemplateUtils.getCommonShutdownFailureContent(),
                    DateTimeUtil.minuteToSecond(PIPELINE_SETTING_WAIT_QUEUE_TIME_MINUTE_DEFAULT),
                    PIPELINE_SETTING_MAX_QUEUE_SIZE_DEFAULT,
                    isTemplate,
                    maxPipelineResNum,
                    pipelineAsCodeSettings?.let { self ->
                        JsonUtil.toJson(self, false)
                    }
                )
                .execute()
        }
    }

    fun saveSetting(dslContext: DSLContext, setting: PipelineSetting, isTemplate: Boolean = false): Int {
        with(TPipelineSetting.T_PIPELINE_SETTING) {
            // #6090 先查询存在情况再做刷新或插入
            val origin = getSetting(dslContext, setting.projectId, setting.pipelineId)
            return if (origin == null) {
                dslContext.insertInto(
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
                    PIPELINE_AS_CODE_SETTINGS
                ).values(
                    setting.projectId,
                    setting.pipelineName,
                    setting.desc,
                    PipelineRunLockType.toValue(setting.runLockType),
                    setting.pipelineId,
                    setting.successSubscription.users,
                    setting.failSubscription.users,
                    setting.successSubscription.groups.joinToString(","),
                    setting.failSubscription.groups.joinToString(","),
                    setting.successSubscription.types.joinToString(",") { it.name },
                    setting.failSubscription.types.joinToString(",") { it.name },
                    setting.failSubscription.wechatGroupFlag,
                    setting.failSubscription.wechatGroup,
                    setting.failSubscription.wechatGroupMarkdownFlag,
                    setting.successSubscription.wechatGroupFlag,
                    setting.successSubscription.wechatGroup,
                    setting.successSubscription.wechatGroupMarkdownFlag,
                    setting.successSubscription.detailFlag,
                    setting.failSubscription.detailFlag,
                    setting.successSubscription.content,
                    setting.failSubscription.content,
                    DateTimeUtil.minuteToSecond(setting.waitQueueTimeMinute),
                    setting.maxQueueSize,
                    isTemplate,
                    setting.maxPipelineResNum,
                    setting.maxConRunningQueueSize,
                    setting.buildNumRule,
                    setting.concurrencyGroup,
                    setting.concurrencyCancelInProgress,
                    setting.cleanVariablesWhenRetry,
                    setting.pipelineAsCodeSettings?.let { self ->
                        JsonUtil.toJson(self, false)
                    }
                ).execute()
            } else {
                val updateSetMoreStep = dslContext.update(this)
                    .set(NAME, setting.pipelineName)
                    .set(DESC, setting.desc)
                    .set(RUN_LOCK_TYPE, PipelineRunLockType.toValue(setting.runLockType))
                    .set(SUCCESS_RECEIVER, setting.successSubscription.users)
                    .set(FAIL_RECEIVER, setting.failSubscription.users)
                    .set(SUCCESS_GROUP, setting.successSubscription.groups.joinToString(","))
                    .set(FAIL_GROUP, setting.failSubscription.groups.joinToString(","))
                    .set(SUCCESS_TYPE, setting.successSubscription.types.joinToString(",") { it.name })
                    .set(FAIL_TYPE, setting.failSubscription.types.joinToString(",") { it.name })
                    .set(FAIL_WECHAT_GROUP_FLAG, setting.failSubscription.wechatGroupFlag)
                    .set(FAIL_WECHAT_GROUP, setting.failSubscription.wechatGroup)
                    .set(FAIL_WECHAT_GROUP_MARKDOWN_FLAG, setting.failSubscription.wechatGroupMarkdownFlag)
                    .set(SUCCESS_WECHAT_GROUP_FLAG, setting.successSubscription.wechatGroupFlag)
                    .set(SUCCESS_WECHAT_GROUP, setting.successSubscription.wechatGroup)
                    .set(SUCCESS_WECHAT_GROUP_MARKDOWN_FLAG, setting.successSubscription.wechatGroupMarkdownFlag)
                    .set(SUCCESS_DETAIL_FLAG, setting.successSubscription.detailFlag)
                    .set(FAIL_DETAIL_FLAG, setting.failSubscription.detailFlag)
                    .set(SUCCESS_CONTENT, setting.successSubscription.content)
                    .set(FAIL_CONTENT, setting.failSubscription.content)
                    .set(WAIT_QUEUE_TIME_SECOND, DateTimeUtil.minuteToSecond(setting.waitQueueTimeMinute))
                    .set(MAX_QUEUE_SIZE, setting.maxQueueSize)
                    .set(MAX_PIPELINE_RES_NUM, setting.maxPipelineResNum)
                    .set(BUILD_NUM_RULE, setting.buildNumRule)
                    .set(CONCURRENCY_GROUP, setting.concurrencyGroup)
                    .set(CONCURRENCY_CANCEL_IN_PROGRESS, setting.concurrencyCancelInProgress)
                    .set(CLEAN_VARIABLES_WHEN_RETRY, setting.cleanVariablesWhenRetry)
                // pipelineAsCodeSettings 默认传空不更新
                setting.pipelineAsCodeSettings?.let { self ->
                    updateSetMoreStep.set(PIPELINE_AS_CODE_SETTINGS, JsonUtil.toJson(self, false))
                }
                // maxConRunningQueueSize 默认传空不更新
                if (setting.maxConRunningQueueSize != null) {
                    updateSetMoreStep.set(MAX_CON_RUNNING_QUEUE_SIZE, setting.maxConRunningQueueSize)
                }
                updateSetMoreStep
                    .where(PIPELINE_ID.eq(setting.pipelineId).and(PROJECT_ID.eq(setting.projectId)))
                    .execute()
            }
        }
    }

    fun getSetting(dslContext: DSLContext, projectId: String, pipelineId: String): TPipelineSettingRecord? {
        with(TPipelineSetting.T_PIPELINE_SETTING) {
            return dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId)))
                .fetchOne()
        }
    }

    fun getSettings(
        dslContext: DSLContext,
        pipelineIds: Set<String>,
        projectId: String? = null
    ): Result<TPipelineSettingRecord> {
        with(TPipelineSetting.T_PIPELINE_SETTING) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PIPELINE_ID.`in`(pipelineIds))
            if (projectId != null) {
                conditions.add(PROJECT_ID.eq(projectId))
            }
            return dslContext.selectFrom(this)
                .where(conditions)
                .fetch()
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
    ): Result<TPipelineSettingRecord> {
        with(TPipelineSetting.T_PIPELINE_SETTING) {
            val conditions =
                mutableListOf<Condition>(
                    PROJECT_ID.eq(projectId),
                    NAME.eq(name),
                    IS_TEMPLATE.eq(isTemplate)
                )
            if (!pipelineId.isNullOrBlank()) conditions.add(PIPELINE_ID.eq(pipelineId))
            return dslContext.selectFrom(this)
                .where(conditions)
                .fetch()
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

    fun updateSetting(dslContext: DSLContext, projectId: String, pipelineId: String, name: String, desc: String) {
        with(TPipelineSetting.T_PIPELINE_SETTING) {
            dslContext.update(this)
                .set(NAME, name)
                .set(DESC, desc)
                .where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId)))
                .execute()
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
}
