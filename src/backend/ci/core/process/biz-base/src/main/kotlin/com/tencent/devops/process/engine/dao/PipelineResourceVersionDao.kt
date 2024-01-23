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

package com.tencent.devops.process.engine.dao

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.BranchVersionAction
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.model.process.Tables.T_PIPELINE_RESOURCE_VERSION
import com.tencent.devops.model.process.tables.records.TPipelineResourceVersionRecord
import com.tencent.devops.process.pojo.pipeline.PipelineResourceVersion
import com.tencent.devops.process.pojo.setting.PipelineVersionSimple
import org.jooq.DSLContext
import org.jooq.RecordMapper
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Suppress("Unused", "LongParameterList", "ReturnCount", "TooManyFunctions")
@Repository
class PipelineResourceVersionDao {

    companion object {
        private val mapper = PipelineResourceVersionJooqMapper()
        private val sampleMapper = PipelineVersionSimpleJooqMapper()
        private const val DEFAULT_PAGE_SIZE = 10
    }

    fun create(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        creator: String,
        version: Int,
        versionName: String?,
        model: Model,
        baseVersion: Int?,
        yaml: String?,
        versionNum: Int?,
        pipelineVersion: Int?,
        triggerVersion: Int?,
        settingVersion: Int?,
        versionStatus: VersionStatus?,
        branchAction: BranchVersionAction?,
        description: String?
    ): TPipelineResourceVersionRecord? {
        return create(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            creator = creator,
            version = version,
            versionName = versionName,
            modelStr = JsonUtil.toJson(model, formatted = false),
            yamlStr = yaml,
            baseVersion = baseVersion,
            versionNum = versionNum,
            pipelineVersion = pipelineVersion,
            triggerVersion = triggerVersion,
            settingVersion = settingVersion,
            versionStatus = versionStatus,
            branchAction = branchAction,
            description = description
        )
    }

    fun create(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        creator: String,
        version: Int,
        versionName: String?,
        modelStr: String,
        baseVersion: Int?,
        yamlStr: String?,
        versionNum: Int?,
        pipelineVersion: Int?,
        triggerVersion: Int?,
        settingVersion: Int?,
        versionStatus: VersionStatus?,
        branchAction: BranchVersionAction?,
        description: String?
    ): TPipelineResourceVersionRecord? {
        with(T_PIPELINE_RESOURCE_VERSION) {
            return dslContext.insertInto(this)
                .set(PROJECT_ID, projectId)
                .set(PIPELINE_ID, pipelineId)
                .set(VERSION, version)
                .set(VERSION_NAME, versionName)
                .set(MODEL, modelStr)
                .set(YAML, yamlStr)
                .set(CREATOR, creator)
                .set(CREATE_TIME, LocalDateTime.now())
                .set(VERSION_NUM, versionNum)
                .set(PIPELINE_VERSION, pipelineVersion)
                .set(TRIGGER_VERSION, triggerVersion)
                .set(SETTING_VERSION, settingVersion)
                .set(STATUS, versionStatus?.name)
                .set(BRANCH_ACTION, branchAction?.name)
                .set(DESCRIPTION, description)
                .set(BASE_VERSION, baseVersion)
                .onDuplicateKeyUpdate()
                .set(MODEL, modelStr)
                .set(YAML, yamlStr)
                .set(CREATOR, creator)
                .set(VERSION_NAME, versionName)
                .set(BASE_VERSION, baseVersion)
                .set(PIPELINE_VERSION, pipelineVersion)
                .set(TRIGGER_VERSION, triggerVersion)
                .set(SETTING_VERSION, settingVersion)
                .set(STATUS, versionStatus?.name)
                .set(BRANCH_ACTION, branchAction?.name)
                .set(DESCRIPTION, description)
                .returning()
                .fetchOne()
        }
    }

    fun getVersionModelString(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        version: Int?,
        includeDraft: Boolean? = null
    ): String? {

        return with(T_PIPELINE_RESOURCE_VERSION) {
            val where = dslContext.select(MODEL)
                .from(this)
                .where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId)))
            if (version != null) {
                where.and(VERSION.eq(version))
            } else {
                // 非新的逻辑请求则保持旧逻辑
                if (includeDraft != true) where.and(STATUS.ne(VersionStatus.COMMITTING.name))
                where.orderBy(VERSION.desc()).limit(1)
            }
            where.fetchAny(0, String::class.java)
        }
    }

    fun getVersionResource(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        version: Int? = null,
        includeDraft: Boolean? = null
    ): PipelineResourceVersion? {
        with(T_PIPELINE_RESOURCE_VERSION) {
            val query = dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId)))
            if (version != null) {
                query.and(VERSION.eq(version))
            } else {
                // 非新的逻辑请求则保持旧逻辑
                if (includeDraft != true) query.and(STATUS.ne(VersionStatus.COMMITTING.name))
                query.orderBy(VERSION.desc()).limit(1)
            }
            return query.fetchAny(mapper)
        }
    }

    fun getBranchVersionResource(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        branchName: String
    ): PipelineResourceVersion? {
        // 一定是取最新的分支版本
        with(T_PIPELINE_RESOURCE_VERSION) {
            return dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId)))
                .and(STATUS.eq(VersionStatus.BRANCH.name))
                .and(
                    BRANCH_ACTION.ne(BranchVersionAction.INACTIVE.name)
                        .or(BRANCH_ACTION.isNull)
                )
                .and(VERSION_NAME.eq(branchName))
                .orderBy(VERSION.desc()).limit(1)
                .fetchAny(mapper)
        }
    }

    fun getLatestVersionResource(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String
    ): PipelineResourceVersion? {
        with(T_PIPELINE_RESOURCE_VERSION) {
            return dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId)))
                .orderBy(VERSION.desc()).limit(1)
                .fetchAny(mapper)
        }
    }

    fun getReleaseVersionRecord(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String
    ): PipelineResourceVersion? {
        with(T_PIPELINE_RESOURCE_VERSION) {
            return dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId)))
                .and(STATUS.eq(VersionStatus.RELEASED.name).or(STATUS.isNull))
                .orderBy(VERSION.desc()).limit(1)
                .fetchAny(mapper)
        }
    }

    fun getDraftVersionResource(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String
    ): PipelineResourceVersion? {
        with(T_PIPELINE_RESOURCE_VERSION) {
            return dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId)))
                .and(STATUS.eq(VersionStatus.COMMITTING.name))
                .fetchAny(mapper)
        }
    }

    fun clearDraftVersion(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String
    ): Int? {
        return with(T_PIPELINE_RESOURCE_VERSION) {
            dslContext.deleteFrom(this)
                .where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId)))
                .and(STATUS.eq(VersionStatus.COMMITTING.name))
                .execute()
        }
    }

    fun clearActiveBranchVersion(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String
    ): Int? {
        return with(T_PIPELINE_RESOURCE_VERSION) {
            dslContext.update(this)
                .set(BRANCH_ACTION, BranchVersionAction.INACTIVE.name)
                .where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId)))
                .and(STATUS.eq(VersionStatus.BRANCH.name))
                .and(
                    BRANCH_ACTION.ne(BranchVersionAction.INACTIVE.name)
                        .or(BRANCH_ACTION.isNull)
                )
                .execute()
        }
    }

    fun deleteEarlyVersion(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        currentVersion: Int,
        maxPipelineResNum: Int
    ): Int {
        return with(T_PIPELINE_RESOURCE_VERSION) {
            dslContext.deleteFrom(this)
                .where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId)))
                .and(VERSION.le(currentVersion - maxPipelineResNum))
                .and(REFER_FLAG.eq(false))
                .execute()
        }
    }

    fun deleteByVer(dslContext: DSLContext, projectId: String, pipelineId: String, version: Int) {
        return with(T_PIPELINE_RESOURCE_VERSION) {
            dslContext.deleteFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(VERSION.eq(version))
                .and(PROJECT_ID.eq(projectId))
                .and(REFER_FLAG.eq(false))
                .execute()
        }
    }

    fun listPipelineVersion(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        offset: Int,
        limit: Int,
        excludeVersion: Int?,
        creator: String?,
        versionName: String?,
        description: String?
    ): List<PipelineVersionSimple> {
        with(T_PIPELINE_RESOURCE_VERSION) {
            val query = dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId)))
                .and(
                    BRANCH_ACTION.ne(BranchVersionAction.INACTIVE.name)
                        .or(BRANCH_ACTION.isNull)
                )
            creator?.let { query.and(CREATOR.eq(creator)) }
            description?.let { query.and(DESCRIPTION.like("%$description%")) }
            versionName?.let {
                query.and(
                    VERSION.like("%$versionName%")
                        .or(VERSION_NAME.like("%$versionName%"))
                )
            }
            excludeVersion?.let {
                query.and(VERSION.notEqual(excludeVersion))
            }
            return query.orderBy(VERSION.desc()).limit(limit).offset(offset).fetch(sampleMapper)
        }
    }

    fun listPipelineVersionInList(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        versions: Set<Int>
    ): List<PipelineVersionSimple> {
        with(T_PIPELINE_RESOURCE_VERSION) {
            return dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId)))
                .and(VERSION.`in`(versions))
                .fetch(sampleMapper)
        }
    }

    fun getVersionCreatorInPage(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        offset: Int,
        limit: Int
    ): List<String> {
        with(T_PIPELINE_RESOURCE_VERSION) {
            return dslContext.selectDistinct(CREATOR)
                .from(this)
                .where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId)))
                .and(
                    BRANCH_ACTION.ne(BranchVersionAction.INACTIVE.name)
                        .or(BRANCH_ACTION.isNull)
                )
                .limit(limit).offset(offset)
                .fetch().map { it.component1() }
        }
    }

    fun count(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        creator: String?,
        description: String?
    ): Int {
        with(T_PIPELINE_RESOURCE_VERSION) {
            val query = dslContext.select(DSL.count(PIPELINE_ID))
                .from(this)
                .where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId)))
                .and(
                    BRANCH_ACTION.ne(BranchVersionAction.INACTIVE.name)
                        .or(BRANCH_ACTION.isNull)
                )
            creator?.let { query.and(CREATOR.eq(creator)) }
            description?.let { query.and(DESCRIPTION.like("%$description%")) }
            return query.fetchOne(0, Int::class.java)!!
        }
    }

    fun countVersionCreator(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String
    ): Int {
        with(T_PIPELINE_RESOURCE_VERSION) {
            val query = dslContext.selectDistinct(CREATOR)
                .from(this)
                .where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId)))
                .and(
                    BRANCH_ACTION.ne(BranchVersionAction.INACTIVE.name)
                        .or(BRANCH_ACTION.isNull)
                )
            return query.fetchCount()
        }
    }

    fun updateDebugBuildId(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        version: Int,
        debugBuildId: String
    ): Boolean {
        with(T_PIPELINE_RESOURCE_VERSION) {
            return dslContext.update(this)
                .set(DEBUG_BUILD_ID, debugBuildId)
                .where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId)).and(VERSION.eq(version)))
                .execute() == 1
        }
    }

    fun deleteAllVersion(dslContext: DSLContext, projectId: String, pipelineId: String) {
        return with(T_PIPELINE_RESOURCE_VERSION) {
            dslContext.deleteFrom(this)
                .where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId)))
                .execute()
        }
    }

    fun getPipelineVersionSimple(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        version: Int
    ): PipelineVersionSimple? {
        with(T_PIPELINE_RESOURCE_VERSION) {
            return dslContext.select(
                PIPELINE_ID,
                CREATOR,
                CREATE_TIME,
                VERSION,
                VERSION_NAME,
                REFER_FLAG,
                REFER_COUNT
            )
                .from(this)
                .where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId)).and(VERSION.eq(version)))
                .fetchOneInto(PipelineVersionSimple::class.java)
        }
    }

    fun updatePipelineVersionReferInfo(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        version: Int,
        referCount: Int,
        referFlag: Boolean? = null
    ) {
        with(T_PIPELINE_RESOURCE_VERSION) {
            val baseStep = dslContext.update(this)
                .set(REFER_COUNT, referCount)
            referFlag?.let { baseStep.set(REFER_FLAG, referFlag) }
            baseStep.where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId)).and(VERSION.eq(version))).execute()
        }
    }

    fun updateSettingVersion(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        version: Int,
        settingVersion: Int
    ): Int {
        with(T_PIPELINE_RESOURCE_VERSION) {
            return dslContext.update(this)
                .set(SETTING_VERSION, settingVersion)
                .where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId)).and(VERSION.eq(version)))
                .execute()
        }
    }

    fun updateBranchVersion(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        branchName: String?,
        branchVersionAction: BranchVersionAction
    ): Int {
        with(T_PIPELINE_RESOURCE_VERSION) {
            val update = dslContext.update(this)
                .set(BRANCH_ACTION, branchVersionAction.name)
                .where(
                    PIPELINE_ID.eq(pipelineId)
                        .and(PROJECT_ID.eq(projectId))
                        .and(STATUS.eq(VersionStatus.BRANCH.name))
                        // 只有非活跃的分支可以被修改状态
                        .and(
                            BRANCH_ACTION.ne(BranchVersionAction.INACTIVE.name)
                                .or(BRANCH_ACTION.isNull)
                        )
                )
            branchName?.let { update.and(VERSION_NAME.eq(branchName)) }
            return update.execute()
        }
    }

    class PipelineResourceVersionJooqMapper : RecordMapper<TPipelineResourceVersionRecord, PipelineResourceVersion> {
        override fun map(record: TPipelineResourceVersionRecord?): PipelineResourceVersion? {
            return record?.let {
                PipelineResourceVersion(
                    projectId = record.projectId,
                    pipelineId = record.pipelineId,
                    version = record.version,
                    model = record.model?.let { str ->
                        try {
                            JsonUtil.to(str, Model::class.java)
                        } catch (ignore: Exception) {
                            null
                        }
                    } ?: return null,
                    yaml = record.yaml,
                    creator = record.creator,
                    versionName = record.versionName,
                    createTime = record.createTime,
                    updateTime = record.updateTime,
                    versionNum = record.versionNum ?: record.version,
                    pipelineVersion = record.pipelineVersion,
                    triggerVersion = record.triggerVersion,
                    settingVersion = record.settingVersion,
                    referFlag = record.referFlag,
                    referCount = record.referCount,
                    status = record.status?.let { VersionStatus.valueOf(it) },
                    branchAction = record.branchAction?.let { BranchVersionAction.valueOf(it) },
                    description = record.description,
                    debugBuildId = record.debugBuildId,
                    baseVersion = record.baseVersion
                )
            }
        }
    }

    class PipelineVersionSimpleJooqMapper : RecordMapper<TPipelineResourceVersionRecord, PipelineVersionSimple> {
        override fun map(record: TPipelineResourceVersionRecord?): PipelineVersionSimple? {
            return record?.let {
                PipelineVersionSimple(
                    pipelineId = record.pipelineId,
                    creator = record.creator ?: "unknown",
                    createTime = record.createTime.timestampmilli(),
                    updateTime = record.updateTime.timestampmilli(),
                    version = record.version ?: 1,
                    versionName = record.versionName ?: "init",
                    referFlag = record.referFlag,
                    referCount = record.referCount,
                    versionNum = record.versionNum ?: record.version ?: 1,
                    pipelineVersion = record.pipelineVersion,
                    triggerVersion = record.triggerVersion,
                    settingVersion = record.settingVersion,
                    status = record.status?.let { VersionStatus.valueOf(it) },
                    debugBuildId = record.debugBuildId,
                    baseVersion = record.baseVersion,
                    description = record.description
                )
            }
        }
    }
}
