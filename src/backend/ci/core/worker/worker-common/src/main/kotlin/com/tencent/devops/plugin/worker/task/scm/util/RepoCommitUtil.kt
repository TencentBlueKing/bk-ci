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
import org.eclipse.jgit.api.LogCommand
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

@Suppress("ALL")
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
            return doSaveGitCommit(
                git = git,
                repo = repo,
                pipelineId = pipelineId,
                buildId = buildId,
                repositoryConfig = repositoryConfig,
                gitType = gitType
            )
        } catch (e: Exception) {
            LoggerService.addErrorLine("save commit fail: ${e.message}")
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
        val latestCommit = getLastCommitId(
            pipelineId = pipelineId,
            repositoryConfig = repositoryConfig,
            repo = repo,
            logsCommand = logsCommand
        )

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
                type = ScmType.parse(gitType),
                pipelineId = pipelineId,
                buildId = buildId,
                commit = it.name,
                committer = it.committerIdent.name,
                commitTime = it.commitTime.toLong(), // 单位:秒
                comment = it.shortMessage,
                repoId = repositoryConfig.repositoryHashId,
                repoName = repositoryConfig.repositoryName,
                elementId = LoggerService.elementId
            )
        }

        if (commits.isEmpty()) {
            saveCommit(
                listOf(
                    CommitData(
                        type = ScmType.parse(gitType),
                        pipelineId = pipelineId,
                        buildId = buildId,
                        commit = "",
                        committer = "",
                        commitTime = 0L,
                        comment = "",
                        repoId = repositoryConfig.repositoryHashId,
                        repoName = repositoryConfig.repositoryName,
                        elementId = LoggerService.elementId
                    )
                )
            )
        } else {
            val newCommit = commits.first()
            newCommitId = newCommit.commit
            LoggerService.addNormalLine("new commit: $newCommitId")
            saveCommit(commits)
        }

        return CommitMaterial(
            lastCommitId = lastCommitId,
            newCommitId = newCommitId,
            newCommitComment = latestCommit?.comment,
            commitTimes = commits.size
        )
    }

    private fun getLastCommitId(
        pipelineId: String,
        repositoryConfig: RepositoryConfig,
        repo: Repository,
        logsCommand: LogCommand
    ): CommitData? {
        val latestCommitList =
            commitResourceApi.getLatestCommit(
                pipelineId = pipelineId,
                elementId = LoggerService.elementId,
                repositoryConfig = repositoryConfig
            ).data
        latestCommitList!!.forEach { latestCommit ->
            val lastCommitId = latestCommit.commit
            try {
                logsCommand.not(resolveRev(repo, lastCommitId))
                LoggerService.addNormalLine("last commit: $lastCommitId")
                return latestCommit
            } catch (e: Exception) {
                LoggerService.addErrorLine("resolve commit fail($lastCommitId): ${e.message} ")
            }
        }
        logsCommand.setMaxCount(1)
        return null
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
            return doSaveSvnCommit(
                manager = manager,
                url = url,
                pipelineId = pipelineId,
                buildId = buildId,
                repositoryConfig = repositoryConfig,
                lastCommit = lastCommit
            )
        } catch (e: Exception) {
            LoggerService.addErrorLine("save svn commit fail: ${e.message}")
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

        val commitData = commitResourceApi.getLatestCommit(
            pipelineId = pipelineId,
            elementId = LoggerService.elementId,
            repositoryConfig = repositoryConfig
        ).data?.firstOrNull()

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
                        type = ScmType.parse(ScmType.CODE_SVN),
                        pipelineId = pipelineId,
                        buildId = buildId,
                        commit = "",
                        committer = "",
                        commitTime = 0L,
                        comment = "",
                        repoId = repositoryConfig.repositoryHashId,
                        repoName = repositoryConfig.repositoryName,
                        elementId = LoggerService.elementId
                    )
                )
            )
            return CommitMaterial(
                lastCommitId = latestCommit.toString(),
                newCommitId = lastCommit.toString(),
                newCommitComment = commitData.comment,
                commitTimes = 0
            )
        }

        val commits = mutableListOf<CommitData>()
        LoggerService.addNormalLine("save svn commit range: [$latestCommit,$lastCommit]")
        logs.receiver = ISvnObjectReceiver<SVNLogEntry> { _, info ->

            commits.add(
                CommitData(
                    type = ScmType.parse(ScmType.CODE_SVN),
                    pipelineId = pipelineId,
                    buildId = buildId,
                    commit = info.revision.toString(),
                    committer = info.author,
                    commitTime = info.date.time / 1000,
                    comment = info.message,
                    repoId = repositoryConfig.repositoryHashId,
                    repoName = repositoryConfig.repositoryName,
                    elementId = LoggerService.elementId
                )
            )
        }

        logs.run()

        val commitsList = commits.filter { it.commit.toLong() != latestCommit }

        saveCommit(commitsList)

        return CommitMaterial(
            lastCommitId = latestCommit?.toString(),
            newCommitId = lastCommit.toString(),
            newCommitComment = commitData?.comment,
            commitTimes = commitsList.size
        )
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
