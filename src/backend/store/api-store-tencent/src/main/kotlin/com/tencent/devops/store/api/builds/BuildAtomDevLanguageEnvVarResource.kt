package com.tencent.devops.store.api.builds

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.AtomDevLanguageEnvVar
import com.tencent.devops.store.pojo.enums.BuildHostOsEnum
import com.tencent.devops.store.pojo.enums.BuildHostTypeEnum
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["BUILD_ATOM_DEV_LANGUAGE_ENV_VAR"], description = "插件-开发语言环境变量")
@Path("/build/market/atom/dev/language/env/var")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface BuildAtomDevLanguageEnvVarResource {

    @ApiOperation("获取插件开发语言相关的环境变量")
    @GET
    @Path("/languages/{language}/types/{buildHostType}/oss/{buildHostOs}")
    fun getAtomDevLanguageEnvVars(
        @ApiParam("开发语言", required = true)
        @PathParam("language")
        language: String,
        @ApiParam("适用构建机类型", required = true)
        @PathParam("buildHostType")
        buildHostType: BuildHostTypeEnum,
        @ApiParam("适用构建机操作系统", required = true)
        @PathParam("buildHostOs")
        buildHostOs: BuildHostOsEnum
    ): Result<List<AtomDevLanguageEnvVar>?>
}