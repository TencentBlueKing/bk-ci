package com.tencent.devops.store.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.common.Classify
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_EXT_SERVICE_CLASSIFY"], description = "流水线-服务扩展分类")
@Path("/user/service/classify")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserExtServiceClassifyResource {

    @ApiOperation("获取所有流水线插件分类信息")
    @GET
    @Path("/")
    fun getAllAtomClassifys(): Result<List<Classify>>
}