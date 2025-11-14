package com.tencent.devops.process.dao.template

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.pipeline.enums.PipelineInstanceTypeEnum
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.BuildNo
import com.tencent.devops.model.process.tables.TPipelineInfo
import com.tencent.devops.model.process.tables.TPipelineTemplateInfo
import com.tencent.devops.model.process.tables.TTemplatePipeline
import com.tencent.devops.model.process.tables.records.TTemplatePipelineRecord
import com.tencent.devops.process.pojo.template.TemplatePipelineStatus
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateRelated
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateRelatedCommonCondition
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateRelatedSimple
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateRelatedUpdateInfo
import org.jooq.Condition
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class PipelineTemplateRelatedDao {
    fun create(
        dslContext: DSLContext,
        record: PipelineTemplateRelated
    ) {
        with(record) {
            with(TTemplatePipeline.T_TEMPLATE_PIPELINE) {
                val now = LocalDateTime.now()
                dslContext.insertInto(
                    this,
                    PROJECT_ID,
                    PIPELINE_ID,
                    INSTANCE_TYPE,
                    ROOT_TEMPLATE_ID,
                    VERSION,
                    VERSION_NAME,
                    TEMPLATE_ID,
                    CREATOR,
                    UPDATOR,
                    CREATED_TIME,
                    UPDATED_TIME,
                    BUILD_NO,
                    PARAM
                ).values(
                    projectId,
                    pipelineId,
                    instanceType.type,
                    rootTemplateId,
                    version,
                    versionName,
                    templateId,
                    creator,
                    updater,
                    now,
                    now,
                    buildNo?.let { JsonUtil.toJson(it) },
                    params?.let { JsonUtil.toJson(it) }
                ).execute()
            }
        }
    }

    fun list(
        dslContext: DSLContext,
        condition: PipelineTemplateRelatedCommonCondition
    ): List<PipelineTemplateRelated> {
        return with(TTemplatePipeline.T_TEMPLATE_PIPELINE) {
            dslContext.selectFrom(this)
                .where(buildQueryCondition(condition))
                .let {
                    if (condition.limit != null && condition.offset != null) {
                        it.offset(condition.offset).limit(condition.limit)
                    } else {
                        it
                    }
                }
                .fetch().map { it.convert() }
        }
    }

    fun listSimple(
        dslContext: DSLContext,
        projectId: String,
        templateId: String,
        pipelineName: String?,
        updater: String?,
        templateVersion: Long?,
        status: TemplatePipelineStatus?,
        pipelineIds: List<String>?,
        instanceTypeEnum: PipelineInstanceTypeEnum,
        limit: Int,
        offset: Int
    ): List<PipelineTemplateRelatedSimple> {
        val templatePipelineTable = TTemplatePipeline.T_TEMPLATE_PIPELINE
        val pipelineInfoTable = TPipelineInfo.T_PIPELINE_INFO
        val pipelineTemplateInfoTable = TPipelineTemplateInfo.T_PIPELINE_TEMPLATE_INFO
        return dslContext.select(
            templatePipelineTable.PROJECT_ID,
            templatePipelineTable.TEMPLATE_ID,
            templatePipelineTable.VERSION,
            templatePipelineTable.VERSION_NAME,
            templatePipelineTable.PIPELINE_ID,
            pipelineInfoTable.PIPELINE_NAME,
            templatePipelineTable.INSTANCE_TYPE,
            templatePipelineTable.INSTANCE_ERROR_INFO,
            templatePipelineTable.CREATOR,
            templatePipelineTable.UPDATOR,
            templatePipelineTable.CREATED_TIME,
            templatePipelineTable.UPDATED_TIME,
            pipelineInfoTable.VERSION,
            templatePipelineTable.STATUS,
            templatePipelineTable.PULL_REQUEST_URL,
            pipelineTemplateInfoTable.RELEASED_VERSION
        )
            .from(templatePipelineTable)
            .join(pipelineInfoTable)
            .on(templatePipelineTable.PIPELINE_ID.eq(pipelineInfoTable.PIPELINE_ID))
            .leftJoin(pipelineTemplateInfoTable)
            .on(templatePipelineTable.TEMPLATE_ID.eq(pipelineTemplateInfoTable.ID))
            .where(
                buildCommonConditions(
                    templatePipelineTable = templatePipelineTable,
                    pipelineInfoTable = pipelineInfoTable,
                    pipelineTemplateInfoTable = pipelineTemplateInfoTable,
                    projectId = projectId,
                    templateId = templateId,
                    pipelineName = pipelineName,
                    updater = updater,
                    templateVersion = templateVersion,
                    status = status,
                    pipelineIds = pipelineIds,
                    instanceTypeEnum = instanceTypeEnum
                )
            )
            .orderBy(templatePipelineTable.UPDATED_TIME.desc())
            .limit(limit)
            .offset(offset)
            .fetch().map {
                PipelineTemplateRelatedSimple(
                    projectId = it.value1(),
                    templateId = it.value2(),
                    version = it.value3(),
                    versionName = it.value4(),
                    pipelineId = it.value5(),
                    pipelineName = it.value6(),
                    instanceType = PipelineInstanceTypeEnum.get(it.value7()),
                    instanceErrorInfo = it.value8(),
                    creator = it.value9(),
                    updater = it.value10(),
                    createdTime = it.value11().timestampmilli(),
                    updatedTime = it.value12().timestampmilli(),
                    pipelineVersion = it.value13(),
                    status = it.value14()?.let { status -> TemplatePipelineStatus.valueOf(status) },
                    pullRequestUrl = it.value15(),
                    releasedVersion = it.value16()
                )
            }
    }

    private fun buildCommonConditions(
        templatePipelineTable: TTemplatePipeline,
        pipelineInfoTable: TPipelineInfo,
        pipelineTemplateInfoTable: TPipelineTemplateInfo,
        projectId: String,
        templateId: String,
        pipelineName: String?,
        updater: String?,
        templateVersion: Long?,
        status: TemplatePipelineStatus?,
        pipelineIds: List<String>?,
        instanceTypeEnum: PipelineInstanceTypeEnum
    ): Condition {
        var conditions = templatePipelineTable.PROJECT_ID.eq(projectId)
            .and(templatePipelineTable.TEMPLATE_ID.eq(templateId))
            .and(templatePipelineTable.DELETED.eq(false))
            .and(templatePipelineTable.INSTANCE_TYPE.eq(instanceTypeEnum.type))
            .and(pipelineInfoTable.PROJECT_ID.eq(projectId))
            .and(pipelineInfoTable.DELETE.eq(false))

        conditions = conditions.let {
            if (!pipelineName.isNullOrBlank()) {
                it.and(pipelineInfoTable.PIPELINE_NAME.like("%$pipelineName%"))
            } else {
                it
            }
        }
        conditions = conditions.let {
            if (!updater.isNullOrBlank()) {
                it.and(templatePipelineTable.UPDATOR.like("%$updater%"))
            } else {
                it
            }
        }
        conditions = conditions.let {
            if (templateVersion != null) {
                it.and(templatePipelineTable.VERSION.eq(templateVersion))
            } else {
                it
            }
        }
        conditions = conditions.let {
            if (status != null) {
                if (status == TemplatePipelineStatus.PENDING_UPDATE) {
                    it.and(templatePipelineTable.STATUS.eq(TemplatePipelineStatus.UPDATED.name))
                        .and(templatePipelineTable.VERSION.ne(pipelineTemplateInfoTable.RELEASED_VERSION))
                } else {
                    it.and(templatePipelineTable.STATUS.eq(status.name))
                }
            } else {
                it
            }
        }
        conditions = conditions.let {
            if (!pipelineIds.isNullOrEmpty()) {
                it.and(templatePipelineTable.PIPELINE_ID.`in`(pipelineIds))
            } else {
                it
            }
        }
        return conditions
    }

    fun countSimple(
        dslContext: DSLContext,
        projectId: String,
        templateId: String,
        pipelineName: String?,
        updater: String?,
        templateVersion: Long?,
        status: TemplatePipelineStatus?,
        pipelineIds: List<String>?,
        instanceTypeEnum: PipelineInstanceTypeEnum
    ): Int {
        val templatePipelineTable = TTemplatePipeline.T_TEMPLATE_PIPELINE
        val pipelineInfoTable = TPipelineInfo.T_PIPELINE_INFO
        val pipelineTemplateInfoTable = TPipelineTemplateInfo.T_PIPELINE_TEMPLATE_INFO
        return dslContext.selectCount()
            .from(templatePipelineTable)
            .join(pipelineInfoTable)
            .on(templatePipelineTable.PIPELINE_ID.eq(pipelineInfoTable.PIPELINE_ID))
            .leftJoin(pipelineTemplateInfoTable)
            .on(templatePipelineTable.TEMPLATE_ID.eq(pipelineTemplateInfoTable.ID))
            .where(
                buildCommonConditions(
                    templatePipelineTable = templatePipelineTable,
                    pipelineInfoTable = pipelineInfoTable,
                    pipelineTemplateInfoTable = pipelineTemplateInfoTable,
                    projectId = projectId,
                    templateId = templateId,
                    pipelineName = pipelineName,
                    updater = updater,
                    templateVersion = templateVersion,
                    status = status,
                    pipelineIds = pipelineIds,
                    instanceTypeEnum = instanceTypeEnum
                )
            )
            .fetchOne(0, Int::class.java)!!
    }

    fun delete(
        dslContext: DSLContext,
        condition: PipelineTemplateRelatedCommonCondition
    ) {
        return with(TTemplatePipeline.T_TEMPLATE_PIPELINE) {
            dslContext.deleteFrom(this)
                .where(buildQueryCondition(condition))
                .execute()
        }
    }

    fun get(
        dslContext: DSLContext,
        condition: PipelineTemplateRelatedCommonCondition
    ): PipelineTemplateRelated? {
        return with(TTemplatePipeline.T_TEMPLATE_PIPELINE) {
            dslContext.selectFrom(this)
                .where(buildQueryCondition(condition))
                .fetchOne()?.convert()
        }
    }

    fun count(
        dslContext: DSLContext,
        condition: PipelineTemplateRelatedCommonCondition
    ): Int {
        return with(TTemplatePipeline.T_TEMPLATE_PIPELINE) {
            dslContext.selectCount().from(this)
                .where(buildQueryCondition(condition))
                .fetchOne(0, Int::class.java)!!
        }
    }

    fun buildQueryCondition(condition: PipelineTemplateRelatedCommonCondition): List<Condition> {
        return with(TTemplatePipeline.T_TEMPLATE_PIPELINE) {
            with(condition) {
                val conditions = mutableListOf<Condition>()
                conditions.add(PROJECT_ID.eq(projectId))
                if (templateId != null) conditions.add(TEMPLATE_ID.eq(templateId))
                if (pipelineId != null) conditions.add(PIPELINE_ID.eq(pipelineId))
                if (version != null) conditions.add(VERSION.eq(version))
                if (versionName != null) conditions.add(VERSION_NAME.eq(versionName))
                if (instanceType != null) conditions.add(INSTANCE_TYPE.eq(instanceType!!.type))
                if (rootTemplateId != null) conditions.add(ROOT_TEMPLATE_ID.eq(rootTemplateId))
                if (deleted != null) conditions.add(DELETED.eq(deleted))
                if (creator != null) conditions.add(CREATOR.eq(creator))
                if (!updater.isNullOrBlank()) conditions.add(UPDATOR.like("%$updater%"))
                if (!pipelineIds.isNullOrEmpty()) {
                    conditions.add(PIPELINE_ID.`in`(pipelineIds))
                }
                if (pullRequestId != null) {
                    conditions.add(PULL_REQUEST_ID.eq(pullRequestId))
                }
                conditions
            }
        }
    }

    fun update(
        dslContext: DSLContext,
        updateInfo: PipelineTemplateRelatedUpdateInfo,
        condition: PipelineTemplateRelatedCommonCondition
    ) {
        with(TTemplatePipeline.T_TEMPLATE_PIPELINE) {
            dslContext.update(this)
                .apply {
                    if (!updateInfo.params.isNullOrEmpty()) set(PARAM, JsonUtil.toJson(updateInfo.params!!))
                    updateInfo.buildNo?.let { set(BUILD_NO, JsonUtil.toJson(it)) }
                    updateInfo.deleted?.let { set(DELETED, it) }
                    updateInfo.instanceErrorInfo?.let { set(INSTANCE_ERROR_INFO, it) }
                    updateInfo.updater?.let { set(UPDATOR, it) }
                    updateInfo.status?.let { set(STATUS, it.name) }
                    updateInfo.pullRequestUrl?.let { set(PULL_REQUEST_URL, it) }
                    updateInfo.pullRequestId?.let { set(PULL_REQUEST_ID, it) }
                }
                .set(UPDATED_TIME, LocalDateTime.now())
                .where(buildQueryCondition(condition))
                .execute()
        }
    }

    fun TTemplatePipelineRecord.convert(): PipelineTemplateRelated {
        return PipelineTemplateRelated(
            projectId = projectId,
            templateId = templateId,
            pipelineId = pipelineId,
            version = version,
            versionName = versionName,
            buildNo = buildNo?.takeIf { it.isNotEmpty() }?.let {
                JsonUtil.to(it, object : TypeReference<BuildNo>() {})
            },
            params = param?.takeIf { it.isNotEmpty() }?.let {
                JsonUtil.to(it, object : TypeReference<List<BuildFormProperty>>() {})
            },
            instanceType = PipelineInstanceTypeEnum.get(instanceType),
            rootTemplateId = rootTemplateId,
            deleted = deleted,
            instanceErrorInfo = instanceErrorInfo,
            createdTime = createdTime.timestampmilli(),
            updatedTime = updatedTime.timestampmilli(),
            creator = creator,
            updater = updator
        )
    }
}
