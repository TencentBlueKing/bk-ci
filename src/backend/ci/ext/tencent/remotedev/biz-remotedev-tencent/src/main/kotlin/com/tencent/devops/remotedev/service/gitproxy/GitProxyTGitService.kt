package com.tencent.devops.remotedev.service.gitproxy

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import com.tencent.bk.audit.annotations.ActionAuditRecord
import com.tencent.bk.audit.annotations.AuditInstanceRecord
import com.tencent.bk.audit.context.ActionAuditContext
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.audit.ActionAuditContent
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.auth.api.ResourceTypeId
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.notify.enums.NotifyType
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.model.remotedev.tables.TWorkspaceWindows
import com.tencent.devops.model.remotedev.tables.records.TProjectTgitIdLinkRecord
import com.tencent.devops.notify.api.service.ServiceNotifyMessageTemplateResource
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.config.TGitConfig
import com.tencent.devops.remotedev.config.async.AsyncExecute
import com.tencent.devops.remotedev.dao.ProjectTGitLinkDao
import com.tencent.devops.remotedev.dao.WorkspaceJoinDao
import com.tencent.devops.remotedev.pojo.TGitRepoDaoData
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
import com.tencent.devops.remotedev.pojo.async.AsyncTGitAclIp
import com.tencent.devops.remotedev.pojo.async.AsyncTGitAclUser
import com.tencent.devops.remotedev.pojo.gitproxy.CreateTGitProjectInfo
import com.tencent.devops.remotedev.pojo.gitproxy.LinktgitData
import com.tencent.devops.remotedev.pojo.gitproxy.ReBindingLinkData
import com.tencent.devops.remotedev.pojo.gitproxy.ReBindingLinkResp
import com.tencent.devops.remotedev.pojo.gitproxy.TGitCredType
import com.tencent.devops.remotedev.pojo.gitproxy.TGitNamespace
import com.tencent.devops.remotedev.pojo.gitproxy.TGitRepoData
import com.tencent.devops.remotedev.pojo.gitproxy.TGitRepoStatus
import com.tencent.devops.remotedev.service.BKItsmService
import com.tencent.devops.remotedev.service.gitproxy.GitProxyTGitService.Companion.logger
import com.tencent.devops.remotedev.service.gitproxy.OffshoreTGitApiClient.Companion.LOG_UPDATE_TGIT_ACL_TAG
import com.tencent.devops.repository.api.ServiceOauthResource
import com.tencent.devops.repository.pojo.enums.GitAccessLevelEnum
import com.tencent.devops.repository.pojo.oauth.GitToken
import java.time.Duration
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

/**
 * 云研发映射到工蜂 ACL访问控制相关
 * 注：新增接口时一定要注意操作记录以及是否能够命中告警
 */
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
    private val redisOperation: RedisOperation,
    private val rabbitTemplate: RabbitTemplate
) {
    // 校验当前凭据的用户是否拥有连接项目的 master 及以上权限
    @ActionAuditRecord(
        actionId = ActionId.TGIT_LINK_CREATE,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.TGIT_LINK
        ),
        content = ActionAuditContent.TGIT_LINK_CREATE_CONTENT
    )
    fun checkUserPermission(
        userId: String,
        projectId: String,
        data: LinktgitData
    ): Map<String, Boolean> {
        val codeProjectUrls = data.codeUrls
        val token = TokenBox(false).getToken(
            projectId = projectId,
            credType = if (data.credId == null) {
                TGitCredType.OAUTH_USER
            } else {
                TGitCredType.CRED_ID
            },
            cred = data.credId ?: userId
        )!!

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
                token = token,
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
                token = token,
                result = result,
                type = TGitProjectType.GIT,
                noGroup = (gitProjectUrls.size == noGroupGitUrls.size)
            )
        }

        // 说明没有一个成功的
        if (result.isEmpty()) {
            return urls.associateWith { false }
        }

        // 审计
        ActionAuditContext.current()
            .setInstanceName(result.toString())
            .addAttribute(ActionAuditContent.PROJECT_CODE_TEMPLATE, projectId)
            .scopeId = projectId

        // ITSM单据以及其触发的流水线变量有长度限制，这里分组提单
        val resultArray = result.filter { it.value.second }.map { Pair(it.key, it.value) }
        resultArray.chunked(20).forEachIndexed { index, chunk ->
            // 关联项目，不符合要求的自动踢出去
            bkitsmService.createTicket(
                projectId = projectId,
                userId = userId,
                tData = chunk.associate { it.first to it.second },
                index = if (resultArray.size > 20) {
                    index + 1
                } else {
                    null
                }
            )
        }

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
                    gitType = if (it.value.first.removeHttpPrefix()
                            .startsWith(tGitConfig.tSvnUrl.removeHttpPrefix())
                    ) {
                        TGitProjectType.SVN.name
                    } else {
                        TGitProjectType.GIT.name
                    },
                    url = it.value.first.removeHttpPrefix(),
                    cred = data.credId ?: userId,
                    credType = if (data.credId == null) {
                        TGitCredType.OAUTH_USER
                    } else {
                        TGitCredType.CRED_ID
                    }
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
    @ActionAuditRecord(
        actionId = ActionId.TGIT_LINK_CREATE,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.TGIT_LINK
        ),
        content = ActionAuditContent.TGIT_LINK_CALLBACK_CREATE_CONTENT
    )
    fun linkTGit(
        userId: String,
        projectId: String,
        repoIds: Map<Long, String>
    ): Map<Long, Boolean> {
        val result = mutableMapOf<Long, Boolean>()

        // 获取项目下正在跑的所有机器IP
        val ips = workspaceJoinDao.fetchWindowsWorkspacesSimple(
            dslContext = dslContext,
            projectId = projectId,
            checkField = listOf(TWorkspaceWindows.T_WORKSPACE_WINDOWS.HOST_IP),
            notStatus = listOf(
                WorkspaceStatus.PREPARING,
                WorkspaceStatus.DELETED,
                WorkspaceStatus.DELIVERING_FAILED
            )
        ).filter { !it.hostIp.isNullOrBlank() }.map { it.hostIp!!.substringAfter(".") }.toSet()
        // 获取项目下正在跑的所有机器的用户
        val users = fetchProjectSpecAclUsers(setOf(projectId))

        // 获取关联的工蜂仓库
        val repoRecord = projectTGitLinkDao.fetch(dslContext, projectId, repoIds.keys).associateBy { it.tgitId }

        // 审计
        ActionAuditContext.current()
            .setInstanceName(repoIds.toString())
            .addAttribute(ActionAuditContent.PROJECT_CODE_TEMPLATE, projectId)
            .scopeId = projectId

        val tokenBox = TokenBox(true)
        repoIds.forEach { (repoId, url) ->
            val record = repoRecord[repoId] ?: return@forEach
            if (record.status == TGitRepoStatus.AVAILABLE.name) {
                return@forEach
            }

            // 当前场景下目前是单一 token，拿不到肯定没了
            val tokenType = TGitCredType.fromStringDefault(record.credType)
            val token = tokenBox.getTokenAndSave(
                projectId, tokenType, when (tokenType) {
                    TGitCredType.OAUTH_USER -> record.cred ?: record.oauthUser
                    TGitCredType.CRED_ID -> {
                        if (record.cred == null) {
                            logger.error("$LOG_UPDATE_TGIT_ACL_TAG|linkTGit get $projectId|${record.tgitId} cred null")
                            return@forEach
                        }
                        record.cred
                    }
                }
            ) ?: return mapOf()

            val ok = incUpdateTGitProjectAcl(
                token = token,
                tGitProjectId = repoId,
                ips = ips,
                specUsers = users
            )
            result[repoId] = ok
            projectTGitLinkDao.updateStatus(
                dslContext = dslContext,
                projectId = projectId,
                tgitId = repoId,
                status = if (ok) {
                    TGitRepoStatus.AVAILABLE
                } else {
                    TGitRepoStatus.ABNORMAL
                }
            )
        }

        return result
    }

    fun tgitLinkList(
        projectId: String
    ): List<TGitRepoData> {
        val repos = projectTGitLinkDao.fetch(dslContext, projectId, null)
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

    // TODO: 理论上现在的逻辑不存在没有 url 的情况了，看看要不要去掉
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

    @ActionAuditRecord(
        actionId = ActionId.TGIT_LINK_DELETE,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.TGIT_LINK,
            instanceIds = "#userId",
            instanceNames = "#repoId"
        ),
        content = ActionAuditContent.TGIT_LINK_DELETE_CONTENT
    )
    fun deleteTgitLink(
        userId: String,
        projectId: String,
        repoId: Long,
        url: String
    ): Boolean {
        // 审计
        ActionAuditContext.current()
            .addAttribute(ActionAuditContent.PROJECT_CODE_TEMPLATE, projectId)
            .scopeId = projectId

        // TODO: 删除时用什么权限，还是这次先不做
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

        // 取消关联时如果当前项目是最后一个关联的项目，那么就直接清空
        // 如果还剩余其他关联项目，那么就拿其他关联项目取并集更新
        val otherProjects = projectTGitLinkDao.fetchByTGitId(
            dslContext = dslContext,
            tgitId = repoId,
            notProjectId = projectId
        ).map { it.projectId }.toSet()
        if (otherProjects.isEmpty()) {
            logger.info("$LOG_UPDATE_TGIT_ACL_TAG|deleteTgitLink|$projectId|$repoId")
            val ok = deleteTGitProjectAcl(
                token = token.accessToken,
                tGitProjectId = repoId.toString()
            )
            if (ok) {
                projectTGitLinkDao.deleteUrl(dslContext, projectId, repoId)
            }
            return ok
        }

        val ips = workspaceJoinDao.fetchWindowsWorkspaces(
            dslContext = dslContext,
            projectIds = otherProjects,
            checkField = listOf(TWorkspaceWindows.T_WORKSPACE_WINDOWS.HOST_IP),
            notStatus = setOf(
                WorkspaceStatus.PREPARING,
                WorkspaceStatus.DELETED,
                WorkspaceStatus.DELIVERING_FAILED
            )
        ).filter { !it.hostIp.isNullOrBlank() }.map { it.hostIp!!.substringAfter(".") }.toSet()
        val users = fetchProjectSpecAclUsers(otherProjects)

        val ok = incUpdateTGitProjectAcl(
            token = token.accessToken,
            tGitProjectId = repoId,
            ips = ips,
            specUsers = users,
            rewrite = true
        )
        if (ok) {
            projectTGitLinkDao.deleteUrl(dslContext, projectId, repoId)
        }

        return ok
    }

    /**
     * 创建和删除云桌面时同步
     */
    fun addOrRemoveAclIp(
        projectId: String,
        ips: Set<String>,
        remove: Boolean,
        tgitId: Long?
    ) {
        logger.info(
            "$LOG_UPDATE_TGIT_ACL_TAG|addOrRemoveAclIp|$projectId|$ips|remove=$remove|$tgitId"
        )
        AsyncExecute.dispatch(
            rabbitTemplate = rabbitTemplate,
            data = AsyncTGitAclIp(
                projectId = projectId,
                ips = ips,
                remove = remove,
                tgitId = tgitId
            ),
            errorLogTag = LOG_UPDATE_TGIT_ACL_TAG
        )
    }

    // 因为IP的唯一性，所以它还是可以单独进行增减分配
    fun doAddOrRemoveAclIp(
        projectId: String,
        ips: Set<String>,
        remove: Boolean,
        tgitId: Long?
    ) {
        fetchProjectTGit(projectId, true, tgitId) { repo, token ->
            val lock = updateTGitLock(repo.tgitId)
            try {
                lock.lock()
                val config = offshoreTGitApiClient.getProjectAcl(
                    accessToken = token,
                    projectId = repo.tgitId.toString()
                )
                if (config == null) {
                    logger.error(
                        "${LOG_UPDATE_TGIT_ACL_TAG}addOrRemoveAclIp|get $projectId|${repo.projectId} acl config error"
                    )
                    return@fetchProjectTGit
                }

                val configIps =
                    config.allowIps?.split(";")?.filter { it.isNotBlank() }?.toMutableSet() ?: mutableSetOf()
                if (remove) {
                    configIps.removeAll(ips)
                } else {
                    configIps.addAll(ips)
                }
                doUpdateIps(
                    token = token,
                    tGitProjectId = repo.tgitId.toString(),
                    ips = configIps
                )
            } finally {
                lock.unlock()
            }
        }
    }

    // 被分配的用户是工作空间下的一个子集，所以无法根据单个工作空间的人员变更得知整个项目的人员变更
    fun refreshProjectTGitSpecUser(
        projectId: String,
        tgitId: Long?
    ) {
        logger.info("$LOG_UPDATE_TGIT_ACL_TAG|refreshProjectTGitSpecUser|$projectId|$tgitId")
        AsyncExecute.dispatch(
            rabbitTemplate = rabbitTemplate,
            data = AsyncTGitAclUser(projectId, tgitId),
            errorLogTag = LOG_UPDATE_TGIT_ACL_TAG
        )
    }

    // 如果一个项目只绑定了一个tGit那么直接使用这个项目的所有的人，否则需要计算所有项目的所有的人
    fun doRefreshProjectTGitSpecUser(
        projectId: String,
        tgitId: Long?
    ) {
        // 获取所有关联项目下正在跑的所有机器的用户
        val usersMap = mutableMapOf<String, Set<String>>()
        fetchProjectTGit(projectId, true, tgitId) { repo, token ->
            val tGitProjects = projectTGitLinkDao.fetchByTGitId(dslContext, repo.tgitId, null)
                .map { it.projectId }.sorted().toSet()
            val mapKey = tGitProjects.joinToString(";")
            val specUsers = if (usersMap.containsKey(mapKey)) {
                usersMap[mapKey]
            } else {
                val tSpecUsers = fetchProjectSpecAclUsers(tGitProjects)
                usersMap[mapKey] = tSpecUsers
                tSpecUsers
            }
            incUpdateTGitProjectAcl(
                token = token,
                tGitProjectId = repo.tgitId,
                ips = null,
                specUsers = specUsers,
                rewrite = true
            )
        }
    }

    // TODO: 未来如果这类仅查询不修改的接口多了应当分开到别的类，保持类中总体的操作统一
    fun getTGitNamespaces(
        projectId: String,
        userId: String,
        page: Int,
        pageSize: Int,
        svnProject: Boolean,
        credId: String?
    ): List<TGitNamespace> {
        val token = TokenBox(false).getToken(
            projectId, if (credId == null) {
                TGitCredType.OAUTH_USER
            } else {
                TGitCredType.CRED_ID
            }, credId ?: userId
        )!!

        if (!svnProject) {
            return offshoreTGitApiClient.getNamespaces(token, page, pageSize)
        }

        val userResult = mutableListOf<TGitNamespace>()
        val result = mutableListOf<TGitNamespace>()
        var fetchPage = 1
        val fetchPageSize = 100
        // 工作空间每次会带一个个人工作空间
        val maxSize = page * pageSize + 1
        while (true) {
            val request = offshoreTGitApiClient.getNamespaces(token, fetchPage, fetchPageSize)
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

    @ActionAuditRecord(
        actionId = ActionId.TGIT_LINK_CREATE,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.TGIT_LINK,
            instanceIds = "#userId"
        ),
        content = ActionAuditContent.TGIT_LINK_CREATE_PROJECT_CONTENT
    )
    fun createProjectAndLinkTGit(
        userId: String,
        info: CreateTGitProjectInfo
    ): Boolean {
        val token = TokenBox(false).getToken(
            info.projectId, if (info.credId == null) {
                TGitCredType.OAUTH_USER
            } else {
                TGitCredType.CRED_ID
            }, info.credId ?: userId
        )!!

        // 审计
        ActionAuditContext.current()
            .setInstanceName("${info.name}|${info.svnProject}|${info.namespaceId}")
            .addAttribute(ActionAuditContent.PROJECT_CODE_TEMPLATE, info.projectId)
            .scopeId = info.projectId

        val data = offshoreTGitApiClient.createProject(
            accessToken = token,
            name = info.name,
            namespaceId = info.namespaceId,
            svnProject = info.svnProject
        )

        // 关联
        val ips = workspaceJoinDao.fetchWindowsWorkspacesSimple(
            dslContext = dslContext,
            projectId = info.projectId,
            checkField = listOf(TWorkspaceWindows.T_WORKSPACE_WINDOWS.HOST_IP),
            notStatus = listOf(
                WorkspaceStatus.PREPARING,
                WorkspaceStatus.DELETED,
                WorkspaceStatus.DELIVERING_FAILED
            )
        ).filter { !it.hostIp.isNullOrBlank() }.map { it.hostIp!!.substringAfter(".") }.toSet()
        val users = fetchProjectSpecAclUsers(setOf(info.projectId))

        val ok = incUpdateTGitProjectAcl(
            token = token,
            tGitProjectId = data.id,
            ips = ips,
            specUsers = users,
            rewrite = true
        )

        val url = data.httpsUrlToRepo ?: data.httpUrlToRepo ?: ""

        projectTGitLinkDao.add(
            dslContext = dslContext,
            projectId = info.projectId,
            tgitId = data.id,
            status = if (ok) {
                TGitRepoStatus.AVAILABLE
            } else {
                TGitRepoStatus.ABNORMAL
            },
            oauthUser = userId,
            gitType = if (info.svnProject) {
                TGitProjectType.SVN.name
            } else {
                TGitProjectType.GIT.name
            },
            url = url.removeHttpPrefix(),
            cred = info.credId ?: userId,
            credType = if (info.credId == null) {
                TGitCredType.OAUTH_USER
            } else {
                TGitCredType.CRED_ID
            }
        )

        return ok
    }

    fun reBinding(data: ReBindingLinkData): List<ReBindingLinkResp> {
        TODO("先整改完支持多 token")
    }

    private val publicIpsCache: LoadingCache<String, String> = Caffeine.newBuilder()
        .maximumSize(5)
        .expireAfterWrite(Duration.ofHours(1))
        .build { key -> redisOperation.get(key, isDistinguishCluster = false) ?: "" }

    // 增量更新
    // 默认规则组仅放云桌面 IP, 并清空用户白名单
    // 分配了云桌面的用户都加到特定访问人群名单
    // 特殊规则组 IP 加上云桌面 IP 及所有公网 IP（过渡期，后续会移除公网 IP）
    // rewrite 直接重写
    private fun incUpdateTGitProjectAcl(
        token: String,
        tGitProjectId: Long,
        ips: Set<String>?,
        specUsers: Set<String>?,
        rewrite: Boolean = false
    ): Boolean {
        logger.info(
            "$LOG_UPDATE_TGIT_ACL_TAG|incUpdateTGitProjectAcl|$tGitProjectId|rewrite=$rewrite|$ips|$specUsers"
        )
        if (rewrite) {
            return doUpdateTGitProjectAcl(
                token = token,
                tGitProjectId = tGitProjectId.toString(),
                ips = ips,
                users = emptySet(),
                specUsers = specUsers
            )
        }
        val lock = updateTGitLock(tGitProjectId)
        try {
            lock.lock()
            val config = offshoreTGitApiClient.getProjectAcl(
                accessToken = token,
                projectId = tGitProjectId.toString()
            ) ?: run {
                logger.error("$LOG_UPDATE_TGIT_ACL_TAG|updateTGitProjectAcl|$tGitProjectId get acl config null")
                return false
            }
            val oldIps = config.allowIps?.split(";")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
            val oldSpecUsers = config.specHitUsers?.split(";")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
            return doUpdateTGitProjectAcl(
                token = token,
                tGitProjectId = tGitProjectId.toString(),
                ips = if (ips != null) {
                    oldIps.union(ips)
                } else {
                    null
                },
                users = if (config.allowUsers?.isNotBlank() == true) {
                    emptySet()
                } else {
                    null
                },
                specUsers = if (specUsers != null) {
                    oldSpecUsers.union(specUsers)
                } else {
                    null
                }
            )
        } finally {
            lock.unlock()
        }
    }

    private fun doUpdateTGitProjectAcl(
        token: String,
        tGitProjectId: String,
        ips: Set<String>?,
        users: Set<String>?,
        specUsers: Set<String>?
    ): Boolean {
        var ipOk = true
        var specIpOk = true
        if (ips != null) {
            val okP = doUpdateIps(token, tGitProjectId, ips)
            ipOk = okP.first
            specIpOk = okP.second
        }

        var userOk = true
        if (users != null) {
            userOk = offshoreTGitApiClient.updateProjectAclUser(token, tGitProjectId, emptySet())
        }

        var specUserOk = true
        if (specUsers != null) {
            specUserOk = offshoreTGitApiClient.updateProjectAclSpecUser(token, tGitProjectId, specUsers)
        }

        return ipOk && userOk && specUserOk && specIpOk
    }

    private fun doUpdateIps(
        token: String,
        tGitProjectId: String,
        ips: Set<String>
    ): Pair<Boolean, Boolean> {
        val ipOk = offshoreTGitApiClient.updateProjectAclIp(token, tGitProjectId, ips)
        val specIpOk = offshoreTGitApiClient.updateProjectAclSpecIps(
            accessToken = token,
            projectId = tGitProjectId,
            ips = ips.plus(
                publicIpsCache.get(REDIS_REMOTEDEV_PUBLIC_IPS)?.split(";")?.filter { it.isNotBlank() }?.toSet()
                    ?: emptySet()
            )
        )
        return Pair(ipOk, specIpOk)
    }

    private fun deleteTGitProjectAcl(
        token: String,
        tGitProjectId: String
    ): Boolean {
        val ipOk = offshoreTGitApiClient.updateProjectAclIp(token, tGitProjectId, emptySet())
        val userOk = offshoreTGitApiClient.updateProjectAclUser(token, tGitProjectId, emptySet())
        val specIpOk = offshoreTGitApiClient.updateProjectAclSpecIps(token, tGitProjectId, emptySet())
        val specUserOk = offshoreTGitApiClient.updateProjectAclSpecUser(token, tGitProjectId, emptySet())
        return ipOk && userOk && specUserOk && specIpOk
    }

    private fun fetchProjectSpecAclUsers(projectIds: Set<String>): Set<String> {
        return workspaceJoinDao.fetchProjectSharedUser(dslContext, projectIds)
            .filter { it.endsWith("@tai") }.map { it.removeSuffix("@tai") }.toSet()
    }

    private fun fetchProjectTGit(
        projectId: String,
        needToken: Boolean,
        tgitId: Long?,
        run: (repo: TProjectTgitIdLinkRecord, token: String) -> Unit
    ) {
        val tokenBox = TokenBox(true)
        projectTGitLinkDao.fetch(
            dslContext = dslContext,
            projectId = projectId,
            tgitIds = if (tgitId == null) {
                null
            } else {
                setOf(tgitId)
            }
        ).filter { it.status == TGitRepoStatus.AVAILABLE.name }.forEach { repo ->
            if (!needToken) {
                run(repo, "")
                return@forEach
            }

            val tokenType = TGitCredType.fromStringDefault(repo.credType)
            val token = tokenBox.getTokenAndSave(
                projectId, tokenType, when (tokenType) {
                    TGitCredType.OAUTH_USER -> repo.cred ?: repo.oauthUser
                    TGitCredType.CRED_ID -> {
                        if (repo.cred == null) {
                            logger.error("$LOG_UPDATE_TGIT_ACL_TAG|linkTGit get $projectId|${repo.tgitId} cred null")
                            return@forEach
                        }
                        repo.cred
                    }
                }
            ) ?: return@forEach

            run(repo, token)
        }
    }

    /**
     * 检查关联的TGit仓库的管理员的权限是否过期
     */
    @Scheduled(cron = "0 50 9 * * ?")
    fun dailyUserAuthDoCheck() {
        logger.info("dailyUserAuthDoCheck start")
        // 基本上每个项目下的权限都是这个项目使用的，所以以项目为做检查呢
        val allProjects = projectTGitLinkDao.fetchAllProject(dslContext)
        val tokenBox = TokenBox(true)
        allProjects.forEach { project ->
            subDailyUserAuthDoCheck(project, tokenBox)
        }
    }

    private fun subDailyUserAuthDoCheck(
        projectId: String,
        tokenBox: TokenBox
    ) {
        val recordData = projectTGitLinkDao.fetch(dslContext, projectId, null).associateBy { it.tgitId }
        val tokenMap = mutableMapOf<String, MutableSet<String>>()
        // 先拿到所有要拿的 token
        recordData.values.forEach { record ->
            val tokenType = TGitCredType.fromStringDefault(repo.credType)
            val token = tokenBox.getTokenAndSave(
                projectId, tokenType, when (tokenType) {
                    TGitCredType.OAUTH_USER -> repo.cred ?: repo.oauthUser
                    TGitCredType.CRED_ID -> {
                        if (repo.cred == null) {
                            logger.error("$LOG_UPDATE_TGIT_ACL_TAG|linkTGit get $projectId|${repo.tgitId} cred null")
                            return@forEach
                        }
                        repo.cred
                    }
                }
            ) ?: return@forEach
        }
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

            val tokenType = TGitCredType.fromStringDefault(repo.credType)
            val token = tokenBox.getTokenAndSave(
                projectId, tokenType, when (tokenType) {
                    TGitCredType.OAUTH_USER -> repo.cred ?: repo.oauthUser
                    TGitCredType.CRED_ID -> {
                        if (repo.cred == null) {
                            logger.error("$LOG_UPDATE_TGIT_ACL_TAG|linkTGit get $projectId|${repo.tgitId} cred null")
                            return@forEach
                        }
                        repo.cred
                    }
                }
            ) ?: return@forEach

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
        logger.info("dailyUserAuthDoCheck|$result")

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
                            "urls" to idAndUrls.values.joinToString(separator = "\n"),
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

    private fun String.removeHttpPrefix() = this.removePrefix("https://").removePrefix("http://")

    private fun updateTGitLock(tGitId: Long): RedisLock =
        RedisLock(redisOperation, "$REDIS_REMOTEDEV_PROJECT_UPDATE_TGIT_ACL:$tGitId", 90)

    companion object {
        private val logger = LoggerFactory.getLogger(GitProxyTGitService::class.java)

        // 云桌面公网ip，可能会动态变化所以放redis里
        private const val REDIS_REMOTEDEV_PUBLIC_IPS = "remotedev:public:ips"

        // 获取工蜂ACL配置的锁，同一时间对同一个项目的配置只能有一个读写
        private const val REDIS_REMOTEDEV_PROJECT_UPDATE_TGIT_ACL = "remotedev:project:update:tgit:acl"
    }

    /**
     * 把获取 Token 的行为全部封装在这里，非线程安全
     * @param save 是否缓存 token
     */
    inner class TokenBox(
        private val save: Boolean
    ) {
        private var oauthUserTokens: MutableMap<String, String>? = null
        private var credIdTokens: MutableMap<String, String>? = null

        init {
            if (save) {
                oauthUserTokens = mutableMapOf()
                credIdTokens = mutableMapOf()
            }
        }

        fun getToken(
            projectId: String,
            credType: TGitCredType,
            cred: String,
            throwE: Boolean = true,
            log: Boolean = false
        ): String? {
            when (credType) {
                TGitCredType.OAUTH_USER -> {
                    val token = requestOauthToken(cred)?.accessToken
                    if (token != null) {
                        return token
                    }
                    if (log) {
                        logger.error("$LOG_UPDATE_TGIT_ACL_TAG|getToken|$projectId|$credType|$cred is null")
                    }
                    if (throwE) {
                        throw ErrorCodeException(
                            errorCode = ErrorCodeEnum.NO_TGIT_OAUTH_ERROR.errorCode,
                            errorType = ErrorCodeEnum.NO_TGIT_OAUTH_ERROR.errorType,
                            params = arrayOf(cred, tGitConfig.tGitUrl)
                        )
                    }
                    return null
                }

                TGitCredType.CRED_ID -> {
                    val token = requestCredIdToken(cred)
                    if (token != null) {
                        return token
                    }
                    if (log) {
                        logger.error("$LOG_UPDATE_TGIT_ACL_TAG|getToken|$projectId|$credType|$cred is null")
                    }
                    if (throwE) {
                        throw ErrorCodeException(
                            errorCode = ErrorCodeEnum.NO_CRED_ID_ERROR.errorCode,
                            errorType = ErrorCodeEnum.NO_CRED_ID_ERROR.errorType,
                            params = arrayOf(projectId, cred)
                        )
                    }
                }
            }
            return null
        }

        fun getTokenAndSave(projectId: String, credType: TGitCredType, cred: String, log: Boolean = true): String? {
            return when (credType) {
                TGitCredType.OAUTH_USER -> {
                    oauthUserTokens?.get(cred) ?: run {
                        val token = getToken(
                            projectId = projectId,
                            credType = credType,
                            cred = cred,
                            throwE = false,
                            log = log
                        ) ?: return null
                        oauthUserTokens?.set(cred, token)
                        token
                    }
                }

                TGitCredType.CRED_ID -> {
                    credIdTokens?.get(cred) ?: run {
                        val token = getToken(
                            projectId = projectId,
                            credType = credType,
                            cred = cred,
                            throwE = false,
                            log = log
                        ) ?: return null
                        credIdTokens?.set(cred, token)
                        token
                    }
                }
            }
        }

        private fun requestOauthToken(userId: String): GitToken? {
            return client.get(ServiceOauthResource::class).tGitGet(userId).data
        }

        private fun requestCredIdToken(credId: String): String? {
            // TODO: 从凭据服务中拿取
            return ""
        }
    }
}
