package com.tencent.devops.remotedev.service.transfer

import com.tencent.devops.common.api.exception.OauthForbiddenException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.remotedev.common.Constansts
import com.tencent.devops.remotedev.pojo.RemoteDevRepository
import com.tencent.devops.remotedev.service.GitTransferService
import com.tencent.devops.repository.api.ServiceOauthResource
import com.tencent.devops.repository.api.scm.ServiceGitResource
import com.tencent.devops.repository.pojo.AuthorizeResult
import com.tencent.devops.repository.pojo.enums.GitCodeBranchesSort
import com.tencent.devops.repository.pojo.enums.GitCodeProjectsOrder
import com.tencent.devops.repository.pojo.enums.RedirectUrlTypeEnum
import com.tencent.devops.repository.pojo.enums.RepoAuthType
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.repository.pojo.git.GitUserInfo
import com.tencent.devops.scm.enums.GitAccessLevelEnum
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.net.URLEncoder

@Service
class TGitTransferService @Autowired constructor(
    private val client: Client
) : GitTransferService {
    override fun isOAuth(
        userId: String,
        redirectUrlType: RedirectUrlTypeEnum?,
        redirectUrl: String?,
        refreshToken: Boolean?
    ): Result<AuthorizeResult> {
        return client.get(ServiceOauthResource::class).isOAuth(
            userId = userId,
            redirectUrlType = redirectUrlType,
            redirectUrl = redirectUrl,
            gitProjectId = null,
            refreshToken = refreshToken
        )
    }

    override fun getProjectList(
        userId: String,
        page: Int,
        pageSize: Int,
        search: String?,
        owned: Boolean?,
        minAccessLevel: GitAccessLevelEnum?
    ): List<RemoteDevRepository> {
        return client.get(ServiceGitResource::class).getGitCodeProjectList(
            accessToken = getAndCheckOauthToken(userId),
            page = page,
            pageSize = pageSize,
            search = search,
            orderBy = GitCodeProjectsOrder.ACTIVITY,
            sort = GitCodeBranchesSort.DESC,
            owned = owned,
            minAccessLevel = minAccessLevel
        ).data?.map { RemoteDevRepository(it.pathWithNamespace, it.httpsUrlToRepo) } ?: emptyList()
    }

    override fun getProjectBranches(
        userId: String,
        pathWithNamespace: String,
        page: Int?,
        pageSize: Int?,
        search: String?
    ): List<String>? {
        return client.get(ServiceGitResource::class).getBranch(
            accessToken = getAndCheckOauthToken(userId),
            userId = userId,
            repository = URLEncoder.encode(pathWithNamespace, "UTF-8"),
            page = page,
            pageSize = pageSize,
            search = search
        ).data?.map { it.name }
    }

    override fun getFileContent(userId: String, pathWithNamespace: String, filePath: String, ref: String): String {
        return client.get(ServiceGitResource::class).getGitFileContent(
            token = getAndCheckOauthToken(userId),
            authType = RepoAuthType.OAUTH,
            repoName = URLEncoder.encode(pathWithNamespace, "UTF-8"),
            ref = ref,
            filePath = filePath
        ).data!!
    }

    override fun getFileNameTree(
        userId: String,
        pathWithNamespace: String,
        path: String?,
        ref: String?,
        recursive: Boolean
    ): List<String> {
        return client.get(ServiceGitResource::class).getGitFileTree(
            gitProjectId = URLEncoder.encode(pathWithNamespace, "UTF-8"),
            path = path ?: "",
            token = getAndCheckOauthToken(userId),
            ref = ref,
            recursive = recursive,
            tokenType = TokenTypeEnum.OAUTH
        ).data?.filter {
            it.type == "blob" && (
                it.name.endsWith(Constansts.devFileExtensionYaml) || it.name.endsWith(
                    Constansts.devFileExtensionYml
                )
                )
        }?.map { it.name } ?: emptyList()
    }

    override fun getAndCheckOauthToken(
        userId: String
    ): String {
        return client.get(ServiceOauthResource::class).gitGet(userId).data?.accessToken
            ?: throw OauthForbiddenException(
                message = "用户[$userId]尚未进行OAUTH授权，请先授权。"
            )
    }

    override fun getUserInfo(userId: String): GitUserInfo {
        return client.get(ServiceGitResource::class).getUserInfoByToken(
            token = getAndCheckOauthToken(userId),
            tokenType = TokenTypeEnum.OAUTH
        ).data!!
    }
}
