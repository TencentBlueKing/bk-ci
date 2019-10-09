package com.tencent.devops.repository.api.scm

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.repository.pojo.scm.SvnFileInfo
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_SCM_SVN"], description = "Service Code SVN resource")
@Path("/service/scm/svn/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceSvnResource {

    @ApiOperation("获取文件内容")
    @GET
    @Path("/getFileContent")
    fun getFileContent(
        @ApiParam(value = "仓库url")
        @QueryParam("url")
        url: String,
        @ApiParam(value = "用户id")
        @QueryParam("userId")
        userId: String,
        @ApiParam(value = "仓库类型")
        @QueryParam("type")
        svnType: String,
        @ApiParam(value = "文件路径")
        @QueryParam("filePath")
        filePath: String,
        @ApiParam(value = "svn版本号")
        @QueryParam("reversion")
        reversion: Long,
        @ApiParam(value = "私钥或用户名")
        @QueryParam("credential1")
        credential1: String,
        @ApiParam(value = "密码")
        @QueryParam("credential2")
        credential2: String? = null
    ): Result<String>

    @ApiOperation("获取目录文件列表")
    @GET
    @Path("/getDir")
    fun getDirectories(
        @ApiParam(value = "仓库url")
        @QueryParam("url")
        url: String,
        @ApiParam(value = "用户id")
        @QueryParam("userId")
        userId: String,
        @ApiParam(value = "仓库类型")
        @QueryParam("type")
        svnType: String,
        @ApiParam(value = "相对路径")
        @QueryParam("svnPath")
        svnPath: String?,
        @ApiParam(value = "revision")
        @QueryParam("revision")
        revision: Long,
        @ApiParam(value = "用户名")
        @QueryParam("credential1")
        credential1: String,
        @ApiParam(value = "密码或私钥")
        @QueryParam("credential2")
        credential2: String,
        @ApiParam(value = "私钥密码")
        @QueryParam("credential3")
        credential3: String?
    ): Result<List<SvnFileInfo>>
}