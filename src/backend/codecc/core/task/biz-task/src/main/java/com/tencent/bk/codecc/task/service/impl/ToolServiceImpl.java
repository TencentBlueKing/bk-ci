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

package com.tencent.bk.codecc.task.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.api.ServiceCheckerRestResource;
import com.tencent.bk.codecc.defect.api.ServiceToolBuildInfoResource;
import com.tencent.bk.codecc.defect.vo.checkerset.AddCheckerSet2TaskReqVO;
import com.tencent.bk.codecc.task.constant.TaskConstants;
import com.tencent.bk.codecc.task.constant.TaskMessageCode;
import com.tencent.bk.codecc.task.dao.mongorepository.TaskRepository;
import com.tencent.bk.codecc.task.dao.mongorepository.ToolRepository;
import com.tencent.bk.codecc.task.dao.mongorepository.ToolStatisticRepository;
import com.tencent.bk.codecc.task.dao.mongotemplate.TaskDao;
import com.tencent.bk.codecc.task.dao.mongotemplate.ToolDao;
import com.tencent.bk.codecc.task.model.TaskInfoEntity;
import com.tencent.bk.codecc.task.model.ToolCheckerSetEntity;
import com.tencent.bk.codecc.task.model.ToolConfigInfoEntity;
import com.tencent.bk.codecc.task.model.ToolCountScriptEntity;
import com.tencent.bk.codecc.task.model.ToolStatisticEntity;
import com.tencent.bk.codecc.task.service.IRegisterToolBizService;
import com.tencent.bk.codecc.task.service.PipelineService;
import com.tencent.bk.codecc.task.service.PlatformService;
import com.tencent.bk.codecc.task.service.TaskService;
import com.tencent.bk.codecc.task.service.ToolService;
import com.tencent.bk.codecc.task.service.specialparam.SpecialParamUtil;
import com.tencent.bk.codecc.task.vo.BatchRegisterVO;
import com.tencent.bk.codecc.task.vo.ParamJsonAndCheckerSetsVO;
import com.tencent.bk.codecc.task.vo.PlatformVO;
import com.tencent.bk.codecc.task.vo.TaskUpdateVO;
import com.tencent.bk.codecc.task.vo.ToolConfigBaseVO;
import com.tencent.bk.codecc.task.vo.ToolConfigInfoVO;
import com.tencent.bk.codecc.task.vo.ToolConfigInfoWithMetadataVO;
import com.tencent.bk.codecc.task.vo.ToolConfigPlatformVO;
import com.tencent.bk.codecc.task.vo.ToolParamJsonAndCheckerSetVO;
import com.tencent.bk.codecc.task.vo.checkerset.ToolCheckerSetVO;
import com.tencent.devops.common.api.QueryTaskListReqVO;
import com.tencent.devops.common.api.ToolMetaBaseVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.auth.api.external.AuthExRegisterApi;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.service.BizServiceFactory;
import com.tencent.devops.common.service.ToolMetaCacheService;
import com.tencent.devops.common.service.utils.PageableUtils;
import com.tencent.devops.common.util.DateTimeUtils;
import com.tencent.devops.common.util.List2StrUtil;
import com.tencent.devops.common.web.aop.annotation.OperationHistory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.tencent.devops.common.util.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tencent.devops.common.constant.ComConstants.BsTaskCreateFrom;
import static com.tencent.devops.common.constant.ComConstants.CommonJudge;
import static com.tencent.devops.common.constant.ComConstants.DefectStatType;
import static com.tencent.devops.common.constant.ComConstants.FOLLOW_STATUS;
import static com.tencent.devops.common.constant.ComConstants.FUNC_TOOL_SWITCH;
import static com.tencent.devops.common.constant.ComConstants.PipelineToolUpdateType;
import static com.tencent.devops.common.constant.ComConstants.Status;
import static com.tencent.devops.common.constant.ComConstants.Tool;

/**
 * ci流水线工具管理服务层代码
 *
 * @version V1.0
 * @date 2019/4/25
 */
@Slf4j
@Service
public class ToolServiceImpl implements ToolService
{
    private static Logger logger = LoggerFactory.getLogger(ToolServiceImpl.class);

    @Value("${time.analysis.maxhour:#{null}}")
    private String maxHour;

    @Autowired
    protected ToolRepository toolRepository;

    @Autowired
    private ToolDao toolDao;

    @Autowired
    protected TaskRepository taskRepository;

    @Autowired
    private TaskDao taskDao;

    @Autowired
    private BizServiceFactory<IRegisterToolBizService> bizServiceFactory;

    @Autowired
    private ToolMetaCacheService toolMetaCache;

    @Autowired
    private PipelineService pipelineService;

    @Autowired
    protected Client client;

    @Autowired
    private AuthExRegisterApi authExRegisterApi;

    @Autowired
    private SpecialParamUtil specialParamUtil;

    @Autowired
    private PlatformService platformService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private ToolStatisticRepository toolStatisticRepository;


    @Override
    public Result<Boolean> registerTools(BatchRegisterVO batchRegisterVO, TaskInfoEntity taskInfoEntity, String userName)
    {
        Result<Boolean> registerResult;
        long taskId = batchRegisterVO.getTaskId();
        if(CollectionUtils.isEmpty(batchRegisterVO.getTools()))
        {
            logger.error("no tools will be registered!");
            return new Result<>(false);
        }
        if (null == taskInfoEntity)
        {
            taskInfoEntity = taskRepository.findFirstByTaskId(taskId);
            if (null == taskInfoEntity)
            {
                log.error("task does not exist! task id: {}", taskId);
                throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS, new String[]{String.valueOf(taskId)}, null);
            }
        }

        //配置流水线编排
        pipelineConfig(batchRegisterVO, taskInfoEntity, userName);

        //批量接入工具
        List<String> failTools = new ArrayList<>();
        List<String> successTools = new ArrayList<>();
        List<ToolConfigInfoEntity> toolConfigInfoEntityList = new ArrayList<>();
        if(CollectionUtils.isNotEmpty(taskInfoEntity.getToolConfigInfoList()))
        {
            toolConfigInfoEntityList = taskInfoEntity.getToolConfigInfoList();
        }
        for (ToolConfigInfoVO toolConfigInfoVO : batchRegisterVO.getTools())
        {
            try
            {
                ToolConfigInfoEntity toolConfigInfoEntity = registerTool(toolConfigInfoVO, taskInfoEntity, userName);
                toolConfigInfoEntityList.add(toolConfigInfoEntity);
                successTools.add(toolConfigInfoVO.getToolName());
            }
            catch (Exception e)
            {
                log.error("register tool fail! tool name: {}", toolConfigInfoVO.getToolName(), e);
                failTools.add(toolConfigInfoVO.getToolName());
            }
        }

        // 新注册的工具都需要设置强制全量扫描标志
        if (CollectionUtils.isNotEmpty(successTools))
        {
            log.info("set force full scan, taskId:{}, toolNames:{}", taskId, successTools);
            client.get(ServiceToolBuildInfoResource.class).setForceFullScan(taskId, successTools);
        }

        // 保存工具信息,只有当不为空的时候才保存
        if(CollectionUtils.isNotEmpty(toolConfigInfoEntityList))
        {
            toolConfigInfoEntityList = toolRepository.saveAll(toolConfigInfoEntityList);
            taskInfoEntity.setToolConfigInfoList(toolConfigInfoEntityList);
        }
        taskRepository.save(taskInfoEntity);



        //全部工具添加失败
        if (failTools.size() == batchRegisterVO.getTools().size())
        {
            registerResult = new Result<>(0, TaskMessageCode.ADD_TOOL_FAIL, "所有工具添加失败", false);
        }
        //全部工具添加成功
        else if (successTools.size() == batchRegisterVO.getTools().size())
        {
            registerResult = new Result<>(true);
        }
        else
        {
            StringBuffer buffer = new StringBuffer();
            formatToolNames(successTools, buffer);
            buffer.append("添加成功；\n");
            formatToolNames(failTools, buffer);
            buffer.append("添加失败");
            registerResult = new Result<>(0, TaskMessageCode.ADD_TOOL_PARTIALLY_SUCCESS, buffer.toString(), false);
        }

        // 接入成功不再自动启动流水线
        /*if (CollectionUtils.isNotEmpty(successTools))
        {
            //启动流水线
            String buildId = pipelineService.startPipeline(taskInfoEntity.getPipelineId(), taskInfoEntity.getProjectId(),
                    taskInfoEntity.getNameEn(), taskInfoEntity.getCreateFrom(), successTools, userName);
            //更新任务状态
            TaskInfoEntity finalTaskInfoEntity = taskInfoEntity;
            successTools.forEach(tool -> pipelineService.updateTaskInitStep(String.valueOf(true), finalTaskInfoEntity, buildId, tool, userName));
        }*/
        return registerResult;
    }


    @Override
    public ToolConfigInfoEntity registerTool(ToolConfigInfoVO toolConfigInfo, TaskInfoEntity taskInfoEntity, String user)
    {
        String toolName = toolConfigInfo.getToolName();
        String toolNames = taskInfoEntity.getToolNames();
        if (StringUtils.isNotEmpty(toolNames))
        {
            List<String> toolNameList = List2StrUtil.fromString(toolNames, ComConstants.TOOL_NAMES_SEPARATOR);
            if (toolNameList.contains(toolName))
            {
                if(CollectionUtils.isNotEmpty(taskInfoEntity.getToolConfigInfoList()) &&
                        taskInfoEntity.getToolConfigInfoList().stream().anyMatch(toolConfigInfoEntity -> toolConfigInfoEntity.getToolName().
                        equalsIgnoreCase(toolConfigInfo.getToolName())))
                {
                    log.error("task [{}] has registered tool before! tool name: {}", taskInfoEntity.getTaskId(), toolName);
                    throw new CodeCCException(CommonMessageCode.RECORD_EXIST, new String[]{toolName}, null);
                }
            }
        }
        IRegisterToolBizService registerToolBizService = bizServiceFactory.createBizService(
                toolName, ComConstants.BusinessType.REGISTER_TOOL.value(), IRegisterToolBizService.class);
        ToolConfigInfoEntity toolConfigInfoEntity = registerToolBizService.registerTool(toolConfigInfo, taskInfoEntity, user);

        //添加项目信息
        addNewTool2Proj(toolConfigInfoEntity, taskInfoEntity, user);

        log.info("register tool[{}] for task[{}] successful", toolName, toolConfigInfoEntity.getTaskId());
        return toolConfigInfoEntity;
    }

    @Override
    @OperationHistory(funcId = FUNC_TOOL_SWITCH)
    public Boolean toolStatusManage(List<String> toolNameList, String manageType, String userName, long taskId)
    {
        TaskInfoEntity taskInfoEntity = taskRepository.findFirstByTaskId(taskId);
        for (String toolName : toolNameList)
        {
            ToolConfigInfoEntity toolConfigInfoEntity = toolRepository.findFirstByTaskIdAndToolName(taskId, toolName);
            if (null == toolConfigInfoEntity)
            {
                log.error("empty tool config info found out! task id: {}, tool name: {}", taskId, toolName);
                throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS, new String[]{toolName}, null);
            }
            if (CommonJudge.COMMON_Y.value().equals(manageType))
            {
                toolConfigInfoEntity.setFollowStatus(toolConfigInfoEntity.getLastFollowStatus());
                log.info("enable tool, task id: {}, tool name: {}", toolConfigInfoEntity.getTaskId(),
                        toolConfigInfoEntity.getToolName());
            }
            else
            {
                toolConfigInfoEntity.setLastFollowStatus(toolConfigInfoEntity.getFollowStatus());
                toolConfigInfoEntity.setFollowStatus(FOLLOW_STATUS.WITHDRAW.value());
                log.info("disable tool, task id: {}, tool name: {}", toolConfigInfoEntity.getTaskId(),
                        toolConfigInfoEntity.getToolName());
            }
            toolRepository.save(toolConfigInfoEntity);
        }

        if (CommonJudge.COMMON_Y.value().equalsIgnoreCase(manageType))
        {
            // registerVo及relPath为空代表CodeElement为空，与之前的逻辑符合
            pipelineService.updatePipelineTools(userName, taskId, toolNameList, taskInfoEntity,
                    PipelineToolUpdateType.ADD, null, null);
        }
        else if (CommonJudge.COMMON_N.value().equalsIgnoreCase(manageType))
        {
            pipelineService.updatePipelineTools(userName, taskId, toolNameList, taskInfoEntity,
                    PipelineToolUpdateType.REMOVE, null, null);
        }
        return true;
    }

    @Override
    public ToolConfigInfoVO getToolByTaskIdAndName(long taskId, String toolName)
    {
        ToolConfigInfoEntity toolConfigInfoEntity = toolRepository.findFirstByTaskIdAndToolName(taskId, toolName);
        if (null == toolConfigInfoEntity)
        {
            log.error("no tool info found!, task id: {}, tool name: {}", taskId, toolName);
            throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS, new String[]{toolName}, null);
        }
        ToolConfigInfoVO toolConfigInfoVO = new ToolConfigInfoVO();
        BeanUtils.copyProperties(toolConfigInfoEntity, toolConfigInfoVO, "ignoreCheckers", "checkerProps");

        // 加入规则集
        if (toolConfigInfoEntity.getCheckerSet() != null)
        {
            ToolCheckerSetVO toolCheckerSetVO = new ToolCheckerSetVO();
            BeanUtils.copyProperties(toolConfigInfoEntity.getCheckerSet(), toolCheckerSetVO);
            toolConfigInfoVO.setCheckerSet(toolCheckerSetVO);
        }
        return toolConfigInfoVO;
    }

    @Override
    public ToolConfigInfoWithMetadataVO getToolWithMetadataByTaskIdAndName(long taskId, String toolName)
    {
        // 获取工具配置
        ToolConfigInfoVO toolConfigInfoVO = getToolByTaskIdAndName(taskId, toolName);
        ToolConfigInfoWithMetadataVO toolConfigInfoWithMetadataVO = new ToolConfigInfoWithMetadataVO();
        BeanUtils.copyProperties(toolConfigInfoVO, toolConfigInfoWithMetadataVO);
        if (toolConfigInfoVO.getCheckerSet() != null)
        {
            toolConfigInfoWithMetadataVO.setCheckerSet(toolConfigInfoVO.getCheckerSet());
        }

        // 获取工具元数据
        ToolMetaBaseVO toolMetaBaseVO = toolMetaCache.getToolBaseMetaCache(toolName);
        toolConfigInfoWithMetadataVO.setToolMetaBaseVO(toolMetaBaseVO);

        // 获取项目语言
        TaskInfoEntity taskInfoEntity = taskRepository.findFirstByTaskId(taskId);
        if (null == taskInfoEntity)
        {
            log.error("task does not exist! task id: {}", taskId);
            throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS, new String[]{String.valueOf(taskId)}, null);
        }
        toolConfigInfoWithMetadataVO.setCodeLang(taskInfoEntity.getCodeLang());
        return toolConfigInfoWithMetadataVO;
    }


    /**
     * 停用流水线
     *
     * @param taskId
     * @param projectId
     * @return
     */
    @Override
    public Boolean deletePipeline(Long taskId, String projectId, String userName)
    {
        boolean deleteTask = authExRegisterApi.deleteCodeCCTask(String.valueOf(taskId), projectId);
        if (!deleteTask)
        {
            return false;
        }

        TaskUpdateVO taskUpdateVO = new TaskUpdateVO();
        taskUpdateVO.setStatus(TaskConstants.TaskStatus.DISABLE.value());
        taskUpdateVO.setDisableTime(String.valueOf(System.currentTimeMillis()));
        return taskDao.updateTask(taskUpdateVO.getTaskId(), taskUpdateVO.getCodeLang(), taskUpdateVO.getNameCn(), taskUpdateVO.getTaskOwner(),
                taskUpdateVO.getTaskMember(), taskUpdateVO.getDisableTime(), taskUpdateVO.getStatus(),
                userName);
    }

    @Override
    public Boolean updatePipelineTool(Long taskId, List<String> toolList, String userName)
    {
        TaskInfoEntity taskInfoEntity = taskRepository.findFirstByTaskId(taskId);
        if (BsTaskCreateFrom.BS_CODECC.name().equals(taskInfoEntity.getCreateFrom()))
        {
            log.error("=========the task is created from codecc!=============");
            throw new CodeCCException(CommonMessageCode.SYSTEM_ERROR);
        }
        pipelineService.updatePipelineTools(userName, taskId, toolList, taskInfoEntity, PipelineToolUpdateType.ADD, null, null);
        return true;
    }

    /**
     * 清除任务和工具关联的规则集
     *
     * @param taskId
     * @param toolNames
     * @return
     */
    @Override
    public Boolean clearCheckerSet(Long taskId, List<String> toolNames)
    {
        if (CollectionUtils.isNotEmpty(toolNames))
        {
            toolDao.clearCheckerSet(taskId, toolNames);
        }
        return true;
    }

    /**
     * 清除任务和工具关联的规则集
     *
     * @param taskId
     * @param toolCheckerSets
     * @return
     */
    @Override
    public Boolean addCheckerSet2Task(Long taskId, List<ToolCheckerSetVO> toolCheckerSets)
    {
        if (CollectionUtils.isNotEmpty(toolCheckerSets))
        {
            List<ToolCheckerSetEntity> toolCheckerSetEntities = Lists.newArrayList();
            for (ToolCheckerSetVO toolCheckerSetVO : toolCheckerSets)
            {
                ToolCheckerSetEntity toolCheckerSetEntity = new ToolCheckerSetEntity();
                BeanUtils.copyProperties(toolCheckerSetVO, toolCheckerSetEntity);
                toolCheckerSetEntities.add(toolCheckerSetEntity);
            }
            toolDao.setCheckerSet(taskId, toolCheckerSetEntities);
        }
        return true;
    }

    /**
     * 修改工具特殊参数和规则集
     *
     * @param user
     * @param taskId
     * @param paramJsonAndCheckerSetsVO
     * @return
     */
    @Override
    public Boolean updateParamJsonAndCheckerSets(String user, Long taskId, ParamJsonAndCheckerSetsVO paramJsonAndCheckerSetsVO)
    {
        TaskInfoEntity taskInfoEntity = taskRepository.findFirstByTaskId(taskId);
        Map<String, ToolConfigInfoEntity> toolConfigInfoEntityMap = Maps.newHashMap();
        if (CollectionUtils.isNotEmpty(taskInfoEntity.getToolConfigInfoList()))
        {
            for (ToolConfigInfoEntity toolConfigInfoEntity : taskInfoEntity.getToolConfigInfoList())
            {
                toolConfigInfoEntityMap.put(toolConfigInfoEntity.getToolName(), toolConfigInfoEntity);
            }
        }
        Set<String> checkerUpdatedTools = Sets.newHashSet();
        Set<String> clearCheckerSetTools = Sets.newHashSet();
        if (CollectionUtils.isNotEmpty(paramJsonAndCheckerSetsVO.getToolConfig()))
        {
            Map<String, ToolConfigInfoEntity> toolConfigMap = Maps.newHashMap();
            for (ToolParamJsonAndCheckerSetVO reqToolConfig : paramJsonAndCheckerSetsVO.getToolConfig())
            {
                String toolName = reqToolConfig.getToolName();
                ToolConfigInfoEntity currentToolConfig = toolConfigInfoEntityMap.get(toolName);
                toolConfigMap.put(toolName, currentToolConfig);
                if (StringUtils.isNotEmpty(reqToolConfig.getParamJson()))
                {
                    if (currentToolConfig != null && !specialParamUtil.isSameParam(toolName, currentToolConfig.getParamJson(), reqToolConfig.getParamJson()))
                    {
                        checkerUpdatedTools.add(toolName);
                        clearCheckerSetTools.add(toolName);
                        currentToolConfig.setParamJson(reqToolConfig.getParamJson());
                        toolRepository.save(currentToolConfig);
                    }
                }
            }

            List<ToolCheckerSetVO> toolCheckerSets = Lists.newArrayList();
            for (ToolParamJsonAndCheckerSetVO toolConfig : paramJsonAndCheckerSetsVO.getToolConfig())
            {
                String toolName = toolConfig.getToolName();
                if (toolConfig.getCheckerSet() != null && StringUtils.isNotEmpty(toolConfig.getCheckerSet().getCheckerSetId()))
                {
                    String checkerSetId = toolConfig.getCheckerSet().getCheckerSetId();
                    if (toolConfigMap.get(toolName) == null || toolConfigMap.get(toolName).getCheckerSet() == null
                            || !checkerSetId.equals(toolConfigMap.get(toolName).getCheckerSet().getCheckerSetId()))
                    {
                        checkerUpdatedTools.add(toolName);
                        ToolCheckerSetVO toolCheckerSetVO = new ToolCheckerSetVO(toolName, checkerSetId, null);
                        toolCheckerSets.add(toolCheckerSetVO);
                        clearCheckerSetTools.remove(toolName);
                    }
                }
            }
            if (CollectionUtils.isNotEmpty(toolCheckerSets))
            {
                client.get(ServiceCheckerRestResource.class).addCheckerSet2Task(user, taskId,
                        new AddCheckerSet2TaskReqVO(toolCheckerSets, CommonJudge.COMMON_N.value(), true));
            }
        }

        // 修改工具特殊参数或规则集后，要设置强制全量扫描标志位
        if (CollectionUtils.isNotEmpty(checkerUpdatedTools))
        {
            log.info("set force full scan, taskId:{}, toolNames:{}", taskId, checkerUpdatedTools);
            client.get(ServiceToolBuildInfoResource.class).setForceFullScan(taskId, Lists.newArrayList(checkerUpdatedTools));
        }

        // 修改工具特殊参数但不需要关联新的规则集的工具，要清除已关联的规则集
        if (CollectionUtils.isNotEmpty(clearCheckerSetTools))
        {
            clearCheckerSet(taskId, Lists.newArrayList(clearCheckerSetTools));
        }
        return true;
    }

    @Override
    public ToolConfigPlatformVO getToolConfigPlatformInfo(Long taskId, String toolName) {
        if (taskId == null || taskId == 0 || StringUtils.isBlank(toolName)) {
            logger.error("taskId or toolName is not allowed to be empty!");
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_NULL, new String[]{"taskId or toolName"}, null);
        }

        ToolConfigInfoEntity toolConfigInfoEntity = toolRepository.findFirstByTaskIdAndToolName(taskId, toolName);
        if (toolConfigInfoEntity == null) {
            logger.error("task [{}] or toolName is invalid!", taskId);
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{"taskId or toolName"}, null);
        }

        ToolConfigPlatformVO toolConfigPlatformVO = new ToolConfigPlatformVO();
        BeanUtils.copyProperties(toolConfigInfoEntity, toolConfigPlatformVO);

        String port = "";
        String userName = "";
        String passwd = "";
        String platformIp = toolConfigInfoEntity.getPlatformIp();
        if (StringUtils.isNotBlank(platformIp)) {
            PlatformVO platformVO = platformService.getPlatformByToolNameAndIp(toolName, platformIp);
            if (null != platformVO) {
                port = platformVO.getPort();
                userName = platformVO.getUserName();
                passwd = platformVO.getPasswd();
            }
        }
        TaskInfoEntity taskInfoEntity = taskRepository.findFirstByTaskId(taskId);

        toolConfigPlatformVO.setIp(platformIp);
        toolConfigPlatformVO.setPort(port);
        toolConfigPlatformVO.setUserName(userName);
        toolConfigPlatformVO.setPassword(passwd);
        toolConfigPlatformVO.setNameEn(taskInfoEntity.getNameEn());
        toolConfigPlatformVO.setNameCn(taskInfoEntity.getNameCn());

        return toolConfigPlatformVO;
    }

    @Override
    public Boolean updateToolPlatformInfo(Long taskId, String userName, ToolConfigPlatformVO toolConfigPlatformVO)
    {
        // 1.检查参数
        if (toolConfigPlatformVO == null)
        {
            logger.error("toolConfigPlatformVO is null!");
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{"reqObj"}, null);
        }
        // 2.检查参数
        Long taskIdReq = toolConfigPlatformVO.getTaskId();
        String toolName = toolConfigPlatformVO.getToolName();
        if (taskIdReq == null || !taskIdReq.equals(taskId) || StringUtils.isBlank(toolName) ||
                StringUtils.isBlank(userName))
        {
            logger.error("parameter is invalid! task:{} userName:{} reqObj:{}", taskId, userName, toolConfigPlatformVO);
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_NULL, new String[]{"parameter"}, null);
        }
        // 检查任务ID是否有效
        TaskInfoEntity taskInfoEntity = taskRepository.findFirstByTaskId(taskIdReq);
        if (taskInfoEntity == null) {
            logger.error("taskId [{}] is invalid!", taskIdReq);
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{"taskId"}, null);
        }
        // 检查platform IP是否存在
        String platformIp = toolConfigPlatformVO.getIp();
        if (StringUtils.isNotBlank(platformIp)) {
            PlatformVO platformVO = platformService.getPlatformByToolNameAndIp(toolName, platformIp);
            if (platformVO == null) {
                logger.error("platform ip [{}] is not found!", platformIp);
                throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{"platform ip"}, null);
            }
        }

        return toolDao.updateToolConfigInfo(taskIdReq, toolName, userName, toolConfigPlatformVO.getSpecConfig(),
                platformIp);
    }

    @Override
    public Result<Boolean> updateTools(Long taskId, String user, BatchRegisterVO batchRegisterVO)
    {
        // 查询任务已接入的工具列表
        TaskInfoEntity taskInfoEntity = taskRepository.findFirstByTaskId(taskId);
        Map<String, ToolConfigInfoEntity> toolConfigInfoEntityMap = Maps.newHashMap();
        if (CollectionUtils.isNotEmpty(taskInfoEntity.getToolConfigInfoList()))
        {
            for (ToolConfigInfoEntity toolConfigInfoEntity : taskInfoEntity.getToolConfigInfoList())
            {
                toolConfigInfoEntityMap.put(toolConfigInfoEntity.getToolName(), toolConfigInfoEntity);
            }
        }

        // 获取需要启用的工具列表
        long curTime = System.currentTimeMillis();
        List<ToolConfigInfoEntity> updateStatusTools = Lists.newArrayList();
        Iterator<ToolConfigInfoVO> it = batchRegisterVO.getTools().iterator();
        Set<String> reqTools = Sets.newHashSet();
        while (it.hasNext())
        {
            ToolConfigInfoVO toolConfigInfoVO = it.next();
            reqTools.add(toolConfigInfoVO.getToolName());
            if (toolConfigInfoEntityMap.get(toolConfigInfoVO.getToolName()) != null)
            {
                ToolConfigInfoEntity toolConfigInfoEntity = toolConfigInfoEntityMap.get(toolConfigInfoVO.getToolName());
                int toolFollowStatus = toolConfigInfoEntity.getFollowStatus();
                if (FOLLOW_STATUS.WITHDRAW.value() == toolFollowStatus)
                {
                    toolConfigInfoEntity.setFollowStatus(toolConfigInfoEntity.getLastFollowStatus());
                    toolConfigInfoEntity.setUpdatedBy(user);
                    toolConfigInfoEntity.setUpdatedDate(curTime);
                    updateStatusTools.add(toolConfigInfoEntity);
                    log.info("enable task {} tool {}", taskId, toolConfigInfoEntity.getToolName());
                }
                it.remove();
            }
        }

        // 获取需要停用的工具列表
        if (CollectionUtils.isNotEmpty(taskInfoEntity.getToolConfigInfoList()))
        {
            for (ToolConfigInfoEntity toolConfigInfoEntity : taskInfoEntity.getToolConfigInfoList())
            {
                if (!reqTools.contains(toolConfigInfoEntity.getToolName()))
                {
                    int toolFollowStatus = toolConfigInfoEntity.getFollowStatus();
                    if (FOLLOW_STATUS.WITHDRAW.value() != toolFollowStatus)
                    {
                        toolConfigInfoEntity.setFollowStatus(FOLLOW_STATUS.WITHDRAW.value());
                        toolConfigInfoEntity.setUpdatedBy(user);
                        toolConfigInfoEntity.setUpdatedDate(curTime);
                        updateStatusTools.add(toolConfigInfoEntity);
                        log.info("disable task {} tool {}", taskId, toolConfigInfoEntity.getToolName());
                    }
                }
            }
        }

        // 更新工具状态
        if (CollectionUtils.isNotEmpty(updateStatusTools))
        {
            toolRepository.saveAll(taskInfoEntity.getToolConfigInfoList());
        }

        // 新增的工具需要接入
        if (CollectionUtils.isNotEmpty(batchRegisterVO.getTools()))
        {
            return registerTools(batchRegisterVO, taskInfoEntity, user);
        }
        else
        {
            return new Result<>(true);
        }
    }

    @NotNull
    private Map<String, Map<String, PlatformVO>> genToolIpInfoMap(List<PlatformVO> platformInfo)
    {
        Map<String, Map<String, PlatformVO>> platformInfoMap = Maps.newHashMap();
        if (platformInfo != null)
        {
            Set<String> tools = platformInfo.stream().map(PlatformVO::getToolName).collect(Collectors.toSet());
            tools.forEach(tool ->
            {
                Map<String, PlatformVO> dateMap = Maps.newHashMap();
                platformInfo.forEach(info ->
                {
                    if (info.getToolName().equals(tool))
                    {
                        dateMap.put(info.getIp(), info);
                    }
                });
                platformInfoMap.put(tool, dateMap);
            });
        }
        return platformInfoMap;
    }

    private void formatToolNames(List<String> tools, StringBuffer buffer)
    {
        buffer.append("工具[");
        for (int i = 0; i < tools.size(); i++)
        {
            String toolName = tools.get(i);
            String displayName = toolMetaCache.getToolDisplayName(toolName);
            buffer.append(null != displayName ? displayName : "");
            if (i != tools.size() - 1)
            {
                buffer.append(", ");
            }
        }
        buffer.append("]");
    }

    /**
     * 配置流水线编排
     *
     * @param batchRegisterVO
     * @param taskInfoEntity
     * @param userName
     * @throws JsonProcessingException
     */
    private void pipelineConfig(BatchRegisterVO batchRegisterVO, TaskInfoEntity taskInfoEntity,
                                String userName)
    {
        long taskId = taskInfoEntity.getTaskId();
        String pipelineId = taskInfoEntity.getPipelineId();
        String relPath = getRelPath(batchRegisterVO);
        if (StringUtils.isEmpty(pipelineId))
        {
            List<String> defaultExecuteDate = getTaskDefaultReportDate();
            String defaultExecuteTime = getTaskDefaultTime();

            // 创建流水线编排并获取流水线ID
            pipelineId = pipelineService.assembleCreatePipeline(batchRegisterVO, taskInfoEntity, defaultExecuteTime, defaultExecuteDate, userName, relPath);
            log.info("create pipeline success! project id: {}, codecc task id: {}, pipeline id: {}", taskInfoEntity.getProjectId(),
                    taskInfoEntity.getTaskId(), pipelineId);

            taskInfoEntity.setPipelineId(pipelineId);
            //表示通过codecc_web平台创建蓝盾codecc任务
            taskInfoEntity.setCreateFrom(BsTaskCreateFrom.BS_CODECC.value());
            //保存定时执行信息
//            taskInfoEntity.setExecuteTime(defaultExecuteTime);
//            taskInfoEntity.setExecuteDate(defaultExecuteDate);
        }
        else
        {
            //更新流水线编排中的工具
            List<String> toolNames = new ArrayList<>();
            for (ToolConfigInfoVO tool : batchRegisterVO.getTools())
            {
                toolNames.add(tool.getToolName());
            }
            if (!toolNames.contains(Tool.GOML.name()))
            {
                relPath = null;
            }
            pipelineService.updatePipelineTools(userName, taskId, toolNames, taskInfoEntity, PipelineToolUpdateType.ADD, batchRegisterVO, relPath);
        }

    }

    /**
     * 获取相对路径
     *
     * @param registerVO
     * @return
     */
    private String getRelPath(BatchRegisterVO registerVO)
    {
        if (CollectionUtils.isNotEmpty(registerVO.getTools()))
        {
            for (ToolConfigInfoVO toolConfig : registerVO.getTools())
            {
                //常量要区分吗
                if (Tool.GOML.name().equals(toolConfig.getToolName()) && StringUtils.isNotEmpty(toolConfig.getParamJson()))
                {
                    JSONObject paramJson = new JSONObject(toolConfig.getParamJson());
                    if (paramJson.has(ComConstants.PARAMJSON_KEY_REL_PATH))
                    {
                        return paramJson.getString(ComConstants.PARAMJSON_KEY_REL_PATH);
                    }
                }
            }
        }
        return "";
    }

    /**
     * 更新工具分析步骤及状态
     *
     * @param toolConfigBaseVO
     */
    @Override
    public void updateToolStepStatus(ToolConfigBaseVO toolConfigBaseVO)
    {
        toolDao.updateToolStepStatusByTaskIdAndToolName(toolConfigBaseVO);
    }

    protected void addNewTool2Proj(ToolConfigInfoEntity toolConfigInfoEntity, TaskInfoEntity taskInfoEntity, String user)
    {
        String toolName = toolConfigInfoEntity.getToolName();
        String toolNames = taskInfoEntity.getToolNames();
        if (StringUtils.isNotEmpty(toolNames))
        {
            toolNames = String.format("%s%s%s", toolNames, ComConstants.TOOL_NAMES_SEPARATOR, toolName);
            taskInfoEntity.setToolNames(toolNames);
        }
        else
        {
            toolNames = toolName;
            taskInfoEntity.setToolNames(toolNames);
        }
        taskInfoEntity.setUpdatedBy(user);
        taskInfoEntity.setUpdatedDate(System.currentTimeMillis());
    }


    protected String getTaskDefaultTime()
    {
        float time = new Random().nextInt(Integer.valueOf(maxHour) * 2) / 2f;
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, (int) (time * 60));
        String defaultTime = timeFormat.format(cal.getTime());
        log.info("task default time {}", defaultTime);
        return defaultTime;
    }


    protected List<String> getTaskDefaultReportDate()
    {
        List<String> reportDate = new ArrayList<>();
        reportDate.add(String.valueOf(Calendar.MONDAY));
        reportDate.add(String.valueOf(Calendar.TUESDAY));
        reportDate.add(String.valueOf(Calendar.WEDNESDAY));
        reportDate.add(String.valueOf(Calendar.THURSDAY));
        reportDate.add(String.valueOf(Calendar.FRIDAY));
        reportDate.add(String.valueOf(Calendar.SATURDAY));
        reportDate.add(String.valueOf(Calendar.SUNDAY));
        return reportDate;
    }

    @Override
    public List<String> getEffectiveToolList(long taskId)
    {
        TaskInfoEntity taskInfoEntity = taskRepository.findFirstByTaskId(taskId);
        //获取工具配置实体类清单
        List<String> toolNameList = getEffectiveToolList(taskInfoEntity);
        return toolNameList;
    }

    @Override
    public List<String> getEffectiveToolList(TaskInfoEntity taskInfoEntity)
    {
        //获取工具配置实体类清单
        List<ToolConfigInfoEntity> toolConfigInfoEntityList = taskInfoEntity.getToolConfigInfoList();
        if (CollectionUtils.isEmpty(toolConfigInfoEntityList))
        {
            return new ArrayList<>();
        }
        List<String> toolNameList = toolConfigInfoEntityList.stream()
                .filter(toolConfigInfoEntity -> FOLLOW_STATUS.WITHDRAW.value() != toolConfigInfoEntity.getFollowStatus())
                .map(ToolConfigInfoEntity::getToolName)
                .collect(Collectors.toList());
        return toolNameList;
    }

    @Override
    public List<ToolConfigInfoVO> batchGetToolConfigList(QueryTaskListReqVO queryReqVO)
    {
        List<ToolConfigInfoVO> toolConfigInfoVoList = Lists.newArrayList();

        Collection<Long> taskIds = queryReqVO.getTaskIds();
        if (CollectionUtils.isNotEmpty(taskIds))
        {
            List<ToolConfigInfoEntity> configInfoEntityList = toolRepository.findByTaskIdIn(taskIds);
            if (CollectionUtils.isNotEmpty(configInfoEntityList))
            {
                toolConfigInfoVoList = configInfoEntityList.stream().map(entity ->
                {
                    ToolConfigInfoVO toolConfigInfoVO = new ToolConfigInfoVO();
                    BeanUtils.copyProperties(entity, toolConfigInfoVO);
                    return toolConfigInfoVO;
                }).collect(Collectors.toList());
            }
        }

        return toolConfigInfoVoList;
    }


    @Override
    public Boolean batchUpdateToolFollowStatus(Integer pageSize)
    {
        Pageable pageable = PageableUtils.getPageable(1, pageSize, "task_id", Sort.Direction.ASC, "");

        // 1.查询有效的任务ID
        List<TaskInfoEntity> taskInfoEntities = taskRepository.findByStatusAndCreateFromIn(Status.ENABLE.value(),
                Lists.newArrayList(BsTaskCreateFrom.BS_CODECC.value(), BsTaskCreateFrom.BS_PIPELINE.value()));
        List<Long> taskList = taskInfoEntities.stream().map(TaskInfoEntity::getTaskId).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(taskList))
        {
            log.error("End batchUpdateToolFollowStatus, task id list is empty!");
            return false;
        }

        // 2.按分页查询未跟进的工具列表
        List<ToolConfigInfoEntity> toolConfigInfoEntities = toolDao.getTaskIdsAndFollowStatusPage(taskList,
                Lists.newArrayList(FOLLOW_STATUS.NOT_FOLLOW_UP_0.value(), FOLLOW_STATUS.NOT_FOLLOW_UP_1.value()),
                pageable);
        if (CollectionUtils.isEmpty(toolConfigInfoEntities))
        {
            log.info("End batchUpdateToolFollowStatus, Not follow up tool is empty!");
            return true;
        }

        // 3.批量更新跟进状态
        toolDao.batchUpdateToolFollowStatus(toolConfigInfoEntities, FOLLOW_STATUS.ACCESSED);

        log.info("finish batchUpdateToolFollowStatus, count: {}", toolConfigInfoEntities.size());
        return true;
    }

    /**
     * 仅用于初始化查询工具数量
     *
     * @param day 天数
     * @return
     */
    @Override
    public Boolean initToolCountScript(Integer day) {
        // 获取日期
        List<String> dates = DateTimeUtils.getBeforeDaily(day);

        // 获取开源的任务id集合
        List<Long> openTaskIdList = taskService.queryTaskIdByType(DefectStatType.GONGFENG_SCAN);
        // 获取非开源的任务id集合
        List<Long> taskIdList = taskService.queryTaskIdByType(DefectStatType.USER);

        List<ToolStatisticEntity> toolCountData = Lists.newArrayList();
        for (String date : dates) {
            // 获取结束时间
            long endTime = DateTimeUtils.getTimeStampEnd(date);
            try {
                // 根据工具名分组 查询开源的各工具数量
                getToolCount(openTaskIdList, toolCountData, date, endTime, DefectStatType.GONGFENG_SCAN.value());
                // 根据工具名分组 查询非开源的各工具数量
                getToolCount(taskIdList, toolCountData, date, endTime, DefectStatType.USER.value());
            } catch (Exception e) {
                log.error("Failed to obtain tool data", e);
            }
        }

        toolStatisticRepository.saveAll(toolCountData);
        return true;
    }

    /**
     * 根据工具名分组 查询开源的工具数量
     *
     * @param taskIdList    任务id集合
     * @param toolCountData 容器
     * @param date          日期
     * @param endTime       时间
     * @param createFrom    来源
     */
    private void getToolCount(List<Long> taskIdList, List<ToolStatisticEntity> toolCountData, String date,
            long endTime, String createFrom) {

        List<ToolCountScriptEntity> toolCountScriptList = toolDao.findDailyToolCount(taskIdList, endTime);

        for (ToolCountScriptEntity toolCountScript : toolCountScriptList) {
            ToolStatisticEntity toolStatisticEntity = new ToolStatisticEntity();
            // 设置时间
            toolStatisticEntity.setDate(date);
            // 设置工具名称
            toolStatisticEntity.setToolName(toolCountScript.getToolName());
            // 设置来源
            toolStatisticEntity.setDataFrom(createFrom);
            // 设置总数量
            toolStatisticEntity.setToolCount(toolCountScript.getCount());

            toolCountData.add(toolStatisticEntity);
        }
    }
}
