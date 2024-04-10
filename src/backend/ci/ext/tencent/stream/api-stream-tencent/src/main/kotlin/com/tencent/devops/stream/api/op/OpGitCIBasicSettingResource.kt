package com.tencent.devops.stream.api.op

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.stream.pojo.StreamBasicSetting
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
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

@Tag(name = "OP_STREAM_SERVICES", description = "stream basic setting 管理")
@Path("/op/basic/setting")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpGitCIBasicSettingResource {

    @Operation(summary = "添加gitCI项目")
    @POST
    @Path("/save")
    fun save(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "工蜂项目", required = true)
        gitCIBasicSetting: StreamBasicSetting
    ): Result<Boolean>

    @Operation(summary = "填充存量流水线的组织架构信息")
    @GET
    @Path("/fixInfo")
    fun fixProjectInfo(): Result<Int>

    @Operation(summary = "修改stream项目信息")
    @POST
    @Path("/{gitProjectId}/update")
    fun updateBasicSetting(
        @Parameter(description = "项目id", required = true)
        @PathParam("gitProjectId")
        gitProjectId: Long,
        @Parameter(description = "是否开启commitCheck", required = true)
        @QueryParam("enableCommitCheck")
        enableCommitCheck: Boolean?,
        @Parameter(description = "是否开启MrComment", required = true)
        @QueryParam("enableMrComment")
        enableMrComment: Boolean?
    ): Result<Boolean>

    @Operation(summary = "填充存量流水线的带有名空间的项目名称")
    @GET
    @Path("/fixNameSpace")
    fun fixProjectNameSpace(): Result<Int>

    @Operation(summary = "修改项目开启人")
    @POST
    @Path("/updateEnableUserId")
    fun updateEnableUserIdByNewUser(
        @Parameter(description = "旧userId", required = true)
        @QueryParam("oldUserId")
        oldUserId: String,
        @Parameter(description = "新userId", required = true)
        @QueryParam("newUserId")
        newUserId: String,
        @Parameter(description = "更新的数量", required = true)
        @QueryParam("limitNumber")
        @Range(min = 1, max = 50, message = "修改的数量不能小于1、大于50")
        @Valid
        limitNumber: Int
    ): Result<Boolean>

    @Operation(summary = "修改工蜂老域名")
    @POST
    @Path("/updateGitDomain")
    fun updateGitDomain(
        @Parameter(description = "git老域名", required = true)
        @QueryParam("oldGitDomain")
        oldGitDomain: String,
        @Parameter(description = "git新域名", required = true)
        @QueryParam("newGitDomain")
        newGitDomain: String,
        @Parameter(description = "更新的数量", required = true)
        @QueryParam("limitNumber")
        @Range(min = 1, max = 1000, message = "修改的数量不能小于1、大于1000")
        @Valid
        limitNumber: Int
    ): Result<Boolean>
}
