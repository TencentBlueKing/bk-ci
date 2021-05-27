package com.tencent.bk.codecc.task.api;

import com.tencent.bk.codecc.task.vo.BuildIdRelationShipVO;
import com.tencent.devops.common.api.ToolMetaBaseVO;
import com.tencent.devops.common.api.pojo.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * 构建机任务接口
 *
 * @version V1.0
 * @date 2019/7/21
 */
@Api(tags = {"SERVICE_RELATIONSHIP"})
@Path("/service/relationship")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ServiceBuildIdRelationshipResource {
    @ApiOperation("查询构建ID关系")
    @Path("/buildId/{buildId}")
    @GET
    Result<BuildIdRelationShipVO> getRelationShip(
            @ApiParam(value = "是否查询详细信息")
            @PathParam("buildId")
                    String buildId);
}
