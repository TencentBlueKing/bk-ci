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
import com.tencent.devops.model.process.tables.TPipelineResource
import com.tencent.devops.model.process.tables.TPipelineResourceVersion
import com.tencent.devops.model.process.tables.TPipelineSetting
import com.tencent.devops.model.process.tables.TPipelineSettingVersion
import com.tencent.devops.model.process.tables.TPipelineSubRef
import com.tencent.devops.model.process.tables.TPipelineTimerBranch
import com.tencent.devops.model.process.tables.TPipelineTriggerDetail
import com.tencent.devops.model.process.tables.TPipelineTriggerEvent
import com.tencent.devops.model.process.tables.TPipelineTriggerReview
import com.tencent.devops.model.process.tables.TPipelineView
import com.tencent.devops.model.process.tables.TPipelineViewGroup
import com.tencent.devops.model.process.tables.TPipelineViewTop
import com.tencent.devops.model.process.tables.TPipelineViewUserLastView
import com.tencent.devops.model.process.tables.TPipelineViewUserSettings
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
import com.tencent.devops.model.process.tables.records.TAuditResourceRecord
import com.tencent.devops.model.process.tables.records.TPipelineBuildContainerRecord
import com.tencent.devops.model.process.tables.records.TPipelineBuildDetailRecord
import com.tencent.devops.model.process.tables.records.TPipelineBuildHistoryDebugRecord
import com.tencent.devops.model.process.tables.records.TPipelineBuildHistoryRecord
import com.tencent.devops.model.process.tables.records.TPipelineBuildRecordContainerRecord
import com.tencent.devops.model.process.tables.records.TPipelineBuildRecordModelRecord
import com.tencent.devops.model.process.tables.records.TPipelineBuildRecordStageRecord
import com.tencent.devops.model.process.tables.records.TPipelineBuildRecordTaskRecord
import com.tencent.devops.model.process.tables.records.TPipelineBuildStageRecord
import com.tencent.devops.model.process.tables.records.TPipelineBuildSummaryRecord
import com.tencent.devops.model.process.tables.records.TPipelineBuildTaskRecord
import com.tencent.devops.model.process.tables.records.TPipelineBuildTemplateAcrossInfoRecord
import com.tencent.devops.model.process.tables.records.TPipelineBuildVarRecord
import com.tencent.devops.model.process.tables.records.TPipelineCallbackRecord
import com.tencent.devops.model.process.tables.records.TPipelineFavorRecord
import com.tencent.devops.model.process.tables.records.TPipelineGroupRecord
import com.tencent.devops.model.process.tables.records.TPipelineInfoRecord
import com.tencent.devops.model.process.tables.records.TPipelineJobMutexGroupRecord
import com.tencent.devops.model.process.tables.records.TPipelineLabelPipelineRecord
import com.tencent.devops.model.process.tables.records.TPipelineLabelRecord
import com.tencent.devops.model.process.tables.records.TPipelineModelTaskRecord
import com.tencent.devops.model.process.tables.records.TPipelineOperationLogRecord
import com.tencent.devops.model.process.tables.records.TPipelinePauseValueRecord
import com.tencent.devops.model.process.tables.records.TPipelineRecentUseRecord
import com.tencent.devops.model.process.tables.records.TPipelineResourceRecord
import com.tencent.devops.model.process.tables.records.TPipelineResourceVersionRecord
import com.tencent.devops.model.process.tables.records.TPipelineSettingRecord
import com.tencent.devops.model.process.tables.records.TPipelineSettingVersionRecord
import com.tencent.devops.model.process.tables.records.TPipelineSubRefRecord
import com.tencent.devops.model.process.tables.records.TPipelineTimerBranchRecord
import com.tencent.devops.model.process.tables.records.TPipelineTriggerDetailRecord
import com.tencent.devops.model.process.tables.records.TPipelineTriggerEventRecord
import com.tencent.devops.model.process.tables.records.TPipelineTriggerReviewRecord
import com.tencent.devops.model.process.tables.records.TPipelineViewGroupRecord
import com.tencent.devops.model.process.tables.records.TPipelineViewRecord
import com.tencent.devops.model.process.tables.records.TPipelineViewTopRecord
import com.tencent.devops.model.process.tables.records.TPipelineViewUserLastViewRecord
import com.tencent.devops.model.process.tables.records.TPipelineViewUserSettingsRecord
import com.tencent.devops.model.process.tables.records.TPipelineWebhookBuildParameterRecord
import com.tencent.devops.model.process.tables.records.TPipelineWebhookQueueRecord
import com.tencent.devops.model.process.tables.records.TPipelineWebhookVersionRecord
import com.tencent.devops.model.process.tables.records.TPipelineYamlBranchFileRecord
import com.tencent.devops.model.process.tables.records.TPipelineYamlInfoRecord
import com.tencent.devops.model.process.tables.records.TPipelineYamlSyncRecord
import com.tencent.devops.model.process.tables.records.TPipelineYamlVersionRecord
import com.tencent.devops.model.process.tables.records.TPipelineYamlViewRecord
import com.tencent.devops.model.process.tables.records.TProjectPipelineCallbackHistoryRecord
import com.tencent.devops.model.process.tables.records.TProjectPipelineCallbackRecord
import com.tencent.devops.model.process.tables.records.TReportRecord
import com.tencent.devops.model.process.tables.records.TTemplatePipelineRecord
import com.tencent.devops.model.process.tables.records.TTemplateRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Suppress("TooManyFunctions", "LargeClass", "LongParameterList")
@Repository
class ProcessDataMigrateDao {

    fun getAuditResourceRecords(
        dslContext: DSLContext,
        projectId: String,
        resourceType: String? = null,
        resourceId: String? = null,
        limit: Int,
        offset: Int
    ): List<TAuditResourceRecord> {
        with(TAuditResource.T_AUDIT_RESOURCE) {
            val conditions = mutableListOf(PROJECT_ID.eq(projectId)).apply {
                resourceType?.takeIf { it.isNotBlank() }?.let { add(RESOURCE_TYPE.eq(it)) }
                resourceId?.takeIf { it.isNotBlank() }?.let { add(RESOURCE_ID.eq(it)) }
            }
            return dslContext.selectFrom(this)
                .where(conditions)
                .orderBy(ID.asc())
                .limit(limit).offset(offset).fetchInto(TAuditResourceRecord::class.java)
        }
    }

    fun migrateAuditResourceData(
        migratingShardingDslContext: DSLContext,
        auditResourceRecords: List<TAuditResourceRecord>
    ) {
        with(TAuditResource.T_AUDIT_RESOURCE) {
            val insertRecords = auditResourceRecords.map { migratingShardingDslContext.newRecord(this, it) }
            migratingShardingDslContext.batchInsert(insertRecords).execute()
        }
    }

    fun getPipelineBuildContainerRecords(
        dslContext: DSLContext,
        projectId: String,
        buildIds: List<String>
    ): List<TPipelineBuildContainerRecord> {
        with(TPipelineBuildContainer.T_PIPELINE_BUILD_CONTAINER) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId).and(BUILD_ID.`in`(buildIds)))
                .fetchInto(TPipelineBuildContainerRecord::class.java)
        }
    }

    fun migratePipelineBuildContainerData(
        migratingShardingDslContext: DSLContext,
        pipelineBuildContainerRecords: List<TPipelineBuildContainerRecord>
    ) {
        with(TPipelineBuildContainer.T_PIPELINE_BUILD_CONTAINER) {
            val insertRecords = pipelineBuildContainerRecords.map { migratingShardingDslContext.newRecord(this, it) }
            migratingShardingDslContext.batchInsert(insertRecords).execute()
        }
    }

    fun getPipelineBuildHistoryRecords(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        limit: Int,
        offset: Int
    ): List<TPipelineBuildHistoryRecord> {
        with(TPipelineBuildHistory.T_PIPELINE_BUILD_HISTORY) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId).and(PIPELINE_ID.eq(pipelineId)))
                .orderBy(BUILD_ID.asc())
                .limit(limit).offset(offset).fetchInto(TPipelineBuildHistoryRecord::class.java)
        }
    }

    fun migratePipelineBuildHistoryData(
        migratingShardingDslContext: DSLContext,
        pipelineBuildHistoryRecords: List<TPipelineBuildHistoryRecord>
    ) {
        with(TPipelineBuildHistory.T_PIPELINE_BUILD_HISTORY) {
            val insertRecords = pipelineBuildHistoryRecords.map { migratingShardingDslContext.newRecord(this, it) }
            migratingShardingDslContext.batchInsert(insertRecords).execute()
        }
    }

    fun getPipelineBuildHistoryDebugRecords(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        limit: Int,
        offset: Int
    ): List<TPipelineBuildHistoryDebugRecord> {
        with(TPipelineBuildHistoryDebug.T_PIPELINE_BUILD_HISTORY_DEBUG) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId).and(PIPELINE_ID.eq(pipelineId)))
                .orderBy(BUILD_ID.asc())
                .limit(limit).offset(offset).fetchInto(TPipelineBuildHistoryDebugRecord::class.java)
        }
    }

    fun migratePipelineBuildHistoryDebugData(
        migratingShardingDslContext: DSLContext,
        pipelineBuildHistoryDebugRecords: List<TPipelineBuildHistoryDebugRecord>
    ) {
        with(TPipelineBuildHistoryDebug.T_PIPELINE_BUILD_HISTORY_DEBUG) {
            val insertRecords = pipelineBuildHistoryDebugRecords.map { migratingShardingDslContext.newRecord(this, it) }
            migratingShardingDslContext.batchInsert(insertRecords).execute()
        }
    }

    fun getPipelineBuildDetailRecords(
        dslContext: DSLContext,
        projectId: String,
        buildIds: List<String>
    ): List<TPipelineBuildDetailRecord> {
        with(TPipelineBuildDetail.T_PIPELINE_BUILD_DETAIL) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId).and(BUILD_ID.`in`(buildIds)))
                .fetchInto(TPipelineBuildDetailRecord::class.java)
        }
    }

    fun migratePipelineBuildDetailData(
        migratingShardingDslContext: DSLContext,
        pipelineBuildDetailRecords: List<TPipelineBuildDetailRecord>
    ) {
        with(TPipelineBuildDetail.T_PIPELINE_BUILD_DETAIL) {
            val insertRecords = pipelineBuildDetailRecords.map { migratingShardingDslContext.newRecord(this, it) }
            migratingShardingDslContext.batchInsert(insertRecords).execute()
        }
    }

    fun getPipelineBuildStageRecords(
        dslContext: DSLContext,
        projectId: String,
        buildIds: List<String>
    ): List<TPipelineBuildStageRecord> {
        with(TPipelineBuildStage.T_PIPELINE_BUILD_STAGE) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId).and(BUILD_ID.`in`(buildIds)))
                .fetchInto(TPipelineBuildStageRecord::class.java)
        }
    }

    fun migratePipelineBuildStageData(
        migratingShardingDslContext: DSLContext,
        pipelineBuildStageRecords: List<TPipelineBuildStageRecord>
    ) {
        with(TPipelineBuildStage.T_PIPELINE_BUILD_STAGE) {
            val insertRecords = pipelineBuildStageRecords.map { migratingShardingDslContext.newRecord(this, it) }
            migratingShardingDslContext.batchInsert(insertRecords).execute()
        }
    }

    fun getPipelineBuildTaskRecords(
        dslContext: DSLContext,
        projectId: String,
        buildIds: List<String>
    ): List<TPipelineBuildTaskRecord> {
        with(TPipelineBuildTask.T_PIPELINE_BUILD_TASK) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId).and(BUILD_ID.`in`(buildIds)))
                .fetchInto(TPipelineBuildTaskRecord::class.java)
        }
    }

    fun migratePipelineBuildTaskData(
        migratingShardingDslContext: DSLContext,
        pipelineBuildTaskRecords: List<TPipelineBuildTaskRecord>
    ) {
        with(TPipelineBuildTask.T_PIPELINE_BUILD_TASK) {
            val insertRecords = pipelineBuildTaskRecords.map { migratingShardingDslContext.newRecord(this, it) }
            migratingShardingDslContext.batchInsert(insertRecords).execute()
        }
    }

    fun getPipelineBuildSummaryRecord(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String
    ): TPipelineBuildSummaryRecord? {
        with(TPipelineBuildSummary.T_PIPELINE_BUILD_SUMMARY) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId).and(PIPELINE_ID.eq(pipelineId)))
                .fetchOne()
        }
    }

    fun migratePipelineBuildSummaryData(
        migratingShardingDslContext: DSLContext,
        buildSummaryRecord: TPipelineBuildSummaryRecord
    ) {
        with(TPipelineBuildSummary.T_PIPELINE_BUILD_SUMMARY) {
            val insertRecord = migratingShardingDslContext.newRecord(this, buildSummaryRecord)
            migratingShardingDslContext.executeInsert(insertRecord)
        }
    }

    fun getPipelineBuildVarRecords(
        dslContext: DSLContext,
        projectId: String,
        buildIds: List<String>
    ): List<TPipelineBuildVarRecord> {
        with(TPipelineBuildVar.T_PIPELINE_BUILD_VAR) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId).and(BUILD_ID.`in`(buildIds)))
                .fetchInto(TPipelineBuildVarRecord::class.java)
        }
    }

    fun migratePipelineBuildVarData(
        migratingShardingDslContext: DSLContext,
        pipelineBuildVarRecords: List<TPipelineBuildVarRecord>
    ) {
        with(TPipelineBuildVar.T_PIPELINE_BUILD_VAR) {
            val insertRecords = pipelineBuildVarRecords.map { migratingShardingDslContext.newRecord(this, it) }
            migratingShardingDslContext.batchInsert(insertRecords).execute()
        }
    }

    fun getPipelineFavorRecords(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        limit: Int,
        offset: Int
    ): List<TPipelineFavorRecord> {
        with(TPipelineFavor.T_PIPELINE_FAVOR) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId).and(PIPELINE_ID.eq(pipelineId)))
                .orderBy(CREATE_USER.asc())
                .limit(limit).offset(offset).fetchInto(TPipelineFavorRecord::class.java)
        }
    }

    fun migratePipelineFavorData(
        migratingShardingDslContext: DSLContext,
        pipelineFavorRecords: List<TPipelineFavorRecord>
    ) {
        with(TPipelineFavor.T_PIPELINE_FAVOR) {
            val insertRecords = pipelineFavorRecords.map { migratingShardingDslContext.newRecord(this, it) }
            migratingShardingDslContext.batchInsert(insertRecords).execute()
        }
    }

    fun getPipelineGroupRecords(
        dslContext: DSLContext,
        projectId: String,
        limit: Int,
        offset: Int
    ): List<TPipelineGroupRecord> {
        with(TPipelineGroup.T_PIPELINE_GROUP) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .orderBy(ID.asc())
                .limit(limit).offset(offset).fetchInto(TPipelineGroupRecord::class.java)
        }
    }

    fun migratePipelineGroupData(
        migratingShardingDslContext: DSLContext,
        pipelineGroupRecords: List<TPipelineGroupRecord>
    ) {
        with(TPipelineGroup.T_PIPELINE_GROUP) {
            val insertRecords = pipelineGroupRecords.map { migratingShardingDslContext.newRecord(this, it) }
            migratingShardingDslContext.batchInsert(insertRecords).execute()
        }
    }

    fun getPipelineInfoRecord(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String
    ): TPipelineInfoRecord? {
        with(TPipelineInfo.T_PIPELINE_INFO) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId).and(PIPELINE_ID.eq(pipelineId)))
                .fetchOne()
        }
    }

    fun migratePipelineInfoData(
        migratingShardingDslContext: DSLContext,
        pipelineInfoRecord: TPipelineInfoRecord
    ) {
        with(TPipelineInfo.T_PIPELINE_INFO) {
            val insertRecord = migratingShardingDslContext.newRecord(this, pipelineInfoRecord)
            migratingShardingDslContext.executeInsert(insertRecord)
        }
    }

    fun getPipelineJobMutexGroupRecords(
        dslContext: DSLContext,
        projectId: String,
        limit: Int,
        offset: Int
    ): List<TPipelineJobMutexGroupRecord> {
        with(TPipelineJobMutexGroup.T_PIPELINE_JOB_MUTEX_GROUP) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .orderBy(JOB_MUTEX_GROUP_NAME.asc())
                .limit(limit).offset(offset).fetchInto(TPipelineJobMutexGroupRecord::class.java)
        }
    }

    fun migratePipelineJobMutexGroupData(
        migratingShardingDslContext: DSLContext,
        pipelineJobMutexGroupRecords: List<TPipelineJobMutexGroupRecord>
    ) {
        with(TPipelineJobMutexGroup.T_PIPELINE_JOB_MUTEX_GROUP) {
            val insertRecords = pipelineJobMutexGroupRecords.map { migratingShardingDslContext.newRecord(this, it) }
            migratingShardingDslContext.batchInsert(insertRecords).execute()
        }
    }

    fun getPipelineLabelRecords(
        dslContext: DSLContext,
        projectId: String,
        limit: Int,
        offset: Int
    ): List<TPipelineLabelRecord> {
        with(TPipelineLabel.T_PIPELINE_LABEL) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .orderBy(ID.asc())
                .limit(limit).offset(offset).fetchInto(TPipelineLabelRecord::class.java)
        }
    }

    fun migratePipelineLabelData(
        migratingShardingDslContext: DSLContext,
        pipelineLabelRecords: List<TPipelineLabelRecord>
    ) {
        with(TPipelineLabel.T_PIPELINE_LABEL) {
            val insertRecords = pipelineLabelRecords.map { migratingShardingDslContext.newRecord(this, it) }
            migratingShardingDslContext.batchInsert(insertRecords).execute()
        }
    }

    fun getPipelineLabelPipelineRecords(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        limit: Int,
        offset: Int
    ): List<TPipelineLabelPipelineRecord> {
        with(TPipelineLabelPipeline.T_PIPELINE_LABEL_PIPELINE) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId).and(PIPELINE_ID.eq(pipelineId)))
                .orderBy(ID.asc())
                .limit(limit).offset(offset).fetchInto(TPipelineLabelPipelineRecord::class.java)
        }
    }

    fun migratePipelineLabelPipelineData(
        migratingShardingDslContext: DSLContext,
        pipelineLabelPipelineRecords: List<TPipelineLabelPipelineRecord>
    ) {
        with(TPipelineLabelPipeline.T_PIPELINE_LABEL_PIPELINE) {
            val insertRecords = pipelineLabelPipelineRecords.map { migratingShardingDslContext.newRecord(this, it) }
            migratingShardingDslContext.batchInsert(insertRecords).execute()
        }
    }

    fun getPipelineModelTaskRecords(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String
    ): List<TPipelineModelTaskRecord> {
        with(TPipelineModelTask.T_PIPELINE_MODEL_TASK) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId).and(PIPELINE_ID.eq(pipelineId)))
                .fetchInto(TPipelineModelTaskRecord::class.java)
        }
    }

    fun migratePipelineModelTaskData(
        migratingShardingDslContext: DSLContext,
        pipelineModelTaskRecords: List<TPipelineModelTaskRecord>
    ) {
        with(TPipelineModelTask.T_PIPELINE_MODEL_TASK) {
            val insertRecords = pipelineModelTaskRecords.map { migratingShardingDslContext.newRecord(this, it) }
            migratingShardingDslContext.batchInsert(insertRecords).execute()
        }
    }

    fun getPipelinePauseValueRecords(
        dslContext: DSLContext,
        projectId: String,
        buildIds: List<String>
    ): List<TPipelinePauseValueRecord> {
        with(TPipelinePauseValue.T_PIPELINE_PAUSE_VALUE) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId).and(BUILD_ID.`in`(buildIds)))
                .fetchInto(TPipelinePauseValueRecord::class.java)
        }
    }

    fun migratePipelinePauseValueData(
        migratingShardingDslContext: DSLContext,
        pipelinePauseValueRecords: List<TPipelinePauseValueRecord>
    ) {
        with(TPipelinePauseValue.T_PIPELINE_PAUSE_VALUE) {
            val insertRecords = pipelinePauseValueRecords.map { migratingShardingDslContext.newRecord(this, it) }
            migratingShardingDslContext.batchInsert(insertRecords).execute()
        }
    }

    fun getPipelineResourceRecord(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String
    ): TPipelineResourceRecord? {
        with(TPipelineResource.T_PIPELINE_RESOURCE) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId).and(PIPELINE_ID.eq(pipelineId)))
                .fetchOne()
        }
    }

    fun migratePipelineResourceData(
        migratingShardingDslContext: DSLContext,
        pipelineResourceRecord: TPipelineResourceRecord
    ) {
        with(TPipelineResource.T_PIPELINE_RESOURCE) {
            val insertRecord = migratingShardingDslContext.newRecord(this, pipelineResourceRecord)
            migratingShardingDslContext.executeInsert(insertRecord)
        }
    }

    fun getPipelineResourceVersionRecords(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        limit: Int,
        offset: Int
    ): List<TPipelineResourceVersionRecord> {
        with(TPipelineResourceVersion.T_PIPELINE_RESOURCE_VERSION) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId).and(PIPELINE_ID.eq(pipelineId)))
                .orderBy(VERSION.asc())
                .limit(limit).offset(offset).fetchInto(TPipelineResourceVersionRecord::class.java)
        }
    }

    fun migratePipelineResourceVersionData(
        migratingShardingDslContext: DSLContext,
        pipelineResourceVersionRecords: List<TPipelineResourceVersionRecord>
    ) {
        with(TPipelineResourceVersion.T_PIPELINE_RESOURCE_VERSION) {
            val insertRecords = pipelineResourceVersionRecords.map { migratingShardingDslContext.newRecord(this, it) }
            migratingShardingDslContext.batchInsert(insertRecords).execute()
        }
    }

    fun getPipelineSettingRecord(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String
    ): TPipelineSettingRecord? {
        with(TPipelineSetting.T_PIPELINE_SETTING) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId).and(PIPELINE_ID.eq(pipelineId)))
                .fetchOne()
        }
    }

    fun migratePipelineSettingData(
        migratingShardingDslContext: DSLContext,
        pipelineSettingRecord: TPipelineSettingRecord
    ) {
        with(TPipelineSetting.T_PIPELINE_SETTING) {
            val insertRecord = migratingShardingDslContext.newRecord(this, pipelineSettingRecord)
            migratingShardingDslContext.executeInsert(insertRecord)
        }
    }

    fun getPipelineSettingVersionRecords(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        limit: Int,
        offset: Int
    ): List<TPipelineSettingVersionRecord> {
        with(TPipelineSettingVersion.T_PIPELINE_SETTING_VERSION) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId).and(PIPELINE_ID.eq(pipelineId)))
                .orderBy(VERSION.asc())
                .limit(limit).offset(offset).fetchInto(TPipelineSettingVersionRecord::class.java)
        }
    }

    fun migratePipelineSettingVersionData(
        migratingShardingDslContext: DSLContext,
        pipelineSettingVersionRecords: List<TPipelineSettingVersionRecord>
    ) {
        with(TPipelineSettingVersion.T_PIPELINE_SETTING_VERSION) {
            val insertRecords = pipelineSettingVersionRecords.map { migratingShardingDslContext.newRecord(this, it) }
            migratingShardingDslContext.batchInsert(insertRecords).execute()
        }
    }

    fun getPipelineViewRecords(
        dslContext: DSLContext,
        projectId: String,
        limit: Int,
        offset: Int
    ): List<TPipelineViewRecord> {
        with(TPipelineView.T_PIPELINE_VIEW) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .orderBy(ID.asc())
                .limit(limit).offset(offset).fetchInto(TPipelineViewRecord::class.java)
        }
    }

    fun migratePipelineViewData(
        migratingShardingDslContext: DSLContext,
        pipelineViewRecords: List<TPipelineViewRecord>
    ) {
        with(TPipelineView.T_PIPELINE_VIEW) {
            val insertRecords = pipelineViewRecords.map { migratingShardingDslContext.newRecord(this, it) }
            migratingShardingDslContext.batchInsert(insertRecords).execute()
        }
    }

    fun getPipelineViewUserLastViewRecords(
        dslContext: DSLContext,
        projectId: String,
        limit: Int,
        offset: Int
    ): List<TPipelineViewUserLastViewRecord> {
        with(TPipelineViewUserLastView.T_PIPELINE_VIEW_USER_LAST_VIEW) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .orderBy(USER_ID.asc())
                .limit(limit).offset(offset).fetchInto(TPipelineViewUserLastViewRecord::class.java)
        }
    }

    fun migratePipelineViewUserLastViewData(
        migratingShardingDslContext: DSLContext,
        pipelineViewUserLastViewRecords: List<TPipelineViewUserLastViewRecord>
    ) {
        with(TPipelineViewUserLastView.T_PIPELINE_VIEW_USER_LAST_VIEW) {
            val insertRecords = pipelineViewUserLastViewRecords.map { migratingShardingDslContext.newRecord(this, it) }
            migratingShardingDslContext.batchInsert(insertRecords).execute()
        }
    }

    fun getPipelineViewUserSettingsRecords(
        dslContext: DSLContext,
        projectId: String,
        limit: Int,
        offset: Int
    ): List<TPipelineViewUserSettingsRecord> {
        with(TPipelineViewUserSettings.T_PIPELINE_VIEW_USER_SETTINGS) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .orderBy(USER_ID.asc())
                .limit(limit).offset(offset).fetchInto(TPipelineViewUserSettingsRecord::class.java)
        }
    }

    fun migratePipelineViewUserSettingsData(
        migratingShardingDslContext: DSLContext,
        pipelineViewUserSettingsRecords: List<TPipelineViewUserSettingsRecord>
    ) {
        with(TPipelineViewUserSettings.T_PIPELINE_VIEW_USER_SETTINGS) {
            val insertRecords = pipelineViewUserSettingsRecords.map { migratingShardingDslContext.newRecord(this, it) }
            migratingShardingDslContext.batchInsert(insertRecords).execute()
        }
    }

    fun getPipelineWebhookQueueRecords(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        limit: Int,
        offset: Int
    ): List<TPipelineWebhookQueueRecord> {
        with(TPipelineWebhookQueue.T_PIPELINE_WEBHOOK_QUEUE) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId).and(PIPELINE_ID.eq(pipelineId)))
                .orderBy(ID.asc())
                .limit(limit).offset(offset).fetchInto(TPipelineWebhookQueueRecord::class.java)
        }
    }

    fun migratePipelineWebhookQueueData(
        migratingShardingDslContext: DSLContext,
        webhookQueueRecords: List<TPipelineWebhookQueueRecord>
    ) {
        with(TPipelineWebhookQueue.T_PIPELINE_WEBHOOK_QUEUE) {
            val insertRecords = webhookQueueRecords.map { migratingShardingDslContext.newRecord(this, it) }
            migratingShardingDslContext.batchInsert(insertRecords).execute()
        }
    }

    fun getProjectPipelineCallbackRecords(
        dslContext: DSLContext,
        projectId: String,
        limit: Int,
        offset: Int
    ): List<TProjectPipelineCallbackRecord> {
        with(TProjectPipelineCallback.T_PROJECT_PIPELINE_CALLBACK) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .orderBy(ID.asc())
                .limit(limit).offset(offset).fetchInto(TProjectPipelineCallbackRecord::class.java)
        }
    }

    fun migrateProjectPipelineCallbackData(
        migratingShardingDslContext: DSLContext,
        projectPipelineCallbackRecords: List<TProjectPipelineCallbackRecord>
    ) {
        with(TProjectPipelineCallback.T_PROJECT_PIPELINE_CALLBACK) {
            val insertRecords = projectPipelineCallbackRecords.map { migratingShardingDslContext.newRecord(this, it) }
            migratingShardingDslContext.batchInsert(insertRecords).execute()
        }
    }

    fun getProjectPipelineCallbackHistoryRecords(
        dslContext: DSLContext,
        projectId: String,
        limit: Int,
        offset: Int
    ): List<TProjectPipelineCallbackHistoryRecord> {
        with(TProjectPipelineCallbackHistory.T_PROJECT_PIPELINE_CALLBACK_HISTORY) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .orderBy(ID.asc(), CREATED_TIME.asc())
                .limit(limit).offset(offset).fetchInto(TProjectPipelineCallbackHistoryRecord::class.java)
        }
    }

    fun migrateProjectPipelineCallbackHistoryData(
        migratingShardingDslContext: DSLContext,
        pipelineCallbackHistoryRecords: List<TProjectPipelineCallbackHistoryRecord>
    ) {
        with(TProjectPipelineCallbackHistory.T_PROJECT_PIPELINE_CALLBACK_HISTORY) {
            val insertRecords = pipelineCallbackHistoryRecords.map { migratingShardingDslContext.newRecord(this, it) }
            migratingShardingDslContext.batchInsert(insertRecords).execute()
        }
    }

    fun getReportRecords(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        limit: Int,
        offset: Int
    ): List<TReportRecord> {
        with(TReport.T_REPORT) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId).and(PIPELINE_ID.eq(pipelineId)))
                .orderBy(ID.asc())
                .limit(limit).offset(offset).fetchInto(TReportRecord::class.java)
        }
    }

    fun migrateReportData(
        migratingShardingDslContext: DSLContext,
        reportRecords: List<TReportRecord>
    ) {
        with(TReport.T_REPORT) {
            val insertRecords = reportRecords.map { migratingShardingDslContext.newRecord(this, it) }
            migratingShardingDslContext.batchInsert(insertRecords).execute()
        }
    }

    fun getTemplateRecords(
        dslContext: DSLContext,
        projectId: String,
        limit: Int,
        offset: Int
    ): List<TTemplateRecord> {
        with(TTemplate.T_TEMPLATE) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .orderBy(VERSION.asc())
                .limit(limit).offset(offset).fetchInto(TTemplateRecord::class.java)
        }
    }

    fun migrateTemplateData(
        migratingShardingDslContext: DSLContext,
        templateRecords: List<TTemplateRecord>
    ) {
        with(TTemplate.T_TEMPLATE) {
            val insertRecords = templateRecords.map { migratingShardingDslContext.newRecord(this, it) }
            migratingShardingDslContext.batchInsert(insertRecords).execute()
        }
    }

    fun getTemplateSettingRecords(
        dslContext: DSLContext,
        projectId: String,
        limit: Int,
        offset: Int
    ): List<TPipelineSettingRecord> {
        with(TPipelineSetting.T_PIPELINE_SETTING) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId).and(IS_TEMPLATE.eq(true)))
                .orderBy(PIPELINE_ID.asc())
                .limit(limit).offset(offset).fetchInto(TPipelineSettingRecord::class.java)
        }
    }

    fun migrateTemplateSettingData(
        migratingShardingDslContext: DSLContext,
        templateSettingRecords: List<TPipelineSettingRecord>
    ) {
        with(TPipelineSetting.T_PIPELINE_SETTING) {
            val insertRecords = templateSettingRecords.map { migratingShardingDslContext.newRecord(this, it) }
            migratingShardingDslContext.batchInsert(insertRecords).execute()
        }
    }

    fun getTemplateSettingVersionRecords(
        dslContext: DSLContext,
        projectId: String,
        limit: Int,
        offset: Int
    ): List<TPipelineSettingVersionRecord> {
        with(TPipelineSettingVersion.T_PIPELINE_SETTING_VERSION) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId).and(IS_TEMPLATE.eq(true)))
                .orderBy(ID.asc())
                .limit(limit).offset(offset).fetchInto(TPipelineSettingVersionRecord::class.java)
        }
    }

    fun migrateTemplateSettingVersionData(
        migratingShardingDslContext: DSLContext,
        templateSettingVersionRecords: List<TPipelineSettingVersionRecord>
    ) {
        with(TPipelineSettingVersion.T_PIPELINE_SETTING_VERSION) {
            val insertRecords = templateSettingVersionRecords.map { migratingShardingDslContext.newRecord(this, it) }
            migratingShardingDslContext.batchInsert(insertRecords).execute()
        }
    }

    fun getTemplatePipelineRecord(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String
    ): TTemplatePipelineRecord? {
        with(TTemplatePipeline.T_TEMPLATE_PIPELINE) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId).and(PIPELINE_ID.eq(pipelineId)))
                .fetchOne()
        }
    }

    fun migrateTemplatePipelineData(
        migratingShardingDslContext: DSLContext,
        tTemplatePipelineRecord: TTemplatePipelineRecord
    ) {
        with(TTemplatePipeline.T_TEMPLATE_PIPELINE) {
            val insertRecord = migratingShardingDslContext.newRecord(this, tTemplatePipelineRecord)
            migratingShardingDslContext.executeInsert(insertRecord)
        }
    }

    fun getBuildTemplateAcrossInfoRecords(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        limit: Int,
        offset: Int
    ): List<TPipelineBuildTemplateAcrossInfoRecord> {
        with(TPipelineBuildTemplateAcrossInfo.T_PIPELINE_BUILD_TEMPLATE_ACROSS_INFO) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId).and(PIPELINE_ID.eq(pipelineId)))
                .orderBy(ID.asc())
                .limit(limit).offset(offset).fetchInto(TPipelineBuildTemplateAcrossInfoRecord::class.java)
        }
    }

    fun migrateBuildTemplateAcrossInfoData(
        migratingShardingDslContext: DSLContext,
        buildTemplateAcrossInfoRecords: List<TPipelineBuildTemplateAcrossInfoRecord>
    ) {
        with(TPipelineBuildTemplateAcrossInfo.T_PIPELINE_BUILD_TEMPLATE_ACROSS_INFO) {
            val insertRecords = buildTemplateAcrossInfoRecords.map { migratingShardingDslContext.newRecord(this, it) }
            migratingShardingDslContext.batchInsert(insertRecords).execute()
        }
    }

    fun getPipelineWebhookBuildParameterRecords(
        dslContext: DSLContext,
        projectId: String,
        buildIds: List<String>
    ): List<TPipelineWebhookBuildParameterRecord> {
        with(TPipelineWebhookBuildParameter.T_PIPELINE_WEBHOOK_BUILD_PARAMETER) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId).and(BUILD_ID.`in`(buildIds)))
                .fetchInto(TPipelineWebhookBuildParameterRecord::class.java)
        }
    }

    fun migratePipelineWebhookBuildParameterData(
        migratingShardingDslContext: DSLContext,
        webhookBuildParameterRecords: List<TPipelineWebhookBuildParameterRecord>
    ) {
        with(TPipelineWebhookBuildParameter.T_PIPELINE_WEBHOOK_BUILD_PARAMETER) {
            val insertRecords = webhookBuildParameterRecords.map { migratingShardingDslContext.newRecord(this, it) }
            migratingShardingDslContext.batchInsert(insertRecords).execute()
        }
    }

    fun getPipelineViewGroupRecords(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        limit: Int,
        offset: Int
    ): List<TPipelineViewGroupRecord> {
        with(TPipelineViewGroup.T_PIPELINE_VIEW_GROUP) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId).and(PIPELINE_ID.eq(pipelineId)))
                .orderBy(ID.asc())
                .limit(limit).offset(offset).fetchInto(TPipelineViewGroupRecord::class.java)
        }
    }

    fun migratePipelineViewGroupData(
        migratingShardingDslContext: DSLContext,
        pipelineViewGroupRecords: List<TPipelineViewGroupRecord>
    ) {
        with(TPipelineViewGroup.T_PIPELINE_VIEW_GROUP) {
            val insertRecords = pipelineViewGroupRecords.map { migratingShardingDslContext.newRecord(this, it) }
            migratingShardingDslContext.batchInsert(insertRecords).execute()
        }
    }

    fun getPipelineViewTopRecords(
        dslContext: DSLContext,
        projectId: String,
        limit: Int,
        offset: Int
    ): List<TPipelineViewTopRecord> {
        with(TPipelineViewTop.T_PIPELINE_VIEW_TOP) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .orderBy(ID.asc())
                .limit(limit).offset(offset).fetchInto(TPipelineViewTopRecord::class.java)
        }
    }

    fun migratePipelineViewTopData(
        migratingShardingDslContext: DSLContext,
        pipelineViewTopRecords: List<TPipelineViewTopRecord>
    ) {
        with(TPipelineViewTop.T_PIPELINE_VIEW_TOP) {
            val insertRecords = pipelineViewTopRecords.map { migratingShardingDslContext.newRecord(this, it) }
            migratingShardingDslContext.batchInsert(insertRecords).execute()
        }
    }

    fun getPipelineRecentUseRecords(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        limit: Int,
        offset: Int
    ): List<TPipelineRecentUseRecord> {
        with(TPipelineRecentUse.T_PIPELINE_RECENT_USE) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId).and(PIPELINE_ID.eq(pipelineId)))
                .orderBy(USER_ID.asc())
                .limit(limit).offset(offset).fetchInto(TPipelineRecentUseRecord::class.java)
        }
    }

    fun migratePipelineRecentUseData(
        migratingShardingDslContext: DSLContext,
        pipelineRecentUseRecords: List<TPipelineRecentUseRecord>
    ) {
        with(TPipelineRecentUse.T_PIPELINE_RECENT_USE) {
            val insertRecords = pipelineRecentUseRecords.map { migratingShardingDslContext.newRecord(this, it) }
            migratingShardingDslContext.batchInsert(insertRecords).execute()
        }
    }

    fun getPipelineBuildRecordContainerRecords(
        dslContext: DSLContext,
        projectId: String,
        buildIds: List<String>
    ): List<TPipelineBuildRecordContainerRecord> {
        with(TPipelineBuildRecordContainer.T_PIPELINE_BUILD_RECORD_CONTAINER) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId).and(BUILD_ID.`in`(buildIds)))
                .fetchInto(TPipelineBuildRecordContainerRecord::class.java)
        }
    }

    fun migratePipelineBuildRecordContainerData(
        migratingShardingDslContext: DSLContext,
        buildRecordContainerRecords: List<TPipelineBuildRecordContainerRecord>
    ) {
        with(TPipelineBuildRecordContainer.T_PIPELINE_BUILD_RECORD_CONTAINER) {
            val insertRecords = buildRecordContainerRecords.map { migratingShardingDslContext.newRecord(this, it) }
            migratingShardingDslContext.batchInsert(insertRecords).execute()
        }
    }

    fun getPipelineBuildRecordModelRecords(
        dslContext: DSLContext,
        projectId: String,
        buildIds: List<String>
    ): List<TPipelineBuildRecordModelRecord> {
        with(TPipelineBuildRecordModel.T_PIPELINE_BUILD_RECORD_MODEL) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId).and(BUILD_ID.`in`(buildIds)))
                .fetchInto(TPipelineBuildRecordModelRecord::class.java)
        }
    }

    fun migratePipelineBuildRecordModelData(
        migratingShardingDslContext: DSLContext,
        buildRecordModelRecords: List<TPipelineBuildRecordModelRecord>
    ) {
        with(TPipelineBuildRecordModel.T_PIPELINE_BUILD_RECORD_MODEL) {
            val insertRecords = buildRecordModelRecords.map { migratingShardingDslContext.newRecord(this, it) }
            migratingShardingDslContext.batchInsert(insertRecords).execute()
        }
    }

    fun getPipelineBuildRecordStageRecords(
        dslContext: DSLContext,
        projectId: String,
        buildIds: List<String>
    ): List<TPipelineBuildRecordStageRecord> {
        with(TPipelineBuildRecordStage.T_PIPELINE_BUILD_RECORD_STAGE) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId).and(BUILD_ID.`in`(buildIds)))
                .fetchInto(TPipelineBuildRecordStageRecord::class.java)
        }
    }

    fun migratePipelineBuildRecordStageData(
        migratingShardingDslContext: DSLContext,
        buildRecordStageRecords: List<TPipelineBuildRecordStageRecord>
    ) {
        with(TPipelineBuildRecordStage.T_PIPELINE_BUILD_RECORD_STAGE) {
            val insertRecords = buildRecordStageRecords.map { migratingShardingDslContext.newRecord(this, it) }
            migratingShardingDslContext.batchInsert(insertRecords).execute()
        }
    }

    fun getPipelineBuildRecordTaskRecords(
        dslContext: DSLContext,
        projectId: String,
        buildIds: List<String>
    ): List<TPipelineBuildRecordTaskRecord> {
        with(TPipelineBuildRecordTask.T_PIPELINE_BUILD_RECORD_TASK) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId).and(BUILD_ID.`in`(buildIds)))
                .fetchInto(TPipelineBuildRecordTaskRecord::class.java)
        }
    }

    fun migratePipelineBuildRecordTaskData(
        migratingShardingDslContext: DSLContext,
        buildRecordTaskRecords: List<TPipelineBuildRecordTaskRecord>
    ) {
        with(TPipelineBuildRecordTask.T_PIPELINE_BUILD_RECORD_TASK) {
            val insertRecords = buildRecordTaskRecords.map { migratingShardingDslContext.newRecord(this, it) }
            migratingShardingDslContext.batchInsert(insertRecords).execute()
        }
    }

    fun getPipelineTriggerReviewRecords(
        dslContext: DSLContext,
        projectId: String,
        buildIds: List<String>
    ): List<TPipelineTriggerReviewRecord> {
        with(TPipelineTriggerReview.T_PIPELINE_TRIGGER_REVIEW) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId).and(BUILD_ID.`in`(buildIds)))
                .fetchInto(TPipelineTriggerReviewRecord::class.java)
        }
    }

    fun migratePipelineTriggerReviewData(
        migratingShardingDslContext: DSLContext,
        pipelineTriggerReviewRecords: List<TPipelineTriggerReviewRecord>
    ) {
        with(TPipelineTriggerReview.T_PIPELINE_TRIGGER_REVIEW) {
            val insertRecords = pipelineTriggerReviewRecords.map { migratingShardingDslContext.newRecord(this, it) }
            migratingShardingDslContext.batchInsert(insertRecords).execute()
        }
    }

    fun getPipelineTimerBranchRecords(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        limit: Int,
        offset: Int
    ): List<TPipelineTimerBranchRecord> {
        with(TPipelineTimerBranch.T_PIPELINE_TIMER_BRANCH) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId).and(PIPELINE_ID.eq(pipelineId)))
                .orderBy(TASK_ID.asc(), REPO_HASH_ID.asc(), BRANCH.asc())
                .limit(limit).offset(offset).fetchInto(TPipelineTimerBranchRecord::class.java)
        }
    }

    fun migratePipelineTimerBranchData(
        migratingShardingDslContext: DSLContext,
        pipelineTimerBranchRecords: List<TPipelineTimerBranchRecord>
    ) {
        with(TPipelineTimerBranch.T_PIPELINE_TIMER_BRANCH) {
            val insertRecords = pipelineTimerBranchRecords.map { migratingShardingDslContext.newRecord(this, it) }
            migratingShardingDslContext.batchInsert(insertRecords).execute()
        }
    }

    fun getPipelineTriggerDetailRecords(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        limit: Int,
        offset: Int
    ): List<TPipelineTriggerDetailRecord> {
        with(TPipelineTriggerDetail.T_PIPELINE_TRIGGER_DETAIL) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId).and(PIPELINE_ID.eq(pipelineId)))
                .orderBy(DETAIL_ID.asc(), CREATE_TIME.asc())
                .limit(limit).offset(offset).fetchInto(TPipelineTriggerDetailRecord::class.java)
        }
    }

    fun migratePipelineTriggerDetailData(
        migratingShardingDslContext: DSLContext,
        pipelineTriggerDetailRecords: List<TPipelineTriggerDetailRecord>
    ) {
        with(TPipelineTriggerDetail.T_PIPELINE_TRIGGER_DETAIL) {
            val insertRecords = pipelineTriggerDetailRecords.map { migratingShardingDslContext.newRecord(this, it) }
            migratingShardingDslContext.batchInsert(insertRecords).execute()
        }
    }

    fun getProjectPipelineTriggerEventRecords(
        dslContext: DSLContext,
        projectId: String,
        limit: Int,
        offset: Int
    ): List<TPipelineTriggerEventRecord> {
        with(TPipelineTriggerEvent.T_PIPELINE_TRIGGER_EVENT) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .orderBy(EVENT_ID.asc(), CREATE_TIME.asc())
                .limit(limit).offset(offset).fetchInto(TPipelineTriggerEventRecord::class.java)
        }
    }

    fun migrateProjectPipelineTriggerEventData(
        migratingShardingDslContext: DSLContext,
        pipelineTriggerEventRecords: List<TPipelineTriggerEventRecord>
    ) {
        with(TPipelineTriggerEvent.T_PIPELINE_TRIGGER_EVENT) {
            val insertRecords = pipelineTriggerEventRecords.map { migratingShardingDslContext.newRecord(this, it) }
            migratingShardingDslContext.batchInsert(insertRecords).execute()
        }
    }

    fun getPipelineYamlInfoRecord(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String
    ): TPipelineYamlInfoRecord? {
        with(TPipelineYamlInfo.T_PIPELINE_YAML_INFO) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId).and(PIPELINE_ID.eq(pipelineId)))
                .fetchOne()
        }
    }

    fun migratePipelineYamlInfoData(
        migratingShardingDslContext: DSLContext,
        pipelineYamlInfoRecord: TPipelineYamlInfoRecord
    ) {
        with(TPipelineYamlInfo.T_PIPELINE_YAML_INFO) {
            val insertRecord = migratingShardingDslContext.newRecord(this, pipelineYamlInfoRecord)
            migratingShardingDslContext.executeInsert(insertRecord)
        }
    }

    fun getPipelineYamlVersionRecords(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        limit: Int,
        offset: Int
    ): List<TPipelineYamlVersionRecord> {
        with(TPipelineYamlVersion.T_PIPELINE_YAML_VERSION) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId).and(PIPELINE_ID.eq(pipelineId)))
                .orderBy(ID.asc())
                .limit(limit).offset(offset).fetchInto(TPipelineYamlVersionRecord::class.java)
        }
    }

    fun migratePipelineYamlVersionData(
        migratingShardingDslContext: DSLContext,
        pipelineYamlVersionRecords: List<TPipelineYamlVersionRecord>
    ) {
        with(TPipelineYamlVersion.T_PIPELINE_YAML_VERSION) {
            val insertRecords = pipelineYamlVersionRecords.map { migratingShardingDslContext.newRecord(this, it) }
            migratingShardingDslContext.batchInsert(insertRecords).execute()
        }
    }

    fun getProjectPipelineYamlSyncRecords(
        dslContext: DSLContext,
        projectId: String,
        limit: Int,
        offset: Int
    ): List<TPipelineYamlSyncRecord> {
        with(TPipelineYamlSync.T_PIPELINE_YAML_SYNC) {
            return dslContext.selectFrom(this).where(PROJECT_ID.eq(projectId))
                .orderBy(REPO_HASH_ID.asc(), FILE_PATH.asc()).limit(limit).offset(offset)
                .fetchInto(TPipelineYamlSyncRecord::class.java)
        }
    }

    fun migrateProjectPipelineYamlSyncData(
        migratingShardingDslContext: DSLContext,
        pipelineYamlSyncRecords: List<TPipelineYamlSyncRecord>
    ) {
        with(TPipelineYamlSync.T_PIPELINE_YAML_SYNC) {
            val insertRecords = pipelineYamlSyncRecords.map { migratingShardingDslContext.newRecord(this, it) }
            migratingShardingDslContext.batchInsert(insertRecords).execute()
        }
    }

    fun getProjectPipelineYamlBranchFileRecords(
        dslContext: DSLContext,
        projectId: String,
        limit: Int,
        offset: Int
    ): List<TPipelineYamlBranchFileRecord> {
        with(TPipelineYamlBranchFile.T_PIPELINE_YAML_BRANCH_FILE) {
            return dslContext.selectFrom(this).where(PROJECT_ID.eq(projectId))
                .orderBy(REPO_HASH_ID.asc(), BRANCH.asc(), FILE_PATH_MD5.asc()).limit(limit).offset(offset)
                .fetchInto(TPipelineYamlBranchFileRecord::class.java)
        }
    }

    fun migrateProjectPipelineYamlBranchFileData(
        migratingShardingDslContext: DSLContext,
        pipelineYamlBranchFileRecords: List<TPipelineYamlBranchFileRecord>
    ) {
        with(TPipelineYamlBranchFile.T_PIPELINE_YAML_BRANCH_FILE) {
            val insertRecords = pipelineYamlBranchFileRecords.map { migratingShardingDslContext.newRecord(this, it) }
            migratingShardingDslContext.batchInsert(insertRecords).execute()
        }
    }

    fun getProjectPipelineYamlViewRecords(
        dslContext: DSLContext,
        projectId: String,
        limit: Int,
        offset: Int
    ): List<TPipelineYamlViewRecord> {
        with(TPipelineYamlView.T_PIPELINE_YAML_VIEW) {
            return dslContext.selectFrom(this).where(PROJECT_ID.eq(projectId))
                .orderBy(REPO_HASH_ID.asc(), DIRECTORY.asc()).limit(limit).offset(offset)
                .fetchInto(TPipelineYamlViewRecord::class.java)
        }
    }

    fun migrateProjectPipelineYamlViewData(
        migratingShardingDslContext: DSLContext,
        pipelineYamlViewRecords: List<TPipelineYamlViewRecord>
    ) {
        with(TPipelineYamlView.T_PIPELINE_YAML_VIEW) {
            val insertRecords = pipelineYamlViewRecords.map { migratingShardingDslContext.newRecord(this, it) }
            migratingShardingDslContext.batchInsert(insertRecords).execute()
        }
    }

    fun getPipelineOperationLogRecords(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        limit: Int,
        offset: Int
    ): List<TPipelineOperationLogRecord> {
        with(TPipelineOperationLog.T_PIPELINE_OPERATION_LOG) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId).and(PIPELINE_ID.eq(pipelineId)))
                .orderBy(ID.asc())
                .limit(limit).offset(offset).fetchInto(TPipelineOperationLogRecord::class.java)
        }
    }

    fun migratePipelineOperationLogData(
        migratingShardingDslContext: DSLContext,
        pipelineOperationLogRecords: List<TPipelineOperationLogRecord>
    ) {
        with(TPipelineOperationLog.T_PIPELINE_OPERATION_LOG) {
            val insertRecords = pipelineOperationLogRecords.map { migratingShardingDslContext.newRecord(this, it) }
            migratingShardingDslContext.batchInsert(insertRecords).execute()
        }
    }

    fun getPipelineWebhookVersionRecords(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        limit: Int,
        offset: Int
    ): List<TPipelineWebhookVersionRecord> {
        with(TPipelineWebhookVersion.T_PIPELINE_WEBHOOK_VERSION) {
            return dslContext.selectFrom(this).where(PROJECT_ID.eq(projectId).and(PIPELINE_ID.eq(pipelineId)))
                .orderBy(VERSION.asc(), TASK_ID.asc()).limit(limit).offset(offset)
                .fetchInto(TPipelineWebhookVersionRecord::class.java)
        }
    }

    fun migratePipelineWebhookVersionData(
        migratingShardingDslContext: DSLContext,
        pipelineWebhookVersionRecords: List<TPipelineWebhookVersionRecord>
    ) {
        with(TPipelineWebhookVersion.T_PIPELINE_WEBHOOK_VERSION) {
            val insertRecords = pipelineWebhookVersionRecords.map { migratingShardingDslContext.newRecord(this, it) }
            migratingShardingDslContext.batchInsert(insertRecords).execute()
        }
    }

    fun getPipelineCallbackRecords(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        limit: Int,
        offset: Int
    ): List<TPipelineCallbackRecord> {
        with(TPipelineCallback.T_PIPELINE_CALLBACK) {
            return dslContext.selectFrom(this).where(PROJECT_ID.eq(projectId).and(PIPELINE_ID.eq(pipelineId)))
                .orderBy(NAME.asc()).limit(limit).offset(offset)
                .fetchInto(TPipelineCallbackRecord::class.java)
        }
    }

    fun migratePipelineCallbackData(
        migratingShardingDslContext: DSLContext,
        pipelineCallbackRecords: List<TPipelineCallbackRecord>
    ) {
        with(TPipelineCallback.T_PIPELINE_CALLBACK) {
            val insertRecords = pipelineCallbackRecords.map { migratingShardingDslContext.newRecord(this, it) }
            migratingShardingDslContext.batchInsert(insertRecords).execute()
        }
    }

    fun getPipelineSubRefRecords(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        limit: Int,
        offset: Int
    ): List<TPipelineSubRefRecord> {
        with(TPipelineSubRef.T_PIPELINE_SUB_REF) {
            return dslContext.selectFrom(this).where(PROJECT_ID.eq(projectId).and(PIPELINE_ID.eq(pipelineId)))
                .orderBy(TASK_ID.asc()).limit(limit).offset(offset)
                .fetchInto(TPipelineSubRefRecord::class.java)
        }
    }

    fun migratePipelineSubRefData(
        migratingShardingDslContext: DSLContext,
        pipelineSubRefRecords: List<TPipelineSubRefRecord>
    ) {
        with(TPipelineSubRef.T_PIPELINE_SUB_REF) {
            val insertRecords = pipelineSubRefRecords.map { migratingShardingDslContext.newRecord(this, it) }
            migratingShardingDslContext.batchInsert(insertRecords).execute()
        }
    }
}
