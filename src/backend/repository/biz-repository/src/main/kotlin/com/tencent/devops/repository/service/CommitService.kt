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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.repository.service

import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.repository.dao.CommitDao
import com.tencent.devops.repository.dao.RepositoryDao
import com.tencent.devops.repository.pojo.commit.CommitData
import com.tencent.devops.repository.pojo.commit.CommitResponse
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CommitService @Autowired constructor(
    private val repositoryDao: RepositoryDao,
    private val commitDao: CommitDao,
    private val dslContext: DSLContext
) {

    fun getCommit(buildId: String): List<CommitResponse> {
        val commits = commitDao.getBuildCommit(dslContext, buildId)

        val repos = repositoryDao.getRepoByIds(dslContext, commits?.map { it.repoId } ?: listOf())
        val repoMap = repos?.map { it.repositoryId.toString() to it }?.toMap() ?: mapOf()

        return commits?.map {
            CommitData(
                it.type,
                it.pipelineId,
                it.buildId,
                it.commit,
                it.committer,
                it.commitTime.timestampmilli(),
                it.comment,
                it.repoId?.toString(),
                it.repoName,
                it.elementId
            )
        }?.groupBy { it.elementId }?.map {
            val elementId = it.value[0].elementId
            val repoId = it.value[0].repoId
            CommitResponse(
                (repoMap[repoId]?.aliasName ?: "unknown repo"),
                elementId,
                it.value.filter { it.commit.isNotBlank() })
        } ?: listOf()
    }

    fun addCommit(commits: List<CommitData>): Int {
        return commitDao.addCommit(dslContext, commits).size
    }

    fun getLatestCommit(
        pipelineId: String,
        elementId: String,
        repositoryId: String,
        repositoryType: RepositoryType?
    ): CommitData? {
        return if (repositoryType == null || repositoryType == RepositoryType.ID) {
            val repoId = HashUtil.decodeOtherIdToLong(repositoryId)
            val data = commitDao.getLatestCommitById(dslContext, pipelineId, elementId, repoId)
            if (data == null) {
                null
            } else {
                CommitData(
                    data.type,
                    pipelineId,
                    data.buildId,
                    data.commit,
                    data.committer,
                    data.commitTime.timestampmilli(),
                    data.comment,
                    data.repoId.toString(),
                    null,
                    data.elementId
                )
            }
        } else {
            val data = commitDao.getLatestCommitByName(dslContext, pipelineId, elementId, repositoryId)
            if (data == null) {
                null
            } else {
                CommitData(
                    data.type,
                    pipelineId,
                    data.buildId,
                    data.commit,
                    data.committer,
                    data.commitTime.timestampmilli(),
                    data.comment,
                    null,
                    data.repoName,
                    data.elementId
                )
            }
        }
    }
}
