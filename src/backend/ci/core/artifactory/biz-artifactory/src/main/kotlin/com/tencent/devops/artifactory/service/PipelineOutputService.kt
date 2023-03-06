package com.tencent.devops.artifactory.service

import com.tencent.devops.artifactory.pojo.PipelineOutput
import com.tencent.devops.artifactory.pojo.PipelineOutputSearchOption

interface PipelineOutputService {
    fun search(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        option: PipelineOutputSearchOption?
    ): List<PipelineOutput>
}
