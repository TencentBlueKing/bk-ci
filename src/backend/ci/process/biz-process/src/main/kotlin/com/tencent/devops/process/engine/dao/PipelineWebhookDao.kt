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

import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.model.process.Tables.T_PIPELINE_WEBHOOK
import com.tencent.devops.model.process.tables.records.TPipelineWebhookRecord
import com.tencent.devops.process.engine.pojo.PipelineWebhook
import org.jooq.DSLContext
import org.jooq.Result
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository

@Repository
class PipelineWebhookDao {

    fun save(dslContext: DSLContext, pipelineWebhook: PipelineWebhook): Int {
        logger.info("save the pipeline webhook=$pipelineWebhook")
        return with(pipelineWebhook) {
            with(T_PIPELINE_WEBHOOK) {
                dslContext.insertInto(
                    this,
                    PROJECT_ID,
                    PIPELINE_ID,
                    REPOSITORY_TYPE,
                    REPO_TYPE,
                    REPO_HASH_ID,
                    REPO_NAME
                )
                    .values(
                        projectId,
                        pipelineId,
                        repositoryType.name,
                        repoType?.name,
                        repoHashId,
                        repoName
                    )
                    .onDuplicateKeyIgnore()
                    .execute()
            }
        }
    }

    fun getPipelineWebHooksByRepositoryType(
        dslContext: DSLContext,
        repositoryType: String,
        offset: Int,
        limit: Int
    ): Result<TPipelineWebhookRecord> {
        return with(T_PIPELINE_WEBHOOK) {
            dslContext.selectFrom(this).where(REPOSITORY_TYPE.eq(repositoryType)).limit(offset, limit).fetch()
        }
    }

    fun delete(dslContext: DSLContext, pipelineId: String): Int {
        return with(T_PIPELINE_WEBHOOK) {
            dslContext.deleteFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .execute()
        }
    }

    fun convert(it: TPipelineWebhookRecord): PipelineWebhook {
        return PipelineWebhook(
            it.projectId,
            it.pipelineId,
            ScmType.valueOf(it.repositoryType),
            convertRepoType(it.repoType),
            it.repoHashId,
            it.repoName
        )
    }

    private fun convertRepoType(repoType: String?): RepositoryType? {
        if (repoType.isNullOrBlank()) {
            return null
        }
        return try {
            RepositoryType.valueOf(repoType!!)
        } catch (e: Exception) {
            logger.warn("Fail to convert the repo type - ($repoType)")
            null
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineWebhookDao::class.java)
    }
}
