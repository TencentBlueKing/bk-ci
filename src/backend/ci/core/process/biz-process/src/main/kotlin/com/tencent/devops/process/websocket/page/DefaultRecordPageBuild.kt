package com.tencent.devops.process.websocket.page

import com.tencent.devops.common.websocket.pojo.BuildPageInfo

class DefaultRecordPageBuild : RecordPageBuild() {
    override fun extRecordPage(buildPageInfo: BuildPageInfo): String? {
        return null
    }
}
