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

package com.tencent.devops.gitci.dao

import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.gitci.pojo.EnvironmentVariables
import com.tencent.devops.gitci.pojo.GitRepositoryConf
import com.tencent.devops.model.gitci.tables.TRepositoryConf
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class GitCISettingDao {

    fun saveSetting(
        dslContext: DSLContext,
        conf: GitRepositoryConf,
        projectCode: String
    ) {
        with(TRepositoryConf.T_REPOSITORY_CONF) {
            dslContext.transaction { configuration ->
                val context = DSL.using(configuration)
                val record = context.selectFrom(this)
                        .where(ID.eq(conf.gitProjectId))
                        .fetchOne()
                val now = LocalDateTime.now()
                if (record == null) {
                    context.insertInto(this,
                        ID,
                        NAME,
                        URL,
                        HOME_PAGE,
                        GIT_HTTP_URL,
                        GIT_SSH_URL,
                        ENABLE_CI,
                        BUILD_PUSHED_BRANCHES,
                        LIMIT_CONCURRENT_JOBS,
                        BUILD_PUSHED_PULL_REQUEST,
                        AUTO_CANCEL_BRANCH_BUILDS,
                        AUTO_CANCEL_PULL_REQUEST_BUILDS,
                        ENV,
                        CREATE_TIME,
                        UPDATE_TIME,
                        PROJECT_CODE
                        )
                        .values(
                            conf.gitProjectId,
                            conf.name,
                            conf.url,
                            conf.homepage,
                            conf.gitHttpUrl,
                            conf.gitSshUrl,
                            conf.enableCi,
                            conf.buildPushedBranches,
                            conf.limitConcurrentJobs,
                            conf.buildPushedPullRequest,
                            conf.autoCancelBranchBuilds,
                            conf.autoCancelPullRequestBuilds,
                            if (conf.env == null) { "" } else { JsonUtil.toJson(conf.env!!) },
                            LocalDateTime.now(),
                            LocalDateTime.now(),
                            projectCode
                        ).execute()
                } else {
                    context.update(this)
                        .set(ENABLE_CI, conf.enableCi)
                            .set(BUILD_PUSHED_BRANCHES, conf.buildPushedBranches)
                            .set(LIMIT_CONCURRENT_JOBS, conf.limitConcurrentJobs)
                            .set(BUILD_PUSHED_PULL_REQUEST, conf.buildPushedPullRequest)
                            .set(AUTO_CANCEL_BRANCH_BUILDS, conf.autoCancelBranchBuilds)
                            .set(AUTO_CANCEL_PULL_REQUEST_BUILDS, conf.autoCancelPullRequestBuilds)
                            .set(ENV, if (conf.env == null) { "" } else { JsonUtil.toJson(conf.env!!) })
                            .set(UPDATE_TIME, now)
                            .set(PROJECT_CODE, projectCode)
                            .where(ID.eq(conf.gitProjectId))
                            .execute()
                }
            }
        }
    }

    fun updateSetting(
        dslContext: DSLContext,
        gitProjectId: Long,
        gitProjectName: String,
        url: String,
        sshUrl: String,
        httpUrl: String,
        homePage: String
    ) {
        with(TRepositoryConf.T_REPOSITORY_CONF) {
            dslContext.transaction { configuration ->
                DSL.using(configuration).update(this)
                    .set(NAME, gitProjectName)
                    .set(URL, url)
                    .set(HOME_PAGE, homePage)
                    .set(GIT_HTTP_URL, httpUrl)
                    .set(GIT_SSH_URL, sshUrl)
                    .set(UPDATE_TIME, LocalDateTime.now())
                    .where(ID.eq(gitProjectId))
                    .execute()
            }
        }
    }

    fun getSetting(dslContext: DSLContext, gitProjectId: Long): GitRepositoryConf? {
        with(TRepositoryConf.T_REPOSITORY_CONF) {
            val conf = dslContext.selectFrom(this)
                .where(ID.eq(gitProjectId))
                .fetchOne()
            if (conf == null) {
                return null
            } else {
                return GitRepositoryConf(
                    gitProjectId = conf.id,
                    name = conf.name,
                    url = conf.url,
                    homepage = conf.homePage,
                    gitHttpUrl = conf.gitHttpUrl,
                    gitSshUrl = conf.gitSshUrl,
                    enableCi = conf.enableCi,
                    buildPushedBranches = conf.buildPushedBranches,
                    limitConcurrentJobs = conf.limitConcurrentJobs,
                    buildPushedPullRequest = conf.buildPushedPullRequest,
                    autoCancelBranchBuilds = conf.autoCancelBranchBuilds,
                    autoCancelPullRequestBuilds = conf.autoCancelPullRequestBuilds,
                    env = if (conf.env.isNullOrBlank()) {
                        null
                    } else {
                        JsonUtil.getObjectMapper().readValue(conf.env) as List<EnvironmentVariables>
                    },
                    createTime = conf.createTime.timestampmilli(),
                    updateTime = conf.updateTime.timestampmilli(),
                    projectCode = conf.projectCode
                        )
            }
        }
    }

    /**
     * 启用或关闭CI项目
     */
    fun enableGitCI(dslContext: DSLContext, gitId: Long, enable: Boolean) {
        with(TRepositoryConf.T_REPOSITORY_CONF) {
            dslContext.update(this)
                .set(ENABLE_CI, enable)
                .where(ID.eq(gitId))
                .execute()
        }
    }
}
