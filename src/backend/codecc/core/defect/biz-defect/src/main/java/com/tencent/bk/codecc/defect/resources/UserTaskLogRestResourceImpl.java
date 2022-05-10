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

package com.tencent.bk.codecc.defect.resources;

import com.tencent.bk.codecc.defect.api.UserTaskLogRestResource;
import com.tencent.bk.codecc.defect.service.GetTaskLogService;
import com.tencent.bk.codecc.defect.service.TaskLogOverviewService;
import com.tencent.bk.codecc.defect.vo.QueryTaskLogVO;
import com.tencent.bk.codecc.defect.vo.TaskLogOverviewVO;
import com.tencent.bk.codecc.task.vo.QueryLogRepVO;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.web.RestResource;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_TASK_ID;

/**
 * 工具侧上报任务分析记录接口实现
 *
 * @version V1.0
 * @date 2019/5/5
 */
@RestResource
public class UserTaskLogRestResourceImpl implements UserTaskLogRestResource {
    @Autowired
    private GetTaskLogService getTaskLogService;

    @Autowired
    private TaskLogOverviewService taskLogOverviewService;

    @Override
    public Result<QueryTaskLogVO> getTaskLogs(String toolName, int page, int pageSize) {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String taskId = request.getHeader(AUTH_HEADER_DEVOPS_TASK_ID);
        QueryTaskLogVO queryTaskLogVO = new QueryTaskLogVO();
        queryTaskLogVO.setTaskId(Long.parseLong(taskId));
        queryTaskLogVO.setToolName(toolName);
        queryTaskLogVO.setPage(page);
        queryTaskLogVO.setPageSize(pageSize);
        return getTaskLogService.queryTaskLog(queryTaskLogVO);
    }

    @Override
    public Result<PageImpl<TaskLogOverviewVO>> getTaskLogs(Long taskId, Integer page, Integer pageSize) {
        return new Result<>(taskLogOverviewService.getTaskLogOverviewList(taskId, page, pageSize));
    }


    @Override
    public Result<QueryLogRepVO> getAnalysisLogs(String userId, String projectId, String pipelineId,
                                                 String buildId, String queryKeywords, String tag) {
        return new Result<>(getTaskLogService.queryAnalysisLog(userId, projectId, pipelineId, buildId, queryKeywords, tag));
    }

    @Override
    // NOCC:ParameterNumber(设计如此:)
    public Result<QueryLogRepVO> getMoreLogs(String userId, String projectId, String pipelineId,
                                             String buildId, Integer num, Boolean fromStart, Long start,
                                             Long end, String tag, Integer executeCount) {
        return new Result<>(getTaskLogService.getMoreLogs(userId, projectId, pipelineId, buildId, num,
                fromStart, start, end, tag, executeCount));
    }

    @Override
    public void downloadLogs(String userId, String projectId, String pipelineId, String buildId,
                             String tag, Integer executeCount) {
        getTaskLogService.downloadLogs(userId, projectId, pipelineId, buildId, tag, executeCount);
    }

    @Override
    // NOCC:ParameterNumber(设计如此:)
    public Result<QueryLogRepVO> getAfterLogs(String userId, String projectId, String pipelineId,
                                              String buildId, Long start, String queryKeywords,
                                              String tag, Integer executeCount) {
        return new Result<>(getTaskLogService.getAfterLogs(userId, projectId, pipelineId, buildId,
                start, queryKeywords, tag, executeCount));
    }


}
