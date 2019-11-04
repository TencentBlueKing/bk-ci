/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.repository.service

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.exception.ClientException
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.DHUtil
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.model.repository.tables.records.TRepositoryRecord
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.repository.constant.RepositoryMessageCode
import com.tencent.devops.repository.constant.RepositoryMessageCode.GET_TICKET_FAIL
import com.tencent.devops.repository.constant.RepositoryMessageCode.GITHUB_INVALID
import com.tencent.devops.repository.constant.RepositoryMessageCode.GITLAB_INVALID
import com.tencent.devops.repository.constant.RepositoryMessageCode.GIT_INVALID
import com.tencent.devops.repository.constant.RepositoryMessageCode.PWD_EMPTY
import com.tencent.devops.repository.constant.RepositoryMessageCode.SVN_INVALID
import com.tencent.devops.repository.constant.RepositoryMessageCode.TGIT_INVALID
import com.tencent.devops.repository.constant.RepositoryMessageCode.USER_NAME_EMPTY
import com.tencent.devops.repository.constant.RepositoryMessageCode.USER_SECRET_EMPTY
import com.tencent.devops.repository.dao.CommitDao
import com.tencent.devops.repository.dao.RepositoryCodeGitDao
import com.tencent.devops.repository.dao.RepositoryCodeGitLabDao
import com.tencent.devops.repository.dao.RepositoryCodeSvnDao
import com.tencent.devops.repository.dao.RepositoryDao
import com.tencent.devops.repository.dao.RepositoryGithubDao
import com.tencent.devops.repository.pojo.CodeGitRepository
import com.tencent.devops.repository.pojo.CodeGitlabRepository
import com.tencent.devops.repository.pojo.CodeSvnRepository
import com.tencent.devops.repository.pojo.CodeTGitRepository
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.repository.pojo.RepositoryInfo
import com.tencent.devops.repository.pojo.RepositoryInfoWithPermission
import com.tencent.devops.repository.pojo.commit.CommitData
import com.tencent.devops.repository.pojo.commit.CommitResponse
import com.tencent.devops.repository.pojo.enums.CodeSvnRegion
import com.tencent.devops.repository.pojo.enums.GitAccessLevelEnum
import com.tencent.devops.repository.pojo.enums.RepoAuthType
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.repository.pojo.enums.VisibilityLevelEnum
import com.tencent.devops.repository.pojo.git.GitProjectInfo
import com.tencent.devops.repository.pojo.git.UpdateGitProjectInfo
import com.tencent.devops.repository.pojo.github.GithubRepository
import com.tencent.devops.repository.service.scm.GitOauthService
import com.tencent.devops.repository.utils.CredentialUtils
import com.tencent.devops.scm.api.ServiceGitResource
import com.tencent.devops.scm.api.ServiceScmResource
import com.tencent.devops.scm.pojo.GitRepositoryResp
import com.tencent.devops.ticket.api.ServiceCredentialResource
import org.bouncycastle.crypto.tls.ConnectionEnd.client
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.Base64
import javax.ws.rs.NotFoundException

@Service
class RepositoryService @Autowired constructor(
		private val repositoryDao: RepositoryDao,
		private val repositoryCodeSvnDao: RepositoryCodeSvnDao,
		private val repositoryCodeGitDao: RepositoryCodeGitDao,
		private val repositoryCodeGitLabDao: RepositoryCodeGitLabDao,
		private val repositoryGithubDao: RepositoryGithubDao,
		private val repostioryScmService: RepostioryScmService,
		private val commitDao: CommitDao,
		private val gitOauthService: GitOauthService,
		private val dslContext: DSLContext,
		private val client: Client,
		private val repositoryPermissionService: RepositoryPermissionService
//        private val authResourceApi: BkAuthResourceApi
) {

	@Value("\${git.devopsPrivateToken}")
	private lateinit var devopsPrivateToken: String

	@Value("\${git.devopsGroupName}")
	private lateinit var devopsGroupName: String

	fun hasCreatePermission(userId: String, projectId: String): Boolean {
		return validatePermission(userId, projectId, AuthPermission.CREATE)
	}

	fun hasAliasName(projectId: String, repositoryHashId: String?, aliasName: String): Boolean {
		val repositoryId = if (repositoryHashId != null) HashUtil.decodeOtherIdToLong(repositoryHashId) else 0L
		if (repositoryId != 0L) {
			val record = repositoryDao.get(dslContext, repositoryId, projectId)
			if (record.aliasName == aliasName) return false
		}
		return repositoryDao.countByProjectAndAliasName(dslContext, projectId, repositoryId, aliasName) != 0L
	}

	fun createGitCodeRepository(
			userId: String,
			projectCode: String?,
			repositoryName: String,
			sampleProjectPath: String?,
			namespaceId: Int?,
			visibilityLevel: VisibilityLevelEnum?,
			tokenType: TokenTypeEnum
	): Result<RepositoryInfo?> {
		logger.info("createGitRepository userId is:$userId,projectCode is:$projectCode, repositoryName is:$repositoryName, sampleProjectPath is:$sampleProjectPath")
		logger.info("createGitRepository  namespaceId is:$namespaceId, visibilityLevel is:$visibilityLevel, tokenType is:$tokenType")
		val getGitTokenResult = getGitToken(tokenType, userId)
		if (getGitTokenResult.isNotOk()) {
			return Result(status = getGitTokenResult.status, message = getGitTokenResult.message ?: "")
		}
		val token = getGitTokenResult.data!!
		val gitRepositoryRespResult: Result<GitRepositoryResp?>
		val gitRepositoryResp: GitRepositoryResp?
		try {
			gitRepositoryRespResult = repostioryScmService.createGitCodeRepository(userId,
					token,
					repositoryName,
					sampleProjectPath,
					namespaceId,
					visibilityLevel,
					tokenType
			)
			logger.info("createGitCodeRepository gitRepositoryRespResult is :$gitRepositoryRespResult")
			if (gitRepositoryRespResult.isOk()) {
				gitRepositoryResp = gitRepositoryRespResult.data
			} else {
				return Result(gitRepositoryRespResult.status, gitRepositoryRespResult.message ?: "")
			}
		} catch (e: Exception) {
			logger.error("createGitCodeRepository error is :$e", e)
			return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.SYSTEM_ERROR)
		}
		logger.info("gitRepositoryResp>> $gitRepositoryResp")
		return if (null != gitRepositoryResp) {
			val codeGitRepository = CodeGitRepository(
					gitRepositoryResp.name,
					gitRepositoryResp.repositoryUrl,
					"",
					gitRepositoryResp.name,
					userId,
					RepoAuthType.OAUTH,
					projectCode,
					null
			)

			// 关联代码库
			val repositoryHashId = if (null != projectCode) serviceCreate(userId, projectCode, codeGitRepository) else null
			logger.info("serviceCreate result>> $repositoryHashId")
			Result(
					RepositoryInfo(
							repositoryHashId,
							gitRepositoryResp.name,
							gitRepositoryResp.repositoryUrl,
							ScmType.CODE_GIT,
							LocalDateTime.now().timestampmilli()
					)
			)
		} else {
			MessageCodeUtil.generateResponseDataObject(CommonMessageCode.SYSTEM_ERROR)
		}
	}

	private fun getGitToken(tokenType: TokenTypeEnum, userId: String): Result<String?> {
		val token = if (TokenTypeEnum.OAUTH == tokenType) {
			val gitToken = gitOauthService.getAccessToken(userId)
			logger.info("gitToken>> $gitToken")
			if (null == gitToken) {
				// 抛出无效的token提示
				return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.OAUTH_TOKEN_IS_INVALID)
			}
			gitToken.accessToken
		} else {
			devopsPrivateToken
		}
		return Result(token)
	}

	fun updateGitCodeRepository(
			userId: String,
			repositoryConfig: RepositoryConfig,
			updateGitProjectInfo: UpdateGitProjectInfo,
			tokenType: TokenTypeEnum
	): Result<Boolean> {
		logger.info("updateGitCodeRepository repositoryConfig is:$repositoryConfig,updateGitProjectInfo is:$updateGitProjectInfo, tokenType is:$tokenType")
		val repo = serviceGet("", repositoryConfig)
		logger.info("the repo is:$repo")
		val projectName = repo.projectName
		val finalTokenType = generateFinalTokenType(tokenType, projectName)
		return updateGitRepositoryInfo(finalTokenType, projectName, userId, updateGitProjectInfo)
	}

	fun updateGitCodeRepository(
			userId: String,
			projectName: String,
			updateGitProjectInfo: UpdateGitProjectInfo,
			tokenType: TokenTypeEnum
	): Result<Boolean> {
		logger.info("updateGitCodeRepository userId is:$userId,projectName is:$projectName,updateGitProjectInfo is:$updateGitProjectInfo, tokenType is:$tokenType")
		return updateGitRepositoryInfo(tokenType, projectName, userId, updateGitProjectInfo)
	}

	private fun updateGitRepositoryInfo(
			tokenType: TokenTypeEnum,
			projectName: String,
			userId: String,
			updateGitProjectInfo: UpdateGitProjectInfo
	): Result<Boolean> {
		val getGitTokenResult = getGitToken(tokenType, userId)
		if (getGitTokenResult.isNotOk()) {
			return Result(status = getGitTokenResult.status, message = getGitTokenResult.message ?: "")
		}
		val token = getGitTokenResult.data!!
		val gitRepositoryRespResult: Result<Boolean>
		return try {
			gitRepositoryRespResult = client.getScm(ServiceGitResource::class)
					.updateGitCodeRepository(token, projectName, updateGitProjectInfo, tokenType)
			logger.info("updateGitCodeRepository gitRepositoryRespResult is :$gitRepositoryRespResult")
			if (gitRepositoryRespResult.isOk()) {
				Result(true)
			} else {
				Result(gitRepositoryRespResult.status, gitRepositoryRespResult.message ?: "", false)
			}
		} catch (e: Exception) {
			logger.error("updateGitCodeRepository error is :$e", e)
			MessageCodeUtil.generateResponseDataObject(CommonMessageCode.SYSTEM_ERROR)
		}
	}

	fun addGitProjectMember(
			userId: String,
			userIdList: List<String>,
			repositoryConfig: RepositoryConfig,
			gitAccessLevel: GitAccessLevelEnum,
			tokenType: TokenTypeEnum
	): Result<Boolean> {
		logger.info("addGitProjectMember userId is:$userId,userIdList is:$userIdList,repositoryConfig is:$repositoryConfig")
		logger.info("addGitProjectMember gitAccessLevel is:$gitAccessLevel,tokenType is:$tokenType")
		val repo: CodeGitRepository = serviceGet("", repositoryConfig) as CodeGitRepository
		logger.info("the repo is:$repo")
		val finalTokenType = generateFinalTokenType(tokenType, repo.projectName)
		val getGitTokenResult = getGitToken(finalTokenType, userId)
		if (getGitTokenResult.isNotOk()) {
			return Result(status = getGitTokenResult.status, message = getGitTokenResult.message, data = false)
		}
		val token = getGitTokenResult.data!!
		val addGitProjectMemberResult = repostioryScmService
				.addGitProjectMember(userIdList, repo.projectName, gitAccessLevel, token, finalTokenType)
		logger.info("addGitProjectMemberResult is :$addGitProjectMemberResult")
		if (addGitProjectMemberResult.isNotOk()) {
			return Result(status = addGitProjectMemberResult.status, message = addGitProjectMemberResult.message, data = false)
		}
		return Result(true)
	}

	fun deleteGitProjectMember(
			userId: String,
			userIdList: List<String>,
			repositoryConfig: RepositoryConfig,
			tokenType: TokenTypeEnum
	): Result<Boolean> {
		logger.info("deleteGitProjectMember userId is:$userId,userIdList is:$userIdList")
		logger.info("deleteGitProjectMember repositoryConfig is:$repositoryConfig,tokenType is:$tokenType")
		val repo: CodeGitRepository = serviceGet("", repositoryConfig) as CodeGitRepository
		logger.info("the repo is:$repo")
		val finalTokenType = generateFinalTokenType(tokenType, repo.projectName)
		val getGitTokenResult = getGitToken(finalTokenType, userId)
		if (getGitTokenResult.isNotOk()) {
			return Result(status = getGitTokenResult.status, message = getGitTokenResult.message, data = false)
		}
		val token = getGitTokenResult.data!!
		val deleteGitProjectMemberResult = repostioryScmService
				.deleteGitProjectMember(userIdList, repo.projectName, token, finalTokenType)
		logger.info("deleteGitProjectMemberResult is :$deleteGitProjectMemberResult")
		if (deleteGitProjectMemberResult.isNotOk()) {
			return Result(status = deleteGitProjectMemberResult.status, message = deleteGitProjectMemberResult.message, data = false)
		}
		return Result(true)
	}

	fun moveGitProjectToGroup(
			userId: String,
			groupCode: String?,
			repositoryConfig: RepositoryConfig,
			tokenType: TokenTypeEnum
	): Result<GitProjectInfo?> {
		logger.info("moveGitProjectToGroup userId is:$userId,groupCode is:$groupCode,repositoryConfig is:$repositoryConfig,tokenType is:$tokenType")
		val repo: CodeGitRepository = serviceGet("", repositoryConfig) as CodeGitRepository
		logger.info("the repo is:$repo")
		val finalTokenType = generateFinalTokenType(tokenType, repo.projectName)
		val getGitTokenResult = getGitToken(finalTokenType, userId)
		if (getGitTokenResult.isNotOk()) {
			return Result(status = getGitTokenResult.status, message = getGitTokenResult.message ?: "")
		}
		val token = getGitTokenResult.data!!
		val moveProjectToGroupResult: Result<GitProjectInfo?>
		return try {
			moveProjectToGroupResult = repostioryScmService.moveProjectToGroup(token, groupCode
					?: devopsGroupName, repo.projectName, finalTokenType)
			logger.info("moveProjectToGroupResult is :$moveProjectToGroupResult")
			if (moveProjectToGroupResult.isOk()) {
				val gitProjectInfo = moveProjectToGroupResult.data!!
				val repositoryId = HashUtil.decodeOtherIdToLong(repo.repoHashId!!)
				dslContext.transaction { t ->
					val context = DSL.using(t)
					repositoryDao.edit(
							context,
							repositoryId,
							gitProjectInfo.namespaceName,
							gitProjectInfo.repositoryUrl
					)
					repositoryCodeGitDao.edit(
							context,
							repositoryId,
							gitProjectInfo.namespaceName,
							repo.userName,
							repo.credentialId,
							repo.authType
					)
				}
				Result(gitProjectInfo)
			} else {
				Result(moveProjectToGroupResult.status, moveProjectToGroupResult.message ?: "")
			}
		} catch (e: Exception) {
			logger.error("moveProjectToGroupResult error is :$e", e)
			MessageCodeUtil.generateResponseDataObject(CommonMessageCode.SYSTEM_ERROR)
		}
	}

	private fun generateFinalTokenType(tokenType: TokenTypeEnum, repoProjectName: String): TokenTypeEnum {
		// 兼容历史插件的代码库不在公共group下的情况，历史插件的代码库信息更新要用用户的token更新
		var finalTokenType = tokenType
		if (!repoProjectName.startsWith(devopsGroupName)) {
			finalTokenType = TokenTypeEnum.OAUTH
		}
		return finalTokenType
	}

	fun userCreate(userId: String, projectId: String, repository: Repository): String {
		// 指定oauth的用户名字只能是登录用户。
		repository.userName = userId
		validatePermission(userId, projectId, AuthPermission.CREATE, "用户($userId)在工程($projectId)下没有代码库创建权限")
		val repositoryId = createRepository(repository, projectId, userId)
		return HashUtil.encodeOtherLongId(repositoryId)
	}

	fun serviceCreate(userId: String, projectId: String, repository: Repository): String {
		val repositoryId = createRepository(repository, projectId, userId)
		return HashUtil.encodeOtherLongId(repositoryId)
	}

	private fun createRepository(
			repository: Repository,
			projectId: String,
			userId: String
	): Long {
		if (!repository.isLegal()) {
			logger.warn("The repository($repository) is illegal")
			val validateResult: Result<String?> = MessageCodeUtil.generateResponseDataObject(RepositoryMessageCode.REPO_PATH_WRONG_PARM, arrayOf(repository.getStartPrefix()))
			throw OperationException(
					validateResult.message!!
			)
		}

		if (hasAliasName(projectId, null, repository.aliasName)) {
			throw OperationException(MessageCodeUtil.generateResponseDataObject<String?>(RepositoryMessageCode.REPO_NAME_EXIST, arrayOf(repository.aliasName)).message!!)
		}

		if (needToCheckToken(repository)) {
			/**
			 * tGit 类型，去除凭据验证
			 */
			if ((repository !is CodeTGitRepository) and (repository !is GithubRepository)) {
				checkRepositoryToken(projectId, repository)
			}
		}

		val repositoryId = dslContext.transactionResult { configuration ->
			val transactionContext = DSL.using(configuration)
			val repositoryId = when (repository) {
				is CodeSvnRepository -> {
					val repositoryId = repositoryDao.create(
							transactionContext,
							projectId,
							userId,
							repository.aliasName,
							repository.getFormatURL(),
							ScmType.CODE_SVN
					)
					repositoryCodeSvnDao.create(
							transactionContext,
							repositoryId,
							repository.region,
							repository.projectName,
							repository.userName,
							repository.credentialId,
							repository.svnType
					)
					repositoryId
				}
				is CodeGitRepository -> {
					val repositoryId = repositoryDao.create(
							transactionContext,
							projectId,
							userId,
							repository.aliasName,
							repository.getFormatURL(),
							ScmType.CODE_GIT
					)
					repositoryCodeGitDao.create(
							transactionContext,
							repositoryId,
							repository.projectName,
							repository.userName,
							repository.credentialId,
							repository.authType
					)
					repositoryId
				}
				is CodeTGitRepository -> {
					val repositoryId = repositoryDao.create(
							transactionContext,
							projectId,
							userId,
							repository.aliasName,
							repository.getFormatURL(),
							ScmType.CODE_TGIT
					)
					repositoryCodeGitDao.create(
							transactionContext,
							repositoryId,
							repository.projectName,
							repository.userName,
							repository.credentialId,
							repository.authType
					)
					repositoryId
				}
				is CodeGitlabRepository -> {
					val repositoryId = repositoryDao.create(
							transactionContext,
							projectId,
							userId,
							repository.aliasName,
							repository.getFormatURL(),
							ScmType.CODE_GITLAB
					)
					repositoryCodeGitLabDao.create(
							transactionContext,
							repositoryId,
							repository.projectName,
							repository.userName,
							repository.credentialId
					)
					repositoryId
				}
				is GithubRepository -> {
					val repositoryId = repositoryDao.create(
							transactionContext,
							projectId,
							userId,
							repository.aliasName,
							repository.getFormatURL(),
							ScmType.GITHUB
					)
					repositoryGithubDao.create(dslContext, repositoryId, repository.projectName, userId)
					repositoryId
				}
				else -> throw RuntimeException("Unknown repository type")
			}
			repositoryId
		}

		createResource(userId, projectId, repositoryId, repository.aliasName)
		return repositoryId
	}

	fun userGet(userId: String, projectId: String, repositoryConfig: RepositoryConfig): Repository {
		val repository = getRepository(projectId, repositoryConfig)

		val repositoryId = repository.repositoryId
		validatePermission(
				userId,
				projectId,
				repositoryId,
				AuthPermission.VIEW,
				MessageCodeUtil.generateResponseDataObject<String>(RepositoryMessageCode.USER_VIEW_PEM_ERROR, arrayOf(userId, projectId, repositoryConfig.getRepositoryId())).message!!
		)
		return compose(repository)
	}

	fun serviceGet(projectId: String, repositoryConfig: RepositoryConfig): Repository {
		return compose(getRepository(projectId, repositoryConfig))
	}

	private fun getRepository(projectId: String, repositoryConfig: RepositoryConfig): TRepositoryRecord {
		logger.info("[$projectId]Start to get the repository - ($repositoryConfig)")
		return when (repositoryConfig.repositoryType) {
			RepositoryType.ID -> {
				val repositoryId = HashUtil.decodeOtherIdToLong(repositoryConfig.getRepositoryId())
				repositoryDao.get(dslContext, repositoryId, projectId)
			}
			RepositoryType.NAME -> {
				repositoryDao.getByName(dslContext, projectId, repositoryConfig.getRepositoryId())
			}
		}
	}

	private fun compose(repository: TRepositoryRecord): Repository {
		val repositoryId = repository.repositoryId
		val hashId = HashUtil.encodeOtherLongId(repository.repositoryId)
		return when (repository.type) {
			ScmType.CODE_SVN.name -> {
				val record = repositoryCodeSvnDao.get(dslContext, repositoryId)
				CodeSvnRepository(
						repository.aliasName,
						repository.url,
						record.credentialId,
						CodeSvnRegion.valueOf(record.region),
						record.projectName,
						record.userName,
						repository.projectId,
						hashId,
						record.svnType
				)
			}
			ScmType.CODE_GIT.name -> {
				val record = repositoryCodeGitDao.get(dslContext, repositoryId)
				CodeGitRepository(
						repository.aliasName,
						repository.url,
						record.credentialId,
						record.projectName,
						record.userName,
						RepoAuthType.parse(record.authType),
						repository.projectId,
						HashUtil.encodeOtherLongId(repository.repositoryId)
				)
			}
			ScmType.CODE_TGIT.name -> {
				val record = repositoryCodeGitDao.get(dslContext, repositoryId)
				CodeTGitRepository(
						repository.aliasName,
						repository.url,
						record.credentialId,
						record.projectName,
						record.userName,
						RepoAuthType.parse(record.authType),
						repository.projectId,
						hashId

				)
			}
			ScmType.CODE_GITLAB.name -> {
				val record = repositoryCodeGitLabDao.get(dslContext, repositoryId)
				CodeGitlabRepository(
						repository.aliasName,
						repository.url,
						record.credentialId,
						record.projectName,
						record.userName,
						repository.projectId,
						hashId
				)
			}
			ScmType.GITHUB.name -> {
				val record = repositoryGithubDao.get(dslContext, repositoryId)
				GithubRepository(
						repository.aliasName,
						repository.url,
						repository.userId,
						record.projectName,
						repository.projectId,
						hashId
				)
			}
			else -> throw RuntimeException("Unknown repository type")
		}
	}

	fun buildGet(buildId: String, repositoryConfig: RepositoryConfig): Repository {
		val buildBasicInfoResult = client.get(ServiceBuildResource::class).serviceBasic(buildId)
		if (buildBasicInfoResult.isNotOk()) {
			throw RemoteServiceException("Failed to build the basic information based on the buildId")
		}
		val buildBasicInfo = buildBasicInfoResult.data
				?: throw RemoteServiceException("Failed to build the basic information based on the buildId")
		return serviceGet(buildBasicInfo.projectId, repositoryConfig)
	}

	fun userEdit(userId: String, projectId: String, repositoryHashId: String, repository: Repository) {
		val repositoryId = HashUtil.decodeOtherIdToLong(repositoryHashId)
		validatePermission(
				userId,
				projectId,
				repositoryId,
				AuthPermission.EDIT,
				MessageCodeUtil.generateResponseDataObject<String>(RepositoryMessageCode.USER_EDIT_PEM_ERROR, arrayOf(userId, projectId, repositoryHashId)).message!!
//            "用户($userId)在工程($projectId)下没有代码库($repositoryHashId)编辑权限"
		)
		val record = repositoryDao.get(dslContext, repositoryId, projectId)
		if (record.projectId != projectId) {
			throw NotFoundException("Repository is not part of the project")
		}

		if (!repository.isLegal()) {
			logger.warn("The repository($repository) is illegal")
			throw OperationException(
					MessageCodeUtil.generateResponseDataObject<String>(RepositoryMessageCode.REPO_PATH_WRONG_PARM, arrayOf(repository.getStartPrefix())).message!!
			)
		}

		if (hasAliasName(projectId, repositoryHashId, repository.aliasName)) {
			throw OperationException(
					MessageCodeUtil.generateResponseDataObject<String>(RepositoryMessageCode.REPO_NAME_EXIST, arrayOf(repository.aliasName)).message!!
			)
		}

		val isGitOauth = repository is CodeGitRepository && repository.authType == RepoAuthType.OAUTH
		if (!isGitOauth) {
			/**
			 * 类型为tGit,去掉凭据验证
			 */
			if ((repository !is CodeTGitRepository) and (repository !is GithubRepository)) {
				checkRepositoryToken(projectId, repository)
			}
		}
		// 判断仓库类型是否一致
		dslContext.transaction { configuration ->
			val transactionContext = DSL.using(configuration)
			when (record.type) {
				ScmType.CODE_GIT.name -> {
					if (repository !is CodeGitRepository) {
						throw OperationException(MessageCodeUtil.getCodeLanMessage(RepositoryMessageCode.GIT_INVALID))
					}
					repositoryDao.edit(
							transactionContext,
							repositoryId,
							repository.aliasName,
							repository.getFormatURL()
					)
					repositoryCodeGitDao.edit(
							transactionContext,
							repositoryId,
							repository.projectName,
							repository.userName,
							repository.credentialId,
							repository.authType
					)
				}
				ScmType.CODE_TGIT.name -> {
					if (repository !is CodeTGitRepository) {
						throw OperationException(MessageCodeUtil.getCodeLanMessage(RepositoryMessageCode.TGIT_INVALID))
					}
					repositoryDao.edit(
							transactionContext,
							repositoryId,
							repository.aliasName,
							repository.getFormatURL()
					)
					repositoryCodeGitDao.edit(
							transactionContext,
							repositoryId,
							repository.projectName,
							repository.userName,
							repository.credentialId,
							repository.authType
					)
				}
				ScmType.CODE_SVN.name -> {
					if (repository !is CodeSvnRepository) {
						throw OperationException(MessageCodeUtil.getCodeLanMessage(RepositoryMessageCode.SVN_INVALID))
					}
					repositoryDao.edit(
							transactionContext,
							repositoryId,
							repository.aliasName,
							repository.getFormatURL()
					)
					repositoryCodeSvnDao.edit(
							transactionContext,
							repositoryId,
							repository.region,
							repository.projectName,
							repository.userName,
							repository.credentialId,
							repository.svnType
					)
				}
				ScmType.CODE_GITLAB.name -> {
					if (repository !is CodeGitlabRepository) {
						throw OperationException(MessageCodeUtil.getCodeLanMessage(RepositoryMessageCode.GITLAB_INVALID))
					}
					repositoryDao.edit(
							transactionContext,
							repositoryId,
							repository.aliasName,
							repository.getFormatURL()
					)
					repositoryCodeGitLabDao.edit(
							transactionContext,
							repositoryId,
							repository.projectName,
							repository.userName,
							repository.credentialId
					)
				}
				ScmType.GITHUB.name -> {
					if (repository !is GithubRepository) {
						throw OperationException(MessageCodeUtil.getCodeLanMessage(RepositoryMessageCode.GITHUB_INVALID))
					}
					repositoryDao.edit(
							transactionContext,
							repositoryId,
							repository.aliasName,
							repository.getFormatURL()
					)
					repositoryGithubDao.edit(dslContext, repositoryId, repository.projectName, repository.userName)
				}
			}
		}
		editResource(projectId, repositoryId, repository.aliasName)
	}

	fun serviceList(
			projectId: String,
			scmType: ScmType?
	): List<RepositoryInfoWithPermission> {
		val dbRecords = repositoryDao.listByProject(dslContext, projectId, scmType)
		val gitRepoIds = dbRecords.filter { it.type == "CODE_GIT" }.map { it.repositoryId }.toSet()
		val gitAuthMap = repositoryCodeGitDao.list(dslContext, gitRepoIds)?.map { it.repositoryId to it }?.toMap()

		return dbRecords.map { repository ->
			val authType = gitAuthMap?.get(repository.repositoryId)?.authType ?: "SSH"
			RepositoryInfoWithPermission(
					HashUtil.encodeOtherLongId(repository.repositoryId),
					repository.aliasName,
					repository.url,
					ScmType.valueOf(repository.type),
					repository.updatedTime.timestamp(),
					true,
					true,
					authType
			)
		}.toList()
	}

	fun serviceCount(
			projectId: Set<String>,
			repositoryHashId: String,
			repositoryType: ScmType?,
			aliasName: String
	): Long {
		val repositoryId = HashUtil.decodeOtherIdToLong(repositoryHashId)
		val repoIds = if (repositoryId == 0L) setOf() else setOf(repositoryId)
		return repositoryDao.count(dslContext, projectId, repositoryType, repoIds, aliasName)
	}

	fun userList(
			userId: String,
			projectId: String,
			repositoryType: ScmType?,
			aliasName: String?,
			offset: Int,
			limit: Int
	): Pair<SQLPage<RepositoryInfoWithPermission>, Boolean> {
		val hasCreatePermission = validatePermission(userId, projectId, AuthPermission.CREATE)
		val permissionToListMap = filterRepositories(
				userId,
				projectId,
				setOf(AuthPermission.LIST, AuthPermission.EDIT, AuthPermission.DELETE)
		)
		val hasListPermissionRepoList = permissionToListMap[AuthPermission.LIST]!!
		val hasEditPermissionRepoList = permissionToListMap[AuthPermission.EDIT]!!
		val hasDeletePermissionRepoList = permissionToListMap[AuthPermission.DELETE]!!

		val count =
				repositoryDao.countByProject(
						dslContext,
						projectId,
						repositoryType,
						aliasName,
						hasListPermissionRepoList.toSet()
				)
		val repositoryRecordList = repositoryDao.listByProject(
				dslContext,
				projectId,
				repositoryType,
				aliasName,
				hasListPermissionRepoList.toSet(),
				offset,
				limit
		)
		val gitRepoIds =
				repositoryRecordList.filter { it.type == ScmType.CODE_GIT.name || it.type == ScmType.CODE_GITLAB.name || it.type == ScmType.CODE_TGIT.name || it.type == ScmType.GITHUB.name }
						.map { it.repositoryId }.toSet()
		val gitAuthMap = repositoryCodeGitDao.list(dslContext, gitRepoIds)?.map { it.repositoryId to it }?.toMap()

		val svnRepoIds =
				repositoryRecordList.filter { it.type == ScmType.CODE_SVN.name }.map { it.repositoryId }.toSet()
		val svnRepoRecords = repositoryCodeSvnDao.list(dslContext, svnRepoIds).map { it.repositoryId to it }.toMap()

		val repositoryList = repositoryRecordList.map {
			val hasEditPermission = hasEditPermissionRepoList.contains(it.repositoryId)
			val hasDeletePermission = hasDeletePermissionRepoList.contains(it.repositoryId)
			val authType = gitAuthMap?.get(it.repositoryId)?.authType ?: "SSH"
			val svnType = svnRepoRecords[it.repositoryId]?.svnType
			RepositoryInfoWithPermission(
					HashUtil.encodeOtherLongId(it.repositoryId),
					it.aliasName,
					it.url,
					ScmType.valueOf(it.type),
					it.updatedTime.timestamp(),
					hasEditPermission,
					hasDeletePermission,
					authType,
					svnType
			)
		}
		return Pair(SQLPage(count, repositoryList), hasCreatePermission)
	}

	fun hasPermissionList(
			userId: String,
			projectId: String,
			repositoryType: ScmType?,
			authPermission: AuthPermission,
			offset: Int,
			limit: Int
	): SQLPage<RepositoryInfo> {
		val hasPermissionList = filterRepository(userId, projectId, authPermission)

		val count = repositoryDao.countByProject(dslContext, projectId, repositoryType, null, hasPermissionList.toSet())
		val repositoryRecordList =
				repositoryDao.listByProject(
						dslContext,
						projectId,
						repositoryType,
						null,
						hasPermissionList.toSet(),
						offset,
						limit
				)
		val repositoryList = repositoryRecordList.map {
			RepositoryInfo(
					HashUtil.encodeOtherLongId(it.repositoryId),
					it.aliasName,
					it.url,
					ScmType.valueOf(it.type),
					it.updatedTime.timestamp()
			)
		}
		return SQLPage(count, repositoryList)
	}

	fun userDelete(userId: String, projectId: String, repositoryHashId: String) {
		val repositoryId = HashUtil.decodeOtherIdToLong(repositoryHashId)
		validatePermission(
				userId,
				projectId,
				repositoryId,
				AuthPermission.DELETE,
				MessageCodeUtil.generateResponseDataObject<String>(RepositoryMessageCode.USER_DELETE_PEM_ERROR, arrayOf(userId, projectId, repositoryHashId)).message!!
		)

		val record = repositoryDao.get(dslContext, repositoryId, projectId)
		if (record.projectId != projectId) {
			throw NotFoundException("Repository is not part of the project")
		}

		deleteResource(projectId, repositoryId)
		repositoryDao.delete(dslContext, repositoryId)
	}

	fun validatePermission(user: String, projectId: String, authPermission: AuthPermission, message: String) {
		if (!validatePermission(user, projectId, authPermission)) {
			throw PermissionForbiddenException(message)
		}
	}

	fun validatePermission(
			user: String,
			projectId: String,
			repositoryId: Long,
			authPermission: AuthPermission,
			message: String
	) {
		if (!validatePermission(user, projectId, repositoryId, authPermission)) {
			throw PermissionForbiddenException(message)
		}
	}

	fun userLock(userId: String, projectId: String, repositoryHashId: String) {
		val repositoryId = HashUtil.decodeOtherIdToLong(repositoryHashId)
		validatePermission(
				userId,
				projectId,
				repositoryId,
				AuthPermission.EDIT,
				MessageCodeUtil.generateResponseDataObject<String>(RepositoryMessageCode.USER_EDIT_PEM_ERROR, arrayOf(userId, projectId, repositoryHashId)).message!!
		)
		val record = repositoryDao.get(dslContext, repositoryId, projectId)
		if (record.projectId != projectId) {
			throw NotFoundException("Repository is not part of the project")
		}
		if (record.type != ScmType.CODE_SVN.name) {
			throw PermissionForbiddenException(
					MessageCodeUtil.generateResponseDataObject<String>(RepositoryMessageCode.REPO_LOCK_UN_SUPPORT, arrayOf(repositoryHashId)).message!!
			)
		}

		repostioryScmService.lock(record.projectId,
				record.url,
				ScmType.CODE_SVN,
				CodeSvnRegion.getRegion(record.url),
				record.userId)
	}

	fun userUnLock(userId: String, projectId: String, repositoryHashId: String) {
		val repositoryId = HashUtil.decodeOtherIdToLong(repositoryHashId)
		validatePermission(
				userId,
				projectId,
				repositoryId,
				AuthPermission.EDIT,
				MessageCodeUtil.generateResponseDataObject<String>(RepositoryMessageCode.USER_EDIT_PEM_ERROR, arrayOf(userId, projectId, repositoryHashId)).message!!
		)
		val record = repositoryDao.get(dslContext, repositoryId, projectId)
		if (record.projectId != projectId) {
			throw NotFoundException("Repository is not part of the project")
		}
		if (record.type != ScmType.CODE_SVN.name) {
			throw PermissionForbiddenException(
					MessageCodeUtil.generateResponseDataObject<String>(RepositoryMessageCode.REPO_LOCK_UN_SUPPORT, arrayOf(repositoryHashId)).message!!
			)
		}
		repostioryScmService.unlock(record.projectId, record.url, ScmType.CODE_SVN, CodeSvnRegion.getRegion(record.url), record.userId)
	}

	private fun filterRepository(user: String, projectId: String, authPermission: AuthPermission): List<Long> {
		val resourceCodeList = repositoryPermissionService.getUserResourceByPermission(
				user,
				projectId,
				authPermission
		)
		return resourceCodeList.map { it.toLong() }
	}

	private fun filterRepositories(
			user: String,
			projectId: String,
			authPermissions: Set<AuthPermission>
	): Map<AuthPermission, List<Long>> {
		val permissionResourcesMap = repositoryPermissionService.getUserResourcesByPermissions(
				user,
				projectId,
				authPermissions
		)
		return permissionResourcesMap.mapValues {
			it.value.map { it.toLong() }
		}
	}

	private fun validatePermission(user: String, projectId: String, authPermission: AuthPermission): Boolean {
		return repositoryPermissionService.validateUserResourcePermission(
				user,
				projectId,
				authPermission
		)
	}

	private fun validatePermission(
			user: String,
			projectId: String,
			repositoryId: Long,
			authPermission: AuthPermission
	): Boolean {
		return repositoryPermissionService.validateUserResourcePermission(
				user,
				projectId,
				repositoryId.toString(),
				authPermission
		)
	}

	private fun createResource(user: String, projectId: String, repositoryId: Long, repositoryName: String) {
		repositoryPermissionService.createResource(
				user,
				projectId,
				repositoryId,
				repositoryName
		)
	}

	private fun editResource(projectId: String, repositoryId: Long, repositoryName: String) {
		repositoryPermissionService.modifyResource(
				projectId,
				repositoryId.toString(),
				repositoryName
		)
	}

	private fun deleteResource(projectId: String, repositoryId: Long) {
		repositoryPermissionService.deleteResource(
				projectId,
				repositoryId
		)
	}

	private fun checkRepositoryToken(projectId: String, repo: Repository) {
		val pair = DHUtil.initKey()
		val encoder = Base64.getEncoder()
		val result = client.get(ServiceCredentialResource::class)
				.get(projectId, repo.credentialId, encoder.encodeToString(pair.publicKey))
		if (result.isNotOk() || result.data == null) {
			logger.warn("It fail to get the credential(${repo.credentialId}) of project($projectId) because of ${result.message}")
			throw ClientException(MessageCodeUtil.getCodeLanMessage(RepositoryMessageCode.GET_TICKET_FAIL))
		}

		val credential = result.data!!
		logger.info("Get the credential($credential)")
		val list = ArrayList<String>()

		list.add(decode(credential.v1, credential.publicKey, pair.privateKey))
		if (!credential.v2.isNullOrEmpty()) {
			list.add(decode(credential.v2!!, credential.publicKey, pair.privateKey))
			if (!credential.v3.isNullOrEmpty()) {
				list.add(decode(credential.v3!!, credential.publicKey, pair.privateKey))
				if (!credential.v4.isNullOrEmpty()) {
					list.add(decode(credential.v4!!, credential.publicKey, pair.privateKey))
				}
			}
		}

		val checkResult = when (repo) {
			is CodeSvnRepository -> {
				val svnCredential = CredentialUtils.getCredential(repo, list, result.data!!.credentialType)
				repostioryScmService.checkPrivateKeyAndToken(
						repo.projectName, repo.getFormatURL(), ScmType.CODE_SVN,
						svnCredential.privateKey, svnCredential.passPhrase, null, repo.region, svnCredential.username
				)
			}
			is CodeGitRepository -> {
				when (repo.authType) {
					RepoAuthType.SSH -> {
						val token = list[0]
						if (list.size < 2) {
							throw OperationException(MessageCodeUtil.getCodeLanMessage(RepositoryMessageCode.USER_SECRET_EMPTY))
						}
						val privateKey = list[1]
						if (privateKey.isEmpty()) {
							throw OperationException(MessageCodeUtil.getCodeLanMessage(RepositoryMessageCode.USER_SECRET_EMPTY))
						}
						val passPhrase = if (list.size > 2) {
							val p = list[2]
							if (p.isEmpty()) {
								null
							} else {
								p
							}
						} else {
							null
						}
						repostioryScmService.checkPrivateKeyAndToken(
								repo.projectName, repo.getFormatURL(), ScmType.CODE_GIT,
								privateKey, passPhrase, token, null, repo.userName
						)
					}
					RepoAuthType.HTTP -> {
						val token = list[0]
						if (list.size < 2) {
							throw OperationException(MessageCodeUtil.getCodeLanMessage(RepositoryMessageCode.USER_NAME_EMPTY))
						}
						val username = list[1]
						if (username.isEmpty()) {
							throw OperationException(MessageCodeUtil.getCodeLanMessage(RepositoryMessageCode.USER_NAME_EMPTY))
						}
						if (list.size < 3) {
							throw OperationException(MessageCodeUtil.getCodeLanMessage(RepositoryMessageCode.PWD_EMPTY))
						}
						val password = list[2]
						if (password.isEmpty()) {
							throw OperationException(MessageCodeUtil.getCodeLanMessage(RepositoryMessageCode.PWD_EMPTY))
						}
						repostioryScmService.checkUsernameAndPassword(
								repo.projectName, repo.getFormatURL(), ScmType.CODE_GIT,
								username, password, token, null, repo.userName
						)
					}
					else -> {
						throw RuntimeException(MessageCodeUtil.generateResponseDataObject<String>(
								RepositoryMessageCode.REPO_TYPE_NO_NEED_CERTIFICATION,
								arrayOf(repo.authType!!.name)
						).message
						)
					}
				}
			}
			is CodeTGitRepository -> {
				when (repo.authType) {
					RepoAuthType.SSH -> {
						val token = list[0]
						if (list.size < 2) {
							throw OperationException(MessageCodeUtil.getCodeLanMessage(RepositoryMessageCode.USER_SECRET_EMPTY))
						}
						val privateKey = list[1]
						if (privateKey.isEmpty()) {
							throw OperationException(MessageCodeUtil.getCodeLanMessage(RepositoryMessageCode.USER_SECRET_EMPTY))
						}
						val passPhrase = if (list.size > 2) {
							val p = list[2]
							if (p.isEmpty()) {
								null
							} else {
								p
							}
						} else {
							null
						}
						repostioryScmService.checkPrivateKeyAndToken(
								repo.projectName, repo.getFormatURL(), ScmType.CODE_GIT,
								privateKey, passPhrase, token, null, repo.userName
						)
					}
					RepoAuthType.HTTP -> {
						val token = list[0]
						if (list.size < 2) {
							throw OperationException(MessageCodeUtil.getCodeLanMessage(RepositoryMessageCode.USER_NAME_EMPTY))
						}
						val username = list[1]
						if (username.isEmpty()) {
							throw OperationException(MessageCodeUtil.getCodeLanMessage(RepositoryMessageCode.USER_NAME_EMPTY))
						}
						if (list.size < 3) {
							throw OperationException(MessageCodeUtil.getCodeLanMessage(RepositoryMessageCode.PWD_EMPTY))
						}
						val password = list[2]
						if (password.isEmpty()) {
							throw OperationException(MessageCodeUtil.getCodeLanMessage(RepositoryMessageCode.PWD_EMPTY))
						}
						repostioryScmService.checkUsernameAndPassword(
								repo.projectName, repo.getFormatURL(), ScmType.CODE_GIT,
								username, password, token, null, repo.userName
						)
					}
					RepoAuthType.HTTPS -> {
						val token = list[0]
						if (list.size < 2) {
							throw OperationException(MessageCodeUtil.getCodeLanMessage(RepositoryMessageCode.USER_NAME_EMPTY))
						}
						val username = list[1]
						if (username.isEmpty()) {
							throw OperationException(MessageCodeUtil.getCodeLanMessage(RepositoryMessageCode.USER_NAME_EMPTY))
						}
						if (list.size < 3) {
							throw OperationException(MessageCodeUtil.getCodeLanMessage(RepositoryMessageCode.PWD_EMPTY))
						}
						val password = list[2]
						if (password.isEmpty()) {
							throw OperationException(MessageCodeUtil.getCodeLanMessage(RepositoryMessageCode.PWD_EMPTY))
						}
						repostioryScmService.checkUsernameAndPassword(
								repo.projectName, repo.getFormatURL(), ScmType.CODE_TGIT,
								username, password, token, null, repo.userName
						)
					}
					else -> {
						throw RuntimeException(MessageCodeUtil.generateResponseDataObject<String>(
								RepositoryMessageCode.REPO_TYPE_NO_NEED_CERTIFICATION,
								arrayOf(repo.authType!!.name)
						).message)
					}
				}
			}
			is CodeGitlabRepository -> {
				repostioryScmService.checkPrivateKeyAndToken(
						repo.projectName, repo.getFormatURL(), ScmType.CODE_GITLAB,
						null, null, list[0], null, repo.userName
				)
			}
			else -> {
				throw RuntimeException("Unknown repo($repo)")
			}
		}

		if (checkResult.isNotOk() || checkResult.data == null) {
			logger.warn("Fail to check the repo because of ${checkResult.message}")
			throw RuntimeException("Fail to check the repo")
		}
		if (!checkResult.data!!.result) {
			logger.warn("Fail to check the repo token & private key because of ${checkResult.data!!.message}")
			throw OperationException(checkResult.data!!.message)
		}
	}

	private fun decode(encode: String, publicKey: String, privateKey: ByteArray): String {
		val decoder = Base64.getDecoder()
		return String(DHUtil.decrypt(decoder.decode(encode), decoder.decode(publicKey), privateKey))
	}

	private fun needToCheckToken(repository: Repository): Boolean {
		if (repository is GithubRepository) {
			return false
		}
		val isGitOauth = repository is CodeGitRepository && repository.authType == RepoAuthType.OAUTH
		if (isGitOauth) {
			return false
		}
		return true
	}

	fun getCommit(buildId: String): List<CommitResponse> {
		val commits = commitDao.getBuildCommit(dslContext, buildId)

		val repos = repositoryDao.getRepoByIds(dslContext, commits?.map { it.repoId } ?: listOf())
		val repoMap = repos?.map { it.repositoryId.toString() to it }?.toMap() ?: mapOf()

		return commits?.map {
			CommitData(
					it.type,
					it.pipelineId,
					it.buildId,
					it.commit,
					it.committer,
					it.commitTime.timestampmilli(),
					it.comment,
					it.repoId?.toString(),
					it.repoName,
					it.elementId
			)
		}?.groupBy { it.elementId }?.map {
			val elementId = it.value[0].elementId
			val repoId = it.value[0].repoId
			CommitResponse(
					(repoMap[repoId]?.aliasName ?: "unknown repo"),
					elementId,
					it.value.filter { it.commit.isNotBlank() })
		} ?: listOf()
	}

	fun addCommit(commits: List<CommitData>): Int {
		return commitDao.addCommit(dslContext, commits).size
	}

	companion object {
		private val logger = LoggerFactory.getLogger(RepositoryService::class.java)
	}

	fun getLatestCommit(
			pipelineId: String,
			elementId: String,
			repositoryId: String,
			repositoryType: RepositoryType?,
			page: Int?,
			pageSize: Int?
	): List<CommitData> {
		val commitList = if (repositoryType == null || repositoryType == RepositoryType.ID) {
			val repoId = HashUtil.decodeOtherIdToLong(repositoryId)
			commitDao.getLatestCommitById(dslContext, pipelineId, elementId, repoId, page, pageSize) ?: return listOf()
		} else {
			commitDao.getLatestCommitByName(dslContext, pipelineId, elementId, repositoryId, page, pageSize)
					?: return listOf()
		}
		return commitList.map { data ->
			CommitData(
					data.type,
					pipelineId,
					data.buildId,
					data.commit,
					data.committer,
					data.commitTime.timestampmilli(),
					data.comment,
					data.repoId.toString(),
					null,
					data.elementId
			)
		}
	}
}