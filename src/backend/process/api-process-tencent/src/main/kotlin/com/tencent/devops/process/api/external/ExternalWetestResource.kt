package com.tencent.devops.process.api.external

import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.FormParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["EXTERNAL_WETEST"], description = "Wetest相关")
@Path("/external/wetest")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ExternalWetestResource {

    @ApiOperation("soda的回调接口")
    @POST
    @Path("/callback")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    fun callback(
        @ApiParam(value = "相当于projectId", required = true)
        @FormParam("productID")
        productID: String,
        @ApiParam(value = "相当于pipelineId", required = true)
        @FormParam("jobID")
        jobID: String,
        @ApiParam(value = "buildID", required = true)
        @FormParam("buildID")
        buildID: String,
        @ApiParam(value = "wetest的taskID", required = true)
        @FormParam("taskID")
        taskID: String,
        @ApiParam(value = "wetest的sodaID，对应pipelineId", required = true)
        @FormParam("sodaID")
        sodaID: String,
        @ApiParam(value = "result_quality", required = false)
        @FormParam("result_quality")
        resultQuality: String,
        @ApiParam(value = "result_devnum", required = false)
        @FormParam("result_devnum")
        resultDevNum: String,
        @ApiParam(value = "result_rate", required = false)
        @FormParam("result_rate")
        resultRate: String,
        @ApiParam(value = "result_problems", required = false)
        @FormParam("result_problems")
        resultProblems: String,
        @ApiParam(value = "result_serious", required = false)
        @FormParam("result_serious")
        resultSerious: String,
        @ApiParam(value = "starttime", required = false)
        @FormParam("starttime")
        startTime: String,
        @ApiParam(value = "endtime", required = false)
        @FormParam("endtime")
        endTime: String
    ): Result<Boolean>
}