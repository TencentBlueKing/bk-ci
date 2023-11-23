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
 *
 */

package com.tencent.devops.process.yaml

import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.client.Client
import com.tencent.devops.process.engine.dao.PipelineYamlInfoDao
import com.tencent.devops.process.engine.dao.PipelineYamlVersionDao
import com.tencent.devops.process.pojo.pipeline.PipelineYamlInfo
import com.tencent.devops.process.pojo.pipeline.PipelineYamlVersion
import com.tencent.devops.process.pojo.pipeline.PipelineYamlVo
import com.tencent.devops.repository.api.ServiceRepositoryResource
import com.tencent.devops.repository.pojo.CodeGitRepository
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PipelineYamlService(
    private val dslContext: DSLContext,
    private val pipelineYamlInfoDao: PipelineYamlInfoDao,
    private val pipelineYamlVersionDao: PipelineYamlVersionDao,
    private val client: Client
) {

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineYamlService::class.java)
    }

    fun save(
        projectId: String,
        repoHashId: String,
        filePath: String,
        pipelineId: String,
        userId: String,
        blobId: String,
        ref: String?,
        version: Int,
        versionName: String
    ) {
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            pipelineYamlInfoDao.save(
                dslContext = transactionContext,
                projectId = projectId,
                repoHashId = repoHashId,
                filePath = filePath,
                pipelineId = pipelineId,
                userId = userId
            )
            pipelineYamlVersionDao.save(
                dslContext = transactionContext,
                projectId = projectId,
                repoHashId = repoHashId,
                filePath = filePath,
                blobId = blobId,
                ref = ref,
                pipelineId = pipelineId,
                version = version,
                versionName = versionName,
                userId = userId
            )
        }
    }

    fun saveYamlPipeline(
        projectId: String,
        repoHashId: String,
        filePath: String,
        pipelineId: String,
        userId: String
    ) {
        pipelineYamlInfoDao.save(
            dslContext = dslContext,
            projectId = projectId,
            repoHashId = repoHashId,
            filePath = filePath,
            pipelineId = pipelineId,
            userId = userId
        )
    }

    fun update(
        projectId: String,
        repoHashId: String,
        filePath: String,
        pipelineId: String,
        userId: String,
        blobId: String,
        ref: String?,
        version: Int,
        versionName: String
    ) {
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            pipelineYamlInfoDao.update(
                dslContext = transactionContext,
                projectId = projectId,
                repoHashId = repoHashId,
                filePath = filePath,
                userId = userId
            )
            pipelineYamlVersionDao.save(
                dslContext = dslContext,
                projectId = projectId,
                repoHashId = repoHashId,
                filePath = filePath,
                blobId = blobId,
                ref = ref,
                pipelineId = pipelineId,
                version = version,
                versionName = versionName,
                userId = userId
            )
        }
    }

    fun getPipelineYamlInfo(
        projectId: String,
        repoHashId: String,
        filePath: String
    ): PipelineYamlInfo? {
        return pipelineYamlInfoDao.get(
            dslContext = dslContext,
            projectId = projectId,
            repoHashId = repoHashId,
            filePath = filePath
        )
    }

    fun getPipelineYamlVersion(
        projectId: String,
        repoHashId: String,
        filePath: String,
        blobId: String
    ): PipelineYamlVersion? {
        return pipelineYamlVersionDao.get(
            dslContext = dslContext,
            projectId = projectId,
            repoHashId = repoHashId,
            filePath = filePath,
            blobId = blobId
        )
    }

    fun getPipelineYamlVo(
        projectId: String,
        pipelineId: String
    ): PipelineYamlVo? {
        val pipelineInfo = pipelineYamlInfoDao.get(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId
        ) ?: run {
            logger.info("pipeline yaml info not found|$projectId|$pipelineId")
            return null
        }
        with(pipelineInfo) {
            return pipelineYamlVo(projectId, pipelineId, repoHashId, filePath)
        }
    }

    fun getPipelineYamlVo(
        projectId: String,
        pipelineId: String,
        version: Int
    ): PipelineYamlVo? {
        val pipelineYamlVersion = pipelineYamlVersionDao.getByPipelineId(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            version = version
        ) ?: run {
            logger.info("pipeline yaml version not found|$projectId|$pipelineId|$version")
            return null
        }
        with(pipelineYamlVersion) {
            return pipelineYamlVo(projectId, pipelineId, repoHashId, filePath)
        }
    }

    private fun pipelineYamlVo(
        projectId: String,
        pipelineId: String,
        repoHashId: String,
        filePath: String
    ): PipelineYamlVo? {
        val repository = client.get(ServiceRepositoryResource::class).get(
            projectId = projectId, repositoryId = repoHashId, repositoryType = RepositoryType.ID
        ).data ?: run {
            logger.info("pipeline yaml version repo not found|$projectId|$pipelineId|$repoHashId")
            return null
        }
        return when (repository) {
            is CodeGitRepository -> {
                val homePage =
                    repository.url.replace("git@", "https://").removeSuffix(".git")
                PipelineYamlVo(
                    repoHashId = repoHashId,
                    scmType = ScmType.CODE_GIT,
                    pathWithNamespace = repository.projectName,
                    webUrl = homePage,
                    filePath = filePath,
                    fileUrl = "$homePage/blob/master/$filePath"
                )
            }

            else -> null
        }
    }

    fun getPipelineYamlVersion(
        projectId: String,
        pipelineId: String,
        version: Int
    ): PipelineYamlVersion? {
        return pipelineYamlVersionDao.getByPipelineId(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            version = version
        )
    }

    fun countPipelineYaml(
        projectId: String,
        repoHashId: String
    ): Long {
        return pipelineYamlInfoDao.countYamlPipeline(
            dslContext = dslContext,
            projectId = projectId,
            repoHashId = repoHashId
        )
    }

    fun delete(
        projectId: String,
        repoHashId: String,
        filePath: String
    ) {
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            pipelineYamlInfoDao.delete(
                dslContext = transactionContext,
                projectId = projectId,
                repoHashId = repoHashId,
                filePath = filePath
            )
            pipelineYamlVersionDao.delete(
                dslContext = transactionContext,
                projectId = projectId,
                repoHashId = repoHashId,
                filePath = filePath
            )
        }
    }
}
