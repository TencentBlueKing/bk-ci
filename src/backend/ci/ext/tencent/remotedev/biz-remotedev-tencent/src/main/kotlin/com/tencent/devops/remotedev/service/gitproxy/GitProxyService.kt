package com.tencent.devops.remotedev.service.gitproxy

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.bk.audit.annotations.ActionAuditRecord
import com.tencent.bk.audit.annotations.AuditAttribute
import com.tencent.bk.audit.annotations.AuditInstanceRecord
import com.tencent.bk.audit.context.ActionAuditContext
import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.audit.ActionAuditContent
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.auth.api.ResourceTypeId
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.dao.RemoteDevCodeProxyDao
import com.tencent.devops.remotedev.pojo.gitproxy.CodeProxyConf
import com.tencent.devops.remotedev.pojo.gitproxy.CreateGitProxyData
import com.tencent.devops.remotedev.pojo.gitproxy.CreateRepoRespData
import com.tencent.devops.remotedev.pojo.gitproxy.FetchRepoResp
import com.tencent.devops.remotedev.pojo.gitproxy.RefreshCodeProxyData
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter

@Service
class GitProxyService @Autowired constructor(
    private val gitproxyBkRepoClient: GitproxyBkRepoClient,
    private val redisOperation: RedisOperation,
    private val dslContext: DSLContext,
    private val codeProxyDao: RemoteDevCodeProxyDao
) {
    @ActionAuditRecord(
        actionId = ActionId.CODE_PROXY_CREATE,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.CODE_PROXY,
            instanceNames = "#data?.repoName",
            instanceIds = "#data?.repoName"
        ),
        content = ActionAuditContent.CODE_PROXY_CREATE_CONTENT
    )
    fun createRepo(
        userId: String,
        data: CreateGitProxyData
    ): Boolean {
        // 判断是否有重复数据
        if (codeProxyDao.checkExistRepo(dslContext, data.projectId, data.repoName)) {
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.CODEPROXY_EXIST_ERROR.errorCode,
                params = arrayOf(data.projectId, data.repoName)
            )
        }
        // 判断项目是否存在，不存在创建
        if (!ifExistBkRepoProject(userId, data.projectId)) {
            gitproxyBkRepoClient.createProject(userId, data.projectId)
            redisOperation.set("REDIS_BKREPO_PROJECT:${data.projectId}", "", 10 * 60)
        }
        val respData = gitproxyBkRepoClient.createRepo(
            userId = userId,
            projectId = data.projectId,
            repoName = data.repoName,
            url = data.url,
            desc = data.desc,
            gitType = data.gitType,
            category = BkRepoCategory.PROXY,
            enableLfs = false
        )
        // 审计
        ActionAuditContext.current()
            .addAttribute(ActionAuditContent.PROJECT_CODE_TEMPLATE, data.projectId)
            .scopeId = data.projectId

        var lfsRespData: CreateRepoRespData? = null
        val enableLfs = data.enableLfsCache == true && data.gitType == ScmType.CODE_TGIT
        if (enableLfs) {
            lfsRespData = gitproxyBkRepoClient.createRepo(
                userId = userId,
                projectId = data.projectId,
                repoName = "$LFS_REPONAME_PREFIX${data.repoName}",
                url = data.url,
                desc = data.desc,
                gitType = data.gitType,
                category = BkRepoCategory.REMOTE,
                enableLfs = true
            )
        }
        codeProxyDao.addCodeProxy(
            dslContext = dslContext,
            projectId = data.projectId,
            name = data.repoName,
            type = scmType2ProxyType(data.gitType)!!,
            url = data.url,
            conf = CodeProxyConf(
                proxyUrl = respData?.configuration?.settings?.clientUrl,
                lfsUrl = lfsRespData?.configuration?.settings?.clientUrl
            ),
            desc = data.desc,
            creator = userId,
            enableLfs = enableLfs
        )
        return true
    }

    private fun ifExistBkRepoProject(userId: String, projectId: String): Boolean {
        if (redisOperation.get("REDIS_BKREPO_PROJECT:$projectId") != null) {
            return true
        }
        if (gitproxyBkRepoClient.existProject(userId, projectId) == true) {
            redisOperation.set("REDIS_BKREPO_PROJECT:$projectId", "", 10 * 60)
            return true
        }
        return false
    }

    fun fetchRepo(
        userId: String,
        projectId: String,
        page: Int,
        pageSize: Int,
        gitType: ScmType?
    ): Page<FetchRepoResp> {
        val pageLimit = PageUtil.convertPageSizeToSQLLimit(page, pageSize)
        val count = codeProxyDao.countFetchCodeProxy(
            dslContext = dslContext,
            projectId = projectId,
            type = scmType2ProxyType(gitType)
        )
        val records = codeProxyDao.fetchCodeProxy(
            dslContext = dslContext,
            projectId = projectId,
            type = scmType2ProxyType(gitType),
            limit = pageLimit
        )

        val resp = records.map { record ->
            val conf = JsonUtil.getObjectMapper().readValue(
                record.conf.data(),
                object : TypeReference<CodeProxyConf>() {}
            )
            FetchRepoResp(
                url = record.url,
                proxyUrl = conf.proxyUrl,
                creator = record.creator,
                createdDate = record.createTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                repoName = record.name,
                type = record.type,
                desc = record.desc,
                lfsUrl = conf.lfsUrl
            )
        }

        return Page(
            pageNumber = page,
            pageSize = pageSize,
            totalRecords = count.toLong(),
            records = resp
        )
    }

    @ActionAuditRecord(
        actionId = ActionId.CODE_PROXY_DELETE,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.CODE_PROXY,
            instanceNames = "#repoName",
            instanceIds = "#repoName"
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.CODE_PROXY_DELETE_CONTENT
    )
    fun deleteRepo(
        userId: String,
        projectId: String,
        repoName: String
    ): Boolean {
        // 从自己的数据库先查在删
        val record = codeProxyDao.fetchSingleCodeProxy(dslContext, projectId, repoName)
        // 查不到做保险删除
        if (record == null) {
            gitproxyBkRepoClient.deleteRepo(userId, projectId, repoName)
            gitproxyBkRepoClient.deleteRepo(userId, projectId, "$LFS_REPONAME_PREFIX$repoName")
            return true
        }
        gitproxyBkRepoClient.deleteRepo(userId, projectId, repoName)
        if (record.enableLfs == true) {
            try {
                gitproxyBkRepoClient.deleteRepo(userId, projectId, "$LFS_REPONAME_PREFIX$repoName")
            } catch (e: Exception) {
                logger.warn("deleteRepo delete lfs repo error", e)
            }
        }
        codeProxyDao.deleteCodeProxy(dslContext, record.projectId, record.name)
        return true
    }

    fun refreshCodeProxy(projectId: String) {
        val proxys = gitproxyBkRepoClient.fetchRepo(
            userId = "admin",
            projectId = projectId,
            page = 1,
            pageSize = 100,
            gitType = null,
            category = BkRepoCategory.PROXY
        )
        val lfs = gitproxyBkRepoClient.fetchRepo(
            userId = "admin",
            projectId = projectId,
            page = 1,
            pageSize = 100,
            gitType = null,
            category = BkRepoCategory.REMOTE
        ).records.associateBy { it.name }

        val data = proxys.records.map { proxy ->
            val lfsUrl = lfs["$LFS_REPONAME_PREFIX${proxy.name}"]?.configuration?.settings?.clientUrl
            RefreshCodeProxyData(
                projectId = proxy.projectId,
                name = proxy.name,
                type = proxy.type,
                url = proxy.configuration.proxy?.url ?: "",
                conf = CodeProxyConf(
                    proxyUrl = proxy.configuration.settings?.clientUrl,
                    lfsUrl
                ),
                desc = proxy.description,
                creator = proxy.createdBy,
                enableLfs = lfsUrl != null
            )
        }

        codeProxyDao.batchAddProxy(dslContext, data)
    }

    private fun scmType2ProxyType(scmType: ScmType?): String? {
        if (scmType == null) {
            return null
        }
        return when (scmType) {
            ScmType.CODE_SVN -> "SVN"
            else -> "GIT"
        }
    }

    companion object {
        private const val LFS_REPONAME_PREFIX = "Lfs_"
        private val logger = LoggerFactory.getLogger(GitProxyService::class.java)
    }
}
