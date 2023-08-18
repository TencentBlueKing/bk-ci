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
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.model.process.Tables.T_PIPELINE_RESOURCE_VERSION
import com.tencent.devops.model.process.tables.records.TPipelineResourceVersionRecord
import com.tencent.devops.process.pojo.pipeline.PipelineResourceVersion
import com.tencent.devops.process.pojo.setting.PipelineVersionSimple
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Suppress("Unused", "LongParameterList", "ReturnCount", "TooManyFunctions")
@Repository
class PipelineResVersionDao {

    fun create(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        creator: String,
        version: Int,
        versionName: String,
        model: Model,
        yaml: String?,
        pipelineVersion: Int?,
        triggerVersion: Int?,
        settingVersion: Int?,
        status: VersionStatus?,
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
            pipelineVersion = pipelineVersion,
            triggerVersion = triggerVersion,
            settingVersion = settingVersion,
            status = status,
            description = description
        )
    }

    fun create(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        creator: String,
        version: Int,
        versionName: String = "init",
        modelStr: String,
        yamlStr: String?,
        pipelineVersion: Int?,
        triggerVersion: Int?,
        settingVersion: Int?,
        status: VersionStatus?,
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
                .set(PIPELINE_VERSION, pipelineVersion)
                .set(TRIGGER_VERSION, triggerVersion)
                .set(SETTING_VERSION, settingVersion)
                .set(STATUS, status?.name)
                .set(DESCRIPTION, description)
                .onDuplicateKeyUpdate()
                .set(MODEL, modelStr)
                .set(CREATOR, creator)
                .set(VERSION_NAME, versionName)
                .set(PIPELINE_VERSION, pipelineVersion)
                .set(TRIGGER_VERSION, triggerVersion)
                .set(SETTING_VERSION, settingVersion)
                .set(STATUS, status?.name)
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
        version: Int?,
        includeDraft: Boolean? = null
    ): PipelineResourceVersion? {
        with(T_PIPELINE_RESOURCE_VERSION) {
            val where = dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId)))
            if (version != null) {
                where.and(VERSION.eq(version))
            } else {
                // 非新的逻辑请求则保持旧逻辑
                if (includeDraft != true) where.and(STATUS.ne(VersionStatus.COMMITTING.name))
                where.orderBy(VERSION.desc()).limit(1)
            }
            val record = where.fetchAny() ?: return null
            return PipelineResourceVersion(
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
                pipelineVersion = record.pipelineVersion,
                triggerVersion = record.triggerVersion,
                settingVersion = record.settingVersion,
                referFlag = record.referFlag,
                referCount = record.referCount,
                status = record.status?.let { VersionStatus.valueOf(it) },
                description = record.description
            )
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
        creator: String?,
        description: String?
    ): List<PipelineVersionSimple> {
        val list = mutableListOf<PipelineVersionSimple>()
        with(T_PIPELINE_RESOURCE_VERSION) {
            val query = dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId)))
            creator?.let { query.and(CREATOR.eq(creator)) }
            description?.let { query.and(DESCRIPTION.like("%$description%")) }
            val result = query
                .orderBy(VERSION.desc()).limit(limit).offset(offset).fetch()
            result.forEach { record ->
                list.add(
                    PipelineVersionSimple(
                        pipelineId = pipelineId,
                        creator = record.creator ?: "unknown",
                        createTime = record.createTime?.timestampmilli() ?: 0,
                        version = record.version ?: 1,
                        versionName = record.versionName ?: "init",
                        referFlag = record.referFlag,
                        referCount = record.referCount,
                        pipelineVersion = record.pipelineVersion,
                        triggerVersion = record.triggerVersion,
                        settingVersion = record.settingVersion,
                        status = record.status?.let { VersionStatus.valueOf(it) },
                        debugBuildId = record.debugBuildId,
                        pacRefs = record.refs
                    )
                )
            }
        }
        return list
    }

    fun listPipelineVersionInList(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        versions: Set<Int>
    ): List<PipelineVersionSimple> {
        val list = mutableListOf<PipelineVersionSimple>()
        with(T_PIPELINE_RESOURCE_VERSION) {
            val result = dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId)))
                .and(VERSION.`in`(versions))
                .fetch()
            result.forEach { record ->
                list.add(
                    PipelineVersionSimple(
                        pipelineId = pipelineId,
                        creator = record.creator ?: "unknown",
                        createTime = record.createTime?.timestampmilli() ?: 0,
                        version = record.version ?: 1,
                        versionName = record.versionName ?: "init",
                        referFlag = record.referFlag,
                        referCount = record.referCount,
                        pipelineVersion = record.pipelineVersion,
                        triggerVersion = record.triggerVersion,
                        settingVersion = record.settingVersion,
                        status = record.status?.let { VersionStatus.valueOf(it) },
                        debugBuildId = record.debugBuildId,
                        pacRefs = record.refs
                    )
                )
            }
        }
        return list
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
            return query.fetchCount()
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
}
