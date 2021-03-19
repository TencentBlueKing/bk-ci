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

package com.tencent.bk.codecc.defect.service.openapi;

import com.tencent.bk.codecc.defect.vo.admin.DeptTaskDefectReqVO;
import com.tencent.bk.codecc.defect.vo.openapi.TaskOverviewDetailRspVO;
import org.springframework.data.domain.Sort;

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
    TaskOverviewDetailRspVO statisticsTaskOverview(DeptTaskDefectReqVO reqVO, Integer pageNum, Integer pageSize,
            Sort.Direction sortType);


    /**
     * 查询个性化扫描任务的各工具分析统计概览
     *
     * @return rsp
     */
    TaskOverviewDetailRspVO statCustomTaskOverview(String customProjSource, Integer pageNum, Integer pageSize, Sort.Direction sortType);


}
