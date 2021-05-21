package com.tencent.bk.codecc.defect.api;

import com.tencent.bk.codecc.defect.vo.TaskLogOverviewVO;
import com.tencent.devops.common.api.pojo.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * 实际执行工具集保存接口
 *
 * @version V2.0
 * @date 2020/11/2
 */
@Api(tags = {"BUILD_CHECKER"}, description = "工具执行记录接口")
@Path("/build/taskLogOverview")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface BuildTaskLogOverviewResource {
    @ApiOperation("保存工具记录")
    @Path("/saveTools")
    @POST
    Result<Boolean> saveActualTools(
            @ApiParam(value = "规则导入请求对象", required = true)
                    TaskLogOverviewVO taskLogOverviewVO
    );
}
