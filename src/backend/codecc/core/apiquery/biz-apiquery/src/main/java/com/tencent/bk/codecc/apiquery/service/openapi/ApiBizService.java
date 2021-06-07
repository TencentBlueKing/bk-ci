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

package com.tencent.bk.codecc.apiquery.service.openapi;


import com.tencent.bk.codecc.apiquery.vo.TaskToolInfoReqVO;
import com.tencent.bk.codecc.apiquery.vo.op.TaskDefectSummaryVO;
import com.tencent.bk.codecc.apiquery.vo.openapi.CheckerDefectStatVO;
import com.tencent.bk.codecc.apiquery.vo.openapi.TaskOverviewDetailRspVO;
import com.tencent.devops.common.api.pojo.Page;

/**
 * OpenApi业务接口
 *
 * @version V1.0
 * @date 2020/3/16
 */

public interface ApiBizService
{
    /**
     * 按组织架构统计任务告警概览情况
     *
     * @param reqVO 请求体
     * @return rsp
     */
    TaskOverviewDetailRspVO statisticsTaskOverview(TaskToolInfoReqVO reqVO, Integer pageNum, Integer pageSize,
            String sortType);


    /**
     * 按规则分页统计告警数
     *
     * @param reqVO 请求体
     * @return page
     */
    Page<CheckerDefectStatVO> statCheckerDefect(TaskToolInfoReqVO reqVO, Integer pageNum, Integer pageSize);

    Page<TaskDefectSummaryVO> queryTaskDefectSum(TaskToolInfoReqVO reqVO, Integer pageNum, Integer pageSize,
            String sortField, String sortType);

    void exportDimensionToFile(TaskToolInfoReqVO reqVO, String fileIndex);

}
