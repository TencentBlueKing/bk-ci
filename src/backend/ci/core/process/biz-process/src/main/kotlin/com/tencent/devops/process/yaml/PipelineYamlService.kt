/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.BranchVersionAction
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import com.tencent.devops.process.engine.dao.PipelineWebhookVersionDao
import com.tencent.devops.process.engine.dao.PipelineYamlBranchFileDao
import com.tencent.devops.process.engine.dao.PipelineYamlInfoDao
import com.tencent.devops.process.engine.dao.PipelineYamlVersionDao
import com.tencent.devops.process.pojo.pipeline.PipelineYamlInfo
import com.tencent.devops.process.pojo.pipeline.PipelineYamlVersion
import com.tencent.devops.process.pojo.pipeline.PipelineYamlVo
import com.tencent.devops.process.pojo.pipeline.enums.PipelineYamlStatus
import com.tencent.devops.process.pojo.webhook.PipelineWebhookVersion
import com.tencent.devops.project.api.service.ServiceAllocIdResource
import com.tencent.devops.repository.api.ServiceRepositoryResource
import com.tencent.devops.repository.pojo.RepoPipelineRefVo
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
    private val pipelineYamlBranchFileDao: PipelineYamlBranchFileDao,
    private val client: Client,
    private val pipelineInfoDao: PipelineInfoDao
) {

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineYamlService::class.java)
        private const val PIPELINE_YAML_VERSION_BIZ_ID = "T_PIPELINE_YAML_VERSION"
    }

    fun save(
        projectId: String,
        repoHashId: String,
        filePath: String,
        directory: String,
        defaultBranch: String?,
        pipelineId: String,
        status: String,
        userId: String,
        blobId: String,
        commitId: String,
        commitTime: LocalDateTime,
        ref: String,
        version: Int,
        webhooks: List<PipelineWebhookVersion>
    ) {
        val id = client.get(ServiceAllocIdResource::class).generateSegmentId(PIPELINE_YAML_VERSION_BIZ_ID).data ?: 0
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            pipelineYamlInfoDao.save(
                dslContext = transactionContext,
                projectId = projectId,
                repoHashId = repoHashId,
                filePath = filePath,
                directory = directory,
                defaultBranch = defaultBranch,
                pipelineId = pipelineId,
                status = status,
                userId = userId
            )
            pipelineYamlVersionDao.save(
                dslContext = transactionContext,
                id = id,
                projectId = projectId,
                repoHashId = repoHashId,
                filePath = filePath,
                ref = ref,
                commitId = commitId,
                commitTime = commitTime,
                blobId = blobId,
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
                filePath = filePath,
                commitId = commitId,
                blobId = blobId,
                commitTime = commitTime
            )
        }
    }

    fun save(
        projectId: String,
        repoHashId: String,
        filePath: String,
        directory: String,
        defaultBranch: String?,
        pipelineId: String,
        status: String,
        userId: String,
        blobId: String,
        commitId: String,
        commitTime: LocalDateTime,
        ref: String,
        version: Int
    ) {
        val id = client.get(ServiceAllocIdResource::class).generateSegmentId(PIPELINE_YAML_VERSION_BIZ_ID).data ?: 0
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            pipelineYamlInfoDao.save(
                dslContext = transactionContext,
                projectId = projectId,
                repoHashId = repoHashId,
                filePath = filePath,
                directory = directory,
                defaultBranch = defaultBranch,
                pipelineId = pipelineId,
                status = status,
                userId = userId
            )
            pipelineYamlVersionDao.save(
                dslContext = transactionContext,
                id = id,
                projectId = projectId,
                repoHashId = repoHashId,
                filePath = filePath,
                ref = ref,
                commitId = commitId,
                commitTime = commitTime,
                blobId = blobId,
                pipelineId = pipelineId,
                version = version,
                userId = userId
            )
            pipelineYamlBranchFileDao.save(
                dslContext = transactionContext,
                projectId = projectId,
                repoHashId = repoHashId,
                branch = ref,
                filePath = filePath,
                commitId = commitId,
                blobId = blobId,
                commitTime = commitTime
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
        commitTime: LocalDateTime,
        ref: String,
        defaultBranch: String?,
        version: Int,
        webhooks: List<PipelineWebhookVersion>
    ) {
        val id = client.get(ServiceAllocIdResource::class).generateSegmentId(PIPELINE_YAML_VERSION_BIZ_ID).data ?: 0
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            pipelineYamlInfoDao.update(
                dslContext = transactionContext,
                projectId = projectId,
                repoHashId = repoHashId,
                filePath = filePath,
                defaultBranch = defaultBranch,
                userId = userId
            )
            pipelineYamlVersionDao.save(
                dslContext = transactionContext,
                id = id,
                projectId = projectId,
                repoHashId = repoHashId,
                filePath = filePath,
                ref = ref,
                commitId = commitId,
                commitTime = commitTime,
                blobId = blobId,
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
                filePath = filePath,
                commitId = commitId,
                blobId = blobId,
                commitTime = commitTime
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

    fun update(
        projectId: String,
        repoHashId: String,
        filePath: String,
        pipelineId: String,
        userId: String,
        blobId: String,
        commitId: String,
        commitTime: LocalDateTime,
        ref: String,
        defaultBranch: String?,
        version: Int
    ) {
        val id = client.get(ServiceAllocIdResource::class).generateSegmentId(PIPELINE_YAML_VERSION_BIZ_ID).data ?: 0
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            pipelineYamlInfoDao.update(
                dslContext = transactionContext,
                projectId = projectId,
                repoHashId = repoHashId,
                filePath = filePath,
                defaultBranch = defaultBranch,
                userId = userId
            )
            pipelineYamlVersionDao.save(
                dslContext = transactionContext,
                id = id,
                projectId = projectId,
                repoHashId = repoHashId,
                filePath = filePath,
                ref = ref,
                commitId = commitId,
                commitTime = commitTime,
                blobId = blobId,
                pipelineId = pipelineId,
                version = version,
                userId = userId
            )
            pipelineYamlBranchFileDao.save(
                dslContext = transactionContext,
                projectId = projectId,
                repoHashId = repoHashId,
                branch = ref,
                filePath = filePath,
                commitId = commitId,
                blobId = blobId,
                commitTime = commitTime
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

    /**
     * 当非默认分支文件删除或者merge时,刷新yaml流水线状态
     */
    fun refreshPipelineYamlStatus(
        projectId: String,
        repoHashId: String,
        filePath: String,
        defaultBranch: String
    ) {
        val branchList = listRef(
            projectId = projectId,
            repoHashId = repoHashId,
            filePath = filePath,
            branchAction = BranchVersionAction.ACTIVE
        )
        val pipelineYamlInfo = pipelineYamlInfoDao.get(
            dslContext = dslContext,
            projectId = projectId,
            repoHashId = repoHashId,
            filePath = filePath
        ) ?: return
        val status = when {
            // 没有任何分支与当前yaml关联,说明yaml文件已经全部删除
            branchList.isEmpty() -> PipelineYamlStatus.DELETED.name
            // 有且仅在主干对yaml文件做过变更,其他分支没有对yaml文件有变更过
            branchList.size == 1 && branchList.contains(defaultBranch) -> PipelineYamlStatus.OK.name
            // 除了主干,还存在其他文件对文件有过变更
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

    /**
     * yaml文件是否在默认分支存在
     */
    fun yamlExistInDefaultBranch(
        projectId: String,
        pipelineIds: List<String>
    ): Map<String, Boolean> {
        val yamlInfoMap = pipelineYamlInfoDao.listByPipelineIds(
            dslContext = dslContext,
            projectId = projectId,
            pipelineIds = pipelineIds
        ).associateBy { it.pipelineId }
        return pipelineIds.associateWith { pipelineId ->
            val yamlInfo = yamlInfoMap[pipelineId]
            // 如果流水线没有绑定PAC,则表示yaml不存在
            if (yamlInfo == null || yamlInfo.defaultBranch.isNullOrBlank()) {
                false
            } else {
                val branchYamlFile = pipelineYamlBranchFileDao.get(
                    dslContext = dslContext,
                    projectId = projectId,
                    repoHashId = yamlInfo.repoHashId,
                    branch = yamlInfo.defaultBranch!!,
                    filePath = yamlInfo.filePath
                )
                if (branchYamlFile == null) {
                    false
                } else {
                    // 默认分支删除,是软删除,不会直接删除
                    !branchYamlFile.deleted
                }
            }
        }
    }

    fun getPipelineYamlVersion(
        projectId: String,
        repoHashId: String,
        filePath: String,
        ref: String? = null,
        commitId: String? = null,
        blobId: String? = null,
        branchAction: String? = null
    ): PipelineYamlVersion? {
        return pipelineYamlVersionDao.getPipelineYamlVersion(
            dslContext = dslContext,
            projectId = projectId,
            repoHashId = repoHashId,
            filePath = filePath,
            ref = ref,
            commitId = commitId,
            blobId = blobId,
            branchAction = branchAction
        )
    }

    fun updateBranchAction(
        projectId: String,
        repoHashId: String,
        filePath: String,
        ref: String,
        branchAction: String
    ) {
        pipelineYamlVersionDao.updateBranchAction(
            dslContext = dslContext,
            projectId = projectId,
            repoHashId = repoHashId,
            filePath = filePath,
            ref = ref,
            branchAction = branchAction
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
        val pipelineYamlVersion = pipelineYamlVersionDao.getPipelineYamlVersion(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            version = version
        )
        return buildPipelineYamlVo(pipelineYamlInfo = pipelineYamlInfo, pipelineYamlVersion = pipelineYamlVersion)
    }

    private fun buildPipelineYamlVo(
        pipelineYamlInfo: PipelineYamlInfo,
        pipelineYamlVersion: PipelineYamlVersion?
    ): PipelineYamlVo? {
        val projectId = pipelineYamlInfo.projectId
        val repoHashId = pipelineYamlInfo.repoHashId
        val filePath = pipelineYamlInfo.filePath

        val repository = with(pipelineYamlInfo) {
            client.get(ServiceRepositoryResource::class).get(
                projectId = projectId, repositoryId = repoHashId, repositoryType = RepositoryType.ID
            ).data ?: run {
                logger.info("pipeline yaml version repo not found|$projectId|$pipelineId|$repoHashId")
                return null
            }
        }
        val homePage =
            repository.url.replace("git@", "https://").removeSuffix(".git")
        return if (pipelineYamlVersion == null) {
            PipelineYamlVo(
                repoHashId = repoHashId,
                scmType = repository.getScmType(),
                pathWithNamespace = repository.projectName,
                webUrl = homePage,
                filePath = filePath,
                status = pipelineYamlInfo.status
            )
        } else {
            PipelineYamlVo(
                repoHashId = repoHashId,
                scmType = repository.getScmType(),
                pathWithNamespace = repository.projectName,
                webUrl = homePage,
                filePath = filePath,
                fileUrl = "$homePage/blob/${pipelineYamlVersion.commitId}/$filePath",
                status = pipelineYamlInfo.status
            )
        }
    }

    fun countPipelineYaml(
        projectId: String,
        repoHashId: String,
        directory: String? = null
    ): Long {
        return pipelineYamlInfoDao.countYamlPipeline(
            dslContext = dslContext,
            projectId = projectId,
            repoHashId = repoHashId,
            directory = directory
        )
    }

    fun listPipelineYaml(
        projectId: String,
        repoHashId: String,
        limit: Int,
        offset: Int
    ): SQLPage<RepoPipelineRefVo> {
        val count = pipelineYamlInfoDao.countYamlPipeline(
            dslContext = dslContext,
            projectId = projectId,
            repoHashId = repoHashId
        )
        val pipelineYamlList = pipelineYamlInfoDao.listYamlPipeline(
            dslContext = dslContext,
            projectId = projectId,
            repoHashId = repoHashId,
            limit = limit,
            offset = offset
        )
        val pipelineInfoMap = pipelineInfoDao.listInfoByPipelineIds(
            dslContext = dslContext,
            projectId = projectId,
            pipelineIds = pipelineYamlList.map { it.pipelineId }.toSet()
        ).associateBy { it.pipelineId }

        val records = pipelineYamlList.map {
            RepoPipelineRefVo(
                projectId = projectId,
                pipelineId = it.pipelineId,
                pipelineName = pipelineInfoMap[it.pipelineId]?.pipelineName ?: ""
            )
        }
        return SQLPage(count = count, records = records)
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

    fun listRef(
        projectId: String,
        repoHashId: String,
        filePath: String,
        branchAction: BranchVersionAction = BranchVersionAction.ACTIVE,
        excludeRef: String? = null
    ): List<String> {
        return pipelineYamlVersionDao.listRef(
            dslContext = dslContext,
            projectId = projectId,
            repoHashId = repoHashId,
            filePath = filePath,
            branchAction = branchAction.name,
            excludeRef = excludeRef
        )
    }

    fun deleteYamlPipeline(
        userId: String,
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
            pipelineYamlVersionDao.deleteAll(
                dslContext = transactionContext,
                projectId = projectId,
                repoHashId = repoHashId,
                filePath = filePath
            )
        }
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
}
