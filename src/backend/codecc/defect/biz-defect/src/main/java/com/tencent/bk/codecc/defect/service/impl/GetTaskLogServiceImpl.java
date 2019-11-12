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

package com.tencent.bk.codecc.defect.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.codecc.defect.dao.mongorepository.TaskLogRepository;
import com.tencent.bk.codecc.defect.model.TaskLogEntity;
import com.tencent.bk.codecc.defect.service.GetTaskLogService;
import com.tencent.bk.codecc.defect.service.LogService;
import com.tencent.bk.codecc.defect.vo.QueryTaskLogVO;
import com.tencent.bk.codecc.defect.vo.TaskLogVO;
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.bk.codecc.task.vo.QueryLogRepVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.exception.UnauthorizedException;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.auth.api.external.BkAuthExPermissionApi;
import com.tencent.devops.common.auth.api.pojo.external.BkAuthExAction;
import com.tencent.devops.common.auth.api.pojo.external.model.BkAuthExResourceActionModel;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.util.JsonUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_USER_ID;

/**
 * LINT获取分析记录的逻辑实现类
 *
 * @version V1.0
 * @date 2019/5/12
 */
@Service
public class GetTaskLogServiceImpl implements GetTaskLogService
{

    @Autowired
    private Client client;

    @Autowired
    private LogService logService;

    @Autowired
    private TaskLogRepository taskLogRepository;

    @Autowired
    private BkAuthExPermissionApi bkAuthExPermissionApi;

    private static Logger logger = LoggerFactory.getLogger(GetTaskLogServiceImpl.class);


    @Override
    public Result queryTaskLog(QueryTaskLogVO queryTaskLogVO)
    {
        int pageNum = queryTaskLogVO.getPage() - 1;
        pageNum = pageNum < 0 ? 0 : pageNum;
        int pageSize = queryTaskLogVO.getPageSize() <= 0 ? 10 : queryTaskLogVO.getPageSize();
        Pageable pageable = new PageRequest(pageNum, pageSize);

        //1.查询分析记录总的数量
        Long totalCount = taskLogRepository.countByTaskIdAndToolName(queryTaskLogVO.getTaskId(), queryTaskLogVO.getToolName());

        //2.查询分析记录
        Page<TaskLogEntity> taskLogEntityPage = taskLogRepository.findByTaskIdAndToolNameOrderByStartTimeDesc(queryTaskLogVO.getTaskId(), queryTaskLogVO.getToolName(), pageable);

        List<TaskLogEntity> taskLogEntityList = taskLogEntityPage.getContent();
        String taskLogEntityListSerializseStr = JsonUtil.INSTANCE.toJson(taskLogEntityList);
        List<TaskLogVO> taskLogVOList = JsonUtil.INSTANCE.to(taskLogEntityListSerializseStr, new TypeReference<List<TaskLogVO>>()
        {
        });

        Page<TaskLogVO> taskLogVoPage = new PageImpl<>(taskLogVOList, pageable, totalCount);
        queryTaskLogVO.setTaskLogPage(taskLogVoPage);

        return new Result(queryTaskLogVO);
    }


    /**
     * 查询分析记录日志
     *
     * @param projectId     项目ID
     * @param pipelineId    流水线ID
     * @param buildId       构建号ID
     * @param queryKeywords 搜索词
     * @param tag           对应element ID
     * @return 日志信息
     */
    @Override
    public QueryLogRepVO queryAnalysisLog(String projectId, String pipelineId, String buildId, String queryKeywords, String tag)
    {
        validatePermission(pipelineId);
        return logService.getAnalysisLog(projectId, pipelineId, buildId, queryKeywords, tag);
    }


    /**
     * 查询更多的日志
     *
     * @param projectId    项目ID
     * @param pipelineId   流水线ID
     * @param buildId      构建号ID
     * @param num          行数
     * @param fromStart    是否顺序显示
     * @param start        开始行数
     * @param end          结束行数
     * @param tag          对应element ID
     * @param executeCount 执行次数
     * @return 日志信息
     */
    @Override
    public QueryLogRepVO getMoreLogs(String projectId, String pipelineId, String buildId, Integer num, Boolean fromStart, Long start, Long end, String tag, Integer executeCount)
    {
        validatePermission(pipelineId);
        return logService.getMoreLogs(projectId, pipelineId, buildId, num, fromStart, start, end, tag, executeCount);
    }


    /**
     * 下载分析记录日志
     *
     * @param projectId    项目ID
     * @param pipelineId   流水线ID
     * @param buildId      构建号ID
     * @param tag          对应element ID
     * @param executeCount 执行次数
     * @return 日志信息
     */
    @Override
    public void downloadLogs(String projectId, String pipelineId, String buildId, String tag, Integer executeCount)
    {
        validatePermission(pipelineId);
        logService.downloadLogs(projectId, pipelineId, buildId, tag, executeCount);
    }


    /**
     * 获取某行后的日志
     *
     * @param projectId    项目ID
     * @param pipelineId   流水线ID
     * @param buildId      构建号ID
     * @param tag          对应element ID
     * @param executeCount 执行次数
     * @return 日志信息
     */
    @Override
    public QueryLogRepVO getAfterLogs(String projectId, String pipelineId, String buildId, Long start, String queryKeywords, String tag, Integer executeCount)
    {
        validatePermission(pipelineId);
        return logService.getAfterLogs(projectId, pipelineId, buildId, start, queryKeywords, tag, executeCount);
    }


    /**
     * 验证权限
     *
     * @param pipelineId
     */
    private void validatePermission(String pipelineId)
    {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        String userName = request.getHeader(AUTH_HEADER_DEVOPS_USER_ID);

        Result<TaskDetailVO> taskInfo = client.get(ServiceTaskRestResource.class).getPipelineTask(pipelineId);
        if (taskInfo.isNotOk() || null == taskInfo.getData())
        {
            logger.error("get task detail info fail! pipeline id: {}", pipelineId);
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }

        TaskDetailVO taskDetail = taskInfo.getData();
        String projectId = taskDetail.getProjectId();
        long taskId = taskDetail.getTaskId();
        if (StringUtils.isBlank(userName) || StringUtils.isBlank(projectId) || taskId == 0)
        {
            logger.error("insufficient param info! user: {}, taskId: {}, projectId: {}", userName, taskId, projectId);
            throw new UnauthorizedException("insufficient param info!");
        }

        List<BkAuthExResourceActionModel> result = bkAuthExPermissionApi.validateBatchPermission(userName, String.valueOf(taskId), projectId, new ArrayList<BkAuthExAction>()
        {{
            add(BkAuthExAction.TASK_OWNER);
            add(BkAuthExAction.TASK_MEMBER);
            add(BkAuthExAction.ADMIN_MEMBER);
        }});
        if (CollectionUtils.isEmpty(result))
        {
            logger.error("empty validate result: {}", userName);
            throw new UnauthorizedException("unauthorized user permission!");
        }

        for (BkAuthExResourceActionModel auth : result)
        {
            if (Objects.nonNull(auth) && auth.isPass())
            {
                return;
            }
        }

        logger.error("validate permission fail! user: {}", userName);
        throw new UnauthorizedException("unauthorized user permission!");
    }


}
