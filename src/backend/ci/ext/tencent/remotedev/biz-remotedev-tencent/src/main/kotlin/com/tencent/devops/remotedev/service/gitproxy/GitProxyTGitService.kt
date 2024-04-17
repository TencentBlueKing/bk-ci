package com.tencent.devops.remotedev.service.gitproxy

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.notify.enums.NotifyType
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.model.remotedev.tables.records.TProjectTgitIdLinkRecord
import com.tencent.devops.notify.api.service.ServiceNotifyMessageTemplateResource
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.config.TGitConfig
import com.tencent.devops.remotedev.dao.ProjectTGitLinkDao
import com.tencent.devops.remotedev.dao.WorkspaceJoinDao
import com.tencent.devops.remotedev.pojo.TGitRepoDaoData
import com.tencent.devops.remotedev.pojo.WorkspaceSearch
import com.tencent.devops.remotedev.pojo.WorkspaceSystemType
import com.tencent.devops.remotedev.pojo.common.QueryType
import com.tencent.devops.remotedev.pojo.gitproxy.CreateTGitProjectInfo
import com.tencent.devops.remotedev.pojo.gitproxy.TGitNamespace
import com.tencent.devops.remotedev.pojo.gitproxy.TGitRepoData
import com.tencent.devops.remotedev.pojo.gitproxy.TGitRepoStatus
import com.tencent.devops.remotedev.service.BKItsmService
import com.tencent.devops.repository.api.ServiceOauthResource
import com.tencent.devops.repository.pojo.enums.GitAccessLevelEnum
import com.tencent.devops.repository.pojo.oauth.GitToken
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.concurrent.Executors

@Suppress("ALL")
@Service
class GitProxyTGitService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val workspaceJoinDao: WorkspaceJoinDao,
    private val projectTGitLinkDao: ProjectTGitLinkDao,
    private val bkitsmService: BKItsmService,
    private val offshoreTGitApiClient: OffshoreTGitApiClient,
    private val tGitConfig: TGitConfig,
    private val redisOperation: RedisOperation
) {
    // 校验当前凭据的用户是否拥有连接项目的 master 及以上权限
    fun checkUserPermission(
        userId: String,
        projectId: String,
        codeProjectUrls: Set<String>
    ): Map<String, Boolean> {
        val token = client.get(ServiceOauthResource::class).tGitGet(userId).data ?: throw ErrorCodeException(
            errorCode = ErrorCodeEnum.NO_TGIT_OAUTH_ERROR.errorCode,
            errorType = ErrorCodeEnum.NO_TGIT_OAUTH_ERROR.errorType,
            params = arrayOf(userId, tGitConfig.tGitUrl)
        )

        val urls = codeProjectUrls.filter { it.isNotBlank() }.map { it.trim().removeHttpPrefix() }.toSet()

        val result = mutableMapOf<Long, Pair<String, Boolean>>()

        // 过滤 svn 项目，目前 SVN项目只能从根组创建项目，所以项目组的分割后俩，项目的分割后三
        val svnProjectUrls = urls.filter {
            it.startsWith(tGitConfig.tSvnUrl.removeHttpPrefix())
        }.toSet()
        val noGroupSvnUrls = svnProjectUrls.filter { it.split("/").filter { s -> s.isNotBlank() }.size == 3 }
        if (svnProjectUrls.isNotEmpty()) {
            filterUrlPermission(
                projectUrls = svnProjectUrls,
                token = token.accessToken,
                result = result,
                type = TGitProjectType.SVN,
                noGroup = (noGroupSvnUrls.size == svnProjectUrls.size)
            )
        }

        // 过滤 git 项目，git的项目结尾有.git不然都按项目组算
        val gitProjectUrls = urls.filter {
            it.startsWith(tGitConfig.tGitUrl.removeHttpPrefix())
        }.toSet()
        val noGroupGitUrls = gitProjectUrls.filter { it.endsWith(".git") }
        if (gitProjectUrls.isNotEmpty()) {
            filterUrlPermission(
                projectUrls = gitProjectUrls,
                token = token.accessToken,
                result = result,
                type = TGitProjectType.GIT,
                noGroup = (gitProjectUrls.size == noGroupGitUrls.size)
            )
        }

        // 说明没有一个成功的
        if (result.isEmpty()) {
            return urls.associateWith { false }
        }

        // 关联项目，不符合要求的自动踢出去
        bkitsmService.createTicket(projectId, userId, result.filter { it.value.second })

        // 入库
        projectTGitLinkDao.batchAdd(
            dslContext = dslContext,
            projectId = projectId,
            data = result.map {
                TGitRepoDaoData(
                    tgitId = it.key,
                    status = if (it.value.second) {
                        TGitRepoStatus.TO_BE_MIGRATED
                    } else {
                        TGitRepoStatus.ABNORMAL
                    },
                    oauthUser = userId,
                    gitType = if (it.value.first.removeHttpPrefix().startsWith(tGitConfig.tSvnUrl.removeHttpPrefix())) {
                        TGitProjectType.SVN.name
                    } else {
                        TGitProjectType.GIT.name
                    },
                    url = it.value.first.removeHttpPrefix()
                )
            }
        )

        // 过滤下成功的和不成功的
        val sucUrls = result.values.map { it.first }.toSet()
        val allResult = mutableMapOf<String, Boolean>()
        // url需要先把组去掉，不然永远都会过滤到组
        val realUrls = noGroupGitUrls + noGroupSvnUrls
        allResult.putAll(realUrls.subtract(sucUrls).associateWith { false })
        allResult.putAll(result.values.associate { it.first to it.second })

        return allResult
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
                offshoreTGitApiClient.getProjectList(
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
                    val url = (project.httpsUrlToRepo ?: project.httpUrlToRepo)?.removeHttpPrefix()
                    if ((url != projectUrl) && (url?.startsWith("${projectUrl.removeSuffix("/")}/") != true)) {
                        return@urls
                    }

                    result[project.id] = Pair(url, true)

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

        // 获取项目下正在跑的所有机器的用户
        val users = workspaceJoinDao.fetchProjectSharedUser(dslContext, projectId)

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

            val ok = updateTGitProjectAcl(
                token = token.accessToken,
                tGitProjectId = repoId.toString(),
                ips = ips,
                users = users
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
                gitType = if (url.removeHttpPrefix().startsWith(tGitConfig.tSvnUrl.removeHttpPrefix())) {
                    TGitProjectType.SVN.name
                } else {
                    TGitProjectType.GIT.name
                },
                url = url.removeHttpPrefix()
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

        // 防止因为未找到url导致页面未展示，所以以数据库数据为准，默认填入数据库有的url
        val result = repos.map {
            TGitRepoData(
                repoId = it.tgitId,
                url = it.url ?: it.tgitId.toString(),
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
                val projects = offshoreTGitApiClient.getProjectList(
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
                        if (!project.httpsUrlToRepo.isNullOrBlank() || !project.httpUrlToRepo.isNullOrBlank()) {
                            result[project.id]?.url = project.httpsUrlToRepo ?: project.httpUrlToRepo!!
                        }
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
            params = arrayOf(userId, tGitConfig.tGitUrl)
        )

        val isSvn = url.removeHttpPrefix().startsWith(tGitConfig.tSvnUrl.removeHttpPrefix())

        // 校验下是否有删除的权限，svn项目用户在根目录下是否是审批人，git项目校验是否有master及以上权限
        if (isSvn) {
            val svnRs = offshoreTGitApiClient.getSvnProjectAuth(
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
            val gitRs = offshoreTGitApiClient.getProjectMemberAll(
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

        val ok = updateTGitProjectAcl(
            token = token.accessToken,
            tGitProjectId = repoId.toString(),
            ips = emptySet(),
            users = emptySet()
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
            fetchProjectTGit(projectId) { repo, token ->
                val config = offshoreTGitApiClient.getProjectAcl(
                    accessToken = token,
                    projectId = repo.tgitId.toString()
                )
                if (config == null) {
                    logger.warn("addOrRemoveAclIp|get $projectId|${repo.projectId} acl config error")
                    return@fetchProjectTGit
                }

                val ips = config.allowIps.split(";").filter { it.isNotBlank() }.toMutableSet()
                if (remove) {
                    ips.remove(ip)
                } else {
                    ips.add(ip)
                }
                updateTGitProjectAcl(
                    token = token,
                    tGitProjectId = repo.tgitId.toString(),
                    ips = ips,
                    users = null
                )
            }
        }
    }

    private val publicIpsCache: LoadingCache<String, String> = Caffeine.newBuilder()
        .maximumSize(5)
        .expireAfterWrite(Duration.ofHours(1))
        .build { key -> redisOperation.get(key, isDistinguishCluster = false) ?: "" }

    // 默认规则组仅放云桌面 IP, 并清空用户白名单
    // 分配了云桌面的用户都加到特定访问人群名单
    // 特殊规则组 IP 加上云桌面 IP 及所有公网 IP（过渡期，后续会移除公网 IP）
    private fun updateTGitProjectAcl(
        token: String,
        tGitProjectId: String,
        ips: Set<String>,
        users: Set<String>?
    ): Boolean {
        val ipOk = offshoreTGitApiClient.updateProjectAclIp(token, tGitProjectId, ips)
        val userOk = offshoreTGitApiClient.updateProjectAclUser(token, tGitProjectId, emptySet())
        val specUserOk = if (users != null) {
            offshoreTGitApiClient.updateProjectAclSpecUser(token, tGitProjectId, users)
        } else {
            true
        }

        val specIpOk = offshoreTGitApiClient.updateProjectAclSpecIps(
            accessToken = token,
            projectId = tGitProjectId,
            ips = ips.plus(
                publicIpsCache.get(REDIS_REMOTEDEV_PUBLIC_IPS)?.split(";")?.filter { it.isNotBlank() }?.toSet()
                    ?: emptySet()
            )
        )

        return ipOk && userOk && specUserOk && specIpOk
    }

    // 被分配的用户是工作空间下的一个子集，所以无法根据单个工作空间的人员变更得知整个项目的人员变更
    fun refreshProjectTGitSpecUser(
        projectId: String
    ) {
        executor.execute {
            // 获取项目下正在跑的所有机器的用户
            val users = workspaceJoinDao.fetchProjectSharedUser(dslContext, projectId)
            fetchProjectTGit(projectId) { repo, token ->
                offshoreTGitApiClient.updateProjectAclSpecUser(token, repo.tgitId.toString(), users)
            }
        }
    }

    private fun fetchProjectTGit(
        projectId: String,
        run: (repo: TProjectTgitIdLinkRecord, token: String) -> Unit
    ) {
        val tokenMap = mutableMapOf<String, String>()
        projectTGitLinkDao.fetch(dslContext, projectId)
            .filter { it.status == TGitRepoStatus.AVAILABLE.name }
            .forEach { repo ->
                val token = if (tokenMap[repo.oauthUser] != null) {
                    tokenMap[repo.oauthUser]
                } else {
                    val newToken = client.get(ServiceOauthResource::class).tGitGet(repo.oauthUser).data
                    if (newToken == null) {
                        logger.warn("fetchProjectTGit|get $projectId|${repo.oauthUser} token is null")
                        return@forEach
                    }
                    tokenMap[repo.oauthUser] = newToken.accessToken
                    newToken.accessToken
                }

                run(repo, token!!)
            }
    }

    /**
     * 检查关联的TGit仓库的管理员的权限是否过期
     */
    @Scheduled(cron = "0 50 9 * * ?")
    fun dailyUserAuthDoCheck() {
        val res = projectTGitLinkDao.fetchAll(dslContext)
        val recordData = mutableMapOf<String, MutableList<TProjectTgitIdLinkRecord>>()
        res.forEach {
            if (recordData[it.oauthUser] == null) {
                recordData[it.oauthUser] = mutableListOf(it)
            } else {
                recordData[it.oauthUser]?.add(it)
            }
        }

        val result = mutableMapOf<String, MutableMap<String, MutableMap<Long, String>>>()
        recordData.forEach { (userId, records) ->
            val svnRecords =
                records.filter { it.gitType == TGitProjectType.SVN.name }.associateBy { it.tgitId }.toMutableMap()
            val gitRecords =
                records.filter { it.gitType != TGitProjectType.SVN.name }.associateBy { it.tgitId }.toMutableMap()

            val token = client.get(ServiceOauthResource::class).tGitGet(userId).data
            if (token == null) {
                logger.warn("TGitLinkAuthCheck|get $userId token is null")
                return@forEach
            }

            filterNoAuthTGitProject(gitRecords, token, result, userId, TGitProjectType.GIT)
            filterNoAuthTGitProject(svnRecords, token, result, userId, TGitProjectType.SVN)
        }

        val projectCodes = result.values.flatMap { it.keys }.toSet()
        val projects = client.get(ServiceProjectResource::class)
            .listByProjectCode(projectCodes).data?.associateBy { it.projectCode }
        if (projects.isNullOrEmpty()) {
            logger.warn("dailyUserAuthDoCheck|$projectCodes listByProjectCode null")
            return
        }
        logger.debug("dailyUserAuthDoCheck|$projectCodes")

        result.forEach { (userId, projectAndIds) ->
            projectAndIds.forEach project@{ (projectId, idAndUrls) ->
                val project = projects[projectId]
                if (project == null) {
                    logger.warn("dailyUserAuthDoCheck|$projectId is null")
                    return@project
                }
                projectTGitLinkDao.batchUpdateStatus(dslContext, projectId, idAndUrls.keys, TGitRepoStatus.ABNORMAL)
                client.get(ServiceNotifyMessageTemplateResource::class).sendNotifyMessageByTemplate(
                    SendNotifyMessageTemplateRequest(
                        templateCode = tGitConfig.expiredPermTmpCode,
                        receivers = mutableSetOf(userId),
                        notifyType = mutableSetOf(NotifyType.EMAIL.name),
                        bodyParams = mapOf(
                            "userId" to userId,
                            "urls" to idAndUrls.keys.joinToString(separator = "\n"),
                            "projectId" to projectId,
                            "projectName" to project.projectName
                        ),
                        cc = project.properties?.remotedevManager
                            ?.split(";")?.filter { it.isNotBlank() }
                            ?.toMutableSet()
                    )
                )
            }
        }
    }

    private fun filterNoAuthTGitProject(
        records: MutableMap<Long, TProjectTgitIdLinkRecord>,
        token: GitToken,
        result: MutableMap<String, MutableMap<String, MutableMap<Long, String>>>,
        userId: String,
        type: TGitProjectType
    ) {
        if (records.isEmpty()) {
            return
        }

        var page = 1
        val pageSize = 100
        while (true) {
            val projects = offshoreTGitApiClient.getProjectList(
                accessToken = token.accessToken,
                page = page,
                pageSize = pageSize,
                search = null,
                minAccessLevel = GitAccessLevelEnum.MASTER,
                type = type
            )
            projects.forEach projects@{ project ->
                if (project.id in records.keys) {
                    // url发生变化时更新url
                    if ((records[project.id] != null) &&
                        !(project.httpsUrlToRepo ?: project.httpUrlToRepo).isNullOrBlank() &&
                        (project.httpsUrlToRepo ?: project.httpUrlToRepo) != records[project.id]?.url
                    ) {
                        projectTGitLinkDao.updateUrl(
                            dslContext = dslContext,
                            projectId = records[project.id]!!.projectId,
                            tgitId = project.id,
                            url = (project.httpsUrlToRepo ?: project.httpUrlToRepo)!!.removeHttpPrefix()
                        )
                    }
                    records.remove(project.id)
                }
            }

            if (projects.size < 100) {
                break
            }
            page++
        }

        val gitResult = if (result[userId] == null) {
            result[userId] = mutableMapOf()
            result[userId]!!
        } else {
            result[userId]!!
        }
        records.values.forEach { record ->
            if (gitResult[record.projectId] == null) {
                gitResult[record.projectId] = mutableMapOf(record.tgitId to record.url)
            } else {
                gitResult[record.projectId]?.set(record.tgitId, record.url)
            }
        }
    }

    fun getTGitNamespaces(
        userId: String,
        page: Int,
        pageSize: Int,
        svnProject: Boolean
    ): List<TGitNamespace> {
        val token = client.get(ServiceOauthResource::class).tGitGet(userId).data ?: throw ErrorCodeException(
            errorCode = ErrorCodeEnum.NO_TGIT_OAUTH_ERROR.errorCode,
            errorType = ErrorCodeEnum.NO_TGIT_OAUTH_ERROR.errorType,
            params = arrayOf(userId, tGitConfig.tGitUrl)
        )
        if (!svnProject) {
            return offshoreTGitApiClient.getNamespaces(token.accessToken, page, pageSize)
        }

        val userResult = mutableListOf<TGitNamespace>()
        val result = mutableListOf<TGitNamespace>()
        var fetchPage = 1
        val fetchPageSize = 100
        // 工作空间每次会带一个个人工作空间
        val maxSize = page * pageSize + 1
        while (true) {
            val request = offshoreTGitApiClient.getNamespaces(token.accessToken, fetchPage, fetchPageSize)
            // 第一次先把个人项目添加进去，后续都不添加
            if (userResult.isEmpty()) {
                userResult.addAll(request.filter { it.kind == TGitNamespaceKind.USER.text })
            }

            result.addAll(request.filter { it.kind != TGitNamespaceKind.USER.text && it.parentId == null })

            // 没有多余的页数就直接退出
            if ((request.size - userResult.size) < pageSize) {
                break
            }

            // 判断筛选的总量是否到达了需要分页的总数
            if (result.size == maxSize) {
                break
            }

            // 都不满足就继续
            fetchPage += 1
        }

        val startIndex = (page - 1) * pageSize
        val endIndex = minOf(startIndex + pageSize, result.size)
        userResult.addAll(result.subList(startIndex, endIndex))

        return userResult
    }

    fun createProjectAndLinkTGit(
        userId: String,
        projectId: String,
        info: CreateTGitProjectInfo
    ): Boolean {
        val token = client.get(ServiceOauthResource::class).tGitGet(userId).data ?: throw ErrorCodeException(
            errorCode = ErrorCodeEnum.NO_TGIT_OAUTH_ERROR.errorCode,
            errorType = ErrorCodeEnum.NO_TGIT_OAUTH_ERROR.errorType,
            params = arrayOf(userId, tGitConfig.tGitUrl)
        )

        val data = offshoreTGitApiClient.createProject(token.accessToken, info.name, info.namespaceId, info.svnProject)
        val res = linkTGit(userId, projectId, mapOf(data.id to (data.httpsUrlToRepo ?: data.httpUrlToRepo ?: "")))
        return res[data.id] ?: false
    }

    private fun String.removeHttpPrefix() = this.removePrefix("https://").removePrefix("http://")

    fun refreshTGitAcl(projectId: String?) {
        executor.execute {
            logger.info("OP|refreshTGitAcl|start refreshTGitAcl")
            val projects = if (projectId.isNullOrBlank()) {
                projectTGitLinkDao.fetchAll(dslContext).map { it.projectId }.toSet()
            } else {
                setOf(projectId)
            }
            projects.forEach { projectId ->
                logger.info("OP|refreshTGitAcl|$projectId start")
                val users = workspaceJoinDao.fetchProjectSharedUser(dslContext, projectId)
                fetchProjectTGit(projectId) { repo, token ->
                    val config =
                        offshoreTGitApiClient.getProjectAcl(token, repo.tgitId.toString()) ?: return@fetchProjectTGit
                    val ips = config.allowIps.split(";").filter { it.isNotBlank() }.toMutableSet()
                    val ok = updateTGitProjectAcl(token, repo.tgitId.toString(), ips, users)
                    if (!ok) {
                        logger.warn("OP|refreshTGitAcl|$projectId|${repo.tgitId}|updateTGitProjectAcl false")
                    } else {
                        logger.info("OP|refreshTGitAcl|$projectId|${repo.tgitId}|updateTGitProjectAcl true")
                    }
                }
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GitProxyTGitService::class.java)

        //  云桌面公网ip，可能会动态变化所以放redis里
        private val REDIS_REMOTEDEV_PUBLIC_IPS = "remotedev:public:ips"
    }
}
