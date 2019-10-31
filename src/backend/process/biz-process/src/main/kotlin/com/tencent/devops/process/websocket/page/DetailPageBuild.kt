package com.tencent.devops.process.websocket.page

import com.tencent.devops.common.websocket.IPath
import com.tencent.devops.common.websocket.pojo.BuildPageInfo

class DetailPageBuild : IPath {
    override fun buildPage(buildPageInfo: BuildPageInfo): String {
        var page = "/console/pipeline/${buildPageInfo.projectId}/${buildPageInfo.pipelineId}/detail/${buildPageInfo.buildId}"
        return page
    }
}