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
 *
 */

package com.tencent.devops.process.engine.dao

import com.tencent.devops.model.process.tables.TPipelineYamlRefer
import com.tencent.devops.model.process.tables.records.TPipelineYamlReferRecord
import com.tencent.devops.process.pojo.pipeline.PipelineYamlRefer
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * 流水线与代码库yml文件关联表
 */
@Repository
class PipelineYamlReferDao {

    fun save(
        dslContext: DSLContext,
        projectId: String,
        repoHashId: String,
        filePath: String,
        pipelineId: String
    ) {
        val now = LocalDateTime.now()
        with(TPipelineYamlRefer.T_PIPELINE_YAML_REFER) {
            dslContext.insertInto(
                this,
                PROJECT_ID,
                REPO_HASH_ID,
                FILE_PATH,
                PIPELINE_ID,
                CREATE_TIME,
                UPDATE_TIME
            ).values(
                projectId,
                repoHashId,
                filePath,
                pipelineId,
                now,
                now
            )
        }
    }

    fun get(
        dslContext: DSLContext,
        projectId: String,
        repoHashId: String,
        filePath: String
    ): PipelineYamlRefer? {
        with(TPipelineYamlRefer.T_PIPELINE_YAML_REFER) {
            val record = dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(REPO_HASH_ID.eq(repoHashId))
                .and(FILE_PATH.eq(filePath))
                .fetchOne()
            return record?.let { convert(it) }
        }
    }

    fun convert(record: TPipelineYamlReferRecord): PipelineYamlRefer {
        return with(record) {
            PipelineYamlRefer(
                projectId = projectId,
                repoHashId = repoHashId,
                filePath = filePath,
                pipelineId = pipelineId
            )
        }
    }
}
