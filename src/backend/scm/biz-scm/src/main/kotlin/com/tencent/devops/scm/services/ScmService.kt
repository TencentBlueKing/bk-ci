package com.tencent.devops.scm.services

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.repository.pojo.enums.CodeSvnRegion
import com.tencent.devops.scm.code.git.CodeGitWebhookEvent
import com.tencent.devops.scm.pojo.RevisionInfo
import com.tencent.devops.scm.pojo.TokenCheckResult
import com.tencent.devops.scm.pojo.request.CommitCheckRequest
import com.tencent.devops.scm.utils.QualityUtils
import com.tencent.devops.scm.utils.ScmFactory
import com.tencent.devops.scm.utils.code.svn.SvnUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class ScmService {

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
        logger.info("[$projectName|$url|$type|$userName] Start to get latest revision")
        val startEpoch = System.currentTimeMillis()
        try {
            return ScmFactory.getScm(projectName, url, type, branchName, privateKey, passPhrase, token, region, userName).getLatestRevision()
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
        logger.info("[$projectName|$url|$type|$userName] Start to list branches")
        val startEpoch = System.currentTimeMillis()
        try {
            return ScmFactory.getScm(projectName, url, type, null, privateKey, passPhrase, token, region, userName)
                .getBranches()
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
            return ScmFactory.getScm(projectName, url, type, null, null, null, token, null, userName).getTags()
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
        logger.info("[$projectName|$url|$type|$token|$userName] Start to check the private key and token")
        val startEpoch = System.currentTimeMillis()
        try {
            ScmFactory.getScm(projectName, url, type, null, privateKey, passPhrase, token, region, userName).checkTokenAndPrivateKey()
        } catch (e: Throwable) {
            logger.warn("Fail to check the private key (projectName=$projectName, type=$type, privateKey=$privateKey, passPhrase=$passPhrase, token=$token, region=$region, username=$userName", e)
            return TokenCheckResult(false, e.message ?: "Fail to check the svn private key")
        } finally {
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to check the private key and token")
        }
        return TokenCheckResult(true, "OK")
    }

    fun checkUsernameAndPassword(
        projectName: String,
        url: String,
        type: ScmType,
        username: String,
        password: String,
        token: String,
        region: CodeSvnRegion?,
        repoUsername: String
    ): TokenCheckResult {
        logger.info("[$projectName|$url|$type|$username|$password|$token|$region|$repoUsername] Start to check the username and password")
        val startEpoch = System.currentTimeMillis()
        try {
            ScmFactory.getScm(projectName, url, type, null, username, password, token, region, repoUsername).checkTokenAndUsername()
        } catch (e: Throwable) {
            logger.warn("Fail to check the private key (projectName=$projectName, type=$type, username=$username, token=$token, region=$region, repoUsername=$repoUsername", e)
            return TokenCheckResult(false, e.message ?: "Fail to check the svn private key")
        } finally {
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to check username and password")
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
        event: String? = null
    ) {
        logger.info("[$projectName|$url|$type|$token|$region|$userName|$event] Start to add web hook")
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
            ScmFactory.getScm(projectName, url, type, null, privateKey, passPhrase, token, region, userName, event)
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
                val scm = ScmFactory.getScm(
                        projectName,
                        url,
                        type,
                        null,
                        privateKey,
                        passPhrase,
                        token,
                        region,
                        "",
                        CodeGitWebhookEvent.MERGE_REQUESTS_EVENTS.value
                )
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

    fun lock(
        projectName: String,
        url: String,
        type: ScmType,
        region: CodeSvnRegion?,
        userName: String
    ) {
        if (type != ScmType.CODE_SVN) {
            logger.warn("repository type ($type) can not lock")
            throw RuntimeException("repository type ($type) can not lock")
        }
        val repName = SvnUtils.getSvnRepName(url)
        val subPath = SvnUtils.getSvnSubPath(url)
        val svnRegion = region ?: CodeSvnRegion.getRegion(url)

        ScmFactory.getScm(projectName, url, type, null, "", "", "", svnRegion, userName).lock(repName, userName, subPath)
    }

    fun unlock(
        projectName: String,
        url: String,
        type: ScmType,
        region: CodeSvnRegion?,
        userName: String
    ) {
        if (type != ScmType.CODE_SVN) {
            logger.warn("repository type ($type) can not unlock")
            throw RuntimeException("repository type ($type) can not unlock")
        }
        val repName = SvnUtils.getSvnRepName(url)
        val subPath = SvnUtils.getSvnSubPath(url)
        val svnRegion = region ?: CodeSvnRegion.getRegion(url)

        ScmFactory.getScm(projectName, url, type, null, "", "", "", svnRegion, userName).unlock(repName, userName, subPath)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ScmService::class.java)
    }
}