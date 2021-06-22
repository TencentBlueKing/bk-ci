/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.codecc.defect.service;

import com.tencent.bk.codecc.defect.vo.TaskLogRepoInfoVO;
import com.tencent.bk.codecc.defect.vo.TaskLogVO;
import com.tencent.bk.codecc.defect.vo.UploadTaskLogStepVO;
import com.tencent.bk.codecc.defect.vo.common.BuildVO;
import com.tencent.devops.common.api.analysisresult.BaseLastAnalysisResultVO;
import com.tencent.devops.common.api.analysisresult.ToolLastAnalysisResultVO;
import com.tencent.devops.common.api.pojo.Result;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 任务分析记录服务接口
 *
 * @version V1.0
 * @date 2019/5/5
 */
public interface TaskLogService
{

    /**
     * 查询最新任务分析记录
     *
     * @param taskId
     * @return
     */
    TaskLogVO getLatestTaskLog(long taskId, String toolName);

    /**
     * 批量查询任务记录，不包含分析结果
     * @param taskId
     * @param toolSet
     * @return
     */
    List<ToolLastAnalysisResultVO> getLastTaskLogResult(long taskId, Set<String> toolSet);

    /**
     * 批量查询最新任务分析记录
     *
     * @param taskId
     * @param toolSet
     * @return
     */
    List<ToolLastAnalysisResultVO> getLastAnalysisResults(long taskId, Set<String> toolSet);

    /**
     * 批量查询最新任务分析记录
     *
     * @param taskId
     * @param buildId
     * @return
     */
    List<ToolLastAnalysisResultVO> getAnalysisResultsByBuildId(long taskId, String buildId);

    /**
     * 查询任务分析记录清单
     *
     * @param taskId
     * @param buildNum
     * @return
     */
    List<ToolLastAnalysisResultVO> getAnalysisResults(long taskId, String buildNum);

    /**
     * 获取最近一次分析记录
     *
     * @param toolLastAnalysisResultVO
     * @param toolName
     * @return
     */
    BaseLastAnalysisResultVO getLastAnalysisResult(ToolLastAnalysisResultVO toolLastAnalysisResultVO, String toolName);

    /**
     * 获取最近一次分析记录
     *
     * @param toolLastAnalysisResultVO
     * @param toolName
     * @return
     */
    BaseLastAnalysisResultVO getAnalysisResult(ToolLastAnalysisResultVO toolLastAnalysisResultVO, String toolName);

    /**
     * 更新go语言的参数建议值信息
     *
     * @param uploadTaskLogStepVO
     * @return
     */
    Boolean uploadDirStructSuggestParam(UploadTaskLogStepVO uploadTaskLogStepVO);

    /**
     * 停止正在运行的任务
     * @param projectId
     * @param pipelineId
     * @param streamName
     * @param taskId
     * @param toolSet
     * @param userName
     * @return
     */
    Boolean stopRunningTask(String projectId, String pipelineId, String streamName, long taskId, Set<String> toolSet, String userName);

    /**
     * 获取当前构建的分析记录
     * @param taskId
     * @param toolName
     * @param buildId
     * @return
     */
    TaskLogVO getBuildTaskLog(long taskId, String toolName, String buildId);

    /**
     * 获取当前构建的分析记录
     * @param taskId
     * @param toolNameSet
     * @param buildId
     * @return
     */
    List<TaskLogVO> getBuildTaskLog(long taskId, List<String> toolNameSet, String buildId);

    /**
     * 上传分析记录
     * @param uploadTaskLogStepVO
     * @return
     */
    Result uploadTaskLog(UploadTaskLogStepVO uploadTaskLogStepVO);

    /**
     * 流水线运行失败调用接口
     * @param taskId
     * @param toolNames
     * @return
     */
    Boolean refreshTaskLogByPipeline(Long taskId, Set<String> toolNames);

    /**
     * 查询任务构建列表
     *
     * @param taskId
     * @return
     */
    List<BuildVO> getTaskBuildInfos(long taskId, int limit);

    /**
     * 批量获取任务的工具最新分析记录
     *
     * @param taskIds  任务ID集合
     * @param toolName 工具名
     * @return list
     */
    List<TaskLogVO> batchTaskLogList(Set<Long> taskIds, String toolName);

    /**
     * 获取时间区间
     *
     * @param taskIdSet 任务ID集合
     * @param startTime 分析开始时间
     * @param endTime   分析结束时间
     * @return list
     */
    List<TaskLogVO> batchTaskLogListByTime(Set<Long> taskIdSet, Long startTime, Long endTime);

    /**
     * 获取最新构建的代码库信息
     *
     * @param taskId
     */
    Map<String, TaskLogRepoInfoVO> getLastAnalyzeRepoInfo(long taskId);

    /**
     * 获取最新构建的指定工具代码库信息
     *
     * @param taskId
     * @param toolName
     */
    TaskLogRepoInfoVO getLastAnalyzeRepoInfo(long taskId, String toolName);

    /**
     * 通过任务id查询最近一次分析记录信息，不分工具
     *
     * @param taskId
     * @return
     */
    List<TaskLogVO> findLastBuildInfo(long taskId);

    /**
     * 获取本次构建执行情况
     *
     */
    List<TaskLogVO> getCurrBuildInfo(long taskId, String buildId);

    Map<String, Boolean> defectCommitSuccess(long taskId, List<String> toolNameSet, String buildId, int stepNum);
}
