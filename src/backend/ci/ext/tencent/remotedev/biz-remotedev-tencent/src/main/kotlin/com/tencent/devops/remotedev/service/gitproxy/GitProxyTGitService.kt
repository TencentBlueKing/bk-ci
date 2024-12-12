package com.tencent.devops.remotedev.service.gitproxy

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import com.tencent.bk.audit.annotations.ActionAuditRecord
import com.tencent.bk.audit.annotations.AuditInstanceRecord
import com.tencent.bk.audit.context.ActionAuditContext
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.audit.ActionAuditContent
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.auth.api.ResourceTypeId
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.utils.I18nUtil
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
import com.tencent.devops.remotedev.service.devcloud.DevcloudService
import com.tencent.devops.remotedev.service.gitproxy.OffshoreTGitApiClient.Companion.LOG_UPDATE_TGIT_ACL_TAG
import com.tencent.devops.repository.pojo.enums.GitAccessLevelEnum
import java.time.Duration
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

// 保存凭据和项目映射关系的数据类型 TGitType -> ( cred -> [TGitId] )
typealias CredAndProjectData = MutableMap<TGitCredType, MutableMap<String, MutableSet<Long>?>>

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
    private val streamBridge: StreamBridge,
    private val devcloudService: DevcloudService
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

        val tokenBox = TokenBox(client, save = true, throwE = true, logE = false)

        // 过滤 svn 项目，目前 SVN项目只能从根组创建项目，所以项目组的分割后俩，项目的分割后三
        val svnProjectUrls = urls.filter {
            it.startsWith(tGitConfig.tSvnUrl.removeHttpPrefix())
        }.toSet()
        val noGroupSvnUrls = svnProjectUrls.filter { it.split("/").filter { s -> s.isNotBlank() }.size == 3 }
        if (svnProjectUrls.isNotEmpty()) {
            val rProjectUrls = svnProjectUrls.map { it.removeHttpPrefix() }.toSet()
            filterRecordWithTGitProjectsData(
                data = credToCredAndProjectData(data.credId ?: userId, credType, null),
                tokenBox = tokenBox,
                projectId = projectId,
                type = TGitProjectType.SVN,
                throwFun = null
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
                data = credToCredAndProjectData(data.credId ?: userId, credType, null),
                tokenBox = tokenBox,
                projectId = projectId,
                type = TGitProjectType.GIT,
                throwFun = null
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

        val tokenBox = TokenBox(client, true)
        repoIds.forEach { repoId ->
            val record = repoRecord[repoId] ?: return@forEach

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

        val (gitData, svnData) = recordToCredAndProjectData(repos) ?: return result.values.toList()
        val tokenBox = TokenBox(client, true)
        val filterFun: (project: TGitProjectInfo, tGitIds: MutableSet<Long>?) -> Boolean =
            filterFun@{ project, tGitIds ->
                if (tGitIds?.contains(project.id) != true) {
                    return@filterFun true
                }

                if (!project.httpsUrlToRepo.isNullOrBlank() || !project.httpUrlToRepo.isNullOrBlank()) {
                    result[project.id]?.url = project.httpsUrlToRepo ?: project.httpUrlToRepo!!
                }
                tGitIds.remove(project.id)

                true
            }
        filterRecordWithTGitProjectsData(svnData, tokenBox, projectId, TGitProjectType.SVN, null, filterFun)
        filterRecordWithTGitProjectsData(gitData, tokenBox, projectId, TGitProjectType.GIT, null, filterFun)

        return result.values.toList()
    }

    private fun credToCredAndProjectData(
        cred: String,
        credType: TGitCredType,
        repoIds: MutableSet<Long>?
    ): CredAndProjectData = mutableMapOf(credType to mutableMapOf(cred to repoIds))

    // 将数据库数据转换为可以和工蜂数据交互的数据类型
    private fun recordToCredAndProjectData(
        records: List<TProjectTgitIdLinkRecord>
    ): Pair<CredAndProjectData, CredAndProjectData>? {
        if (records.isEmpty()) {
            return null
        }

        val svnData = mutableMapOf<TGitCredType, MutableMap<String, MutableSet<Long>?>>()
        val gitData = mutableMapOf<TGitCredType, MutableMap<String, MutableSet<Long>?>>()
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

        return Pair(gitData, svnData)
    }

    // 使用工蜂的项目信息与输入数据做交互处理
    private fun filterRecordWithTGitProjectsData(
        data: CredAndProjectData,
        tokenBox: TokenBox,
        projectId: String,
        type: TGitProjectType,
        throwFun: ((e: Throwable, credType: TGitCredType, cred: String, repoIds: MutableSet<Long>?) -> Unit)?,
        run: (project: TGitProjectInfo, tGitIds: MutableSet<Long>?) -> Boolean
    ) {
        data.forEach { (credType, credAndRepos) ->
            credAndRepos.forEach credAndRepos@{ (cred, repoIds) ->
                try {
                    doFilterRecordWithTGitProjectsData(
                        tokenBox = tokenBox,
                        projectId = projectId,
                        credType = credType,
                        cred = cred,
                        type = type,
                        run = run,
                        repoIds = repoIds,
                        throwE = throwFun != null
                    )
                } catch (e: Throwable) {
                    throwFun?.invoke(e, credType, cred, repoIds)
                }
                return@credAndRepos
            }
        }
    }

    private fun doFilterRecordWithTGitProjectsData(
        tokenBox: TokenBox,
        projectId: String,
        credType: TGitCredType,
        cred: String,
        type: TGitProjectType,
        run: (project: TGitProjectInfo, tGitIds: MutableSet<Long>?) -> Boolean,
        repoIds: MutableSet<Long>?,
        throwE: Boolean
    ) {
        val token = tokenBox.get(
            projectId = projectId,
            credType = credType,
            cred = cred
        ) ?: return

        var page = 1
        val pageSize = 100

        while (true) {
            val projects = offshoreTGitApiClient.getProjectList(
                token = token,
                page = page,
                pageSize = pageSize,
                search = null,
                minAccessLevel = GitAccessLevelEnum.MASTER,
                type = type,
                throwE = throwE
            )

            // 过滤项目信息
            projects.forEach projects@{ project ->
                val ok = run(project, repoIds)
                if (!ok) {
                    return
                }
            }

            if (projects.size < 100 || (repoIds != null && repoIds.isEmpty())) {
                break
            }
            page++
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
        onlyDelete: Boolean?
    ): Boolean {
        // 审计
        ActionAuditContext.current()
            .addAttribute(ActionAuditContent.PROJECT_CODE_TEMPLATE, projectId)
            .scopeId = projectId

        if (onlyDelete == true) {
            projectTGitLinkDao.deleteUrl(dslContext, projectId, repoId)
            return true
        }

        try {
            return doUnbindingTGitLink(userId, projectId, repoId)
        } catch (e: Exception) {
            // 解绑失败仍然删除蓝盾的绑定
            projectTGitLinkDao.deleteUrl(dslContext, projectId, repoId)
            throw e
        }
    }

    // 单独抽出，方便做绑定失败的相关操作
    private fun doUnbindingTGitLink(
        userId: String,
        projectId: String,
        repoId: Long
    ): Boolean {
        val linkRecord = projectTGitLinkDao.fetchAny(dslContext, projectId, repoId) ?: return true
        val tokenType = TGitCredType.fromStringDefault(linkRecord.credType)
        val tokenBox = TokenBox(client = client, save = false, throwE = false, logE = true)
        val token = tokenBox.get(
            projectId = projectId,
            credType = tokenType,
            cred = when (tokenType) {
                TGitCredType.OAUTH_USER -> userId
                TGitCredType.CRED_ACCESS_TOKEN_ID -> {
                    if (linkRecord.cred == null) {
                        throw ErrorCodeException(
                            errorCode = ErrorCodeEnum.REMOVE_TGIT_LINK_ERROR.errorCode,
                            errorType = ErrorCodeEnum.REMOVE_TGIT_LINK_ERROR.errorType,
                            params = arrayOf("cred is null")
                        )
                    }
                    linkRecord.cred
                }
            }
        ) ?: throw ErrorCodeException(
            errorCode = ErrorCodeEnum.REMOVE_TGIT_LINK_ERROR.errorCode,
            errorType = ErrorCodeEnum.REMOVE_TGIT_LINK_ERROR.errorType,
            params = arrayOf(
                I18nUtil.getCodeLanMessage(
                    messageCode = ErrorCodeEnum.NO_TGIT_OAUTH_ERROR.errorCode,
                    params = arrayOf(userId, tGitConfig.tGitUrl)
                )
            )
        )

        // 校验下是否有删除的权限，svn项目用户在根目录下是否是审批人，git项目校验是否有master及以上权限
        if (linkRecord.gitType == TGitProjectType.SVN.name) {
            val svnRs = offshoreTGitApiClient.getSvnProjectAuth(
                token = token,
                projectId = repoId.toString()
            )
            if (svnRs?.approverUsers?.any { it.username == userId } != true) {
                throw ErrorCodeException(
                    errorCode = ErrorCodeEnum.REMOVE_TGIT_LINK_ERROR.errorCode,
                    errorType = ErrorCodeEnum.REMOVE_TGIT_LINK_ERROR.errorType,
                    params = arrayOf(
                        I18nUtil.getCodeLanMessage(
                            messageCode = ErrorCodeEnum.NO_TGIT_PREMISSION.errorCode,
                            params = arrayOf(userId)
                        )
                    )
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
                    errorCode = ErrorCodeEnum.REMOVE_TGIT_LINK_ERROR.errorCode,
                    errorType = ErrorCodeEnum.REMOVE_TGIT_LINK_ERROR.errorType,
                    params = arrayOf(
                        I18nUtil.getCodeLanMessage(
                            messageCode = ErrorCodeEnum.NO_TGIT_PREMISSION.errorCode,
                            params = arrayOf(userId)
                        )
                    )
                )
            }
        }

        // 取消关联时如果当前项目是最后一个关联的项目，那么就直接清空
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
            projectTGitLinkDao.deleteUrl(dslContext, projectId, repoId)
            return ok
        }

        // 如果还剩余其他关联项目，那么就拿其他关联项目取并集更新
        val ips = workspaceJoinDao.fetchWindowsWorkspaces(
            dslContext = dslContext,
            projectIds = otherProjects,
            checkField = listOf(TWorkspaceWindows.T_WORKSPACE_WINDOWS.HOST_IP),
            notStatus = setOf(
                WorkspaceStatus.PREPARING,
                WorkspaceStatus.DELETED,
                WorkspaceStatus.DELIVERING_FAILED
            )
        ).filter { !it.hostIp.isNullOrBlank() }.map { it.hostIp!!.substringAfter(".") }.toMutableSet()
        otherProjects.forEach { op ->
            ips.addAll(fetchDevcloudCvm(op))
        }
        val users = fetchProjectSpecAclUsers(otherProjects)

        val ok = incUpdateTGitProjectAcl(
            token = token,
            tGitProjectId = repoId,
            ips = ips,
            specUsers = users,
            rewrite = true
        )

        projectTGitLinkDao.deleteUrl(dslContext, projectId, repoId)

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
            streamBridge = streamBridge,
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
            streamBridge = streamBridge,
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
        val token = TokenBox(client, false).get(
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
        val token = TokenBox(client, false).get(
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
        ).filter { !it.hostIp.isNullOrBlank() }.map { it.hostIp!!.substringAfter(".") }.toMutableSet()
        ips.addAll(fetchDevcloudCvm(info.projectId))
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
        val tokenBox = TokenBox(client, save = true, throwE = true, logE = false)
        val credType = if (data.credId == null) {
            TGitCredType.OAUTH_USER
        } else {
            TGitCredType.CRED_ACCESS_TOKEN_ID
        }

        // 检查当前ID是否有权限
        val checkedId = mutableSetOf<Long>()
        val svnRepoIds = data.idMap.filter { it.value == TGitProjectType.SVN.name }.keys.toSet()
        if (svnRepoIds.isNotEmpty()) {
            filterRecordWithTGitProjectsData(
                data = credToCredAndProjectData(data.credId ?: userId, credType, svnRepoIds.toMutableSet()),
                tokenBox = tokenBox,
                projectId = data.projectId,
                type = TGitProjectType.SVN,
                throwFun = null
            ) { project, tGitIds ->
                if (project.id in svnRepoIds) {
                    checkedId.add(project.id)
                    tGitIds?.remove(project.id)
                }
                return@filterRecordWithTGitProjectsData true
            }
        }
        val gitRepoIds = data.idMap.filter { it.value == TGitProjectType.GIT.name }.keys.toSet()
        if (gitRepoIds.isNotEmpty()) {
            filterRecordWithTGitProjectsData(
                data = credToCredAndProjectData(data.credId ?: userId, credType, gitRepoIds.toMutableSet()),
                tokenBox = tokenBox,
                projectId = data.projectId,
                type = TGitProjectType.GIT,
                throwFun = null
            ) { project, tGitIds ->
                if (project.id in gitRepoIds) {
                    checkedId.add(project.id)
                    tGitIds?.remove(project.id)
                }
                return@filterRecordWithTGitProjectsData true
            }
        }

        if (checkedId.isNotEmpty()) {
            // 将信息凭据信息写入
            projectTGitLinkDao.batchUpdateCred(
                dslContext = dslContext,
                projectId = data.projectId,
                tgitIds = checkedId,
                status = TGitRepoStatus.TO_BE_MIGRATED,
                oauthUser = userId,
                cred = data.credId ?: userId,
                credType = credType
            )
            val res = linkTGit(
                projectId = data.projectId,
                repoIds = checkedId,
                innerToken = tokenBox.get(data.projectId, credType, (data.credId ?: userId))
            )
            if (res.values.contains(false)) {
                if (res.size == 1) {
                    throw ErrorCodeException(
                        errorCode = ErrorCodeEnum.REBINDING_SUB_ERROR.errorCode,
                        errorType = ErrorCodeEnum.REBINDING_SUB_ERROR.errorType
                    )
                } else {
                    throw ErrorCodeException(
                        errorCode = ErrorCodeEnum.REBINDING_ERROR.errorCode,
                        errorType = ErrorCodeEnum.REBINDING_ERROR.errorType
                    )
                }
            }
        }
        if (data.idMap.keys == checkedId) {
            return
        }

        if (data.idMap.size == 1) {
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.REBINDING_SUB_ERROR.errorCode,
                errorType = ErrorCodeEnum.REBINDING_SUB_ERROR.errorType
            )
        } else {
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

    private fun fetchDevcloudCvm(
        projectId: String
    ): Set<String> {
        try {
            var page = 1
            val pageSize = 100
            val result = mutableSetOf<String>()

            while (true) {
                val cvmPage = devcloudService.fetchCVMList(
                    userId = "landun",
                    project = projectId,
                    page = page,
                    pageSize = pageSize
                )

                // 过滤项目信息
                cvmPage.records.forEach { cvm ->
                    result.add(cvm.ip ?: return@forEach)
                }

                if (cvmPage.count < pageSize) {
                    break
                }
                page++
            }

            return result
        } catch (e: Exception) {
            logger.error("$LOG_UPDATE_TGIT_ACL_TAG|fetchDevcloudCvm error", e)
            return emptySet()
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
        val tokenBox = TokenBox(client, true)
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
        val lock = RedisLock(redisOperation, REDIS_DAILY_USER_AUTHDOCHECK, 3600L)
        try {
            if (!lock.tryLock()) {
                logger.info("dailyUserAuthDoCheck, get lock failed, skip")
                return
            }
            logger.info("dailyUserAuthDoCheck start")
            // 基本上每个项目下的权限都是这个项目使用的，所以以项目为做检查呢
            val allProjects = projectTGitLinkDao.fetchAllProject(dslContext)
            allProjects.forEach { project ->
                subDailyUserAuthDoCheck(project)
            }
            logger.info("dailyUserAuthDoCheck end")
        } catch (e: Throwable) {
            logger.error("$LOG_UPDATE_TGIT_ACL_TAG|dailyUserAuthDoCheck failed", e)
        } finally {
            lock.unlock()
        }
    }

    private fun subDailyUserAuthDoCheck(
        projectId: String
    ) {
        val records = projectTGitLinkDao.fetch(dslContext, projectId, null)
        val recordsTGitMap = records.associateBy { it.tgitId }.toMutableMap()

        // 所有可以恢复的记录
        val canAvailableRecords = mutableSetOf<Long>()

        val (gitData, svnData) = recordToCredAndProjectData(records) ?: return
        val tokenBox = TokenBox(client, true)
        // 查询出现异常时，先打日志进行告警,不进行状态转换
        val throwFun: (e: Throwable, credType: TGitCredType, cred: String, repoIds: MutableSet<Long>?) -> Unit =
            throwFun@{ e, credType, cred, repoIds ->
                // 401的不用告警，就是没有权限
                if (e is RemoteServiceException && e.httpStatus == 401) {
                    return@throwFun
                }
                repoIds?.forEach { id -> recordsTGitMap.remove(id) }
                logger.error(
                    "$LOG_UPDATE_TGIT_ACL_TAG|filterRecordWithTGitProjectsData|$projectId|$credType|$cred error",
                    e
                )
            }

        val filterFun: (project: TGitProjectInfo, tGitIds: MutableSet<Long>?) -> Boolean =
            filterFun@{ project, tGitIds ->
                val record = recordsTGitMap[project.id] ?: return@filterFun true

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

                recordsTGitMap.remove(project.id)
                tGitIds?.remove(project.id)

                true
            }
        filterRecordWithTGitProjectsData(svnData, tokenBox, projectId, TGitProjectType.SVN, throwFun, filterFun)
        filterRecordWithTGitProjectsData(gitData, tokenBox, projectId, TGitProjectType.GIT, throwFun, filterFun)

        // 恢复状态已经正常的
        if (canAvailableRecords.isNotEmpty()) {
            projectTGitLinkDao.batchUpdateStatus(dslContext, projectId, canAvailableRecords, TGitRepoStatus.AVAILABLE)
        }

        // 剩下的就是没有找到符合权限的需要发通知
        val result = mutableMapOf<String, MutableMap<Long, String>>()
        recordsTGitMap.values.forEach { record ->
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
        logger.info("dailyUserAuthDoCheck|result|$result")

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
        val users = mutableSetOf(userId).apply { managers?.let { addAll(it) } }.joinToString(",")
        try {
            bkitsmService.createCheckTicket(projectId, "devops", users, urls)
        } catch (e: Exception) {
            logger.error("$LOG_UPDATE_TGIT_ACL_TAG|sendCheckNotify|$users|$projectId|$projectName|error", e)
        }
    }

    fun checkProjectExist(projectId: String): Boolean {
        projectTGitLinkDao.fetch(dslContext, projectId, null).ifEmpty { return false }
        return true
    }

    fun deleteRepos(projectId: String, tGitIds: Set<Long>): Boolean {
        projectTGitLinkDao.deleteIds(dslContext, projectId, tGitIds)
        return true
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

        // 工蜂巡检定时任务锁
        private const val REDIS_DAILY_USER_AUTHDOCHECK = "remotedev:tgit:daily:check:lock"
    }
}
