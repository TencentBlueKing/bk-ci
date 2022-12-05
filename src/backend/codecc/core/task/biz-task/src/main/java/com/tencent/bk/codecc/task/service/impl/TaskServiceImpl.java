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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to
 * use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.codecc.task.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.api.ServiceCheckerSetRestResource;
import com.tencent.bk.codecc.defect.api.ServiceClusterStatisticRestReource;
import com.tencent.bk.codecc.defect.api.ServiceMetricsRestResource;
import com.tencent.bk.codecc.defect.api.ServiceTaskLogOverviewResource;
import com.tencent.bk.codecc.defect.api.ServiceTaskLogRestResource;
import com.tencent.bk.codecc.defect.api.ServiceToolBuildInfoResource;
import com.tencent.bk.codecc.defect.dto.ScanTaskTriggerDTO;
import com.tencent.bk.codecc.defect.vo.MetricsVO;
import com.tencent.bk.codecc.defect.vo.TaskLogOverviewVO;
import com.tencent.bk.codecc.defect.vo.TaskLogRepoInfoVO;
import com.tencent.bk.codecc.defect.vo.common.AuthorTransferVO;
import com.tencent.bk.codecc.quartz.pojo.JobExternalDto;
import com.tencent.bk.codecc.quartz.pojo.OperationType;
import com.tencent.bk.codecc.task.constant.TaskConstants;
import com.tencent.bk.codecc.task.constant.TaskMessageCode;
import com.tencent.bk.codecc.task.dao.CommonDao;
import com.tencent.bk.codecc.task.dao.mongorepository.BaseDataRepository;
import com.tencent.bk.codecc.task.dao.mongorepository.CustomProjRepository;
import com.tencent.bk.codecc.task.dao.mongorepository.GongfengPublicProjRepository;
import com.tencent.bk.codecc.task.dao.mongorepository.TaskRepository;
import com.tencent.bk.codecc.task.dao.mongorepository.TaskStatisticRepository;
import com.tencent.bk.codecc.task.dao.mongorepository.ToolMetaRepository;
import com.tencent.bk.codecc.task.dao.mongorepository.ToolRepository;
import com.tencent.bk.codecc.task.dao.mongotemplate.TaskDao;
import com.tencent.bk.codecc.task.enums.TaskSortType;
import com.tencent.bk.codecc.task.model.BaseDataEntity;
import com.tencent.bk.codecc.task.model.CustomProjEntity;
import com.tencent.bk.codecc.task.model.DisableTaskEntity;
import com.tencent.bk.codecc.task.model.GongfengPublicProjEntity;
import com.tencent.bk.codecc.task.model.NewDefectJudgeEntity;
import com.tencent.bk.codecc.task.model.NotifyCustomEntity;
import com.tencent.bk.codecc.task.model.TaskInfoEntity;
import com.tencent.bk.codecc.task.model.TaskStatisticEntity;
import com.tencent.bk.codecc.task.model.ToolConfigInfoEntity;
import com.tencent.bk.codecc.task.model.ToolMetaEntity;
import com.tencent.bk.codecc.task.pojo.GongfengPublicProjModel;
import com.tencent.bk.codecc.task.pojo.TofOrganizationInfo;
import com.tencent.bk.codecc.task.pojo.TofStaffInfo;
import com.tencent.bk.codecc.task.service.EmailNotifyService;
import com.tencent.bk.codecc.task.service.IAuthorTransferBizService;
import com.tencent.bk.codecc.task.service.PipelineService;
import com.tencent.bk.codecc.task.service.TaskService;
import com.tencent.bk.codecc.task.service.ToolService;
import com.tencent.bk.codecc.task.service.UserManageService;
import com.tencent.bk.codecc.task.tof.TofClientApi;
import com.tencent.bk.codecc.task.vo.BatchRegisterVO;
import com.tencent.bk.codecc.task.vo.CodeLibraryInfoVO;
import com.tencent.bk.codecc.task.vo.DevopsProjectOrgVO;
import com.tencent.bk.codecc.task.vo.MetadataVO;
import com.tencent.bk.codecc.task.vo.NotifyCustomVO;
import com.tencent.bk.codecc.task.vo.RepoInfoVO;
import com.tencent.bk.codecc.task.vo.TaskBaseVO;
import com.tencent.bk.codecc.task.vo.TaskCodeLibraryVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.bk.codecc.task.vo.TaskListReqVO;
import com.tencent.bk.codecc.task.vo.TaskListVO;
import com.tencent.bk.codecc.task.vo.TaskMemberVO;
import com.tencent.bk.codecc.task.vo.TaskOverviewVO;
import com.tencent.bk.codecc.task.vo.TaskOverviewVO.LastAnalysis;
import com.tencent.bk.codecc.task.vo.TaskOverviewVO.LastCluster;
import com.tencent.bk.codecc.task.vo.TaskOwnerAndMemberVO;
import com.tencent.bk.codecc.task.vo.TaskStatusVO;
import com.tencent.bk.codecc.task.vo.TaskUpdateVO;
import com.tencent.bk.codecc.task.vo.ToolConfigBaseVO;
import com.tencent.bk.codecc.task.vo.ToolConfigInfoVO;
import com.tencent.bk.codecc.task.vo.ToolConfigParamJsonVO;
import com.tencent.bk.codecc.task.vo.checkerset.ToolCheckerSetVO;
import com.tencent.bk.codecc.task.vo.pipeline.PipelineTaskVO;
import com.tencent.bk.codecc.task.vo.pipeline.PipelineToolParamVO;
import com.tencent.bk.codecc.task.vo.pipeline.PipelineToolVO;
import com.tencent.bk.codecc.task.vo.scanconfiguration.NewDefectJudgeVO;
import com.tencent.bk.codecc.task.vo.scanconfiguration.ScanConfigurationVO;
import com.tencent.bk.codecc.task.vo.scanconfiguration.TimeAnalysisConfigVO;
import com.tencent.bk.codecc.task.vo.tianyi.QueryMyTasksReqVO;
import com.tencent.bk.codecc.task.vo.tianyi.TaskInfoVO;
import com.tencent.devops.common.api.GetLastAnalysisResultsVO;
import com.tencent.devops.common.api.QueryTaskListReqVO;
import com.tencent.devops.common.api.ToolMetaBaseVO;
import com.tencent.devops.common.api.analysisresult.ToolLastAnalysisResultVO;
import com.tencent.devops.common.api.checkerset.CheckerSetVO;
import com.tencent.devops.common.api.clusterresult.BaseClusterResultVO;
import com.tencent.devops.common.api.clusterresult.CcnClusterResultVO;
import com.tencent.devops.common.api.clusterresult.DefectClusterResultVO;
import com.tencent.devops.common.api.clusterresult.DupcClusterResultVO;
import com.tencent.devops.common.api.clusterresult.SecurityClusterResultVO;
import com.tencent.devops.common.api.clusterresult.StandardClusterResultVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.exception.StreamException;
import com.tencent.devops.common.api.exception.UnauthorizedException;
import com.tencent.devops.common.api.pojo.GlobalMessage;
import com.tencent.devops.common.api.pojo.Page;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.auth.api.external.AuthExPermissionApi;
import com.tencent.devops.common.auth.api.external.AuthExRegisterApi;
import com.tencent.devops.common.auth.api.external.AuthTaskService;
import com.tencent.devops.common.auth.api.pojo.external.AuthRole;
import com.tencent.devops.common.auth.api.pojo.external.CodeCCAuthAction;
import com.tencent.devops.common.auth.api.pojo.external.PipelineAuthAction;
import com.tencent.devops.common.auth.api.util.PermissionUtil;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.ScanStatus;
import com.tencent.devops.common.constant.ComConstants.ToolType;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.service.ToolMetaCacheService;
import com.tencent.devops.common.service.utils.GlobalMessageUtil;
import com.tencent.devops.common.service.utils.PageableUtils;
import com.tencent.devops.common.util.DateTimeUtils;
import com.tencent.devops.common.util.JsonUtil;
import com.tencent.devops.common.util.List2StrUtil;
import com.tencent.devops.common.util.ListSortUtil;
import com.tencent.devops.common.util.OkhttpUtils;
import com.tencent.devops.common.web.aop.annotation.OperationHistory;
import lombok.extern.slf4j.Slf4j;
import net.sf.json.JSONObject;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import com.tencent.devops.common.util.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_PROJECT_ID;
import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_TASK_ID;
import static com.tencent.devops.common.constant.ComConstants.BsTaskCreateFrom;
import static com.tencent.devops.common.constant.ComConstants.DISABLE_ACTION;
import static com.tencent.devops.common.constant.ComConstants.ENABLE_ACTION;
import static com.tencent.devops.common.constant.ComConstants.FOLLOW_STATUS;
import static com.tencent.devops.common.constant.ComConstants.FUNC_CODE_REPOSITORY;
import static com.tencent.devops.common.constant.ComConstants.FUNC_SCAN_SCHEDULE;
import static com.tencent.devops.common.constant.ComConstants.FUNC_TASK_INFO;
import static com.tencent.devops.common.constant.ComConstants.FUNC_TASK_SWITCH;
import static com.tencent.devops.common.constant.ComConstants.FUNC_TRIGGER_ANALYSIS;
import static com.tencent.devops.common.constant.ComConstants.MODIFY_INFO;
import static com.tencent.devops.common.constant.ComConstants.Status;
import static com.tencent.devops.common.constant.ComConstants.Step4MutliTool;
import static com.tencent.devops.common.constant.ComConstants.StepStatus;
import static com.tencent.devops.common.constant.ComConstants.TRIGGER_ANALYSIS;
import static com.tencent.devops.common.constant.ComConstants.Tool;
import static com.tencent.devops.common.constant.RedisKeyConstants.GLOBAL_TOOL_PARAMS_LABEL_NAME;
import static com.tencent.devops.common.constant.RedisKeyConstants.GLOBAL_TOOL_PARAMS_TIPS;
import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_EXPIRED_TASK_STATUS;
import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_EXTERNAL_JOB;
import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_SCORING_OPENSOURCE;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_EXPIRED_TASK_STATUS;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_SCORING_OPENSOURCE;

/**
 * 任务服务实现类
 *
 * @version V1.0
 * @date 2019/4/23
 */
@Service
@Slf4j
public class TaskServiceImpl implements TaskService {
    @Autowired
    private AuthExPermissionApi authExPermissionApi;

    @Autowired
    private AuthExRegisterApi authExRegisterApi;

    @Autowired
    private PipelineService pipelineService;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ToolRepository toolRepository;

    @Autowired
    private GongfengPublicProjRepository gongfengPublicProjRepository;

    @Autowired
    private ToolMetaCacheService toolMetaCache;

    @Autowired
    private EmailNotifyService emailNotifyService;

    @Autowired
    private IAuthorTransferBizService authorTransferBizService;

    @Autowired
    private ToolService toolService;

    @Autowired
    private CommonDao commonDao;

    @Autowired
    private TaskDao taskDao;

    @Autowired
    private Client client;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GlobalMessageUtil globalMessageUtil;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private UserManageService userManageService;

    @Autowired
    private TofClientApi tofClientApi;

    @Autowired
    private ToolMetaRepository toolMetaRepository;

    @Autowired
    private TaskService taskService;

    //    @Autowired
    //    private GongfengTriggerService gongfengTriggerService;
    @Autowired
    private BaseDataRepository baseDataRepository;

    @Autowired
    private CustomProjRepository customProjRepository;

    @Autowired
    private TaskStatisticRepository taskStatisticRepository;

    @Autowired
    private AuthTaskService authTaskService;

    @Value("${git.path:#{null}}")
    private String gitCodePath;

    @Value("${codecc.privatetoken:#{null}}")
    private String gitPrivateToken = null;

    private LoadingCache<String, BaseDataEntity> toolTypeBaseDataCache = CacheBuilder.newBuilder()
            .refreshAfterWrite(2, TimeUnit.HOURS)
            .build(new CacheLoader<String, BaseDataEntity>() {
                @Override
                public BaseDataEntity load(String toolName) {
                    if (StringUtils.isBlank(toolName)) {
                        return null;
                    }

                    ToolMetaEntity toolMeta = toolMetaRepository.findFirstByName(toolName);
                    String toolType = toolMeta.getType();
                    return baseDataRepository.findFirstByParamCode(toolType);
                }
            });

    @Override
    public TaskListVO getTaskList(String projectId, String user, TaskSortType taskSortType,
                                  TaskListReqVO taskListReqVO) {

        Set<TaskInfoEntity> resultTasks = getQualifiedTaskList(projectId, user, null,
                null != taskListReqVO ? taskListReqVO.getTaskSource() : null);

        final String toolIdsOrder = commonDao.getToolOrder();

        List<TaskDetailVO> taskDetailVOList = resultTasks.stream().
                filter(taskInfoEntity ->
                        StringUtils.isNotEmpty(taskInfoEntity.getToolNames()) &&
                                //流水线停用任务不展示
                                !(taskInfoEntity.getStatus().equals(TaskConstants.TaskStatus.DISABLE.value()) &&
                                        BsTaskCreateFrom.BS_PIPELINE.value().equalsIgnoreCase(taskInfoEntity.getCreateFrom()))).
                map(taskInfoEntity ->
                {
                    TaskDetailVO taskDetailVO = new TaskDetailVO();
                    taskDetailVO.setTaskId(taskInfoEntity.getTaskId());
                    taskDetailVO.setToolNames(taskInfoEntity.getToolNames());
                    return taskDetailVO;
                }).
                collect(Collectors.toList());

        Result<Map<String, List<ToolLastAnalysisResultVO>>> taskAndTaskLogResult =
                client.get(ServiceTaskLogRestResource.class)
                        .getBatchTaskLatestTaskLog(taskDetailVOList);
        Map<String, List<ToolLastAnalysisResultVO>> taskAndTaskLogMap;
        if (taskAndTaskLogResult.isOk() &&
                MapUtils.isNotEmpty(taskAndTaskLogResult.getData())) {
            taskAndTaskLogMap = taskAndTaskLogResult.getData();
        } else {
            log.error("get batch task log fail or task log is empty!");
            taskAndTaskLogMap = new HashMap<>();
        }


        //对工具清单进行处理
        List<TaskDetailVO> taskDetailVOS = resultTasks.stream().
                filter(taskInfoEntity ->
                        //流水线停用任务不展示
                        !(taskInfoEntity.getStatus().equals(TaskConstants.TaskStatus.DISABLE.value()) &&
                                BsTaskCreateFrom.BS_PIPELINE.value().equalsIgnoreCase(taskInfoEntity.getCreateFrom()))).
                map(taskInfoEntity ->
                {
                    TaskDetailVO taskDetailVO = new TaskDetailVO();
                    BeanUtils.copyProperties(taskInfoEntity, taskDetailVO, "toolConfigInfoList");
                    //设置置顶标识
                    Set<String> topUsers = taskInfoEntity.getTopUser();
                    if (CollectionUtils.isNotEmpty(topUsers) && topUsers.contains(user)) {
                        taskDetailVO.setTopFlag(1);
                    } else {
                        taskDetailVO.setTopFlag(-1);
                    }
                    List<ToolConfigInfoEntity> toolConfigInfoEntityList = taskInfoEntity.getToolConfigInfoList();
                    //获取分析完成时间
                    List<ToolLastAnalysisResultVO> taskLogGroupVOs = new ArrayList<>();
                    String toolNames = taskInfoEntity.getToolNames();
                    if (StringUtils.isNotEmpty(toolNames)) {
                        if (MapUtils.isNotEmpty(taskAndTaskLogMap)) {
                            taskLogGroupVOs = taskAndTaskLogMap.get(String.valueOf(taskInfoEntity.getTaskId()));
                            if (null == taskLogGroupVOs) {
                                taskLogGroupVOs = new ArrayList<>();
                            }
                        }
                    }

                    if (CollectionUtils.isNotEmpty(toolConfigInfoEntityList)) {
                        List<ToolConfigInfoVO> toolConfigInfoVOList = new ArrayList<>();
                        boolean isAllSuspended = true;
                        Long minStartTime = Long.MAX_VALUE;
                        Boolean processFlag = false;
                        Integer totalFinishStep = 0;
                        Integer totalStep = 0;
                        for (ToolConfigInfoEntity toolConfigInfoEntity : toolConfigInfoEntityList) {

                            if (null == toolConfigInfoEntity || StringUtils.isEmpty(toolConfigInfoEntity.getToolName())) {
                                continue;
                            }

                            // 获取工具展示名称
                            //                            String displayName = toolMetaCache.getToolDisplayName
                            //                            (toolConfigInfoEntity.getToolName());

                            // 获取工具展示名称
                            ToolMetaBaseVO toolMetaBaseVO =
                                    toolMetaCache.getToolBaseMetaCache(toolConfigInfoEntity.getToolName());


                            if (toolConfigInfoEntity.getFollowStatus() !=
                                    FOLLOW_STATUS.WITHDRAW.value()) {

                                //更新工具显示状态
                                //如果有失败的工具，则显示失败的状态
                                if (!processFlag) {
                                    processFlag = taskDetailDisplayInfo(toolConfigInfoEntity, taskDetailVO,
                                            toolMetaBaseVO.getDisplayName());
                                }
                                //添加进度条
                                totalFinishStep += toolConfigInfoEntity.getCurStep();
                                switch (toolMetaBaseVO.getPattern()) {
                                    case "LINT":
                                        totalStep += 5;
                                        break;
                                    case "CCN":
                                        totalStep += 5;
                                        break;
                                    case "DUPC":
                                        totalStep += 5;
                                        break;
                                    default:
                                        totalStep += 6;
                                        break;
                                }

                            }


                            ToolConfigInfoVO toolConfigInfoVO = new ToolConfigInfoVO();
                            BeanUtils.copyProperties(toolConfigInfoEntity, toolConfigInfoVO);

                            //设置分析完成时间
                            for (ToolLastAnalysisResultVO toolLastAnalysisResultVO : taskLogGroupVOs) {
                                if (toolLastAnalysisResultVO.getToolName().equalsIgnoreCase(toolConfigInfoVO.getToolName())) {
                                    toolConfigInfoVO.setEndTime(toolLastAnalysisResultVO.getEndTime());
                                    toolConfigInfoVO.setStartTime(toolLastAnalysisResultVO.getStartTime());
                                }
                            }
                            minStartTime = Math.min(minStartTime, toolConfigInfoVO.getStartTime());


                            if (StringUtils.isNotEmpty(toolMetaBaseVO.getDisplayName())) {
                                toolConfigInfoVO.setDisplayName(toolMetaBaseVO.getDisplayName());
                            }

                            if (toolConfigInfoVO.getFollowStatus() !=
                                    FOLLOW_STATUS.WITHDRAW.value()) {
                                isAllSuspended = false;
                            }
                            if (toolConfigInfoEntity.getCheckerSet() != null) {
                                ToolCheckerSetVO checkerSetVO = new ToolCheckerSetVO();
                                BeanUtils.copyProperties(toolConfigInfoEntity.getCheckerSet(), checkerSetVO);
                                toolConfigInfoVO.setCheckerSet(checkerSetVO);
                            }
                            toolConfigInfoVOList.add(toolConfigInfoVO);
                        }
                        if (isAllSuspended) {
                            log.info("all tool is suspended! task id: {}", taskInfoEntity.getTaskId());
                            if (CollectionUtils.isNotEmpty(toolConfigInfoVOList)) {
                                toolConfigInfoVOList.get(0)
                                        .setFollowStatus(FOLLOW_STATUS.EXPERIENCE.value());
                            }
                        }
                        if (totalStep == 0) {
                            taskDetailVO.setDisplayProgress(0);
                        } else {
                            if (totalFinishStep > totalStep) {
                                totalFinishStep = totalStep;
                            }
                            taskDetailVO.setDisplayProgress(totalFinishStep * 100 / totalStep);
                        }
                        if (null == taskDetailVO.getDisplayStepStatus()) {
                            taskDetailVO.setDisplayStepStatus(StepStatus.SUCC.value());
                        }
                        if (minStartTime < Long.MAX_VALUE) {
                            taskDetailVO.setMinStartTime(minStartTime);
                        } else {
                            taskDetailVO.setMinStartTime(0L);
                        }
                        log.info("handle tool list finish! task id: {}", taskInfoEntity.getTaskId());
                        taskDetailVO.setToolConfigInfoList(toolConfigInfoVOList);
                    } else {
                        log.info("tool list is empty! task id: {}", taskInfoEntity.getTaskId());
                        taskDetailVO.setToolConfigInfoList(new ArrayList<>());
                        taskDetailVO.setMinStartTime(0L);
                    }

                    List<ToolConfigInfoVO> toolConfigInfoVOs = new ArrayList<>();
                    //重置工具顺序，并且对工具清单顺序也进行重排
                    taskDetailVO.setToolNames(resetToolOrderByType(taskDetailVO.getToolNames(), toolIdsOrder,
                            taskDetailVO.getToolConfigInfoList(),
                            toolConfigInfoVOs));
                    taskDetailVO.setToolConfigInfoList(toolConfigInfoVOs);
                    return taskDetailVO;
                }).
                collect(Collectors.toList());
        //根据任务状态过滤
        if (null != taskListReqVO.getTaskStatus()) {
            taskDetailVOS = taskDetailVOS.stream().filter(taskDetailVO -> {
                Boolean selected = false;
                switch (taskListReqVO.getTaskStatus()) {
                    case SUCCESS:
                        if (null != taskDetailVO.getDisplayStepStatus() && null != taskDetailVO.getDisplayStep() &&
                                taskDetailVO.getDisplayStepStatus() == StepStatus.SUCC.value() &&
                                taskDetailVO.getDisplayStep() >= Step4MutliTool.COMPLETE.value()) {
                            selected = true;
                        }
                        break;
                    case FAIL:
                        if (null != taskDetailVO.getDisplayStepStatus() &&
                                taskDetailVO.getDisplayStepStatus() == StepStatus.FAIL.value()) {
                            selected = true;
                        }
                        break;
                    case WAITING:
                        if (null == taskDetailVO.getDisplayStepStatus() ||
                                (null != taskDetailVO.getDisplayStepStatus() &&
                                        taskDetailVO.getDisplayStepStatus() == StepStatus.SUCC.value() &&
                                        (null == taskDetailVO.getDisplayStep() ||
                                                taskDetailVO.getDisplayStep() == StepStatus.SUCC.value()))) {
                            selected = true;
                        }
                        break;
                    case ANALYSING:
                        if (null != taskDetailVO.getDisplayStepStatus() && null != taskDetailVO.getDisplayStep() &&
                                taskDetailVO.getDisplayStepStatus() != StepStatus.FAIL.value() &&
                                taskDetailVO.getDisplayStep() > Step4MutliTool.READY.value() &&
                                taskDetailVO.getDisplayStep() < Step4MutliTool.COMPLETE.value()) {
                            selected = true;
                        }
                        break;
                    case DISABLED:
                        if (Status.DISABLE.value() == taskDetailVO.getStatus()) {
                            selected = true;
                        }
                        break;
                    default:
                        break;
                }
                return selected;
            }).collect(Collectors.toList());
        }

        taskDetailVOS.forEach(taskDetailVO -> taskDetailVO.setCodeLibraryInfo(
                getRepoInfo(taskDetailVO.getTaskId())));
        return sortByDate(taskDetailVOS, taskSortType);
    }


    @Override
    public TaskListVO getTaskBaseList(String projectId, String user) {
        Set<TaskInfoEntity> resultSet = getQualifiedTaskList(projectId, user, null, null);
        if (CollectionUtils.isNotEmpty(resultSet)) {
            List<TaskDetailVO> taskBaseVOList = resultSet.stream().map(taskInfoEntity ->
            {
                TaskDetailVO taskDetailVO = new TaskDetailVO();
                taskDetailVO.setTaskId(taskInfoEntity.getTaskId());
                taskDetailVO.setEntityId(taskInfoEntity.getEntityId());
                taskDetailVO.setNameCn(taskInfoEntity.getNameCn());
                taskDetailVO.setNameEn(taskInfoEntity.getNameEn());
                taskDetailVO.setStatus(taskInfoEntity.getStatus());
                taskDetailVO.setToolNames(taskInfoEntity.getToolNames());
                taskDetailVO.setCreatedDate(taskInfoEntity.getCreatedDate());
                taskDetailVO.setDisableTime(taskInfoEntity.getDisableTime());
                //设置置顶标识
                taskDetailVO.setTopFlag(-1);
                if (CollectionUtils.isNotEmpty(taskInfoEntity.getTopUser())) {
                    if (taskInfoEntity.getTopUser().contains(user)) {
                        taskDetailVO.setTopFlag(1);
                    }
                }
                return taskDetailVO;
            }).
                    collect(Collectors.toList());
            List<TaskDetailVO> enableTaskList = taskBaseVOList.stream()
                    .filter(taskDetailVO ->
                            !TaskConstants.TaskStatus.DISABLE.value().equals(taskDetailVO.getStatus()))
                    .sorted((o1, o2) -> o2.getTopFlag() - o1.getTopFlag() == 0
                            ? o2.getCreatedDate().compareTo(o1.getCreatedDate()) : o2.getTopFlag() - o1.getTopFlag())
                    .collect(Collectors.toList());
            List<TaskDetailVO> disableTaskList = taskBaseVOList.stream()
                    .filter(taskDetailVO ->
                            TaskConstants.TaskStatus.DISABLE.value().equals(taskDetailVO.getStatus()))
                    .sorted((o1, o2) -> o2.getTopFlag() - o1.getTopFlag() == 0 ?
                            (StringUtils.isEmpty(o2.getDisableTime()) ? Long.valueOf(0) :
                                    Long.valueOf(o2.getDisableTime()))
                                    .compareTo(StringUtils.isEmpty(o1.getDisableTime()) ? Long.valueOf(0) :
                                            Long.valueOf(o1.getDisableTime())) :
                            o2.getTopFlag() - o1.getTopFlag())
                    .collect(Collectors.toList());
            return new TaskListVO(enableTaskList, disableTaskList);


        }
        return null;
    }


    /**
     * 查询符合条件的任务清单
     *
     * @param projectId
     * @param user
     * @return
     */
    private Set<TaskInfoEntity> getQualifiedTaskList(String projectId, String user,
                                                     Integer taskStatus, String taskSource) {
        log.info("begin to get task list! project:{}, user:{}, taskStatus:{}, taskSource:{}",
                projectId, user, taskStatus, taskSource);

        Set<String> createFromSet;
        if (StringUtils.isNotEmpty(taskSource)) {
            createFromSet = Sets.newHashSet(taskSource);
        } else {
            createFromSet = Sets.newHashSet(BsTaskCreateFrom.BS_PIPELINE.value(), BsTaskCreateFrom.BS_CODECC.value());
        }
        Set<TaskInfoEntity> taskInfoEntities = taskRepository.findByProjectIdAndCreateFromIn(projectId, createFromSet);
        // 查询用户有权限的CodeCC任务
        Set<String> tasks = authExPermissionApi.queryTaskListForUser(user, projectId,
                Sets.newHashSet(CodeCCAuthAction.REPORT_VIEW.getActionName()));

        // 查询用户有权限的流水线
        Set<String> pipelines = authExPermissionApi.queryPipelineListForUser(user, projectId,
                Sets.newHashSet(PipelineAuthAction.VIEW.getActionName()));

        //查询任务清单速度优化
        Set<TaskInfoEntity> resultTasks = taskInfoEntities.stream().filter(taskInfoEntity ->
                ((CollectionUtils.isNotEmpty(taskInfoEntity.getTaskOwner())
                        && taskInfoEntity.getTaskOwner().contains(user)
                        && taskInfoEntity.getStatus().equals(TaskConstants.TaskStatus.DISABLE.value())
                        && !(BsTaskCreateFrom.BS_PIPELINE.value().equalsIgnoreCase(taskInfoEntity.getCreateFrom())))
                        || (CollectionUtils.isNotEmpty(tasks)
                            && tasks.contains(String.valueOf(taskInfoEntity.getTaskId())))
                        || (CollectionUtils.isNotEmpty(pipelines) && pipelines.contains(taskInfoEntity.getPipelineId()))
                        //加上任务灰度池的查询场景，系统管理员有权限查询灰度池项目的任务清单
                        || (taskInfoEntity.getProjectId().startsWith(ComConstants.GRAY_PROJECT_PREFIX)
                        && authExPermissionApi.isAdminMember(user)))
                        //如果有过滤条件，要加过滤
                        && (taskInfoEntity.getStatus().equals(taskStatus) || null == taskStatus)
        ).collect(Collectors.toSet());

        log.info("task mongorepository finish, project:{}, user:{}, taskStatus:{}, taskSource:{}, list length: {}",
                projectId, user, taskStatus, taskSource, resultTasks.size());
        return resultTasks;
    }


    @Override
    public TaskBaseVO getTaskInfo() {
        HttpServletRequest request =
                ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String taskId = request.getHeader(AUTH_HEADER_DEVOPS_TASK_ID);
        String projectId = request.getHeader(AUTH_HEADER_DEVOPS_PROJECT_ID);
        log.info("getTaskInfo: {}", taskId);
        if (!StringUtils.isNumeric(taskId)) {
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{String.valueOf(taskId)},
                    null);
        }
        TaskInfoEntity taskEntity = taskRepository.findFirstByTaskId(Long.valueOf(taskId));

        if (taskEntity == null) {
            log.error("can not find task by taskId: {}", taskId);
            throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS, new String[]{taskId}, null);
        }

        TaskBaseVO taskBaseVO = new TaskBaseVO();
        BeanUtils.copyProperties(taskEntity, taskBaseVO);

        // 加入新告警判定配置
        if (taskEntity.getNewDefectJudge() != null) {
            NewDefectJudgeVO newDefectJudge = new NewDefectJudgeVO();
            BeanUtils.copyProperties(taskEntity.getNewDefectJudge(), newDefectJudge);
            taskBaseVO.setNewDefectJudge(newDefectJudge);
        }

        //添加个性化报告信息
        NotifyCustomVO notifyCustomVO = new NotifyCustomVO();
        NotifyCustomEntity notifyCustomEntity = taskEntity.getNotifyCustomInfo();
        if (null != notifyCustomEntity) {
            BeanUtils.copyProperties(notifyCustomEntity, notifyCustomVO);
        }
        taskBaseVO.setNotifyCustomInfo(notifyCustomVO);

        // 给工具分类及排序，并加入规则集
        sortedToolList(taskBaseVO, taskEntity.getToolConfigInfoList());

        //获取规则和规则集数量
        Result<TaskBaseVO> checkerCountVO =
                client.get(ServiceCheckerSetRestResource.class).getCheckerAndCheckerSetCount(Long.valueOf(taskId),
                        projectId);
        if (checkerCountVO.isOk() && null != checkerCountVO.getData()) {
            taskBaseVO.setCheckerSetName(checkerCountVO.getData().getCheckerSetName());
            taskBaseVO.setCheckerCount(checkerCountVO.getData().getCheckerCount());
        }

        taskBaseVO.setCodeLibraryInfo(getRepoInfo(Long.valueOf(taskId)));

        //如果是灰度项目，则需要显示为api创建
        if (taskEntity.getProjectId().startsWith(ComConstants.GRAY_PROJECT_PREFIX)) {
            taskBaseVO.setCreateFrom(BsTaskCreateFrom.API_TRIGGER.value());
        }
        return taskBaseVO;
    }

    @Override
    public TaskDetailVO getTaskInfoById(Long taskId) {
        TaskInfoEntity taskEntity = taskRepository.findFirstByTaskId(taskId);
        if (taskEntity == null) {
            log.error("can not find task by taskId: {}", taskId);
            throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS, new String[]{String.valueOf(taskId)}, null);
        }

        try {
            String taskInfoStr = objectMapper.writeValueAsString(taskEntity);
            // 注入任务类型和创建来源

            TaskDetailVO taskDetailVO = objectMapper.readValue(taskInfoStr,
                    new TypeReference<TaskDetailVO>() {
                    });
            setTaskCreateInfo(taskDetailVO);
            return taskDetailVO;
        } catch (IOException e) {
            String message = "string conversion TaskDetailVO error";
            log.error(message);
            throw new StreamException(message);
        }
    }

    @Override
    public TaskDetailVO getTaskInfoWithoutToolsByTaskId(Long taskId) {
        TaskInfoEntity taskEntity = taskRepository.findTaskInfoWithoutToolsFirstByTaskId(taskId);
        if (taskEntity == null) {
            log.error("can not find task by taskId: {}", taskId);
            throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS, new String[]{String.valueOf(taskId)}, null);
        }

        TaskDetailVO taskDetailVO = new TaskDetailVO();
        BeanUtils.copyProperties(taskEntity, taskDetailVO);

        if (taskEntity.getNewDefectJudge() != null) {
            NewDefectJudgeVO newDefectJudgeVO = new NewDefectJudgeVO();
            BeanUtils.copyProperties(taskEntity.getNewDefectJudge(), newDefectJudgeVO);
            taskDetailVO.setNewDefectJudge(newDefectJudgeVO);
        }

        return taskDetailVO;
    }

    @Override
    public TaskDetailVO getTaskInfoByStreamName(String streamName) {
        TaskInfoEntity taskEntity = taskRepository.findFirstByNameEn(streamName);

        if (taskEntity == null) {
            log.error("can not find task by streamName: {}", streamName);
            throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS, new String[]{streamName}, null);
        }

        TaskDetailVO taskDetailVO = new TaskDetailVO();
        BeanUtils.copyProperties(taskEntity, taskDetailVO);

        // 加入工具列表
        List<ToolConfigInfoEntity> toolEntityList = taskEntity.getToolConfigInfoList();
        if (CollectionUtils.isNotEmpty(toolEntityList)) {
            Set<String> toolSet = new HashSet<>();
            taskDetailVO.setToolSet(toolSet);

            for (ToolConfigInfoEntity toolEntity : toolEntityList) {
                if (TaskConstants.FOLLOW_STATUS.WITHDRAW.value() != toolEntity.getFollowStatus()) {
                    toolSet.add(toolEntity.getToolName());
                }
            }
        }


        taskDetailVO.setToolConfigInfoList(toolEntityList.stream().map(toolConfigInfoEntity -> {
            ToolConfigInfoVO toolConfigInfoVO = new ToolConfigInfoVO();
            BeanUtils.copyProperties(toolConfigInfoEntity, toolConfigInfoVO, "checkerSet");
            return toolConfigInfoVO;
        }).collect(Collectors.toList()));

        // 加入通知定制配置
        if (taskEntity.getNotifyCustomInfo() != null) {
            NotifyCustomVO notifyCustomVO = new NotifyCustomVO();
            BeanUtils.copyProperties(taskEntity.getNotifyCustomInfo(), notifyCustomVO);
            taskDetailVO.setNotifyCustomInfo(notifyCustomVO);
        }

        // 加入新、历史告警判定
        if (taskEntity.getNewDefectJudge() != null) {
            NewDefectJudgeVO newDefectJudgeVO = new NewDefectJudgeVO();
            BeanUtils.copyProperties(taskEntity.getNewDefectJudge(), newDefectJudgeVO);
            taskDetailVO.setNewDefectJudge(newDefectJudgeVO);
        }

        // 是否回写工蜂
        if (taskEntity.getMrCommentEnable() != null) {
            taskDetailVO.setMrCommentEnable(taskEntity.getMrCommentEnable());
        }

        return taskDetailVO;
    }


    private Boolean taskDetailDisplayInfo(ToolConfigInfoEntity toolConfigInfoEntity, TaskDetailVO taskDetailVO,
                                          String displayName) {
        Integer displayStepStatus = 0;
        //检测到有任务运行中（非成功状态）
        Boolean processFlag = false;
        //更新工具显示状态
        //如果有失败的工具，则显示失败的状态
        if (toolConfigInfoEntity.getStepStatus() == StepStatus.FAIL.value()) {
            displayStepStatus = StepStatus.FAIL.value();
            taskDetailVO.setDisplayStepStatus(displayStepStatus);
            taskDetailVO.setDisplayToolName(toolConfigInfoEntity.getToolName());
            taskDetailVO.setDisplayStep(toolConfigInfoEntity.getCurStep());
            taskDetailVO.setDisplayName(displayName);
            processFlag = true;
        }
        //如果没找到失败的工具，有分析中的工具，则显示分析中
        else if (toolConfigInfoEntity.getStepStatus() == StepStatus.SUCC.value() &&
                toolConfigInfoEntity.getCurStep() < Step4MutliTool.COMPLETE.value() &&
                toolConfigInfoEntity.getCurStep() > Step4MutliTool.READY.value() &&
                displayStepStatus != StepStatus.FAIL.value()) {
            taskDetailVO.setDisplayToolName(toolConfigInfoEntity.getToolName());
            taskDetailVO.setDisplayStep(toolConfigInfoEntity.getCurStep());
            taskDetailVO.setDisplayName(displayName);
            processFlag = true;
        }
        //如果没找到失败的工具，有准备的工具，则显示准备
        else if (toolConfigInfoEntity.getStepStatus() == StepStatus.SUCC.value() &&
                toolConfigInfoEntity.getCurStep() == Step4MutliTool.READY.value() &&
                displayStepStatus != StepStatus.FAIL.value()) {
            taskDetailVO.setDisplayToolName(toolConfigInfoEntity.getToolName());
            taskDetailVO.setDisplayStep(toolConfigInfoEntity.getCurStep());
            taskDetailVO.setDisplayName(displayName);
            processFlag = true;
        }
        //如果还没找到其他状态，则显示成功
        else if (toolConfigInfoEntity.getStepStatus() == StepStatus.SUCC.value() &&
                toolConfigInfoEntity.getCurStep() >= Step4MutliTool.COMPLETE.value() &&
                StringUtils.isBlank(taskDetailVO.getDisplayToolName())) {
            taskDetailVO.setDisplayToolName(toolConfigInfoEntity.getToolName());
            taskDetailVO.setDisplayStep(toolConfigInfoEntity.getCurStep());
            taskDetailVO.setDisplayName(displayName);
        }
        return processFlag;

    }

    /**
     * 获取任务接入的工具列表
     *
     * @param taskId
     * @return
     */
    @Override
    public TaskBaseVO getTaskToolList(long taskId) {
        List<ToolConfigInfoEntity> toolEntityList = toolRepository.findByTaskId(Long.valueOf(taskId));

        TaskBaseVO taskBaseVO = new TaskBaseVO();

        // 给工具分类及排序
        sortedToolList(taskBaseVO, toolEntityList);

        return taskBaseVO;
    }

    /**
     * 修改任务
     *
     * @param taskUpdateVO
     * @param userName
     * @return
     */
    @Override
    @OperationHistory(funcId = FUNC_TASK_INFO, operType = MODIFY_INFO)
    public Boolean updateTask(TaskUpdateVO taskUpdateVO, Long taskId, String userName) {
        // 检查参数
        if (!checkParam(taskUpdateVO)) {
            return false;
        }

        // 任务是否注册过
        TaskInfoEntity taskInfoEntity = taskRepository.findFirstByTaskId(taskId);
        if (Objects.isNull(taskInfoEntity)) {
            log.error("can not find task info");
            throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS, new String[]{String.valueOf(taskId)}, null);
        }

        // 修改任务信息
        taskDao.updateTask(taskUpdateVO.getTaskId(), taskUpdateVO.getCodeLang(), taskUpdateVO.getNameCn(),
                taskUpdateVO.getTaskOwner(),
                taskUpdateVO.getTaskMember(), taskUpdateVO.getDisableTime(), taskUpdateVO.getStatus(),
                userName);

        //根据语言解绑规则集
        if (!taskUpdateVO.getCodeLang().equals(taskInfoEntity.getCodeLang())) {
            log.info("update the code lang, and set full scan: {}, {} -> {}", taskId, taskInfoEntity.getCodeLang(),
                    taskUpdateVO.getCodeLang());
            client.get(ServiceCheckerSetRestResource.class).updateCheckerSetAndTaskRelation(taskId,
                    taskUpdateVO.getCodeLang(), userName);
        }

        return true;
    }

    /**
     * 修改任务基本信息 - 内部服务间调用
     *
     * @param taskUpdateVO
     * @param userName
     * @return
     */
    @Override
    public Boolean updateTaskByServer(TaskUpdateVO taskUpdateVO, String userName) {
        return taskDao.updateTask(taskUpdateVO.getTaskId(), taskUpdateVO.getCodeLang(), taskUpdateVO.getNameCn(),
                taskUpdateVO.getTaskOwner(),
                taskUpdateVO.getTaskMember(), taskUpdateVO.getDisableTime(), taskUpdateVO.getStatus(),
                userName);
    }

    @Override
    public TaskOverviewVO getTaskOverview(Long taskId, String buildNum) {
        TaskInfoEntity taskEntity = taskRepository.findToolListFirstByTaskId(taskId);
        if (taskEntity == null) {
            log.error("can not find task by taskId: {}", taskId);
            throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS, new String[]{String.valueOf(taskId)}, null);
        }

        TaskOverviewVO taskOverviewVO = new TaskOverviewVO();
        taskOverviewVO.setTaskId(taskId);
        List<LastAnalysis> toolLastAnalysisList = new ArrayList<>();
        Map<String, LastAnalysis> toolLastAnalysisMap = new HashMap<>();

        List<ToolLastAnalysisResultVO> lastAnalysisResultVOs;

        if (NumberUtils.isNumber(buildNum)) {
            GetLastAnalysisResultsVO getLastAnalysisResultsVO = new GetLastAnalysisResultsVO();
            getLastAnalysisResultsVO.setTaskId(taskId);
            getLastAnalysisResultsVO.setBuildNum(buildNum);

            // 调用defect模块的接口获取工具的某一次分析结果
            Result<List<ToolLastAnalysisResultVO>> result =
                    client.get(ServiceTaskLogRestResource.class).getAnalysisResults(getLastAnalysisResultsVO);
            if (result.isNotOk() || null == result.getData()) {
                log.error("get analysis results fail! taskId is: {}, msg: {}", taskId, result.getMessage());
                throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
            }

            lastAnalysisResultVOs = result.getData();

            if (CollectionUtils.isNotEmpty(lastAnalysisResultVOs)) {
                String buildId = "";
                for (ToolLastAnalysisResultVO resultVO : lastAnalysisResultVOs) {
                    int curStep = resultVO.getCurrStep();
                    if (Arrays.asList(Tool.COVERITY.name(), Tool.KLOCWORK.name()).contains(resultVO.getToolName())) {
                        if (curStep == ComConstants.Step4Cov.DEFECT_SYNS.value()) {
                            curStep = ComConstants.Step4Cov.COMPLETE.value();
                        }
                    } else {
                        if (curStep == Step4MutliTool.COMMIT.value()) {
                            curStep = Step4MutliTool.COMPLETE.value();
                        }
                    }

                    int stepStatus = resultVO.getFlag() == ComConstants.StepFlag.FAIL.value()
                            ? StepStatus.FAIL.value() : StepStatus.SUCC.value();

                    LastAnalysis lastAnalysis = new LastAnalysis();
                    String toolName = resultVO.getToolName();
                    lastAnalysis.setToolName(toolName);
                    lastAnalysis.setCurStep(curStep);
                    lastAnalysis.setStepStatus(stepStatus);
                    toolLastAnalysisMap.put(toolName, lastAnalysis);
                    toolLastAnalysisList.add(lastAnalysis);
                    buildId = resultVO.getBuildId();
                }
                // 获取度量信息
                Result<MetricsVO> metricsRes = client.get(ServiceMetricsRestResource.class).getMetrics(taskId, buildId);
                if (metricsRes.isOk() && metricsRes.getData() != null) {
                    try {
                        org.apache.commons.beanutils.BeanUtils.copyProperties(taskOverviewVO, metricsRes.getData());
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            List<ToolConfigInfoEntity> toolConfigInfoList = taskEntity.getToolConfigInfoList();

            if (CollectionUtils.isEmpty(toolConfigInfoList)) {
                return taskOverviewVO;
            }

            for (ToolConfigInfoEntity tool : toolConfigInfoList) {
                if (tool == null) {
                    continue;
                }
                int followStatus = tool.getFollowStatus();
                if (followStatus != TaskConstants.FOLLOW_STATUS.WITHDRAW.value()) {
                    LastAnalysis lastAnalysis = new LastAnalysis();
                    String toolName = tool.getToolName();
                    lastAnalysis.setToolName(toolName);
                    lastAnalysis.setCurStep(tool.getCurStep());
                    lastAnalysis.setStepStatus(tool.getStepStatus());
                    toolLastAnalysisMap.put(toolName, lastAnalysis);
                    toolLastAnalysisList.add(lastAnalysis);
                }
            }

            GetLastAnalysisResultsVO getLastAnalysisResultsVO = new GetLastAnalysisResultsVO();
            getLastAnalysisResultsVO.setTaskId(taskId);
            getLastAnalysisResultsVO.setToolSet(toolLastAnalysisMap.keySet());

            // 调用defect模块的接口获取工具的最近一次分析结果
            Result<List<ToolLastAnalysisResultVO>> result =
                    client.get(ServiceTaskLogRestResource.class).getLastAnalysisResults(getLastAnalysisResultsVO);
            if (result.isNotOk() || null == result.getData()) {
                log.error("get last analysis results fail! taskId is: {}, msg: {}", taskId, result.getMessage());
                throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
            }

            lastAnalysisResultVOs = result.getData();
        }

        if (CollectionUtils.isNotEmpty(lastAnalysisResultVOs)) {
            String buildId = "";
            for (ToolLastAnalysisResultVO toolLastAnalysisResultVO : lastAnalysisResultVOs) {
                LastAnalysis lastAnalysis =
                        toolLastAnalysisMap.get(toolLastAnalysisResultVO.getToolName());
                lastAnalysis.setLastAnalysisResult(toolLastAnalysisResultVO.getLastAnalysisResultVO());
                long elapseTime = toolLastAnalysisResultVO.getElapseTime();
                long endTime = toolLastAnalysisResultVO.getEndTime();
                long startTime = toolLastAnalysisResultVO.getStartTime();
                long lastAnalysisTime = startTime;
                if (elapseTime == 0 && endTime != 0) {
                    elapseTime = endTime - startTime;
                }

                lastAnalysis.setElapseTime(elapseTime);
                lastAnalysis.setLastAnalysisTime(lastAnalysisTime);
                lastAnalysis.setBuildId(toolLastAnalysisResultVO.getBuildId());
                lastAnalysis.setBuildNum(toolLastAnalysisResultVO.getBuildNum());
                buildId = toolLastAnalysisResultVO.getBuildId();
            }
            // 获取度量信息
            Result<MetricsVO> metricsRes = client.get(ServiceMetricsRestResource.class).getMetrics(taskId, buildId);
            if (metricsRes.isOk() && metricsRes.getData() != null) {
                try {
                    org.apache.commons.beanutils.BeanUtils.copyProperties(taskOverviewVO, metricsRes.getData());
                } catch (IllegalAccessException | InvocationTargetException e) {
                    log.error("", e);
                }
            }
        }
        String orderToolIds = commonDao.getToolOrder();
        List<String> toolOrderList = Arrays.asList(orderToolIds.split(","));

        toolLastAnalysisList = toolLastAnalysisList.stream()
                .filter(lastAnalysis ->
                        !lastAnalysis.getToolName().equals(Tool.GITHUBSTATISTIC.name())
                                && !lastAnalysis.getToolName().equals(Tool.SCC.name()))
                .sorted(Comparator.comparingInt(o -> toolOrderList.indexOf(o.getToolName())))
                .collect(Collectors.toList());

        taskOverviewVO.setTaskId(taskId);
        //taskOverviewVO.setStatus();
        taskOverviewVO.setLastAnalysisResultList(toolLastAnalysisList);

        return taskOverviewVO;
    }

    @Override
    public TaskOverviewVO getTaskOverview(Long taskId, String buildNum, String orderBy) {
        if (StringUtils.isBlank(orderBy) || orderBy.equals("TOOL")) {
            return getTaskOverview(taskId, buildNum);
        }

        TaskInfoEntity taskEntity = taskRepository.findToolListFirstByTaskId(taskId);
        if (taskEntity == null) {
            log.error("can not find task by taskId: {}", taskId);
            throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS, new String[]{String.valueOf(taskId)}, null);
        }
        List<ToolConfigInfoEntity> toolConfigInfoList = taskEntity.getToolConfigInfoList();
        Map<String, List<String>> toolMap = toolConfigInfoList.stream()
                .filter(it -> it.getFollowStatus() != TaskConstants.FOLLOW_STATUS.WITHDRAW.value()
                        && !it.getToolName().equals(Tool.IP_CHECK.name()))
                .map(ToolConfigInfoEntity::getToolName)
                .collect(Collectors.groupingBy(it -> toolMetaCache.getToolBaseMetaCache(it).getType()));

        TaskOverviewVO taskOverviewVO = new TaskOverviewVO();
        Map<String, LastCluster> lastClusterResultMap = new LinkedHashMap<>(5);
        lastClusterResultMap.put(ToolType.DEFECT.name(),
                new LastCluster(new DefectClusterResultVO(
                        ToolType.DEFECT.name(), toolMap.getOrDefault(
                        ToolType.DEFECT.name(), Collections.emptyList()).size(),
                        toolMap.getOrDefault(ToolType.DEFECT.name(), Collections.emptyList()))));

        lastClusterResultMap.put(ToolType.SECURITY.name(),
                new LastCluster(new SecurityClusterResultVO(
                        ToolType.SECURITY.name(), toolMap.getOrDefault(
                        ToolType.SECURITY.name(), Collections.emptyList()).size(),
                        toolMap.getOrDefault(ToolType.SECURITY.name(), Collections.emptyList()))));

        lastClusterResultMap.put(ToolType.STANDARD.name(),
                new LastCluster(new StandardClusterResultVO(
                        ToolType.STANDARD.name(), toolMap.getOrDefault(
                        ToolType.STANDARD.name(), Collections.emptyList()).size(),
                        toolMap.getOrDefault(ToolType.STANDARD.name(), Collections.emptyList()))));

        lastClusterResultMap.put(ToolType.CCN.name(),
                new LastCluster(new CcnClusterResultVO(
                        ToolType.CCN.name(), toolMap.getOrDefault(
                        ToolType.CCN.name(), Collections.emptyList()).size(),
                        toolMap.getOrDefault(ToolType.CCN.name(), Collections.emptyList()))));

        lastClusterResultMap.put(ToolType.DUPC.name(),
                new LastCluster(new DupcClusterResultVO(
                        ToolType.DUPC.name(), toolMap.getOrDefault(
                        ToolType.DUPC.name(), Collections.emptyList()).size(),
                        toolMap.getOrDefault(ToolType.DUPC.name(), Collections.emptyList()))));

        Result<TaskLogOverviewVO> result = client.get(ServiceTaskLogOverviewResource.class)
                .getTaskLogOverview(taskId, null, ScanStatus.SUCCESS.getCode());
        if (result.isOk() && result.getData() != null) {
            TaskLogOverviewVO taskLogOverviewVO = result.getData();
            String buildId = taskLogOverviewVO.getBuildId();
            log.info("get task overview by type: {} {}", taskId, buildId);
            // 获取维度统计信息
            Result<List<BaseClusterResultVO>> clusterResult =
                    client.get(ServiceClusterStatisticRestReource.class).getClusterStatistic(taskId, buildId);
            if (clusterResult.isOk() && clusterResult.getData() != null) {
                List<BaseClusterResultVO> clusterVOList = clusterResult.getData();
                log.info("task overview Test {}", clusterVOList);
                clusterVOList.forEach(baseClusterResultVO -> {
                    baseClusterResultVO.setToolList(
                            toolMap.getOrDefault(baseClusterResultVO.getType(), Collections.emptyList()));
                    baseClusterResultVO.setToolNum(
                            toolMap.getOrDefault(baseClusterResultVO.getType(), Collections.emptyList()).size());
                    lastClusterResultMap.put(baseClusterResultVO.getType(),
                            new LastCluster(baseClusterResultVO));
                });
            }

            // 获取度量信息
            Result<MetricsVO> metricsRes = client.get(ServiceMetricsRestResource.class).getMetrics(taskId, buildId);
            if (metricsRes.isOk() && metricsRes.getData() != null) {
                try {
                    org.apache.commons.beanutils.BeanUtils.copyProperties(taskOverviewVO, metricsRes.getData());
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }

        taskOverviewVO.setTaskId(taskId);
        taskOverviewVO.setLastClusterResultList(new ArrayList<>(lastClusterResultMap.values()));
        return taskOverviewVO;
    }


    /**
     * 开启任务
     *
     * @param taskId
     * @return
     */
    @Override
    @OperationHistory(funcId = FUNC_TASK_SWITCH, operType = ENABLE_ACTION)
    public Boolean startTask(Long taskId, String userName) {
        if (authExPermissionApi.isAdminMember(userName)) {
            return doStartTask(taskId, userName, false);
        } else {
            return doStartTask(taskId, userName, true);
        }
    }


    /**
     * 开启任务
     *
     * @param taskId          任务ID
     * @param userName        操作人
     * @param checkPermission 是否检查权限
     * @return boolean
     */
    private Boolean doStartTask(Long taskId, String userName, boolean checkPermission) {
        TaskInfoEntity taskEntity = taskRepository.findFirstByTaskId(taskId);
        if (Objects.isNull(taskEntity)) {
            log.error("taskInfo not exists! task id is: {}", taskId);
            throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS, new String[]{String.valueOf(taskId)}, null);
        }
        List<String> taskMemberList = taskEntity.getTaskMember();
        List<String> taskOwnerList = taskEntity.getTaskOwner();
        Boolean taskMemberPermission = CollectionUtils.isEmpty(taskMemberList) || !taskMemberList.contains(userName);
        Boolean taskOwnerPermission = CollectionUtils.isEmpty(taskOwnerList) || !taskOwnerList.contains(userName);
        if (checkPermission && taskMemberPermission && taskOwnerPermission) {
            log.error("current user has no permission to the task");
            throw new CodeCCException(CommonMessageCode.PERMISSION_DENIED, new String[]{userName}, null);
        }

        if (CollectionUtils.isNotEmpty(taskEntity.getExecuteDate())
                && StringUtils.isNotBlank(taskEntity.getExecuteTime())) {
            log.error("The task is already open and cannot be repeated.");
            throw new CodeCCException(TaskMessageCode.TASK_HAS_START);
        }

        // 如果是蓝盾项目，要开启流水线定时触发任务
        if (StringUtils.isNotBlank(taskEntity.getProjectId())) {
            // 启动时，把原先的定时任务恢复
            DisableTaskEntity lastDisableTaskInfo = taskEntity.getLastDisableTaskInfo();
            if (Objects.isNull(lastDisableTaskInfo)) {
                log.error("pipeline execution timing is empty.");
                //                throw new CodeCCException(TaskMessageCode.PIPELINE_EXECUTION_TIME_EMPTY);
            } else {
                String lastExecuteTime = lastDisableTaskInfo.getLastExecuteTime();
                List<String> lastExecuteDate = lastDisableTaskInfo.getLastExecuteDate();

                // 开启定时执行的日期时间
                taskEntity.setExecuteTime(lastExecuteTime);
                taskEntity.setExecuteDate(lastExecuteDate);
                // 删除DB保存的执行时间
                taskEntity.setLastDisableTaskInfo(null);
                pipelineService.modifyCodeCCTiming(taskEntity, lastExecuteDate, lastExecuteTime, userName);
            }
        }

        taskEntity.setDisableTime("");
        taskEntity.setDisableReason("");
        taskEntity.setStatus(TaskConstants.TaskStatus.ENABLE.value());

        //在权限中心重新注册任务
        authExRegisterApi.registerCodeCCTask(userName, String.valueOf(taskId), taskEntity.getNameEn(),
                taskEntity.getProjectId());

        //恢复日报
        if (null != taskEntity.getNotifyCustomInfo()
                && StringUtils.isNotBlank(taskEntity.getNotifyCustomInfo().getReportJobName())) {
            JobExternalDto jobExternalDto =
                    new JobExternalDto(taskEntity.getNotifyCustomInfo().getReportJobName(), "", "", "", new HashMap<>(),
                            OperationType.RESUME);
            rabbitTemplate.convertAndSend(EXCHANGE_EXTERNAL_JOB, "", jobExternalDto);
        }

        return taskDao.updateEntity(taskEntity, userName);
    }


    /**
     * 停用任务
     *
     * @param taskId
     * @param disabledReason
     * @param userName
     * @return
     */
    @Override
    @OperationHistory(funcId = FUNC_TASK_SWITCH, operType = DISABLE_ACTION)
    public Boolean stopTask(Long taskId, String disabledReason, String userName) {
        TaskInfoEntity taskEntity = taskRepository.findFirstByTaskId(taskId);
        if (Objects.isNull(taskEntity)) {
            log.error("taskInfo not exists! task id is: {}", taskId);
            throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS, new String[]{String.valueOf(taskId)}, null);
        }

        if (authExPermissionApi.isAdminMember(userName)) {
            return doStopTask(taskEntity, disabledReason, userName, false);
        } else {
            return doStopTask(taskEntity, disabledReason, userName, true);
        }
    }

    /**
     * 停用任务
     *
     * @param pipelineId
     * @param disabledReason
     * @param userName
     * @return
     */
    @Override
    @OperationHistory(funcId = FUNC_TASK_SWITCH, operType = DISABLE_ACTION)
    public Boolean stopTask(String pipelineId, String disabledReason, String userName) {
        TaskInfoEntity taskEntity = taskRepository.findFirstByPipelineId(pipelineId);
        if (Objects.isNull(taskEntity)) {
            log.error("taskInfo not exists! pipeline id is: {}", pipelineId);
            throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS, new String[]{String.valueOf(pipelineId)},
                    null);
        }
        return doStopTask(taskEntity, disabledReason, userName, false);
    }

    /**
     * 管理员在OP停用任务
     *
     * @param taskId         任务ID
     * @param disabledReason 停用理由
     * @param userName       操作人
     * @return boolean
     */
    @Override
    public Boolean stopTaskByAdmin(Long taskId, String disabledReason, String userName) {
        TaskInfoEntity taskEntity = taskRepository.findFirstByTaskId(taskId);
        if (Objects.isNull(taskEntity)) {
            log.error("taskInfo not exists! task id is: {}", taskId);
            throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS, new String[]{String.valueOf(taskId)}, null);
        }
        return doStopTask(taskEntity, disabledReason, userName, false);
    }


    /**
     * 管理员在OP开启任务
     *
     * @param taskId   任务ID
     * @param userName 操作人
     * @return boolean
     */
    @Override
    public Boolean startTaskByAdmin(Long taskId, String userName) {
        return doStartTask(taskId, userName, false);
    }


    private Boolean doStopTask(TaskInfoEntity taskEntity,
                               String disabledReason,
                               String userName,
                               boolean checkPermission) {
        long taskId = taskEntity.getTaskId();
        if (BsTaskCreateFrom.GONGFENG_SCAN.value().equalsIgnoreCase(taskEntity.getCreateFrom())) {
            log.info("gongfeng project not allowed to disable");
            throw new CodeCCException(CommonMessageCode.PERMISSION_DENIED, new String[]{userName}, null);
        }

        //判断是否有权限
        List<String> taskMemberList = taskEntity.getTaskMember();
        List<String> taskOwnerList = taskEntity.getTaskOwner();
        Boolean taskMemberPermission = CollectionUtils.isEmpty(taskMemberList) || !taskMemberList.contains(userName);
        Boolean taskOwnerPermission = CollectionUtils.isEmpty(taskOwnerList) || !taskOwnerList.contains(userName);
        if (checkPermission && taskMemberPermission && taskOwnerPermission) {
            log.error("current user has no permission to the task");
            throw new CodeCCException(CommonMessageCode.PERMISSION_DENIED, new String[]{userName}, new Exception());
        }

        if (StringUtils.isNotBlank(taskEntity.getDisableTime())) {
            log.error("The task is already close and cannot be repeated.");
            throw new CodeCCException(TaskMessageCode.TASK_HAS_CLOSE);
        }

        // 如果是蓝盾项目，并且是服务创建的，要停止流水线定时触发任务
        if (StringUtils.isNotBlank(taskEntity.getProjectId())
                && BsTaskCreateFrom.BS_CODECC.value().equalsIgnoreCase(taskEntity.getCreateFrom())) {
            String executeTime = taskEntity.getExecuteTime();
            List<String> executeDate = taskEntity.getExecuteDate();

            if (CollectionUtils.isEmpty(executeDate)) {
                log.error("pipeline execute date is empty. task id : {}", taskId);
                executeDate = Collections.emptyList();
            }

            if (StringUtils.isBlank(executeTime)) {
                log.error("pipeline execute time is empty. task id : {}", taskId);
                executeTime = "";
            }

            // 调用蓝盾API 删除定时构建原子
            pipelineService.deleteCodeCCTiming(userName, taskEntity);

            // 存储启用日期时间到DisableTaskEntity
            DisableTaskEntity lastDisableTaskInfo = taskEntity.getLastDisableTaskInfo();
            if (Objects.isNull(lastDisableTaskInfo)) {
                lastDisableTaskInfo = new DisableTaskEntity();
            }

            lastDisableTaskInfo.setLastExecuteTime(executeTime);
            lastDisableTaskInfo.setLastExecuteDate(executeDate);
            taskEntity.setLastDisableTaskInfo(lastDisableTaskInfo);
        }

        //要将权限中心的任务成员，任务管理员同步到task表下面，便于后续启用时再进行注册
        TaskMemberVO taskMemberVO = getTaskUsers(taskId, taskEntity.getProjectId());
        taskEntity.setTaskMember(taskMemberVO.getTaskMember());
        taskEntity.setTaskOwner(taskMemberVO.getTaskOwner());
        taskEntity.setTaskViewer(taskMemberVO.getTaskViewer());

        //在权限中心中删除相应的资源
        if (BsTaskCreateFrom.BS_CODECC.value().equalsIgnoreCase(taskEntity.getCreateFrom())) {
            try {
                authExRegisterApi.deleteCodeCCTask(String.valueOf(taskId), taskEntity.getProjectId());
            } catch (UnauthorizedException e) {
                log.error("delete iam resource fail! error message: {}", e.getMessage());
                throw new CodeCCException(TaskMessageCode.CLOSE_TASK_FAIL);
            }
        }

        log.info("stopping task: delete pipeline scheduled atom and auth center resource success! project id: {}",
                taskEntity.getProjectId());

        taskEntity.setExecuteDate(new ArrayList<>());
        taskEntity.setExecuteTime("");
        taskEntity.setDisableTime(String.valueOf(System.currentTimeMillis()));
        taskEntity.setDisableReason(disabledReason);
        taskEntity.setStatus(TaskConstants.TaskStatus.DISABLE.value());

        //停止日报
        if (null != taskEntity.getNotifyCustomInfo() && StringUtils.isNotBlank(taskEntity.getNotifyCustomInfo().getReportJobName())) {
            JobExternalDto jobExternalDto = new JobExternalDto(
                    taskEntity.getNotifyCustomInfo().getReportJobName(),
                    "",
                    "",
                    "",
                    new HashMap<>(),
                    OperationType.PARSE
            );
            rabbitTemplate.convertAndSend(EXCHANGE_EXTERNAL_JOB, "", jobExternalDto);
        }

        return taskDao.updateEntity(taskEntity, userName);
    }


    /**
     * 获取代码库配置信息
     *
     * @param taskId
     * @return
     */
    @Override
    public TaskCodeLibraryVO getCodeLibrary(Long taskId) {
        TaskInfoEntity taskEntity = taskRepository.findFirstByTaskId(taskId);
        if (Objects.isNull(taskEntity)) {
            log.error("taskInfo not exists! task id is: {}", taskId);
            throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS, new String[]{String.valueOf(taskId)}, null);
        }

        // 获取所有工具的基础信息
        Map<String, ToolMetaBaseVO> toolMetaMap = toolMetaCache.getToolMetaListFromCache(Boolean.FALSE, Boolean.TRUE);

        // 获取排序好的所有工具
        String[] toolIdArr = Optional.ofNullable(taskEntity.getToolNames())
                .map(tool -> tool.split(ComConstants.STRING_SPLIT))
                .orElse(new String[]{});

        // 获取工具配置Map
        Map<String, JSONObject> chooseJsonMap = getToolConfigInfoMap(taskEntity);

        Map<String, GlobalMessage> tipsMessage = globalMessageUtil.getGlobalMessageMap(GLOBAL_TOOL_PARAMS_TIPS);
        Map<String, GlobalMessage> labelNameMessage =
                globalMessageUtil.getGlobalMessageMap(GLOBAL_TOOL_PARAMS_LABEL_NAME);
        List<ToolConfigParamJsonVO> paramJsonList = new ArrayList<>();
        for (String toolName : toolIdArr) {
            // 工具被禁用则不显示
            if (!chooseJsonMap.keySet().contains(toolName)) {
                continue;
            }

            // 获取工具对应的基本数据
            ToolMetaBaseVO toolMetaBaseVO = toolMetaMap.get(toolName);

            if (Objects.nonNull(toolMetaBaseVO)) {
                String params = toolMetaBaseVO.getParams();
                if (StringUtils.isNotBlank(params) && !ComConstants.STRING_NULL_ARRAY.equals(params)) {
                    JSONObject chooseJson = chooseJsonMap.get(toolName);
                    List<Map<String, Object>> arrays = JsonUtil.INSTANCE.to(params);
                    for (Map<String, Object> array : arrays) {
                        ToolConfigParamJsonVO toolConfig = JsonUtil.INSTANCE.mapTo(array, ToolConfigParamJsonVO.class);
                        String toolChooseValue = Objects.isNull(chooseJson)
                                ? toolConfig.getVarDefault()
                                : StringUtils.isBlank((String) chooseJson.get(toolConfig.getVarName()))
                                        ? toolConfig.getVarDefault() : (String) chooseJson.get(toolConfig.getVarName());


                        // 工具参数标签[ labelName ]国际化
                        GlobalMessage labelGlobalMessage = labelNameMessage.get(String.format("%s:%s", toolName,
                                toolConfig.getVarName()));
                        if (Objects.nonNull(labelGlobalMessage)) {
                            String globalLabelName = globalMessageUtil.getMessageByLocale(labelGlobalMessage);
                            toolConfig.setLabelName(globalLabelName);
                        }

                        // 工具参数提示[ tips ]国际化
                        GlobalMessage tipGlobalMessage = tipsMessage.get(String.format("%s:%s", toolName,
                                toolConfig.getVarName()));
                        if (Objects.nonNull(tipGlobalMessage)) {
                            String globalTips = globalMessageUtil.getMessageByLocale(tipGlobalMessage);
                            toolConfig.setVarTips(globalTips);
                        }

                        toolConfig.setTaskId(taskId);
                        toolConfig.setToolName(toolMetaBaseVO.getName());
                        toolConfig.setChooseValue(toolChooseValue);
                        paramJsonList.add(toolConfig);
                    }
                }
            }
        }

        TaskCodeLibraryVO taskCodeLibrary = getRepoInfo(taskId);
        BeanUtils.copyProperties(taskEntity, taskCodeLibrary);
        taskCodeLibrary.setToolConfigList(paramJsonList);
        taskCodeLibrary.setRepoHashId(taskEntity.getRepoHashId());

        return taskCodeLibrary;
    }


    @Override
    public Boolean checkTaskExists(long taskId) {
        return taskRepository.findFirstByTaskId(taskId) != null;
    }


    /**
     * 更新代码库信息
     *
     * @param taskId
     * @param taskDetailVO
     * @return
     */
    @Override
    @OperationHistory(funcId = FUNC_CODE_REPOSITORY, operType = MODIFY_INFO)
    public Boolean updateCodeLibrary(Long taskId, String userName, TaskDetailVO taskDetailVO) {
        TaskInfoEntity taskEntity = taskRepository.findFirstByTaskId(taskId);
        if (Objects.isNull(taskEntity)) {
            log.error("taskInfo not exists! task id is: {}", taskId);
            throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS, new String[]{String.valueOf(taskId)}, null);
        }

        // 更新工具配置信息
        updateToolConfigInfoEntity(taskDetailVO, taskEntity, userName);

        // 代码仓库是否修改
        boolean repoIdUpdated = false;
        if (StringUtils.isNotEmpty(taskDetailVO.getRepoHashId())) {
            if (!taskDetailVO.getRepoHashId().equals(taskEntity.getRepoHashId())) {
                log.info("change repo for task: {}, {} -> {}", taskDetailVO.getTaskId(), taskEntity.getRepoHashId(),
                        taskDetailVO.getRepoHashId());
                repoIdUpdated = true;
            }
        }
        taskEntity.setRepoHashId(taskDetailVO.getRepoHashId());
        taskEntity.setBranch(taskDetailVO.getBranch());
        taskEntity.setScmType(taskDetailVO.getScmType());
        taskEntity.setAliasName(taskDetailVO.getAliasName());
        taskEntity.setOsType(StringUtils.isNotEmpty(taskDetailVO.getOsType()) ? taskDetailVO.getOsType() :
                taskEntity.getOsType());
        taskEntity.setBuildEnv(MapUtils.isNotEmpty(taskDetailVO.getBuildEnv()) ? taskDetailVO.getBuildEnv() :
                taskEntity.getBuildEnv());
        taskEntity.setProjectBuildType((StringUtils.isNotEmpty(taskDetailVO.getProjectBuildType())
                ? taskDetailVO.getProjectBuildType() : taskEntity.getProjectBuildType()));
        taskEntity.setProjectBuildCommand(StringUtils.isNotEmpty(taskDetailVO.getProjectBuildCommand())
                ? taskDetailVO.getProjectBuildCommand() : taskEntity.getProjectBuildCommand());

        BatchRegisterVO registerVO = new BatchRegisterVO();
        registerVO.setRepoHashId(taskEntity.getRepoHashId());
        registerVO.setBranch(taskEntity.getBranch());
        registerVO.setScmType(taskEntity.getScmType());
        registerVO.setOsType(taskDetailVO.getOsType());
        registerVO.setBuildEnv(taskDetailVO.getBuildEnv());
        registerVO.setProjectBuildType(taskDetailVO.getProjectBuildType());
        registerVO.setProjectBuildCommand(taskDetailVO.getProjectBuildCommand());
        // 更新流水线设置
        // 新版v3插件不需要更新model，直接codecc后台取对应数据了
        pipelineService.updateCodeLibrary(userName, registerVO, taskEntity);

        // 设置强制全量扫描标志
        if (repoIdUpdated) {
            setForceFullScan(taskEntity);
        }

        return taskDao.updateEntity(taskEntity, userName);
    }


    private String getRelPath(List<ToolConfigParamJsonVO> toolConfigList) {
        if (CollectionUtils.isNotEmpty(toolConfigList)) {
            for (ToolConfigParamJsonVO toolConfigParamJsonVO : toolConfigList) {
                if (Tool.GOML.name().equalsIgnoreCase(toolConfigParamJsonVO.getToolName())) {
                    if (ComConstants.PARAMJSON_KEY_REL_PATH.equalsIgnoreCase(toolConfigParamJsonVO.getVarName())) {
                        return toolConfigParamJsonVO.getChooseValue();
                    }
                }
            }
        }
        return "";
    }


    /**
     * 获取任务成员及管理员清单
     *
     * @param taskId
     * @param projectId
     * @return
     */
    @Override
    public TaskMemberVO getTaskUsers(long taskId, String projectId) {

        TaskMemberVO taskMemberVO = new TaskMemberVO();
        String taskCreateFrom = authTaskService.getTaskCreateFrom(taskId);
        if (BsTaskCreateFrom.BS_CODECC.value().equals(taskCreateFrom)) {
            // 获取各角色对应用户列表
            List<String> taskMembers = authExPermissionApi.queryTaskUserListForAction(String.valueOf(taskId), projectId,
                    PermissionUtil.INSTANCE.getCodeCCPermissionsFromActions(AuthRole.TASK_MEMBER.getCodeccActions()));
            List<String> taskOwners = authExPermissionApi.queryTaskUserListForAction(String.valueOf(taskId), projectId,
                    PermissionUtil.INSTANCE.getCodeCCPermissionsFromActions(AuthRole.TASK_OWNER.getCodeccActions()));
            List<String> taskViews = authExPermissionApi.queryTaskUserListForAction(String.valueOf(taskId), projectId,
                    PermissionUtil.INSTANCE.getCodeCCPermissionsFromActions(AuthRole.TASK_VIEWER.getCodeccActions()));
            taskMemberVO.setTaskMember(taskMembers);
            taskMemberVO.setTaskOwner(taskOwners);
            taskMemberVO.setTaskViewer(taskViews);
        }

        return taskMemberVO;
    }

    @Override
    public Boolean manualExecuteTaskNoProxy(long taskId, String isFirstTrigger, String userName) {
        return manualExecuteTask(taskId, isFirstTrigger, userName);
    }

    @Override
    @OperationHistory(funcId = FUNC_TRIGGER_ANALYSIS, operType = TRIGGER_ANALYSIS)
    public Boolean manualExecuteTask(long taskId, String isFirstTrigger, String userName) {
        TaskInfoEntity taskInfoEntity = taskRepository.findFirstByTaskId(taskId);
        List<ToolConfigInfoEntity> toolConfigInfoEntityList = taskInfoEntity.getToolConfigInfoList();
        if (CollectionUtils.isEmpty(toolConfigInfoEntityList)) {
            log.info("tool list is empty! task id: {}", taskId);
            return false;
        }

        List<ToolConfigInfoEntity> clocList = toolConfigInfoEntityList.stream().filter(it ->
                Tool.CLOC.name().equals(it.getToolName())
        ).collect(Collectors.toList());

        if (clocList.isEmpty()) {
            ToolConfigInfoEntity clocToolConfig = new ToolConfigInfoEntity();
            long time = System.currentTimeMillis();
            clocToolConfig.setTaskId(taskInfoEntity.getTaskId());
            clocToolConfig.setToolName(Tool.CLOC.name());
            clocToolConfig.setCreatedBy(userName);
            clocToolConfig.setCreatedDate(time);
            clocToolConfig.setUpdatedBy(userName);
            clocToolConfig.setUpdatedDate(time);
            clocToolConfig.setCurStep(Step4MutliTool.READY.value());
            clocToolConfig.setStepStatus(StepStatus.SUCC.value());
            clocToolConfig.setFollowStatus(FOLLOW_STATUS.NOT_FOLLOW_UP_0.value());
            clocToolConfig.setLastFollowStatus(FOLLOW_STATUS.NOT_FOLLOW_UP_0.value());
            toolConfigInfoEntityList.add(clocToolConfig);
            List<ToolConfigInfoEntity> tools = toolRepository.saveAll(toolConfigInfoEntityList);
            taskInfoEntity.setToolConfigInfoList(tools);
            taskRepository.save(taskInfoEntity);
        } else if (clocList.get(0).getFollowStatus() == FOLLOW_STATUS.WITHDRAW.value()) {
            ToolConfigInfoEntity tool = clocList.get(0);
            tool.setFollowStatus(FOLLOW_STATUS.ACCESSED.value());
            toolRepository.save(tool);
        }

        Set<String> toolSet = toolConfigInfoEntityList.stream().filter(toolConfigInfoEntity ->
                FOLLOW_STATUS.WITHDRAW.value() != toolConfigInfoEntity.getFollowStatus()
        ).map(ToolConfigInfoEntity::getToolName
        ).collect(Collectors.toSet());

        if (CollectionUtils.isNotEmpty(toolSet)) {
            // 支持并发后不再停用正在运行的流水线
            //停止原有正在运行的流水线
            /*Result<Boolean> stopResult = client.get(ServiceTaskLogRestResource.class).stopRunningTask
            (taskInfoEntity.getPipelineId(), taskInfoEntity.getNameEn(),
                    toolSet, taskInfoEntity.getProjectId(), taskId, userName);
            if (stopResult.isNotOk() || null == stopResult.getData() || !stopResult.getData())
            {
                log.error("stop running pipeline fail! task id: {}", taskId);
                throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
            }*/

            // 启动流水线
            String buildId = pipelineService.startPipeline(taskInfoEntity.getPipelineId(),
                    taskInfoEntity.getProjectId(),
                    taskInfoEntity.getNameEn(), taskInfoEntity.getCreateFrom(), new ArrayList<>(toolSet), userName);

            //更新任务状态
            toolSet.forEach(tool ->
                    pipelineService.updateTaskInitStep(isFirstTrigger, taskInfoEntity, buildId, tool, userName)
            );

            log.info("start pipeline and send delay message");
            rabbitTemplate.convertAndSend(EXCHANGE_EXPIRED_TASK_STATUS, ROUTE_EXPIRED_TASK_STATUS,
                    new ScanTaskTriggerDTO(taskId, buildId), message ->
                    {
                        //todo 配置在配置文件里
                        message.getMessageProperties().setDelay(15 * 60 * 60 * 1000);
                        return message;
                    });
        }
        return true;
    }

    @Override
    public Boolean sendStartTaskSignal(Long taskId, String buildId) {
        //todo 后续和流水线对齐
        rabbitTemplate.convertAndSend(EXCHANGE_EXPIRED_TASK_STATUS, ROUTE_EXPIRED_TASK_STATUS,
                new ScanTaskTriggerDTO(taskId, buildId), message ->
                {
                    message.getMessageProperties().setDelay(24 * 60 * 60 * 1000);
                    return message;
                });
        return true;
    }


    /**
     * 通过流水线ID获取任务信息
     *
     * @param pipelineId
     * @param user
     * @return
     */
    @Override
    public PipelineTaskVO getTaskInfoByPipelineId(String pipelineId, String user) {
        TaskInfoEntity taskInfoEntity = taskRepository.findFirstByPipelineId(pipelineId);
        if (taskInfoEntity == null) {
            log.error("can not find task by pipeline id: {}", pipelineId);
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{"pipeline id"}, null);
        }
        PipelineTaskVO taskDetailVO = new PipelineTaskVO();
        taskDetailVO.setProjectId(taskInfoEntity.getProjectId());
        taskDetailVO.setTaskId(taskInfoEntity.getTaskId());
        taskDetailVO.setTools(Lists.newArrayList());
        taskDetailVO.setEnName(taskInfoEntity.getNameEn());
        taskDetailVO.setCnName(taskInfoEntity.getNameCn());

        List<String> openTools = Lists.newArrayList();
        if (CollectionUtils.isNotEmpty(taskInfoEntity.getToolConfigInfoList())) {
            for (ToolConfigInfoEntity toolConfigInfoEntity : taskInfoEntity.getToolConfigInfoList()) {
                if (TaskConstants.FOLLOW_STATUS.WITHDRAW.value() != toolConfigInfoEntity.getFollowStatus()) {
                    openTools.add(toolConfigInfoEntity.getToolName());
                    PipelineToolVO pipelineToolVO = new PipelineToolVO();
                    pipelineToolVO.setToolName(toolConfigInfoEntity.getToolName());
                    if (toolConfigInfoEntity.getCheckerSet() != null) {
                        CheckerSetVO checkerSetVO = new CheckerSetVO();
                        BeanUtils.copyProperties(toolConfigInfoEntity.getCheckerSet(), checkerSetVO);
                        pipelineToolVO.setCheckerSetInUse(checkerSetVO);
                    }
                    if (StringUtils.isNotEmpty(toolConfigInfoEntity.getParamJson())) {
                        pipelineToolVO.setParams(getParams(toolConfigInfoEntity.getParamJson()));
                    }
                    taskDetailVO.getTools().add(pipelineToolVO);
                }
            }
        }

        //        if (StringUtils.isNotEmpty(user) && CollectionUtils.isNotEmpty(openTools))
        //        {
        //            Map<String, DividedCheckerSetsVO> checkerSetsMap = Maps.newHashMap();
        //            if (StringUtils.isNotEmpty(user))
        //            {
        //                Result<UserCheckerSetsVO> checkerSetsResult = client.get(ServiceCheckerRestResource.class)
        //                .getCheckerSets(taskInfoEntity.getTaskId(),
        //                        new GetCheckerSetsReqVO(openTools), user, taskInfoEntity.getProjectId());
        //                if (checkerSetsResult.isNotOk() || null == checkerSetsResult.getData() || CollectionUtils
        //                .isEmpty(checkerSetsResult.getData().getCheckerSets()))
        //                {
        //                    log.error("get checker sets fail! pipeline id: {}, task id: {}, user: {}", pipelineId,
        //                    taskInfoEntity.getTaskId(), user);
        //                    throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        //                }
        //                for (DividedCheckerSetsVO checkerSets : checkerSetsResult.getData().getCheckerSets())
        //                {
        //                    checkerSetsMap.put(checkerSets.getToolName(), checkerSets);
        //                }
        //            }
        //            for (PipelineToolVO pipelineToolVO : taskDetailVO.getTools())
        //            {
        //                if (checkerSetsMap.get(pipelineToolVO.getToolName()) != null)
        //                {
        //                    pipelineToolVO.setToolCheckerSets(checkerSetsMap.get(pipelineToolVO.getToolName()));
        //                }
        //            }
        //        }

        // 加入语言的显示名称
        List<String> codeLanguages = pipelineService.localConvertDevopsCodeLang(taskInfoEntity.getCodeLang());
        taskDetailVO.setCodeLanguages(codeLanguages);

        return taskDetailVO;
    }

    @Override
    public TaskStatusVO getTaskStatus(Long taskId) {
        TaskInfoEntity taskInfoEntity = taskRepository.findFirstByTaskId(taskId);
        if (null == taskInfoEntity) {
            return null;
        }
        return new TaskStatusVO(taskInfoEntity.getStatus(), taskInfoEntity.getGongfengProjectId());
    }

    /**
     * 获取所有的基础工具信息
     *
     * @return
     */
    @Override
    public Map<String, ToolMetaBaseVO> getToolMetaListFromCache() {
        return toolMetaCache.getToolMetaListFromCache(Boolean.TRUE, Boolean.FALSE);
    }

    @Override
    public TaskInfoEntity getTaskById(Long taskId) {
        return taskRepository.findFirstByTaskId(taskId);
    }

    @Override
    public Boolean saveTaskInfo(TaskInfoEntity taskInfoEntity) {
        taskRepository.save(taskInfoEntity);
        return true;
    }

    @Override
    public List<TaskBaseVO> getTasksByBgId(Integer bgId) {
        List<TaskInfoEntity> taskInfoEntityList = taskRepository.findByBgId(bgId);
        if (CollectionUtils.isNotEmpty(taskInfoEntityList)) {
            return taskInfoEntityList.stream().map(taskInfoEntity ->
            {
                TaskBaseVO taskBaseVO = new TaskBaseVO();
                BeanUtils.copyProperties(taskInfoEntity, taskBaseVO,
                        "taskOwner", "executeDate", "enableToolList", "disableToolList");
                return taskBaseVO;
            }).collect(Collectors.toList());
        } else {
            return new ArrayList<>();
        }
    }

    @Override
    public List<TaskBaseVO> getTasksByIds(List<Long> taskIds) {
        List<TaskInfoEntity> taskInfoEntityList = taskRepository.findByTaskIdIn(taskIds);
        if (CollectionUtils.isNotEmpty(taskInfoEntityList)) {
            return taskInfoEntityList.stream().map(taskInfoEntity ->
            {
                TaskBaseVO taskBaseVO = new TaskBaseVO();
                BeanUtils.copyProperties(taskInfoEntity, taskBaseVO,
                        "taskOwner", "executeDate", "enableToolList", "disableToolList");
                return taskBaseVO;
            }).collect(Collectors.toList());
        } else {
            return new ArrayList<>();
        }
    }

    /**
     * 设置强制全量扫描标志
     *
     * @param taskEntity
     */
    @Override
    public void setForceFullScan(TaskInfoEntity taskEntity) {
        if (CollectionUtils.isNotEmpty(taskEntity.getToolConfigInfoList())) {
            List<String> setForceFullScanToolNames = Lists.newArrayList();
            for (ToolConfigInfoEntity toolConfigInfoEntity : taskEntity.getToolConfigInfoList()) {
                setForceFullScanToolNames.add(toolConfigInfoEntity.getToolName());
            }
            log.info("set force full scan, taskId:{}, toolNames:{}", taskEntity.getTaskId(), setForceFullScanToolNames);
            Result<Boolean> toolBuildInfoVOResult =
                    client.get(ServiceToolBuildInfoResource.class).setForceFullScan(taskEntity.getTaskId(),
                            setForceFullScanToolNames);
            if (toolBuildInfoVOResult == null || toolBuildInfoVOResult.isNotOk()) {
                log.error("set force full san failed! taskId={}, toolNames={}", taskEntity.getScanType(),
                        setForceFullScanToolNames);
                throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL,
                        new String[]{"set force full san failed!"}, null);
            }
        }
    }

    /**
     * 修改任务扫描触发配置
     *
     * @param taskId
     * @param user
     * @param scanConfigurationVO
     * @return
     */
    @Override
    @OperationHistory(funcId = FUNC_SCAN_SCHEDULE, operType = MODIFY_INFO)
    public Boolean updateScanConfiguration(Long taskId, String user, ScanConfigurationVO scanConfigurationVO) {
        // 更新定时分析配置
        TaskInfoEntity taskInfoEntity = taskRepository.findFirstByTaskId(taskId);
        if (scanConfigurationVO.getTimeAnalysisConfig() != null && BsTaskCreateFrom.BS_CODECC.value().equals(taskInfoEntity.getCreateFrom())) {
            TimeAnalysisConfigVO timeAnalysisConfigVO = scanConfigurationVO.getTimeAnalysisConfig();
            if (timeAnalysisConfigVO != null) {
                // 调用Kotlin方法时需要去掉null
                if (timeAnalysisConfigVO.getExecuteDate() == null) {
                    timeAnalysisConfigVO.setExecuteDate(Lists.newArrayList());
                }
                if (timeAnalysisConfigVO.getExecuteTime() == null) {
                    timeAnalysisConfigVO.setExecuteTime("");
                }
                //保存任务清单
                pipelineService.modifyCodeCCTiming(taskInfoEntity, timeAnalysisConfigVO.getExecuteDate(),
                        timeAnalysisConfigVO.getExecuteTime(), user);
            }
        }

        // 更新扫描方式
        if (scanConfigurationVO.getScanType() != null) {
            // 如果扫描方式由增量变成全量，需要设置强制全量，避免走快速增量的逻辑
            if (taskInfoEntity.getScanType() == ComConstants.ScanType.INCREMENTAL.code
                    && scanConfigurationVO.getScanType() == ComConstants.ScanType.FULL.code) {
                setForceFullScan(taskInfoEntity);
            }
            taskInfoEntity.setScanType(scanConfigurationVO.getScanType());
        }

        // 更新新告警判定配置
        NewDefectJudgeVO defectJudge = scanConfigurationVO.getNewDefectJudge();
        if (defectJudge != null) {
            NewDefectJudgeEntity newDefectJudgeEntity = new NewDefectJudgeEntity();
            BeanUtils.copyProperties(defectJudge, newDefectJudgeEntity);
            if (StringUtils.isNotEmpty(defectJudge.getFromDate())) {
                newDefectJudgeEntity.setFromDateTime(DateTimeUtils.convertStringDateToLongTime(defectJudge.getFromDate(), DateTimeUtils.yyyyMMddFormat));
            }
            taskInfoEntity.setNewDefectJudge(newDefectJudgeEntity);
        }

        // 更新告警作者转换配置
        authorTransfer(taskId, scanConfigurationVO, taskInfoEntity);

        // 更新扫描方式
        if (scanConfigurationVO.getMrCommentEnable() != null) {
            taskInfoEntity.setMrCommentEnable(scanConfigurationVO.getMrCommentEnable());
        }

        taskRepository.save(taskInfoEntity);
        return true;
    }

    @Override
    public Boolean authorTransferForApi(Long taskId, List<ScanConfigurationVO.TransferAuthorPair> transferAuthorPairs,
                                        String userId) {
        log.info("api author transfer function, user id: {}, task id: {}", userId, taskId);
        TaskInfoEntity taskInfoEntity = taskRepository.findFirstByTaskId(taskId);
        ScanConfigurationVO scanConfigurationVO = new ScanConfigurationVO();
        scanConfigurationVO.setTransferAuthorList(transferAuthorPairs);
        authorTransfer(taskId, scanConfigurationVO, taskInfoEntity);
        return true;
    }

    private void authorTransfer(Long taskId, ScanConfigurationVO scanConfigurationVO, TaskInfoEntity taskInfoEntity) {
        AuthorTransferVO authorTransferVO = new AuthorTransferVO();
        authorTransferVO.setTaskId(taskId);
        List<String> tools = toolService.getEffectiveToolList(taskInfoEntity);
        authorTransferVO.setEffectiveTools(tools);
        List<ScanConfigurationVO.TransferAuthorPair> transferAuthorList = scanConfigurationVO.getTransferAuthorList();
        if (CollectionUtils.isNotEmpty(transferAuthorList)) {
            List<AuthorTransferVO.TransferAuthorPair> newTransferAuthorList = transferAuthorList.stream()
                    .map(authorPair ->
                    {
                        AuthorTransferVO.TransferAuthorPair transferAuthorPair =
                                new AuthorTransferVO.TransferAuthorPair();
                        transferAuthorPair.setSourceAuthor(authorPair.getSourceAuthor());
                        transferAuthorPair.setTargetAuthor(authorPair.getTargetAuthor());
                        return transferAuthorPair;
                    })
                    .collect(Collectors.toList());
            authorTransferVO.setTransferAuthorList(newTransferAuthorList);
        }
        authorTransferBizService.authorTransfer(authorTransferVO);
    }

    /**
     * 更新工具配置信息
     *
     * @param taskDetailVO
     * @param taskEntity
     */
    private void updateToolConfigInfoEntity(TaskDetailVO taskDetailVO, TaskInfoEntity taskEntity, String userName) {
        // 获取当前任务的工具的配置信息
        List<ToolConfigInfoEntity> toolConfigList = taskEntity.getToolConfigInfoList();
        // 提交更新任务工具的配置信息
        List<ToolConfigParamJsonVO> updateToolConfigList = taskDetailVO.getDevopsToolParams();

        //根据原有的和提交的，更新工具参数

        if (CollectionUtils.isNotEmpty(toolConfigList) && CollectionUtils.isNotEmpty(updateToolConfigList)) {
            //提交参数map
            Map<String, String> paramMap = updateToolConfigList.stream().collect(Collectors.toMap(
                    ToolConfigParamJsonVO::getVarName, ToolConfigParamJsonVO::getChooseValue
            ));
            toolConfigList.forEach(toolConfigInfoEntity ->
            {
                ToolMetaBaseVO toolMetaBaseVO = toolMetaCache.getToolBaseMetaCache(toolConfigInfoEntity.getToolName());
                String toolParamJson = toolMetaBaseVO.getParams();
                if (StringUtils.isEmpty(toolParamJson)) {
                    return;
                }
                //原有参数
                String previousParamJson = toolConfigInfoEntity.getParamJson();
                JSONObject previousParamObj = StringUtils.isNotBlank(previousParamJson)
                        ? JSONObject.fromObject(previousParamJson) : new JSONObject();
                JSONArray toolParamsArray = new JSONArray(toolParamJson);
                for (int i = 0; i < toolParamsArray.length(); i++) {
                    org.json.JSONObject paramJsonObj = toolParamsArray.getJSONObject(i);
                    String varName = paramJsonObj.getString("varName");
                    String varValue = paramMap.get(varName);
                    if (StringUtils.isNotEmpty(varValue)) {
                        previousParamObj.put(varName, varValue);
                    }
                }
                toolConfigInfoEntity.setParamJson(previousParamObj.toString());
                toolConfigInfoEntity.setUpdatedBy(userName);
                toolConfigInfoEntity.setUpdatedDate(System.currentTimeMillis());
                toolRepository.save(toolConfigInfoEntity);
            });
        }
    }


    /**
     * 获取工具配置Map
     *
     * @param taskEntity
     * @return
     */
    @NotNull
    private Map<String, JSONObject> getToolConfigInfoMap(TaskInfoEntity taskEntity) {
        // 获取工具配置的值
        List<ToolConfigInfoEntity> toolConfigInfoList = taskEntity.getToolConfigInfoList();
        Map<String, JSONObject> chooseJsonMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(toolConfigInfoList)) {
            // 排除下架的工具
            toolConfigInfoList.stream()
                    .filter(config -> config.getFollowStatus() != FOLLOW_STATUS.WITHDRAW.value())
                    .forEach(config ->
                    {
                        String paramJson = config.getParamJson();
                        JSONObject params = new JSONObject();
                        if (StringUtils.isNotBlank(paramJson) && !ComConstants.STRING_NULL_ARRAY.equals(paramJson)) {
                            params = JSONObject.fromObject(paramJson);
                        }
                        chooseJsonMap.put(config.getToolName(), params);
                    });
        }
        return chooseJsonMap;
    }


    /**
     * 判断提交的参数是否为空
     *
     * @param taskUpdateVO
     * @return
     */
    private Boolean checkParam(TaskUpdateVO taskUpdateVO) {
        if (StringUtils.isBlank(taskUpdateVO.getNameCn())) {
            return false;
        }
        return taskUpdateVO.getCodeLang() > 0;
    }


    /**
     * 给工具分类及排序
     *
     * @param taskBaseVO
     * @param toolEntityList
     */
    private void sortedToolList(TaskBaseVO taskBaseVO, List<ToolConfigInfoEntity> toolEntityList) {
        // 如果工具不为空，对工具排序并且赋值工具展示名
        if (CollectionUtils.isNotEmpty(toolEntityList)) {
            List<ToolConfigBaseVO> enableToolList = new ArrayList<>();
            List<ToolConfigBaseVO> disableToolList = new ArrayList<>();

            List<String> toolIDArr = getToolOrders();
            for (String toolName : toolIDArr) {
                for (ToolConfigInfoEntity toolEntity : toolEntityList) {
                    if (toolName.equals(toolEntity.getToolName())) {
                        ToolConfigBaseVO toolConfigBaseVO = new ToolConfigBaseVO();
                        BeanUtils.copyProperties(toolEntity, toolConfigBaseVO);
                        ToolMetaBaseVO toolMetaBaseVO = toolMetaCache.getToolBaseMetaCache(toolName);
                        toolConfigBaseVO.setToolDisplayName(toolMetaBaseVO.getDisplayName());
                        toolConfigBaseVO.setToolPattern(toolMetaBaseVO.getPattern());
                        toolConfigBaseVO.setToolType(toolMetaBaseVO.getType());

                        // 加入规则集
                        if (toolEntity.getCheckerSet() != null) {
                            CheckerSetVO checkerSetVO = new CheckerSetVO();
                            BeanUtils.copyProperties(toolEntity.getCheckerSet(), checkerSetVO);
                            toolConfigBaseVO.setCheckerSet(checkerSetVO);
                        }

                        if (TaskConstants.FOLLOW_STATUS.WITHDRAW.value() == toolConfigBaseVO.getFollowStatus()) {
                            disableToolList.add(toolConfigBaseVO);
                        } else {
                            enableToolList.add(toolConfigBaseVO);
                        }
                    }
                }
            }

            taskBaseVO.setEnableToolList(enableToolList);
            taskBaseVO.setDisableToolList(disableToolList);
        }
    }


    /**
     * 重置工具的顺序，数据库中工具是按接入的先后顺序排序的，前端展示要按照工具类型排序
     *
     * @param toolNames
     * @param toolIdsOrder
     * @return
     */
    private String resetToolOrderByType(String toolNames, final String toolIdsOrder,
                                        List<ToolConfigInfoVO> unsortedToolList,
                                        List<ToolConfigInfoVO> sortedToolList) {
        if (StringUtils.isEmpty(toolNames)) {
            return null;
        }

        String[] toolNamesArr = toolNames.split(",");
        List<String> originToolList = Arrays.asList(toolNamesArr);
        String[] toolIDArr = toolIdsOrder.split(",");
        List<String> orderToolList = Arrays.asList(toolIDArr);
        Iterator<String> it = orderToolList.iterator();
        StringBuffer sb = new StringBuffer();
        while (it.hasNext()) {
            String toolId = it.next();
            if (originToolList.contains(toolId)) {
                sb.append(toolId).append(",");
                List<ToolConfigInfoVO> filteredList = unsortedToolList.stream().
                        filter(toolConfigInfoVO ->
                                toolId.equalsIgnoreCase(toolConfigInfoVO.getToolName())
                        ).
                        collect(Collectors.toList());

                sortedToolList.addAll(CollectionUtils.isNotEmpty(filteredList) ? filteredList : Collections.EMPTY_LIST);
            }
        }
        if (sb.length() > 0) {
            toolNames = sb.substring(0, sb.length() - 1);
        }

        return toolNames;
    }


    private TaskListVO sortByDate(List<TaskDetailVO> taskDetailVOS, TaskSortType taskSortType) {
        TaskListVO taskList = new TaskListVO(Collections.emptyList(), Collections.emptyList());
        List<TaskDetailVO> enableProjs = new ArrayList<>();
        List<TaskDetailVO> disableProjs = new ArrayList<>();
        for (TaskDetailVO taskDetailVO : taskDetailVOS) {
            if (!TaskConstants.TaskStatus.DISABLE.value().equals(taskDetailVO.getStatus())) {
                enableProjs.add(taskDetailVO);
            } else {
                disableProjs.add(taskDetailVO);
            }
        }
        if (CollectionUtils.isNotEmpty(taskDetailVOS)) {
            //分离已启用项目和停用项目

            //启用的项目按创建时间倒排,如果有置顶就放在最前面
            switch (taskSortType) {
                case CREATE_DATE:
                    enableProjs.sort((o1, o2) ->
                            o2.getTopFlag() - o1.getTopFlag() == 0 ?
                                    o2.getCreatedDate().compareTo(o1.getCreatedDate()) :
                                    o2.getTopFlag() - o1.getTopFlag()
                    );
                    break;
                case LAST_EXECUTE_DATE:
                    enableProjs.sort((o1, o2) ->
                            o2.getTopFlag() - o1.getTopFlag() == 0 ?
                                    o2.getMinStartTime().compareTo(o1.getMinStartTime()) :
                                    o2.getTopFlag() - o1.getTopFlag()
                    );
                    break;
                case SIMPLIFIED_PINYIN:
                    enableProjs.sort((o1, o2) ->
                            o2.getTopFlag() - o1.getTopFlag() == 0 ?
                                    Collator.getInstance(Locale.TRADITIONAL_CHINESE).compare(StringUtils.isNotBlank(o1.getNameCn()) ? o1.getNameCn() : o1.getNameEn(),
                                            StringUtils.isNotBlank(o2.getNameCn()) ? o2.getNameCn() : o2.getNameEn()) :
                                    o2.getTopFlag() - o1.getTopFlag()
                    );
                    break;
                default:
                    enableProjs.sort((o1, o2) ->
                            o2.getTopFlag() - o1.getTopFlag() == 0 ?
                                    o2.getCreatedDate().compareTo(o1.getCreatedDate()) :
                                    o2.getTopFlag() - o1.getTopFlag()
                    );
                    break;
            }


            //重建projectList
            taskList.setEnableTasks(enableProjs);
            taskList.setDisableTasks(disableProjs);
        }
        return taskList;
    }

    /**
     * 获取工具排序
     *
     * @return
     */
    private List<String> getToolOrders() {
        String toolIdsOrder = commonDao.getToolOrder();
        return List2StrUtil.fromString(toolIdsOrder, ComConstants.STRING_SPLIT);
    }

    /**
     * 获取工具特殊参数列表
     *
     * @param paramJsonStr
     * @return
     */
    private List<PipelineToolParamVO> getParams(String paramJsonStr) {
        List<PipelineToolParamVO> params = Lists.newArrayList();
        if (StringUtils.isNotEmpty(paramJsonStr)) {
            JSONObject paramJson = JSONObject.fromObject(paramJsonStr);
            if (paramJson != null && !paramJson.isNullObject()) {
                for (Object paramKeyObj : paramJson.keySet()) {
                    String paramKey = (String) paramKeyObj;
                    PipelineToolParamVO pipelineToolParamVO = new PipelineToolParamVO(paramKey,
                            paramJson.getString(paramKey));
                    params.add(pipelineToolParamVO);
                }
            }
        }
        return params;
    }


    /**
     * 根据条件获取任务基本信息清单
     *
     * @param taskListReqVO 请求体对象
     * @return list
     */
    @Override
    public TaskListVO getTaskDetailList(QueryTaskListReqVO taskListReqVO) {
        Integer taskStatus = taskListReqVO.getStatus();
        String toolName = taskListReqVO.getToolName();
        Integer bgId = taskListReqVO.getBgId();
        Integer deptId = taskListReqVO.getDeptId();
        List<String> createFrom = taskListReqVO.getCreateFrom();
        Boolean isExcludeTaskIds = Boolean.valueOf(taskListReqVO.getIsExcludeTaskIds());
        List<Long> taskIdsReq = Lists.newArrayList(taskListReqVO.getTaskIds());

        List<Integer> deptIds = null;
        if (deptId != null && deptId != 0) {
            deptIds = Lists.newArrayList(deptId);
        }

        TaskListVO taskList = new TaskListVO(Collections.emptyList(), Collections.emptyList());
        List<TaskDetailVO> tasks = new ArrayList<>();

        // 根据isExcludeTaskIds来判断参数taskIdsReq 的处理方式，来获取任务ID列表
        List<Long> queryTaskIds = getTaskIdListByFlag(toolName, isExcludeTaskIds, taskIdsReq);

        // 根据任务状态获取注册过该工具的任务列表
        List<TaskInfoEntity> taskInfoEntityList =
                taskDao.queryTaskInfoEntityList(taskStatus, bgId, deptIds, queryTaskIds, createFrom, null);
        if (CollectionUtils.isNotEmpty(taskInfoEntityList)) {
            taskInfoEntityList.forEach(entity ->
            {
                TaskDetailVO taskDetailVO = new TaskDetailVO();
                BeanUtils.copyProperties(entity, taskDetailVO);
                tasks.add(taskDetailVO);
            });
        }

        if (Status.ENABLE.value() == taskStatus) {
            taskList.setEnableTasks(tasks);
        } else {
            taskList.setDisableTasks(tasks);
        }
        return taskList;
    }

    /**
     * 根据isExcludeTaskIds来判断参数taskIdsReq 的处理方式，来获取任务ID列表
     *
     * @param toolName         工具名称
     * @param isExcludeTaskIds true: 排除taskIdsReq false: 从taskIdsReq排除
     * @param taskIdsReq       参数(任务ID列表)
     * @return task id list
     */
    @NotNull
    private List<Long> getTaskIdListByFlag(String toolName, Boolean isExcludeTaskIds, List<Long> taskIdsReq) {
        List<Long> queryTaskIds;
        if (BooleanUtils.isTrue(isExcludeTaskIds)) {
            List<Long> notWithdrawTasks = Lists.newArrayList();
            List<ToolConfigInfoEntity> toolConfigInfos =
                    toolRepository.findByToolNameAndFollowStatusNot(toolName, FOLLOW_STATUS.WITHDRAW.value());
            if (CollectionUtils.isNotEmpty(toolConfigInfos)) {
                toolConfigInfos.forEach(entity -> notWithdrawTasks.add(entity.getTaskId()));
            }
            // 剔除参数taskListReqVO的任务
            notWithdrawTasks.removeAll(taskIdsReq);
            queryTaskIds = notWithdrawTasks;
        } else {
            List<Long> withdrawTasks = Lists.newArrayList();
            List<ToolConfigInfoEntity> toolConfigInfos = toolRepository.findByToolNameAndFollowStatusIs(toolName,
                    FOLLOW_STATUS.WITHDRAW.value());
            if (CollectionUtils.isNotEmpty(toolConfigInfos)) {
                toolConfigInfos.forEach(entity -> withdrawTasks.add(entity.getTaskId()));
            }
            // 剔除已下架该工具的任务
            taskIdsReq.removeAll(withdrawTasks);
            queryTaskIds = taskIdsReq;
        }
        return queryTaskIds;
    }


    @Override
    public Page<TaskInfoVO> getTasksByAuthor(QueryMyTasksReqVO reqVO) {
        checkParam(reqVO);
        String repoUrl = reqVO.getRepoUrl();
        String branch = reqVO.getBranch();

        List<TaskInfoVO> tasks = Lists.newArrayList();

        List<TaskInfoEntity> allUserTasks =
                taskRepository.findTaskList(reqVO.getAuthor(), TaskConstants.TaskStatus.ENABLE.value());

        if (CollectionUtils.isNotEmpty(allUserTasks)) {
            Set<String> taskProjectIdList = Sets.newHashSet();
            allUserTasks.forEach(task -> {
                String bkProjectId = task.getProjectId();
                if (StringUtils.isNotEmpty(bkProjectId)) {
                    taskProjectIdList.add(bkProjectId);
                }
            });

            Map<String, RepoInfoVO> repoInfoVoMap = pipelineService.getRepoUrlByBkProjects(taskProjectIdList);
            String repoHashId = "";
            RepoInfoVO repoInfoVO = repoInfoVoMap.get(repoUrl);
            if (repoInfoVO != null) {
                repoHashId = repoInfoVO.getRepoHashId();
            }

            for (TaskInfoEntity task : allUserTasks) {
                // 过滤任务
                if (taskFilterIsTrue(branch, repoHashId, task)) {
                    continue;
                }

                TaskInfoVO taskInfoVO = new TaskInfoVO();
                taskInfoVO.setTaskId(task.getTaskId());
                taskInfoVO.setNameCn(task.getNameCn());
                taskInfoVO.setProjectId(task.getProjectId());

                List<String> tools = Lists.newArrayList();
                task.getToolConfigInfoList().forEach(toolInfo -> {
                    // 过滤掉已停用
                    if (toolInfo != null && toolInfo.getFollowStatus() != FOLLOW_STATUS.WITHDRAW.value()) {
                        tools.add(toolInfo.getToolName());
                    }
                });

                taskInfoVO.setToolNames(tools);
                tasks.add(taskInfoVO);
            }
        }

        return sortAndPage(reqVO.getPageNum(), reqVO.getPageSize(), reqVO.getSortType(), reqVO.getSortField(), tasks);
    }

    @Override
    public void updateReportInfo(Long taskId, NotifyCustomVO notifyCustomVO) {
        TaskInfoEntity taskInfoEntity = taskRepository.findFirstByTaskId(taskId);
        NotifyCustomEntity previousNofityEntity = taskInfoEntity.getNotifyCustomInfo();
        log.info("update report info from build, task id: {}, before: {}", taskId, previousNofityEntity);

        OperationType operationType;
        if (null != previousNofityEntity &&
                CollectionUtils.isNotEmpty(previousNofityEntity.getReportDate()) &&
                null != previousNofityEntity.getReportTime() &&
                CollectionUtils.isNotEmpty(previousNofityEntity.getReportTools())) {
            operationType = OperationType.RESCHEDULE;
        } else {
            operationType = OperationType.ADD;
        }

        NotifyCustomEntity notifyCustomEntity = new NotifyCustomEntity();
        BeanUtils.copyProperties(notifyCustomVO, notifyCustomEntity);
        //如果定时任务信息不为空，则与定时调度平台通信
        if (CollectionUtils.isNotEmpty(notifyCustomVO.getReportDate()) &&
                null != notifyCustomVO.getReportTime() &&
                CollectionUtils.isNotEmpty(notifyCustomVO.getReportTools())) {
            String jobName = emailNotifyService.addEmailScheduleTask(taskId, notifyCustomVO.getReportDate(),
                    notifyCustomVO.getReportTime(), operationType, null == previousNofityEntity ? null :
                            previousNofityEntity.getReportJobName());
            notifyCustomEntity.setReportJobName(jobName);
        }

        log.info("update report info from build, task id: {}, after: {}", taskId, notifyCustomEntity);
        taskInfoEntity.setNotifyCustomInfo(notifyCustomEntity);
        taskRepository.save(taskInfoEntity);
    }

    @Override
    public Boolean updateTopUserInfo(Long taskId, String user, Boolean topFlag) {
        TaskInfoEntity taskInfoEntity = taskRepository.findFirstByTaskId(taskId);
        if (null == taskInfoEntity) {
            return false;
        }
        Set<String> topUser = taskInfoEntity.getTopUser();
        //如果是置顶操作
        if (topFlag) {
            if (CollectionUtils.isEmpty(topUser)) {
                taskInfoEntity.setTopUser(new HashSet<String>() {{
                    add(user);
                }});
            } else {
                topUser.add(user);
            }
        }
        //如果是取消置顶操作
        else {
            if (CollectionUtils.isEmpty(topUser)) {
                log.error("top user list is empty! task id: {}", taskId);
                return false;
            } else {
                topUser.remove(user);
            }
        }

        taskRepository.save(taskInfoEntity);
        return true;
    }

    @Override
    public TaskInfoEntity getTaskByGongfengId(Integer gongfengProjectId) {
        return taskRepository.findFirstByGongfengProjectId(gongfengProjectId);
    }

    @Override
    public List<TaskDetailVO> getTaskInfoList(QueryTaskListReqVO taskListReqVO) {
        List<TaskInfoEntity> taskInfoEntityList =
                taskDao.queryTaskInfoEntityList(taskListReqVO.getStatus(), taskListReqVO.getBgId(),
                        taskListReqVO.getDeptIds(), taskListReqVO.getTaskIds(), taskListReqVO.getCreateFrom(), taskListReqVO.getUserId());

        return entities2TaskDetailVoList(taskInfoEntityList);
    }

    @Override
    public Page<TaskDetailVO> getTaskDetailPage(@NotNull QueryTaskListReqVO reqVO) {
        Sort.Direction direction = Sort.Direction.valueOf(reqVO.getSortType());
        Pageable pageable = PageableUtils
                .getPageable(reqVO.getPageNum(), reqVO.getPageSize(), reqVO.getSortField(), direction, "task_id");

        org.springframework.data.domain.Page<TaskInfoEntity> entityPage = taskRepository
                .findByStatusAndBgIdAndDeptIdInAndCreateFromIn(reqVO.getStatus(), reqVO.getBgId(), reqVO.getDeptIds(),
                        reqVO.getCreateFrom(), pageable);
        List<TaskInfoEntity> taskInfoEntityList = entityPage.getContent();

        List<TaskDetailVO> taskInfoList = entities2TaskDetailVoList(taskInfoEntityList);

        // 页码+1展示
        return new Page<>(entityPage.getTotalElements(), entityPage.getNumber() + 1, entityPage.getSize(),
                entityPage.getTotalPages(), taskInfoList);
    }

    @NotNull
    private List<TaskDetailVO> entities2TaskDetailVoList(List<TaskInfoEntity> taskInfoEntityList) {
        List<TaskDetailVO> taskInfoList = Lists.newArrayList();
        if (CollectionUtils.isNotEmpty(taskInfoEntityList)) {
            taskInfoList = taskInfoEntityList.stream().map(taskInfoEntity ->
            {
                TaskDetailVO taskDetailVO = new TaskDetailVO();
                BeanUtils.copyProperties(taskInfoEntity, taskDetailVO);
                return taskDetailVO;
            }).collect(Collectors.toList());
        }
        return taskInfoList;
    }


    private boolean taskFilterIsTrue(String branch, String repoHashId, TaskInfoEntity task) {
        // 如果不是工蜂代码扫描创建的任务
        String createFrom = task.getCreateFrom();
        if (!BsTaskCreateFrom.GONGFENG_SCAN.value().equals(createFrom)) {
            // 过滤代码库不匹配的
            String taskRepoHashId = task.getRepoHashId();
            if (StringUtils.isBlank(taskRepoHashId) || !taskRepoHashId.equals(repoHashId)) {
                return true;
            }
            // 过滤分支不符合的，参数branch为null则不检查
            if (branch != null && !branch.equals(task.getBranch())) {
                return true;
            }
        }
        // 过滤未添加工具的
        return task.getToolConfigInfoList() == null;
    }


    /**
     * 检查参数并赋默认值
     *
     * @param reqVO req
     */
    private void checkParam(QueryMyTasksReqVO reqVO) {
        if (reqVO.getPageNum() == null) {
            reqVO.setPageNum(1);
        }

        if (reqVO.getPageSize() == null) {
            reqVO.setPageSize(10);
        }

        if (reqVO.getSortField() == null) {
            reqVO.setSortField("taskId");
        }
    }

    private void setEmptyAnalyze(TaskInfoEntity taskEntity, TaskOverviewVO taskOverviewVO) {
        List<LastAnalysis> lastAnalyses = new ArrayList<>();
        if (taskEntity.getToolConfigInfoList() != null) {
            taskEntity.getToolConfigInfoList().forEach(toolConfigInfoEntity -> {
                LastAnalysis lastAnalysis = new LastAnalysis();
                lastAnalysis.setToolName(toolConfigInfoEntity.getToolName());
                lastAnalyses.add(lastAnalysis);
            });
        }
        taskOverviewVO.setLastAnalysisResultList(lastAnalyses);
    }


    @NotNull
    private Page<TaskInfoVO> sortAndPage(int pageNum, int pageSize, String sortType, String sortField,
                                         List<TaskInfoVO> tasks) {
        if (!Sort.Direction.ASC.name().equalsIgnoreCase(sortType)) {
            sortType = Sort.Direction.DESC.name();
        }
        ListSortUtil.sort(tasks, sortField, sortType);

        int totalPageNum = 0;
        int total = tasks.size();
        pageNum = pageNum - 1 < 0 ? 0 : pageNum - 1;
        pageSize = pageSize <= 0 ? 10 : pageSize;
        if (total > 0) {
            totalPageNum = (total + pageSize - 1) / pageSize;
        }

        int subListBeginIdx = pageNum * pageSize;
        int subListEndIdx = subListBeginIdx + pageSize;
        if (subListBeginIdx > total) {
            subListBeginIdx = 0;
        }
        List<TaskInfoVO> taskInfoVoList = tasks.subList(subListBeginIdx, subListEndIdx > total ? total : subListEndIdx);

        return new Page<>(total, pageNum == 0 ? 1 : pageNum, pageSize, totalPageNum, taskInfoVoList);
    }


    @Override
    public Set<Integer> queryDeptIdByBgId(Integer bgId) {
        // 指定工蜂扫描的部门ID
        List<TaskInfoEntity> deptIdList = taskDao.queryDeptId(bgId, BsTaskCreateFrom.GONGFENG_SCAN.value());

        return deptIdList.stream().filter(elem -> elem.getDeptId() > 0).map(TaskInfoEntity::getDeptId)
                .collect(Collectors.toSet());
    }

    @Override
    public Boolean refreshTaskOrgInfo(Long taskId) {
        boolean result = false;
        TaskInfoEntity taskInfoEntity = taskRepository.findFirstByTaskId(taskId);
        if (taskInfoEntity == null) {
            log.error("refreshTaskOrgInfo infoEntity is not found: {}", taskId);
            return false;
        }

        DevopsProjectOrgVO devopsProjectOrg = userManageService.getDevopsProjectOrg(taskInfoEntity.getProjectId());
        if (devopsProjectOrg == null) {
            devopsProjectOrg = new DevopsProjectOrgVO();
        }
        Integer bgId = devopsProjectOrg.getBgId();
        if (bgId == null || bgId <= 0) {
            TofStaffInfo staffInfo =
                    tofClientApi.getStaffInfoByUserName(taskInfoEntity.getTaskOwner().get(0)).getData();
            if (staffInfo == null) {
                log.error("getStaffInfoByUserName is null: {}", taskId);
                return false;
            }
            TofOrganizationInfo orgInfo = tofClientApi.getOrganizationInfoByGroupId(staffInfo.getGroupId());
            if (orgInfo == null) {
                log.error("getOrganizationInfoByGroupId is null: {}", taskId);
                return false;
            }
            devopsProjectOrg.setBgId(orgInfo.getBgId());
            devopsProjectOrg.setDeptId(orgInfo.getDeptId());
            devopsProjectOrg.setCenterId(orgInfo.getCenterId());
        }

        taskInfoEntity.setBgId(devopsProjectOrg.getBgId());
        taskInfoEntity.setDeptId(devopsProjectOrg.getDeptId());
        taskInfoEntity.setCenterId(devopsProjectOrg.getCenterId());
        result = taskDao.updateOrgInfo(taskInfoEntity);

        return result;
    }

    @Override
    public void updateTaskOwnerAndMember(TaskOwnerAndMemberVO taskOwnerAndMemberVO, Long taskId) {
        TaskInfoEntity taskInfoEntity = taskRepository.findFirstByTaskId(taskId);
        if (null == taskInfoEntity) {
            return;
        }
        taskInfoEntity.setTaskMember(taskOwnerAndMemberVO.getTaskMember());
        taskInfoEntity.setTaskOwner(taskOwnerAndMemberVO.getTaskOwner());
        taskRepository.save(taskInfoEntity);
    }

    @Override
    public List<Long> getBkPluginTaskIds() {
        Map<String, Object> params = new HashMap<>();
        params.put("status", Status.ENABLE.value());
        params.put("project_id", "CUSTOMPROJ_TEG_CUSTOMIZED");

        Map<String, Object> nParams = new HashMap<>();
        nParams.put("gongfeng_project_id", null);
        List<TaskInfoEntity> openSourceTaskList = taskDao.queryTaskInfoByCustomParam(params, nParams);

        log.info("bk plugin tasks {}", openSourceTaskList.size());

        Map<Integer, List<TaskInfoEntity>> proMap = openSourceTaskList.stream()
                .collect(Collectors.groupingBy(TaskInfoEntity::getGongfengProjectId));
        List<GongfengPublicProjEntity> projEntityList = gongfengPublicProjRepository.findByIdIn(proMap.keySet());
        List<Long> taskIds = Lists.newArrayList();
        projEntityList.stream()
                .filter(gongfengPublicProjEntity -> StringUtils.isNotBlank(gongfengPublicProjEntity.getHttpUrlToRepo())
                        && gongfengPublicProjEntity.getHttpUrlToRepo().contains("/bkdevops-plugins/"))
                .forEach(gongfengPublicProjEntity -> taskIds.add(proMap.get(gongfengPublicProjEntity.getId())
                        .get(0).getTaskId()));

        log.info("bk plugin gongfeng tasks {}", taskIds.size());
        return taskIds;
    }

    @Override
    public Boolean triggerBkPluginScoring() {
        rabbitTemplate.convertAndSend(EXCHANGE_SCORING_OPENSOURCE, ROUTE_SCORING_OPENSOURCE, "");
        return Boolean.TRUE;
    }

    @Override
    public List<MetadataVO> listTaskToolDimension(Long taskId) {
        TaskInfoEntity taskInfo = taskRepository.findFirstByTaskId(taskId);
        Map<String, MetadataVO> resultMap = new HashMap<>();
        taskInfo.getToolConfigInfoList().forEach(it -> {
            try {
                BaseDataEntity toolTypeBaseData = toolTypeBaseDataCache.get(it.getToolName());
                if (toolTypeBaseData != null) {
                    MetadataVO metadataVO = new MetadataVO();
                    metadataVO.setKey(toolTypeBaseData.getParamCode());
                    metadataVO.setName(toolTypeBaseData.getParamName());
                    metadataVO.setFullName(toolTypeBaseData.getParamExtend1());
                    resultMap.put(toolTypeBaseData.getParamCode(), metadataVO);
                }
            } catch (ExecutionException e) {
                throw new CodeCCException("list task tool dimension for task fail: " + taskId + ", " + e.getMessage());
            }
        });

        return new ArrayList<>(resultMap.values());
    }

    @Override
    public TaskDetailVO getTaskInfoByAliasName(String aliasName) {
        // 为了防止同名插件删除后又创建，aliasName并不准确，使用repoId数字ID查询
        GongfengPublicProjModel gongfengModel;
        try {
            aliasName = URLDecoder.decode(aliasName, "UTF-8");
            gongfengModel = getGongfengProjectInfo(aliasName);
        } catch (UnsupportedEncodingException e) {
            log.error("", e);
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{"代码库别名转码失败"});
        }

        if (gongfengModel == null) {
            log.error("repo is not exists: {}", aliasName);
            return null;
        }
        return getTaskInfoByGongfengId(gongfengModel.getId(), gongfengModel);
    }

    @Override
    public TaskDetailVO getTaskInfoByGongfengId(int id, GongfengPublicProjModel gongfengPublicProjModel) {
        GongfengPublicProjModel gongfengModel = gongfengPublicProjModel;
        if (gongfengModel == null) {
            try {
                gongfengModel = getGongfengProjectInfo(String.valueOf(id));
            } catch (UnsupportedEncodingException e) {
                log.error("", e);
                throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{"代码库信息获取失败"});
            }
        }

        if (gongfengModel == null || gongfengModel.getVisibilityLevel() == null) {
            log.error("repo is not exists: {}", id);
            return null;
        }

        //修改task获取逻辑
        TaskInfoEntity taskInfoEntity = taskRepository.findFirstByGongfengProjectIdAndStatusAndProjectIdRegex(
                gongfengModel.getId(), TaskConstants.TaskStatus.ENABLE.value(), "^(CODE_)");
        if (null == taskInfoEntity || taskInfoEntity.getTaskId() == 0L) {
            CustomProjEntity customProjEntity = customProjRepository.findFirstByGongfengProjectIdAndCustomProjSource(
                    gongfengModel.getId(), "TEG_CUSTOMIZED");
            if (null != customProjEntity && StringUtils.isNotBlank(customProjEntity.getPipelineId())) {
                taskInfoEntity = taskRepository.findFirstByPipelineId(customProjEntity.getPipelineId());
            }
            if (null == taskInfoEntity || taskInfoEntity.getTaskId() == 0L || !TaskConstants.TaskStatus.ENABLE.value()
                    .equals(taskInfoEntity.getStatus())) {
                customProjEntity = customProjRepository
                        .findFirstByGongfengProjectIdAndCustomProjSource(
                                gongfengModel.getId(), "bkdevops-plugins");
                if (null != customProjEntity && StringUtils.isNotBlank(customProjEntity.getPipelineId())) {
                    taskInfoEntity = taskRepository.findFirstByPipelineId(customProjEntity.getPipelineId());
                }
            }
        }

        if (taskInfoEntity == null) {
            log.info("get task by alias, task is null");
            return null;
        }

        TaskDetailVO taskDetailVO = new TaskDetailVO();
        BeanUtils.copyProperties(taskInfoEntity, taskDetailVO);
        if (taskInfoEntity.getToolConfigInfoList() != null) {
            List<ToolConfigInfoVO> toolConfigInfoVOList =
                    new ArrayList<>(taskInfoEntity.getToolConfigInfoList().size());
            taskInfoEntity.getToolConfigInfoList().stream()
                    .filter(toolConfigInfoEntity ->
                            toolConfigInfoEntity.getFollowStatus() != FOLLOW_STATUS.WITHDRAW.value())
                    .forEach(toolConfigInfoEntity -> {
                        ToolConfigInfoVO toolConfigInfoVO = new ToolConfigInfoVO();
                        BeanUtils.copyProperties(toolConfigInfoEntity, toolConfigInfoVO);
                        toolConfigInfoVOList.add(toolConfigInfoVO);
                    });
            taskDetailVO.setToolConfigInfoList(toolConfigInfoVOList);
        }
        return taskDetailVO;
    }

    /**
     * 通过repoId获取代码库详细信息
     */
    private GongfengPublicProjModel getGongfengProjectInfo(String repoId) throws UnsupportedEncodingException {
        String url = gitCodePath + "/api/v3/projects/" + URLEncoder.encode(repoId, "UTF-8");

        //从工蜂拉取信息，并按分页下发
        String result = OkhttpUtils.INSTANCE.doGet(url, Collections.singletonMap("PRIVATE-TOKEN", gitPrivateToken));
        if (StringUtils.isBlank(result)) {
            log.info("null returned from api");
            return null;
        }
        return JsonUtil.INSTANCE.to(result, GongfengPublicProjModel.class);
    }

    @Override
    public List<Long> queryTaskIdByCreateFrom(List<String> taskCreateFrom) {
        List<TaskInfoEntity> taskInfoEntityList =
                taskRepository.findByStatusAndCreateFromIn(Status.ENABLE.value(), taskCreateFrom);
        return taskInfoEntityList.stream().map(TaskInfoEntity::getTaskId).collect(Collectors.toList());
    }

    /**
     * 获取开源或非开源的有效任务ID
     *
     * @param defectStatType enum
     * @return list
     */
    @Override
    public List<Long> queryTaskIdByType(@NotNull ComConstants.DefectStatType defectStatType) {
        List<String> createFrom;
        if (ComConstants.DefectStatType.GONGFENG_SCAN.value().equals(defectStatType.value())) {
            createFrom = Lists.newArrayList(BsTaskCreateFrom.GONGFENG_SCAN.value());
        } else {
            createFrom = Lists.newArrayList(BsTaskCreateFrom.BS_CODECC.value(), BsTaskCreateFrom.BS_PIPELINE.value());

        }
        return queryTaskIdByCreateFrom(createFrom);
    }

    /**
     * 初始化获取任务数量
     *
     * @param day 天数
     * @return
     */
    @Override
    public Boolean initTaskCountScript(Integer day) {
        // 获取日期
        List<String> dates = DateTimeUtils.getBeforeDaily(day);
        // 获取每天对应的任务数量
        List<TaskStatisticEntity> taskCountData = Lists.newArrayList();
        for (String date : dates) {
            try {
                // 获取开源任务数量
                getTaskCount(taskCountData, date, ComConstants.DefectStatType.GONGFENG_SCAN.value());
                // 获取非开源任务数量
                getTaskCount(taskCountData, date, ComConstants.DefectStatType.USER.value());
            } catch (Exception e) {
                log.error("Failed to obtain task data: {}", date, e);
            }
        }
        taskStatisticRepository.saveAll(taskCountData);
        return true;
    }

    /**
     * 获取任务数量
     *
     * @param taskCountData 容器
     * @param date          时间(string)
     * @param createFrom    来源
     */
    private void getTaskCount(@NotNull List<TaskStatisticEntity> taskCountData, String date, String createFrom) {
        // 获取结束时间
        long[] startTimeAndEndTime = DateTimeUtils.getStartTimeAndEndTime(date, date);
        TaskStatisticEntity taskStatisticEntity = new TaskStatisticEntity();
        taskStatisticEntity.setDataFrom(createFrom);
        taskStatisticEntity.setDate(date);
        // 根据结束时间、来源获取任务数量
        Long count = taskDao.findDailyTaskCount(startTimeAndEndTime[1], createFrom);
        taskStatisticEntity.setTaskCount(count.intValue());

        List<Long> taskIdList;
        if (createFrom.equals(ComConstants.DefectStatType.GONGFENG_SCAN.value())) {
            taskIdList = taskService.queryTaskIdByCreateFrom(Collections.singletonList(createFrom));
        } else {
            taskIdList = taskService.queryTaskIdByCreateFrom(
                    Lists.newArrayList(BsTaskCreateFrom.BS_CODECC.value(), BsTaskCreateFrom.BS_PIPELINE.value()));
        }
        // 获取分析次数 封装请求体
        QueryTaskListReqVO queryTaskListReqVO = new QueryTaskListReqVO();
        queryTaskListReqVO.setStartTime(startTimeAndEndTime[0]);
        queryTaskListReqVO.setEndTime(startTimeAndEndTime[1]);
        queryTaskListReqVO.setTaskIds(taskIdList);
        // 调用接口获取分析次数
        Integer taskAnalyzeCount =
                client.get(ServiceTaskLogOverviewResource.class).getTaskAnalyzeCount(queryTaskListReqVO).getData();
        taskStatisticEntity.setAnalyzeCount(taskAnalyzeCount != null ? taskAnalyzeCount : 0);
        taskCountData.add(taskStatisticEntity);
    }

    @Override
    public TaskDetailVO getTaskInfoWithoutToolsByStreamName(String nameEn) {

        TaskInfoEntity taskEntity = taskRepository.findFirstByNameEn(nameEn);
        if (taskEntity == null) {
            log.error("can not find task by streamName: {}", nameEn);
            throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS, new String[]{nameEn}, null);
        }

        TaskDetailVO taskDetailVO = new TaskDetailVO();
        BeanUtils.copyProperties(taskEntity, taskDetailVO);
        return taskDetailVO;
    }

    /**
     * 根据任务类型设置任务类型和创建来源
     * 需要 BS_PIPELINE / BS_CODECC / GONGFENG_SCAN
     * 对于工蜂扫描任务需要区分 API 任务和 定时扫描任务，API触发任务给出相应的 apCode
     *
     * @param taskDetailVO
     */
    private void setTaskCreateInfo(TaskDetailVO taskDetailVO) {
        if (taskDetailVO.getCreateFrom().equals(BsTaskCreateFrom.GONGFENG_SCAN.value())) {
            // 如果是 OTEAM 项目的话，设置为和定时触发一样的开源治理项目
            if (taskDetailVO.getProjectId().equals("CUSTOMPROJ_TEG_CUSTOMIZED")) {
                taskDetailVO.setTaskType(BsTaskCreateFrom.TIMING_SCAN.value());
                taskDetailVO.setCreateSource(taskDetailVO.getCreatedBy());
                return;
            }
            CustomProjEntity customProjEntity;
            if (taskDetailVO.getPipelineId() != null) {
                customProjEntity = customProjRepository.findFirstByPipelineId(taskDetailVO.getPipelineId());
            } else {
                customProjEntity = customProjRepository.findFirstByTaskId(taskDetailVO.getTaskId());
            }

            // 如果不是 OTEAM 项目并且是私有API触发项目，则设置为API触发项目
            if (customProjEntity != null) {
                taskDetailVO.setTaskType(BsTaskCreateFrom.API_TRIGGER.value());
                taskDetailVO.setCreateSource(taskDetailVO.getCreatedBy());
                if (StringUtils.isNotBlank(customProjEntity.getAppCode())) {
                    taskDetailVO.setCreateSource(customProjEntity.getAppCode());
                }
                return;
            }

            // 如果是开源项目，则设置为定时触发的开源治理项目
            taskDetailVO.setTaskType(BsTaskCreateFrom.TIMING_SCAN.value());
            boolean isPublicProj =
                    gongfengPublicProjRepository.existsById(taskDetailVO.getGongfengProjectId());
            if (!isPublicProj) {
                log.error("invalid gongfeng scan task: {}", taskDetailVO.getTaskId());
            }
            return;
        }

        //如果是灰度项目，则要显示api触发来源
        if (StringUtils.isNotBlank(taskDetailVO.getProjectId())
                && taskDetailVO.getProjectId().startsWith(ComConstants.GRAY_PROJECT_PREFIX)) {
            taskDetailVO.setTaskType(BsTaskCreateFrom.API_TRIGGER.value());
            taskDetailVO.setCreateSource(taskDetailVO.getCreatedBy());
            return;
        }

        // 设置为非工蜂项目
        taskDetailVO.setTaskType(taskDetailVO.getCreateFrom());
        taskDetailVO.setCreateSource(taskDetailVO.getCreatedBy());
    }

    /**
     * 获取代码库 路径/别名 和 分支名称
     *
     * @param taskId
     */
    @Override
    public TaskCodeLibraryVO getRepoInfo(Long taskId) {
        TaskInfoEntity taskEntity = taskRepository.findFirstByTaskId(taskId);
        if (Objects.isNull(taskEntity)) {
            log.error("taskInfo not exists! task id is: {}", taskId);
            throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS,
                    new String[]{String.valueOf(taskId)}, null);
        }

        TaskCodeLibraryVO taskCodeLibrary = new TaskCodeLibraryVO();

        if (taskEntity.getCreateFrom().equals(BsTaskCreateFrom.BS_PIPELINE.value())) {
            log.info("task create from pipeline: {}", taskId);
            setPipelineRepoInfo(taskCodeLibrary, taskId);
        } else if (taskEntity.getCreateFrom().equals(BsTaskCreateFrom.BS_CODECC.value())) {
            log.info("task create from codecc: {}", taskId);
            /*
             * 当create_from是codecc时，需要区分两种情况
             * 1. 如果是灰度池中的项目，则需要取代码库信息，获取方式和开源项目一样
             * 2. 如果是一般项目，则按照原来逻辑进行
             */
            if (!taskEntity.getProjectId().startsWith(ComConstants.GRAY_PROJECT_PREFIX)) {
                taskCodeLibrary.setCodeInfo(Collections.singletonList(
                        new CodeLibraryInfoVO("",
                                taskEntity.getAliasName(),
                                taskEntity.getBranch())));
            } else {
                setGongfengRepoInfo(taskCodeLibrary, taskEntity);
            }

        } else if (taskEntity.getCreateFrom().equals(BsTaskCreateFrom.GONGFENG_SCAN.value())) {
            // 对于工蜂扫描任务还需要区分是api创建任务还是定时任务
            log.info("task create from gongfeng scan: {}", taskId);
            setGongfengRepoInfo(taskCodeLibrary, taskEntity);
        } else {
            log.error("invalid task: {}", taskId);
        }

        return taskCodeLibrary;
    }

    @Override
    public boolean addWhitePath(long taskId, List<String> pathList) {
        TaskInfoEntity taskInfoEntity = taskRepository.findFirstByTaskId(taskId);
        boolean flag = isListEqualsExpectOrder(pathList, taskInfoEntity.getWhitePaths());

        if (!flag && taskDao.upsertPathOfTask(taskId, pathList)) {
            // 设置强制全量扫描标志
            setForceFullScan(taskInfoEntity);
            return true;
        }
        return false;
    }

    private boolean isListEqualsExpectOrder(List<?> l1, List<?> l2) {
        if (l1 == null || l2 == null) {
            return l1 == l2;
        }
        return (l1.containsAll(l2) && l2.containsAll(l1));
    }

    /**
     * 设置工蜂扫描任务的代码库信息
     * 工蜂扫描包含开源扫描任务和 API触发任务，需要做区分
     *
     * @param taskCodeLibraryVO
     * @param taskInfoEntity
     */
    private void setGongfengRepoInfo(TaskCodeLibraryVO taskCodeLibraryVO,
                                     TaskInfoEntity taskInfoEntity) {

        CustomProjEntity customProjEntity =
                customProjRepository.findFirstByPipelineId(taskInfoEntity.getPipelineId());
        if (customProjEntity != null) {
            log.info("gongfeng task create from API: {}", taskInfoEntity.getTaskId());
            // APi创建处理逻辑
            taskCodeLibraryVO.setCodeInfo(
                    Collections.singletonList(
                            new CodeLibraryInfoVO(customProjEntity.getUrl(),
                                    pickupAliasNameFromUrl(customProjEntity.getUrl()),
                                    customProjEntity.getBranch())));
        } else {
            log.info("gongfeng task create from open source: {}", taskInfoEntity.getTaskId());
            GongfengPublicProjEntity publicProjEntity
                    = gongfengPublicProjRepository.findFirstById(taskInfoEntity.getGongfengProjectId());

            // 如果存在于开源表中，是定时触发任务，否则代表任务不合法
            if (publicProjEntity != null) {
                taskCodeLibraryVO.setCodeInfo(Collections.singletonList(
                        new CodeLibraryInfoVO(
                                publicProjEntity.getHttpsUrlToRepo(),
                                pickupAliasNameFromUrl(publicProjEntity.getHttpsUrlToRepo()),
                                publicProjEntity.getDefaultBranch())));
            }
        }
    }

    /**
     * 设置流水线创建任务的代码库信息
     * 流水线代码库信息不存在于 Task 表，需要从构建记录中解析
     *
     * @param taskId
     * @param taskCodeLibrary
     */
    private void setPipelineRepoInfo(TaskCodeLibraryVO taskCodeLibrary, long taskId) {
        Result<Map<String, TaskLogRepoInfoVO>> res = client.get(ServiceTaskLogRestResource.class)
                .getLastAnalyzeRepoInfo(taskId);
        if (res == null || res.isNotOk() || res.getData() == null) {
            log.error("fail to get last analyze repoInfo, taskId: {}", taskId);
            return;
        }

        List<CodeLibraryInfoVO> codeLibraryInfoVOList = new ArrayList<>();
        Map<String, TaskLogRepoInfoVO> repoInfo = res.getData();
        repoInfo.keySet()
                .stream()
                .filter(repoUrl -> StringUtils.isNotBlank(repoUrl)
                        && repoInfo.get(repoUrl) != null
                        && StringUtils.isNotBlank(repoInfo.get(repoUrl).getBranch()))
                .forEach(repoUrl -> {
                    codeLibraryInfoVOList.add(
                            new CodeLibraryInfoVO(
                                    repoUrl,
                                    pickupAliasNameFromUrl(repoUrl),
                                    repoInfo.get(repoUrl).getBranch()
                            )
                    );
                });
        taskCodeLibrary.setCodeInfo(codeLibraryInfoVOList);
    }

    /**
     * 从 Git / SVN 代码库URL中提取代码库别名
     * 不限于 HTTP 协议
     *
     * @param url
     */
    private String pickupAliasNameFromUrl(String url) {
        return url.replaceFirst("(.*)://[^/]+/", "")
                .replaceFirst("\\.[a-zA-Z]*(/)?", "");
    }

    private TaskInfoEntity getTaskFromPublic(int id, boolean isFindCustom) {
        log.info("execute opensource trigger pipeline by repoId $id");
        GongfengPublicProjEntity proj = gongfengPublicProjRepository.findFirstById(id);
        if (proj == null) {
            if (!isFindCustom) {
                return getTaskFromCustom(id, true);
            } else {
                throw new CodeCCException("2300020");
            }
        }

        TaskInfoEntity taskInfoEntity = taskRepository.findFirstByGongfengProjectIdAndStatusAndProjectIdRegex(
                proj.getId(),
                TaskConstants.TaskStatus.ENABLE.value(),
                "^(CODE_)"
        );

        if (taskInfoEntity == null && !isFindCustom) {
            return getTaskFromCustom(id, true);
        } else {
            return taskInfoEntity;
        }
    }

    private TaskInfoEntity getTaskFromCustom(int id, boolean isFindPublic) {
        log.info("get custom task to trigger pipeline by repoId $id");
        CustomProjEntity proj = customProjRepository.findFirstByGongfengProjectIdAndCustomProjSource(
                id,
                "bkdevops-plugins"
        );

        if (proj == null) {
            proj = customProjRepository.findFirstByGongfengProjectIdAndCustomProjSource(
                    id,
                    "codecc"
            );
        }

        if (proj == null) {
            log.error("task has been not created: repoId: $id");
            if (!isFindPublic) {
                return getTaskFromPublic(id, true);
            } else {
                throw new CodeCCException("2300020");
            }
        }

        TaskInfoEntity taskInfoEntity = taskRepository.findFirstByTaskIdAndStatus(
                proj.getTaskId(),
                TaskConstants.TaskStatus.ENABLE.value()
        );

        if (taskInfoEntity == null && !isFindPublic) {
            return getTaskFromPublic(id, true);
        } else {
            return taskInfoEntity;
        }

    }

}
