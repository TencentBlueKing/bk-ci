package com.tencent.devops.store.api.ideatom

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.common.Classify
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_MARKET_IDE_ATOM_CLASSIFY"], description = "IDE插件市场-IDE插件分类")
@Path("/user/market/ideAtom/classifys")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserIdeAtomClassifyResource {

    @ApiOperation("获取所有IDE插件分类信息")
    @GET
    @Path("/")
    fun getAllIdeAtomClassifys(): Result<List<Classify>>
}