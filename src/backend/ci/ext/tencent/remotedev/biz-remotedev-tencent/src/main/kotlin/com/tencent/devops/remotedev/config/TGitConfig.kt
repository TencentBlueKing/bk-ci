package com.tencent.devops.remotedev.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * 工蜂相关配置
 */
@Component
class TGitConfig {
    @Value("\${tgit.gitUrl:}")
    val tGitUrl: String = ""

    @Value("\${tgit.svnUrl:}")
    val tSvnUrl: String = ""

    @Value("\${tgit.ip:}")
    val tGitIp: String = ""

    @Value("\${tgit.expiredPermTmpCode:}")
    val expiredPermTmpCode: String = ""
}
