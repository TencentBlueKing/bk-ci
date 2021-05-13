package com.tencent.bk.codecc.defect.api;

import com.tencent.bk.codecc.defect.vo.TaskPersonalStatisticwVO;
import com.tencent.devops.common.api.pojo.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import java.util.List;

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_TASK_ID;
import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_USER_ID;

@Api(tags = {"USER_STATISTIC"}, description = "用户相关统计数据接口")
@Path("/user/statistic")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface UserOverviewRestResource {

    @ApiOperation("获取用户个人待处理数据")
    @Path("/overview/personal")
    @GET
    Result<TaskPersonalStatisticwVO> overview(
        @ApiParam("任务id")
        @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
            Long taskId,
        @ApiParam("用户名")
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String username);

    @ApiOperation("获取用户个人待处理数据")
    @Path("/overview/personal/list")
    @GET
    Result<List<TaskPersonalStatisticwVO>> overviewList(
            @ApiParam("任务id")
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
            Long taskId);
}
