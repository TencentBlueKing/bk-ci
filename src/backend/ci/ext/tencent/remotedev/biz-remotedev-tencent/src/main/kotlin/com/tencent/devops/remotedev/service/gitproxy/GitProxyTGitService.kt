package com.tencent.devops.remotedev.service.gitproxy

import com.sun.org.slf4j.internal.LoggerFactory
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.client.Client
import com.tencent.devops.model.remotedev.tables.records.TProjectTgitLinkRecord
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
import okhttp3.Dns
import okhttp3.OkHttpClient
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.InetAddress
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
    ): Map<Long, Pair<String, Boolean>> {
        val token = client.get(ServiceOauthResource::class).tGitGet(userId).data ?: throw ErrorCodeException(
            errorCode = ErrorCodeEnum.NO_TGIT_OAUTH_ERROR.errorCode,
            errorType = ErrorCodeEnum.NO_TGIT_OAUTH_ERROR.errorType,
            params = arrayOf(userId, tGitUrl)
        )

        val urls = codeProjectUrls.filter { it.isNotBlank() }.map { it.trim() }.toSet()

        val result = mutableMapOf<Long, Pair<String, Boolean>>()

        // 过滤 svn 项目，目前 SVN项目只能从根组创建项目，所以项目组的分割后俩项目的分割后三
        val svnProjectUrls = urls.filter {
            it.removeHttpPrefix().startsWith(tSvnUrl.removeHttpPrefix())
        }.toSet()
        if (svnProjectUrls.isNotEmpty()) {
            val noGroup = svnProjectUrls.all { it.split("/").filter { s -> s.isNotBlank() }.size == 3 }
            filterUrlPermission(svnProjectUrls, token.accessToken, result, TGitProjectType.SVN, noGroup)
        }

        // 过滤 git 项目，git的项目结尾有.git不然都按项目组算
        val gitProjectUrls = urls.filter {
            it.removeHttpPrefix().startsWith(tGitUrl.removeHttpPrefix())
        }.toSet()
        if (gitProjectUrls.isNotEmpty()) {
            val noGroup = gitProjectUrls.all { it.endsWith(".git") }
            filterUrlPermission(gitProjectUrls, token.accessToken, result, TGitProjectType.GIT, noGroup)
        }

        if (result.isEmpty()) {
            return emptyMap()
        }

        // 关联项目，不符合要求的自动踢出去
        bkitsmService.createTicket(projectId, userId, result.filter { it.value.second })

        // 入库
        projectTGitLinkDao.batchAdd(
            dslContext = dslContext,
            projectId = projectId,
            urls = result.map {
                TGitRepoDaoData(
                    tgitId = it.key,
                    status = if (it.value.second) {
                        TGitRepoStatus.TO_BE_MIGRATED
                    } else {
                        TGitRepoStatus.ABNORMAL
                    },
                    oauthUser = userId,
                    gitType = if (it.value.first.removeHttpPrefix().startsWith(tSvnUrl.removeHttpPrefix())) {
                        TGitProjectType.SVN.name
                    } else {
                        TGitProjectType.GIT.name
                    }
                )
            }
        )

        return result
    }

    private fun filterUrlPermission(
        projectUrls: Set<String>,
        token: String,
        result: MutableMap<Long, Pair<String, Boolean>>,
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
                    accessToken = token,
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

                    result[project.id] = Pair(url.removeHttpPrefix(), true)

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
        repoIds: Map<Long, String>
    ): Map<Long, Boolean> {
        // 获取oauth
        val token = client.get(ServiceOauthResource::class).tGitGet(userId).data
        if (token == null) {
            logger.error("linkTGit get $userId token null")
            return emptyMap()
        }

        val result = mutableMapOf<Long, Boolean>()

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
        val repoMap = projectTGitLinkDao.fetch(dslContext, projectId).associate {
            it.tgitId to Pair(it.oauthUser, it.status)
        }

        repoIds.forEach { (repoId, url) ->
            if ((repoMap[repoId] != null) &&
                (repoMap[repoId]?.second == TGitRepoStatus.AVAILABLE.name) &&
                (repoMap[repoId]?.first == userId)
            ) {
                return@forEach
            }

            val ok = TGitApiClient.addProjectAclIp(
                client = okHttpClient,
                gitUrl = tGitUrl,
                accessToken = token.accessToken,
                projectId = repoId.toString(),
                ips = ips
            )
            result[repoId] = ok
            projectTGitLinkDao.add(
                dslContext = dslContext,
                projectId = projectId,
                tgitId = repoId,
                status = if (ok) {
                    TGitRepoStatus.AVAILABLE
                } else {
                    TGitRepoStatus.ABNORMAL
                },
                oauthUser = userId,
                gitType = if (url.removeHttpPrefix().startsWith(tSvnUrl.removeHttpPrefix())) {
                    TGitProjectType.SVN.name
                } else {
                    TGitProjectType.GIT.name
                }
            )
        }

        return result
    }

    fun tgitLinkList(
        projectId: String
    ): List<TGitRepoData> {
        val repos = projectTGitLinkDao.fetch(dslContext, projectId)
        if (repos.isEmpty()) {
            return emptyList()
        }

        // 防止因为未找到url导致页面未展示，所以以数据库数据为准，默认填入id
        val result = repos.map {
            TGitRepoData(
                repoId = it.tgitId,
                url = it.tgitId.toString(),
                status = TGitRepoStatus.fromStr(it.status)
            )
        }.associateBy { it.repoId }

        val svnData = mutableMapOf<String, MutableSet<Long>>()
        repos.filter { it.gitType == TGitProjectType.SVN.name }.forEach {
            if (svnData[it.oauthUser] == null) {
                svnData[it.oauthUser] = mutableSetOf(it.tgitId)
            } else {
                svnData[it.oauthUser]?.add(it.tgitId)
            }
        }
        val gitData = mutableMapOf<String, MutableSet<Long>>()
        repos.filter { it.gitType != TGitProjectType.SVN.name }.forEach {
            if (gitData[it.oauthUser] == null) {
                gitData[it.oauthUser] = mutableSetOf(it.tgitId)
            } else {
                gitData[it.oauthUser]?.add(it.tgitId)
            }
        }

        val tokenMap = mutableMapOf<String, String>()
        requestTGitRepoUrl(svnData, tokenMap, projectId, result, TGitProjectType.SVN)
        requestTGitRepoUrl(gitData, tokenMap, projectId, result, TGitProjectType.GIT)

        return result.values.toList()
    }

    private fun requestTGitRepoUrl(
        data: MutableMap<String, MutableSet<Long>>,
        tokenMap: MutableMap<String, String>,
        projectId: String,
        result: Map<Long, TGitRepoData>,
        type: TGitProjectType
    ) {
        data.forEach { (userId, repoIds) ->
            val token = if (tokenMap[userId] != null) {
                tokenMap[userId]
            } else {
                val newToken = client.get(ServiceOauthResource::class).tGitGet(userId).data
                if (newToken == null) {
                    logger.warn("addOrRemoveAclIp|get $projectId|$userId token is null")
                    return@forEach
                }
                tokenMap[userId] = newToken.accessToken
                newToken.accessToken
            } ?: return@forEach

            var page = 1
            val pageSize = 100

            while (true) {
                val projects = TGitApiClient.getProjectList(
                    client = okHttpClient,
                    gitUrl = tGitUrl,
                    accessToken = token,
                    page = page,
                    pageSize = pageSize,
                    search = null,
                    minAccessLevel = GitAccessLevelEnum.MASTER,
                    type = type
                )

                // 过滤项目信息
                projects.forEach projects@{ project ->
                    if (repoIds.contains(project.id)) {
                        result[project.id]?.url =
                            project.httpsUrlToRepo ?: project.httpUrlToRepo ?: project.id.toString()
                        repoIds.remove(project.id)
                    }
                }

                if (projects.size < 100 || repoIds.isEmpty()) {
                    break
                }
                page++
            }
        }
    }

    fun deleteTgitLink(
        userId: String,
        projectId: String,
        repoId: Long,
        url: String
    ): Boolean {
        val token = client.get(ServiceOauthResource::class).tGitGet(userId).data ?: throw ErrorCodeException(
            errorCode = ErrorCodeEnum.NO_TGIT_OAUTH_ERROR.errorCode,
            errorType = ErrorCodeEnum.NO_TGIT_OAUTH_ERROR.errorType,
            params = arrayOf(userId, tGitUrl)
        )

        val isSvn = url.removeHttpPrefix().startsWith(tSvnUrl.removeHttpPrefix())

        // 校验下是否有删除的权限，svn项目用户在根目录下是否是审批人，git项目校验是否有master及以上权限
        if (isSvn) {
            val svnRs = TGitApiClient.getSvnProjectAuth(
                client = okHttpClient,
                gitUrl = tGitUrl,
                accessToken = token.accessToken,
                projectId = repoId.toString()
            )
            if (svnRs?.approverUsers?.any { it.username == userId } != true) {
                throw ErrorCodeException(
                    errorCode = ErrorCodeEnum.NO_TGIT_PREMISSION.errorCode,
                    params = arrayOf(userId)
                )
            }
        } else {
            val gitRs = TGitApiClient.getProjectMemberAll(
                client = okHttpClient,
                gitUrl = tGitUrl,
                accessToken = token.accessToken,
                projectId = repoId.toString(),
                userId = userId
            )
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
            projectId = repoId.toString(),
            ips = emptySet()
        )

        if (ok) {
            projectTGitLinkDao.deleteUrl(dslContext, projectId, repoId)
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

                    val config = TGitApiClient.getProjectAcl(
                        client = okHttpClient,
                        gitUrl = tGitUrl,
                        accessToken = token!!,
                        projectId = repo.tgitId.toString()
                    )
                    if (config == null) {
                        logger.warn("addOrRemoveAclIp|get $projectId|${repo.projectId} acl config error")
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
                        projectId = repo.tgitId.toString(),
                        ips = ips
                    )
                }
        }
    }

    fun migrateTGitData(projectId: String?) {
        val res = projectTGitLinkDao.fetchOld(dslContext, projectId)
        val recordData = mutableMapOf<String, MutableList<TProjectTgitLinkRecord>>()
        res.forEach {
            if (recordData[it.projectId] == null) {
                recordData[it.projectId] = mutableListOf(it)
            } else {
                recordData[it.projectId]?.add(it)
            }
        }

        recordData.forEach { (projectId, records) ->
            val recordsMap = records.associateBy { it.url.trim() }

            val svnData = mutableMapOf<String, MutableSet<String>>()
            records.filter { it.url.removeHttpPrefix().startsWith(tSvnUrl.removeHttpPrefix()) }.forEach {
                if (svnData[it.oauthUser] == null) {
                    svnData[it.oauthUser] = mutableSetOf(it.url)
                } else {
                    svnData[it.oauthUser]?.add(it.url)
                }
            }
            val gitData = mutableMapOf<String, MutableSet<String>>()
            records.filter { !it.url.removeHttpPrefix().startsWith(tSvnUrl.removeHttpPrefix()) }.forEach {
                if (gitData[it.oauthUser] == null) {
                    gitData[it.oauthUser] = mutableSetOf(it.url)
                } else {
                    gitData[it.oauthUser]?.add(it.url)
                }
            }

            val tokenMap = mutableMapOf<String, String>()

            val result = mutableMapOf<Long, Pair<String, Boolean>>()

            // 过滤 git 项目
            gitData.forEach gitForEach@{ (userId, urls) ->
                val token = if (tokenMap[userId] != null) {
                    tokenMap[userId]
                } else {
                    val newToken = client.get(ServiceOauthResource::class).tGitGet(userId).data
                    if (newToken == null) {
                        logger.warn("addOrRemoveAclIp|get $projectId|$userId token is null")
                        return@gitForEach
                    }
                    tokenMap[userId] = newToken.accessToken
                    newToken.accessToken
                } ?: return@gitForEach
                if (urls.isNotEmpty()) {
                    filterUrlPermission(urls, token, result, TGitProjectType.GIT, true)
                }
                // 入库
                projectTGitLinkDao.batchAdd(
                    dslContext = dslContext,
                    projectId = projectId,
                    urls = result.map {
                        TGitRepoDaoData(
                            tgitId = it.key,
                            status = if (it.value.first.trim() in recordsMap) {
                                TGitRepoStatus.fromStr(recordsMap[it.value.first.trim()]?.status ?: "")
                            } else {
                                if (it.value.second) {
                                    TGitRepoStatus.AVAILABLE
                                } else {
                                    TGitRepoStatus.ABNORMAL
                                }
                            },
                            oauthUser = userId,
                            gitType = if (it.value.first.removeHttpPrefix().startsWith(tSvnUrl.removeHttpPrefix())) {
                                TGitProjectType.SVN.name
                            } else {
                                TGitProjectType.GIT.name
                            }
                        )
                    }
                )
            }

            // 过滤 svn 项目
            svnData.forEach svnForEach@{ (userId, urls) ->
                val token = if (tokenMap[userId] != null) {
                    tokenMap[userId]
                } else {
                    val newToken = client.get(ServiceOauthResource::class).tGitGet(userId).data
                    if (newToken == null) {
                        logger.warn("addOrRemoveAclIp|get $projectId|$userId token is null")
                        return@svnForEach
                    }
                    tokenMap[userId] = newToken.accessToken
                    newToken.accessToken
                } ?: return@svnForEach
                if (urls.isNotEmpty()) {
                    filterUrlPermission(urls, token, result, TGitProjectType.SVN, true)
                }
                // 入库
                projectTGitLinkDao.batchAdd(
                    dslContext = dslContext,
                    projectId = projectId,
                    urls = result.map {
                        TGitRepoDaoData(
                            tgitId = it.key,
                            status = if (it.value.first.trim() in recordsMap) {
                                TGitRepoStatus.fromStr(recordsMap[it.value.first.trim()]?.status ?: "")
                            } else {
                                if (it.value.second) {
                                    TGitRepoStatus.AVAILABLE
                                } else {
                                    TGitRepoStatus.ABNORMAL
                                }
                            },
                            oauthUser = userId,
                            gitType = if (it.value.first.removeHttpPrefix().startsWith(tSvnUrl.removeHttpPrefix())) {
                                TGitProjectType.SVN.name
                            } else {
                                TGitProjectType.GIT.name
                            }
                        )
                    }
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
