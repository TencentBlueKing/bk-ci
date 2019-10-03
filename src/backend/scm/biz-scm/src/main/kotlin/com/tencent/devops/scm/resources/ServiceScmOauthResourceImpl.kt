package com.tencent.devops.scm.resources

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.repository.pojo.enums.CodeSvnRegion
import com.tencent.devops.scm.api.ServiceScmOauthResource
import com.tencent.devops.scm.pojo.RevisionInfo
import com.tencent.devops.scm.pojo.TokenCheckResult
import com.tencent.devops.scm.pojo.request.CommitCheckRequest
import com.tencent.devops.scm.services.ScmOauthService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceScmOauthResourceImpl @Autowired constructor(private val scmOauthService: ScmOauthService) : ServiceScmOauthResource {

    override fun getLatestRevision(
        projectName: String,
        url: String,
        type: ScmType,
        branchName: String?,
        additionalPath: String?,
        privateKey: String?,
        passPhrase: String?,
        token: String?,
        region: CodeSvnRegion?,
        userName: String?
    ): Result<RevisionInfo> {
        logger.info("Start to get the code latest version of (projectName=$projectName, url=$url, type=$type, branch=$branchName, additionalPath=$additionalPath, privateKey=$privateKey, passPhrase=$passPhrase, token=$token, region=$region, username=$userName)")
        return Result(scmOauthService.getLatestRevision(projectName, url, type, branchName, privateKey, passPhrase, token, region, userName))
    }

    override fun listBranches(
        projectName: String,
        url: String,
        type: ScmType,
        privateKey: String?,
        passPhrase: String?,
        token: String?,
        region: CodeSvnRegion?,
        userName: String?
    ): Result<List<String>> {
        logger.info("Start to list the branches of (projectName=$projectName, url=$url, type=$type, privateKey=$privateKey, passPhrase=$passPhrase, token=$token, region=$region, username=$userName)")
        return Result(scmOauthService.listBranches(projectName, url, type, privateKey, passPhrase, token, region, userName))
    }

    override fun listTags(projectName: String, url: String, type: ScmType, token: String, userName: String): Result<List<String>> {
        logger.info("Start to list the branches of (projectName=$projectName, url=$url, type=$type, token=$token, username=$userName)")
        return Result(scmOauthService.listTags(projectName, url, type, token, userName))
    }

    override fun checkPrivateKeyAndToken(
        projectName: String,
        url: String,
        type: ScmType,
        privateKey: String?,
        passPhrase: String?,
        token: String?,
        region: CodeSvnRegion?,
        userName: String
    ): Result<TokenCheckResult> {
        logger.info("Start to check the private key and token of (projectName=$projectName, url=$url, type=$type, privateKey=$privateKey, passPhrase=$passPhrase, token=$token, region=$region, username=$userName)")
        return Result(scmOauthService.checkPrivateKeyAndToken(projectName, url, type, privateKey, passPhrase, token, region, userName))
    }

    override fun addWebHook(
        projectName: String,
        url: String,
        type: ScmType,
        privateKey: String?,
        passPhrase: String?,
        token: String?,
        region: CodeSvnRegion?,
        userName: String,
        event: String?
    ): Result<Boolean> {
        logger.info("Start to add the web hook of (projectName=$projectName, url=$url, type=$type, token=$token, username=$userName, event=$event)")
        scmOauthService.addWebHook(projectName, url, type, privateKey, passPhrase, token, region, userName, event)
        return Result(true)
    }

    override fun addCommitCheck(
        request: CommitCheckRequest
    ): Result<Boolean> {
        logger.info("Start to add the commit check of request($request)")
        scmOauthService.addCommitCheck(request)
        return Result(true)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ServiceScmOauthResourceImpl::class.java)
    }
}