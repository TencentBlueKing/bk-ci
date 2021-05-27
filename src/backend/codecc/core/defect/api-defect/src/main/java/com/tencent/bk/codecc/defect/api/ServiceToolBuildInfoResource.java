package com.tencent.bk.codecc.defect.api;

import com.tencent.bk.codecc.defect.vo.SetForceFullScanReqVO;
import com.tencent.devops.common.api.pojo.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * 工具构建信息接口
 *
 * @version V1.0
 * @date 2019/11/17
 */
@Api(tags = {"SERVICE_TOOL_BUILD_INFO"}, description = "工具构建信息接口")
@Path("/service/toolBuildInfo")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ServiceToolBuildInfoResource
{
    @ApiOperation("设置强制全量扫描标志位")
    @Path("/task/{taskId}/forceFullScanSymbol")
    @POST
    Result<Boolean> setForceFullScan(
            @ApiParam(value = "任务ID", required = true)
            @PathParam("taskId")
                    Long taskId,
            @ApiParam(value = "任务id及工具集映射参数", required = true)
                    List<String> toolNames);

    @ApiOperation("设置运行时栈强制全量扫描标志位")
    @Path("/stack/task/{taskId}/forceFullScan")
    @POST
    Result<Boolean> setToolBuildStackFullScan(
            @ApiParam(value = "任务ID", required = true)
            @PathParam("taskId")
                    Long taskId,
            @ApiParam(value = "任务id及工具集映射参数", required = true)
                    SetForceFullScanReqVO setForceFullScanReqVO);
}
