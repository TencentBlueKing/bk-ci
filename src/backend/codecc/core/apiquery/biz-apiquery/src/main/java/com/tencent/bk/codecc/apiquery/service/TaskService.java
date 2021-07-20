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

import com.tencent.bk.codecc.apiquery.defect.model.FirstAnalysisSuccessModel;
import com.tencent.bk.codecc.apiquery.task.model.TaskInfoModel;
import com.tencent.bk.codecc.apiquery.vo.DeptInfoVO;
import com.tencent.bk.codecc.apiquery.vo.TaskInfoExtVO;
import com.tencent.bk.codecc.apiquery.vo.TaskToolInfoReqVO;
import com.tencent.bk.codecc.apiquery.vo.op.ActiveTaskStatisticsVO;
import com.tencent.bk.codecc.apiquery.vo.op.TaskAndToolStatChartVO;
import com.tencent.bk.codecc.apiquery.vo.op.TaskCodeLineStatVO;
import com.tencent.bk.codecc.apiquery.vo.report.UserLogInfoChartVO;
import com.tencent.devops.common.api.UserLogInfoStatVO;
import com.tencent.devops.common.api.pojo.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TaskService
{

    /**
     * OP:多条件分页查询任务列表
     *
     * @param reqVO     请求体
     * @return page
     */
    Page<TaskInfoExtVO> getOverAllTaskList(TaskToolInfoReqVO reqVO, Integer pageNum, Integer pageSize, String sortField,
            String sortType);

    /**
     * 获取子部门列表
     * @param parentId 父ID
     * @return list
     */
    List<DeptInfoVO> getChildDeptList(String parentId);


    /**
     * OP:多条件分页查询活跃任务列表
     *
     * @param reqVO     请求体
     * @return page
     */
    Page<ActiveTaskStatisticsVO> getActiveTaskList(TaskToolInfoReqVO reqVO, Integer pageNum, Integer pageSize,
            String sortField, String sortType);


    /**
     * 获取任务某个工具的首次分析成功时间对象
     *
     * @param taskId   任务ID
     * @param toolName 工具名
     * @return model
     */
    FirstAnalysisSuccessModel getFirstAnalyzeSuccess(Long taskId, String toolName);


    /**
     * 多条件分页获取任务model列表
     *
     * @param reqVO 请求体
     * @return page
     */
    Page<TaskInfoModel> findTaskInfoPage(TaskToolInfoReqVO reqVO, Pageable pageable);

    /**
     * 获取每日登录用户列表
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return list
     */
    Page<UserLogInfoStatVO> findDailyUserLogInfoList(String startTime, String endTime, Integer pageNum,
            Integer pageSize, String sortField, String sortType);

    /**
     * 获取总用户登录列表
     *
     * @return list
     */
    Page<UserLogInfoStatVO> findAllUserLogInfoStatList(Integer pageNum, Integer pageSize, String sortField,
            String sortType);

    /**
     * 获取每日用戶登录情况折线图数据
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return list
     */
    List<UserLogInfoChartVO> dailyUserLogInfoData(String startTime, String endTime);

    /**
     * 获取全部用戶登录情况折线图数据
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return list
     */
    List<UserLogInfoChartVO> sumUserLogInfoStatData(String startTime, String endTime);

    /**
     * 获取每周用戶登录情况折线图数据
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return list
     */
    List<UserLogInfoChartVO> weekUserLogInfoData(String startTime, String endTime);

    /**
     * 获取年对应的每一周时间段
     *
     * @return list
     */
    List<String> getWeekTime();

    /**
     * 获取任务和活跃任务折线图数据
     *
     * @param reqVO 任务工具信息请求体
     * @return list
     */
    List<TaskAndToolStatChartVO> taskAndActiveTaskData(TaskToolInfoReqVO reqVO);

    /**
     * 获取任务分析折线图数据
     *
     * @param reqVO 任务工具信息请求体
     * @return list
     */
    List<TaskAndToolStatChartVO> taskAnalyzeCountData(TaskToolInfoReqVO reqVO);

    Page<TaskCodeLineStatVO> queryTaskCodeLineStat(TaskToolInfoReqVO reqVO, Integer pageNum, Integer pageSize,
            String sortField, String sortType);
}
