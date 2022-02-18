package com.tencent.devops.process.websocket.page

import com.tencent.devops.common.websocket.pojo.BuildPageInfo

class DefaultStatusPageBuild : StatusPageBuild() {
    override fun extStatusPage(buildPageInfo: BuildPageInfo): String? {
        return null
    }
}
