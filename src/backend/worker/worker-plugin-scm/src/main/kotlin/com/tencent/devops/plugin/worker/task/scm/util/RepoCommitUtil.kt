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

package com.tencent.devops.plugin.worker.task.scm.util

import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.repository.pojo.commit.CommitData
import com.tencent.devops.repository.pojo.commit.CommitMaterial
import com.tencent.devops.worker.common.api.ApiFactory
import com.tencent.devops.worker.common.api.scm.CommitSDKApi
import com.tencent.devops.worker.common.logger.LoggerService
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.lib.Repository
import org.tmatesoft.svn.core.SVNLogEntry
import org.tmatesoft.svn.core.SVNURL
import org.tmatesoft.svn.core.wc.SVNClientManager
import org.tmatesoft.svn.core.wc.SVNRevision
import org.tmatesoft.svn.core.wc2.ISvnObjectReceiver
import org.tmatesoft.svn.core.wc2.SvnRevisionRange
import org.tmatesoft.svn.core.wc2.SvnTarget

object RepoCommitUtil {

    private val commitResourceApi = ApiFactory.create(CommitSDKApi::class)

    /**
     * 保存并返回commit信息
     * return (lastCommitId. newCommitId)
     */
    fun saveGitCommit(
        git: Git,
        repo: Repository,
        pipelineId: String,
        buildId: String,
        repositoryConfig: RepositoryConfig,
        gitType: ScmType = ScmType.CODE_GIT
    ): CommitMaterial {
        try {
            return doSaveGitCommit(git, repo, pipelineId, buildId, repositoryConfig, gitType)
        } catch (e: Exception) {
            LoggerService.addRedLine("save commit fail: ${e.message}")
        }
        return CommitMaterial(null, null, null, 0)
    }

    private fun doSaveGitCommit(
        git: Git,
        repo: Repository,
        pipelineId: String,
        buildId: String,
        repositoryConfig: RepositoryConfig,
        gitType: ScmType
    ): CommitMaterial {
        val logsCommand = git.log()
        val latestCommit = commitResourceApi.getLatestCommit(pipelineId, LoggerService.elementId, repositoryConfig).data

        val lastCommitId = latestCommit?.commit
        if (lastCommitId != null) {
            logsCommand.not(resolveRev(repo, lastCommitId))
            LoggerService.addNormalLine("last commit: $lastCommitId")
        } else {
            logsCommand.setMaxCount(1)
        }

        var newCommitId: String? = null
        val commits = logsCommand.call().map {
            CommitData(
                ScmType.parse(gitType),
                pipelineId,
                buildId,
                it.name,
                it.committerIdent.name,
                it.commitTime.toLong(), // 单位:秒
                it.shortMessage,
                repositoryConfig.repositoryHashId,
                repositoryConfig.repositoryName,
                LoggerService.elementId
            )
        }

        if (commits.isEmpty()) {
            saveCommit(
                listOf(
                    CommitData(
                        ScmType.parse(gitType),
                        pipelineId,
                        buildId,
                        "",
                        "",
                        0L,
                        "",
                        repositoryConfig.repositoryHashId,
                        repositoryConfig.repositoryName,
                        LoggerService.elementId
                    )
                )
            )
        } else {
            val newCommit = commits.first()
            newCommitId = newCommit.commit
            LoggerService.addNormalLine("new commit: $newCommitId")
            saveCommit(commits)
        }

        return CommitMaterial(lastCommitId, newCommitId, latestCommit?.comment, commits.size)
    }

    /**
     * 保存并返回revision信息
     * return (lastRevision, newRevision)
     */
    fun saveSvnCommit(
        manager: SVNClientManager,
        url: SVNURL,
        pipelineId: String,
        buildId: String,
        repositoryConfig: RepositoryConfig,
        lastCommit: Long
    ): CommitMaterial {
        try {
            return doSaveSvnCommit(manager, url, pipelineId, buildId, repositoryConfig, lastCommit)
        } catch (e: Exception) {
            LoggerService.addRedLine("save svn commit fail: ${e.message}")
        }

        return CommitMaterial(null, lastCommit.toString(), null, 0)
    }

    private fun doSaveSvnCommit(
        manager: SVNClientManager,
        url: SVNURL,
        pipelineId: String,
        buildId: String,
        repositoryConfig: RepositoryConfig,
        lastCommit: Long
    ): CommitMaterial {
        val logs = manager.logClient.operationsFactory.createLog()

        val commitData = commitResourceApi.getLatestCommit(pipelineId, LoggerService.elementId, repositoryConfig).data
        // 获取最后一次commit
        val latestCommit = commitData?.commit?.toLong()

        if (latestCommit != null) {
            logs.addRange(SvnRevisionRange.create(SVNRevision.create(latestCommit), SVNRevision.create(lastCommit)))
        } else {
            logs.addRange(SvnRevisionRange.create(SVNRevision.create(lastCommit), SVNRevision.create(0L)))
            logs.limit = 1
        }

        logs.addTarget(SvnTarget.fromURL(url))
        if (latestCommit != null && latestCommit == lastCommit) {
            LoggerService.addNormalLine("svn commit is the newest..")

            saveCommit(
                listOf(
                    CommitData(
                        ScmType.parse(ScmType.CODE_SVN),
                        pipelineId,
                        buildId,
                        "",
                        "",
                        0L,
                        "",
                        repositoryConfig.repositoryHashId,
                        repositoryConfig.repositoryName,
                        LoggerService.elementId
                    )
                )
            )
            return CommitMaterial(latestCommit.toString(), lastCommit.toString(), commitData.comment, 0)
        }

        val commits = mutableListOf<CommitData>()
        LoggerService.addNormalLine("save svn commit range: [$latestCommit,$lastCommit]")
        logs.receiver = ISvnObjectReceiver<SVNLogEntry> { target, info ->

            commits.add(
                CommitData(
                    ScmType.parse(ScmType.CODE_SVN),
                    pipelineId,
                    buildId,
                    info.revision.toString(),
                    info.author,
                    info.date.time / 1000,
                    info.message,
                    repositoryConfig.repositoryHashId,
                    repositoryConfig.repositoryName,
                    LoggerService.elementId
                )
            )
        }
        logs.run()
        val commitsList = commits.filter { it.commit.toLong() != latestCommit }
        saveCommit(commitsList)

        return CommitMaterial(latestCommit?.toString(), lastCommit.toString(), commitData?.comment, commitsList.size)
    }

    private fun saveCommit(commits: List<CommitData>) {
        commitResourceApi.addCommit(commits)
    }

    private fun resolveRev(repo: Repository, rev: String): ObjectId {
        val ref = repo.findRef(rev)
        return if (ref == null) {
            repo.resolve(rev)
        } else {
            getActualRefObjectId(repo, ref)
        }
    }

    private fun getActualRefObjectId(repo: Repository, ref: Ref): ObjectId {
        val repoPeeled = repo.peel(ref)
        return if (repoPeeled.peeledObjectId != null) {
            repoPeeled.peeledObjectId
        } else ref.objectId
    }
}