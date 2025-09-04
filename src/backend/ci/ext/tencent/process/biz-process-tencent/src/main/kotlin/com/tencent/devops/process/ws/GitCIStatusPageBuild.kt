package com.tencent.devops.process.ws

import com.tencent.devops.common.websocket.pojo.BuildPageInfo
import com.tencent.devops.process.websocket.page.StatusPageBuild

class GitCIStatusPageBuild : StatusPageBuild() {
    override fun extStatusPage(buildPageInfo: BuildPageInfo): String? {
        return "/pipeline/${buildPageInfo.projectId}"
    }
}
