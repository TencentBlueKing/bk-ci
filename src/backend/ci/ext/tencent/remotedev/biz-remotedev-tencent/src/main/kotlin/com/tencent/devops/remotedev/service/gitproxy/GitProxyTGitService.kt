package com.tencent.devops.remotedev.service.gitproxy

import com.sun.org.slf4j.internal.LoggerFactory
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.client.Client
import com.tencent.devops.remotedev.dao.ProjectTGitLinkDao
import com.tencent.devops.remotedev.dao.WorkspaceJoinDao
import com.tencent.devops.remotedev.pojo.WorkspaceSearch
import com.tencent.devops.remotedev.pojo.WorkspaceSystemType
import com.tencent.devops.remotedev.pojo.common.QueryType
import com.tencent.devops.remotedev.service.BKItsmService
import com.tencent.devops.repository.api.ServiceOauthResource
import com.tencent.devops.repository.pojo.enums.GitAccessLevelEnum
import com.tencent.devops.repository.pojo.oauth.GitToken
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.URLEncoder

@Suppress("ALL")
@Service
class GitProxyTGitService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val workspaceJoinDao: WorkspaceJoinDao,
    private val projectTGitLinkDao: ProjectTGitLinkDao,
    private val bkitsmService: BKItsmService
) {

    @Value("\${tgit.gitUrl:}")
    private val tGitUrl: String = ""

    @Value("\${tgit.svnUrl:}")
    private val tSvnUrl: String = ""

    // 校验当前凭据的用户是否拥有连接项目的 master 及以上权限
    fun checkUserPermission(
        userId: String,
        projectId: String,
        codeProjectUrls: Set<String>
    ): Map<String, Boolean> {
        // 获取操作者 oauth
        // TODO: 返回一个特定的错误码返回前端用来做 oauth 认证
        val token = client.get(ServiceOauthResource::class).tGitGet(userId).data ?: throw ErrorCodeException(
            errorCode = "0",
            errorType = ErrorType.USER
        )

        val urls = codeProjectUrls.filter { it.isNotBlank() }.map { it.trim() }.toSet()

        val result = mutableMapOf<String, Boolean>()

        // 过滤 svn 项目，目前 SVN项目只能从根组创建项目，所以项目组的分割后俩项目的分割后三
        val svnProjectUrls = urls.filter {
            it.removeHttpPrefix().startsWith(tSvnUrl.removeHttpPrefix())
        }.toSet()
        if (svnProjectUrls.isNotEmpty()) {
            val noGroup = svnProjectUrls.all { it.split("/").filter { s -> s.isNotBlank() }.size == 3 }
            filterUrlPermission(svnProjectUrls, token, result, TGitProjectType.SVN, noGroup)
        }

        // 过滤 git 项目，git的项目结尾有.git不然都按项目组算
        val gitProjectUrls = urls.filter {
            it.removeHttpPrefix().startsWith(tGitUrl.removeHttpPrefix())
        }.toSet()
        if (gitProjectUrls.isNotEmpty()) {
            val noGroup = gitProjectUrls.all { it.endsWith(".git") }
            filterUrlPermission(urls, token, result, TGitProjectType.GIT, noGroup)
        }

        // TODO: 如果没有符合的选项，则告知前端
        if (result.isEmpty()) {
            return emptyMap()
        }

        // 关联项目，不符合要求的自动踢出去
        bkitsmService.createTicket(projectId, userId, result.filter { it.value }.keys)

        return result
    }

    private fun filterUrlPermission(
        projectUrls: Set<String>,
        token: GitToken,
        result: MutableMap<String, Boolean>,
        type: TGitProjectType,
        noGroup: Boolean
    ) {
        var page = 1
        val pageSize = 100
        while (true) {
            val projects = if (projectUrls.isNotEmpty()) {
                TGitApiClient.getProjectList(
                    accessToken = token.accessToken,
                    gitUrl = tGitUrl,
                    page = page,
                    pageSize = pageSize,
                    search = null,
                    minAccessLevel = GitAccessLevelEnum.MASTER,
                    type = type
                )
            } else {
                emptyList()
            }

            // 过滤项目信息
            projects.forEach { project ->
                projectUrls.forEach urls@{ projectUrl ->
                    if (project.httpsUrlToRepo.isNullOrBlank()) {
                        logger.warn("filterUrlPermission|httpsUrl is null $project")
                        return@urls
                    }
                    if (project.httpsUrlToRepo.removeHttpPrefix() != projectUrl &&
                        !project.httpsUrlToRepo.removeHttpPrefix().startsWith(projectUrl)
                    ) {
                        return@forEach
                    }
                    val level = GitAccessLevelEnum.MASTER.level
                    result[projectUrl.removeHttpPrefix()] = when {
                        (project.permissions?.projectAccess?.accessLevel ?: 0) >= level -> true
                        (project.permissions?.shareGroupAccess?.accessLevel ?: 0) >= level -> true
                        (project.permissions?.groupAccess?.accessLevel ?: 0) >= level -> true
                        else -> false
                    }

                    // 如果全都是项目判断那么只要项目判断完就可以退出
                    if (noGroup && result.keys.subtract(projectUrls).isEmpty()) {
                        return
                    }
                }
            }

            if (projects.size < 100) {
                break
            }
            page++
        }
    }

    fun linkTGit(
        userId: String,
        projectId: String,
        urls: Set<String>
    ): Map<String, Boolean> {
        // 获取oauth
        val token = client.get(ServiceOauthResource::class).tGitGet(userId).data
        if (token == null) {
            logger.error("linkTGit get $userId token null")
            return emptyMap()
        }

        val result = mutableMapOf<String, Boolean>()

        // 获取项目下正在跑的所有机器IP
        val ips = workspaceJoinDao.limitFetchProjectWorkspace(
            dslContext = dslContext,
            null,
            queryType = QueryType.WEB,
            search = WorkspaceSearch(
                projectId = listOf(projectId),
                workspaceSystemType = listOf(WorkspaceSystemType.WINDOWS_GPU),
                onFuzzyMatch = false
            )
        )?.filter { !it.hostName.isNullOrBlank() }?.map {
            it.hostName?.split(".")?.let { host ->
                host.subList(1, host.size).joinToString(separator = ".")
            }!!
        }?.toSet() ?: emptySet()

        urls.filter { it.isNotBlank() }.map { it.trim() }.forEach { url ->
            val fullPath = URLEncoder.encode(
                url.removeHttpPrefix()
                    .removePrefix(tGitUrl.removeHttpPrefix())
                    .removePrefix(tSvnUrl.removeHttpPrefix())
                    .removePrefix("/"),
                "UTF8"
            )

            val ok = TGitApiClient.addProjectAclIp(tGitUrl, token.accessToken, fullPath, ips)
            if (!ok) {
                result[url] = false
                return@forEach
            }

            projectTGitLinkDao.add(dslContext, projectId, url)
            result[url] = true
        }

        return result
    }

    fun tgitLinkList(
        projectId: String
    ): Set<String> {
        return projectTGitLinkDao.fetchUrl(dslContext, projectId)
    }

    private fun String.removeHttpPrefix() = this.removePrefix("https://").removePrefix("http://")

    companion object {
        private val logger = LoggerFactory.getLogger(GitProxyTGitService::class.java)
    }
}
