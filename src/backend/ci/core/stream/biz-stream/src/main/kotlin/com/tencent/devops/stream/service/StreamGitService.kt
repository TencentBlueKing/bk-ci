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

package com.tencent.devops.stream.service

import com.tencent.devops.stream.dao.StreamBasicSettingDao
import com.tencent.devops.stream.pojo.StreamGitProjectInfoWithProject
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class StreamGitService @Autowired constructor(
    private val dslContext: DSLContext,
    private val streamBasicSettingDao: StreamBasicSettingDao,
    private val streamGitTransferService: StreamGitTransferService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(StreamGitService::class.java)
    }

    fun getProjectInfo(gitProjectId: String): StreamGitProjectInfoWithProject? {
        logger.info("StreamGitService|getProjectInfo|get from DB|gitProjectId=$gitProjectId")
        val setting = try {
            streamBasicSettingDao.getSetting(dslContext, gitProjectId.toLong())
        } catch (e: NumberFormatException) {
            streamBasicSettingDao.getSettingByPathWithNameSpace(dslContext, gitProjectId)
        } ?: return null

        return try {
            streamGitTransferService.getGitProjectInfo(
                gitProjectId = gitProjectId,
                userId = setting.enableUserId
            )
        } catch (e: Exception) {
            logger.info("StreamGitService|getProjectInfo|stream scm service is unavailable|gitProjectId=$gitProjectId")
            StreamGitProjectInfoWithProject(
                gitProjectId = setting.gitProjectId,
                name = setting.name,
                homepage = setting.homepage,
                gitHttpUrl = setting.gitHttpUrl.replace("https", "http"),
                gitHttpsUrl = setting.gitHttpUrl,
                gitSshUrl = setting.gitSshUrl,
                nameWithNamespace = setting.nameWithNamespace,
                pathWithNamespace = setting.pathWithNamespace,
                defaultBranch = "master",
                description = setting.gitProjectDesc,
                avatarUrl = setting.gitProjectAvatar,
                routerTag = null
            )
        }
    }
}
