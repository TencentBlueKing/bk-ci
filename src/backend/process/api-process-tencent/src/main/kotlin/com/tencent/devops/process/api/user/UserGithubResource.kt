package com.tencent.devops.process.api.user

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.process.pojo.github.GithubAppUrl
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_GITHUB"], description = "用户-Github")
@Path("/user/github")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserGithubResource {

    @ApiOperation("获取github触发原子配置")
    @GET
    @Path("/githubAppUrl")
    fun getGithubAppUrl(): Result<GithubAppUrl>
}