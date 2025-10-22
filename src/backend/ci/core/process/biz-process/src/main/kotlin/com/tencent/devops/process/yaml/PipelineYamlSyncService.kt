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

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.engine.dao.PipelineYamlSyncDao
import com.tencent.devops.process.pojo.pipeline.PipelineYamlSyncInfo
import com.tencent.devops.process.pojo.trigger.PipelineTriggerReasonDetail
import com.tencent.devops.process.yaml.pojo.YamlPathListEntry
import com.tencent.devops.repository.api.ServiceRepositoryPacResource
import com.tencent.devops.repository.pojo.enums.RepoYamlSyncStatusEnum
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineYamlSyncService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val pipelineYamlSyncDao: PipelineYamlSyncDao,
    private val redisOperation: RedisOperation
) {

    fun initPacSyncDetail(
        projectId: String,
        repoHashId: String,
        yamlPathList: List<YamlPathListEntry>
    ) {
        val syncFileInfoList = yamlPathList.map {
            PipelineYamlSyncInfo(
                filePath = it.yamlPath,
                fileUrl = it.yamlUrl,
                syncStatus = RepoYamlSyncStatusEnum.SYNC
            )
        }
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            pipelineYamlSyncDao.delete(
                dslContext = transactionContext,
                projectId = projectId,
                repoHashId = repoHashId
            )
            pipelineYamlSyncDao.batchAdd(
                dslContext = transactionContext,
                projectId = projectId,
                repoHashId = repoHashId,
                syncFileInfoList = syncFileInfoList
            )
        }
        val syncStatus = if (syncFileInfoList.isEmpty()) {
            RepoYamlSyncStatusEnum.SUCCEED.name
        } else {
            RepoYamlSyncStatusEnum.SYNC.name
        }
        client.get(ServiceRepositoryPacResource::class).updateYamlSyncStatus(
            projectId = projectId,
            repoHashId = repoHashId,
            syncStatus = syncStatus
        )
    }

    fun enablePacFailed(
        projectId: String,
        repoHashId: String
    ) {
        client.get(ServiceRepositoryPacResource::class).updateYamlSyncStatus(
            projectId = projectId,
            repoHashId = repoHashId,
            syncStatus = RepoYamlSyncStatusEnum.FAILED.name
        )
    }

    /**
     * 同步成功
     */
    fun syncSuccess(projectId: String, repoHashId: String, filePath: String) {
        val syncFileInfo = PipelineYamlSyncInfo(filePath = filePath, syncStatus = RepoYamlSyncStatusEnum.SUCCEED)
        pipelineYamlSyncDao.updateSyncStatus(
            dslContext = dslContext,
            projectId = projectId,
            repoHashId = repoHashId,
            syncFileInfo = syncFileInfo
        )
        updateYamlSyncStatus(projectId = projectId, repoHashId = repoHashId, syncFileInfo = syncFileInfo)
    }

    /**
     * 同步失败
     */
    fun syncFailed(
        projectId: String,
        repoHashId: String,
        filePath: String,
        reason: String,
        reasonDetail: PipelineTriggerReasonDetail
    ) {
        val syncFileInfo = PipelineYamlSyncInfo(
            filePath = filePath,
            syncStatus = RepoYamlSyncStatusEnum.FAILED,
            reason = reason,
            reasonDetail = JsonUtil.toJson(reasonDetail)
        )
        updateYamlSyncStatus(projectId = projectId, repoHashId = repoHashId, syncFileInfo = syncFileInfo)
    }

    fun listSyncFailedYaml(
        projectId: String,
        repoHashId: String
    ): List<PipelineYamlSyncInfo> {
        return pipelineYamlSyncDao.listYamlSync(
            dslContext = dslContext,
            projectId = projectId,
            repoHashId = repoHashId,
            syncStatus = RepoYamlSyncStatusEnum.FAILED.name
        ).map {
            PipelineYamlSyncInfo(
                filePath = it.filePath,
                fileUrl = it.fileUrl,
                syncStatus = RepoYamlSyncStatusEnum.valueOf(it.syncStatus),
                reason = it.reason,
                reasonDetail = it.reasonDetail?.let { detail ->
                    try {
                        // 同步错误原因只会有一条
                        JsonUtil.to(detail, PipelineTriggerReasonDetail::class.java).getReasonDetailList()?.get(0)
                    } catch (ignored: Throwable) {
                        detail
                    }
                }
            )
        }
    }

    fun delete(
        projectId: String,
        repoHashId: String
    ) {
        pipelineYamlSyncDao.delete(
            dslContext = dslContext,
            projectId = projectId,
            repoHashId = repoHashId
        )
    }

    private fun updateYamlSyncStatus(
        projectId: String,
        repoHashId: String,
        syncFileInfo: PipelineYamlSyncInfo
    ) {
        pipelineYamlSyncDao.updateSyncStatus(
            dslContext = dslContext,
            projectId = projectId,
            repoHashId = repoHashId,
            syncFileInfo = syncFileInfo
        )
        val lock = RedisLock(
            redisOperation,
            "pipeline:yaml:sync:$projectId:$repoHashId", 60L
        )
        // 修改代码库整体同步状态
        lock.use {
            lock.lock()
            val syncStatusList = pipelineYamlSyncDao.listYamlSync(
                dslContext = dslContext,
                projectId = projectId,
                repoHashId = repoHashId
            ).map { it.syncStatus }
            // 还有正在同步的文件,不修改状态
            if (syncStatusList.contains(RepoYamlSyncStatusEnum.SYNC.name)) {
                return
            }
            val syncStatus = if (syncStatusList.contains(RepoYamlSyncStatusEnum.FAILED.name)) {
                RepoYamlSyncStatusEnum.FAILED.name
            } else {
                RepoYamlSyncStatusEnum.SUCCEED.name
            }
            client.get(ServiceRepositoryPacResource::class).updateYamlSyncStatus(
                projectId = projectId,
                repoHashId = repoHashId,
                syncStatus = syncStatus
            )
        }
    }
}
