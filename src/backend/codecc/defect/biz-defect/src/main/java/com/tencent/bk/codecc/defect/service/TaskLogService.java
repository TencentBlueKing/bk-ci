/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
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

import com.tencent.bk.codecc.defect.vo.TaskLogVO;
import com.tencent.bk.codecc.defect.vo.UploadTaskLogStepVO;
import com.tencent.devops.common.api.BaseLastAnalysisResultVO;
import com.tencent.devops.common.api.ToolLastAnalysisResultVO;

import java.util.List;
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
     * 批量查询最新任务分析记录
     *
     * @param taskId
     * @param toolSet
     * @return
     */
    List<ToolLastAnalysisResultVO> getLastAnalysisResults(long taskId, Set<String> toolSet);

    /**
     * 查询任务分析记录清单
     *
     * @param taskId
     * @param toolName
     * @return
     */
    List<ToolLastAnalysisResultVO> getAnalysisResultsList(long taskId, String toolName);

    /**
     * 获取最近一次分析记录
     *
     * @param toolLastAnalysisResultVO
     * @param toolName
     * @return
     */
    BaseLastAnalysisResultVO getLastAnalysisResult(ToolLastAnalysisResultVO toolLastAnalysisResultVO, String toolName);

    /**
     * 更新go语言的参数建议值信息
     *
     * @param uploadTaskLogStepVO
     * @return
     */
    Boolean uploadDirStructSuggestParam(UploadTaskLogStepVO uploadTaskLogStepVO);

    /**
     * 停止正在运行的任务
     *
     * @param taskId
     * @param toolSet
     * @param taskBaseVO
     * @param userName
     * @return
     */
    Boolean stopRunningTask(String projectId, String pipelineId, String streamName, long taskId, Set<String> toolSet, String userName);
}
