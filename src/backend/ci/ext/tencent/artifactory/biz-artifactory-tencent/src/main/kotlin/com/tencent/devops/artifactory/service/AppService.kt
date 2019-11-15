package com.tencent.devops.artifactory.service

import com.tencent.devops.artifactory.pojo.Url
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType

interface AppService {
    fun getExternalDownloadUrl(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String,
        ttl: Int,
        directed: Boolean = false
    ): Url

    fun getExternalDownloadUrlDirected(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String,
        ttl: Int
    ): Url

    fun getExternalPlistDownloadUrl(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String,
        ttl: Int,
        directed: Boolean = false
    ): Url

    fun getPlistFile(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String,
        ttl: Int,
        directed: Boolean = false,
        experienceHashId: String?
    ): String
}