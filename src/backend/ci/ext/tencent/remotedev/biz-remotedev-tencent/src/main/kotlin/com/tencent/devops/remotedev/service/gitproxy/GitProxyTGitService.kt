package com.tencent.devops.remotedev.service.gitproxy

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import com.tencent.bk.audit.annotations.ActionAuditRecord
import com.tencent.bk.audit.annotations.AuditInstanceRecord
import com.tencent.bk.audit.context.ActionAuditContext
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.DHKeyPair
import com.tencent.devops.common.api.util.DHUtil
import com.tencent.devops.common.audit.ActionAuditContent
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.auth.api.ResourceTypeId
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.model.remotedev.tables.TWorkspaceWindows
import com.tencent.devops.model.remotedev.tables.records.TProjectTgitIdLinkRecord
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
import com.tencent.devops.remotedev.pojo.gitproxy.TGitCredType
import com.tencent.devops.remotedev.pojo.gitproxy.TGitNamespace
import com.tencent.devops.remotedev.pojo.gitproxy.TGitRepoData
import com.tencent.devops.remotedev.pojo.gitproxy.TGitRepoStatus
import com.tencent.devops.remotedev.service.BKItsmService
import com.tencent.devops.remotedev.service.gitproxy.OffshoreTGitApiClient.Companion.LOG_UPDATE_TGIT_ACL_TAG
import com.tencent.devops.repository.api.ServiceOauthResource
import com.tencent.devops.repository.pojo.enums.GitAccessLevelEnum
import com.tencent.devops.repository.pojo.oauth.GitToken
import com.tencent.devops.ticket.api.ServiceCredentialResource
import com.tencent.devops.ticket.pojo.enums.CredentialType
import java.time.Duration
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.Base64

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
        val credType = if (data.credId == null) {
            TGitCredType.OAUTH_USER
        } else {
            TGitCredType.CRED_ACCESS_TOKEN_ID
        }

        val urls = codeProjectUrls.filter { it.isNotBlank() }.map { it.trim().removeHttpPrefix() }.toSet()

        val result = mutableMapOf<Long, Pair<String, Boolean>>()

        val tokenBox = TokenBox(true, true, false)

        // 过滤 svn 项目，目前 SVN项目只能从根组创建项目，所以项目组的分割后俩，项目的分割后三
        val svnProjectUrls = urls.filter {
            it.startsWith(tGitConfig.tSvnUrl.removeHttpPrefix())
        }.toSet()
        val noGroupSvnUrls = svnProjectUrls.filter { it.split("/").filter { s -> s.isNotBlank() }.size == 3 }
        if (svnProjectUrls.isNotEmpty()) {
            val rProjectUrls = svnProjectUrls.map { it.removeHttpPrefix() }.toSet()
            filterRecordWithTGitProjectsData(
                data = mutableMapOf(credType to mutableMapOf((data.credId ?: userId) to mutableSetOf())),
                tokenBox = tokenBox,
                projectId = projectId,
                type = TGitProjectType.SVN
            ) { project, _ ->
                val ok = checkUrlPermission(project, rProjectUrls, (noGroupSvnUrls.size == svnProjectUrls.size), result)
                return@filterRecordWithTGitProjectsData ok
            }
        }

        // 过滤 git 项目，git的项目结尾有.git不然都按项目组算
        val gitProjectUrls = urls.filter {
            it.startsWith(tGitConfig.tGitUrl.removeHttpPrefix())
        }.toSet()
        val noGroupGitUrls = gitProjectUrls.filter { it.endsWith(".git") }
        if (gitProjectUrls.isNotEmpty()) {
            val rProjectUrls = gitProjectUrls.map { it.removeHttpPrefix() }.toSet()
            filterRecordWithTGitProjectsData(
                data = mutableMapOf(credType to mutableMapOf((data.credId ?: userId) to mutableSetOf())),
                tokenBox = tokenBox,
                projectId = projectId,
                type = TGitProjectType.GIT
            ) { project, _ ->
                val ok = checkUrlPermission(project, rProjectUrls, (gitProjectUrls.size == noGroupGitUrls.size), result)
                return@filterRecordWithTGitProjectsData ok
            }
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
            bkitsmService.createLinkTicket(
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
                        TGitCredType.CRED_ACCESS_TOKEN_ID
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

    private fun checkUrlPermission(
        project: TGitProjectInfo,
        rProjectUrls: Set<String>,
        noGroup: Boolean,
        result: MutableMap<Long, Pair<String, Boolean>>
    ): Boolean {
        rProjectUrls.forEach urls@{ projectUrl ->
            if (project.httpsUrlToRepo.isNullOrBlank() && project.httpUrlToRepo.isNullOrBlank()) {
                logger.warn("filterUrlPermission|httpsUrl is null $project")
                return true
            }
            val url = (project.httpsUrlToRepo ?: project.httpUrlToRepo)?.removeHttpPrefix()
            if ((url != projectUrl) && (url?.startsWith("${projectUrl.removeSuffix("/")}/") != true)) {
                return@urls
            }

            result[project.id] = Pair(url, true)

            // 如果全都是项目判断那么只要项目判断完就可以退出
            if (noGroup && rProjectUrls.subtract(result.keys).isEmpty()) {
                return false
            }
        }
        return true
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
        projectId: String,
        repoIds: Set<Long>,
        innerToken: TGitToken? = null
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
        val repoRecord = projectTGitLinkDao.fetch(dslContext, projectId, repoIds).associateBy { it.tgitId }

        // 审计
        ActionAuditContext.current()
            .setInstanceName(repoIds.toString())
            .addAttribute(ActionAuditContent.PROJECT_CODE_TEMPLATE, projectId)
            .scopeId = projectId

        val tokenBox = TokenBox(true)
        repoIds.forEach { repoId ->
            val record = repoRecord[repoId] ?: return@forEach
            if (record.status == TGitRepoStatus.AVAILABLE.name) {
                return@forEach
            }

            // 当前场景下目前是单一 token，拿不到肯定没了
            val tokenType = TGitCredType.fromStringDefault(record.credType)
            val token = innerToken ?: tokenBox.get(
                projectId = projectId,
                credType = tokenType,
                cred = when (tokenType) {
                    TGitCredType.OAUTH_USER -> record.cred ?: record.oauthUser
                    TGitCredType.CRED_ACCESS_TOKEN_ID -> {
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
                repoType = it.gitType,
                url = it.url ?: it.tgitId.toString(),
                status = TGitRepoStatus.fromStr(it.status)
            )
        }.associateBy { it.repoId }

        filterRecordWithTGitProjects(projectId, repos) { project, tGitIds ->
            if (!tGitIds.contains(project.id)) {
                return@filterRecordWithTGitProjects true
            }

            if (!project.httpsUrlToRepo.isNullOrBlank() || !project.httpUrlToRepo.isNullOrBlank()) {
                result[project.id]?.url = project.httpsUrlToRepo ?: project.httpUrlToRepo!!
            }
            tGitIds.remove(project.id)

            return@filterRecordWithTGitProjects true
        }

        return result.values.toList()
    }

    // 使用工蜂的项目信息与数据库的数据做交互处理
    private fun filterRecordWithTGitProjects(
        projectId: String,
        records: List<TProjectTgitIdLinkRecord>,
        run: (project: TGitProjectInfo, tGitIds: MutableSet<Long>) -> Boolean
    ) {
        if (records.isEmpty()) {
            return
        }

        val svnData = mutableMapOf<TGitCredType, MutableMap<String, MutableSet<Long>>>()
        val gitData = mutableMapOf<TGitCredType, MutableMap<String, MutableSet<Long>>>()
        records.forEach {
            val credType = TGitCredType.fromStringDefault(it.credType)
            val cred = it.cred ?: it.oauthUser
            if (it.gitType == TGitProjectType.SVN.name) {
                if (svnData[credType] == null) {
                    svnData[credType] = mutableMapOf(cred to mutableSetOf(it.tgitId))
                } else if (svnData[credType]!![cred] == null) {
                    svnData[credType]!![cred] = mutableSetOf(it.tgitId)
                } else {
                    svnData[credType]!![cred]!!.add(it.tgitId)
                }
                return@forEach
            }

            if (gitData[credType] == null) {
                gitData[credType] = mutableMapOf(cred to mutableSetOf(it.tgitId))
            } else if (gitData[credType]!![cred] == null) {
                gitData[credType]!![cred] = mutableSetOf(it.tgitId)
            } else {
                gitData[credType]!![cred]!!.add(it.tgitId)
            }
        }

        val tokenBox = TokenBox(true)

        filterRecordWithTGitProjectsData(svnData, tokenBox, projectId, TGitProjectType.SVN, run)
        filterRecordWithTGitProjectsData(gitData, tokenBox, projectId, TGitProjectType.GIT, run)
    }

    private fun filterRecordWithTGitProjectsData(
        data: MutableMap<TGitCredType, MutableMap<String, MutableSet<Long>>>,
        tokenBox: TokenBox,
        projectId: String,
        type: TGitProjectType,
        run: (project: TGitProjectInfo, tGitIds: MutableSet<Long>) -> Boolean
    ) {
        data.forEach { (credType, credAndRepos) ->
            credAndRepos.forEach credAndRepos@{ (cred, repoIds) ->
                val token = tokenBox.get(
                    projectId = projectId,
                    credType = credType,
                    cred = cred
                ) ?: return@credAndRepos

                var page = 1
                val pageSize = 100

                while (true) {
                    val projects = offshoreTGitApiClient.getProjectList(
                        token = token,
                        page = page,
                        pageSize = pageSize,
                        search = null,
                        minAccessLevel = GitAccessLevelEnum.MASTER,
                        type = type
                    )

                    // 过滤项目信息
                    projects.forEach projects@{ project ->
                        val ok = run(project, repoIds)
                        if (!ok) {
                            return
                        }
                    }

                    if (projects.size < 100 || repoIds.isEmpty()) {
                        break
                    }
                    page++
                }
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

        // TODO: 删除需要一个新单子评审下
        val tokenR = client.get(ServiceOauthResource::class).tGitGet(userId).data ?: throw ErrorCodeException(
            errorCode = ErrorCodeEnum.NO_TGIT_OAUTH_ERROR.errorCode,
            errorType = ErrorCodeEnum.NO_TGIT_OAUTH_ERROR.errorType,
            params = arrayOf(userId, tGitConfig.tGitUrl)
        )
        val token = TGitToken(tokenR.accessToken, false)

        val isSvn = url.removeHttpPrefix().startsWith(tGitConfig.tSvnUrl.removeHttpPrefix())

        // 校验下是否有删除的权限，svn项目用户在根目录下是否是审批人，git项目校验是否有master及以上权限
        if (isSvn) {
            val svnRs = offshoreTGitApiClient.getSvnProjectAuth(
                token = token,
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
                token = token,
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
                token = token,
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
            token = token,
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
                    token = token!!,
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
                token = token!!,
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
        val token = TokenBox(false).get(
            projectId, if (credId == null) {
                TGitCredType.OAUTH_USER
            } else {
                TGitCredType.CRED_ACCESS_TOKEN_ID
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
        val token = TokenBox(false).get(
            info.projectId, if (info.credId == null) {
                TGitCredType.OAUTH_USER
            } else {
                TGitCredType.CRED_ACCESS_TOKEN_ID
            }, info.credId ?: userId
        )!!

        // 审计
        ActionAuditContext.current()
            .setInstanceName("${info.name}|${info.svnProject}|${info.namespaceId}")
            .addAttribute(ActionAuditContent.PROJECT_CODE_TEMPLATE, info.projectId)
            .scopeId = info.projectId

        val data = offshoreTGitApiClient.createProject(
            token = token,
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
                TGitCredType.CRED_ACCESS_TOKEN_ID
            }
        )

        return ok
    }

    fun reBinding(userId: String, data: ReBindingLinkData) {
        val tokenBox = TokenBox(true, true, false)
        val credType = if (data.credId == null) {
            TGitCredType.OAUTH_USER
        } else {
            TGitCredType.CRED_ACCESS_TOKEN_ID
        }

        // 检查当前ID是否有权限
        val checkedId = mutableSetOf<Long>()
        val svnRepoIds = data.idMap.filter { it.value == TGitProjectType.SVN.name }.keys.toSet()
        if (svnRepoIds.isNotEmpty()) {
            if (svnRepoIds.size == 1) {
                filterRecordWithTGitProjectsData(
                    data = mutableMapOf(credType to mutableMapOf((data.credId ?: userId) to mutableSetOf())),
                    tokenBox = tokenBox,
                    projectId = data.projectId,
                    type = TGitProjectType.SVN
                ) { project, _ ->
                    if (project.id in svnRepoIds) {
                        checkedId.add(project.id)
                    }
                    return@filterRecordWithTGitProjectsData true
                }
            }
        }
        val gitRepoIds = data.idMap.filter { it.value == TGitProjectType.GIT.name }.keys.toSet()
        if (gitRepoIds.isNotEmpty()) {
            filterRecordWithTGitProjectsData(
                data = mutableMapOf(credType to mutableMapOf((data.credId ?: userId) to mutableSetOf())),
                tokenBox = tokenBox,
                projectId = data.projectId,
                type = TGitProjectType.GIT
            ) { project, _ ->
                if (project.id in gitRepoIds) {
                    checkedId.add(project.id)
                }
                return@filterRecordWithTGitProjectsData true
            }
        }
        val noCheckedIds = data.idMap.keys.toMutableSet().let {
            it.removeAll(checkedId)
            it
        }

        linkTGit(data.projectId, checkedId, tokenBox.get(data.projectId, credType, (data.credId ?: userId)))

        if (noCheckedIds.isNotEmpty()) {
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.REBINDING_ERROR.errorCode,
                errorType = ErrorCodeEnum.REBINDING_ERROR.errorType
            )
        }
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
        token: TGitToken,
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
                token = token,
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
        token: TGitToken,
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
        token: TGitToken,
        tGitProjectId: String,
        ips: Set<String>
    ): Pair<Boolean, Boolean> {
        val ipOk = offshoreTGitApiClient.updateProjectAclIp(token, tGitProjectId, ips)
        val specIpOk = offshoreTGitApiClient.updateProjectAclSpecIps(
            token = token,
            projectId = tGitProjectId,
            ips = ips.plus(
                publicIpsCache.get(REDIS_REMOTEDEV_PUBLIC_IPS)?.split(";")?.filter { it.isNotBlank() }?.toSet()
                    ?: emptySet()
            )
        )
        return Pair(ipOk, specIpOk)
    }

    private fun deleteTGitProjectAcl(
        token: TGitToken,
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
        run: (repo: TProjectTgitIdLinkRecord, token: TGitToken?) -> Unit
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
                run(repo, null)
                return@forEach
            }

            val tokenType = TGitCredType.fromStringDefault(repo.credType)
            val token = tokenBox.get(
                projectId = projectId,
                credType = tokenType,
                cred = when (tokenType) {
                    TGitCredType.OAUTH_USER -> repo.cred ?: repo.oauthUser
                    TGitCredType.CRED_ACCESS_TOKEN_ID -> {
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
    @Scheduled(cron = "0 50 3 * * ?")
    fun dailyUserAuthDoCheck() {
        logger.info("dailyUserAuthDoCheck start")
        // 基本上每个项目下的权限都是这个项目使用的，所以以项目为做检查呢
        val allProjects = projectTGitLinkDao.fetchAllProject(dslContext)
        allProjects.forEach { project ->
            subDailyUserAuthDoCheck(project)
        }
    }

    private fun subDailyUserAuthDoCheck(
        projectId: String
    ) {
        val records = projectTGitLinkDao.fetch(dslContext, projectId, null)
        val recordsMap = records.associateBy { it.tgitId }.toMutableMap()

        val canAvailableRecords = mutableSetOf<Long>()

        filterRecordWithTGitProjects(projectId, records) { project, tgitIds ->
            val record = recordsMap[project.id] ?: return@filterRecordWithTGitProjects true

            // url发生变化时更新url
            val tGitUrl = project.httpsUrlToRepo ?: project.httpUrlToRepo
            if (!tGitUrl.isNullOrBlank() && tGitUrl != record.url) {
                projectTGitLinkDao.updateUrl(
                    dslContext = dslContext,
                    projectId = record.projectId,
                    tgitId = record.tgitId,
                    url = tGitUrl.removeHttpPrefix()
                )
            }

            // 如果以前是没权限，现在有权限了应该恢复
            if (record.status == TGitRepoStatus.ABNORMAL.name) {
                canAvailableRecords.add(record.tgitId)
            }

            recordsMap.remove(project.id)
            tgitIds.remove(project.id)

            return@filterRecordWithTGitProjects true
        }

        // 恢复状态已经正常的
        if (canAvailableRecords.isNotEmpty()) {
            projectTGitLinkDao.batchUpdateStatus(dslContext, projectId, canAvailableRecords, TGitRepoStatus.AVAILABLE)
        }

        // 剩下的就是没有找到符合权限的需要发通知
        val result = mutableMapOf<String, MutableMap<Long, String>>()
        recordsMap.values.forEach { record ->
            val userId = when (TGitCredType.fromStringDefault(record.credType)) {
                TGitCredType.OAUTH_USER -> record.cred ?: record.oauthUser
                TGitCredType.CRED_ACCESS_TOKEN_ID -> record.oauthUser
            }
            if (result[userId] == null) {
                result[userId] = mutableMapOf(record.tgitId to record.url)
            } else {
                result[userId]!![record.tgitId] = record.url
            }
        }
        logger.info("dailyUserAuthDoCheck|canAvailableRecords|$canAvailableRecords")
        logger.info("dailyUserAuthDoCheck|canAvailableRecords|$result")

        val project = client.get(ServiceProjectResource::class)
            .listByProjectCode(setOf(projectId)).data?.firstOrNull()
        if (project == null) {
            logger.warn("dailyUserAuthDoCheck|$projectId listByProjectCode null")
            return
        }
        logger.info("dailyUserAuthDoCheck|$result")

        result.forEach { (userId, idAndUrls) ->
            projectTGitLinkDao.batchUpdateStatus(dslContext, projectId, idAndUrls.keys, TGitRepoStatus.ABNORMAL)
            sendCheckNotify(
                userId = userId,
                urls = idAndUrls.values.toList(),
                projectId = projectId,
                projectName = project.projectName,
                managers = project.properties?.remotedevManager?.split(";")?.filter { it.isNotBlank() }
                    ?.toMutableSet()
            )
        }
    }

    private fun sendCheckNotify(
        userId: String,
        urls: List<String>,
        projectId: String,
        projectName: String,
        managers: MutableSet<String>?
    ) {
        try {
            bkitsmService.createCheckTicket(projectId, "devops", userId, urls)
        } catch (e: Exception) {
            logger.error("$LOG_UPDATE_TGIT_ACL_TAG|sendCheckNotify|$userId|$projectId|$projectName|error", e)
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
        private val save: Boolean,
        private val bThrowE: Boolean? = null,
        private val bLogE: Boolean? = null
    ) {
        private var oauthUserTokens: MutableMap<String, TGitToken>? = null
        private var credIdTokens: MutableMap<String, TGitToken>? = null
        private var dhKeyPair: DHKeyPair? = null

        init {
            if (save) {
                oauthUserTokens = mutableMapOf()
                credIdTokens = mutableMapOf()
            }
        }

        fun get(
            projectId: String,
            credType: TGitCredType,
            cred: String,
            throwE: Boolean? = null,
            log: Boolean? = null
        ): TGitToken? {
            if (save) {
                return getTokenAndSave(
                    projectId = projectId,
                    credType = credType,
                    cred = cred,
                    throwE = throwE ?: bThrowE ?: false,
                    log = log ?: bLogE ?: true
                )
            }
            return getToken(
                projectId = projectId,
                credType = credType,
                cred = cred,
                throwE = throwE ?: bThrowE ?: true,
                log = log ?: bLogE ?: false
            )
        }

        private fun getToken(
            projectId: String,
            credType: TGitCredType,
            cred: String,
            throwE: Boolean,
            log: Boolean
        ): TGitToken? {
            when (credType) {
                TGitCredType.OAUTH_USER -> {
                    val token = requestOauthToken(cred)?.accessToken
                    if (token != null) {
                        return TGitToken(token, false)
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

                TGitCredType.CRED_ACCESS_TOKEN_ID -> {
                    val token = requestCredIdToken(projectId, cred)
                    if (token != null) {
                        return TGitToken(token, true)
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

        private fun getTokenAndSave(
            projectId: String,
            credType: TGitCredType,
            cred: String,
            throwE: Boolean,
            log: Boolean
        ): TGitToken? {
            return when (credType) {
                TGitCredType.OAUTH_USER -> {
                    oauthUserTokens?.get(cred) ?: run {
                        val token = getToken(
                            projectId = projectId,
                            credType = credType,
                            cred = cred,
                            throwE = throwE,
                            log = log
                        ) ?: return null
                        oauthUserTokens?.set(cred, token)
                        token
                    }
                }

                TGitCredType.CRED_ACCESS_TOKEN_ID -> {
                    credIdTokens?.get(cred) ?: run {
                        val token = getToken(
                            projectId = projectId,
                            credType = credType,
                            cred = cred,
                            throwE = throwE,
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

        private fun requestCredIdToken(projectId: String, credId: String): String? {
            val pair = if (dhKeyPair == null) {
                dhKeyPair = DHUtil.initKey()
                dhKeyPair
            } else {
                dhKeyPair
            }!!
            val encoder = Base64.getEncoder()
            val decoder = Base64.getDecoder()
            val credRes = client.get(ServiceCredentialResource::class).get(
                projectId = projectId,
                credentialId = credId,
                publicKey = encoder.encodeToString(pair.publicKey)
            )
            if (credRes.isNotOk()) {
                logger.error("$LOG_UPDATE_TGIT_ACL_TAG|requestCredIdToken|$projectId|$credId get ticket fail", credRes)
                return null
            }
            val cred = credRes.data ?: return null
            if (cred.credentialType != CredentialType.ACCESSTOKEN) {
                logger.warn("$LOG_UPDATE_TGIT_ACL_TAG|requestCredIdToken|$projectId|$credId cred type not access_token")
                return null
            }
            return String(DHUtil.decrypt(decoder.decode(cred.v1), decoder.decode(cred.publicKey), pair.privateKey))
        }
    }
}
