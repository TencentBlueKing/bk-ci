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

import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.stream.constant.StreamMessageCode.PROJECT_ALREADY_EXISTS
import com.tencent.devops.stream.constant.StreamMessageCode.PROJECT_NOT_EXIST
import com.tencent.devops.stream.dao.GitPipelineResourceDao
import com.tencent.devops.stream.dao.GitProjectConfDao
import com.tencent.devops.stream.dao.GitRequestEventBuildDao
import com.tencent.devops.stream.dao.GitRequestEventNotBuildDao
import com.tencent.devops.stream.pojo.GitProjectConf
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.ws.rs.core.Response

@Service
class GitProjectConfService @Autowired constructor(
    private val dslContext: DSLContext,
    private val gitProjectConfDao: GitProjectConfDao,
    private val gitPipelineResourceDao: GitPipelineResourceDao,
    private val gitRequestEventBuildDao: GitRequestEventBuildDao,
    private val gitRequestEventNotBuildDao: GitRequestEventNotBuildDao
) {
    companion object {
        private val logger = LoggerFactory.getLogger(GitProjectConfService::class.java)
    }

    fun create(gitProjectId: Long, name: String, url: String, enable: Boolean): Boolean {
        logger.info("GitProjectConfService|Create|id|$gitProjectId|name|$name|url|$url|enable|$enable")
        val record = gitProjectConfDao.get(dslContext, gitProjectId)
        if (null != record) {
            throw CustomException(Response.Status.BAD_REQUEST,
                MessageUtil.getMessageByLocale(
                    messageCode = PROJECT_ALREADY_EXISTS,
                    language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
                ))
        }
        gitProjectConfDao.create(dslContext, gitProjectId, name, url, enable)
        return true
    }

    fun update(gitProjectId: Long, name: String?, url: String?, enable: Boolean?): Boolean {
        logger.info("GitProjectConfService|update|id|$gitProjectId|name|$name|url|$url|enable|$enable")
        gitProjectConfDao.get(dslContext, gitProjectId) ?: throw CustomException(Response.Status.BAD_REQUEST,
            MessageUtil.getMessageByLocale(
                messageCode = PROJECT_NOT_EXIST,
                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
            ))
        gitProjectConfDao.update(dslContext, gitProjectId, name, url, enable)
        return true
    }

    fun delete(gitProjectId: Long): Boolean {
        logger.info("GitProjectConfService|Delete|id|$gitProjectId")
        gitProjectConfDao.delete(dslContext, gitProjectId)
        return true
    }

    fun list(gitProjectId: Long?, name: String?, url: String?, page: Int, pageSize: Int): List<GitProjectConf> {
        return gitProjectConfDao.getList(dslContext, gitProjectId, name, url, page, pageSize).map {
            GitProjectConf(
                gitProjectId = it.id,
                name = it.name,
                url = it.url,
                enable = it.enable,
                createTime = it.createTime.timestamp(),
                updateTime = it.updateTime.timestamp()
            )
        }
    }

    fun count(gitProjectId: Long?, name: String?, url: String?): Int {
        return gitProjectConfDao.count(dslContext, gitProjectId, name, url)
    }

    fun fixPipelineVersion(): Int {
        var count = 0
        val allPipeline = gitPipelineResourceDao.getAllPipeline(dslContext)
        allPipeline.forEach {
            if (!it.latestBuildId.isNullOrBlank()) {
                val build = gitRequestEventBuildDao.getLatestBuild(
                    dslContext = dslContext,
                    gitProjectId = it.gitProjectId,
                    pipelineId = it.pipelineId
                ) ?: return@forEach
                if (build.normalizedYaml.contains("v2.0")) {
                    count += gitPipelineResourceDao.fixPipelineVersion(
                        dslContext = dslContext,
                        pipelineId = it.pipelineId,
                        version = "v2.0"
                    )
                }
            }
            Thread.sleep(100)
        }
        logger.info("GitProjectConfService|fixPipelineVersion|count|$count")
        fixBuildVersion()
        fixNotBuildVersion()
        return count
    }

    fun fixBuildVersion(): Int {
        val limitCount = 5
        var count = 0
        var startId = 0L
        var currBuilds = gitRequestEventBuildDao.getProjectAfterId(
            dslContext = dslContext,
            startId = startId,
            limit = limitCount
        )
        while (currBuilds.isNotEmpty()) {
            currBuilds.forEach {
                if (it.normalizedYaml.contains("v2.0")) {
                    it.version = "v2.0"
                    count++
                }
                startId = it.id
            }
            gitRequestEventBuildDao.batchUpdateBuild(dslContext, currBuilds)
            logger.info(
                "GitProjectConfService|fixBuildVersion" +
                    "|project|${currBuilds.map { it.id }.toList()}|fixed count|$count"
            )
            Thread.sleep(100)
            currBuilds = gitRequestEventBuildDao.getProjectAfterId(dslContext, startId, limitCount)
        }
        logger.info("GitProjectConfService|fixBuildVersion|count|$count")
        return count
    }

    fun fixNotBuildVersion(): Int {
        val limitCount = 10
        var count = 0
        var startId = 22000000L
        var currBuilds = gitRequestEventNotBuildDao.getProjectAfterId(dslContext, startId, limitCount)
        while (currBuilds.isNotEmpty()) {
            currBuilds.forEach {
                if (it.normalizedYaml?.contains("v2.0") == true) {
                    it.version = "v2.0"
                    count++
                }
                startId = it.id
            }
            gitRequestEventNotBuildDao.batchUpdateBuild(dslContext, currBuilds)
            logger.info(
                "GitProjectConfService|fixNotBuildVersion" +
                    "|project|${currBuilds.map { it.id }.toList()}|count|$count"
            )
            Thread.sleep(100)
            currBuilds = gitRequestEventNotBuildDao.getProjectAfterId(dslContext, startId, limitCount)
        }
        logger.info("GitProjectConfService|fixNotBuildVersion|finished count|$count")
        return count
    }
}
