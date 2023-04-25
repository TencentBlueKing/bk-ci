package com.tencent.devops.process.ws

import com.tencent.devops.common.websocket.pojo.BuildPageInfo
import com.tencent.devops.process.websocket.page.HistoryPageBuild

class GitCIHistoryPageBuild : HistoryPageBuild() {
    override fun extHistoryPage(buildPageInfo: BuildPageInfo): String? {
        return "/pipeline/${buildPageInfo.pipelineId}/${buildPageInfo.projectId}"
    }
}
