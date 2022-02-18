package com.tencent.devops.process.websocket.page

import com.tencent.devops.common.websocket.pojo.BuildPageInfo

class DefaultDetailPageBuild : DetailPageBuild() {
    override fun extDetailPage(buildPageInfo: BuildPageInfo): String? {
        return null
    }
}
