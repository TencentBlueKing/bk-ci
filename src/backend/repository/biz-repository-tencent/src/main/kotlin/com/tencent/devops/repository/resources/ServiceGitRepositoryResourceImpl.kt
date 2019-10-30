package com.tencent.devops.repository.resources

import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.pipeline.utils.RepositoryConfigUtils
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.repository.api.ServiceGitRepositoryResource
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.repository.pojo.RepositoryId
import com.tencent.devops.repository.pojo.RepositoryInfo
import com.tencent.devops.repository.pojo.RepositoryInfoWithPermission
import com.tencent.devops.repository.pojo.enums.GitAccessLevelEnum
import com.tencent.devops.repository.pojo.enums.Permission
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.repository.pojo.enums.VisibilityLevelEnum
import com.tencent.devops.repository.pojo.git.GitProjectInfo
import com.tencent.devops.repository.pojo.git.UpdateGitProjectInfo
import com.tencent.devops.repository.service.RepoFileService
import com.tencent.devops.repository.service.RepositoryService
import com.tencent.devops.repository.service.RepositoryUserService
import org.springframework.beans.factory.annotation.Autowired
import java.net.URLDecoder

@RestResource
class ServiceGitRepositoryResourceImpl @Autowired constructor(
		private val repoFileService: RepoFileService,
		private val repositoryService: RepositoryService,
		private val repositoryUserService: RepositoryUserService
) : ServiceGitRepositoryResource {

	override fun createGitCodeRepository(userId: String, projectCode: String, repositoryName: String, sampleProjectPath: String, namespaceId: Int?, visibilityLevel: VisibilityLevelEnum?, tokenType: TokenTypeEnum): Result<RepositoryInfo?> {
		return repositoryService.createGitCodeRepository(userId, projectCode, repositoryName, sampleProjectPath, namespaceId, visibilityLevel, tokenType)
	}

	override fun updateGitCodeRepository(userId: String, repoId: String, updateGitProjectInfo: UpdateGitProjectInfo, tokenType: TokenTypeEnum): Result<Boolean> {
		return repositoryService.updateGitCodeRepository(userId, RepositoryConfigUtils.buildConfig(repoId, null), updateGitProjectInfo, tokenType)
	}

	override fun addGitProjectMember(userId: String, userIdList: List<String>, repoId: String, gitAccessLevel: GitAccessLevelEnum, tokenType: TokenTypeEnum): Result<Boolean> {
		return repositoryService.addGitProjectMember(userId, userIdList, RepositoryConfigUtils.buildConfig(repoId, null), gitAccessLevel, tokenType)
	}

	override fun deleteGitProjectMember(userId: String, userIdList: List<String>, repoId: String, tokenType: TokenTypeEnum): Result<Boolean> {
		return repositoryService.deleteGitProjectMember(userId, userIdList, RepositoryConfigUtils.buildConfig(repoId, null), tokenType)
	}

	override fun updateRepositoryUserInfo(userId: String, projectCode: String, repositoryHashId: String): Result<Boolean> {
		return repositoryUserService.updateRepositoryUserInfo(userId, projectCode, repositoryHashId)
	}

	override fun moveGitProjectToGroup(userId: String, groupCode: String?, repoId: String, tokenType: TokenTypeEnum): Result<GitProjectInfo?> {
		return repositoryService.moveGitProjectToGroup(userId, groupCode, RepositoryConfigUtils.buildConfig(repoId, null), tokenType)
	}

	override fun getFileContent(repoId: String, filePath: String, reversion: String?, branch: String?, repositoryType: RepositoryType?): Result<String> {
		return Result(repoFileService.getFileContent(RepositoryConfigUtils.buildConfig(repoId, repositoryType), filePath, reversion, branch))
	}

	override fun delete(userId: String, projectId: String, repositoryHashId: String): Result<Boolean> {
		repositoryService.userDelete(userId, projectId, repositoryHashId)
		return Result(true)
	}
}