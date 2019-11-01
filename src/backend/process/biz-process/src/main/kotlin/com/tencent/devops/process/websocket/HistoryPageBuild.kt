package com.tencent.devops.process.websocket

import com.tencent.devops.common.websocket.IPath
import com.tencent.devops.common.websocket.pojo.BuildPageInfo

class HistoryPageBuild : IPath {
    override fun buildPage(buildPageInfo: BuildPageInfo): String {
        var page = "/console/pipeline/${buildPageInfo.projectId}/${buildPageInfo.pipelineId}/history"
        return page
    }
}