package com.tencent.devops.process.engine.dao

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.model.process.Tables.T_PIPELINE_RESOURCE_DRAFT_VERSION
import com.tencent.devops.model.process.tables.records.TPipelineResourceDraftVersionRecord
import com.tencent.devops.process.pojo.pipeline.PipelineDraftVersionSimple
import com.tencent.devops.process.pojo.pipeline.PipelineResourceDraftVersion
import com.tencent.devops.process.pojo.pipeline.PipelineResourceVersion
import org.jooq.DSLContext
import org.jooq.RecordMapper
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class PipelineResourceDraftVersionDao {

    fun create(
        dslContext: DSLContext,
        userId: String,
        pipelineResourceVersion: PipelineResourceVersion,
        draftVersion: Int,
        baseDraftVersion: Int?,
        baseVersionName: String?
    ) {
        with(T_PIPELINE_RESOURCE_DRAFT_VERSION) {
            val modelStr = JsonUtil.toJson(pipelineResourceVersion.model, formatted = false)
            val createTime = LocalDateTime.now()
            dslContext.insertInto(this)
                .set(PROJECT_ID, pipelineResourceVersion.projectId)
                .set(PIPELINE_ID, pipelineResourceVersion.pipelineId)
                .set(VERSION, pipelineResourceVersion.version)
                .set(DRAFT_VERSION, draftVersion)
                .set(MODEL, modelStr)
                .set(YAML, pipelineResourceVersion.yaml)
                .set(YAML_VERSION, pipelineResourceVersion.yamlVersion)
                .set(SETTING_VERSION, pipelineResourceVersion.settingVersion)
                .set(BASE_VERSION, pipelineResourceVersion.baseVersion)
                .set(BASE_VERSION_NAME, baseVersionName)
                .set(BASE_DRAFT_VERSION, baseDraftVersion)
                .set(CREATOR, userId)
                .set(CREATE_TIME, createTime)
                .set(UPDATER, userId)
                .set(UPDATE_TIME, createTime)
                .execute()
        }
    }

    fun get(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        version: Int,
        draftVersion: Int
    ): PipelineResourceDraftVersion? {
        with(T_PIPELINE_RESOURCE_DRAFT_VERSION) {
            return dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(PROJECT_ID.eq(projectId))
                .and(VERSION.eq(version))
                .and(DRAFT_VERSION.eq(draftVersion))
                .fetchOne(mapper)
        }
    }

    fun getVersionModelString(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        version: Int,
        draftVersion: Int
    ): String? {
        with(T_PIPELINE_RESOURCE_DRAFT_VERSION) {
            return dslContext.select(MODEL).from(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(PROJECT_ID.eq(projectId))
                .and(VERSION.eq(version))
                .and(DRAFT_VERSION.eq(draftVersion))
                .fetchOne(0, String::class.java)
        }
    }

    fun getLatest(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        version: Int
    ): PipelineResourceDraftVersion? {
        with(T_PIPELINE_RESOURCE_DRAFT_VERSION) {
            return dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(PROJECT_ID.eq(projectId))
                .and(VERSION.eq(version))
                .orderBy(CREATE_TIME.desc())
                .limit(1)
                .fetchOne(mapper)
        }
    }

    fun count(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        version: Int
    ): Long {
        with(T_PIPELINE_RESOURCE_DRAFT_VERSION) {
            return dslContext.selectCount().from(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(PROJECT_ID.eq(projectId))
                .and(VERSION.eq(version))
                .fetchOne(0, Long::class.java)!!
        }
    }

    fun list(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        version: Int,
        limit: Int,
        offset: Int
    ): List<PipelineDraftVersionSimple> {
        with(T_PIPELINE_RESOURCE_DRAFT_VERSION) {
            val query = dslContext.select(
                PROJECT_ID,
                PIPELINE_ID,
                VERSION,
                DRAFT_VERSION,
                BASE_VERSION,
                BASE_VERSION_NAME,
                BASE_DRAFT_VERSION,
                CREATOR,
                CREATE_TIME,
                UPDATER,
                UPDATE_TIME
            ).from(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(PROJECT_ID.eq(projectId))
                .and(VERSION.eq(version))
            return query.orderBy(DRAFT_VERSION.desc())
                .limit(limit).offset(offset)
                .fetch().map {
                    PipelineDraftVersionSimple(
                        projectId = it.value1(),
                        pipelineId = it.value2(),
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

    fun update(
        dslContext: DSLContext,
        userId: String,
        pipelineResourceVersion: PipelineResourceVersion,
        draftVersion: Int,
        baseDraftVersion: Int?
    ) {
        with(T_PIPELINE_RESOURCE_DRAFT_VERSION) {
            val modelStr = JsonUtil.toJson(pipelineResourceVersion.model, formatted = false)
            dslContext.update(this)
                .set(MODEL, modelStr)
                .set(YAML, pipelineResourceVersion.yaml)
                .set(YAML_VERSION, pipelineResourceVersion.yamlVersion)
                .set(SETTING_VERSION, pipelineResourceVersion.settingVersion)
                .set(BASE_VERSION, pipelineResourceVersion.baseVersion)
                .set(BASE_DRAFT_VERSION, baseDraftVersion)
                .set(UPDATER, userId)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(PROJECT_ID.eq(pipelineResourceVersion.projectId))
                .and(PIPELINE_ID.eq(pipelineResourceVersion.pipelineId))
                .and(VERSION.eq(pipelineResourceVersion.version))
                .and(DRAFT_VERSION.eq(draftVersion))
                .execute()
        }
    }

    private class PipelineResourceDraftVersionJooqMapper :
        RecordMapper<TPipelineResourceDraftVersionRecord, PipelineResourceDraftVersion> {
        override fun map(record: TPipelineResourceDraftVersionRecord?): PipelineResourceDraftVersion? {
            return record?.let { r ->
                PipelineResourceDraftVersion(
                    projectId = r.projectId,
                    pipelineId = r.pipelineId,
                    version = r.version,
                    draftVersion = r.draftVersion,
                    model = JsonUtil.to(r.model, Model::class.java),
                    yaml = r.yaml,
                    yamlVersion = r.yamlVersion,
                    settingVersion = r.settingVersion,
                    baseVersion = r.baseVersion,
                    baseVersionName = r.baseVersionName,
                    baseDraftVersion = r.baseDraftVersion,
                    creator = r.creator,
                    createTime = r.createTime,
                    updater = r.updater,
                    updateTime = r.updateTime
                )
            }
        }
    }

    companion object {
        private val mapper = PipelineResourceDraftVersionJooqMapper()
    }
}
