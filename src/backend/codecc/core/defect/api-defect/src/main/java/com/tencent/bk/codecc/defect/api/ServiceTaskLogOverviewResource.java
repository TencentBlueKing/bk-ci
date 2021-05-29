package com.tencent.bk.codecc.defect.api;

import com.tencent.bk.codecc.defect.vo.TaskLogOverviewVO;
import com.tencent.devops.common.api.QueryTaskListReqVO;
import com.tencent.devops.common.api.annotation.ServiceInterface;
import com.tencent.devops.common.api.pojo.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Api(tags = {"BUILD_CHECKER"}, description = "工具执行记录接口")
@Path("/service/taskLogOverview")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ServiceInterface(value = "defect")
public interface ServiceTaskLogOverviewResource {
    @ApiOperation("获取工具记录")
    @Path("/")
    @GET
    Result<TaskLogOverviewVO> getTaskLogOverview(
            @ApiParam(value = "任务ID", required = true)
            @HeaderParam("taskId")
                    Long taskId,
            @ApiParam(value = "构建号")
            @QueryParam("buildId")
                    String buildId,
            @ApiParam(value = "任务状态")
            @QueryParam("status")
                    Integer status
    );

    @ApiOperation("获取工具记录")
    @Path("/analyze/result")
    @GET
    Result<TaskLogOverviewVO> getAnalyzeResult(
            @ApiParam(value = "任务ID", required = true)
            @HeaderParam("taskId")
                    Long taskId,
            @ApiParam(value = "构建号")
            @QueryParam("buildId")
                    String buildId,
            @ApiParam(value = "构建号")
            @QueryParam("buildNum")
                    String buildNum,
            @ApiParam(value = "构建号")
            @QueryParam("status")
                    Integer status
    );

    @ApiOperation("获取任务分析次数")
    @Path("/analyze/count")
    @POST
    Result<Integer> getTaskAnalyzeCount(
            @ApiParam(value = "请求体")
            @Valid QueryTaskListReqVO reqVO
    );

}
