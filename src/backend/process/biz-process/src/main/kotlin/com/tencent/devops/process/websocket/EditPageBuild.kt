package com.tencent.devops.process.websocket

import com.tencent.devops.common.websocket.IPath
import com.tencent.devops.common.websocket.pojo.BuildPageInfo

class EditPageBuild : IPath {
    override fun buildPage(buildPageInfo: BuildPageInfo): String {
        val page = "/console/pipeline/${buildPageInfo.projectId}/${buildPageInfo.pipelineId}/edit"
        return page
    }
}