package com.tencent.devops.process.dao

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.model.process.tables.TPipelineBuildTemplateAcrossInfo
import com.tencent.devops.model.process.tables.records.TPipelineBuildTemplateAcrossInfoRecord
import com.tencent.devops.process.pojo.BuildTemplateAcrossInfo
import com.tencent.devops.process.pojo.TemplateAcrossInfoType
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class PipelineBuildTemplateAcrossInfoDao {
    fun create(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String? = null,
        templateId: String,
        templateType: TemplateAcrossInfoType,
        templateInstancesIds: List<String>,
        targetProjectId: String,
        userId: String
    ) {
        with(TPipelineBuildTemplateAcrossInfo.T_PIPELINE_BUILD_TEMPLATE_ACROSS_INFO) {
            dslContext.insertInto(
                this,
                TEMPLATE_ID,
                PROJECT_ID,
                PIPELINE_ID,
                BUILD_ID,
                TEMPLATE_TYPE,
                TEMPLATE_INSTANCE_IDS,
                TARGET_PROJECT_ID,
                CREATE_TIME,
                CREATOR
            ).values(
                templateId,
                projectId,
                pipelineId,
                buildId,
                templateType.name,
                JsonUtil.toJson(templateInstancesIds),
                targetProjectId,
                LocalDateTime.now(),
                userId
            ).execute()
        }
    }

    fun batchCreate(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String? = null,
        userId: String,
        templateAcrossInfos: List<BuildTemplateAcrossInfo>
    ) {
        if (templateAcrossInfos.isEmpty()) {
            return
        }
        templateAcrossInfos.forEach { info ->
            with(TPipelineBuildTemplateAcrossInfo.T_PIPELINE_BUILD_TEMPLATE_ACROSS_INFO) {
                dslContext.insertInto(
                    this,
                    TEMPLATE_ID,
                    PROJECT_ID,
                    PIPELINE_ID,
                    BUILD_ID,
                    TEMPLATE_TYPE,
                    TEMPLATE_INSTANCE_IDS,
                    TARGET_PROJECT_ID,
                    CREATE_TIME,
                    CREATOR
                ).values(
                    info.templateId,
                    projectId,
                    pipelineId,
                    buildId,
                    info.templateType.name,
                    JsonUtil.toJson(info.templateInstancesIds),
                    info.targetProjectId,
                    LocalDateTime.now(),
                    userId
                ).execute()
            }
        }
    }

    fun batchUpdate(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        templateAcrossInfos: List<BuildTemplateAcrossInfo>
    ) {
        if (templateAcrossInfos.isEmpty()) {
            return
        }

        with(TPipelineBuildTemplateAcrossInfo.T_PIPELINE_BUILD_TEMPLATE_ACROSS_INFO) {
            dslContext.batched { c ->
                templateAcrossInfos.forEach { info ->
                    c.dsl().update(this)
                        .set(TEMPLATE_INSTANCE_IDS, JsonUtil.toJson(info.templateInstancesIds, formatted = false))
                        .where(PROJECT_ID.eq(projectId))
                        .and(PIPELINE_ID.eq(pipelineId))
                        .and(BUILD_ID.eq(buildId))
                        .and(TEMPLATE_TYPE.eq(info.templateType.name))
                        .execute()
                }
            }
        }
    }

    fun get(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        templateId: String
    ): List<TPipelineBuildTemplateAcrossInfoRecord> {
        with(TPipelineBuildTemplateAcrossInfo.T_PIPELINE_BUILD_TEMPLATE_ACROSS_INFO) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(TEMPLATE_ID.eq(templateId))
                .and(PIPELINE_ID.eq(pipelineId))
                .fetch()
        }
    }

    fun getByTemplateId(
        dslContext: DSLContext,
        projectId: String,
        templateId: String
    ): List<TPipelineBuildTemplateAcrossInfoRecord> {
        with(TPipelineBuildTemplateAcrossInfo.T_PIPELINE_BUILD_TEMPLATE_ACROSS_INFO) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(TEMPLATE_ID.eq(templateId))
                .fetch()
        }
    }

    fun updateBuildId(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        templateId: String,
        buildId: String
    ): Int {
        with(TPipelineBuildTemplateAcrossInfo.T_PIPELINE_BUILD_TEMPLATE_ACROSS_INFO) {
            return dslContext.update(this)
                .set(BUILD_ID, buildId)
                .where(PROJECT_ID.eq(projectId))
                .and(TEMPLATE_ID.eq(templateId))
                .and(PIPELINE_ID.eq(pipelineId))
                .execute()
        }
    }

    fun delete(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        templateId: String
    ): Int {
        with(TPipelineBuildTemplateAcrossInfo.T_PIPELINE_BUILD_TEMPLATE_ACROSS_INFO) {
            return dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(TEMPLATE_ID.eq(templateId))
                .and(PIPELINE_ID.eq(pipelineId))
                .execute()
        }
    }

    fun deleteByPipelineId(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String
    ): Int {
        with(TPipelineBuildTemplateAcrossInfo.T_PIPELINE_BUILD_TEMPLATE_ACROSS_INFO) {
            return dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .execute()
        }
    }

    fun deleteByBuildId(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String
    ): Int {
        with(TPipelineBuildTemplateAcrossInfo.T_PIPELINE_BUILD_TEMPLATE_ACROSS_INFO) {
            return dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(BUILD_ID.eq(buildId))
                .execute()
        }
    }
}
