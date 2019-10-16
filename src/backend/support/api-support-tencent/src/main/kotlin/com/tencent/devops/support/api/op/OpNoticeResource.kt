package com.tencent.devops.support.api.op

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.core.MediaType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.support.model.app.NoticeRequest
import com.tencent.devops.support.model.app.pojo.Notice
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces

@Api(tags = ["OP_NOTICE"], description = "OP-公告")
@Path("/op/notice")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpNoticeResource {

    @ApiOperation("获取所有公告信息")
    @GET
    @Path("/")
    fun getAllNotice(): Result<List<Notice>>

    @ApiOperation("获取公告信息")
    @GET
    @Path("/{id}")
    fun getNotice(
        @ApiParam(value = "公告id", required = true)
        @PathParam("id")
        id: Long
    ): Result<Notice?>

    @ApiOperation("新增公告信息")
    @POST
    @Path("/")
    fun addNotice(
        @ApiParam(value = "公告请求报文体", required = true)
        noticeRequest: NoticeRequest
    ): Result<Int>

    @ApiOperation("更新公告信息")
    @PUT
    @Path("/{id}")
    fun updateNotice(
        @ApiParam(value = "公告id", required = true)
        @PathParam("id")
        id: Long,
        @ApiParam(value = "公告请求报文体", required = true)
        noticeRequest: NoticeRequest
    ): Result<Int>

    @ApiOperation("删除公告信息")
    @DELETE
    @Path("/{id}")
    fun deleteNotice(
        @ApiParam(value = "公告id", required = true)
        @PathParam("id")
        id: Long
    ): Result<Int>
}