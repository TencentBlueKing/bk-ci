package com.tencent.devops.repository.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.repository.pojo.RepositoryInfo
import com.tencent.devops.repository.pojo.enums.GitAccessLevelEnum
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.repository.pojo.enums.VisibilityLevelEnum
import com.tencent.devops.repository.pojo.git.GitProjectInfo
import com.tencent.devops.repository.pojo.git.UpdateGitProjectInfo
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_REPOSITORY"], description = "服务-git代码库资源")
@Path("/service/repositories")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServcieGitRepositoryResource {

	@ApiOperation("创建git代码库")
	@POST
	@Path("/git/create/repository")
	fun createGitCodeRepository(
			@ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
			@HeaderParam(AUTH_HEADER_USER_ID)
			userId: String,
			@ApiParam("项目编码", required = true)
			@QueryParam("projectCode")
			projectCode: String,
			@ApiParam("代码库名称", required = true)
			@QueryParam("repositoryName")
			repositoryName: String,
			@ApiParam("样例工程路径", required = true)
			@QueryParam("sampleProjectPath")
			sampleProjectPath: String,
			@ApiParam(value = "命名空间ID", required = false)
			@QueryParam("namespaceId")
			namespaceId: Int?,
			@ApiParam(value = "项目可视范围", required = false)
			@QueryParam("visibilityLevel")
			visibilityLevel: VisibilityLevelEnum?,
			@ApiParam(value = "token类型 1：oauth 2:privateKey", required = true)
			@QueryParam("tokenType")
			tokenType: TokenTypeEnum
	): Result<RepositoryInfo?>

	@ApiOperation("更新git代码库信息")
	@PUT
	@Path("/git/update/repository")
	fun updateGitCodeRepository(
			@ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
			@HeaderParam(AUTH_HEADER_USER_ID)
			userId: String,
			@ApiParam(value = "仓库id", required = true)
			@QueryParam("repoId")
			repoId: String,
			@ApiParam("代码库更新信息", required = true)
			updateGitProjectInfo: UpdateGitProjectInfo,
			@ApiParam(value = "token类型 1：oauth 2:privateKey", required = true)
			@QueryParam("tokenType")
			tokenType: TokenTypeEnum
	): Result<Boolean>

	@ApiOperation("为项目成员赋予代码库权限")
	@POST
	@Path("/git/repository/members/add")
	fun addGitProjectMember(
			@ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
			@HeaderParam(AUTH_HEADER_USER_ID)
			userId: String,
			@ApiParam("增加的用户列表", required = true)
			@QueryParam("userIdList")
			userIdList: List<String>,
			@ApiParam(value = "仓库id", required = true)
			@QueryParam("repoId")
			repoId: String,
			@ApiParam(value = "git访问权限", required = true)
			@QueryParam("gitAccessLevel")
			gitAccessLevel: GitAccessLevelEnum,
			@ApiParam(value = "token类型 1：oauth 2:privateKey", required = true)
			@QueryParam("tokenType")
			tokenType: TokenTypeEnum
	): Result<Boolean>

	@ApiOperation("删除项目成员的代码库权限")
	@DELETE
	@Path("/git/repository/members/delete")
	fun deleteGitProjectMember(
			@ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
			@HeaderParam(AUTH_HEADER_USER_ID)
			userId: String,
			@ApiParam("删除的用户列表", required = true)
			@QueryParam("userIdList")
			userIdList: List<String>,
			@ApiParam(value = "仓库id", required = true)
			@QueryParam("repoId")
			repoId: String,
			@ApiParam(value = "token类型 1：oauth 2:privateKey", required = true)
			@QueryParam("tokenType")
			tokenType: TokenTypeEnum
	): Result<Boolean>

	@ApiOperation("更新代码库用户信息")
	@PUT
	@Path("/git/repository/user/info/update")
	fun updateRepositoryUserInfo(
			@ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
			@HeaderParam(AUTH_HEADER_USER_ID)
			userId: String,
			@ApiParam("项目编码", required = true)
			@QueryParam("projectCode")
			projectCode: String,
			@ApiParam("代码库HashId", required = true)
			@QueryParam("repositoryHashId")
			repositoryHashId: String
	): Result<Boolean>

	@ApiOperation("把项目迁移到指定项目组下")
	@GET
	@Path("/git/move/repository/group")
	fun moveGitProjectToGroup(
			@ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
			@HeaderParam(AUTH_HEADER_USER_ID)
			userId: String,
			@ApiParam(value = "项目组代码", required = false)
			@QueryParam("groupCode")
			groupCode: String?,
			@ApiParam(value = "仓库id", required = true)
			@QueryParam("repoId")
			repoId: String,
			@ApiParam(value = "token类型 1：oauth 2:privateKey", required = true)
			@QueryParam("tokenType")
			tokenType: TokenTypeEnum
	): Result<GitProjectInfo?>

	@ApiOperation("获取代码仓库单个文件内容")
	@GET
	@Path("/{repoId}/getFileContent")
	fun getFileContent(
			@ApiParam(value = "仓库id")
			@PathParam("repoId")
			repoId: String,
			@ApiParam(value = "文件路径")
			@QueryParam("filePath")
			filePath: String,
			@ApiParam(value = "版本号（svn）")
			@QueryParam("reversion")
			reversion: String?,
			@ApiParam(value = "分支（git）")
			@QueryParam("branch")
			branch: String?,
			@ApiParam("代码库请求类型", required = true)
			@QueryParam("repositoryType")
			repositoryType: RepositoryType?
	): Result<String>

	@ApiOperation("删除代码库")
	@DELETE
	@Path("/{projectId}/{repositoryHashId}")
	fun delete(
			@ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
			@HeaderParam(AUTH_HEADER_USER_ID)
			userId: String,
			@ApiParam("项目ID", required = true)
			@PathParam("projectId")
			projectId: String,
			@ApiParam("代码库哈希ID", required = true)
			@PathParam("repositoryHashId")
			repositoryHashId: String
	): Result<Boolean>
}