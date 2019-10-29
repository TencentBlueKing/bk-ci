package com.tencent.devops.artifactory.api.builds

import com.tencent.devops.artifactory.pojo.CombinationPath
import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.artifactory.pojo.PathList
import com.tencent.devops.artifactory.pojo.PathPair
import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["BUILD_CUSTOM_DIR"], description = "版本仓库-自定义目录")
@Path("/build/customDir")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface BuildCustomDirResource {

    @ApiOperation("列举当前根目录下的所有文件")
    // @Path("/projects/{projectId}/list")
    @Path("/{projectId}/list")
    @GET
    fun list(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("文件夹路径", required = false)
        @QueryParam("path")
        path: String
    ): List<FileInfo>

    @ApiOperation("新建文件夹")
    // @Path("/projects/{projectId}/dir")
    @Path("/{projectId}/dir")
    @POST
    fun mkdir(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("文件夹路径", required = false)
        @QueryParam("path")
        path: String
    ): Result<Boolean>

    @ApiOperation("重命名")
    // @Path("/projects/{projectId}/rename")
    @Path("/{projectId}/rename")
    @POST
    fun rename(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("文件路径组合", required = false)
        pathPair: PathPair
    ): Result<Boolean>

    @ApiOperation("复制文件")
    // @Path("/projects/{projectId}/copy")
    @Path("/{projectId}/copy")
    @POST
    fun copy(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("组合路径", required = false)
        combinationPath: CombinationPath
    ): Result<Boolean>

    @ApiOperation("移动文件")
    // @Path("/projects/{projectId}/move")
    @Path("/{projectId}/move")
    @POST
    fun move(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("组合路径", required = false)
        combinationPath: CombinationPath
    ): Result<Boolean>

    @ApiOperation("删除文件")
    // @Path("/projects/{projectId}/delete")
    @Path("/{projectId}/")
    @DELETE
    fun delete(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("多个路径", required = false)
        pathList: PathList
    ): Result<Boolean>
}