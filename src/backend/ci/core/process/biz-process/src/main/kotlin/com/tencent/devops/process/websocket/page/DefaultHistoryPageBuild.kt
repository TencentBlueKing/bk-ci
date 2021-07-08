package com.tencent.devops.process.websocket.page

import com.tencent.devops.common.websocket.pojo.BuildPageInfo

class DefaultHistoryPageBuild : HistoryPageBuild() {
    override fun extHistoryPage(buildPageInfo: BuildPageInfo): String? {
        return null
    }
}
