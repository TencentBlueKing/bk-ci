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

import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.model.plugin.tables.TPluginGithubStat
import com.tencent.devops.plugin.api.pojo.GithubStatRequest
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class GithubStatDao {

    /**
     * 不存在则新增，否则更新
     */
    fun createOrUpdate(
        dslContext: DSLContext,
        owner: String,
        repo: String,
        githubStatRequest: GithubStatRequest
    ) {
        with(TPluginGithubStat.T_PLUGIN_GITHUB_STAT) {
            dslContext.insertInto(this,
                    ID,
                    OWNER,
                    REPO,
                    STAT_DATE,
                    ISSUE_CNT_ALL,
                    ISSUE_CNT_OPEN,
                    ISSUE_CNT_CLOSED,
                    PR_CNT_ALL,
                    PR_CNT_OPEN,
                    PR_CNT_CLOSED,
                    COMMITS_CNT
            ).values(
                    UUIDUtil.generate(),
                    owner,
                    repo,
                    githubStatRequest.statDate,
                    githubStatRequest.issueCntAll,
                    githubStatRequest.issueCntOpen,
                    githubStatRequest.issueCntClosed,
                    githubStatRequest.prCntAll,
                    githubStatRequest.prCntOpen,
                    githubStatRequest.prCntClosed,
                    githubStatRequest.commitsCnt
            )
                    .onDuplicateKeyUpdate()
                    .set(ISSUE_CNT_ALL, githubStatRequest.issueCntAll)
                    .set(ISSUE_CNT_OPEN, githubStatRequest.issueCntOpen)
                    .set(ISSUE_CNT_CLOSED, githubStatRequest.issueCntClosed)
                    .set(PR_CNT_ALL, githubStatRequest.prCntAll)
                    .set(PR_CNT_OPEN, githubStatRequest.prCntOpen)
                    .set(PR_CNT_CLOSED, githubStatRequest.prCntClosed)
                    .set(COMMITS_CNT, githubStatRequest.commitsCnt)
                    .set(UPDATE_TIME, java.time.LocalDateTime.now())
                    .execute()
        }
    }
}
