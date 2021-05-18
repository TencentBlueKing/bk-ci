/*
 * Tencent is pleased to support the open source community by making BlueKing available.
 * Copyright (C) 2017-2018 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.bk.codecc.apiquery.api;

import com.tencent.bk.codecc.apiquery.vo.DeptInfoVO;
import com.tencent.bk.codecc.apiquery.vo.TaskInfoExtVO;
import com.tencent.bk.codecc.apiquery.vo.TaskToolInfoReqVO;
import com.tencent.bk.codecc.apiquery.vo.ToolConfigPlatformVO;
import com.tencent.bk.codecc.apiquery.vo.op.ActiveTaskStatisticsVO;
import com.tencent.devops.common.api.pojo.Page;
import com.tencent.devops.common.api.pojo.CodeCCResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_USER_ID;


/**
 * op任务接口实现
 *
 * @version V2.0
 * @date 2020/4/24
 */

@Api(tags = {"OP_TASK"}, description = "任务管理接口")
@Path("/op/task")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface OpTaskRestResource
{

    @ApiOperation("获取工具platform信息列表")
    @Path("/platform/list")
    @GET
    CodeCCResult<Page<ToolConfigPlatformVO>> getPlatformInfo(@ApiParam(value = "任务ID") @QueryParam("taskId") Long taskId,
            @ApiParam(value = "工具名称") @QueryParam("toolName") String toolName,
            @ApiParam(value = "platform ip") @QueryParam("platformIp") String platformIp,
            @ApiParam(value = "页数") @QueryParam(value = "pageNum") Integer pageNum,
            @ApiParam(value = "每页多少条") @QueryParam(value = "pageSize") Integer pageSize,
            @ApiParam(value = "排序类型") @QueryParam(value = "sortType") String sortType);


    @ApiOperation("获取工具platform信息详情")
    @Path("/platform/detail")
    @GET
    CodeCCResult<ToolConfigPlatformVO> getPlatformDetail(@ApiParam(value = "任务ID") @QueryParam("taskId") Long taskId,
            @ApiParam(value = "工具名称") @QueryParam("toolName") String toolName);


    @ApiOperation("获取所有任务信息")
    @Path("/overall")
    @POST
    CodeCCResult<Page<TaskInfoExtVO>> getOverAllTaskList(@ApiParam(value = "任务管理请求体") @Valid TaskToolInfoReqVO reqVO,
            @ApiParam(value = "页数") @QueryParam(value = "pageNum") Integer pageNum,
            @ApiParam(value = "每页多少条") @QueryParam(value = "pageSize") Integer pageSize,
            @ApiParam(value = "排序字段") @QueryParam(value = "sortField") String sortField,
            @ApiParam(value = "排序类型") @QueryParam(value = "sortType") String sortType);


    @ApiOperation("获取子部门信息列表")
    @Path("/dept/list")
    @GET
    CodeCCResult<List<DeptInfoVO>> getDeptList(@ApiParam(value = "父级部门ID") @QueryParam("parentId") String parentId);


    @ApiOperation("通过分析记录查询时间范围内的活跃项目")
    @Path("/activeTask/list")
    @POST
    CodeCCResult<Page<ActiveTaskStatisticsVO>> queryActiveTaskListByLog(
            @ApiParam(value = "用户名", required = true) @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID) String userName,
            @ApiParam(value = "按组织架构查询任务告警请求", required = true) @Valid TaskToolInfoReqVO taskToolInfoReqVO,
            @ApiParam(value = "页数") @QueryParam(value = "pageNum") Integer pageNum,
            @ApiParam(value = "每页多少条") @QueryParam(value = "pageSize") Integer pageSize,
            @ApiParam(value = "排序字段") @QueryParam(value = "sortField") String sortField,
            @ApiParam(value = "排序类型") @QueryParam(value = "sortType") String sortType
    );


}
