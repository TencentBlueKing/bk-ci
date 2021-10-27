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
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.model.process.Tables.T_PIPELINE_RESOURCE
import com.tencent.devops.process.pojo.setting.PipelineModelVersion
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record3
import org.jooq.Result
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Suppress("TooManyFunctions", "LongParameterList")
@Repository
class PipelineResDao {

    fun create(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        creator: String,
        version: Int,
        model: Model
    ) {
        logger.info("Create the pipeline model pipelineId=$pipelineId, version=$version")
        with(T_PIPELINE_RESOURCE) {
            val modelString = JsonUtil.toJson(model, formatted = false)
            dslContext.insertInto(
                this,
                PROJECT_ID,
                PIPELINE_ID,
                VERSION,
                MODEL,
                CREATOR,
                CREATE_TIME
            )
                .values(projectId, pipelineId, version, modelString, creator, LocalDateTime.now())
                .onDuplicateKeyUpdate()
                .set(MODEL, modelString)
                .set(CREATOR, creator)
                .set(CREATE_TIME, LocalDateTime.now())
                .execute()
        }
    }

    fun getLatestVersionModelString(dslContext: DSLContext, pipelineId: String) =
        getVersionModelString(dslContext, pipelineId, null)

    fun getVersionModelString(
        dslContext: DSLContext,
        pipelineId: String,
        version: Int?
    ): String? {

        return with(T_PIPELINE_RESOURCE) {
            val where = dslContext.select(MODEL)
                .from(this)
                .where(PIPELINE_ID.eq(pipelineId))
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
        pipelineIds: Set<String>
    ): Result<Record3<String, Int, String>>? {
        val tpr = T_PIPELINE_RESOURCE.`as`("tpr")
        val t = dslContext.select(
            tpr.PIPELINE_ID.`as`("PIPELINE_ID"),
            DSL.max(tpr.VERSION).`as`("VERSION")
        ).from(tpr)
            .where(tpr.PIPELINE_ID.`in`(pipelineIds))
            .groupBy(tpr.PIPELINE_ID)
        return dslContext.select(tpr.PIPELINE_ID, tpr.VERSION, tpr.MODEL).from(tpr)
            .join(t)
            .on(
                tpr.PIPELINE_ID.eq(t.field("PIPELINE_ID", String::class.java))
                    .and(tpr.VERSION.eq(t.field("VERSION", Int::class.java)))
            )
            .fetch()
    }

    fun deleteAllVersion(dslContext: DSLContext, pipelineId: String): Int {
        return with(T_PIPELINE_RESOURCE) {
            dslContext.deleteFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .execute()
        }
    }

    fun deleteEarlyVersion(dslContext: DSLContext, pipelineId: String, beforeVersion: Int): Int {
        return with(T_PIPELINE_RESOURCE) {
            dslContext.deleteFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(VERSION.lt(beforeVersion))
                .execute()
        }
    }

    fun updatePipelineModel(
        dslContext: DSLContext,
        userId: String,
        pipelineModelVersionList: List<PipelineModelVersion>
    ) {
        with(T_PIPELINE_RESOURCE) {
            val updateStep = pipelineModelVersionList.map {
                val conditions = mutableListOf<Condition>()
                conditions.add(PIPELINE_ID.eq(it.pipelineId))
                val version = it.version
                if (version != null) {
                    conditions.add(VERSION.eq(version))
                }
                dslContext.update(this)
                    .set(MODEL, it.model)
                    .set(CREATOR, userId)
                    .where(conditions)
            }
            dslContext.batch(updateStep).execute()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineResDao::class.java)
    }
}
