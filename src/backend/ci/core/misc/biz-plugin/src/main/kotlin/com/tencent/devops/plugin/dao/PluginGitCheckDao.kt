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

package com.tencent.devops.plugin.dao

import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.model.plugin.tables.TPluginGitCheck
import com.tencent.devops.model.plugin.tables.records.TPluginGitCheckRecord
import com.tencent.devops.plugin.api.pojo.PluginGitCheck
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class PluginGitCheckDao {

    fun getOrNull(
        dslContext: DSLContext,
        pipelineId: String,
        repositoryConfig: RepositoryConfig,
        commitId: String,
        context: String,
        targetBranch: String?
    ): TPluginGitCheckRecord? {
        with(TPluginGitCheck.T_PLUGIN_GIT_CHECK) {
            val step = dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(COMMIT_ID.eq(commitId))
                .and(CONTEXT.eq(context))
            when (repositoryConfig.repositoryType) {
                RepositoryType.ID -> step.and(REPO_ID.eq(repositoryConfig.getRepositoryId()))
                RepositoryType.NAME -> step.and(REPO_NAME.eq(repositoryConfig.getRepositoryId()))
            }
            if (targetBranch.isNullOrEmpty()) {
                step.and(TARGET_BRANCH.isNull)
            } else {
                step.and(TARGET_BRANCH.eq(targetBranch))
            }
            return step.fetchAny()
        }
    }

    fun create(
        dslContext: DSLContext,
        pluginGitCheck: PluginGitCheck
    ) {
        val now = LocalDateTime.now()
        with(pluginGitCheck) {
            with(TPluginGitCheck.T_PLUGIN_GIT_CHECK) {
                dslContext.insertInto(
                    this,
                    PIPELINE_ID,
                    BUILD_NUMBER,
                    REPO_ID,
                    REPO_NAME,
                    COMMIT_ID,
                    CREATE_TIME,
                    UPDATE_TIME,
                    CONTEXT,
                    TARGET_BRANCH
                ).values(
                    pipelineId,
                    buildNumber,
                    repositoryHashId,
                    repositoryName,
                    commitId,
                    now,
                    now,
                    context,
                    targetBranch
                ).execute()
            }
        }
    }

    fun update(dslContext: DSLContext, id: Long, buildNumber: Int) {
        with(TPluginGitCheck.T_PLUGIN_GIT_CHECK) {
            dslContext.update(this)
                .set(UPDATE_TIME, LocalDateTime.now())
                .set(BUILD_NUMBER, buildNumber)
                .where(ID.eq(id))
                .execute()
        }
    }
}
