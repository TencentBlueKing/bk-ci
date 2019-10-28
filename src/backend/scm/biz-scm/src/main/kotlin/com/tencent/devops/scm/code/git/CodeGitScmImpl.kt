package com.tencent.devops.scm.code.git

import com.tencent.devops.scm.code.git.api.CODE_GIT_URL
import com.tencent.devops.scm.code.git.api.GitApi
import com.tencent.devops.scm.IScm
import com.tencent.devops.scm.exception.ScmException
import com.tencent.devops.scm.pojo.RevisionInfo
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.slf4j.LoggerFactory

class CodeGitScmImpl constructor(
    override val projectName: String,
    override val branchName: String?,
    override val url: String,
    private val privateKey: String?,
    private val passPhrase: String?,
    private val token: String,
    private val event: String? = null
) : IScm {

    override fun getLatestRevision(): RevisionInfo {
        val branch = branchName ?: "master"
        val gitBranch = gitApi.getBranch(CODE_GIT_URL, token, projectName, branch)
        return RevisionInfo(
                gitBranch.commit.id,
                gitBranch.commit.message,
                branch)
    }

    override fun getBranches() =
            gitApi.listBranches(CODE_GIT_URL, token, projectName)

    override fun getTags() =
            gitApi.listTags(CODE_GIT_URL, token, projectName)

    override fun checkTokenAndPrivateKey() {
        if (privateKey == null) {
            throw RuntimeException("私钥为空")
        }
        // Check if token legal
        try {
            getBranches()
        } catch (t: Throwable) {
            logger.warn("Fail to list all branches", t)
            throw RuntimeException("Git Token 不正确")
        }

        try {
            // Check the private key
            val command = Git.lsRemoteRepository()
            val credentialSetter = CodeGitCredentialSetter(privateKey, passPhrase)
            credentialSetter.setGitCredential(command)
            command.setRemote(url).call()
        } catch (e: Throwable) {
            logger.warn("Fail to check the private key of git", e)
            throw RuntimeException("Git 私钥不对")
        }
    }

    override fun checkTokenAndUsername() {
        if (privateKey == null) {
            throw RuntimeException("用户密码为空")
        }

        // Check if token legal
        try {
            getBranches()
        } catch (t: Throwable) {
            logger.warn("Fail to list all branches", t)
            throw RuntimeException("Git Token 不正确")
        }

        try {
            val command = Git.lsRemoteRepository()
            command.setRemote(url)
            command.setCredentialsProvider(UsernamePasswordCredentialsProvider(privateKey, passPhrase))
            command.call()
        } catch (t: Throwable) {
            logger.warn("Fail to check the username and password of git", t)
            throw RuntimeException("Git 用户名或者密码不对")
        }
    }

    override fun addWebHook(hookUrl: String) {
        if (token.isEmpty()) {
            throw RuntimeException("Git Token为空")
        }
        if (hookUrl.isEmpty()) {
            throw RuntimeException("Git hook url为空")
        }
        try {
            gitApi.addWebhook(CODE_GIT_URL, token, projectName, hookUrl, event)
        } catch (e: ScmException) {
            throw RuntimeException("Git Token不正确")
        }
    }

    override fun addCommitCheck(commitId: String, state: String, targetUrl: String, context: String, description: String, block: Boolean) {
        if (token.isEmpty()) {
            throw RuntimeException("Git Token为空")
        }
        try {
            gitApi.addCommitCheck(CODE_GIT_URL, token, projectName, commitId, state, targetUrl, context, description, block)
        } catch (e: ScmException) {
            throw RuntimeException("Git Token不正确")
        }
    }

    override fun addMRComment(mrId: Long, comment: String) {
        gitApi.addMRComment(CODE_GIT_URL, token, projectName, mrId, comment)
    }

    override fun lock(repname: String, applicant: String, subpath: String) {
        logger.info("Git can not lock")
    }

    override fun unlock(repname: String, applicant: String, subpath: String) {
        logger.info("Git can not unlock")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CodeGitScmImpl::class.java)
        private val gitApi = GitApi()
    }
}