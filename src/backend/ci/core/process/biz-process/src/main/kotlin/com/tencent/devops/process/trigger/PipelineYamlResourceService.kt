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

package com.tencent.devops.process.trigger

import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.engine.dao.PipelineYamlInfoDao
import com.tencent.devops.process.engine.dao.PipelineYamlVersionDao
import com.tencent.devops.process.pojo.pipeline.PipelineYamlVo
import com.tencent.devops.process.service.PipelineInfoFacadeService
import com.tencent.devops.process.trigger.actions.BaseAction
import com.tencent.devops.process.trigger.pojo.PacTriggerLock
import com.tencent.devops.process.trigger.pojo.YamlPathListEntry
import com.tencent.devops.repository.api.ServiceRepositoryResource
import com.tencent.devops.repository.pojo.CodeGitRepository
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineYamlResourceService @Autowired constructor(
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val pipelineYamlInfoDao: PipelineYamlInfoDao,
    private val pipelineYamlVersionDao: PipelineYamlVersionDao,
    private val pipelineInfoFacadeService: PipelineInfoFacadeService,
    private val client: Client
) {

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineYamlResourceService::class.java)
    }

    fun syncYamlPipeline(
        projectId: String,
        action: BaseAction
    ) {
        val triggerPipeline = action.data.context.pipeline!!
        val filePath = triggerPipeline.filePath
        val yamlFile = action.data.context.yamlFile!!
        logger.info("syncYamlPipeline|$projectId|pipeline:${triggerPipeline}|yamlFile:${yamlFile}")
        PacTriggerLock(
            redisOperation = redisOperation,
            projectId = projectId,
            repoHashId = action.data.setting.repoHashId,
            filePath = filePath
        ).use {
            it.lock()
            if (triggerPipeline.pipelineId.isBlank()) {
                createPipelineIfAbsent(
                    projectId = projectId,
                    action = action,
                    yamlFile = yamlFile
                )
            } else {
                updatePipelineIfAbsent(
                    projectId = projectId,
                    pipelineId = triggerPipeline.pipelineId,
                    action = action,
                    yamlFile = yamlFile
                )
            }
        }
    }

    /**
     * 创建yaml流水线,如果流水线不存在则创建，创建则更新版本
     */
    fun createPipelineIfAbsent(
        projectId: String,
        action: BaseAction,
        yamlFile: YamlPathListEntry
    ) {
        // 再次确认yaml文件是否已经创建出流水线
        pipelineYamlInfoDao.get(
            dslContext = dslContext,
            projectId = projectId,
            repoHashId = action.data.setting.repoHashId,
            filePath = yamlFile.yamlPath,
        )?.let {
            updatePipelineIfAbsent(
                projectId = projectId,
                pipelineId = it.pipelineId,
                action = action,
                yamlFile = yamlFile
            )
        } ?: run {
            createYamlPipeline(
                projectId = projectId,
                action = action,
                yamlFile = yamlFile
            )
        }
    }

    fun updatePipelineIfAbsent(
        projectId: String,
        pipelineId: String,
        action: BaseAction,
        yamlFile: YamlPathListEntry
    ) {
        pipelineYamlVersionDao.get(
            dslContext = dslContext,
            projectId = projectId,
            repoHashId = action.data.setting.repoHashId,
            filePath = yamlFile.yamlPath,
            blobId = yamlFile.blobId!!
        ) ?: run {
            updateYamlPipeline(
                projectId = projectId,
                pipelineId = pipelineId,
                action = action,
                yamlFile = yamlFile
            )
        }
    }

    fun createYamlPipeline(
        projectId: String,
        action: BaseAction,
        yamlFile: YamlPathListEntry
    ) {
        logger.info("create yaml pipeline|$projectId|$yamlFile")
        val yamlContent = action.getYamlContent(yamlFile.yamlPath)
        val branch = action.data.eventCommon.branch
        val deployPipelineResult = pipelineInfoFacadeService.createYamlPipeline(
            userId = action.data.setting.enableUser,
            projectId = projectId,
            yml = yamlContent.content,
            branchName = branch,
            isDefaultBranch = branch == action.data.context.defaultBranch
        )
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            pipelineYamlInfoDao.save(
                dslContext = transactionContext,
                projectId = projectId,
                repoHashId = action.data.setting.repoHashId,
                filePath = yamlFile.yamlPath,
                pipelineId = deployPipelineResult.pipelineId,
                userId = action.data.getUserId()
            )
            pipelineYamlVersionDao.save(
                dslContext = transactionContext,
                projectId = projectId,
                repoHashId = action.data.setting.repoHashId,
                filePath = yamlFile.yamlPath,
                blobId = yamlFile.blobId!!,
                ref = branch,
                pipelineId = deployPipelineResult.pipelineId,
                version = deployPipelineResult.version,
                versionName = deployPipelineResult.versionName!!,
                userId = action.data.getUserId()
            )
        }
    }

    fun updateYamlPipeline(
        projectId: String,
        pipelineId: String,
        action: BaseAction,
        yamlFile: YamlPathListEntry
    ) {
        logger.info("update yaml pipeline|$projectId|$yamlFile")
        val yamlContent = action.getYamlContent(yamlFile.yamlPath)
        val branch = action.data.eventCommon.branch
        val deployPipelineResult = pipelineInfoFacadeService.updateYamlPipeline(
            userId = action.data.setting.enableUser,
            projectId = projectId,
            pipelineId = pipelineId,
            yml = yamlContent.content,
            branchName = branch,
            isDefaultBranch = branch == action.data.context.defaultBranch
        )
        pipelineYamlVersionDao.save(
            dslContext = dslContext,
            projectId = projectId,
            repoHashId = action.data.setting.repoHashId,
            filePath = yamlFile.yamlPath,
            blobId = yamlFile.blobId!!,
            ref = branch,
            pipelineId = deployPipelineResult.pipelineId,
            version = deployPipelineResult.version,
            versionName = deployPipelineResult.versionName!!,
            userId = action.data.getUserId()
        )
    }

    fun getPipelineYamlInfo(
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
            val repository = client.get(ServiceRepositoryResource::class).get(
                projectId = projectId, repositoryId = repoHashId, repositoryType = RepositoryType.ID
            ).data ?: run {
                logger.info("pipeline yaml version repo not found|$projectId|$pipelineId|$version|$repoHashId")
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
    }
}
