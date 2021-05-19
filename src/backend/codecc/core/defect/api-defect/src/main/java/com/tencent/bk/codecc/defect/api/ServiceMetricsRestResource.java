package com.tencent.bk.codecc.defect.api;

import com.tencent.bk.codecc.defect.vo.MetricsVO;
import com.tencent.devops.common.api.pojo.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Api(tags = {"SERVICE_DEFECT"}, description = "告警模块树服务")
@Path("/service/metrics")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ServiceMetricsRestResource {
    @ApiOperation("度量信息")
    @Path("/")
    @GET
    Result<MetricsVO> getMetrics(
            @ApiParam(value = "任务ID", required = true)
            @HeaderParam(value = "taskId")
                    Long taskId,
            @ApiParam(value = "构建号", required = true)
            @QueryParam(value = "buildId")
                    String buildId);
}
