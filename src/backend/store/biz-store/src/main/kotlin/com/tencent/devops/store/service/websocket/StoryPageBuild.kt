package com.tencent.devops.store.service.websocket

import com.tencent.devops.common.websocket.IPath
import com.tencent.devops.common.websocket.pojo.BuildPageInfo

class StoryPageBuild : IPath {
    override fun buildPage(buildPageInfo: BuildPageInfo): String {
        val page = "/console/store/releaseProgress/shelf/${buildPageInfo.atomId}"
        return page
    }
}