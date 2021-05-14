package com.tencent.bk.codecc.defect.api;

import com.tencent.devops.common.api.annotation.ServiceInterface;
import com.tencent.devops.common.api.clusterresult.BaseClusterResultVO;
import com.tencent.devops.common.api.pojo.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Api(tags = {"SERVICE_CLUSTER_STATISTIC"}, description = "聚类统计接口")
@Path("/service/clusterStatistic")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ServiceInterface(value = "defect")
public interface ServiceClusterStatisticRestReource {
    @ApiOperation("获取聚类统计信息")
    @Path("/")
    @GET
    Result<List<BaseClusterResultVO>> getClusterStatistic(
            @ApiParam(value = "任务id", required = true)
            @QueryParam("taskId")
                    long taskId,
            @ApiParam(value = "构建号", required = true)
            @QueryParam("buildId")
                    String buildId
    );
}
