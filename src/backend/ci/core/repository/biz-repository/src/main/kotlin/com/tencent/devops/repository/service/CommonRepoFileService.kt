package com.tencent.devops.repository.service

import com.tencent.devops.repository.pojo.enums.RepoAuthType
import com.tencent.devops.repository.service.scm.IGitService
import com.tencent.devops.scm.utils.code.git.GitUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CommonRepoFileService @Autowired constructor(
    private val gitService: IGitService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(CommonRepoFileService::class.java)
    }

    fun getGitFileContent(repoUrl: String, filePath: String, ref: String?, token: String, authType: RepoAuthType?, subModule: String?): String {
        val projectName = if (subModule.isNullOrBlank()) GitUtils.getProjectName(repoUrl) else subModule
        return gitService.getGitFileContent(
                repoName = projectName!!,
                filePath = filePath.removePrefix("/"),
                authType = authType,
                token = token,
                ref = ref ?: "master")
    }
}