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

import com.tencent.bk.codecc.defect.vo.QueryTaskLogVO;
import com.tencent.bk.codecc.defect.vo.admin.DeptTaskDefectReqVO;
import com.tencent.bk.codecc.defect.vo.admin.DeptTaskDefectRspVO;
import com.tencent.bk.codecc.task.vo.QueryLogRepVO;
import com.tencent.devops.common.api.QueryTaskListReqVO;
import com.tencent.devops.common.api.pojo.Result;

/**
 * 任务分析记录服务层
 *
 * @version V1.0
 * @date 2019/5/5
 */
public interface GetTaskLogService {

    /**
     * 查询分析记录
     *
     * @param queryTaskLogVO
     * @return
     */
    Result<QueryTaskLogVO> queryTaskLog(QueryTaskLogVO queryTaskLogVO);


    /**
     * 查询分析记录日志
     *
     * @param projectId
     * @param pipelineId
     * @param buildId
     * @param tag
     * @return
     */
    QueryLogRepVO queryAnalysisLog(String userId, String projectId, String pipelineId, String buildId,
                                   String queryKeywords, String tag);


    /**
     * 获取分析记录更多日志
     *
     * @param projectId
     * @param pipelineId
     * @param buildId
     * @param tag
     * @return
     */
    // NOCC:ParameterNumber(设计如此:)
    QueryLogRepVO getMoreLogs(String userId, String projectId, String pipelineId, String buildId, Integer num,
                              Boolean fromStart, Long start, Long end, String tag, Integer executeCount);


    /**
     * 下载分析记录日志
     *
     * @param projectId
     * @param pipelineId
     * @param buildId
     * @param tag
     * @param executeCount
     * @return
     */
    void downloadLogs(String userId, String projectId, String pipelineId, String buildId,
                      String tag, Integer executeCount);


    /**
     * 获取某行后的日志
     *
     * @param projectId
     * @param pipelineId
     * @param buildId
     * @param tag
     * @param executeCount
     * @return
     */
    // NOCC:ParameterNumber(设计如此:)
    QueryLogRepVO getAfterLogs(String userId, String projectId, String pipelineId, String buildId,
                               Long start, String queryKeywords, String tag, Integer executeCount);

    /**
     * 获取活跃任务列表(是否活跃看时间区间内 任务的任意工具有成功分析的记录都为活跃)
     *
     * @param deptTaskDefectReqVO reqObj
     * @return resp
     */
    DeptTaskDefectRspVO getActiveTaskList(DeptTaskDefectReqVO deptTaskDefectReqVO);

}
