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
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.dao.mongorepository.TaskLogRepository;
import com.tencent.bk.codecc.defect.dao.mongotemplate.TaskLogDao;
import com.tencent.bk.codecc.defect.model.TaskLogEntity;
import com.tencent.bk.codecc.defect.service.GetTaskLogService;
import com.tencent.bk.codecc.defect.service.LogService;
import com.tencent.bk.codecc.defect.utils.ConvertUtil;
import com.tencent.bk.codecc.defect.vo.QueryTaskLogVO;
import com.tencent.bk.codecc.defect.vo.TaskLogVO;
import com.tencent.bk.codecc.defect.vo.admin.ActiveTaskStatisticsVO;
import com.tencent.bk.codecc.defect.vo.admin.DeptTaskDefectReqVO;
import com.tencent.bk.codecc.defect.vo.admin.DeptTaskDefectRspVO;
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.bk.codecc.task.api.UserMetaRestResource;
import com.tencent.bk.codecc.task.vo.MetadataVO;
import com.tencent.bk.codecc.task.vo.QueryLogRepVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.bk.codecc.task.vo.pipeline.PipelineTaskVO;
import com.tencent.devops.common.api.QueryTaskListReqVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.exception.UnauthorizedException;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.auth.api.external.AuthExPermissionApi;
import com.tencent.devops.common.auth.api.external.AuthTaskService;
import com.tencent.devops.common.auth.api.pojo.external.CodeCCAuthAction;
import com.tencent.devops.common.auth.api.pojo.external.PipelineAuthAction;
import com.tencent.devops.common.auth.api.pojo.external.model.BkAuthExResourceActionModel;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.constant.RedisKeyConstants;
import com.tencent.devops.common.util.DateTimeUtils;
import com.tencent.devops.common.util.JsonUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.tencent.devops.common.util.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_USER_ID;

/**
 * LINT获取分析记录的逻辑实现类
 *
 * @version V1.0
 * @date 2019/5/12
 */
@Service
public class GetTaskLogServiceImpl implements GetTaskLogService {

    @Autowired
    private Client client;

    @Autowired
    private LogService logService;

    @Autowired
    private TaskLogDao taskLogDao;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private TaskLogRepository taskLogRepository;

    @Autowired
    private AuthExPermissionApi bkAuthExPermissionApi;

    @Autowired
    private AuthTaskService authTaskService;

    private static Logger logger = LoggerFactory.getLogger(GetTaskLogServiceImpl.class);


    @Override
    public Result<QueryTaskLogVO> queryTaskLog(QueryTaskLogVO queryTaskLogVO) {
        int pageNum = queryTaskLogVO.getPage() - 1;
        pageNum = Math.max(pageNum, 0);
        int pageSize = queryTaskLogVO.getPageSize() <= 0 ? 10 : queryTaskLogVO.getPageSize();
        Pageable pageable = PageRequest.of(pageNum, pageSize);

        // 查询分析记录
        List<TaskLogEntity> taskLogEntities = taskLogRepository.findByTaskIdAndToolName(queryTaskLogVO.getTaskId(),
                queryTaskLogVO.getToolName());

        // 排序和分页
        taskLogEntities.sort(((o1, o2) -> {
            Integer value2 = Integer.valueOf(StringUtils.isNotEmpty(o2.getBuildNum()) ? o2.getBuildNum() : "-1");
            Integer value1 = Integer.valueOf(StringUtils.isNotEmpty(o1.getBuildNum()) ? o1.getBuildNum() : "-1");
            return value2.compareTo(value1);
        }));
        int totalCount = taskLogEntities.size();
        int subListBeginIdx = pageNum * pageSize;
        int subListEndIdx = subListBeginIdx + pageSize;
        if (subListBeginIdx > totalCount) {
            subListBeginIdx = 0;
        }
        taskLogEntities = taskLogEntities.subList(subListBeginIdx, Math.min(subListEndIdx, totalCount));

        String taskLogEntityListJsonStr = JsonUtil.INSTANCE.toJson(taskLogEntities);
        List<TaskLogVO> taskLogVOList = JsonUtil.INSTANCE.to(
                taskLogEntityListJsonStr, new TypeReference<List<TaskLogVO>>() {
                });

        Page<TaskLogVO> taskLogVoPage = new PageImpl<>(taskLogVOList, pageable, totalCount);
        queryTaskLogVO.setTaskLogPage(taskLogVoPage);

        return new Result<>(queryTaskLogVO);
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
    public QueryLogRepVO queryAnalysisLog(String userId, String projectId, String pipelineId, String buildId,
                                          String queryKeywords, String tag) {
        validatePermission(pipelineId);
        return logService.getAnalysisLog(userId, projectId, pipelineId, buildId, queryKeywords, tag);
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
    // NOCC:ParameterNumber(设计如此:)
    public QueryLogRepVO getMoreLogs(String userId, String projectId, String pipelineId, String buildId, Integer num,
                                     Boolean fromStart, Long start, Long end, String tag, Integer executeCount) {
        validatePermission(pipelineId);
        return logService.getMoreLogs(userId, projectId, pipelineId, buildId, num,
                fromStart, start, end, tag, executeCount);
    }


    /**
     * 下载分析记录日志
     *
     * @param projectId    项目ID
     * @param pipelineId   流水线ID
     * @param buildId      构建号ID
     * @param tag          对应element ID
     * @param executeCount 执行次数
     */
    @Override
    public void downloadLogs(String userId, String projectId, String pipelineId, String buildId,
                             String tag, Integer executeCount) {
        validatePermission(pipelineId);
        logService.downloadLogs(userId, projectId, pipelineId, buildId, tag, executeCount);
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
    // NOCC:ParameterNumber(设计如此:)
    public QueryLogRepVO getAfterLogs(String userId, String projectId, String pipelineId, String buildId,
                                      Long start, String queryKeywords, String tag, Integer executeCount) {
        validatePermission(pipelineId);
        return logService.getAfterLogs(userId, projectId, pipelineId, buildId, start,
                queryKeywords, tag, executeCount);
    }

    /**
     * 获取活跃任务列表
     *
     * @param deptTaskDefectReqVO reqObj
     * @return activeTaskList
     */
    @Override
    public DeptTaskDefectRspVO getActiveTaskList(DeptTaskDefectReqVO deptTaskDefectReqVO) {
        logger.info("getActiveTaskList req content: {}", deptTaskDefectReqVO);
        DeptTaskDefectRspVO deptTaskListRspVO = new DeptTaskDefectRspVO();

        List<ActiveTaskStatisticsVO> activeTaskList = Lists.newArrayList();
        QueryTaskListReqVO queryTaskListReqVO = new QueryTaskListReqVO();

        BeanUtils.copyProperties(deptTaskDefectReqVO, queryTaskListReqVO);
        Result<List<TaskDetailVO>> result =
                client.get(ServiceTaskRestResource.class).batchGetTaskList(queryTaskListReqVO);

        List<TaskDetailVO> taskDetailVoList = result.getData();
        if (CollectionUtils.isNotEmpty(taskDetailVoList)) {
            Set<Long> taskIdSet = taskDetailVoList.stream()
                    .filter(taskDetailVO -> StringUtils.isNotEmpty(taskDetailVO.getToolNames()))
                    .map(TaskDetailVO::getTaskId).collect(Collectors.toSet());

            // 时间范围
            long startTime = DateTimeUtils.getTimeStampStart(deptTaskDefectReqVO.getStartDate());
            long endTime = DateTimeUtils.getTimeStampEnd(deptTaskDefectReqVO.getEndDate());

            // 批量获取任务分析日志
            List<TaskLogEntity> taskLogList = taskLogDao.findTaskLogByTime(taskIdSet, startTime, endTime);

            // 识别活跃任务
            Map<Long, String> activeTaskMap = getActiveTaskMap(taskLogList);

            // 获取语言元数据
            Result<Map<String, List<MetadataVO>>> metaDataResult =
                    client.get(UserMetaRestResource.class).metadatas(ComConstants.KEY_CODE_LANG);
            Map<String, List<MetadataVO>> metaDataResultData = metaDataResult.getData();
            if (metaDataResultData == null) {
                throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
            }
            List<MetadataVO> metadataVoList = metaDataResultData.get(ComConstants.KEY_CODE_LANG);

            // 获取组织架构信息
            Map<String, String> deptInfo =
                    (Map<String, String>) redisTemplate.opsForHash().entries(RedisKeyConstants.KEY_DEPT_INFOS);

            activeTaskList = taskDetailVoList.stream().map(taskDetailVO -> {
                ActiveTaskStatisticsVO activeTaskVO = new ActiveTaskStatisticsVO();
                BeanUtils.copyProperties(taskDetailVO, activeTaskVO);

                String status = activeTaskMap.get(taskDetailVO.getTaskId());
                activeTaskVO.setIsActive(status == null ? "非活跃" : status);

                activeTaskVO.setBgName(deptInfo.get(String.valueOf(taskDetailVO.getBgId())));
                activeTaskVO.setDeptName(deptInfo.get(String.valueOf(taskDetailVO.getDeptId())));
                activeTaskVO.setCenterName(deptInfo.get(String.valueOf(taskDetailVO.getCenterId())));
                activeTaskVO.setCodeLang(ConvertUtil.convertCodeLang(taskDetailVO.getCodeLang(), metadataVoList));

                return activeTaskVO;
            }).collect(Collectors.toList());
        }

        deptTaskListRspVO.setActiveTaskList(activeTaskList);
        return deptTaskListRspVO;
    }

    @NotNull
    private Map<Long, String> getActiveTaskMap(List<TaskLogEntity> taskLogList) {
        Map<Long, String> activeTaskMap = Maps.newHashMap();
        if (CollectionUtils.isNotEmpty(taskLogList)) {
            for (TaskLogEntity taskLogEntity : taskLogList) {
                long taskId = taskLogEntity.getTaskId();


                String status = activeTaskMap.get(taskId);
                if (StringUtils.isNotEmpty(status)) {
                    continue;
                }

                String toolName = taskLogEntity.getToolName();
                int currStep = taskLogEntity.getCurrStep();
                int flag = taskLogEntity.getFlag();

                if (ComConstants.Tool.COVERITY.name().equals(toolName)) {
                    if (currStep == ComConstants.Step4Cov.DEFECT_SYNS.value()
                            && flag == ComConstants.StepFlag.SUCC.value()) {
                        activeTaskMap.put(taskId, "活跃");
                    }
                } else {
                    if (currStep == ComConstants.Step4MutliTool.COMMIT.value()
                            && flag == ComConstants.StepFlag.SUCC.value()) {
                        activeTaskMap.put(taskId, "活跃");
                    }
                }
            }
        }
        return activeTaskMap;
    }


    /**
     * 验证权限
     *
     * @param pipelineId
     */
    private void validatePermission(String pipelineId) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        String userName = request.getHeader(AUTH_HEADER_DEVOPS_USER_ID);

        Result<PipelineTaskVO> taskInfo =
                client.get(ServiceTaskRestResource.class).getPipelineTask(pipelineId, "");
        if (taskInfo.isNotOk() || null == taskInfo.getData()) {
            logger.error("get task detail info fail! pipeline id: {}", pipelineId);
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }

        PipelineTaskVO taskDetail = taskInfo.getData();
        String projectId = taskDetail.getProjectId();
        long taskId = taskDetail.getTaskId();
        if (StringUtils.isBlank(userName) || StringUtils.isBlank(projectId) || taskId == 0) {
            logger.error("insufficient param info! user: {}, taskId: {}, projectId: {}", userName, taskId, projectId);
            throw new UnauthorizedException("insufficient param info!");
        }

        String taskCreateFrom = authTaskService.getTaskCreateFrom(taskId);
        List<BkAuthExResourceActionModel> result;
        if (ComConstants.BsTaskCreateFrom.BS_PIPELINE.value().equals(taskCreateFrom)) {
            result = bkAuthExPermissionApi.validatePipelineBatchPermission(userName,
                    String.valueOf(taskId), projectId, Sets.newHashSet(PipelineAuthAction.VIEW.getActionName()));
        } else {
            result = bkAuthExPermissionApi.validateTaskBatchPermission(userName,
                    String.valueOf(taskId), projectId, Sets.newHashSet(CodeCCAuthAction.REPORT_VIEW.getActionName()));
        }
        if (CollectionUtils.isEmpty(result)) {
            logger.error("empty validate result: {}", userName);
            throw new UnauthorizedException("unauthorized user permission!");
        }

        for (BkAuthExResourceActionModel auth : result) {
            if (Objects.nonNull(auth) && auth.isPass()) {
                return;
            }
        }

        logger.error("validate permission fail! user: {}", userName);
        throw new UnauthorizedException("unauthorized user permission!");
    }

}
