package com.tencent.devops.common.websocket

import com.tencent.devops.common.websocket.pojo.BuildPageInfo

interface IPath {
    fun buildPage(buildPageInfo: BuildPageInfo): String
}