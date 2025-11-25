package com.tencent.devops.store.api.trigger

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.pojo.atom.form.AtomForm
import com.tencent.devops.store.pojo.atom.AtomResp
import com.tencent.devops.store.pojo.atom.AtomRespItem
import com.tencent.devops.store.pojo.trigger.TriggerGroupInfo
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "USER_PIPELINE_CONTAINER", description = "触发资源")
@Path("/user/market/trigger")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserTriggerResource {

    @Operation(summary = "预览配置信息")
    @GET
    @Path("/event/preview/{storeId}")
    fun preview(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "组件ID", required = true)
        @PathParam("storeId")
        storeId: String
    ): Result<AtomForm?>

    @Operation(summary = "触发器分类信息")
    @GET
    @Path("/types")
    fun types(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "插件分类id", required = false)
        @QueryParam("classifyId")
        classifyId: String?,
        @Parameter(description = "项目编码", required = true)
        @QueryParam("projectCode")
        projectCode: String
    ): Result<List<TriggerGroupInfo>>

    @Operation(summary = "触发器列表")
    @GET
    @Path("/atoms")
    fun atoms(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目编码", required = true)
        @QueryParam("projectCode")
        projectCode: String,
        @Parameter(description = "分类", required = true)
        @QueryParam("type")
        type: String?,
        @Parameter(description = "插件分类id", required = false)
        @QueryParam("classifyId")
        classifyId: String?,
        @Parameter(description = "插件搜索关键字", required = false)
        @QueryParam("keyword")
        keyword: String?,
        @Parameter(description = "页码", required = true)
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页数量", required = true)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<AtomResp<AtomRespItem>?>
}