package com.tencent.devops.process.websocket.page

import com.tencent.devops.common.websocket.pojo.BuildPageInfo

class GithubStatusPageBuild : StatusPageBuild() {
    override fun extStatusPage(buildPageInfo: BuildPageInfo): String? {
        return "/pipeline/${buildPageInfo.projectId}"
    }
}
