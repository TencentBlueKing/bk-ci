package com.tencent.devops.remotedev.service.gitproxy

import com.sun.org.slf4j.internal.LoggerFactory
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.client.Client
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.dao.ProjectTGitLinkDao
import com.tencent.devops.remotedev.dao.WorkspaceJoinDao
import com.tencent.devops.remotedev.pojo.TGitRepoDaoData
import com.tencent.devops.remotedev.pojo.WorkspaceSearch
import com.tencent.devops.remotedev.pojo.WorkspaceSystemType
import com.tencent.devops.remotedev.pojo.common.QueryType
import com.tencent.devops.remotedev.pojo.gitproxy.TGitRepoData
import com.tencent.devops.remotedev.pojo.gitproxy.TGitRepoStatus
import com.tencent.devops.remotedev.service.BKItsmService
import com.tencent.devops.repository.api.ServiceOauthResource
import com.tencent.devops.repository.pojo.enums.GitAccessLevelEnum
import com.tencent.devops.repository.pojo.oauth.GitToken
import okhttp3.Dns
import okhttp3.OkHttpClient
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.InetAddress
import java.net.URLEncoder
import java.security.cert.CertificateException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

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

    @Value("\${tgit.ip:}")
    private val tGitIp: String = ""

    // 校验当前凭据的用户是否拥有连接项目的 master 及以上权限
    fun checkUserPermission(
        userId: String,
        projectId: String,
        codeProjectUrls: Set<String>
    ): Map<String, Boolean> {
        val token = client.get(ServiceOauthResource::class).tGitGet(userId).data ?: throw ErrorCodeException(
            errorCode = ErrorCodeEnum.NO_TGIT_OAUTH_ERROR.errorCode,
            errorType = ErrorCodeEnum.NO_TGIT_OAUTH_ERROR.errorType,
            params = arrayOf(userId, tGitUrl)
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
            filterUrlPermission(gitProjectUrls, token, result, TGitProjectType.GIT, noGroup)
        }

        if (result.isEmpty()) {
            return emptyMap()
        }

        // 关联项目，不符合要求的自动踢出去
        bkitsmService.createTicket(projectId, userId, result.filter { it.value }.keys)

        // 入库
        projectTGitLinkDao.batchAdd(
            dslContext = dslContext,
            projectId = projectId,
            urls = result.map {
                TGitRepoDaoData(
                    url = it.key,
                    status = if (it.value) {
                        TGitRepoStatus.TO_BE_MIGRATED
                    } else {
                        TGitRepoStatus.ABNORMAL
                    },
                    oauthUser = userId
                )
            }
        )

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
        val rProjectUrls = projectUrls.map { it.removeHttpPrefix() }.toSet()
        while (true) {
            val projects = if (rProjectUrls.isNotEmpty()) {
                TGitApiClient.getProjectList(
                    client = okHttpClient,
                    gitUrl = tGitUrl,
                    accessToken = token.accessToken,
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
            projects.forEach projects@{ project ->
                rProjectUrls.forEach urls@{ projectUrl ->
                    if (project.httpsUrlToRepo.isNullOrBlank() && project.httpUrlToRepo.isNullOrBlank()) {
                        logger.warn("filterUrlPermission|httpsUrl is null $project")
                        return@projects
                    }
                    val url = project.httpsUrlToRepo ?: project.httpUrlToRepo
                    if (url?.removeHttpPrefix() != projectUrl &&
                        url?.removeHttpPrefix()?.startsWith(projectUrl) != true
                    ) {
                        return@urls
                    }

                    result[url.removeHttpPrefix()] = true

                    // 如果全都是项目判断那么只要项目判断完就可以退出
                    if (noGroup && rProjectUrls.subtract(result.keys).isEmpty()) {
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

    /**
     * OP回调链接工蜂acl
     */
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

        // 获取关联的工蜂仓库
        val repoMap =
            projectTGitLinkDao.fetch(dslContext, projectId).associate { it.url to Pair(it.oauthUser, it.status) }

        urls.filter { it.isNotBlank() }.map { it.trim() }.forEach { url ->
            if ((repoMap[url] != null) &&
                (repoMap[url]?.second == TGitRepoStatus.AVAILABLE.name) &&
                (repoMap[url]?.first == userId)
            ) {
                return@forEach
            }

            val fullPath = URLEncoder.encode(
                url.removeHttpPrefix()
                    .removePrefix(tGitUrl.removeHttpPrefix())
                    .removePrefix(tSvnUrl.removeHttpPrefix())
                    .removePrefix("/"),
                "UTF8"
            )

            val ok = TGitApiClient.addProjectAclIp(
                client = okHttpClient,
                gitUrl = tGitUrl,
                accessToken = token.accessToken,
                projectId = fullPath,
                ips = ips
            )
            result[url] = ok
            projectTGitLinkDao.add(
                dslContext = dslContext,
                projectId = projectId,
                url = url,
                status = if (ok) {
                    TGitRepoStatus.AVAILABLE
                } else {
                    TGitRepoStatus.ABNORMAL
                },
                oauthUser = userId
            )
        }

        return result
    }

    fun tgitLinkList(
        projectId: String
    ): List<TGitRepoData> {
        return projectTGitLinkDao.fetch(dslContext, projectId).map {
            TGitRepoData(
                url = it.url,
                status = TGitRepoStatus.fromStr(it.status)
            )
        }
    }

    fun deleteTgitLink(
        userId: String,
        projectId: String,
        url: String
    ): Boolean {
        val token = client.get(ServiceOauthResource::class).tGitGet(userId).data ?: throw ErrorCodeException(
            errorCode = ErrorCodeEnum.NO_TGIT_OAUTH_ERROR.errorCode,
            errorType = ErrorCodeEnum.NO_TGIT_OAUTH_ERROR.errorType,
            params = arrayOf(userId, tGitUrl)
        )

        val isSvn = url.removeHttpPrefix().startsWith(tSvnUrl.removeHttpPrefix())

        val fullPath = URLEncoder.encode(
            url.removeHttpPrefix()
                .removePrefix(tGitUrl.removeHttpPrefix())
                .removePrefix(tSvnUrl.removeHttpPrefix())
                .removePrefix("/"),
            "UTF8"
        )

        // 校验下是否有删除的权限，svn项目用户在根目录下是否是审批人，git项目校验是否有master及以上权限
        if (isSvn) {
            val svnRs = TGitApiClient.getSvnProjectAuth(okHttpClient, tGitUrl, token.accessToken, fullPath)
            if (svnRs?.approverUsers?.any { it.username == userId } != true) {
                throw ErrorCodeException(
                    errorCode = ErrorCodeEnum.NO_TGIT_PREMISSION.errorCode,
                    params = arrayOf(userId)
                )
            }
        } else {
            val gitRs = TGitApiClient.getProjectMemberAll(okHttpClient, tGitUrl, token.accessToken, fullPath, userId)
            if (gitRs?.any {
                it.username == userId && (it.accessLevel ?: 0) >= GitAccessLevelEnum.MASTER.level
            } != true
            ) {
                throw ErrorCodeException(
                    errorCode = ErrorCodeEnum.NO_TGIT_PREMISSION.errorCode,
                    params = arrayOf(userId)
                )
            }
        }

        val ok = TGitApiClient.addProjectAclIp(
            client = okHttpClient,
            gitUrl = tGitUrl,
            accessToken = token.accessToken,
            projectId = fullPath,
            ips = emptySet()
        )

        if (ok) {
            projectTGitLinkDao.deleteUrl(dslContext, projectId, url)
        }

        return ok
    }

    /**
     * 创建和删除云桌面时同步
     */
    private val executor = Executors.newCachedThreadPool()
    fun addOrRemoveAclIp(
        projectId: String,
        ip: String,
        remove: Boolean
    ) {
        executor.execute {
            val tokenMap = mutableMapOf<String, String>()
            projectTGitLinkDao.fetch(dslContext, projectId)
                .filter { it.status == TGitRepoStatus.AVAILABLE.name }
                .forEach { repo ->
                    val token = if (tokenMap[repo.oauthUser] != null) {
                        tokenMap[repo.oauthUser]
                    } else {
                        val newToken = client.get(ServiceOauthResource::class).tGitGet(repo.oauthUser).data
                        if (newToken == null) {
                            logger.warn("addOrRemoveAclIp|get $projectId|${repo.oauthUser} token is null")
                            return@forEach
                        }
                        tokenMap[repo.oauthUser] = newToken.accessToken
                        newToken.accessToken
                    }

                    val fullPath = URLEncoder.encode(
                        repo.url.removeHttpPrefix()
                            .removePrefix(tGitUrl.removeHttpPrefix())
                            .removePrefix(tSvnUrl.removeHttpPrefix())
                            .removePrefix("/"),
                        "UTF8"
                    )

                    val config = TGitApiClient.getProjectAcl(
                        client = okHttpClient,
                        gitUrl = tGitUrl,
                        accessToken = token!!,
                        projectId = fullPath
                    )
                    if (config == null) {
                        logger.warn("addOrRemoveAclIp|get $projectId|$fullPath acl config error")
                        return@forEach
                    }

                    val ips = config.allowIps.split(";").filter { it.isNotBlank() }.toMutableSet()
                    if (remove) {
                        ips.remove(ip)
                    } else {
                        ips.add(ip)
                    }
                    TGitApiClient.addProjectAclIp(
                        client = okHttpClient,
                        gitUrl = tGitUrl,
                        accessToken = token,
                        projectId = fullPath,
                        ips = ips
                    )
                }
        }
    }

    private fun String.removeHttpPrefix() = this.removePrefix("https://").removePrefix("http://")

    companion object {
        private val logger = LoggerFactory.getLogger(GitProxyTGitService::class.java)
        private fun sslSocketFactory(): SSLSocketFactory {
            try {
                val sslContext = SSLContext.getInstance("SSL")
                sslContext.init(null, trustAllCerts, java.security.SecureRandom())
                return sslContext.socketFactory
            } catch (ingored: Exception) {
                throw RemoteServiceException(ingored.message!!)
            }
        }

        private val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            @Throws(CertificateException::class)
            override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {
            }

            @Throws(CertificateException::class)
            override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {
            }

            override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> {
                return arrayOf()
            }
        })
    }

    private lateinit var okHttpClient: OkHttpClient

    @PostConstruct
    private fun init() {
        okHttpClient = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .sslSocketFactory(sslSocketFactory(), trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .dns(
                TGitDns(
                    url = tGitUrl.removeHttpPrefix(),
                    ips = tGitIp.split(";").filter { it.isNotBlank() }.toSet()
                )
            )
            .build()
    }
}

class TGitDns(
    private val url: String,
    private val ips: Set<String>
) : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        return if (hostname == url) {
            // 返回特殊的IP地址
            ips.map { InetAddress.getByName(it) }
        } else {
            // 对于其他主机名使用系统默认的DNS解析
            Dns.SYSTEM.lookup(hostname)
        }
    }
}
