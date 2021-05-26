package com.tencent.devops.common.auth.utils

import com.tencent.devops.common.api.exception.ParamBlankException

object GitCIUtils {

    const val GITLABLE = "git_"

    fun getGitCiProjectId(projectCode: String): String {
        if (!projectCode.contains(GITLABLE)) {
            throw ParamBlankException("gitCI project not start git_ $projectCode")
        }
        return projectCode.substringAfter(GITLABLE)
    }
}
