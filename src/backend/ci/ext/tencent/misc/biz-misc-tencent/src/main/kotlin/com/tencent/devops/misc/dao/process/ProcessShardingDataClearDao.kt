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

import com.tencent.devops.model.process.tables.TAuditResource
import com.tencent.devops.model.process.tables.TPipelineBuildDetail
import com.tencent.devops.model.process.tables.TPipelineBuildHistory
import com.tencent.devops.model.process.tables.TPipelineBuildSummary
import com.tencent.devops.model.process.tables.TPipelineFailureBuild
import com.tencent.devops.model.process.tables.TPipelineFavor
import com.tencent.devops.model.process.tables.TPipelineGroup
import com.tencent.devops.model.process.tables.TPipelineInfo
import com.tencent.devops.model.process.tables.TPipelineJobMutexGroup
import com.tencent.devops.model.process.tables.TPipelineLabel
import com.tencent.devops.model.process.tables.TPipelineLabelPipeline
import com.tencent.devops.model.process.tables.TPipelineModelTask
import com.tencent.devops.model.process.tables.TPipelinePauseValue
import com.tencent.devops.model.process.tables.TPipelineResource
import com.tencent.devops.model.process.tables.TPipelineResourceVersion
import com.tencent.devops.model.process.tables.TPipelineSetting
import com.tencent.devops.model.process.tables.TPipelineSettingVersion
import com.tencent.devops.model.process.tables.TPipelineTransferHistory
import com.tencent.devops.model.process.tables.TPipelineView
import com.tencent.devops.model.process.tables.TPipelineViewUserLastView
import com.tencent.devops.model.process.tables.TPipelineViewUserSettings
import com.tencent.devops.model.process.tables.TPipelineWebhookQueue
import com.tencent.devops.model.process.tables.TProjectPipelineCallback
import com.tencent.devops.model.process.tables.TReport
import com.tencent.devops.model.process.tables.TTemplate
import com.tencent.devops.model.process.tables.TTemplatePipeline
import com.tencent.devops.model.process.tables.TTemplateTransferHistory
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
@Suppress("TooManyFunctions")
class ProcessShardingDataClearDao {

    fun deleteAuditResourceByProjectId(dslContext: DSLContext, projectId: String) {
        with(TAuditResource.T_AUDIT_RESOURCE) {
            dslContext.deleteFrom(this)
                .where(RESOURCE_TYPE.eq("pipeline").and(PROJECT_ID.eq(projectId)))
                .execute()
        }
    }

    fun deletePipelineFavorByProjectId(dslContext: DSLContext, projectId: String) {
        with(TPipelineFavor.T_PIPELINE_FAVOR) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .execute()
        }
    }

    fun deletePipelineGroupByProjectId(dslContext: DSLContext, projectId: String) {
        with(TPipelineGroup.T_PIPELINE_GROUP) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .execute()
        }
    }

    fun deletePipelineInfoByProjectId(dslContext: DSLContext, projectId: String) {
        with(TPipelineInfo.T_PIPELINE_INFO) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .execute()
        }
    }

    fun deletePipelineJobMutexGroupByProjectId(dslContext: DSLContext, projectId: String) {
        with(TPipelineJobMutexGroup.T_PIPELINE_JOB_MUTEX_GROUP) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .execute()
        }
    }

    fun deletePipelineLabelByProjectId(dslContext: DSLContext, projectId: String) {
        with(TPipelineLabel.T_PIPELINE_LABEL) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .execute()
        }
    }

    fun deletePipelineLabelPipelineByProjectId(dslContext: DSLContext, projectId: String) {
        with(TPipelineLabelPipeline.T_PIPELINE_LABEL_PIPELINE) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .execute()
        }
    }

    fun deletePipelineTransferHistoryByProjectId(dslContext: DSLContext, projectId: String) {
        with(TPipelineTransferHistory.T_PIPELINE_TRANSFER_HISTORY) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .execute()
        }
    }

    fun deletePipelineViewByProjectId(dslContext: DSLContext, projectId: String) {
        with(TPipelineView.T_PIPELINE_VIEW) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .execute()
        }
    }

    fun deletePipelineViewUserLastViewByProjectId(dslContext: DSLContext, projectId: String) {
        with(TPipelineViewUserLastView.T_PIPELINE_VIEW_USER_LAST_VIEW) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .execute()
        }
    }

    fun deletePipelineViewUserSettingsByProjectId(dslContext: DSLContext, projectId: String) {
        with(TPipelineViewUserSettings.T_PIPELINE_VIEW_USER_SETTINGS) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .execute()
        }
    }

    fun deleteProjectPipelineCallbackByProjectId(dslContext: DSLContext, projectId: String) {
        with(TProjectPipelineCallback.T_PROJECT_PIPELINE_CALLBACK) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .execute()
        }
    }

    fun deleteTemplateByProjectId(dslContext: DSLContext, projectId: String) {
        with(TTemplate.T_TEMPLATE) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .execute()
        }
    }

    fun deleteTemplateTransferHistoryByProjectId(dslContext: DSLContext, projectId: String) {
        with(TTemplateTransferHistory.T_TEMPLATE_TRANSFER_HISTORY) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
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

    fun deletePipelineFailureBuildByPipelineId(dslContext: DSLContext, pipelineId: String) {
        with(TPipelineFailureBuild.T_PIPELINE_FAILURE_BUILD) {
            dslContext.deleteFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
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

    fun deletePipelineResourceByPipelineId(dslContext: DSLContext, pipelineId: String) {
        with(TPipelineResource.T_PIPELINE_RESOURCE) {
            dslContext.deleteFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .execute()
        }
    }

    fun deletePipelineResourceVersionByPipelineId(dslContext: DSLContext, pipelineId: String) {
        with(TPipelineResourceVersion.T_PIPELINE_RESOURCE_VERSION) {
            dslContext.deleteFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .execute()
        }
    }

    fun deletePipelineSettingByPipelineId(dslContext: DSLContext, pipelineId: String) {
        with(TPipelineSetting.T_PIPELINE_SETTING) {
            dslContext.deleteFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .execute()
        }
    }

    fun deletePipelineSettingVersionByPipelineId(dslContext: DSLContext, pipelineId: String) {
        with(TPipelineSettingVersion.T_PIPELINE_SETTING_VERSION) {
            dslContext.deleteFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .execute()
        }
    }

    fun deletePipelineWebhookQueueByPipelineId(dslContext: DSLContext, pipelineId: String) {
        with(TPipelineWebhookQueue.T_PIPELINE_WEBHOOK_QUEUE) {
            dslContext.deleteFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .execute()
        }
    }

    fun deleteTemplatePipelineByPipelineId(dslContext: DSLContext, pipelineId: String) {
        with(TTemplatePipeline.T_TEMPLATE_PIPELINE) {
            dslContext.deleteFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .execute()
        }
    }

    fun deleteBuildDetailByBuildId(dslContext: DSLContext, buildId: String) {
        with(TPipelineBuildDetail.T_PIPELINE_BUILD_DETAIL) {
            dslContext.deleteFrom(this)
                .where(BUILD_ID.eq(buildId))
                .execute()
        }
    }

    fun deletePipelinePauseValueByBuildId(dslContext: DSLContext, buildId: String) {
        with(TPipelinePauseValue.T_PIPELINE_PAUSE_VALUE) {
            dslContext.deleteFrom(this)
                .where(BUILD_ID.eq(buildId))
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

    fun deleteBuildHistoryByBuildId(dslContext: DSLContext, buildId: String) {
        with(TPipelineBuildHistory.T_PIPELINE_BUILD_HISTORY) {
            dslContext.deleteFrom(this)
                .where(BUILD_ID.eq(buildId))
                .execute()
        }
    }
}
