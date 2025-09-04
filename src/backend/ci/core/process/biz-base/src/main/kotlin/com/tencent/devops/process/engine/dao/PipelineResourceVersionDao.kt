/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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
import com.tencent.devops.model.process.tables.TPipelineResourceVersion
import com.tencent.devops.model.process.tables.records.TPipelineResourceVersionRecord
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.pojo.pipeline.PipelineResourceVersion
import com.tencent.devops.process.pojo.setting.PipelineVersionSimple
import com.tencent.devops.process.utils.PipelineVersionUtils
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
        userId: String,
        projectId: String,
        pipelineId: String,
        version: Int,
        versionName: String,
        model: Model,
        baseVersion: Int?,
        yamlStr: String?,
        yamlVersion: String?,
        versionNum: Int?,
        pipelineVersion: Int?,
        triggerVersion: Int?,
        settingVersion: Int?,
        versionStatus: VersionStatus?,
        branchAction: BranchVersionAction?,
        description: String?
    ): TPipelineResourceVersionRecord? {
        with(T_PIPELINE_RESOURCE_VERSION) {
            val modelStr = JsonUtil.toJson(model, formatted = false)
            val createTime = LocalDateTime.now()
            val releaseTime = createTime.takeIf {
                // 发布时间根据版本转为RELEASED状态为准，默认和新增分支版本也记录为发布时间
                versionStatus == VersionStatus.RELEASED ||
                    versionStatus == VersionStatus.BRANCH || versionStatus == null
            }
            return dslContext.insertInto(this)
                .set(PROJECT_ID, projectId)
                .set(PIPELINE_ID, pipelineId)
                .set(VERSION, version)
                .set(VERSION_NAME, versionName)
                .set(MODEL, modelStr)
                .set(YAML, yamlStr)
                .set(YAML_VERSION, yamlVersion)
                .set(CREATOR, userId)
                .set(UPDATER, userId)
                .set(CREATE_TIME, createTime)
                .set(VERSION_NUM, versionNum)
                .set(PIPELINE_VERSION, pipelineVersion)
                .set(TRIGGER_VERSION, triggerVersion)
                .set(SETTING_VERSION, settingVersion)
                .set(STATUS, versionStatus?.name)
                .set(BRANCH_ACTION, branchAction?.name)
                .set(DESCRIPTION, description)
                .set(BASE_VERSION, baseVersion)
                .set(REFER_FLAG, false)
                .set(RELEASE_TIME, releaseTime)
                .onDuplicateKeyUpdate()
                .set(MODEL, modelStr)
                .set(YAML, yamlStr)
                .set(YAML_VERSION, yamlVersion)
                .set(UPDATER, userId)
                .set(VERSION_NUM, versionNum)
                .set(VERSION_NAME, versionName)
                .set(BASE_VERSION, baseVersion)
                .set(PIPELINE_VERSION, pipelineVersion)
                .set(TRIGGER_VERSION, triggerVersion)
                .set(SETTING_VERSION, settingVersion)
                .set(STATUS, versionStatus?.name)
                .set(BRANCH_ACTION, branchAction?.name)
                .set(DESCRIPTION, description)
                .set(RELEASE_TIME, releaseTime)
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
                if (includeDraft != true) where.and(
                    (
                        STATUS.ne(VersionStatus.COMMITTING.name)
                            .and(STATUS.ne(VersionStatus.DELETE.name))
                        )
                        .or(STATUS.isNull)
                )
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
                if (includeDraft != true) query.and(
                    (
                        STATUS.ne(VersionStatus.COMMITTING.name)
                            .and(STATUS.ne(VersionStatus.DELETE.name))
                        )
                        .or(STATUS.isNull)
                )
                query.orderBy(VERSION.desc()).limit(1)
            }
            return query.fetchAny(mapper)
        }
    }

    fun getBranchVersionResource(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        branchName: String?
    ): PipelineResourceVersion? {
        // 一定是取最新的分支版本
        with(T_PIPELINE_RESOURCE_VERSION) {
            val select = dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId)))
                .and(STATUS.eq(VersionStatus.BRANCH.name))
                .and(
                    BRANCH_ACTION.ne(BranchVersionAction.INACTIVE.name)
                        .or(BRANCH_ACTION.isNull)
                )
            branchName?.let { select.and(VERSION_NAME.eq(branchName)) }
            return select.orderBy(VERSION.desc()).limit(1)
                .fetchAny(mapper)
        }
    }

    fun getActiveBranchVersionCount(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String
    ): Int {
        // 一定是取最新的分支版本
        with(T_PIPELINE_RESOURCE_VERSION) {
            return dslContext.selectCount()
                .from(this)
                .where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId)))
                .and(STATUS.eq(VersionStatus.BRANCH.name))
                .and(
                    BRANCH_ACTION.ne(BranchVersionAction.INACTIVE.name)
                        .or(BRANCH_ACTION.isNull)
                )
                .fetchOne(0, Int::class.java)!!
        }
    }

    fun getLatestVersionResource(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String
    ): PipelineResourceVersion? {
        with(T_PIPELINE_RESOURCE_VERSION) {
            // 这里只需要返回当前VERSION数字最大的记录，不需要关心版本状态
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
        pipelineId: String,
        branchName: String? = null
    ): Int {
        with(T_PIPELINE_RESOURCE_VERSION) {
            val update = dslContext.update(this)
                .set(BRANCH_ACTION, BranchVersionAction.INACTIVE.name)
                .set(UPDATE_TIME, DSL.field(UPDATE_TIME.name, LocalDateTime::class.java))
                .where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId)))
                .and(STATUS.eq(VersionStatus.BRANCH.name))
                .and(
                    BRANCH_ACTION.ne(BranchVersionAction.INACTIVE.name)
                        .or(BRANCH_ACTION.isNull)
                )
            branchName?.let { update.and(VERSION_NAME.eq(branchName)) }
            return update.execute()
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

    fun deleteByVersion(dslContext: DSLContext, projectId: String, pipelineId: String, version: Int) {
        return with(T_PIPELINE_RESOURCE_VERSION) {
            dslContext.update(this)
                .set(STATUS, VersionStatus.DELETE.name)
                .set(UPDATE_TIME, DSL.field(UPDATE_TIME.name, LocalDateTime::class.java))
                .where(PIPELINE_ID.eq(pipelineId))
                .and(VERSION.eq(version))
                .and(PROJECT_ID.eq(projectId))
                .execute()
        }
    }

    fun listPipelineVersion(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        pipelineInfo: PipelineInfo,
        queryUnknownRelatedFlag: Boolean? = null,
        maxQueryVersion: Int? = null,
        offset: Int,
        limit: Int,
        includeDraft: Boolean? = null,
        excludeVersion: Int? = null,
        creator: String? = null,
        versionName: String? = null,
        description: String? = null,
        buildOnly: Boolean? = false
    ): List<PipelineVersionSimple> {
        with(T_PIPELINE_RESOURCE_VERSION) {
            val query = dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId)))
                .and(
                    STATUS.ne(VersionStatus.DELETE.name)
                        .or(STATUS.isNull)
                )
                .and(
                    BRANCH_ACTION.ne(BranchVersionAction.INACTIVE.name)
                        .or(BRANCH_ACTION.isNull)
                )
            if (includeDraft == false) {
                query.and(
                    STATUS.ne(VersionStatus.COMMITTING.name)
                        .or(STATUS.isNull)
                )
            }
            excludeVersion?.let {
                query.and(VERSION.notEqual(excludeVersion))
            }
            versionName?.let {
                query.and(
                    VERSION.like("%$versionName%")
                        .or(VERSION_NAME.like("%$versionName%"))
                )
            }
            creator?.let { query.and(CREATOR.eq(creator)) }
            description?.let { query.and(DESCRIPTION.like("%$description%")) }
            if (queryUnknownRelatedFlag == true) {
                query.and(REFER_FLAG.isNull)
            } else if (queryUnknownRelatedFlag == false) {
                query.and(REFER_FLAG.isNotNull)
            }
            maxQueryVersion?.let {
                query.and(VERSION.le(maxQueryVersion))
            }
            if (buildOnly == true) {
                query.and(
                    STATUS.ne(VersionStatus.RELEASED.name)
                        .or(VERSION.eq(getLatestReleaseVersion(dslContext, pipelineId, projectId)))
                )
            }
            val list = query.orderBy(
                RELEASE_TIME.desc(), VERSION.desc()
            ).limit(limit).offset(offset).fetch(sampleMapper)
            list.forEach { if (it.version == pipelineInfo.version) it.latestReleasedFlag = true }
            return list
        }
    }

    private fun TPipelineResourceVersion.getLatestReleaseVersion(
        dslContext: DSLContext,
        pipelineId: String,
        projectId: String
    ) = dslContext.select(VERSION).from(this)
        .where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId)))
        .and(STATUS.eq(VersionStatus.RELEASED.name).or(STATUS.isNull))
        .orderBy(RELEASE_TIME.desc(), VERSION.desc())
        .limit(1)

    fun listPipelineVersionInList(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        versions: Set<Int>
    ): List<PipelineVersionSimple> {
        with(T_PIPELINE_RESOURCE_VERSION) {
            return dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId)))
                .and(STATUS.ne(VersionStatus.DELETE.name))
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
                .and(STATUS.ne(VersionStatus.DELETE.name))
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
        includeDraft: Boolean?,
        versionName: String?,
        creator: String?,
        description: String?,
        buildOnly: Boolean? = false
    ): Int {
        with(T_PIPELINE_RESOURCE_VERSION) {
            val query = dslContext.select(DSL.count(PIPELINE_ID))
                .from(this)
                .where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId)))
                .and(
                    STATUS.ne(VersionStatus.DELETE.name)
                        .or(STATUS.isNull)
                )
                .and(
                    BRANCH_ACTION.ne(BranchVersionAction.INACTIVE.name)
                        .or(BRANCH_ACTION.isNull)
                )
            if (includeDraft == false) {
                query.and(
                    STATUS.ne(VersionStatus.COMMITTING.name)
                        .or(STATUS.isNull)
                )
            }
            versionName?.let {
                query.and(
                    VERSION.like("%$versionName%")
                        .or(VERSION_NAME.like("%$versionName%"))
                )
            }
            creator?.let { query.and(CREATOR.eq(creator)) }
            description?.let { query.and(DESCRIPTION.like("%$description%")) }
            if (buildOnly == true) {
                query.and(
                    STATUS.ne(VersionStatus.RELEASED.name)
                        .or(VERSION.eq(getLatestReleaseVersion(dslContext, pipelineId, projectId)))
                )
            }
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
                .and(STATUS.ne(VersionStatus.DELETE.name))
                .and(
                    BRANCH_ACTION.ne(BranchVersionAction.INACTIVE.name)
                        .or(BRANCH_ACTION.isNull)
                )
            return query.count()
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
                .set(UPDATE_TIME, DSL.field(UPDATE_TIME.name, LocalDateTime::class.java))
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
            return dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId)).and(VERSION.eq(version)))
                .fetchAny(sampleMapper)
        }
    }

    fun updatePipelineVersionReferInfo(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        versions: List<Int>,
        referCount: Int,
        referFlag: Boolean? = null
    ) {
        with(T_PIPELINE_RESOURCE_VERSION) {
            val baseStep = dslContext.update(this)
                .set(REFER_COUNT, referCount)
                .set(UPDATE_TIME, DSL.field(UPDATE_TIME.name, LocalDateTime::class.java))
            referFlag?.let { baseStep.set(REFER_FLAG, referFlag) }
            baseStep.where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId)).and(VERSION.`in`(versions)))
                .execute()
        }
    }

    fun updateSettingVersion(
        dslContext: DSLContext,
        userId: String,
        projectId: String,
        pipelineId: String,
        version: Int,
        settingVersion: Int
    ): Int {
        with(T_PIPELINE_RESOURCE_VERSION) {
            return dslContext.update(this)
                .set(SETTING_VERSION, settingVersion)
                .set(UPDATER, userId)
                .where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId)).and(VERSION.eq(version)))
                .execute()
        }
    }

    fun updateBranchVersion(
        dslContext: DSLContext,
        userId: String,
        projectId: String,
        pipelineId: String,
        branchName: String?,
        branchVersionAction: BranchVersionAction
    ): Int {
        with(T_PIPELINE_RESOURCE_VERSION) {
            val update = dslContext.update(this)
                .set(UPDATER, userId)
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
                val status = record.status?.let { VersionStatus.valueOf(it) } ?: VersionStatus.RELEASED
                val versionNum = (record.versionNum ?: record.version ?: 1)
                    .takeIf { status == VersionStatus.RELEASED }
                val versionName = record.versionName.takeIf {
                    name -> name != "init"
                } ?: PipelineVersionUtils.getVersionName(
                    versionNum, record.version, record.triggerVersion, record.settingVersion
                ) ?: "V$versionNum(${record.versionName})"
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
                    yamlVersion = record.yamlVersion,
                    creator = record.creator,
                    updater = record.updater,
                    versionName = versionName,
                    createTime = record.createTime,
                    updateTime = record.updateTime,
                    versionNum = versionNum,
                    pipelineVersion = record.pipelineVersion,
                    triggerVersion = record.triggerVersion,
                    settingVersion = record.settingVersion,
                    referFlag = record.referFlag,
                    referCount = record.referCount,
                    status = status,
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
                // 兼容版本管理前的历史数据
                val status = record.status?.let { VersionStatus.valueOf(it) } ?: VersionStatus.RELEASED
                val versionNum = (record.versionNum ?: record.version ?: 1)
                    .takeIf { status == VersionStatus.RELEASED }
                // 如果名称已经为init的则尝试计算新版本名，如果获取不到新的版本名称则保持init
                val versionName = record.versionName?.takeIf {
                    name -> name != "init"
                } ?: PipelineVersionUtils.getVersionName(
                    versionNum, record.version, record.triggerVersion, record.settingVersion
                ) ?: "V$versionNum(${record.versionName ?: "init"})"
                PipelineVersionSimple(
                    pipelineId = record.pipelineId,
                    creator = record.creator ?: "unknown",
                    createTime = record.createTime.timestampmilli(),
                    updater = record.updater,
                    updateTime = record.updateTime.timestampmilli(),
                    version = record.version ?: 1,
                    versionName = versionName,
                    referFlag = record.referFlag,
                    referCount = record.referCount,
                    versionNum = versionNum,
                    pipelineVersion = record.pipelineVersion,
                    triggerVersion = record.triggerVersion,
                    settingVersion = record.settingVersion,
                    status = status,
                    debugBuildId = record.debugBuildId,
                    baseVersion = record.baseVersion,
                    description = record.description,
                    yamlVersion = record.yamlVersion
                )
            }
        }
    }
}
