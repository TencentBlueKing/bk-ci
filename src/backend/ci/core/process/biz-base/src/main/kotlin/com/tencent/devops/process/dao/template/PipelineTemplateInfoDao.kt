package com.tencent.devops.process.dao.template

import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.api.util.toLocalDateTimeOrDefault
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.common.pipeline.template.PipelineTemplateType
import com.tencent.devops.common.pipeline.template.UpgradeStrategyEnum
import com.tencent.devops.model.process.tables.TPipelineTemplateInfo
import com.tencent.devops.model.process.tables.records.TPipelineTemplateInfoRecord
import com.tencent.devops.process.pojo.template.TemplateType
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateCommonCondition
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateInfoUpdateInfo
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateInfoV2
import com.tencent.devops.store.pojo.template.enums.TemplateStatusEnum
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class PipelineTemplateInfoDao {
    fun create(
        dslContext: DSLContext,
        record: PipelineTemplateInfoV2
    ) {
        with(TPipelineTemplateInfo.T_PIPELINE_TEMPLATE_INFO) {
            dslContext.insertInto(
                this,
                ID,
                PROJECT_ID,
                NAME,
                DESC,
                MODE,
                PUBLISH_STRATEGY,
                UPGRADE_STRATEGY,
                SETTING_SYNC_STRATEGY,
                CATEGORY,
                TYPE,
                LOGO_URL,
                PAC,
                RELEASED_VERSION,
                RELEASED_VERSION_NAME,
                RELEASED_SETTING_VERSION,
                LATEST_VERSION_STATUS,
                STORE_STATUS,
                SRC_TEMPLATE_ID,
                SRC_TEMPLATE_PROJECT_ID,
                DEBUG_PIPELINE_COUNT,
                INSTANCE_PIPELINE_COUNT,
                CREATOR,
                UPDATER,
                CREATED_TIME,
                UPDATE_TIME
            ).values(
                record.id,
                record.projectId,
                record.name,
                record.desc,
                record.mode.name,
                record.publishStrategy?.name,
                record.upgradeStrategy?.name,
                record.settingSyncStrategy?.name,
                record.category,
                record.type.name,
                record.logoUrl,
                record.enablePac,
                record.releasedVersion,
                record.releasedVersionName,
                record.releasedSettingVersion,
                record.latestVersionStatus.name,
                record.storeStatus.name,
                record.srcTemplateId,
                record.srcTemplateProjectId,
                record.debugPipelineCount,
                record.instancePipelineCount,
                record.creator,
                record.updater,
                record.createdTime.toLocalDateTimeOrDefault(),
                record.updateTime.toLocalDateTimeOrDefault()
            ).onDuplicateKeyUpdate()
                .set(NAME, record.name)
                .set(DESC, record.desc)
                .set(PUBLISH_STRATEGY, record.publishStrategy?.name)
                .set(UPGRADE_STRATEGY, record.upgradeStrategy?.name)
                .set(SETTING_SYNC_STRATEGY, record.settingSyncStrategy?.name)
                .set(CATEGORY, record.category)
                .set(LOGO_URL, record.logoUrl)
                .set(PAC, record.enablePac)
                .set(RELEASED_VERSION, record.releasedVersion)
                .set(RELEASED_VERSION_NAME, record.releasedVersionName)
                .set(RELEASED_SETTING_VERSION, record.releasedSettingVersion)
                .set(LATEST_VERSION_STATUS, record.latestVersionStatus.name)
                .set(STORE_STATUS, record.storeStatus.name)
                .set(DEBUG_PIPELINE_COUNT, record.debugPipelineCount)
                .set(INSTANCE_PIPELINE_COUNT, record.instancePipelineCount)
                .set(UPDATER, record.updater)
                .set(UPDATE_TIME, record.updateTime.toLocalDateTimeOrDefault())
                .execute()
        }
    }

    fun update(
        dslContext: DSLContext,
        record: PipelineTemplateInfoUpdateInfo,
        commonCondition: PipelineTemplateCommonCondition
    ) {
        with(TPipelineTemplateInfo.T_PIPELINE_TEMPLATE_INFO) {
            val now = LocalDateTime.now()
            dslContext.update(this)
                .apply {
                    record.name?.let { set(NAME, it) }
                    record.desc?.let { set(DESC, it) }
                    record.category?.let { set(CATEGORY, it) }
                    record.logoUrl?.let { set(LOGO_URL, it) }
                    record.enablePac?.let { set(PAC, it) }
                    record.releasedVersion?.let { set(RELEASED_VERSION, it) }
                    record.releasedVersionName?.let { set(RELEASED_VERSION_NAME, it) }
                    record.releasedSettingVersion?.let { set(RELEASED_SETTING_VERSION, it) }
                    record.storeStatus?.let { set(STORE_STATUS, it.name) }
                    record.debugPipelineCount?.let { set(DEBUG_PIPELINE_COUNT, it) }
                    record.instancePipelineCount?.let { set(INSTANCE_PIPELINE_COUNT, it) }
                    record.latestVersionStatus?.let { set(LATEST_VERSION_STATUS, it.name) }
                    record.publishStrategy?.let { set(PUBLISH_STRATEGY, it.name) }
                    record.upgradeStrategy?.let { set(UPGRADE_STRATEGY, it.name) }
                    record.settingSyncStrategy?.let { set(SETTING_SYNC_STRATEGY, it.name) }
                    record.updater?.let { set(UPDATER, it) }
                }
                .set(UPDATE_TIME, now)
                .where(buildQueryCondition(commonCondition))
                .execute()
        }
    }

    fun updateInstancePipelineCount(
        dslContext: DSLContext,
        projectId: String,
        templateId: String,
        count: Int
    ) {
        with(TPipelineTemplateInfo.T_PIPELINE_TEMPLATE_INFO) {
            dslContext.update(this)
                .set(INSTANCE_PIPELINE_COUNT, count)
                .where(PROJECT_ID.eq(projectId))
                .and(ID.eq(templateId))
                .execute()
        }
    }

    fun list(
        dslContext: DSLContext,
        commonCondition: PipelineTemplateCommonCondition
    ): List<PipelineTemplateInfoV2> {
        return with(TPipelineTemplateInfo.T_PIPELINE_TEMPLATE_INFO) {
            dslContext.selectFrom(this)
                .where(buildQueryCondition(commonCondition))
                .orderBy(UPDATE_TIME.desc())
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

    fun listAllIds(
        dslContext: DSLContext,
        projectId: String
    ): List<String> {
        return with(TPipelineTemplateInfo.T_PIPELINE_TEMPLATE_INFO) {
            dslContext.select(ID)
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .fetch()
                .map { it.value1() }
        }
    }

    fun listSrcTemplateIds(
        dslContext: DSLContext,
        projectId: String
    ): List<String> {
        return with(TPipelineTemplateInfo.T_PIPELINE_TEMPLATE_INFO) {
            dslContext.select(SRC_TEMPLATE_ID)
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(SRC_TEMPLATE_ID.isNotNull) // 显式过滤非空值
                .fetch()
                .mapNotNull { it.value1() } // 二次确保非空
        }
    }

    fun count(
        dslContext: DSLContext,
        commonCondition: PipelineTemplateCommonCondition
    ): Int {
        return with(TPipelineTemplateInfo.T_PIPELINE_TEMPLATE_INFO) {
            dslContext.selectCount().from(this)
                .where(buildQueryCondition(commonCondition))
                .fetchOne(0, Int::class.java)!!
        }
    }

    fun get(
        dslContext: DSLContext,
        commonCondition: PipelineTemplateCommonCondition
    ): PipelineTemplateInfoV2? {
        return with(TPipelineTemplateInfo.T_PIPELINE_TEMPLATE_INFO) {
            dslContext.selectFrom(this)
                .where(buildQueryCondition(commonCondition))
                .fetchOne()?.convert()
        }
    }

    fun getType2Count(
        dslContext: DSLContext,
        projectId: String,
        templateIds: List<String>
    ): Map<String, Int> {
        return with(TPipelineTemplateInfo.T_PIPELINE_TEMPLATE_INFO) {
            dslContext.select(TYPE, DSL.count())
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(ID.`in`(templateIds))
                .groupBy(TYPE)
                .fetch().map { Pair(it.value1(), it.value2()) }.toMap()
        }
    }

    fun getSource2count(
        dslContext: DSLContext,
        commonCondition: PipelineTemplateCommonCondition
    ): Map<String, Int> {
        return with(TPipelineTemplateInfo.T_PIPELINE_TEMPLATE_INFO) {
            dslContext.select(MODE, DSL.count())
                .from(this)
                .where(buildQueryCondition(commonCondition))
                .groupBy(MODE)
                .fetch().map { Pair(it.value1(), it.value2()) }.toMap()
        }
    }

    fun get(
        dslContext: DSLContext,
        templateId: String
    ): PipelineTemplateInfoV2? {
        return with(TPipelineTemplateInfo.T_PIPELINE_TEMPLATE_INFO) {
            dslContext.selectFrom(this)
                .where(ID.eq(templateId))
                .fetchOne()?.convert()
        }
    }

    fun get(
        dslContext: DSLContext,
        projectId: String,
        templateId: String
    ): PipelineTemplateInfoV2? {
        return with(TPipelineTemplateInfo.T_PIPELINE_TEMPLATE_INFO) {
            dslContext.selectFrom(this)
                .where(ID.eq(templateId))
                .and(PROJECT_ID.eq(projectId))
                .fetchOne()?.convert()
        }
    }

    fun isNameExist(
        dslContext: DSLContext,
        projectId: String,
        templateName: String,
        excludeTemplateId: String?
    ): Boolean {
        return with(TPipelineTemplateInfo.T_PIPELINE_TEMPLATE_INFO) {
            val where = dslContext.select(ID)
                .from(this)
                .where(PROJECT_ID.eq(projectId))

            if (excludeTemplateId != null) {
                where.and(ID.notEqual(excludeTemplateId))
            }

            where.and(NAME.eq(templateName)).fetch().isNotEmpty
        }
    }

    fun delete(
        dslContext: DSLContext,
        commonCondition: PipelineTemplateCommonCondition
    ) {
        return with(TPipelineTemplateInfo.T_PIPELINE_TEMPLATE_INFO) {
            dslContext.deleteFrom(this)
                .where(buildQueryCondition(commonCondition))
                .execute()
        }
    }

    fun buildQueryCondition(commonCondition: PipelineTemplateCommonCondition): MutableList<Condition> {
        return with(TPipelineTemplateInfo.T_PIPELINE_TEMPLATE_INFO) {
            commonCondition.checkAllFieldsAreNull()
            with(commonCondition) {
                val conditions = mutableListOf<Condition>()
                if (projectId != null) conditions.add(PROJECT_ID.eq(projectId))
                if (templateId != null) conditions.add(ID.eq(templateId))
                if (fuzzySearchName != null && fuzzySearchName!!.isNotBlank()) {
                    conditions.add(NAME.like("%$fuzzySearchName%"))
                }
                if (mode != null) conditions.add(MODE.eq(mode!!.name))
                if (desc != null && desc!!.isNotBlank()) conditions.add(DESC.like("%$desc%"))
                if (exactSearchName != null && exactSearchName!!.isNotBlank()) conditions.add(NAME.eq(exactSearchName))
                if (type != null) conditions.add(TYPE.eq(type!!.value))
                if (enablePac != null) conditions.add(PAC.eq(enablePac))
                if (releasedVersion != null) conditions.add(RELEASED_VERSION.eq(releasedVersion))
                if (releasedVersionName != null) conditions.add(RELEASED_VERSION_NAME.eq(releasedVersionName))
                if (releasedSettingVersion != null) conditions.add(RELEASED_SETTING_VERSION.eq(releasedSettingVersion))
                if (storeStatus != null) conditions.add(STORE_STATUS.eq(storeStatus!!.name))
                if (storeFlag == true) conditions.add(STORE_STATUS.eq(TemplateStatusEnum.RELEASED.name))
                if (srcTemplateId != null) conditions.add(SRC_TEMPLATE_ID.eq(srcTemplateId))
                if (srcTemplateProjectId != null) conditions.add(SRC_TEMPLATE_PROJECT_ID.eq(srcTemplateProjectId))
                if (debugPipelineCount != null) conditions.add(DEBUG_PIPELINE_COUNT.eq(debugPipelineCount))
                if (instancePipelineCount != null) conditions.add(INSTANCE_PIPELINE_COUNT.eq(instancePipelineCount))
                if (creator != null) conditions.add(CREATOR.eq(creator))
                if (updater != null) conditions.add(UPDATER.eq(updater))
                when {
                    filterTemplateIds != null && filterTemplateIds!!.isEmpty() -> {
                        conditions.add(ID.isNull)
                    }

                    filterTemplateIds != null -> {
                        conditions.add(ID.`in`(filterTemplateIds))
                    }
                }
                if (latestVersionStatus != null) conditions.add(LATEST_VERSION_STATUS.eq(latestVersionStatus!!.name))
                if (upgradeStrategy != null) conditions.add(UPGRADE_STRATEGY.eq(upgradeStrategy!!.name))
                conditions
            }
        }
    }

    fun listPacSettings(
        dslContext: DSLContext,
        templateIds: List<String>
    ): Map<String, Boolean> {
        return with(TPipelineTemplateInfo.T_PIPELINE_TEMPLATE_INFO) {
            dslContext.select(ID, PAC)
                .from(this)
                .where(ID.`in`(templateIds))
                .fetch().map {
                    Pair(it.value1(), it.value2())
                }.toMap()
        }
    }

    fun TPipelineTemplateInfoRecord.convert(): PipelineTemplateInfoV2 {
        val mode = TemplateType.valueOf(this.mode)
        return PipelineTemplateInfoV2(
            id = this.id,
            projectId = this.projectId,
            name = this.name,
            desc = this.desc,
            mode = mode,
            upgradeStrategy = this.upgradeStrategy?.let { UpgradeStrategyEnum.valueOf(it) },
            settingSyncStrategy = this.settingSyncStrategy?.let { UpgradeStrategyEnum.valueOf(it) },
            publishStrategy = this.publishStrategy?.let { UpgradeStrategyEnum.valueOf(it) },
            sourceName = TemplateType.getDisplayName(mode),
            category = this.category,
            type = PipelineTemplateType.valueOf(this.type),
            logoUrl = this.logoUrl,
            enablePac = this.pac,
            releasedVersion = this.releasedVersion,
            releasedVersionName = releasedVersionName,
            releasedSettingVersion = this.releasedSettingVersion,
            latestVersionStatus = VersionStatus.get(this.latestVersionStatus),
            storeStatus = TemplateStatusEnum.valueOf(this.storeStatus),
            srcTemplateId = this.srcTemplateId,
            srcTemplateProjectId = this.srcTemplateProjectId,
            debugPipelineCount = this.debugPipelineCount,
            instancePipelineCount = this.instancePipelineCount,
            creator = this.creator,
            updater = this.updater,
            createdTime = this.createdTime.timestampmilli(),
            updateTime = this.updateTime.timestampmilli()
        )
    }
}
