package com.tencent.devops.store.api

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["EXTENSION_SERVICE_COMMENT"], description = "服务扩展_评论")
@Path("/user/extension/services/commnets")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserExtServiceCommentReplyResource {

    @ApiOperation("添加评论")
    @Path("/services/{serviceCodes}/comment")
    @POST
    fun createServiceComment()

    @ApiOperation("修改评论")
    @Path("/services/{serviceCodes}/comment")
    @PUT
    fun updateServiceComment()

    @ApiOperation("获取单条评论")
    @Path("/{commentId}")
    @GET
    fun getServiceComment()

    @ApiOperation("获取扩展服务所有评论")
    @Path("/servcies/{serviceCode}")
    @GET
    fun getServiceCommentByServiceCode()
}