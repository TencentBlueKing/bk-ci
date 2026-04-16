package com.tencent.devops.process.dao

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.model.process.Tables.T_PIPELINE_TEMPLATE_RESOURCE_DRAFT_VERSION
import com.tencent.devops.model.process.tables.records.TPipelineTemplateResourceDraftVersionRecord
import com.tencent.devops.process.pojo.template.PipelineTemplateDraftVersionSimple
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateResource
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateResourceDraftVersion
import org.jooq.DSLContext
import org.jooq.RecordMapper
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class PipelineTemplateResourceDraftVersionDao {

    fun create(
        dslContext: DSLContext,
        userId: String,
        pipelineTemplateResource: PipelineTemplateResource,
        draftVersion: Int,
        baseDraftVersion: Int?
    ) {
        with(T_PIPELINE_TEMPLATE_RESOURCE_DRAFT_VERSION) {
            val modelStr = JsonUtil.toJson(pipelineTemplateResource.model, formatted = false)
            val params = pipelineTemplateResource.params?.let {
                JsonUtil.toJson(it, formatted = false)
            }
            val createTime = LocalDateTime.now()
            dslContext.insertInto(this)
                .set(PROJECT_ID, pipelineTemplateResource.projectId)
                .set(TEMPLATE_ID, pipelineTemplateResource.templateId)
                .set(VERSION, pipelineTemplateResource.version)
                .set(DRAFT_VERSION, draftVersion)
                .set(SETTING_VERSION, pipelineTemplateResource.settingVersion)
                .set(TYPE, pipelineTemplateResource.type.name)
                .set(SRC_TEMPLATE_PROJECT_ID, pipelineTemplateResource.srcTemplateProjectId)
                .set(SRC_TEMPLATE_ID, pipelineTemplateResource.srcTemplateId)
                .set(SRC_TEMPLATE_VERSION, pipelineTemplateResource.srcTemplateVersion)
                .set(BASE_VERSION, pipelineTemplateResource.baseVersion)
                .set(BASE_VERSION_NAME, pipelineTemplateResource.baseVersionName)
                .set(BASE_DRAFT_VERSION, baseDraftVersion)
                .set(PARAMS, params)
                .set(MODEL, modelStr)
                .set(YAML, pipelineTemplateResource.yaml)
                .set(CREATOR, userId)
                .set(CREATED_TIME, createTime)
                .set(UPDATER, userId)
                .set(UPDATE_TIME, createTime)
                .execute()
        }
    }

    fun get(
        dslContext: DSLContext,
        projectId: String,
        templateId: String,
        version: Long,
        draftVersion: Int
    ): PipelineTemplateResourceDraftVersion? {
        with(T_PIPELINE_TEMPLATE_RESOURCE_DRAFT_VERSION) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(TEMPLATE_ID.eq(templateId))
                .and(VERSION.eq(version))
                .and(DRAFT_VERSION.eq(draftVersion))
                .fetchOne(mapper)
        }
    }

    fun getLatest(
        dslContext: DSLContext,
        projectId: String,
        templateId: String,
        version: Long
    ): PipelineTemplateResourceDraftVersion? {
        with(T_PIPELINE_TEMPLATE_RESOURCE_DRAFT_VERSION) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(TEMPLATE_ID.eq(templateId))
                .and(VERSION.eq(version))
                .orderBy(CREATED_TIME.desc())
                .limit(1)
                .fetchOne(mapper)
        }
    }

    fun update(
        dslContext: DSLContext,
        userId: String,
        pipelineTemplateResource: PipelineTemplateResource,
        draftVersion: Int,
        baseDraftVersion: Int?
    ) {
        with(T_PIPELINE_TEMPLATE_RESOURCE_DRAFT_VERSION) {
            val modelStr = JsonUtil.toJson(pipelineTemplateResource.model, formatted = false)
            val params = pipelineTemplateResource.params?.let {
                JsonUtil.toJson(it, formatted = false)
            }
            dslContext.update(this)
                .set(SETTING_VERSION, pipelineTemplateResource.settingVersion)
                .set(TYPE, pipelineTemplateResource.type.name)
                .set(SRC_TEMPLATE_PROJECT_ID, pipelineTemplateResource.srcTemplateProjectId)
                .set(SRC_TEMPLATE_ID, pipelineTemplateResource.srcTemplateId)
                .set(SRC_TEMPLATE_VERSION, pipelineTemplateResource.srcTemplateVersion)
                .set(BASE_VERSION, pipelineTemplateResource.baseVersion)
                .set(BASE_VERSION_NAME, pipelineTemplateResource.baseVersionName)
                .set(BASE_DRAFT_VERSION, baseDraftVersion)
                .set(PARAMS, params)
                .set(MODEL, modelStr)
                .set(YAML, pipelineTemplateResource.yaml)
                .set(UPDATER, userId)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(PROJECT_ID.eq(pipelineTemplateResource.projectId))
                .and(TEMPLATE_ID.eq(pipelineTemplateResource.templateId))
                .and(VERSION.eq(pipelineTemplateResource.version))
                .and(DRAFT_VERSION.eq(draftVersion))
                .execute()
        }
    }

    fun count(
        dslContext: DSLContext,
        projectId: String,
        templateId: String,
        version: Long
    ): Long {
        return with(T_PIPELINE_TEMPLATE_RESOURCE_DRAFT_VERSION) {
            dslContext.selectCount()
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(TEMPLATE_ID.eq(templateId))
                .and(VERSION.eq(version))
                .fetchOne(0, Long::class.java)!!
        }
    }

    fun list(
        dslContext: DSLContext,
        projectId: String,
        templateId: String,
        version: Long,
        limit: Int,
        offset: Int
    ): List<PipelineTemplateDraftVersionSimple> {
        with(T_PIPELINE_TEMPLATE_RESOURCE_DRAFT_VERSION) {
            val query = dslContext.select(
                PROJECT_ID,
                TEMPLATE_ID,
                VERSION,
                DRAFT_VERSION,
                BASE_VERSION,
                BASE_VERSION_NAME,
                BASE_DRAFT_VERSION,
                CREATOR,
                CREATED_TIME,
                UPDATER,
                UPDATE_TIME
            ).from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(TEMPLATE_ID.eq(templateId))
                .and(VERSION.eq(version))
            return query.orderBy(DRAFT_VERSION.desc())
                .limit(limit).offset(offset)
                .fetch().map {
                    PipelineTemplateDraftVersionSimple(
                        projectId = it.value1(),
                        templateId = it.value2(),
                        version = it.value3(),
                        draftVersion = it.value4(),
                        baseVersion = it.value5(),
                        baseVersionName = it.value6(),
                        baseDraftVersion = it.value7(),
                        creator = it.value8(),
                        createTime = it.value9().timestampmilli(),
                        updater = it.value10(),
                        updateTime = it.value11().timestampmilli()
                    )
                }
        }
    }

    private class TemplateResourceDraftVersionJooqMapper :
        RecordMapper<TPipelineTemplateResourceDraftVersionRecord, PipelineTemplateResourceDraftVersion> {
        override fun map(record: TPipelineTemplateResourceDraftVersionRecord?): PipelineTemplateResourceDraftVersion? {
            return record?.let { r ->
                PipelineTemplateResourceDraftVersion(
                    projectId = r.projectId,
                    templateId = r.templateId,
                    version = r.version,
                    draftVersion = r.draftVersion,
                    settingVersion = r.settingVersion,
                    type = r.type,
                    srcTemplateProjectId = r.srcTemplateProjectId,
                    srcTemplateId = r.srcTemplateId,
                    srcTemplateVersion = r.srcTemplateVersion,
                    baseVersion = r.baseVersion,
                    baseVersionName = r.baseVersionName,
                    baseDraftVersion = r.baseDraftVersion,
                    params = r.params?.let { JsonUtil.to(it, object : TypeReference<List<BuildFormProperty>>() {}) },
                    model = JsonUtil.to(r.model, Model::class.java),
                    yaml = r.yaml,
                    creator = r.creator,
                    createTime = r.createdTime.timestampmilli(),
                    updater = r.updater,
                    updateTime = r.updateTime.timestampmilli()
                )
            }
        }
    }

    companion object {
        private val mapper = TemplateResourceDraftVersionJooqMapper()
    }
}
