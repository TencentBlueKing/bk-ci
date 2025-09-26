package com.tencent.devops.process.engine.dao

import com.tencent.devops.common.api.constant.ID
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.pipeline.template.PipelineTemplateType
import com.tencent.devops.model.process.tables.TPipelineBuildVersionDiff
import com.tencent.devops.model.process.tables.records.TPipelineBuildVersionDiffRecord
import com.tencent.devops.process.pojo.BuildVersionDiffInfo
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class PipelineBuildVersionDiffDao {
    fun create(
        dslContext: DSLContext,
        buildVersionDiffInfo: BuildVersionDiffInfo
    ) {
        with(TPipelineBuildVersionDiff.T_PIPELINE_BUILD_VERSION_DIFF) {
            dslContext.insertInto(
                this,
                ID,
                PROJECT_ID,
                PIPELINE_ID,
                BUILD_ID,
                TEMPLATE_TYPE,
                TEMPLATE_NAME,
                TEMPLATE_ID,
                TEMPLATE_VERSION_NAME,
                PREV_TEMPLATE_VERSION,
                CURR_TEMPLATE_VERSION,
                PREV_TEMPLATE_VERSION_REF,
                CURR_TEMPLATE_VERSION_REF
            ).values(
                UUIDUtil.generate(),
                buildVersionDiffInfo.projectId,
                buildVersionDiffInfo.pipelineId,
                buildVersionDiffInfo.buildId,
                buildVersionDiffInfo.templateType.name,
                buildVersionDiffInfo.templateName,
                buildVersionDiffInfo.templateId,
                buildVersionDiffInfo.templateVersionName,
                buildVersionDiffInfo.prevTemplateVersion,
                buildVersionDiffInfo.currTemplateVersion,
                buildVersionDiffInfo.prevTemplateVersionRef,
                buildVersionDiffInfo.currTemplateVersionRef
            ).execute()
        }
    }

    fun list(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String
    ): List<BuildVersionDiffInfo> {
        with(TPipelineBuildVersionDiff.T_PIPELINE_BUILD_VERSION_DIFF) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(BUILD_ID.eq(buildId))
                .fetch().map { it.convert() }
        }
    }

    fun delete(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String
    ): Int {
        with(TPipelineBuildVersionDiff.T_PIPELINE_BUILD_VERSION_DIFF) {
            return dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(BUILD_ID.eq(buildId))
                .execute()
        }
    }

    fun TPipelineBuildVersionDiffRecord.convert(): BuildVersionDiffInfo {
        return BuildVersionDiffInfo(
            projectId = this.projectId,
            pipelineId = this.pipelineId,
            buildId = this.buildId,
            templateType = PipelineTemplateType.valueOf(this.templateType),
            templateName = this.templateName,
            templateId = this.templateId,
            templateVersionName = this.templateVersionName,
            prevTemplateVersion = this.prevTemplateVersion,
            currTemplateVersion = this.currTemplateVersion,
            prevTemplateVersionRef = this.prevTemplateVersionRef,
            currTemplateVersionRef = this.currTemplateVersionRef
        )
    }
}
