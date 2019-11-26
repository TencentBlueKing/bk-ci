package com.tencent.devops.artifactory.service

import com.tencent.devops.artifactory.pojo.DownloadUrl
import com.tencent.devops.artifactory.pojo.Url
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.common.pipeline.enums.ChannelCode

interface RepoDownloadService {
    fun getDownloadUrl(token: String): DownloadUrl

    fun serviceGetExternalDownloadUrl(userId: String, projectId: String, artifactoryType: ArtifactoryType, argPath: String, ttl: Int, directed: Boolean = false): Url

    fun serviceGetInnerDownloadUrl(userId: String, projectId: String, artifactoryType: ArtifactoryType, argPath: String, ttl: Int, directed: Boolean = false): Url

    fun getDownloadUrl(userId: String, projectId: String, artifactoryType: ArtifactoryType, argPath: String, channelCode: ChannelCode?): Url

    fun getIoaUrl(userId: String, projectId: String, artifactoryType: ArtifactoryType, argPath: String): Url

    fun getExternalUrl(userId: String, projectId: String, artifactoryType: ArtifactoryType, argPath: String): Url

    fun shareUrl(userId: String, projectId: String, artifactoryType: ArtifactoryType, argPath: String, ttl: Int, downloadUsers: String)

    fun getThirdPartyDownloadUrl(projectId: String, pipelineId: String, buildId: String, artifactoryType: ArtifactoryType, argPath: String, ttl: Int?): List<String>
}