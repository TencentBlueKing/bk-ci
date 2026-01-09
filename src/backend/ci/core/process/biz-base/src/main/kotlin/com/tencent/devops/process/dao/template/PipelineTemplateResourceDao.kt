package com.tencent.devops.process.dao.template

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.api.util.toLocalDateTime
import com.tencent.devops.common.api.util.toLocalDateTimeOrDefault
import com.tencent.devops.common.pipeline.enums.BranchVersionAction
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.template.ITemplateModel
import com.tencent.devops.common.pipeline.template.PipelineTemplateType
import com.tencent.devops.model.process.tables.TPipelineTemplateResourceVersion
import com.tencent.devops.model.process.tables.records.TPipelineTemplateResourceVersionRecord
import com.tencent.devops.process.pojo.PipelineTemplateVersionSimple
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateResource
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateResourceCommonCondition
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateResourceUpdateInfo
import com.tencent.devops.store.pojo.template.enums.TemplateStatusEnum
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class PipelineTemplateResourceDao {
    fun create(
        dslContext: DSLContext,
        record: PipelineTemplateResource
    ) {
        with(TPipelineTemplateResourceVersion.T_PIPELINE_TEMPLATE_RESOURCE_VERSION) {
            val params = record.params?.let { JsonUtil.toJson(it, false) }
            val model = record.model.let { JsonUtil.toJson(it, false) }
            dslContext.insertInto(
                this,
                PROJECT_ID,
                TEMPLATE_ID,
                TYPE,
                STORE_STATUS,
                SETTING_VERSION,
                VERSION,
                NUMBER,
                VERSION_NAME,
                VERSION_NUM,
                SETTING_VERSION_NUM,
                SRC_TEMPLATE_PROJECT_ID,
                SRC_TEMPLATE_ID,
                SRC_TEMPLATE_VERSION,
                PIPELINE_VERSION,
                TRIGGER_VERSION,
                BASE_VERSION,
                BASE_VERSION_NAME,
                PARAMS,
                MODEL,
                YAML,
                STATUS,
                BRANCH_ACTION,
                DESCRIPTION,
                SORT_WEIGHT,
                CREATOR,
                UPDATER,
                RELEASE_TIME,
                CREATED_TIME,
                UPDATE_TIME
            ).values(
                record.projectId,
                record.templateId,
                record.type.name,
                record.storeStatus.name,
                record.settingVersion,
                record.version,
                record.number,
                record.versionName,
                record.versionNum,
                record.settingVersionNum,
                record.srcTemplateProjectId,
                record.srcTemplateId,
                record.srcTemplateVersion,
                record.pipelineVersion,
                record.triggerVersion,
                record.baseVersion,
                record.baseVersionName,
                params,
                model,
                record.yaml,
                record.status.name,
                record.branchAction?.name,
                record.description,
                record.sortWeight,
                record.creator,
                record.updater,
                record.releaseTime.toLocalDateTimeOrDefault(),
                record.createdTime.toLocalDateTimeOrDefault(),
                record.updateTime.toLocalDateTimeOrDefault()
            ).onDuplicateKeyUpdate()
                .set(STORE_STATUS, record.storeStatus.name)
                .set(SETTING_VERSION, record.settingVersion)
                .set(VERSION, record.version)
                .set(NUMBER, record.number)
                .set(VERSION_NAME, record.versionName)
                .set(VERSION_NUM, record.versionNum)
                .set(SETTING_VERSION_NUM, record.settingVersionNum)
                .set(PIPELINE_VERSION, record.pipelineVersion)
                .set(TRIGGER_VERSION, record.triggerVersion)
                .set(PIPELINE_VERSION, record.pipelineVersion)
                .set(PARAMS, params)
                .set(MODEL, model)
                .set(YAML, record.yaml)
                .set(STATUS, record.status.name)
                .set(BRANCH_ACTION, record.branchAction?.name)
                .set(DESCRIPTION, record.description)
                .set(SORT_WEIGHT, record.sortWeight)
                .set(UPDATER, record.updater)
                .set(UPDATE_TIME, record.updateTime.toLocalDateTimeOrDefault())
                .set(RELEASE_TIME, record.releaseTime?.toLocalDateTime())
                .execute()
        }
    }

    fun update(
        dslContext: DSLContext,
        record: PipelineTemplateResourceUpdateInfo,
        commonCondition: PipelineTemplateResourceCommonCondition
    ): Int {
        return with(TPipelineTemplateResourceVersion.T_PIPELINE_TEMPLATE_RESOURCE_VERSION) {
            val now = LocalDateTime.now()
            dslContext.update(this)
                .apply {
                    record.version?.let { set(VERSION, it) }
                    record.settingVersionNum?.let { set(SETTING_VERSION_NUM, it) }
                    record.versionName?.let { set(VERSION_NAME, it) }
                    record.versionNum?.let { set(VERSION_NUM, it) }
                    record.pipelineVersion?.let { set(PIPELINE_VERSION, it) }
                    record.triggerVersion?.let { set(TRIGGER_VERSION, it) }
                    record.baseVersion?.let { set(BASE_VERSION, it) }
                    record.baseVersionName?.let { set(BASE_VERSION_NAME, it) }
                    record.params?.let { set(PARAMS, JsonUtil.toJson(it, false)) }
                    record.model?.let { set(MODEL, JsonUtil.toJson(it, false)) }
                    record.yaml?.let { set(YAML, it) }
                    record.status?.let { set(STATUS, it.name) }
                    record.branchAction?.let { set(BRANCH_ACTION, it.name) }
                    record.description?.let { set(DESCRIPTION, it) }
                    record.releaseTime?.let { set(RELEASE_TIME, it) }
                    record.updater?.let { set(UPDATER, it) }
                    record.sortWeight?.let { set(SORT_WEIGHT, it) }
                    record.storeStatus?.let { set(STORE_STATUS, it.name) }
                }
                .set(UPDATE_TIME, now)
                .where(buildQueryCondition(commonCondition))
                .execute()
        }
    }

    fun transformTemplateToCustom(
        dslContext: DSLContext,
        projectId: String,
        templateId: String
    ) {
        with(TPipelineTemplateResourceVersion.T_PIPELINE_TEMPLATE_RESOURCE_VERSION) {
            dslContext.update(this)
                .set(SRC_TEMPLATE_VERSION, DSL.inline(null as Long?))
                .set(SRC_TEMPLATE_PROJECT_ID, DSL.inline(null as String?))
                .set(SRC_TEMPLATE_ID, DSL.inline(null as String?))
                .where(PROJECT_ID.eq(projectId))
                .and(TEMPLATE_ID.eq(templateId))
                .execute()
        }
    }

    fun list(
        dslContext: DSLContext,
        commonCondition: PipelineTemplateResourceCommonCondition
    ): List<PipelineTemplateResource> {
        return with(TPipelineTemplateResourceVersion.T_PIPELINE_TEMPLATE_RESOURCE_VERSION) {
            dslContext.selectFrom(this)
                .where(buildQueryCondition(commonCondition))
                .let {
                    if (commonCondition.page != null && commonCondition.pageSize != null) {
                        it.offset((commonCondition.page!! - 1) * commonCondition.pageSize!!)
                            .limit(commonCondition.pageSize)
                    } else {
                        it
                    }
                }
                .fetch().map { it.convert() }
        }
    }

    fun count(
        dslContext: DSLContext,
        commonCondition: PipelineTemplateResourceCommonCondition
    ): Int {
        return with(TPipelineTemplateResourceVersion.T_PIPELINE_TEMPLATE_RESOURCE_VERSION) {
            dslContext.selectCount().from(this)
                .where(buildQueryCondition(commonCondition))
                .fetchOne(0, Int::class.java)!!
        }
    }

    /**
     * 统计模板版本数量
     */
    fun countVersions(
        dslContext: DSLContext,
        projectId: String,
        templateId: String
    ): Int {
        return with(TPipelineTemplateResourceVersion.T_PIPELINE_TEMPLATE_RESOURCE_VERSION) {
            dslContext.selectCount().from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(TEMPLATE_ID.eq(templateId))
                .fetchOne(0, Int::class.java)!!
        }
    }

    fun get(
        dslContext: DSLContext,
        commonCondition: PipelineTemplateResourceCommonCondition
    ): PipelineTemplateResource? {
        return with(TPipelineTemplateResourceVersion.T_PIPELINE_TEMPLATE_RESOURCE_VERSION) {
            dslContext.selectFrom(this)
                .where(buildQueryCondition(commonCondition))
                .fetchOne()?.convert()
        }
    }

    fun get(
        dslContext: DSLContext,
        projectId: String,
        templateId: String,
        version: Long
    ): PipelineTemplateResource? {
        return with(TPipelineTemplateResourceVersion.T_PIPELINE_TEMPLATE_RESOURCE_VERSION) {
            dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(TEMPLATE_ID.eq(templateId))
                .and(VERSION.eq(version))
                .fetchOne()?.convert()
        }
    }

    fun getVersions(
        dslContext: DSLContext,
        commonCondition: PipelineTemplateResourceCommonCondition
    ): List<PipelineTemplateVersionSimple> {
        return with(TPipelineTemplateResourceVersion.T_PIPELINE_TEMPLATE_RESOURCE_VERSION) {
            dslContext.select(
                TEMPLATE_ID,
                SETTING_VERSION,
                VERSION,
                VERSION_NAME,
                VERSION_NUM,
                PIPELINE_VERSION,
                TRIGGER_VERSION,
                BASE_VERSION,
                BASE_VERSION_NAME,
                SRC_TEMPLATE_PROJECT_ID,
                SRC_TEMPLATE_ID,
                SRC_TEMPLATE_VERSION,
                STATUS,
                DESCRIPTION,
                CREATOR,
                UPDATER,
                CREATED_TIME,
                UPDATE_TIME,
                STORE_STATUS,
                NUMBER
            ).from(this)
                .where(buildQueryCondition(commonCondition))
                .orderBy(SORT_WEIGHT.desc(), RELEASE_TIME.desc(), NUMBER.desc())
                .fetch()
                .map {
                    PipelineTemplateVersionSimple(
                        pipelineId = it.value1(),
                        settingVersion = it.value2(),
                        version = it.value3().toInt(),
                        versionName = it.value4() ?: "",
                        versionNum = it.value5(),
                        pipelineVersion = it.value6(),
                        triggerVersion = it.value7(),
                        baseVersion = it.value8()?.toInt(),
                        baseVersionName = it.value9(),
                        srcTemplateProjectId = it.value10(),
                        srcTemplateId = it.value11(),
                        srcTemplateVersion = it.value12()?.toInt(),
                        status = VersionStatus.get(it.value13()),
                        description = it.value14(),
                        creator = it.value15(),
                        updater = it.value16(),
                        createTime = it.value17().timestampmilli(),
                        updateTime = it.value18().timestampmilli(),
                        storeFlag = it.value19() == TemplateStatusEnum.RELEASED.name,
                        number = it.value20()
                    )
                }
        }
    }

    fun getLatestRecord(
        dslContext: DSLContext,
        projectId: String? = null,
        templateId: String,
        status: VersionStatus? = null,
        storeStatus: TemplateStatusEnum? = null,
        storeStatusList: List<TemplateStatusEnum>? = null,
        version: Long? = null,
        versionName: String? = null,
        includeDelete: Boolean = false
    ): PipelineTemplateResource? {
        with(TPipelineTemplateResourceVersion.T_PIPELINE_TEMPLATE_RESOURCE_VERSION) {
            val conditions = mutableListOf<Condition>()
            if (projectId != null) {
                conditions.add(PROJECT_ID.eq(projectId))
            }
            conditions.add(TEMPLATE_ID.eq(templateId))
            if (!includeDelete) {
                conditions.add(STATUS.ne(VersionStatus.DELETE.name))
            }
            if (status != null) {
                conditions.add(STATUS.eq(status.name))
                // 获取活跃的分支版本
                if (status == VersionStatus.BRANCH) {
                    conditions.add(BRANCH_ACTION.eq(BranchVersionAction.ACTIVE.name))
                }
            }
            if (version != null) {
                conditions.add(VERSION.eq(version))
            }
            if (versionName != null) {
                conditions.add(VERSION_NAME.eq(versionName))
            }
            if (storeStatus != null) {
                conditions.add(STORE_STATUS.eq(storeStatus.name))
            }
            if (!storeStatusList.isNullOrEmpty()) {
                conditions.add(STORE_STATUS.`in`(storeStatusList.map { it.name }))
            }
            val query = dslContext.selectFrom(this).where(conditions)
            // 正式版本,应该按照versionNum排序,
            // 不然如果版本顺序是: 正式(v1)->草稿->正式(v2),
            // 如果把草稿发布,那么草稿应是v3,如果再创建一个正式版本,如果按照仅按照version排序,正式版本的versionNum还是v3
            if (status == VersionStatus.RELEASED) {
                query.orderBy(VERSION_NUM.desc(), VERSION.desc())
            } else {
                query.orderBy(VERSION.desc())
            }
            return query.limit(1).fetchOne()?.convert()
        }
    }

    fun listLatestReleasedVersions(
        dslContext: DSLContext,
        templateIds: List<String>
    ): List<PipelineTemplateVersionSimple> {
        return with(TPipelineTemplateResourceVersion.T_PIPELINE_TEMPLATE_RESOURCE_VERSION) {
            // 子查询获取每个模板的最大NUMBER
            val maxNumbers = dslContext.select(TEMPLATE_ID, DSL.max(NUMBER).`as`("max_number"))
                .from(this)
                .where(TEMPLATE_ID.`in`(templateIds))
                .and(STATUS.eq(VersionStatus.RELEASED.name))
                .groupBy(TEMPLATE_ID)
                .asTable("m")
            // 主查询关联获取VERSION
            dslContext.select(
                TEMPLATE_ID,
                SETTING_VERSION,
                VERSION,
                VERSION_NAME,
                VERSION_NUM,
                PIPELINE_VERSION,
                TRIGGER_VERSION,
                BASE_VERSION,
                BASE_VERSION_NAME,
                SRC_TEMPLATE_VERSION,
                STATUS,
                DESCRIPTION,
                CREATOR,
                UPDATER,
                CREATED_TIME,
                UPDATE_TIME,
                NUMBER
            )
                .from(this)
                .join(maxNumbers)
                .on(
                    TEMPLATE_ID.eq(maxNumbers.field(TEMPLATE_ID)),
                    NUMBER.eq(maxNumbers.field("max_number", Int::class.java))
                )
                .fetch().map {
                    PipelineTemplateVersionSimple(
                        pipelineId = it.value1(),
                        settingVersion = it.value2(),
                        version = it.value3().toInt(),
                        versionName = it.value4() ?: "",
                        versionNum = it.value5(),
                        pipelineVersion = it.value6(),
                        triggerVersion = it.value7(),
                        baseVersion = it.value8()?.toInt(),
                        baseVersionName = it.value9(),
                        srcTemplateVersion = it.value10()?.toInt(),
                        status = VersionStatus.get(it.value11()),
                        description = it.value12(),
                        creator = it.value13(),
                        updater = it.value14(),
                        createTime = it.value15().timestampmilli(),
                        updateTime = it.value16().timestampmilli(),
                        number = it.value17()
                    )
                }
        }
    }

    fun delete(
        dslContext: DSLContext,
        commonCondition: PipelineTemplateResourceCommonCondition
    ) {
        with(TPipelineTemplateResourceVersion.T_PIPELINE_TEMPLATE_RESOURCE_VERSION) {
            dslContext.deleteFrom(this)
                .where(buildQueryCondition(commonCondition))
                .execute()
        }
    }

    @Suppress("NestedBlockDepth")
    fun buildQueryCondition(commonCondition: PipelineTemplateResourceCommonCondition): MutableList<Condition> {
        with(TPipelineTemplateResourceVersion.T_PIPELINE_TEMPLATE_RESOURCE_VERSION) {
            with(commonCondition) {
                val conditions = mutableListOf<Condition>()
                conditions.add(PROJECT_ID.eq(projectId))
                if (!includeDeleted) {
                    conditions.add(STATUS.ne(VersionStatus.DELETE.name))
                }
                conditions.add((BRANCH_ACTION.ne(BranchVersionAction.INACTIVE.name)).or(BRANCH_ACTION.isNull))
                // 优先处理批量查询条件
                if (!templateVersionPairs.isNullOrEmpty()) {
                    val pairConditions = templateVersionPairs!!.map { pair ->
                        DSL.row(TEMPLATE_ID, VERSION).eq(DSL.row(pair.templateId, pair.version.toLong()))
                    }
                    conditions.add(DSL.or(pairConditions))
                } else {
                    // 如果没有批量查询条件，则处理单个 templateId 和 version 条件
                    if (templateId != null) conditions.add(TEMPLATE_ID.eq(templateId))
                    if (version != null) conditions.add(VERSION.eq(version))
                    if (!versions.isNullOrEmpty()) conditions.add(VERSION.`in`(versions))
                }
                if (type != null) conditions.add(TYPE.eq(type!!.value))
                if (settingVersion != null) conditions.add(SETTING_VERSION.eq(settingVersion))
                if (number != null) conditions.add(NUMBER.eq(number))
                if (versionName != null && versionName!!.isNotBlank()) conditions.add(VERSION_NAME.eq(versionName))
                if (!fuzzyVersionName.isNullOrBlank()) conditions.add(VERSION_NAME.like("%$fuzzyVersionName%"))
                if (settingVersionNum != null) conditions.add(SETTING_VERSION_NUM.eq(settingVersionNum))
                if (versionNum != null) conditions.add(VERSION_NUM.eq(versionNum))
                if (pipelineVersion != null) conditions.add(PIPELINE_VERSION.eq(pipelineVersion))
                if (triggerVersion != null) conditions.add(TRIGGER_VERSION.eq(triggerVersion))
                if (baseVersion != null) conditions.add(BASE_VERSION.eq(baseVersion))
                if (status != null) conditions.add(STATUS.eq(status!!.name))
                if (branchAction != null) conditions.add(BRANCH_ACTION.eq(branchAction!!.name))
                if (creator != null) conditions.add(CREATOR.eq(creator))
                if (updater != null) conditions.add(UPDATER.eq(updater))
                if (releaseTime != null) conditions.add(RELEASE_TIME.eq(releaseTime))
                if (srcTemplateProjectId != null) conditions.add(SRC_TEMPLATE_PROJECT_ID.eq(srcTemplateProjectId))
                if (srcTemplateId != null) conditions.add(SRC_TEMPLATE_ID.eq(srcTemplateId))
                if (srcTemplateVersion != null) conditions.add(SRC_TEMPLATE_VERSION.eq(srcTemplateVersion))
                if (description != null) conditions.add(DESCRIPTION.like("%$description%"))
                if (includeDraft == false) conditions.add(STATUS.notEqual(VersionStatus.COMMITTING.name))
                if (ltNumber != null) conditions.add(NUMBER.lt(ltNumber))
                if (geNumber != null) conditions.add(NUMBER.ge(geNumber))
                if (gtNumber != null) conditions.add(NUMBER.gt(gtNumber))
                if (!srcTemplateVersions.isNullOrEmpty()) conditions.add(SRC_TEMPLATE_VERSION.`in`(srcTemplateVersions))
                if (storeFlag == true) {
                    conditions.add(STORE_STATUS.eq(TemplateStatusEnum.RELEASED.name))
                }
                if (storeFlag == false) {
                    conditions.add(STORE_STATUS.ne(TemplateStatusEnum.RELEASED.name))
                }
                if (storeStatus != null) {
                    conditions.add(STORE_STATUS.eq(storeStatus!!.name))
                }
                return conditions
            }
        }
    }

    fun TPipelineTemplateResourceVersionRecord.convert(): PipelineTemplateResource {
        return PipelineTemplateResource(
            projectId = this.projectId,
            templateId = this.templateId,
            type = PipelineTemplateType.valueOf(this.type),
            settingVersion = this.settingVersion,
            version = this.version,
            number = this.number,
            versionName = this.versionName,
            versionNum = this.versionNum,
            settingVersionNum = this.settingVersionNum,
            pipelineVersion = this.pipelineVersion,
            triggerVersion = this.triggerVersion,
            srcTemplateProjectId = this.srcTemplateProjectId,
            srcTemplateId = this.srcTemplateId,
            srcTemplateVersion = this.srcTemplateVersion,
            baseVersion = this.baseVersion,
            baseVersionName = this.baseVersionName,
            params = this.params?.let { JsonUtil.to(it, object : TypeReference<List<BuildFormProperty>>() {}) },
            model = this.model.let { JsonUtil.to(this.model, ITemplateModel::class.java) },
            yaml = this.yaml,
            status = VersionStatus.get(this.status),
            branchAction = this.branchAction?.let { BranchVersionAction.get(it) },
            description = this.description,
            creator = this.creator,
            updater = this.updater,
            releaseTime = this.releaseTime?.timestampmilli(),
            sortWeight = this.sortWeight,
            storeStatus = TemplateStatusEnum.valueOf(this.storeStatus)
        )
    }
}
