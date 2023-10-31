package com.tencent.devops.remotedev.service.gitproxy

import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.remotedev.dao.RemoteDevCodeProxyDao
import com.tencent.devops.remotedev.pojo.gitproxy.CodeProxyConf
import com.tencent.devops.remotedev.pojo.gitproxy.CreateGitProxyData
import com.tencent.devops.remotedev.pojo.gitproxy.FetchRepoResp
import org.jooq.DSLContext
import org.jooq.JSON
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit

@Service
class GitProxyService @Autowired constructor(
    private val gitproxyBkRepoClient: GitproxyBkRepoClient,
    private val redisOperation: RedisOperation,
    private val dslContext: DSLContext,
    private val codeProxyDao: RemoteDevCodeProxyDao
) {
    fun createRepo(
        userId: String,
        data: CreateGitProxyData
    ): Boolean {
        // 判断项目是否存在，不存在创建
        if (!ifExistBkRepoProject(userId, data.projectId)) {
            gitproxyBkRepoClient.createProject(userId, data.projectId)
            redisOperation.set("REDIS_BKREPO_PROJECT:${data.projectId}", "", 10 * 60)
        }
        gitproxyBkRepoClient.createRepo(
            userId = userId,
            projectId = data.projectId,
            repoName = data.repoName,
            url = data.url,
            desc = data.desc,
            gitType = data.gitType,
            category = BkRepoCategory.PROXY,
            enableLfs = false
        )
        if (data.enableLfsCache == true && data.gitType == ScmType.CODE_GIT) {
            gitproxyBkRepoClient.createRepo(
                userId = userId,
                projectId = data.projectId,
                repoName = "Lfs_${data.repoName}",
                url = data.url,
                desc = data.desc,
                gitType = data.gitType,
                category = BkRepoCategory.REMOTE,
                enableLfs = true
            )
        }
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
            val conf = JsonUtil.to<CodeProxyConf>(record.conf.data())
            FetchRepoResp(
                url = record.url,
                proxyUrl = conf.proxyUrl,
                creator = record.creator,
                createdDate = dateFormat.format(record.createTime),
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

    fun deleteRepo(
        userId: String,
        projectId: String,
        repoName: String
    ): Boolean {
        gitproxyBkRepoClient.deleteRepo(userId, projectId, repoName)
        return true
    }

    private fun scmType2ProxyType(scmType: ScmType?): String {
        return when (scmType) {
            ScmType.CODE_SVN -> "SVN"
            else -> "GIT"
        }
    }

    companion object {
        // bkrepo project 缓存
        const val REDIS_BKREPO_PROJECT = "remotedev:bkrepo:existProject"
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    }
}
