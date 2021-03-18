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

package com.tencent.devops.plugin.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.plugin.api.pojo.GitGroupStatRequest
import com.tencent.devops.plugin.dao.TgitGroupStatDao
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TgitGroupStatService @Autowired constructor(
    private val tgitGroupStatDao: TgitGroupStatDao,
    private val dslContext: DSLContext
) {
    companion object {
        private val logger = LoggerFactory.getLogger(TgitGroupStatService::class.java)
    }

    /**
     * 上报git项目组统计数据
     */
    fun reportGitGroupStat(
        group: String,
        gitGroupStatRequest: GitGroupStatRequest
    ): Result<Boolean> {
        logger.info("gitGroupStatRequest: $gitGroupStatRequest")
        val latestRecord = tgitGroupStatDao.getLatestRecord(dslContext, group, gitGroupStatRequest.statDate)
        logger.info("latestRecord: $latestRecord")
        if (latestRecord != null) {
            val latestCommitCount = latestRecord["COMMIT_COUNT"] as Int
            val latestCommitCountOpen = latestRecord["COMMIT_COUNT_OPEN"] as Int
            val latestProjectCount = latestRecord["PROJECT_COUNT"] as Int
            val latestProjectCountOpen = latestRecord["PROJECT_COUNT_OPEN"] as Int
            val latestUserCount = latestRecord["USER_COUNT"] as Int
            val latestUserCountOpen = latestRecord["USER_COUNT_OPEN"] as Int
            gitGroupStatRequest.commitIncre = gitGroupStatRequest.commitCount - latestCommitCount
            gitGroupStatRequest.commitIncreOpen = gitGroupStatRequest.commitCountOpen - latestCommitCountOpen
            gitGroupStatRequest.projectIncre = gitGroupStatRequest.projectCount - latestProjectCount
            gitGroupStatRequest.projectIncreOpen = gitGroupStatRequest.projectCountOpen - latestProjectCountOpen
            gitGroupStatRequest.userIncre = gitGroupStatRequest.userCount - latestUserCount
            gitGroupStatRequest.userIncreOpen = gitGroupStatRequest.userCountOpen - latestUserCountOpen
        } else {
            gitGroupStatRequest.commitIncre = 0
            gitGroupStatRequest.commitIncreOpen = 0
            gitGroupStatRequest.projectIncre = 0
            gitGroupStatRequest.projectIncreOpen = 0
            gitGroupStatRequest.userIncre = 0
            gitGroupStatRequest.userIncreOpen = 0
        }

        logger.info("gitGroupStat: $gitGroupStatRequest")
        tgitGroupStatDao.createOrUpdate(dslContext = dslContext, group = group, gitGroupStatRequest = gitGroupStatRequest)

        return Result(true)
    }
}
