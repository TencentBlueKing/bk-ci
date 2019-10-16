package com.tencent.devops.support.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.support.model.app.pojo.Notice
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["NOTICE"], description = "公告")
@Path("/user/notice")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface NoticeResource {

    @ApiOperation("获取有效期内的公告")
    @GET
    @Path("/valid")
    fun getValidNotice(): Result<Notice?>

    @ApiOperation("获取所有的公告")
    @GET
    @Path("/")
    fun getAllNotice(): Result<List<Notice>>
}