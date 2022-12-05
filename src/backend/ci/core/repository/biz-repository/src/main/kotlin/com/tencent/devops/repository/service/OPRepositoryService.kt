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

package com.tencent.devops.repository.service

import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.repository.dao.RepositoryDao
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

@Service
class OPRepositoryService @Autowired constructor(
    private val repositoryDao: RepositoryDao,
    private val dslContext: DSLContext
) {
    fun addHashId() {
        val startTime = System.currentTimeMillis()
        logger.info("OPRepositoryService:begin addHashId-----------")
        val threadPoolExecutor = ThreadPoolExecutor(
            1,
            1,
            0,
            TimeUnit.SECONDS,
            LinkedBlockingQueue(1),
            Executors.defaultThreadFactory(),
            ThreadPoolExecutor.AbortPolicy()
        )
        threadPoolExecutor.submit {
            logger.info("OPRepositoryService:begin addHashId threadPoolExecutor-----------")
            var offset = 0
            val limit = 1000
            try {
                do {
                    val repoRecords = repositoryDao.getAllRepo(dslContext, limit, offset)
                    val repoSize = repoRecords?.size
                    logger.info("repoSize:$repoSize")
                    repoRecords?.map {
                        val id = it.value1()
                        val hashId = HashUtil.encodeOtherLongId(it.value1())
                        repositoryDao.updateHashId(dslContext, id, hashId)
                    }
                    offset += limit
                } while (repoSize == 1000)
            } catch (e: Exception) {
                logger.warn("OpRepositoryService：addHashId failed | $e ")
            } finally {
                threadPoolExecutor.shutdown()
            }
        }
        logger.info("OPRepositoryService:finish addHashId-----------")
        logger.info("addhashid time cost: ${System.currentTimeMillis() - startTime}")
    }

    @SuppressWarnings("NestedBlockDepth", "MagicNumber")
    fun updateGitDomain(
        oldGitDomain: String,
        newGitDomain: String,
        grayProject: String?,
        grayWeight: Int?,
        grayWhiteProject: String?
    ): Boolean {
        logger.info(
            "start to update gitDomain|oldGitDomain:$oldGitDomain,newGitDomain:$newGitDomain," +
                "grayProject:$grayProject,grayWeight:$grayWeight,grayWhiteProject:$grayWhiteProject"
        )
        var offset = 0
        val limit = 1000
        val grayWhiteProjects = grayWhiteProject?.split(",") ?: emptyList()
        val grayProjects = grayProject?.split(",") ?: emptyList()
        try {
            do {
                logger.info("update gitDomain project range,offset:$offset,limit:$limit")
                val projectIds = repositoryDao.getProjectIdByGitDomain(
                    dslContext = dslContext,
                    gitDomain = oldGitDomain,
                    limit = limit,
                    offset = offset
                )
                val projectSize = projectIds.size
                logger.info("update gitDomain projectSize:$projectSize")
                projectIds.forEach { projectId ->
                    if (isGrayProject(
                            projectId = projectId,
                            grayProjects = grayProjects,
                            grayWhiteProjects = grayWhiteProjects,
                            grayWeight = grayWeight
                        )
                    ) {
                        logger.info("update gitDomain projectId:$projectId")
                        repositoryDao.updateGitDomainByProjectId(
                            dslContext = dslContext,
                            oldGitDomain = oldGitDomain,
                            newGitDomain = newGitDomain,
                            projectId = projectId
                        )
                    }
                }
                offset += limit
            } while (projectSize == 1000)
        } catch (ignore: Exception) {
            logger.warn("Failed to update gitDomain", ignore)
        }
        return true
    }

    @SuppressWarnings("MagicNumber")
    private fun isGrayProject(
        projectId: String,
        grayProjects: List<String>,
        grayWhiteProjects: List<String>,
        grayWeight: Int?
    ): Boolean {
        val hash = (projectId.hashCode() and Int.MAX_VALUE) % 100
        return when {
            grayWhiteProjects.contains(projectId) -> false
            grayProjects.contains(projectId) -> true
            hash <= (grayWeight ?: -1) -> true
            else -> false
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(OPRepositoryService::class.java)
    }
}
