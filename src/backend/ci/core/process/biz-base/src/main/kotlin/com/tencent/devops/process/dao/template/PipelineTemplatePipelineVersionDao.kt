package com.tencent.devops.process.dao.template

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.enums.PipelineInstanceTypeEnum
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.BuildNo
import com.tencent.devops.model.process.tables.TTemplatePipelineVersion
import com.tencent.devops.model.process.tables.records.TTemplatePipelineVersionRecord
import com.tencent.devops.common.pipeline.enums.TemplateRefType
import com.tencent.devops.process.pojo.template.v2.PTemplatePipelineVersion
import com.tencent.devops.process.pojo.template.v2.PTemplatePipelineVersionCommonCondition
import com.tencent.devops.process.pojo.template.v2.PTemplatePipelineVersionUpdateInfo
import org.jooq.Condition
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class PipelineTemplatePipelineVersionDao {

    /**
     * 创建一个新的模版流水线版本关联记录，如果已存在（根据主键判断），则更新它。
     */
    fun createOrUpdate(
        dslContext: DSLContext,
        record: PTemplatePipelineVersion
    ) {
        with(record) {
            with(TTemplatePipelineVersion.T_TEMPLATE_PIPELINE_VERSION) {
                val now = LocalDateTime.now()
                val buildNoJson = buildNo?.let { JsonUtil.toJson(it) }
                val paramsJson = params?.let { JsonUtil.toJson(it) }

                dslContext.insertInto(
                    this,
                    PROJECT_ID,
                    PIPELINE_ID,
                    PIPELINE_VERSION,
                    PIPELINE_VERSION_NAME,
                    INSTANCE_TYPE,
                    BUILD_NO,
                    PARAM,
                    TEMPLATE_REF_TYPE,
                    INPUT_TEMPLATE_ID,
                    INPUT_TEMPLATE_VERSION_NAME,
                    INPUT_TEMPLATE_FILE_PATH,
                    INPUT_TEMPLATE_REF,
                    TEMPLATE_ID,
                    TEMPLATE_VERSION,
                    TEMPLATE_VERSION_NAME,
                    CREATE_TIME,
                    UPDATE_TIME,
                    CREATOR,
                    UPDATER
                ).values(
                    record.projectId,
                    record.pipelineId,
                    record.pipelineVersion,
                    record.pipelineVersionName,
                    record.instanceType.name,
                    buildNoJson, // 使用序列化后的字符串
                    paramsJson,  // 使用序列化后的字符串
                    record.refType!!.name,
                    record.inputTemplateId,
                    record.inputTemplateVersionName,
                    record.inputTemplateFilePath,
                    record.inputTemplateRef,
                    record.templateId,
                    record.templateVersion,
                    record.templateVersionName,
                    now, // 创建时间
                    now, // 更新时间（在创建时与创建时间一致）
                    record.creator,
                    record.updater
                ).onDuplicateKeyUpdate()
                    // 3. 指定在发生键冲突时要更新的字段
                    .set(PIPELINE_VERSION_NAME, record.pipelineVersionName)
                    .set(INSTANCE_TYPE, record.instanceType.name)
                    .set(BUILD_NO, buildNoJson)
                    .set(PARAM, paramsJson)
                    .set(TEMPLATE_REF_TYPE, record.refType!!.name)
                    .set(INPUT_TEMPLATE_ID, record.inputTemplateId)
                    .set(INPUT_TEMPLATE_VERSION_NAME, record.inputTemplateVersionName)
                    .set(INPUT_TEMPLATE_FILE_PATH, record.inputTemplateFilePath)
                    .set(INPUT_TEMPLATE_REF, record.inputTemplateRef)
                    .set(TEMPLATE_ID, record.templateId)
                    .set(TEMPLATE_VERSION, record.templateVersion)
                    .set(TEMPLATE_VERSION_NAME, record.templateVersionName)
                    .set(UPDATER, record.updater) // 更新操作者
                    .set(UPDATE_TIME, now) // 更新时间戳
                    .execute()
            }
        }
    }

    /**
     * 根据条件查询单个记录
     */
    fun get(
        dslContext: DSLContext,
        condition: PTemplatePipelineVersionCommonCondition
    ): PTemplatePipelineVersion? {
        return with(TTemplatePipelineVersion.T_TEMPLATE_PIPELINE_VERSION) {
            dslContext.selectFrom(this)
                .where(buildQueryCondition(condition))
                .fetchOne()?.convert()
        }
    }

    /**
     * 获取指定流水线的最新版本记录
     * @param projectId 项目ID
     * @param pipelineId 流水线ID
     * @return PTemplatePipelineVersion? 最新版本记录或null
     */
    fun getLatestVersion(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String
    ): PTemplatePipelineVersion? {
        return with(TTemplatePipelineVersion.T_TEMPLATE_PIPELINE_VERSION) {
            dslContext.selectFrom(this)
                .where(
                    buildQueryCondition(
                        PTemplatePipelineVersionCommonCondition(
                            projectId = projectId,
                            pipelineId = pipelineId
                        )
                    )
                )
                .orderBy(PIPELINE_VERSION.desc())
                .limit(1)
                .fetchOne()?.convert()
        }
    }

    /**
     * 根据条件查询记录列表
     */
    fun list(
        dslContext: DSLContext,
        condition: PTemplatePipelineVersionCommonCondition
    ): List<PTemplatePipelineVersion> {
        return with(TTemplatePipelineVersion.T_TEMPLATE_PIPELINE_VERSION) {
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

    /**
     * 根据条件统计数量
     */
    fun count(
        dslContext: DSLContext,
        condition: PTemplatePipelineVersionCommonCondition
    ): Int {
        return with(TTemplatePipelineVersion.T_TEMPLATE_PIPELINE_VERSION) {
            dslContext.selectCount().from(this)
                .where(buildQueryCondition(condition))
                .fetchOne(0, Int::class.java)!!
        }
    }

    /**
     * 根据条件更新记录
     */
    fun update(
        dslContext: DSLContext,
        updateInfo: PTemplatePipelineVersionUpdateInfo,
        condition: PTemplatePipelineVersionCommonCondition
    ) {
        with(TTemplatePipelineVersion.T_TEMPLATE_PIPELINE_VERSION) {
            dslContext.update(this)
                .apply {
                    updateInfo.buildNo?.let { set(BUILD_NO, JsonUtil.toJson(it)) }
                    if (!updateInfo.params.isNullOrEmpty()) {
                        set(PARAM, JsonUtil.toJson(updateInfo.params!!))
                    }
                    updateInfo.updater?.let { set(UPDATER, it) }
                }
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(buildQueryCondition(condition))
                .execute()
        }
    }

    /**
     * 根据条件删除记录
     */
    fun delete(
        dslContext: DSLContext,
        condition: PTemplatePipelineVersionCommonCondition
    ) {
        with(TTemplatePipelineVersion.T_TEMPLATE_PIPELINE_VERSION) {
            dslContext.deleteFrom(this)
                .where(buildQueryCondition(condition))
                .execute()
        }
    }

    /**
     * 构建动态查询条件
     */
    private fun buildQueryCondition(condition: PTemplatePipelineVersionCommonCondition): List<Condition> {
        return with(TTemplatePipelineVersion.T_TEMPLATE_PIPELINE_VERSION) {
            with(condition) {
                val conditions = mutableListOf<Condition>()
                conditions.add(PROJECT_ID.eq(projectId))
                pipelineId?.let { conditions.add(PIPELINE_ID.eq(it)) }
                pipelineVersion?.let { conditions.add(PIPELINE_VERSION.eq(it)) }
                pipelineVersionName?.let { conditions.add(PIPELINE_VERSION_NAME.eq(it)) }
                instanceType?.let { conditions.add(INSTANCE_TYPE.eq(it.name)) }
                refType?.let { conditions.add(TEMPLATE_REF_TYPE.eq(it.name)) }
                inputTemplateId?.let { conditions.add(INPUT_TEMPLATE_ID.eq(it)) }
                inputTemplateVersionName?.let { conditions.add(INPUT_TEMPLATE_VERSION_NAME.eq(it)) }
                inputTemplateFilePath?.let { conditions.add(INPUT_TEMPLATE_FILE_PATH.eq(it)) }
                inputTemplateRef?.let { conditions.add(INPUT_TEMPLATE_REF.eq(it)) }
                templateId?.let { conditions.add(TEMPLATE_ID.eq(it)) }
                templateVersion?.let { conditions.add(TEMPLATE_VERSION.eq(it)) }
                templateVersionName?.let { conditions.add(TEMPLATE_VERSION_NAME.eq(it)) }
                creator?.let { conditions.add(CREATOR.eq(it)) }
                updater?.let { conditions.add(UPDATER.eq(it)) }
                return conditions
            }
        }
    }

    /**
     * 将 jOOQ Record 对象转换为 Pojo 对象
     */
    private fun TTemplatePipelineVersionRecord.convert(): PTemplatePipelineVersion {
        return PTemplatePipelineVersion(
            projectId = projectId,
            pipelineId = pipelineId,
            pipelineVersion = pipelineVersion,
            pipelineVersionName = pipelineVersionName,
            instanceType = PipelineInstanceTypeEnum.valueOf(instanceType),
            buildNo = buildNo?.takeIf { it.isNotBlank() }?.let {
                JsonUtil.to(it, object : TypeReference<BuildNo>() {})
            },
            params = param?.takeIf { it.isNotBlank() }?.let {
                JsonUtil.to(it, object : TypeReference<List<BuildFormProperty>>() {})
            },
            refType = TemplateRefType.valueOf(templateRefType),
            inputTemplateId = inputTemplateId,
            inputTemplateVersionName = inputTemplateVersionName,
            inputTemplateFilePath = inputTemplateFilePath,
            inputTemplateRef = inputTemplateRef,
            templateId = templateId,
            templateVersion = templateVersion,
            templateVersionName = templateVersionName,
            creator = creator,
            updater = updater
        )
    }
}
