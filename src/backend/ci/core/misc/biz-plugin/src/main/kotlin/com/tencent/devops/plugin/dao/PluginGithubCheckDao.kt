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

package com.tencent.devops.plugin.dao

import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.model.plugin.tables.TPluginGithubCheck
import com.tencent.devops.model.plugin.tables.records.TPluginGithubCheckRecord
import com.tencent.devops.plugin.api.pojo.GithubCheckRun
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class PluginGithubCheckDao {

    fun getOrNull(
        dslContext: DSLContext,
        pipelineId: String,
        repositoryConfig: RepositoryConfig,
        commitId: String
    ): TPluginGithubCheckRecord? {
        with(TPluginGithubCheck.T_PLUGIN_GITHUB_CHECK) {
            val step = dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(COMMIT_ID.eq(commitId))
            when (repositoryConfig.repositoryType) {
                RepositoryType.ID -> step.and(REPO_ID.eq(repositoryConfig.getRepositoryId()))
                RepositoryType.NAME -> step.and(REPO_NAME.eq(repositoryConfig.getRepositoryId()))
            }

            return step.fetchOne()
        }
    }

    fun create(
        dslContext: DSLContext,
        checkRun: GithubCheckRun
    ) {
        val now = LocalDateTime.now()
        with(checkRun) {
            with(TPluginGithubCheck.T_PLUGIN_GITHUB_CHECK) {
                dslContext.insertInto(
                    this,
                    PIPELINE_ID,
                    BUILD_NUMBER,
                    REPO_ID,
                    REPO_NAME,
                    COMMIT_ID,
                    CHECK_RUN_ID,
                    CREATE_TIME,
                    UPDATE_TIME,
                    CHECK_RUN_NAME
                ).values(
                    pipelineId,
                    buildNumber,
                    repositoryConfig.repositoryHashId,
                    repositoryConfig.repositoryName,
                    commitId,
                    checkRunId,
                    now,
                    now,
                    checkRunName
                ).execute()
            }
        }
    }

    fun update(dslContext: DSLContext, id: Long, buildNumber: Int, checkRunId: Long) {
        with(TPluginGithubCheck.T_PLUGIN_GITHUB_CHECK) {
            dslContext.update(this)
                .set(BUILD_NUMBER, buildNumber)
                .set(CHECK_RUN_ID, checkRunId)
                .where(ID.eq(id))
                .execute()
        }
    }
}
