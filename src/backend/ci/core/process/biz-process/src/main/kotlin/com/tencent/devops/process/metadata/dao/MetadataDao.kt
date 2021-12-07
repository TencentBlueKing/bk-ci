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

package com.tencent.devops.process.metadata.dao

import com.tencent.devops.model.process.tables.TMetadata
import com.tencent.devops.model.process.tables.records.TMetadataRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Suppress("ALL")
@Repository
class MetadataDao {
    fun create(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        metaDataId: String,
        metaDataValue: String,
        id: Long? = null
    ): Long {
        val now = LocalDateTime.now()
        with(TMetadata.T_METADATA) {
            val record = dslContext.insertInto(
                this,
                PROJECT_ID,
                PIPELINE_ID,
                BUILD_ID,
                META_DATA_ID,
                META_DATA_VALUE,
                CREATE_TIME,
                ID
            ).values(
                projectId,
                pipelineId,
                buildId,
                metaDataId,
                metaDataValue,
                now,
                id
            )
                .returning(ID)
                .fetchOne()
            return record!!.id
        }
    }

    fun batchCreate(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        metaPairList: List<Triple<String, String, Long?>>
    ) {
        val now = LocalDateTime.now()
        with(TMetadata.T_METADATA) {
            metaPairList.map {
                val record = TMetadataRecord(it.third, projectId, pipelineId, buildId, it.first, it.second, now)
                dslContext.executeInsert(record)
            }
        }
    }

    fun list(dslContext: DSLContext, projectId: String, buildId: String): Result<TMetadataRecord> {
        with(TMetadata.T_METADATA) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(BUILD_ID.eq(buildId))
                .fetch()
        }
    }
}
