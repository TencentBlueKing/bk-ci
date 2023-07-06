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
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.model.process.Tables.T_PIPELINE_RESOURCE_VERSION
import com.tencent.devops.process.pojo.pipeline.PipelineResourceVersion
import com.tencent.devops.process.pojo.setting.PipelineVersionSimple
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Suppress("Unused", "LongParameterList", "ReturnCount")
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
        trigger: TriggerContainer,
        pipelineVersion: Int?,
        triggerVersion: Int?,
        settingVersion: Int?,
        draftFlag: Boolean
    ) {
        create(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            creator = creator,
            version = version,
            versionName = versionName,
            modelString = JsonUtil.toJson(model, formatted = false),
            pipelineVersion = pipelineVersion,
            triggerVersion = triggerVersion,
            settingVersion = settingVersion,
            draftFlag = draftFlag
        )
    }

    fun create(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        creator: String,
        version: Int,
        versionName: String = "init",
        modelString: String,
        pipelineVersion: Int?,
        triggerVersion: Int?,
        settingVersion: Int?,
        draftFlag: Boolean
    ) {
        with(T_PIPELINE_RESOURCE_VERSION) {
            dslContext.insertInto(this)
                .set(PROJECT_ID, projectId)
                .set(PIPELINE_ID, pipelineId)
                .set(VERSION, version)
                .set(VERSION_NAME, versionName)
                .set(MODEL, modelString)
                .set(CREATOR, creator)
                .set(CREATE_TIME, LocalDateTime.now())
                .set(PIPELINE_VERSION, pipelineVersion)
                .set(TRIGGER_VERSION, triggerVersion)
                .set(SETTING_VERSION, settingVersion)
                .set(DRAFT_FLAG, draftFlag)
                .onDuplicateKeyUpdate()
                .set(MODEL, modelString)
                .set(CREATOR, creator)
                .set(VERSION_NAME, versionName)
                .set(PIPELINE_VERSION, pipelineVersion)
                .set(TRIGGER_VERSION, triggerVersion)
                .set(SETTING_VERSION, settingVersion)
                .execute()
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
                if (includeDraft != true) where.and(DRAFT_FLAG.ne(true))
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
                if (includeDraft != true) where.and(DRAFT_FLAG.ne(true))
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
                creator = record.creator,
                versionName = record.versionName,
                createTime = record.createTime,
                pipelineVersion = record.pipelineVersion,
                triggerVersion = record.triggerVersion,
                settingVersion = record.settingVersion,
                referFlag = record.referFlag,
                referCount = record.referCount,
                draftFlag = record.draftFlag,
                refs = record.refs
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
        limit: Int
    ): List<PipelineVersionSimple> {
        val list = mutableListOf<PipelineVersionSimple>()
        with(T_PIPELINE_RESOURCE_VERSION) {
            val result = dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId)))
                .orderBy(VERSION.desc())
                .limit(limit).offset(offset)
                .fetch()

            result.forEach {
                list.add(
                    PipelineVersionSimple(
                        pipelineId = pipelineId,
                        creator = it.creator ?: "unknown",
                        createTime = it.createTime?.timestampmilli() ?: 0,
                        version = it.version ?: 1,
                        versionName = it.versionName ?: "init",
                        referFlag = it.referFlag,
                        referCount = it.referCount,
                        pipelineVersion = it.pipelineVersion,
                        triggerVersion = it.triggerVersion,
                        settingVersion = it.settingVersion,
                        draftFlag = it.draftFlag,
                        debugBuildId = it.debugBuildId,
                        pacRefs = it.refs
                    )
                )
            }
        }
        return list
    }

    fun count(dslContext: DSLContext, projectId: String, pipelineId: String): Int {
        with(T_PIPELINE_RESOURCE_VERSION) {
            return dslContext.select(DSL.count(PIPELINE_ID))
                .from(this)
                .where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId)))
                .fetchOne(0, Int::class.java)!!
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
}
