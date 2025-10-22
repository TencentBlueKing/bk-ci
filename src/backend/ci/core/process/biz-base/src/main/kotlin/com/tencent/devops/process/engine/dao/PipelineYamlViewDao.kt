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
 *
 */

package com.tencent.devops.process.engine.dao

import com.tencent.devops.model.process.tables.TPipelineYamlView
import com.tencent.devops.model.process.tables.records.TPipelineYamlViewRecord
import com.tencent.devops.process.pojo.pipeline.PipelineYamlView
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class PipelineYamlViewDao {

    fun save(
        dslContext: DSLContext,
        projectId: String,
        repoHashId: String,
        directory: String,
        viewId: Long
    ) {
        with(TPipelineYamlView.T_PIPELINE_YAML_VIEW) {
            dslContext.insertInto(
                this,
                PROJECT_ID,
                REPO_HASH_ID,
                DIRECTORY,
                VIEW_ID
            ).values(
                projectId,
                repoHashId,
                directory,
                viewId
            ).execute()
        }
    }

    fun get(
        dslContext: DSLContext,
        projectId: String,
        repoHashId: String,
        directory: String
    ): PipelineYamlView? {
        with(TPipelineYamlView.T_PIPELINE_YAML_VIEW) {
            val record = dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(REPO_HASH_ID.eq(repoHashId))
                .and(DIRECTORY.eq(directory))
                .fetchOne()
            return record?.let { convert(it) }
        }
    }

    fun getByViewId(
        dslContext: DSLContext,
        projectId: String,
        viewId: Long
    ): PipelineYamlView? {
        with(TPipelineYamlView.T_PIPELINE_YAML_VIEW) {
            val record = dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(VIEW_ID.eq(viewId))
                .fetchOne()
            return record?.let { convert(it) }
        }
    }

    fun listViewIds(
        dslContext: DSLContext,
        projectId: String
    ): List<Long> {
        with(TPipelineYamlView.T_PIPELINE_YAML_VIEW) {
            return dslContext.select(VIEW_ID).from(this)
                .where(PROJECT_ID.eq(projectId))
                .fetch(0, Long::class.java)
        }
    }

    fun listRepoYamlView(
        dslContext: DSLContext,
        projectId: String,
        repoHashId: String
    ): List<PipelineYamlView> {
        with(TPipelineYamlView.T_PIPELINE_YAML_VIEW) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(REPO_HASH_ID.eq(repoHashId))
                .fetch().map { convert(it) }
        }
    }

    fun delete(
        dslContext: DSLContext,
        projectId: String,
        repoHashId: String,
        directory: String
    ) {
        with(TPipelineYamlView.T_PIPELINE_YAML_VIEW) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(REPO_HASH_ID.eq(repoHashId))
                .and(DIRECTORY.eq(directory))
                .execute()
        }
    }

    fun convert(record: TPipelineYamlViewRecord): PipelineYamlView {
        return with(record) {
            PipelineYamlView(
                projectId = projectId,
                repoHashId = repoHashId,
                directory = directory,
                viewId = viewId
            )
        }
    }
}
