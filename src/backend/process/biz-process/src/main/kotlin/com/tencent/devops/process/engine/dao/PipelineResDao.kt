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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.engine.dao

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.model.process.Tables.T_PIPELINE_RESOURCE
import com.tencent.devops.model.process.tables.records.TPipelineResourceRecord
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository

@Repository
class PipelineResDao @Autowired constructor(private val objectMapper: ObjectMapper) {

    fun create(
        dslContext: DSLContext,
        pipelineId: String,
        version: Int,
        model: Model
    ) {
        logger.info("Create the pipeline model pipelineId=$pipelineId, version=$version")
        with(T_PIPELINE_RESOURCE) {
            val modelString = objectMapper.writeValueAsString(model)
            dslContext.insertInto(
                this,
                PIPELINE_ID,
                VERSION,
                MODEL
            )
                .values(pipelineId, version, modelString)
                .onDuplicateKeyUpdate()
                .set(MODEL, modelString)
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

    fun listModelResource(
        dslContext: DSLContext,
        pipelineIds: Set<String>
    ): List<TPipelineResourceRecord> {
        return with(T_PIPELINE_RESOURCE) {
            dslContext.selectFrom(this)
                .where(PIPELINE_ID.`in`(pipelineIds))
                .fetch()
        }
    }

    fun getAllVersionModel(
        dslContext: DSLContext,
        pipelineId: String
    ): List<String> {

        return with(T_PIPELINE_RESOURCE) {
            dslContext.select(MODEL)
                .from(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .orderBy(VERSION.desc())
                .fetch(0, String::class.java)
        }
    }

    fun deleteAllVersion(dslContext: DSLContext, pipelineId: String): Int {
        return with(T_PIPELINE_RESOURCE) {
            dslContext.deleteFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .execute()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineResDao::class.java)
    }
}
