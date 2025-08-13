package com.tencent.devops.environment.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.environment.pojo.NodeTag
import com.tencent.devops.environment.pojo.NodeTagReq
import com.tencent.devops.environment.pojo.NodeTagUpdateReq
import com.tencent.devops.environment.pojo.UpdateNodeTag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "USER_NODE", description = "用户-节点标签")
@Path("/user/nodetag")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserNodeTagResource {
    @Operation(summary = "创建便签")
    @POST
    @Path("/create")
    fun createTag(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @QueryParam("projectId")
        projectId: String,
        data: NodeTagReq
    ): Result<Boolean>

    @Operation(summary = "查询项目标签和对应节点数")
    @GET
    @Path("/fetchTag")
    fun fetchTag(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @QueryParam("projectId")
        projectId: String
    ): Result<List<NodeTag>>

    @Operation(summary = "编辑节点标签信息")
    @POST
    @Path("/editTag")
    fun editTag(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @QueryParam("projectId")
        projectId: String,
        data: UpdateNodeTag
    ): Result<Boolean>

    @Operation(summary = "删除标签")
    @DELETE
    @Path("/deleteTag")
    fun deleteTag(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "标签名ID", required = true)
        @QueryParam("tagKeyId")
        tagKeyId: Long,
        @Parameter(description = "标签值ID,为空则删除标签名", required = true)
        @QueryParam("tagValueId")
        tagValueId: Long?
    ): Result<Boolean>

    @Operation(summary = "修改标签")
    @PUT
    @Path("/updateTag")
    fun updateTag(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @QueryParam("projectId")
        projectId: String,
        data: NodeTagUpdateReq
    ): Result<Boolean>

    @Operation(summary = "编辑节点标签信息")
    @POST
    @Path("/batchEditTag")
    fun batchEditTag(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @QueryParam("projectId")
        projectId: String,
        data: List<UpdateNodeTag>
    ): Result<Boolean>
}