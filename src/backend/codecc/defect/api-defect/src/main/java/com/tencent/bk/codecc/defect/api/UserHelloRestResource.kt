package com.tencent.bk.codecc.defect.api

import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["CODECC_DEMO"], description = "示例")
@Path("/user/hello") // 首段表示这个接口是哪个微服务的，第二段表示这个资源， 第三段表示是资源的id标识
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserHelloRestResource {

    @ApiOperation("通过ID获取Hello对象") // 这里是对swagger接口的简要说明接口名称
    @Path("/") // REST资源路径定义，{}表示这个是一个path参数可变
    @GET
    fun getHello(
    ): Result<String> // 结果中可能没有


    @ApiOperation("通过ID获取Hello对象") // 这里是对swagger接口的简要说明接口名称
    @Path("/hello") // REST资源路径定义，{}表示这个是一个path参数可变
    @GET
    fun getHello1(
    ): Result<String> // 结果中可能没有

}
