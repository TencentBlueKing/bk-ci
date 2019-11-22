package com.tencent.devops.gitci.client

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.gitci.pojo.GitRepositoryConf
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.scm.api.ServiceGitResource
import com.tencent.devops.scm.pojo.CommitCheckRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.Collections

@Service
class ScmClient @Autowired constructor(
    private val client: Client
) {
    fun pushCommitCheck(
        commitId: String,
        description: String,
        mergeRequestId: Long,
        buildId: String,
        userId: String,
        status: String,
        gitProjectConf: GitRepositoryConf
    ) = try {
        val titleData = mutableListOf<String>()
        val resultMap = mutableMapOf<String, MutableList<List<String>>>()

        val token = getAccessToken(gitProjectConf.gitProjectId).first
        val buildNum = getBuildNum(gitProjectConf.projectCode.toString(), buildId)
        val request = CommitCheckRequest(
            gitProjectConf.gitProjectId.toString(),
            gitProjectConf.gitHttpUrl,
            ScmType.CODE_GIT,
            null,
            null,
            token,
            null,
            commitId,
            status,
            gitProjectConf.homepage + "/ci/pipelines#/build/" + buildId + "?buildNum=" + buildNum,
            "",
            description,
            false,
            mergeRequestId,
            Pair(titleData, resultMap)
        )
        logger.info("user $userId buildId $buildId pushCommitCheck: $request")
        client.getScm(ServiceGitResource::class).addCommitCheck(request)
    } catch (e: Exception) {
        logger.error("user $userId buildId $buildId pushCommitCheck error.", e)
    }

    private fun getAccessToken(gitProjectId: Long): Pair<String, String?> {
        val gitOauthData = client.getScm(ServiceGitResource::class).getToken(gitProjectId).data
            ?: throw RuntimeException("cannot found oauth access token for user($gitProjectId)")
        return gitOauthData.accessToken to null
    }

    private fun getBuildNum(projectCode: String, buildId: String): String {
        val buildHistoryList = client.get(ServiceBuildResource::class).getBatchBuildStatus(projectCode, Collections.singleton(buildId), ChannelCode.GIT).data
        return if (null == buildHistoryList || buildHistoryList.isEmpty()) {
            logger.info("Get branch build history list return empty, gitProjectId: $projectCode")
            ""
        } else {
            buildHistoryList[0].buildNum.toString()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ScmClient::class.java)
    }
}