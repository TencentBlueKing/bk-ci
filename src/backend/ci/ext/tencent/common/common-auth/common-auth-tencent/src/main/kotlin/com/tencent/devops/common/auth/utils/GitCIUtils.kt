package com.tencent.devops.common.auth.utils

object GitCIUtils {

    const val GITLABLE = "git_"

    fun getGitCiProjectId(projectCode: String): String {
        if (!projectCode.contains(GITLABLE)) {
            return projectCode
        }
        return projectCode.substringAfter(GITLABLE)
    }
}
