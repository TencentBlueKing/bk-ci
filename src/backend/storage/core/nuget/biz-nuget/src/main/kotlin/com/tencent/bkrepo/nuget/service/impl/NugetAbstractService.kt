package com.tencent.bkrepo.nuget.service.impl

import com.tencent.bkrepo.common.api.constant.CharPool.SLASH
import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.api.PackageClient
import org.springframework.beans.factory.annotation.Autowired

open class NugetAbstractService {
    @Autowired
    lateinit var nodeClient: NodeClient

    @Autowired
    lateinit var packageClient: PackageClient

    fun getV2Url(artifactInfo: ArtifactInfo): String {
        val url = HttpContextHolder.getRequest().requestURL
        val domain = url.delete(url.length - HttpContextHolder.getRequest().requestURI.length, url.length)
        return domain.append(SLASH).append(artifactInfo.getRepoIdentify()).toString()
    }

    fun getV3Url(artifactInfo: ArtifactInfo): String {
        val url = HttpContextHolder.getRequest().requestURL
        val domain = url.delete(url.length - HttpContextHolder.getRequest().requestURI.length, url.length)
        return domain.append(SLASH).append("v3").append(SLASH).append(artifactInfo.getRepoIdentify()).toString()
    }

    /**
     * check node exists
     */
    fun exist(projectId: String, repoName: String, fullPath: String): Boolean {
        return nodeClient.checkExist(projectId, repoName, fullPath).data ?: false
    }
}
