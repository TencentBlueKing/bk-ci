package com.tencent.devops.stream.api.op

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.stream.pojo.StreamBasicSetting
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["OP_STREAM_SERVICES"], description = "stream basic setting 管理")
@Path("/op/basic/setting")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpGitCIBasicSettingResource {

    @ApiOperation("添加gitCI项目")
    @POST
    @Path("/save")
    fun save(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam(value = "工蜂项目", required = true)
        gitCIBasicSetting: StreamBasicSetting
    ): Result<Boolean>

    @ApiOperation("填充存量流水线的组织架构信息")
    @GET
    @Path("/fixInfo")
    fun fixProjectInfo(): Result<Int>

    @ApiOperation("修改stream项目信息")
    @POST
    @Path("/{gitProjectId}/update")
    fun updateBasicSetting(
        @ApiParam(value = "项目id", required = true)
        @PathParam("gitProjectId")
        gitProjectId: Long,
        @ApiParam(value = "是否开启commitCheck", required = true)
        @QueryParam("enableCommitCheck")
        enableCommitCheck: Boolean?,
        @ApiParam(value = "是否开启MrComment", required = true)
        @QueryParam("enableMrComment")
        enableMrComment: Boolean?
    ): Result<Boolean>

    @ApiOperation("填充存量流水线的带有名空间的项目名称")
    @GET
    @Path("/fixNameSpace")
    fun fixProjectNameSpace(): Result<Int>
}
