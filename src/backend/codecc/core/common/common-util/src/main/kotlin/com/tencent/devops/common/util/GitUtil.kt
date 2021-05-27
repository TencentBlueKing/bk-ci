package com.tencent.devops.common.util

import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder

object GitUtil {

    fun urlDecode(s: String) = URLDecoder.decode(s, "UTF-8")

    fun urlEncode(s: String) = URLEncoder.encode(s, "UTF-8")

    fun getUrl(projectName: String, sshHost: String) =
        "$sshHost:$projectName.git"

    fun getProjectName(gitUrl: String): String {
        return if (gitUrl.startsWith("http")) {
            URL(gitUrl).path.removeSuffix(".git").removePrefix("/")
        } else {
            gitUrl.split(":").last().removeSuffix(".git")
        }
    }
}
