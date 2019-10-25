package com.tencent.devops.store.api.ideatom

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.common.Category
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_MARKET_IDE_ATOM_CATEGORY"], description = "IDE插件-IDE插件范畴")
@Path("/user/market/ideAtom/categorys")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserIdeAtomCategoryResource {

    @ApiOperation("获取所有IDE插件范畴信息")
    @GET
    @Path("/")
    fun getAllIdeAtomCategorys(): Result<List<Category>?>
}