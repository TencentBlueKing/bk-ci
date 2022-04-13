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

package com.tencent.devops.stream.dao

import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.stream.pojo.EmailProperty
import com.tencent.devops.stream.pojo.EnvironmentVariables
import com.tencent.devops.stream.pojo.GitRepositoryConf
import com.tencent.devops.stream.pojo.RtxCustomProperty
import com.tencent.devops.stream.pojo.RtxGroupProperty
import com.tencent.devops.model.stream.tables.TRepositoryConf
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Suppress("ALL")
@Repository
class GitCISettingDao {

    fun getSetting(dslContext: DSLContext, gitProjectId: Long): GitRepositoryConf? {
        with(TRepositoryConf.T_REPOSITORY_CONF) {
            val conf = dslContext.selectFrom(this)
                .where(ID.eq(gitProjectId))
                .fetchOne()
            if (conf == null) {
                return null
            } else {
                val rtxCustomProperty = try {
                    if (!conf.rtxCustomProperty.isNullOrBlank()) {
                        JsonUtil.getObjectMapper().readValue(conf.rtxCustomProperty) as RtxCustomProperty
                    } else null
                } catch (e: Exception) {
                    null
                }

                val emailProperty = try {
                    if (!conf.emailProperty.isNullOrBlank()) {
                        JsonUtil.getObjectMapper().readValue(conf.emailProperty) as EmailProperty
                    } else null
                } catch (e: Exception) {
                    null
                }

                val rtxGroupProperty = try {
                    if (!conf.rtxGroupProperty.isNullOrBlank()) {
                        JsonUtil.getObjectMapper().readValue(conf.rtxGroupProperty) as RtxGroupProperty
                    } else null
                } catch (e: Exception) {
                    null
                }

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
                    projectCode = conf.projectCode,
                    rtxCustomProperty = rtxCustomProperty,
                    emailProperty = emailProperty,
                    rtxGroupProperty = rtxGroupProperty,
                    onlyFailedNotify = conf.onlyFailedNotify,
                    enableMrBlock = conf.enableMrBlock
                )
            }
        }
    }
}
