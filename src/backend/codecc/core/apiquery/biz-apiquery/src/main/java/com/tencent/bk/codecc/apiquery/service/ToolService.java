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

package com.tencent.bk.codecc.apiquery.service;


import com.tencent.bk.codecc.apiquery.vo.TaskToolInfoReqVO;
import com.tencent.bk.codecc.apiquery.vo.ToolAnalyzeVO;
import com.tencent.bk.codecc.apiquery.vo.ToolConfigPlatformVO;
import com.tencent.bk.codecc.apiquery.vo.ToolRegisterStatisticsVO;
import com.tencent.bk.codecc.apiquery.vo.ToolAnalyzeStatVO;
import com.tencent.bk.codecc.apiquery.vo.ToolRegisterVO;
import com.tencent.bk.codecc.apiquery.vo.op.TaskAndToolStatChartVO;
import com.tencent.bk.codecc.apiquery.vo.op.ToolElapseTimeVO;
import com.tencent.devops.common.api.pojo.Page;

import java.util.List;
import java.util.Map;


/**
 * 工具管理服务接口
 *
 * @version V1.0
 * @date 2020/4/24
 */
public interface ToolService
{

    /**
     * 获取工具platform配置信息列表
     *
     * @param taskId     任务ID
     * @param toolName   工具名
     * @param platformIp IP
     * @return list
     */
    Page<ToolConfigPlatformVO> getPlatformInfoList(Long taskId, String toolName, String platformIp, Integer pageNum,
            Integer pageSize, String sortType);

    /**
     * 获取任务Platform详情信息
     *
     * @param taskId     任务ID
     * @param toolName   工具名
     * @return vo
     */
    ToolConfigPlatformVO getTaskPlatformDetail(Long taskId, String toolName);


    /**
     * 获取接入工具的任务ID列表
     *
     * @param taskIds      任务ID列表
     * @param toolName     工具
     * @param followStatus 跟进状态
     * @param isNot        是否取反查询(跟进状态)
     * @return list
     */
    List<Long> findTaskIdByToolNames(List<Long> taskIds, String toolName, Integer followStatus, boolean isNot);

    /**
     * 获取工具注册统计信息
     *
     * @param taskToolInfoReqVO
     * @return
     */
    List<ToolRegisterStatisticsVO> getToolRegisterStatisticsList(TaskToolInfoReqVO taskToolInfoReqVO);

    /**
     * 获取工具注册明细信息
     */
    Page<ToolRegisterVO> getAllToolRegisterList(TaskToolInfoReqVO reqVO, Integer pageNum, Integer pageSize,
            String sortField, String sortType);

    /**
     * 获取工具和活跃工具折线图数据
     *
     * @param reqVO 任务工具信息请求体
     * @return list
     */
    List<TaskAndToolStatChartVO> toolAndActiveToolStatData(TaskToolInfoReqVO reqVO);

    /**
     * 获取工具分析次数折线图数据
     *
     * @param reqVO 任务工具信息请求体
     * @return
     */
    Map<String, List<TaskAndToolStatChartVO>> toolAnalyzeCountData(TaskToolInfoReqVO reqVO);

    /**
     * 获取工具执行统计数据
     * @param reqVO 任务工具信息请求体
     * @return
     */
    List<ToolAnalyzeStatVO> getToolAnalyzeStatList(TaskToolInfoReqVO reqVO);

    /**
     * 获取工具执行明细信息
     *
     * @param reqVO     工具执行明细请求体
     * @param pageNum   页码
     * @param pageSize  每页数量
     * @param sortField 排序字段
     * @param sortType  排序类型
     * @return page
     */
    Page<ToolAnalyzeVO> getToolAnalyzeInfoList(TaskToolInfoReqVO reqVO, Integer pageNum, Integer pageSize,
            String sortField, String sortType);

    Map<String, List<ToolElapseTimeVO>> queryAnalyzeElapseTimeChart(TaskToolInfoReqVO reqVO);

}
