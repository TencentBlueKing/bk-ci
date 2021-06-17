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

package com.tencent.bk.codecc.apiquery.resources;

import com.tencent.bk.codecc.apiquery.api.OpToolRestResource;
import com.tencent.bk.codecc.apiquery.service.ToolService;
import com.tencent.bk.codecc.apiquery.vo.TaskToolInfoReqVO;
import com.tencent.bk.codecc.apiquery.vo.ToolAnalyzeStatVO;
import com.tencent.bk.codecc.apiquery.vo.ToolAnalyzeVO;
import com.tencent.bk.codecc.apiquery.vo.ToolRegisterStatisticsVO;
import com.tencent.bk.codecc.apiquery.vo.ToolRegisterVO;
import com.tencent.bk.codecc.apiquery.vo.op.TaskAndToolStatChartVO;
import com.tencent.bk.codecc.apiquery.vo.op.ToolElapseTimeVO;
import com.tencent.devops.common.api.pojo.Page;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.web.RestResource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

/**
 * op工具接口实现
 */
@RestResource
public class OpToolRestResourceImpl implements OpToolRestResource {
    @Autowired
    private ToolService toolService;

    @Override
    public Result<Page<ToolRegisterVO>> getToolRegisterInfoList(TaskToolInfoReqVO reqVO, Integer pageNum,
            Integer pageSize, String sortField, String sortType) {
        return new Result<>(toolService.getAllToolRegisterList(reqVO, pageNum, pageSize, sortField, sortType));
    }

    @Override
    public Result<List<ToolRegisterStatisticsVO>> getToolRegisterStatisticsList(TaskToolInfoReqVO taskToolInfoReqVO) {
        return new Result<>(toolService.getToolRegisterStatisticsList(taskToolInfoReqVO));
    }

    @Override
    public Result<List<TaskAndToolStatChartVO>> toolAndActiveToolStatData(TaskToolInfoReqVO reqVO) {
        return new Result<>(toolService.toolAndActiveToolStatData(reqVO));
    }

    @Override
    public Result<Map<String, List<TaskAndToolStatChartVO>>> toolAnalyzeCountData(TaskToolInfoReqVO reqVO) {
        return new Result<>(toolService.toolAnalyzeCountData(reqVO));
    }

    @Override
    public Result<List<ToolAnalyzeStatVO>> toolAnalyzeStatData(TaskToolInfoReqVO reqVO) {
        return new Result<>(toolService.getToolAnalyzeStatList(reqVO));
    }

    @Override
    public Result<Page<ToolAnalyzeVO>> getToolAnalyzeInfoList(TaskToolInfoReqVO reqVO, Integer pageNum,
            Integer pageSize, String sortField, String sortType) {
        return new Result<>(toolService.getToolAnalyzeInfoList(reqVO, pageNum, pageSize, sortField, sortType));
    }

    @Override
    public Result<Map<String, List<ToolElapseTimeVO>>> getToolElapseTimeChart(TaskToolInfoReqVO reqVO) {
        return new Result<>(toolService.queryAnalyzeElapseTimeChart(reqVO));
    }

}
