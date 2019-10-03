package com.tencent.devops.repository.api

import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["EXTERNAL_REPO"], description = "外部-codecc-仓库资源")
@Path("/external/codecc/repo/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ExternalCodeccRepoResource {

    @ApiOperation("获取仓库单个文件内容")
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
        @ApiParam(value = "子模块项目名称")
        @QueryParam("subModule")
        subModule: String? = null,
        @ApiParam("代码库请求类型", required = true)
        @QueryParam("repositoryType")
        repositoryType: RepositoryType?
    ): Result<String>
}