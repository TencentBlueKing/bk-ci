package com.tencent.devops.common.webhook.service.code

import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.webhook.util.EventCacheUtil
import com.tencent.devops.repository.api.ServiceP4Resource
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.scm.code.p4.api.P4ServerInfo
import com.tencent.devops.scm.pojo.GitCommit
import com.tencent.devops.scm.pojo.GitMrInfo
import com.tencent.devops.scm.pojo.GitMrReviewInfo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 蓝盾事件触发变量缓存处理
 */
@Service
class EventCacheService @Autowired constructor(
    private val gitScmService: GitScmService,
    private val client: Client
) {

    fun getMergeRequestReviewersInfo(projectId: String, mrId: Long?, repo: Repository): GitMrReviewInfo? {
        val eventCache = EventCacheUtil.getOrInitRepoCache(projectId = projectId, repo = repo)
        return eventCache?.gitMrReviewInfo ?: run {
            val mrReviewInfo = gitScmService.getMergeRequestReviewersInfo(
                projectId = projectId,
                mrId = mrId,
                repo = repo
            )
            eventCache?.gitMrReviewInfo = mrReviewInfo
            mrReviewInfo
        }
    }

    fun getMergeRequestInfo(projectId: String, mrId: Long?, repo: Repository): GitMrInfo? {
        val eventCache = EventCacheUtil.getOrInitRepoCache(projectId = projectId, repo = repo)
        return eventCache?.gitMrInfo ?: run {
            val mrInfo = gitScmService.getMergeRequestInfo(
                projectId = projectId,
                mrId = mrId,
                repo = repo
            )
            eventCache?.gitMrInfo = mrInfo
            mrInfo
        }
    }

    fun getMergeRequestChangeInfo(projectId: String, mrId: Long?, repo: Repository): Set<String> {
        val eventCache = EventCacheUtil.getOrInitRepoCache(projectId = projectId, repo = repo)
        return eventCache?.gitMrChangeFiles ?: run {
            val mrChangeInfo = gitScmService.getMergeRequestChangeInfo(
                projectId = projectId,
                mrId = mrId,
                repo = repo
            )
            val changeFiles = mrChangeInfo?.files?.map {
                if (it.deletedFile) {
                    it.oldPath
                } else {
                    it.newPath
                }
            }?.toSet() ?: emptySet()
            eventCache?.gitMrChangeFiles = changeFiles
            changeFiles
        }
    }

    fun getChangeFileList(projectId: String, repo: Repository, from: String, to: String): Set<String> {
        val eventCache = EventCacheUtil.getOrInitRepoCache(projectId = projectId, repo = repo)
        return eventCache?.gitCompareChangeFiles ?: run {
            val compareChangFile = gitScmService.getChangeFileList(
                projectId = projectId,
                repo = repo,
                from = from,
                to = to
            )
            eventCache?.gitCompareChangeFiles = compareChangFile
            compareChangFile
        }
    }

    fun getRepoAuthUser(projectId: String, repo: Repository): String {
        val eventCache = EventCacheUtil.getOrInitRepoCache(projectId = projectId, repo = repo)
        return eventCache?.repoAuthUser ?: run {
            val repoAuthUser = gitScmService.getRepoAuthUser(
                projectId = projectId,
                repo = repo
            )
            eventCache?.repoAuthUser = repoAuthUser
            repoAuthUser
        }
    }

    fun getDefaultBranchLatestCommitInfo(projectId: String, repo: Repository): Pair<String?, GitCommit?> {
        val eventCache = EventCacheUtil.getOrInitRepoCache(projectId = projectId, repo = repo)
        return eventCache?.gitDefaultBranchLatestCommitInfo ?: run {
            val gitDefaultBranchLatestCommitInfo = gitScmService.getDefaultBranchLatestCommitInfo(
                projectId = projectId,
                repo = repo
            )
            eventCache?.gitDefaultBranchLatestCommitInfo = gitDefaultBranchLatestCommitInfo
            gitDefaultBranchLatestCommitInfo
        }
    }

    fun getP4ChangelistFiles(
        repo: Repository,
        projectId: String,
        repositoryId: String,
        repositoryType: RepositoryType?,
        change: Int
    ): List<String> {
        val eventCache = EventCacheUtil.getOrInitRepoCache(projectId = projectId, repo = repo)
        return eventCache?.p4ChangeFiles ?: run {
            val changeFiles = client.get(ServiceP4Resource::class).getChangelistFiles(
                projectId = projectId,
                repositoryId = repositoryId,
                repositoryType = repositoryType,
                change = change
            ).data?.map { it.depotPathString } ?: emptyList()
            eventCache?.p4ChangeFiles = changeFiles
            changeFiles
        }
    }

    fun getP4ServerInfo(
        repo: Repository,
        projectId: String,
        repositoryId: String,
        repositoryType: RepositoryType?
    ): P4ServerInfo? {
        val eventCache = EventCacheUtil.getOrInitRepoCache(projectId = projectId, repo = repo)
        return eventCache?.serverInfo ?: run {
            client.get(ServiceP4Resource::class).getServerInfo(
                projectId = projectId,
                repositoryId = repositoryId,
                repositoryType = repositoryType
            ).data
        }
    }
}
