package com.tencent.devops.remotedev.service.transfer

import com.tencent.devops.common.api.constant.HTTP_200
import com.tencent.devops.common.api.constant.HTTP_403
import com.tencent.devops.common.api.exception.OauthForbiddenException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.sdk.github.pojo.GithubRepo
import com.tencent.devops.common.sdk.github.request.GetRepositoryContentRequest
import com.tencent.devops.common.sdk.github.request.GetTreeRequest
import com.tencent.devops.common.sdk.github.request.ListBranchesRequest
import com.tencent.devops.common.sdk.github.request.ListRepositoriesRequest
import com.tencent.devops.remotedev.common.Constansts
import com.tencent.devops.remotedev.pojo.RemoteDevRepository
import com.tencent.devops.remotedev.service.GitTransferService
import com.tencent.devops.repository.api.ServiceGithubResource
import com.tencent.devops.repository.api.github.ServiceGithubBranchResource
import com.tencent.devops.repository.api.github.ServiceGithubDatabaseResource
import com.tencent.devops.repository.api.github.ServiceGithubOauthResource
import com.tencent.devops.repository.api.github.ServiceGithubRepositoryResource
import com.tencent.devops.repository.api.github.ServiceGithubUserResource
import com.tencent.devops.repository.api.scm.ServiceGitResource
import com.tencent.devops.repository.pojo.AuthorizeResult
import com.tencent.devops.repository.pojo.enums.GitCodeBranchesSort
import com.tencent.devops.repository.pojo.enums.GitCodeProjectsOrder
import com.tencent.devops.repository.pojo.enums.RedirectUrlTypeEnum
import com.tencent.devops.repository.pojo.git.GitUserInfo
import com.tencent.devops.scm.enums.GitAccessLevelEnum
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class GithubTransferService @Autowired constructor(
    private val client: Client
) : GitTransferService {

    companion object {
        private const val DEFAULT_GITHUB_PER_PAGE = 100
        private const val DEFAULT_PAGE = 1
        private const val DEFAULT_PAGE_SIZE = 30
    }

    override fun isOAuth(
        userId: String,
        redirectUrlType: RedirectUrlTypeEnum?,
        redirectUrl: String?,
        refreshToken: Boolean?
    ): Result<AuthorizeResult> {
        val accessToken = client.get(ServiceGithubResource::class).getAccessToken(userId).data?.accessToken
        if (accessToken == null || refreshToken == true) {
            val url = client.get(ServiceGithubOauthResource::class).oauthUrl(
                redirectUrl = redirectUrl ?: "",
                userId = userId
            ).data ?: ""
            return Result(AuthorizeResult(status = HTTP_403, url = url))
        }
        return Result(AuthorizeResult(status = HTTP_200, url = ""))
    }

    override fun getProjectList(
        userId: String,
        page: Int,
        pageSize: Int,
        search: String?,
        owned: Boolean?,
        minAccessLevel: GitAccessLevelEnum?
    ): List<RemoteDevRepository> {
        client.get(ServiceGitResource::class).getGitCodeProjectList(
            accessToken = getAndCheckOauthToken(userId),
            page = page,
            pageSize = pageSize,
            search = search,
            orderBy = GitCodeProjectsOrder.ACTIVITY,
            sort = GitCodeBranchesSort.DESC,
            owned = owned,
            minAccessLevel = minAccessLevel
        ).data?.map { RemoteDevRepository(it.pathWithNamespace, it.httpsUrlToRepo) } ?: emptyList()
        // search  owned  minAccessLevel 参数暂时没使用
        var githubPage = DEFAULT_PAGE
        val repos = mutableListOf<RemoteDevRepository>()
        // github查询有权限列表不支持名称搜索，需要自实现搜索
        // TODO 目前先全部查出来然后再分页,后面需要改造
        run outside@{
            while (true) {
                val request = ListRepositoriesRequest(
                    page = githubPage,
                    perPage = DEFAULT_GITHUB_PER_PAGE
                )
                val githubRepos = client.get(ServiceGithubRepositoryResource::class).listRepositories(
                    request = request,
                    token = getAndCheckOauthToken(userId)
                ).data!!
                val filterGithubRepos =
                    githubRepos.filter { search(search, it) }.map { RemoteDevRepository(it.fullName, it.cloneUrl) }
                repos.addAll(filterGithubRepos)
                if (githubRepos.size < DEFAULT_GITHUB_PER_PAGE) {
                    return@outside
                }
                githubPage++
            }
        }
        val start = (page - 1) * pageSize
        val end = (start + pageSize).coerceAtMost(repos.size)
        return if (start >= repos.size) {
            emptyList()
        } else {
            repos.subList(start, end)
        }
    }

    private fun search(search: String?, githubRepo: GithubRepo): Boolean {
        if (search.isNullOrBlank()) {
            return true
        }
        return githubRepo.fullName.contains(search)
    }

    override fun getProjectBranches(
        userId: String,
        pathWithNamespace: String,
        page: Int?,
        pageSize: Int?,
        search: String?
    ): List<String>? {
        return client.get(ServiceGithubBranchResource::class).listBranch(
            token = getAndCheckOauthToken(userId),
            request = ListBranchesRequest(
                repoName = pathWithNamespace,
                page = page ?: DEFAULT_PAGE,
                perPage = pageSize ?: DEFAULT_PAGE_SIZE
            )
        ).data?.map { it.name }
    }

    override fun getFileContent(userId: String, pathWithNamespace: String, filePath: String, ref: String): String {
        return client.get(ServiceGithubRepositoryResource::class).getRepositoryContent(
            token = getAndCheckOauthToken(userId),
            request = GetRepositoryContentRequest(
                repoName = pathWithNamespace,
                path = filePath,
                ref = ref
            )
        ).data?.getDecodedContentAsString() ?: ""
    }

    override fun getFileNameTree(
        userId: String,
        pathWithNamespace: String,
        path: String?,
        ref: String?,
        recursive: Boolean
    ): List<String> {
        return client.get(ServiceGithubDatabaseResource::class).getTree(
            token = getAndCheckOauthToken(userId),
            request = GetTreeRequest(
                repoName = pathWithNamespace,
                treeSha = "${ref!!}:$path",
                recursive = recursive.toString()
            )
        ).data?.tree?.filter {
            it.type == "blob" && (
                it.path.endsWith(Constansts.devFileExtensionYaml) || it.path.endsWith(
                    Constansts.devFileExtensionYml
                )
                )
        }?.map { it.path } ?: emptyList()
    }

    override fun getAndCheckOauthToken(
        userId: String
    ): String {
        return client.get(ServiceGithubResource::class).getAccessToken(userId).data?.accessToken
            ?: throw OauthForbiddenException(
                message = "用户[$userId]尚未进行OAUTH授权，请先授权。"
            )
    }

    override fun getUserInfo(userId: String): GitUserInfo {
        return client.get(ServiceGithubUserResource::class).getUser(
            token = getAndCheckOauthToken(userId)
        ).data!!.let {
            GitUserInfo(
                id = it.id,
                email = it.email,
                username = it.login,
                webUrl = it.htmlUrl,
                name = it.name,
                state = it.type,
                avatarUrl = it.avatarUrl
            )
        }
    }
}
