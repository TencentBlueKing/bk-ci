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

import com.tencent.devops.common.api.constant.coerceAtMaxLength
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.model.process.tables.records.TPipelineYamlBranchFileRecord
import com.tencent.devops.process.engine.dao.PipelineWebhookVersionDao
import com.tencent.devops.process.engine.dao.PipelineYamlBranchFileDao
import com.tencent.devops.process.engine.dao.PipelineYamlInfoDao
import com.tencent.devops.process.engine.dao.PipelineYamlVersionDao
import com.tencent.devops.process.engine.dao.PipelineYamlViewDao
import com.tencent.devops.process.pojo.pipeline.PipelineYamlInfo
import com.tencent.devops.process.pojo.pipeline.PipelineYamlVersion
import com.tencent.devops.process.pojo.pipeline.PipelineYamlView
import com.tencent.devops.process.pojo.pipeline.PipelineYamlVo
import com.tencent.devops.process.pojo.pipeline.enums.PipelineYamlStatus
import com.tencent.devops.process.pojo.webhook.PipelineWebhookVersion
import com.tencent.devops.repository.api.ServiceRepositoryResource
import com.tencent.devops.repository.pojo.CodeGitRepository
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class PipelineYamlService(
    private val dslContext: DSLContext,
    private val pipelineYamlInfoDao: PipelineYamlInfoDao,
    private val pipelineYamlVersionDao: PipelineYamlVersionDao,
    private val pipelineWebhookVersionDao: PipelineWebhookVersionDao,
    private val pipelineYamlViewDao: PipelineYamlViewDao,
    private val pipelineYamlBranchFileDao: PipelineYamlBranchFileDao,
    private val client: Client
) {

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineYamlService::class.java)
        private const val MAX_FILE_PATH_LENGTH = 512
    }

    fun save(
        projectId: String,
        repoHashId: String,
        filePath: String,
        directory: String,
        pipelineId: String,
        status: String,
        userId: String,
        blobId: String,
        commitId: String,
        ref: String,
        version: Int,
        webhooks: List<PipelineWebhookVersion>
    ) {
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            pipelineYamlInfoDao.save(
                dslContext = transactionContext,
                projectId = projectId,
                repoHashId = repoHashId,
                filePath = filePath,
                directory = directory,
                pipelineId = pipelineId,
                status = status,
                userId = userId
            )
            pipelineYamlVersionDao.save(
                dslContext = transactionContext,
                projectId = projectId,
                repoHashId = repoHashId,
                filePath = filePath,
                blobId = blobId,
                commitId = commitId,
                ref = ref,
                pipelineId = pipelineId,
                version = version,
                userId = userId
            )
            pipelineWebhookVersionDao.batchSave(
                dslContext = transactionContext,
                webhooks = webhooks
            )
            pipelineYamlBranchFileDao.save(
                dslContext = transactionContext,
                projectId = projectId,
                repoHashId = repoHashId,
                branch = ref,
                filePath = filePath
            )
        }
    }

    fun update(
        projectId: String,
        repoHashId: String,
        filePath: String,
        pipelineId: String,
        userId: String,
        blobId: String,
        commitId: String,
        ref: String,
        defaultBranch: String?,
        version: Int,
        webhooks: List<PipelineWebhookVersion>,
        needDeleteVersion: Boolean
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
            if (needDeleteVersion) {
                pipelineYamlVersionDao.deleteByBlobId(
                    dslContext = transactionContext,
                    projectId = projectId,
                    repoHashId = repoHashId,
                    filePath = filePath,
                    blobId = blobId
                )
            }
            pipelineYamlVersionDao.save(
                dslContext = transactionContext,
                projectId = projectId,
                repoHashId = repoHashId,
                filePath = filePath,
                blobId = blobId,
                commitId = commitId,
                ref = ref,
                pipelineId = pipelineId,
                version = version,
                userId = userId
            )
            pipelineWebhookVersionDao.batchSave(
                dslContext = transactionContext,
                webhooks = webhooks
            )
            pipelineYamlBranchFileDao.save(
                dslContext = transactionContext,
                projectId = projectId,
                repoHashId = repoHashId,
                branch = ref,
                filePath = filePath
            )
        }
        if (!defaultBranch.isNullOrBlank()) {
            refreshPipelineYamlStatus(
                projectId = projectId,
                repoHashId = repoHashId,
                filePath = filePath,
                defaultBranch = defaultBranch
            )
        }
    }

    fun refreshPipelineYamlStatus(
        projectId: String,
        repoHashId: String,
        filePath: String,
        defaultBranch: String
    ) {
        val branchList = pipelineYamlBranchFileDao.listBranch(
            dslContext = dslContext,
            projectId = projectId,
            repoHashId = repoHashId,
            filePath = filePath
        )
        val pipelineYamlInfo = pipelineYamlInfoDao.get(
            dslContext = dslContext,
            projectId = projectId,
            repoHashId = repoHashId,
            filePath = filePath
        ) ?: return
        val status = when {
            branchList.isEmpty() -> PipelineYamlStatus.DELETED.name
            branchList.size == 1 && branchList.contains(defaultBranch) -> PipelineYamlStatus.OK.name
            branchList.isNotEmpty() && branchList.contains(defaultBranch) -> PipelineYamlStatus.UN_MERGED.name
            else -> null
        }
        if (status != null && pipelineYamlInfo.status != status) {
            logger.info(
                "update pipeline yaml status|" +
                        "$projectId|$repoHashId|$filePath|from:${pipelineYamlInfo.status}|to:$status"
            )
            pipelineYamlInfoDao.updateStatus(
                dslContext = dslContext,
                projectId = projectId,
                repoHashId = repoHashId,
                filePath = filePath,
                status = status
            )
        }
    }

    fun updatePipelineYamlStatus(
        projectId: String,
        repoHashId: String,
        filePath: String,
        status: String
    ) {
        pipelineYamlInfoDao.updateStatus(
            dslContext = dslContext,
            projectId = projectId,
            repoHashId = repoHashId,
            filePath = filePath,
            status = status
        )
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

    fun getPipelineYamlInfo(
        projectId: String,
        pipelineId: String
    ): PipelineYamlInfo? {
        return pipelineYamlInfoDao.get(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId
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
        pipelineId: String,
        version: Int
    ): PipelineYamlVo? {
        val pipelineYamlInfo =
            pipelineYamlInfoDao.get(dslContext = dslContext, projectId = projectId, pipelineId = pipelineId) ?: run {
                logger.info("pipeline yaml not found|$projectId|$pipelineId")
                return null
            }
        val pipelineYamlVersion = pipelineYamlVersionDao.getByPipelineId(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            version = version
        ) ?: run {
            logger.info("pipeline yaml version not found|$projectId|$pipelineId|$version")
            return null
        }
        return pipelineYamlVo(pipelineYamlInfo = pipelineYamlInfo, pipelineYamlVersion = pipelineYamlVersion)
    }

    private fun pipelineYamlVo(
        pipelineYamlInfo: PipelineYamlInfo,
        pipelineYamlVersion: PipelineYamlVersion
    ): PipelineYamlVo? {
        with(pipelineYamlVersion) {
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
                        fileUrl = "$homePage/blob/$commitId/$filePath",
                        status = pipelineYamlInfo.status
                    )
                }

                else -> null
            }
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

    fun getAllYamlPipeline(
        projectId: String,
        repoHashId: String
    ): List<PipelineYamlInfo> {
        return pipelineYamlInfoDao.getAllByRepo(
            dslContext = dslContext,
            projectId = projectId,
            repoHashId = repoHashId
        )
    }

    fun deleteYamlPipeline(
        userId: String,
        projectId: String,
        repoHashId: String,
        filePath: String
    ) {
        val deleteTime = DateTimeUtil.toDateTime(LocalDateTime.now(), "yyMMddHHmmSS")
        val deleteFilePath = "$filePath[$deleteTime]"
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            pipelineYamlInfoDao.delete(
                dslContext = transactionContext,
                userId = userId,
                projectId = projectId,
                repoHashId = repoHashId,
                filePath = filePath,
                deleteFilePath = deleteFilePath.coerceAtMaxLength(MAX_FILE_PATH_LENGTH)
            )
            pipelineYamlVersionDao.deleteAll(
                dslContext = transactionContext,
                projectId = projectId,
                repoHashId = repoHashId,
                filePath = filePath
            )
        }
    }

    fun savePipelineYamlView(
        projectId: String,
        repoHashId: String,
        directory: String,
        viewId: Long
    ) {
        pipelineYamlViewDao.save(
            dslContext = dslContext,
            projectId = projectId,
            repoHashId = repoHashId,
            directory = directory,
            viewId = viewId
        )
    }

    fun getPipelineYamlView(
        projectId: String,
        repoHashId: String,
        directory: String
    ): PipelineYamlView? {
        return pipelineYamlViewDao.get(
            dslContext = dslContext,
            projectId = projectId,
            repoHashId = repoHashId,
            directory = directory
        )
    }

    fun listRepoYamlView(
        projectId: String,
        repoHashId: String
    ): List<PipelineYamlView> {
        return pipelineYamlViewDao.listRepoYamlView(
            dslContext = dslContext,
            projectId = projectId,
            repoHashId = repoHashId
        )
    }

    fun deleteYamlView(
        projectId: String,
        repoHashId: String,
        directory: String
    ) {
        pipelineYamlViewDao.delete(
            dslContext = dslContext,
            projectId = projectId,
            repoHashId = repoHashId,
            directory = directory
        )
    }

    fun deleteBranchFile(
        projectId: String,
        repoHashId: String,
        branch: String,
        filePath: String
    ) {
        pipelineYamlBranchFileDao.deleteFile(
            dslContext = dslContext,
            projectId = projectId,
            repoHashId = repoHashId,
            branch = branch,
            filePath = filePath
        )
    }

    fun getAllBranchFilePath(
        projectId: String,
        repoHashId: String,
        branch: String
    ): List<String> {
        return pipelineYamlBranchFileDao.getAllFilePath(
            dslContext = dslContext,
            projectId = projectId,
            repoHashId = repoHashId,
            branch = branch
        )
    }

    fun getBranchFilePath(
        projectId: String,
        repoHashId: String,
        branch: String,
        filePath: String
    ): TPipelineYamlBranchFileRecord? {
        return pipelineYamlBranchFileDao.get(
            dslContext = dslContext,
            projectId = projectId,
            repoHashId = repoHashId,
            branch = branch,
            filePath = filePath
        )
    }
}
