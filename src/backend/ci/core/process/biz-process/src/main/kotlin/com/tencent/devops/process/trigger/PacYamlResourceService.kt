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

import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.engine.dao.PipelineYamlReferDao
import com.tencent.devops.process.engine.dao.PipelineYamlVersionDao
import com.tencent.devops.process.service.PipelineInfoFacadeService
import com.tencent.devops.process.trigger.actions.BaseAction
import com.tencent.devops.process.trigger.pojo.CheckType
import com.tencent.devops.process.trigger.pojo.PacTriggerLock
import com.tencent.devops.process.trigger.pojo.YamlPathListEntry
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PacYamlResourceService @Autowired constructor(
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val pipelineYamlReferDao: PipelineYamlReferDao,
    private val pipelineYamlVersionDao: PipelineYamlVersionDao,
    private val pipelineInfoFacadeService: PipelineInfoFacadeService
) {

    fun syncYamlPipeline(
        projectId: String,
        action: BaseAction
    ) {
        action.getYamlPathList().filter { it.checkType == CheckType.NEED_CHECK }.forEach { entry ->
            PacTriggerLock(
                redisOperation = redisOperation,
                projectId = projectId,
                repoHashId = action.data.setting.repoHashId,
                filePath = entry.yamlPath
            ).use {
                it.lock()
                val pipelineYamlRefer = pipelineYamlReferDao.get(
                    dslContext = dslContext,
                    projectId = projectId,
                    repoHashId = action.data.setting.repoHashId,
                    filePath = entry.yamlPath,
                )
                if (pipelineYamlRefer == null) {
                    createYamlPipeline(
                        projectId = projectId,
                        action = action,
                        entry = entry
                    )
                } else {
                    updateYamlPipeline(
                        projectId = projectId,
                        pipelineId = pipelineYamlRefer.pipelineId,
                        action = action,
                        entry = entry
                    )
                }
            }
        }
    }

    fun createYamlPipeline(
        projectId: String,
        action: BaseAction,
        entry: YamlPathListEntry
    ) {
        val yamlContent = action.getYamlContent(entry.yamlPath)
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
            pipelineYamlReferDao.save(
                dslContext = transactionContext,
                projectId = projectId,
                repoHashId = action.data.setting.repoHashId,
                filePath = entry.yamlPath,
                pipelineId = deployPipelineResult.pipelineId
            )
            pipelineYamlVersionDao.save(
                dslContext = transactionContext,
                projectId = projectId,
                repoHashId = action.data.setting.repoHashId,
                filePath = entry.yamlPath,
                blobId = entry.blobId!!,
                pipelineId = deployPipelineResult.pipelineId,
                version = deployPipelineResult.version,
                // TODO 需要改成具体的版本名称
                versionName = "p.1.1"
            )
        }
    }

    fun updateYamlPipeline(
        projectId: String,
        pipelineId: String,
        action: BaseAction,
        entry: YamlPathListEntry
    ) {
        val yamlContent = action.getYamlContent(entry.yamlPath)
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
            filePath = entry.yamlPath,
            blobId = entry.blobId!!,
            pipelineId = deployPipelineResult.pipelineId,
            version = deployPipelineResult.version,
            // TODO 需要改成具体的版本名称
            versionName = "p.1.1"
        )
    }
}
