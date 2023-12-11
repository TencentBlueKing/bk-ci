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

import com.tencent.devops.common.client.Client
import com.tencent.devops.process.yaml.pojo.YamlPathListEntry
import com.tencent.devops.repository.api.ServiceRepositoryPacResource
import com.tencent.devops.repository.pojo.RepoYamlSyncInfo
import com.tencent.devops.repository.pojo.enums.RepoYamlSyncStatusEnum
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineYamlSyncService @Autowired constructor(
    private val client: Client
) {

    fun initPacSyncDetail(
        projectId: String,
        repoHashId: String,
        yamlPathList: List<YamlPathListEntry>
    ) {
        val syncFileInfoList =
            yamlPathList.map { RepoYamlSyncInfo(filePath = it.yamlPath, syncStatus = RepoYamlSyncStatusEnum.SYNC) }
        client.get(ServiceRepositoryPacResource::class).initPacSyncDetail(
            projectId = projectId,
            repositoryHashId = repoHashId,
            syncFileInfoList = syncFileInfoList
        )
    }

    /**
     * 同步成功
     */
    fun syncSuccess(projectId: String, repoHashId: String, filePath: String) {
        val syncFileInfo = RepoYamlSyncInfo(filePath = filePath, syncStatus = RepoYamlSyncStatusEnum.SUCCEED)
        client.get(ServiceRepositoryPacResource::class).updatePacSyncStatus(
            projectId = projectId,
            repositoryHashId = repoHashId,
            syncFileInfo = syncFileInfo
        )
    }

    /**
     * 同步失败
     */
    fun syncFailed(
        projectId: String,
        repoHashId: String,
        filePath: String,
        reason: String,
        reasonDetail: String
    ) {
        val syncFileInfo = RepoYamlSyncInfo(
            filePath = filePath,
            syncStatus = RepoYamlSyncStatusEnum.FAILED,
            reason = reason,
            reasonDetail = reasonDetail
        )
        client.get(ServiceRepositoryPacResource::class).updatePacSyncStatus(
            projectId = projectId,
            repositoryHashId = repoHashId,
            syncFileInfo = syncFileInfo
        )
    }
}
