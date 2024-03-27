package com.tencent.devops.common.webhook.service.code

import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.webhook.service.code.matcher.ScmWebhookMatcher
import com.tencent.devops.common.webhook.util.EventCacheUtil
import com.tencent.devops.repository.api.ServiceP4Resource
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.repository.sdk.github.response.PullRequestResponse
import com.tencent.devops.scm.code.p4.api.P4ChangeList
import com.tencent.devops.scm.code.p4.api.P4ServerInfo
import com.tencent.devops.scm.pojo.GitCommit
import com.tencent.devops.scm.pojo.GitCommitReviewInfo
import com.tencent.devops.scm.pojo.GitMrInfo
import com.tencent.devops.scm.pojo.GitMrReviewInfo
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
        maxCount: Int,
        matcher: ScmWebhookMatcher,
        projectId: String,
        pipelineId: String,
        buildId: String
    ): List<WebhookCommit> {
        val eventCache = EventCacheUtil.getOrInitRepoCache(projectId = projectId, repo = repo)
        // 缓存第一页的数据,如果缓存全部数据,可能会导致OOM。目前只有少量的数据才会分页，能够减少大量请求数。
        val firstPageWebhookCommitList = eventCache?.webhookCommitList ?: run {
            val webhookCommitList = getWebhookCommitList(
                repo = repo,
                matcher = matcher,
                projectId = projectId,
                pipelineId = pipelineId,
                useScrollPage = false,
                maxCount = maxCount
            )
            eventCache?.webhookCommitList = webhookCommitList
            webhookCommitList
        }
        return if (firstPageWebhookCommitList.size == WEBHOOK_COMMIT_PAGE_SIZE) {
            val otherWebhookCommitList = getWebhookCommitList(
                repo = repo,
                matcher = matcher,
                projectId = projectId,
                pipelineId = pipelineId,
                useScrollPage = true,
                maxCount = maxCount
            )
            firstPageWebhookCommitList.toMutableList().addAll(otherWebhookCommitList)
            firstPageWebhookCommitList
        } else {
            firstPageWebhookCommitList
        }.let {
            // 超过最大数量
            if (it.size > maxCount) {
                it.subList(0, maxCount)
            } else {
                it
            }
        }
    }

    /**
     * @param useScrollPage 是否需要分页查询, true-需要滚动, false-不需要滚动
     */
    private fun getWebhookCommitList(
        repo: Repository,
        matcher: ScmWebhookMatcher,
        projectId: String,
        pipelineId: String,
        useScrollPage: Boolean,
        maxCount: Int
    ): List<WebhookCommit> {
        // 需要滚动滚动查询时，第一页数据已经有了不需要重复请求接口，直接从第二页开始查，避免再次调接口
        var page = if(useScrollPage) 2 else 1
        val webhookCommitList = mutableListOf<WebhookCommit>()
        try {
            while (true) {
                val list = matcher.getWebhookCommitList(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    repository = repo,
                    page = page,
                    size = WEBHOOK_COMMIT_PAGE_SIZE
                )
                webhookCommitList.addAll(list)
                if (webhookCommitList.size < WEBHOOK_COMMIT_PAGE_SIZE ||
                    !useScrollPage ||
                    webhookCommitList.size >= maxCount
                ) break
                page++
            }
        } catch (ignored: Throwable) {
            logger.info("fail to get webhook commit list | err is $ignored")
        }
        return webhookCommitList
    }

    companion object {
        private val logger = LoggerFactory.getLogger(EventCacheService::class.java)
        private const val WEBHOOK_COMMIT_PAGE_SIZE = 500
    }
}
