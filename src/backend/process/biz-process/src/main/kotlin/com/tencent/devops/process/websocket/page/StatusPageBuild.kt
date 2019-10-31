package com.tencent.devops.process.websocket.page

import com.tencent.devops.common.websocket.IPath
import com.tencent.devops.common.websocket.pojo.BuildPageInfo

class StatusPageBuild : IPath {
    override fun buildPage(buildPageInfo: BuildPageInfo): String {
        var page = "/console/pipeline/${buildPageInfo.projectId}/list"
        return page
    }
}