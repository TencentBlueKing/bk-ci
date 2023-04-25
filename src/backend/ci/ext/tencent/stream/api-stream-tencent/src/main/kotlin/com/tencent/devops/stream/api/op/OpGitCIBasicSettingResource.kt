package com.tencent.devops.stream.api.op

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.stream.pojo.StreamBasicSetting
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.hibernate.validator.constraints.Range
import javax.validation.Valid
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

    @ApiOperation("修改项目开启人")
    @POST
    @Path("/updateEnableUserId")
    fun updateEnableUserIdByNewUser(
        @ApiParam(value = "旧userId", required = true)
        @QueryParam("oldUserId")
        oldUserId: String,
        @ApiParam(value = "新userId", required = true)
        @QueryParam("newUserId")
        newUserId: String,
        @ApiParam(value = "更新的数量", required = true)
        @QueryParam("limitNumber")
        @Range(min = 1, max = 50, message = "修改的数量不能小于1、大于50")
        @Valid
        limitNumber: Int
    ): Result<Boolean>

    @ApiOperation("修改工蜂老域名")
    @POST
    @Path("/updateGitDomain")
    fun updateGitDomain(
        @ApiParam(value = "git老域名", required = true)
        @QueryParam("oldGitDomain")
        oldGitDomain: String,
        @ApiParam(value = "git新域名", required = true)
        @QueryParam("newGitDomain")
        newGitDomain: String,
        @ApiParam(value = "更新的数量", required = true)
        @QueryParam("limitNumber")
        @Range(min = 1, max = 1000, message = "修改的数量不能小于1、大于1000")
        @Valid
        limitNumber: Int
    ): Result<Boolean>
}
