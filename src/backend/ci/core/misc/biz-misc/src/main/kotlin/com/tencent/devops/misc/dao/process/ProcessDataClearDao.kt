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

package com.tencent.devops.misc.dao.process

import com.tencent.devops.model.process.tables.TPipelineBuildCommits
import com.tencent.devops.model.process.tables.TPipelineBuildContainer
import com.tencent.devops.model.process.tables.TPipelineBuildDetail
import com.tencent.devops.model.process.tables.TPipelineBuildHistory
import com.tencent.devops.model.process.tables.TPipelineBuildRecordContainer
import com.tencent.devops.model.process.tables.TPipelineBuildRecordModel
import com.tencent.devops.model.process.tables.TPipelineBuildRecordStage
import com.tencent.devops.model.process.tables.TPipelineBuildRecordTask
import com.tencent.devops.model.process.tables.TPipelineBuildStage
import com.tencent.devops.model.process.tables.TPipelineBuildSummary
import com.tencent.devops.model.process.tables.TPipelineBuildTask
import com.tencent.devops.model.process.tables.TPipelineBuildTemplateAcrossInfo
import com.tencent.devops.model.process.tables.TPipelineBuildVar
import com.tencent.devops.model.process.tables.TPipelineInfo
import com.tencent.devops.model.process.tables.TPipelineLabelPipeline
import com.tencent.devops.model.process.tables.TPipelineModelTask
import com.tencent.devops.model.process.tables.TPipelineRemoteAuth
import com.tencent.devops.model.process.tables.TPipelineResource
import com.tencent.devops.model.process.tables.TPipelineResourceVersion
import com.tencent.devops.model.process.tables.TPipelineSetting
import com.tencent.devops.model.process.tables.TPipelineSettingVersion
import com.tencent.devops.model.process.tables.TPipelineTimer
import com.tencent.devops.model.process.tables.TPipelineViewGroup
import com.tencent.devops.model.process.tables.TPipelineWebhook
import com.tencent.devops.model.process.tables.TPipelineWebhookBuildParameter
import com.tencent.devops.model.process.tables.TReport
import com.tencent.devops.model.process.tables.TTemplatePipeline
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
@Suppress("TooManyFunctions")
class ProcessDataClearDao {

    fun deleteBuildTaskByBuildId(dslContext: DSLContext, projectId: String, buildId: String) {
        with(TPipelineBuildTask.T_PIPELINE_BUILD_TASK) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(BUILD_ID.eq(buildId))
                .execute()
        }
    }

    fun deleteBuildVarByBuildId(dslContext: DSLContext, projectId: String, buildId: String) {
        with(TPipelineBuildVar.T_PIPELINE_BUILD_VAR) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(BUILD_ID.eq(buildId))
                .execute()
        }
    }

    fun deleteBuildContainerByBuildId(dslContext: DSLContext, projectId: String, buildId: String) {
        with(TPipelineBuildContainer.T_PIPELINE_BUILD_CONTAINER) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(BUILD_ID.eq(buildId))
                .execute()
        }
    }

    fun deleteBuildStageByBuildId(dslContext: DSLContext, projectId: String, buildId: String) {
        with(TPipelineBuildStage.T_PIPELINE_BUILD_STAGE) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(BUILD_ID.eq(buildId))
                .execute()
        }
    }

    fun deleteBuildDetailByBuildId(dslContext: DSLContext, projectId: String, buildId: String) {
        with(TPipelineBuildDetail.T_PIPELINE_BUILD_DETAIL) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(BUILD_ID.eq(buildId))
                .execute()
        }
    }

    fun deleteReportByBuildId(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String
    ) {
        with(TReport.T_REPORT) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(BUILD_ID.eq(buildId))
                .execute()
        }
    }

    fun deleteBuildHistoryByBuildId(dslContext: DSLContext, projectId: String, buildId: String) {
        with(TPipelineBuildHistory.T_PIPELINE_BUILD_HISTORY) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(BUILD_ID.eq(buildId))
                .execute()
        }
    }

    fun deleteBuildRecordPipelineByBuildId(dslContext: DSLContext, projectId: String, buildId: String) {
        with(TPipelineBuildRecordModel.T_PIPELINE_BUILD_RECORD_MODEL) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(BUILD_ID.eq(buildId))
                .execute()
        }
    }

    fun deleteBuildRecordStageByBuildId(dslContext: DSLContext, projectId: String, buildId: String) {
        with(TPipelineBuildRecordStage.T_PIPELINE_BUILD_RECORD_STAGE) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(BUILD_ID.eq(buildId))
                .execute()
        }
    }

    fun deleteBuildRecordContainerByBuildId(dslContext: DSLContext, projectId: String, buildId: String) {
        with(TPipelineBuildRecordContainer.T_PIPELINE_BUILD_RECORD_CONTAINER) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(BUILD_ID.eq(buildId))
                .execute()
        }
    }

    fun deleteBuildRecordTaskByBuildId(dslContext: DSLContext, projectId: String, buildId: String) {
        with(TPipelineBuildRecordTask.T_PIPELINE_BUILD_RECORD_TASK) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(BUILD_ID.eq(buildId))
                .execute()
        }
    }

    fun deleteBuildWebhookParameter(dslContext: DSLContext, projectId: String, buildId: String) {
        with(TPipelineWebhookBuildParameter.T_PIPELINE_WEBHOOK_BUILD_PARAMETER) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(BUILD_ID.eq(buildId))
                .execute()
        }
    }

    fun deleteBuildCommits(dslContext: DSLContext, projectId: String, buildId: String) {
        with(TPipelineBuildCommits.T_PIPELINE_BUILD_COMMITS) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(BUILD_ID.eq(buildId))
                .execute()
        }
    }

    fun deletePipelineLabelByPipelineId(dslContext: DSLContext, projectId: String, pipelineId: String) {
        with(TPipelineLabelPipeline.T_PIPELINE_LABEL_PIPELINE) {
            dslContext.deleteFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(PROJECT_ID.eq(projectId))
                .execute()
        }
    }

    fun deletePipelineModelTaskByPipelineId(dslContext: DSLContext, projectId: String, pipelineId: String) {
        with(TPipelineModelTask.T_PIPELINE_MODEL_TASK) {
            dslContext.deleteFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(PROJECT_ID.eq(projectId))
                .execute()
        }
    }

    fun deletePipelineRemoteAuthByPipelineId(dslContext: DSLContext, projectId: String, pipelineId: String) {
        with(TPipelineRemoteAuth.T_PIPELINE_REMOTE_AUTH) {
            dslContext.deleteFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(PROJECT_ID.eq(projectId))
                .execute()
        }
    }

    fun deletePipelineResourceByPipelineId(dslContext: DSLContext, projectId: String, pipelineId: String) {
        with(TPipelineResource.T_PIPELINE_RESOURCE) {
            dslContext.deleteFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(PROJECT_ID.eq(projectId))
                .execute()
        }
    }

    fun deletePipelineResourceVersionByPipelineId(dslContext: DSLContext, projectId: String, pipelineId: String) {
        with(TPipelineResourceVersion.T_PIPELINE_RESOURCE_VERSION) {
            dslContext.deleteFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(PROJECT_ID.eq(projectId))
                .execute()
        }
    }

    fun deletePipelineSettingByPipelineId(dslContext: DSLContext, projectId: String, pipelineId: String) {
        with(TPipelineSetting.T_PIPELINE_SETTING) {
            dslContext.deleteFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(PROJECT_ID.eq(projectId))
                .execute()
        }
    }

    fun deletePipelineSettingVersionByPipelineId(dslContext: DSLContext, projectId: String, pipelineId: String) {
        with(TPipelineSettingVersion.T_PIPELINE_SETTING_VERSION) {
            dslContext.deleteFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(PROJECT_ID.eq(projectId))
                .execute()
        }
    }

    fun deletePipelineTimerByPipelineId(dslContext: DSLContext, projectId: String, pipelineId: String) {
        with(TPipelineTimer.T_PIPELINE_TIMER) {
            dslContext.deleteFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(PROJECT_ID.eq(projectId))
                .execute()
        }
    }

    fun deletePipelineWebhookByPipelineId(dslContext: DSLContext, projectId: String, pipelineId: String) {
        with(TPipelineWebhook.T_PIPELINE_WEBHOOK) {
            dslContext.deleteFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(PROJECT_ID.eq(projectId))
                .execute()
        }
    }

    fun deleteTemplatePipelineByPipelineId(dslContext: DSLContext, projectId: String, pipelineId: String) {
        with(TTemplatePipeline.T_TEMPLATE_PIPELINE) {
            dslContext.deleteFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(PROJECT_ID.eq(projectId))
                .execute()
        }
    }

    fun deletePipelineBuildSummaryByPipelineId(dslContext: DSLContext, projectId: String, pipelineId: String) {
        with(TPipelineBuildSummary.T_PIPELINE_BUILD_SUMMARY) {
            dslContext.deleteFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(PROJECT_ID.eq(projectId))
                .execute()
        }
    }

    fun deletePipelineInfoByPipelineId(dslContext: DSLContext, projectId: String, pipelineId: String) {
        with(TPipelineInfo.T_PIPELINE_INFO) {
            dslContext.deleteFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(PROJECT_ID.eq(projectId))
                .execute()
        }
    }

    fun deletePipelineTemplateAcrossInfo(dslContext: DSLContext, projectId: String, pipelineId: String) {
        with(TPipelineBuildTemplateAcrossInfo.T_PIPELINE_BUILD_TEMPLATE_ACROSS_INFO) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .execute()
        }
    }

    fun deletePipelineBuildTemplateAcrossInfo(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String
    ) {
        with(TPipelineBuildTemplateAcrossInfo.T_PIPELINE_BUILD_TEMPLATE_ACROSS_INFO) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(BUILD_ID.eq(buildId))
                .execute()
        }
    }

    fun deletePipelineViewGroup(dslContext: DSLContext, projectId: String, pipelineId: String) {
        with(TPipelineViewGroup.T_PIPELINE_VIEW_GROUP) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .execute()
        }
    }
}
