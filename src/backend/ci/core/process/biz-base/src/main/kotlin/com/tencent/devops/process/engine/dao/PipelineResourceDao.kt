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
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.model.process.Tables.T_PIPELINE_RESOURCE
import com.tencent.devops.model.process.tables.records.TPipelineResourceRecord
import com.tencent.devops.process.pojo.pipeline.PipelineResourceVersion
import com.tencent.devops.process.utils.PipelineVersionUtils
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record3
import org.jooq.RecordMapper
import org.jooq.Result
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Suppress("TooManyFunctions", "LongParameterList", "ReturnCount")
@Repository
class PipelineResourceDao {

    fun create(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        creator: String,
        version: Int,
        versionName: String?,
        model: Model,
        yamlStr: String?,
        yamlVersion: String?,
        versionNum: Int?,
        pipelineVersion: Int?,
        triggerVersion: Int?,
        settingVersion: Int?
    ) {
        logger.info("Create the pipeline model pipelineId=$pipelineId, version=$version")
        with(T_PIPELINE_RESOURCE) {
            val modelString = JsonUtil.toJson(model, formatted = false)
            dslContext.insertInto(this)
                .set(PROJECT_ID, projectId)
                .set(PIPELINE_ID, pipelineId)
                .set(VERSION, version)
                .set(VERSION_NUM, versionNum)
                .set(VERSION_NAME, versionName)
                .set(MODEL, modelString)
                .set(YAML, yamlStr)
                .set(YAML_VERSION, yamlVersion)
                .set(CREATOR, creator)
                .set(CREATE_TIME, LocalDateTime.now())
                .set(PIPELINE_VERSION, pipelineVersion)
                .set(TRIGGER_VERSION, triggerVersion)
                .set(SETTING_VERSION, settingVersion)
                .onDuplicateKeyUpdate()
                .set(MODEL, modelString)
                .set(YAML, yamlStr)
                .set(YAML_VERSION, yamlVersion)
                .set(CREATOR, creator)
                .set(VERSION_NUM, versionNum)
                .set(VERSION_NAME, versionName)
                .set(PIPELINE_VERSION, pipelineVersion)
                .set(TRIGGER_VERSION, triggerVersion)
                .set(SETTING_VERSION, settingVersion)
                .execute()
        }
    }

    fun updateReleaseVersion(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        version: Int,
        versionName: String?,
        model: Model,
        yamlStr: String?,
        yamlVersion: String?,
        versionNum: Int?,
        pipelineVersion: Int?,
        triggerVersion: Int?,
        settingVersion: Int?
    ) {
        logger.info("Update the pipeline model pipelineId=$pipelineId, version=$version")
        with(T_PIPELINE_RESOURCE) {
            dslContext.update(this)
                .set(VERSION, version)
                .set(VERSION_NUM, versionNum)
                .set(VERSION_NAME, versionName)
                .set(MODEL, JsonUtil.toJson(model, formatted = false))
                .set(YAML, yamlStr)
                .set(YAML_VERSION, yamlVersion)
                .set(PIPELINE_VERSION, pipelineVersion)
                .set(TRIGGER_VERSION, triggerVersion)
                .set(SETTING_VERSION, settingVersion)
                .where(PROJECT_ID.eq(projectId)).and(PIPELINE_ID.eq(pipelineId))
                .execute()
        }
    }

    fun getReleaseVersionResource(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String
    ): PipelineResourceVersion? {
        with(T_PIPELINE_RESOURCE) {
            return dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId)))
                .orderBy(VERSION.desc()).limit(1)
                .fetchAny(mapper)
        }
    }

    fun getLatestVersionModelString(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String
    ) = getVersionModelString(
        dslContext = dslContext,
        projectId = projectId,
        pipelineId = pipelineId,
        version = null
    )

    fun getVersionModelString(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        version: Int?
    ): String? {

        return with(T_PIPELINE_RESOURCE) {
            val where = dslContext.select(MODEL)
                .from(this)
                .where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId)))
            if (version != null) {
                where.and(VERSION.eq(version))
            } else {
                where.orderBy(VERSION.desc()).limit(1)
            }
            where.fetchAny(0, String::class.java)
        } // if (record != null) objectMapper.readValue(record) else null
    }

    fun listLatestModelResource(
        dslContext: DSLContext,
        pipelineIds: Set<String>,
        projectId: String? = null
    ): Result<Record3<String, Int, String>>? {
        val tpr = T_PIPELINE_RESOURCE.`as`("tpr")
        val conditions = mutableListOf<Condition>()
        conditions.add(tpr.PIPELINE_ID.`in`(pipelineIds))
        if (projectId != null) {
            conditions.add(tpr.PROJECT_ID.eq(projectId))
        }
        val t = dslContext.select(
            tpr.PIPELINE_ID.`as`("PIPELINE_ID"),
            DSL.max(tpr.VERSION).`as`("VERSION")
        ).from(tpr)
            .where(conditions)
            .groupBy(tpr.PIPELINE_ID)
        return dslContext.select(tpr.PIPELINE_ID, tpr.VERSION, tpr.MODEL).from(tpr)
            .join(t)
            .on(
                tpr.PIPELINE_ID.eq(t.field("PIPELINE_ID", String::class.java))
                    .and(tpr.VERSION.eq(t.field("VERSION", Int::class.java)))
            )
            .fetch()
    }

    fun deleteAllVersion(dslContext: DSLContext, projectId: String, pipelineId: String): Int {
        return with(T_PIPELINE_RESOURCE) {
            dslContext.deleteFrom(this)
                .where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId)))
                .execute()
        }
    }

    /**
     * 获取最新的modelString
     *
     * @return Map<PIPELINE_ID, MODEL>
     */
    fun listModelString(
        dslContext: DSLContext,
        projectId: String,
        pipelineIds: Collection<String>
    ): Map<String, String> {
        with(T_PIPELINE_RESOURCE) {
            val record3s = dslContext.select(PIPELINE_ID, MODEL, VERSION)
                .from(this)
                .where(PIPELINE_ID.`in`(pipelineIds).and(PROJECT_ID.eq(projectId)))
                .fetch()
            if (record3s.isEmpty()) {
                return emptyMap()
            }
            val result = mutableMapOf<String, String>()
            val maxVersionMap = mutableMapOf<String, Int>()
            record3s.forEach {
                val maxVersion = maxVersionMap[it.get(PIPELINE_ID)]
                if (maxVersion == null || maxVersion < it.get(VERSION)) {
                    maxVersionMap[it.get(PIPELINE_ID)] = it.get(VERSION)
                    result[it.get(PIPELINE_ID)] = it.get(MODEL)
                }
            }
            return result
        }
    }

    class PipelineResourceVersionJooqMapper : RecordMapper<TPipelineResourceRecord, PipelineResourceVersion> {
        override fun map(record: TPipelineResourceRecord?): PipelineResourceVersion? {
            return record?.let {
                val versionNum = (record.versionNum ?: record.version ?: 1)
                // 如果名称已经为init的则尝试计算新版本名，如果获取不到新的版本名称则保持init
                val versionName = record.versionName?.takeIf {
                    name -> name != "init"
                } ?: PipelineVersionUtils.getVersionName(
                    versionNum, record.version, record.triggerVersion, record.settingVersion
                ) ?: "V$versionNum(${record.versionName ?: "init"})"
                PipelineResourceVersion(
                    projectId = record.projectId,
                    pipelineId = record.pipelineId,
                    version = record.version,
                    status = VersionStatus.RELEASED,
                    model = record.model?.let { str ->
                        try {
                            JsonUtil.to(str, Model::class.java)
                        } catch (ignore: Exception) {
                            null
                        }
                    } ?: return null,
                    yaml = record.yaml,
                    yamlVersion = record.yamlVersion,
                    creator = record.creator ?: "unknown",
                    updater = null,
                    versionName = versionName,
                    createTime = record.createTime,
                    updateTime = null,
                    versionNum = record.versionNum,
                    pipelineVersion = record.pipelineVersion,
                    triggerVersion = record.triggerVersion,
                    settingVersion = record.settingVersion
                )
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineResourceDao::class.java)
        private val mapper = PipelineResourceVersionJooqMapper()
    }
}
