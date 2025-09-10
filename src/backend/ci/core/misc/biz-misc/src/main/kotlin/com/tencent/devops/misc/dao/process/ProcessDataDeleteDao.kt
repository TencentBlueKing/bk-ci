/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.misc.dao.process

import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.pojo.element.quality.QualityGateInElement
import com.tencent.devops.common.pipeline.pojo.element.quality.QualityGateOutElement
import com.tencent.devops.model.process.tables.TAuditResource
import com.tencent.devops.model.process.tables.TPipelineBuildContainer
import com.tencent.devops.model.process.tables.TPipelineBuildDetail
import com.tencent.devops.model.process.tables.TPipelineBuildHistory
import com.tencent.devops.model.process.tables.TPipelineBuildHistoryDebug
import com.tencent.devops.model.process.tables.TPipelineBuildRecordContainer
import com.tencent.devops.model.process.tables.TPipelineBuildRecordModel
import com.tencent.devops.model.process.tables.TPipelineBuildRecordStage
import com.tencent.devops.model.process.tables.TPipelineBuildRecordTask
import com.tencent.devops.model.process.tables.TPipelineBuildStage
import com.tencent.devops.model.process.tables.TPipelineBuildSummary
import com.tencent.devops.model.process.tables.TPipelineBuildTask
import com.tencent.devops.model.process.tables.TPipelineBuildTemplateAcrossInfo
import com.tencent.devops.model.process.tables.TPipelineBuildVar
import com.tencent.devops.model.process.tables.TPipelineCallback
import com.tencent.devops.model.process.tables.TPipelineFavor
import com.tencent.devops.model.process.tables.TPipelineGroup
import com.tencent.devops.model.process.tables.TPipelineInfo
import com.tencent.devops.model.process.tables.TPipelineJobMutexGroup
import com.tencent.devops.model.process.tables.TPipelineLabel
import com.tencent.devops.model.process.tables.TPipelineLabelPipeline
import com.tencent.devops.model.process.tables.TPipelineModelTask
import com.tencent.devops.model.process.tables.TPipelineOperationLog
import com.tencent.devops.model.process.tables.TPipelinePauseValue
import com.tencent.devops.model.process.tables.TPipelineRecentUse
import com.tencent.devops.model.process.tables.TPipelineRemoteAuth
import com.tencent.devops.model.process.tables.TPipelineResource
import com.tencent.devops.model.process.tables.TPipelineResourceVersion
import com.tencent.devops.model.process.tables.TPipelineSetting
import com.tencent.devops.model.process.tables.TPipelineSettingVersion
import com.tencent.devops.model.process.tables.TPipelineSubRef
import com.tencent.devops.model.process.tables.TPipelineTimer
import com.tencent.devops.model.process.tables.TPipelineTimerBranch
import com.tencent.devops.model.process.tables.TPipelineTriggerDetail
import com.tencent.devops.model.process.tables.TPipelineTriggerEvent
import com.tencent.devops.model.process.tables.TPipelineTriggerReview
import com.tencent.devops.model.process.tables.TPipelineView
import com.tencent.devops.model.process.tables.TPipelineViewGroup
import com.tencent.devops.model.process.tables.TPipelineViewTop
import com.tencent.devops.model.process.tables.TPipelineViewUserLastView
import com.tencent.devops.model.process.tables.TPipelineViewUserSettings
import com.tencent.devops.model.process.tables.TPipelineWebhook
import com.tencent.devops.model.process.tables.TPipelineWebhookBuildParameter
import com.tencent.devops.model.process.tables.TPipelineWebhookQueue
import com.tencent.devops.model.process.tables.TPipelineWebhookVersion
import com.tencent.devops.model.process.tables.TPipelineYamlBranchFile
import com.tencent.devops.model.process.tables.TPipelineYamlInfo
import com.tencent.devops.model.process.tables.TPipelineYamlSync
import com.tencent.devops.model.process.tables.TPipelineYamlVersion
import com.tencent.devops.model.process.tables.TPipelineYamlView
import com.tencent.devops.model.process.tables.TProjectPipelineCallback
import com.tencent.devops.model.process.tables.TProjectPipelineCallbackHistory
import com.tencent.devops.model.process.tables.TReport
import com.tencent.devops.model.process.tables.TTemplate
import com.tencent.devops.model.process.tables.TTemplatePipeline
import org.jooq.Condition
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Suppress("TooManyFunctions", "LargeClass")
@Repository
class ProcessDataDeleteDao {

    fun deleteAuditResource(
        dslContext: DSLContext,
        projectId: String,
        resourceType: String? = null,
        resourceId: String? = null
    ) {
        with(TAuditResource.T_AUDIT_RESOURCE) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PROJECT_ID.eq(projectId))
            if (!resourceType.isNullOrBlank()) {
                conditions.add(RESOURCE_TYPE.eq(resourceType))
            }
            if (!resourceId.isNullOrBlank()) {
                conditions.add(RESOURCE_ID.eq(resourceId))
            }
            dslContext.deleteFrom(this)
                .where(conditions)
                .execute()
        }
    }

    fun deletePipelineBuildContainer(dslContext: DSLContext, projectId: String, pipelineId: String) {
        with(TPipelineBuildContainer.T_PIPELINE_BUILD_CONTAINER) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId).and(PIPELINE_ID.eq(pipelineId)))
                .execute()
        }
    }

    fun deletePipelineBuildContainer(dslContext: DSLContext, projectId: String, buildIds: List<String>) {
        with(TPipelineBuildContainer.T_PIPELINE_BUILD_CONTAINER) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId).and(BUILD_ID.`in`(buildIds)))
                .execute()
        }
    }

    fun deletePipelineBuildHistory(dslContext: DSLContext, projectId: String, buildId: String): Int {
        with(TPipelineBuildHistory.T_PIPELINE_BUILD_HISTORY) {
            return dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId).and(BUILD_ID.eq(buildId)))
                .execute()
        }
    }

    fun deletePipelineBuildHistory(dslContext: DSLContext, projectId: String, pipelineIds: List<String>) {
        with(TPipelineBuildHistory.T_PIPELINE_BUILD_HISTORY) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId).and(PIPELINE_ID.`in`(pipelineIds)))
                .execute()
        }
    }

    fun deletePipelineBuildDetail(dslContext: DSLContext, projectId: String, buildIds: List<String>) {
        with(TPipelineBuildDetail.T_PIPELINE_BUILD_DETAIL) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId).and(BUILD_ID.`in`(buildIds)))
                .execute()
        }
    }

    fun deletePipelineBuildStage(dslContext: DSLContext, projectId: String, pipelineId: String) {
        with(TPipelineBuildStage.T_PIPELINE_BUILD_STAGE) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId).and(PIPELINE_ID.eq(pipelineId)))
                .execute()
        }
    }

    fun deletePipelineBuildStage(dslContext: DSLContext, projectId: String, buildIds: List<String>) {
        with(TPipelineBuildStage.T_PIPELINE_BUILD_STAGE) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId).and(BUILD_ID.`in`(buildIds)))
                .execute()
        }
    }

    fun deletePipelineBuildTask(dslContext: DSLContext, projectId: String, buildIds: List<String>) {
        with(TPipelineBuildTask.T_PIPELINE_BUILD_TASK) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId).and(BUILD_ID.`in`(buildIds)))
                .execute()
        }
    }

    fun deletePipelineBuildSummary(dslContext: DSLContext, projectId: String, pipelineIds: List<String>) {
        with(TPipelineBuildSummary.T_PIPELINE_BUILD_SUMMARY) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId).and(PIPELINE_ID.`in`(pipelineIds)))
                .execute()
        }
    }

    fun deletePipelineBuildVar(dslContext: DSLContext, projectId: String, buildIds: List<String>) {
        with(TPipelineBuildVar.T_PIPELINE_BUILD_VAR) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId).and(BUILD_ID.`in`(buildIds)))
                .execute()
        }
    }

    fun deletePipelineFavor(dslContext: DSLContext, projectId: String, pipelineId: String) {
        with(TPipelineFavor.T_PIPELINE_FAVOR) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId).and(PIPELINE_ID.eq(pipelineId)))
                .execute()
        }
    }

    fun deletePipelineGroup(dslContext: DSLContext, projectId: String) {
        with(TPipelineGroup.T_PIPELINE_GROUP) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .execute()
        }
    }

    fun deletePipelineInfo(dslContext: DSLContext, projectId: String, pipelineIds: List<String>) {
        with(TPipelineInfo.T_PIPELINE_INFO) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId).and(PIPELINE_ID.`in`(pipelineIds)))
                .execute()
        }
    }

    fun deletePipelineJobMutexGroup(dslContext: DSLContext, projectId: String) {
        with(TPipelineJobMutexGroup.T_PIPELINE_JOB_MUTEX_GROUP) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .execute()
        }
    }

    fun deletePipelineLabel(dslContext: DSLContext, projectId: String) {
        with(TPipelineLabel.T_PIPELINE_LABEL) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .execute()
        }
    }

    fun deletePipelineLabelPipeline(dslContext: DSLContext, projectId: String, pipelineIds: List<String>) {
        with(TPipelineLabelPipeline.T_PIPELINE_LABEL_PIPELINE) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId).and(PIPELINE_ID.`in`(pipelineIds)))
                .execute()
        }
    }

    fun deletePipelineModelTask(dslContext: DSLContext, projectId: String, pipelineIds: List<String>) {
        with(TPipelineModelTask.T_PIPELINE_MODEL_TASK) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId).and(PIPELINE_ID.`in`(pipelineIds)))
                .execute()
        }
    }

    fun deletePipelinePauseValue(dslContext: DSLContext, projectId: String, buildIds: List<String>) {
        with(TPipelinePauseValue.T_PIPELINE_PAUSE_VALUE) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId).and(BUILD_ID.`in`(buildIds)))
                .execute()
        }
    }

    fun deletePipelineResource(dslContext: DSLContext, projectId: String, pipelineIds: List<String>) {
        with(TPipelineResource.T_PIPELINE_RESOURCE) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId).and(PIPELINE_ID.`in`(pipelineIds)))
                .execute()
        }
    }

    fun deletePipelineResourceVersion(dslContext: DSLContext, projectId: String, pipelineIds: List<String>) {
        with(TPipelineResourceVersion.T_PIPELINE_RESOURCE_VERSION) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId).and(PIPELINE_ID.`in`(pipelineIds)))
                .execute()
        }
    }

    fun deletePipelineSetting(dslContext: DSLContext, projectId: String, pipelineIds: List<String>? = null) {
        with(TPipelineSetting.T_PIPELINE_SETTING) {
            val conditions = mutableListOf(PROJECT_ID.eq(projectId)).apply {
                pipelineIds?.let { add(PIPELINE_ID.`in`(pipelineIds)) }
            }
            dslContext.deleteFrom(this)
                .where(conditions)
                .execute()
        }
    }

    fun deletePipelineSettingVersion(dslContext: DSLContext, projectId: String, pipelineIds: List<String>? = null) {
        with(TPipelineSettingVersion.T_PIPELINE_SETTING_VERSION) {
            val conditions = mutableListOf(PROJECT_ID.eq(projectId)).apply {
                pipelineIds?.let { add(PIPELINE_ID.`in`(pipelineIds)) }
            }
            dslContext.deleteFrom(this)
                .where(conditions)
                .execute()
        }
    }

    fun deletePipelineView(dslContext: DSLContext, projectId: String) {
        with(TPipelineView.T_PIPELINE_VIEW) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .execute()
        }
    }

    fun deletePipelineViewUserLastView(dslContext: DSLContext, projectId: String) {
        with(TPipelineViewUserLastView.T_PIPELINE_VIEW_USER_LAST_VIEW) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .execute()
        }
    }

    fun deletePipelineViewUserSettings(dslContext: DSLContext, projectId: String) {
        with(TPipelineViewUserSettings.T_PIPELINE_VIEW_USER_SETTINGS) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .execute()
        }
    }

    fun deletePipelineWebhookQueue(dslContext: DSLContext, projectId: String, buildIds: List<String>) {
        with(TPipelineWebhookQueue.T_PIPELINE_WEBHOOK_QUEUE) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId).and(BUILD_ID.`in`(buildIds)))
                .execute()
        }
    }

    fun deleteProjectPipelineCallback(dslContext: DSLContext, projectId: String) {
        with(TProjectPipelineCallback.T_PROJECT_PIPELINE_CALLBACK) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .execute()
        }
    }

    fun deleteProjectPipelineCallbackHistory(dslContext: DSLContext, projectId: String) {
        with(TProjectPipelineCallbackHistory.T_PROJECT_PIPELINE_CALLBACK_HISTORY) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .execute()
        }
    }

    fun deleteReport(dslContext: DSLContext, projectId: String, pipelineId: String, buildIds: List<String>) {
        with(TReport.T_REPORT) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId).and(PIPELINE_ID.eq(pipelineId)).and(BUILD_ID.`in`(buildIds)))
                .execute()
        }
    }

    fun deleteTemplate(dslContext: DSLContext, projectId: String) {
        with(TTemplate.T_TEMPLATE) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .execute()
        }
    }

    fun deleteTemplatePipeline(dslContext: DSLContext, projectId: String, pipelineIds: List<String>) {
        with(TTemplatePipeline.T_TEMPLATE_PIPELINE) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId).and(PIPELINE_ID.`in`(pipelineIds)))
                .execute()
        }
    }

    fun deletePipelineBuildTemplateAcrossInfo(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildIds: List<String>
    ) {
        with(TPipelineBuildTemplateAcrossInfo.T_PIPELINE_BUILD_TEMPLATE_ACROSS_INFO) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId).and(PIPELINE_ID.eq(pipelineId)).and(BUILD_ID.`in`(buildIds)))
                .execute()
        }
    }

    fun deletePipelineWebhookBuildParameter(dslContext: DSLContext, projectId: String, buildIds: List<String>) {
        with(TPipelineWebhookBuildParameter.T_PIPELINE_WEBHOOK_BUILD_PARAMETER) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId).and(BUILD_ID.`in`(buildIds)))
                .execute()
        }
    }

    fun deletePipelineTriggerReview(dslContext: DSLContext, projectId: String, buildIds: List<String>) {
        with(TPipelineTriggerReview.T_PIPELINE_TRIGGER_REVIEW) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId).and(BUILD_ID.`in`(buildIds)))
                .execute()
        }
    }

    fun deletePipelineViewGroup(dslContext: DSLContext, projectId: String, pipelineId: String) {
        with(TPipelineViewGroup.T_PIPELINE_VIEW_GROUP) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId).and(PIPELINE_ID.eq(pipelineId)))
                .execute()
        }
    }

    fun deletePipelineViewTop(dslContext: DSLContext, projectId: String) {
        with(TPipelineViewTop.T_PIPELINE_VIEW_TOP) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .execute()
        }
    }

    fun deletePipelineRecentUse(dslContext: DSLContext, projectId: String, pipelineId: String) {
        with(TPipelineRecentUse.T_PIPELINE_RECENT_USE) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId).and(PIPELINE_ID.eq(pipelineId)))
                .execute()
        }
    }

    fun deletePipelineBuildRecordContainer(dslContext: DSLContext, projectId: String, buildIds: List<String>) {
        with(TPipelineBuildRecordContainer.T_PIPELINE_BUILD_RECORD_CONTAINER) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId).and(BUILD_ID.`in`(buildIds)))
                .execute()
        }
    }

    fun deletePipelineBuildRecordModel(dslContext: DSLContext, projectId: String, buildIds: List<String>) {
        with(TPipelineBuildRecordModel.T_PIPELINE_BUILD_RECORD_MODEL) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId).and(BUILD_ID.`in`(buildIds)))
                .execute()
        }
    }

    fun deletePipelineBuildRecordStage(dslContext: DSLContext, projectId: String, buildIds: List<String>) {
        with(TPipelineBuildRecordStage.T_PIPELINE_BUILD_RECORD_STAGE) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId).and(BUILD_ID.`in`(buildIds)))
                .execute()
        }
    }

    fun deletePipelineBuildRecordTask(
        dslContext: DSLContext,
        projectId: String,
        buildIds: List<String>,
        skipTaskDeleteFlag: Boolean? = null
    ) {
        with(TPipelineBuildRecordTask.T_PIPELINE_BUILD_RECORD_TASK) {
            val conditions = mutableListOf<Condition>().apply {
                add(PROJECT_ID.eq(projectId))
                if (buildIds.isNotEmpty()) {
                    add(BUILD_ID.`in`(buildIds))
                }
                if (skipTaskDeleteFlag == true) {
                    // 为了构建详情页组装数据方便，skip状态的post和质量红线相关task记录不删除
                    add(STATUS.eq(BuildStatus.SKIP.name))
                    add(POST_INFO.isNull)
                    add(
                        CLASS_TYPE.notIn(
                            listOf(
                                QualityGateInElement.classType,
                                QualityGateOutElement.classType
                            )
                        )
                    )
                }
            }
            dslContext.deleteFrom(this)
                .where(conditions)
                .execute()
        }
    }

    fun deletePipelineTriggerDetail(dslContext: DSLContext, projectId: String, pipelineId: String) {
        with(TPipelineTriggerDetail.T_PIPELINE_TRIGGER_DETAIL) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId).and(PIPELINE_ID.eq(pipelineId)))
                .execute()
        }
    }

    fun deletePipelineAuditResource(dslContext: DSLContext, projectId: String, pipelineId: String) {
        with(TAuditResource.T_AUDIT_RESOURCE) {
            dslContext.deleteFrom(this)
                .where(
                    PROJECT_ID.eq(projectId)
                        .and(RESOURCE_TYPE.eq(AuthResourceType.PIPELINE_DEFAULT.value)).and(RESOURCE_ID.eq(pipelineId))
                )
                .execute()
        }
    }

    fun deletePipelineRemoteAuth(dslContext: DSLContext, projectId: String, pipelineId: String) {
        with(TPipelineRemoteAuth.T_PIPELINE_REMOTE_AUTH) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId).and(PIPELINE_ID.eq(pipelineId)))
                .execute()
        }
    }

    fun deletePipelineWebhook(dslContext: DSLContext, projectId: String, pipelineId: String) {
        with(TPipelineWebhook.T_PIPELINE_WEBHOOK) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId).and(PIPELINE_ID.eq(pipelineId)))
                .execute()
        }
    }

    fun deletePipelineTimer(dslContext: DSLContext, projectId: String, pipelineId: String) {
        with(TPipelineTimer.T_PIPELINE_TIMER) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId).and(PIPELINE_ID.eq(pipelineId)))
                .execute()
        }
    }

    fun deletePipelineTriggerEvent(dslContext: DSLContext, projectId: String) {
        with(TPipelineTriggerEvent.T_PIPELINE_TRIGGER_EVENT) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .execute()
        }
    }

    fun deletePipelineBuildHistoryDebug(dslContext: DSLContext, projectId: String, pipelineIds: List<String>) {
        with(TPipelineBuildHistoryDebug.T_PIPELINE_BUILD_HISTORY_DEBUG) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId).and(PIPELINE_ID.`in`(pipelineIds)))
                .execute()
        }
    }

    fun deletePipelineTimerBranch(dslContext: DSLContext, projectId: String, pipelineId: String) {
        with(TPipelineTimerBranch.T_PIPELINE_TIMER_BRANCH) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId).and(PIPELINE_ID.eq(pipelineId)))
                .execute()
        }
    }

    fun deletePipelineYamlInfo(dslContext: DSLContext, projectId: String, pipelineId: String) {
        with(TPipelineYamlInfo.T_PIPELINE_YAML_INFO) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId).and(PIPELINE_ID.eq(pipelineId)))
                .execute()
        }
    }

    fun deletePipelineYamlVersion(dslContext: DSLContext, projectId: String, pipelineId: String) {
        with(TPipelineYamlVersion.T_PIPELINE_YAML_VERSION) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId).and(PIPELINE_ID.eq(pipelineId)))
                .execute()
        }
    }

    fun deletePipelineYamlSync(dslContext: DSLContext, projectId: String) {
        with(TPipelineYamlSync.T_PIPELINE_YAML_SYNC) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .execute()
        }
    }

    fun deletePipelineYamlBranchFile(dslContext: DSLContext, projectId: String) {
        with(TPipelineYamlBranchFile.T_PIPELINE_YAML_BRANCH_FILE) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .execute()
        }
    }

    fun deletePipelineYamlView(dslContext: DSLContext, projectId: String) {
        with(TPipelineYamlView.T_PIPELINE_YAML_VIEW) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .execute()
        }
    }

    fun deletePipelineOperationLog(dslContext: DSLContext, projectId: String, pipelineId: String) {
        with(TPipelineOperationLog.T_PIPELINE_OPERATION_LOG) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId).and(PIPELINE_ID.eq(pipelineId)))
                .execute()
        }
    }

    fun deletePipelineWebhookVersion(dslContext: DSLContext, projectId: String, pipelineId: String) {
        with(TPipelineWebhookVersion.T_PIPELINE_WEBHOOK_VERSION) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId).and(PIPELINE_ID.eq(pipelineId)))
                .execute()
        }
    }

    fun deletePipelineCallback(dslContext: DSLContext, projectId: String, pipelineId: String) {
        with(TPipelineCallback.T_PIPELINE_CALLBACK) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId).and(PIPELINE_ID.eq(pipelineId)))
                .execute()
        }
    }

    fun deletePipelineSubRef(dslContext: DSLContext, projectId: String, pipelineId: String) {
        with(TPipelineSubRef.T_PIPELINE_SUB_REF) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId).and(PIPELINE_ID.eq(pipelineId)))
                .execute()
        }
    }
}
