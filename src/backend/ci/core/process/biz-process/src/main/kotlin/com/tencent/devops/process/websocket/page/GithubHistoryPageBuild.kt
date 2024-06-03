package com.tencent.devops.process.websocket.page

import com.tencent.devops.common.websocket.pojo.BuildPageInfo

class GithubHistoryPageBuild : HistoryPageBuild() {
    override fun extHistoryPage(buildPageInfo: BuildPageInfo): String? {
        return "/pipeline/${buildPageInfo.pipelineId}/${buildPageInfo.projectId}"
    }
}
