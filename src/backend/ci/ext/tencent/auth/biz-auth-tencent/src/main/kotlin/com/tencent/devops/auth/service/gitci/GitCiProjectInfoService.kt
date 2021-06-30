package com.tencent.devops.auth.service.gitci

import com.google.common.cache.CacheBuilder
import com.tencent.devops.common.client.Client
import com.tencent.devops.scm.api.ServiceGitCiResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.TimeUnit

class GitCiProjectInfoService @Autowired constructor(
    val client: Client
) {
    private val gitCIUserCache = CacheBuilder.newBuilder()
        .maximumSize(2000)
        .expireAfterWrite(24, TimeUnit.HOURS)
        .build<String/*userId*/, String>()

    private val projectPublicCache = CacheBuilder.newBuilder()
        .maximumSize(2000)
        .expireAfterWrite(1, TimeUnit.HOURS)
        .build<String/*project*/, String?>()

    fun checkProjectPublic(projectCode: String): Boolean {
        if (!projectPublicCache.getIfPresent(projectCode).isNullOrEmpty()) {
            return true
        } else {
            val gitProjectInfo = client.getScm(ServiceGitCiResource::class).getGitCodeProjectInfo(projectCode).data
            if (gitProjectInfo != null) {
                logger.info("project $projectCode visibilityLevel: ${gitProjectInfo?.visibilityLevel}")
                if (gitProjectInfo.visibilityLevel != null && gitProjectInfo.visibilityLevel!! > 0) {
                    projectPublicCache.put(projectCode, gitProjectInfo.visibilityLevel.toString())
                    return true
                }
            } else {
                logger.warn("project $projectCode get projectInfo is empty")
            }
        }
        return false
    }

    fun getGitUserByRtx(rtxUserId: String, projectCode: String): String? {
        return if (!gitCIUserCache.getIfPresent(rtxUserId).isNullOrEmpty()) {
            gitCIUserCache.getIfPresent(rtxUserId)!!
        } else {
            val gitUserId = client.getScm(ServiceGitCiResource::class).getGitUserId(rtxUserId, projectCode).data
            if (gitUserId != null) {
                gitCIUserCache.put(rtxUserId, gitUserId)
            }
            gitUserId
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(GitCiProjectInfoService::class.java)
    }
}
