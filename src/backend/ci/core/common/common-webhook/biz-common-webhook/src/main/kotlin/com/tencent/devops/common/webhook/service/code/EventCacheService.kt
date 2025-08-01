package com.tencent.devops.common.webhook.service.code

import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.webhook.service.code.matcher.ScmWebhookMatcher
import com.tencent.devops.common.webhook.util.EventCacheUtil
import com.tencent.devops.repository.api.ServiceP4Resource
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.repository.sdk.github.response.CommitResponse
import com.tencent.devops.repository.sdk.github.response.PullRequestResponse
import com.tencent.devops.scm.code.p4.api.P4ChangeList
import com.tencent.devops.scm.code.p4.api.P4ServerInfo
import com.tencent.devops.scm.enums.TapdRefType
import com.tencent.devops.scm.pojo.GitCommit
import com.tencent.devops.scm.pojo.GitCommitReviewInfo
import com.tencent.devops.scm.pojo.GitMrInfo
import com.tencent.devops.scm.pojo.GitMrReviewInfo
import com.tencent.devops.scm.pojo.GitTagInfo
import com.tencent.devops.scm.pojo.TapdWorkItem
import com.tencent.devops.scm.pojo.WebhookCommit
import org.slf4j.LoggerFactory
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
    ): P4ChangeList? {
        val eventCache = EventCacheUtil.getOrInitRepoCache(projectId = projectId, repo = repo)
        return eventCache?.p4ChangeFiles ?: run {
            val changeFiles = client.get(ServiceP4Resource::class).getChangelist(
                projectId = projectId,
                repositoryId = repositoryId,
                repositoryType = repositoryType,
                change = change
            ).data
            eventCache?.p4ChangeFiles = changeFiles
            changeFiles
        }
    }

    fun getP4ShelvedChangelistFiles(
        repo: Repository,
        projectId: String,
        repositoryId: String,
        repositoryType: RepositoryType?,
        change: Int
    ): P4ChangeList? {
        val eventCache = EventCacheUtil.getOrInitRepoCache(projectId = projectId, repo = repo)
        return eventCache?.p4ShelveChangeFiles ?: run {
            val changeFiles = client.get(ServiceP4Resource::class).getShelvedChangeList(
                projectId = projectId,
                repositoryId = repositoryId,
                repositoryType = repositoryType,
                change = change
            ).data
            eventCache?.p4ShelveChangeFiles = changeFiles
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
            val p4ServerInfo = client.get(ServiceP4Resource::class).getServerInfo(
                projectId = projectId,
                repositoryId = repositoryId,
                repositoryType = repositoryType
            ).data
            eventCache?.serverInfo = p4ServerInfo
            p4ServerInfo
        }
    }

    /**
     * 获取日常评审信息
     */

    fun getCommitReviewInfo(
        projectId: String,
        commitReviewId: Long?,
        repo: Repository
    ): GitCommitReviewInfo? {
        val eventCache = EventCacheUtil.getOrInitRepoCache(projectId = projectId, repo = repo)
        return eventCache?.gitCommitReviewInfo ?: run {
            val commitReviewInfo = gitScmService.getCommitReviewInfo(
                projectId = projectId,
                commitReviewId = commitReviewId,
                repo = repo
            )
            eventCache?.gitCommitReviewInfo = commitReviewInfo
            commitReviewInfo
        }
    }

    fun getPrInfo(
        githubRepoName: String,
        pullNumber: String,
        repo: Repository,
        projectId: String
    ): PullRequestResponse? {
        val eventCache = EventCacheUtil.getOrInitRepoCache(projectId = projectId, repo = repo)
        return eventCache?.githubPrInfo ?: run {
            val prInfo = gitScmService.getPrInfo(
                repo = repo,
                githubRepoName = githubRepoName,
                pullNumber = pullNumber
            )
            eventCache?.githubPrInfo = prInfo
            prInfo
        }
    }

    @SuppressWarnings("NestedBlockDepth")
    fun getWebhookCommitList(
        repo: Repository,
        matcher: ScmWebhookMatcher,
        projectId: String,
        pipelineId: String
    ): List<WebhookCommit> {
        val eventCache = EventCacheUtil.getOrInitRepoCache(projectId = projectId, repo = repo)
        // 缓存第一页的数据
        return eventCache?.webhookCommitList ?: run {
            try {
                val webhookCommitList = matcher.getWebhookCommitList(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    repository = repo,
                    page = 1,
                    size = WEBHOOK_COMMIT_PAGE_SIZE
                )
                eventCache?.webhookCommitList = webhookCommitList
                webhookCommitList
            } catch (ignored: Throwable) {
                logger.info("fail to get webhook commit list | err is $ignored")
                emptyList()
            }
        }
    }

    fun getGithubCommitInfo(
        githubRepoName: String,
        commitId: String,
        repo: Repository,
        projectId: String
    ): CommitResponse? {
        val eventCache = EventCacheUtil.getOrInitRepoCache(projectId = projectId, repo = repo)
        return eventCache?.githubCommitInfo ?: run {
            val githubCommitInfo = gitScmService.getGithubCommitInfo(
                githubRepoName = githubRepoName,
                commitId = commitId,
                repo = repo
            )
            eventCache?.githubCommitInfo = githubCommitInfo
            githubCommitInfo
        }
    }

    fun getTagInfo(
        projectId: String,
        repo: Repository,
        tagName: String
    ): GitTagInfo? {
        val eventCache = EventCacheUtil.getOrInitRepoCache(projectId = projectId, repo = repo)
        return eventCache?.tagInfo ?: run {
            val tagInfo = gitScmService.getTag(
                projectId = projectId,
                repo = repo,
                tagName = tagName
            )
            eventCache?.tagInfo = tagInfo
            tagInfo
        }
    }

    fun getTapdItem(
        projectId: String,
        repo: Repository,
        refType: TapdRefType,
        iid: Long
    ): List<TapdWorkItem>? {
        val eventCache = EventCacheUtil.getOrInitRepoCache(projectId = projectId, repo = repo)
        return eventCache?.tapdWorkItems ?: run {
            val tapdWorkItems = gitScmService.getTapdItem(
                projectId = projectId,
                repo = repo,
                refType = refType.value,
                iid = iid
            )
            eventCache?.tapdWorkItems = tapdWorkItems
            tapdWorkItems
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(EventCacheService::class.java)
        private const val WEBHOOK_COMMIT_PAGE_SIZE = 500
    }
}
