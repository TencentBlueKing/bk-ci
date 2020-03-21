package com.tencent.devops.artifactory.service

import java.io.File

interface FileService {
    fun downloadFileTolocal(
        projectId: String,
        pipelineId: String,
        buildId: String,
        fileName: String,
        isCustom: Boolean
    ): List<File>
}