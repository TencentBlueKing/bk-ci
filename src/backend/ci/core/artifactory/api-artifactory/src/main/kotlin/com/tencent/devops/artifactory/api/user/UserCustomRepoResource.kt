package com.tencent.devops.artifactory.api.user

import com.tencent.devops.artifactory.pojo.DirNode
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_CUSTOM_REPO"], description = "制品仓库-自定义仓库")
@Path("/user/custom-repo")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserCustomRepoResource {

    @ApiOperation("获取目录树")
    @GET
    @Path("/{projectId}/dir/tree")
    fun dirTree(
        @ApiParam("用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("路径", required = false)
        @QueryParam("path")
        path: String?,
        @ApiParam("模糊搜索目录名", required = false)
        @QueryParam("name")
        name: String?
    ): Result<DirNode>

    @ApiOperation("分页获取目录树")
    @GET
    @Path("/{projectId}/dir/tree/page")
    fun dirTreePage(
        @ApiParam("用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("路径", required = false)
        @QueryParam("path")
        path: String?,
        @ApiParam("模糊搜索目录名", required = false)
        @QueryParam("name")
        name: String?,
        @ApiParam("第几页", required = true)
        @QueryParam("page")
        page: Int,
        @ApiParam("每页条数", required = true)
        @QueryParam("pageSize")
        pageSize: Int
    ): Result<Page<DirNode>>
}
