package com.tencent.devops.process.ws

import com.tencent.devops.common.websocket.pojo.BuildPageInfo
import com.tencent.devops.process.websocket.page.DetailPageBuild

class GitCIDetailPageBuild : DetailPageBuild() {
    override fun extDetailPage(buildPageInfo: BuildPageInfo): String? {
        return "/pipeline/${buildPageInfo.pipelineId}/detail/${buildPageInfo.buildId}/${buildPageInfo.projectId}"
    }
}
