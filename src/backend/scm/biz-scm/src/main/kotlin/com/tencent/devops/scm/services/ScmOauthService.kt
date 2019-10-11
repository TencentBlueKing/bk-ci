package com.tencent.devops.scm.services

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.repository.pojo.enums.CodeSvnRegion
import com.tencent.devops.scm.pojo.RevisionInfo
import com.tencent.devops.scm.pojo.TokenCheckResult
import com.tencent.devops.scm.pojo.request.CommitCheckRequest
import com.tencent.devops.scm.utils.QualityUtils
import com.tencent.devops.scm.utils.ScmOauthFactory
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class ScmOauthService {

    @Value("\${git.webhook.callback.url:#{null}}")
    private val gitHookUrl: String? = null

    @Value("\${gitlab.webhook.callback.url:#{null}}")
    private val gitlabHookUrl: String? = null

    @Value("\${svn.webhook.callback.url:#{null}}")
    private val svnHookUrl: String? = null

    fun getLatestRevision(
        projectName: String,
        url: String,
        type: ScmType,
        branchName: String?,
        privateKey: String?,
        passPhrase: String?,
        token: String?,
        region: CodeSvnRegion?,
        userName: String?
    ): RevisionInfo {
        logger.info("[$projectName|$url|$type|$branchName|$userName] Start to get the latest oauth revision")
        val startEpoch = System.currentTimeMillis()
        try {
            return ScmOauthFactory.getScm(
                projectName,
                url,
                type,
                branchName,
                privateKey,
                passPhrase,
                token,
                region,
                userName,
                null
            ).getLatestRevision()
        } finally {
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to get the latest revision")
        }
    }

    fun listBranches(
        projectName: String,
        url: String,
        type: ScmType,
        privateKey: String?,
        passPhrase: String?,
        token: String?,
        region: CodeSvnRegion?,
        userName: String?
    ): List<String> {
        logger.info("[$projectName|$url|$type|$userName] Start to list the branches")
        val startEpoch = System.currentTimeMillis()
        try {
            return ScmOauthFactory.getScm(
                projectName,
                url,
                type,
                null,
                privateKey,
                passPhrase,
                token,
                region,
                userName,
                null
            ).getBranches()
        } finally {
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to list branches")
        }
    }

    fun listTags(
        projectName: String,
        url: String,
        type: ScmType,
        token: String,
        userName: String
    ): List<String> {
        logger.info("[$projectName|$url|$type|$token|$userName] Start to list tags")
        val startEpoch = System.currentTimeMillis()
        try {
            return ScmOauthFactory.getScm(projectName, url, type, null, null, null, token, null, userName, null).getTags()
        } finally {
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to list tags")
        }
    }

    fun checkPrivateKeyAndToken(
        projectName: String,
        url: String,
        type: ScmType,
        privateKey: String?,
        passPhrase: String?,
        token: String?,
        region: CodeSvnRegion?,
        userName: String
    ): TokenCheckResult {
        logger.info("[$projectName|$url|$type|$userName] Start to check private key and token")
        val startEpoch = System.currentTimeMillis()
        try {
            ScmOauthFactory.getScm(projectName, url, type, null, privateKey, passPhrase, token, region, userName, null).checkTokenAndPrivateKey()
        } catch (e: Throwable) {
            logger.warn("Fail to check the private key (projectName=$projectName, type=$type, privateKey=$privateKey, passPhrase=$passPhrase, token=$token, region=$region, username=$userName", e)
            return TokenCheckResult(false, e.message ?: "Fail to check the svn private key")
        } finally {
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to check private key and token")
        }
        return TokenCheckResult(true, "OK")
    }

    fun addWebHook(
        projectName: String,
        url: String,
        type: ScmType,
        privateKey: String?,
        passPhrase: String?,
        token: String?,
        region: CodeSvnRegion?,
        userName: String,
        event: String?
    ) {
        logger.info("[$projectName|$url|$type|$userName] Start to add web hook")
        val startEpoch = System.currentTimeMillis()
        try {
            val hookUrl = when (type) {
                ScmType.CODE_GIT -> {
                    if (gitHookUrl.isNullOrEmpty()) {
                        logger.warn("The git webhook url is not settle")
                        throw RuntimeException("The git hook url is not settle")
                    }
                    gitHookUrl!!
                }
                ScmType.CODE_GITLAB -> {
                    if (gitlabHookUrl.isNullOrEmpty()) {
                        logger.warn("The gitlab webhook url is not settle")
                        throw RuntimeException("The gitlab webhook url is not settle")
                    }
                    gitlabHookUrl!!
                }
                ScmType.CODE_SVN -> {
                    if (svnHookUrl.isNullOrEmpty()) {
                        logger.warn("The svn webhook url is not settle")
                        throw RuntimeException("The svn webhook url is not settle")
                    }
                    svnHookUrl!!
                }
                else -> {
                    logger.warn("Unknown repository type ($type) when add webhook")
                    throw RuntimeException("Unknown repository type ($type) when add webhook")
                }
            }
            ScmOauthFactory.getScm(projectName, url, type, null, privateKey, passPhrase, token, region, userName, event)
                .addWebHook(hookUrl)
        } finally {
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to add web hook")
        }
    }

    fun addCommitCheck(
        request: CommitCheckRequest
    ) {
        val startEpoch = System.currentTimeMillis()
        try {
            with(request) {
                val scm = ScmOauthFactory.getScm(projectName, url, type, null, privateKey, passPhrase, token, region, "", "")
                scm.addCommitCheck(commitId, state, targetUrl, context, description, block)
                if (mrRequestId != null) {
                    if (reportData.second.isEmpty()) return
                    val comment = QualityUtils.getQualityReport(reportData.first, reportData.second)
                    scm.addMRComment(mrRequestId!!, comment)
                }
            }
        } finally {
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to add commit check")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ScmOauthService::class.java)
    }
}