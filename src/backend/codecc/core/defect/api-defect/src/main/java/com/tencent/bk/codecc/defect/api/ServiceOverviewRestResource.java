package com.tencent.bk.codecc.defect.api;

import com.tencent.devops.common.api.pojo.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Api(tags = {"SERVICE_STATISTIC"}, description = "服务相关统计数据接口")
@Path("/service/statistic")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ServiceOverviewRestResource {

    @ApiOperation("获取用户个人待处理数据")
    @Path("/overview/refresh")
    @POST
    Result<Boolean> refresh(
        @ApiParam("任务id")
        @QueryParam("taskId")
            Long taskId,
        @ApiParam("额外信息")
        @QueryParam("extraInfo")
            String extraInfo);
}
