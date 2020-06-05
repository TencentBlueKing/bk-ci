package com.tencent.devops.artifactory.service

import com.tencent.devops.artifactory.pojo.FileInfo

interface PipelineBuildArtifactoryService {

    fun getArtifactList(projectId: String, pipelineId: String, buildId: String): List<FileInfo>

    fun synArtifactoryInfo(
        userId: String,
        artifactList: List<FileInfo>,
        projectId: String,
        pipelineId: String,
        buildId: String,
        buildNum: Int
    )
}