package com.tencent.devops.support.api.external

import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["EXTERNAL_WECHART_WORK"], description = "外部-企业微信")
@Path("/external/wechat-work")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ExternalWechatWorkResource {

    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.TEXT_XML)
    @ApiOperation("企业微信回调接口(POST)")
    @POST
    @Path("/callback")
    fun callback(
        @ApiParam(value = "消息体签名", required = true)
        @QueryParam(value = "msg_signature")
        signature: String,
        @ApiParam(value = "时间戳", required = true)
        @QueryParam(value = "timestamp")
        timestamp: Long,
        @ApiParam(value = "随机数字串", required = true)
        @QueryParam(value = "nonce")
        nonce: String,
        @ApiParam(value = "回调密文", required = true)
        reqData: String?
    ): Result<Boolean>

    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.TEXT_XML)
    @ApiOperation("企业微信回调接口(GET)")
    @GET
    @Path("/callback")
    fun callback(
        @ApiParam(value = "消息体签名", required = true)
        @QueryParam(value = "msg_signature")
        signature: String,
        @ApiParam(value = "时间戳", required = true)
        @QueryParam(value = "timestamp")
        timestamp: Long,
        @ApiParam(value = "随机数字串", required = true)
        @QueryParam(value = "nonce")
        nonce: String,
        @ApiParam(value = "随机加密字符串", required = true)
        @QueryParam(value = "echostr")
        echoStr: String,
        @ApiParam(value = "回调密文", required = false)
        reqData: String?
    ): Result<String>
}