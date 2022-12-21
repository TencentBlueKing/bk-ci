package com.tencent.devops.process.websocket.page

import com.tencent.devops.common.websocket.pojo.BuildPageInfo

class GithubDetailPageBuild : DetailPageBuild() {
    override fun extDetailPage(buildPageInfo: BuildPageInfo): String? {
        return "/pipeline/${buildPageInfo.pipelineId}/detail/${buildPageInfo.buildId}/${buildPageInfo.projectId}"
    }
}
