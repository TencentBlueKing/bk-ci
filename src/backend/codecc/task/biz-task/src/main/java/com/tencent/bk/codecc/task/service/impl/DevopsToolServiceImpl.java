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
import com.tencent.bk.codecc.task.constant.TaskConstants;
import com.tencent.bk.codecc.task.constant.TaskMessageCode;
import com.tencent.bk.codecc.task.dao.ToolMetaCache;
import com.tencent.bk.codecc.task.dao.mongotemplate.TaskDao;
import com.tencent.bk.codecc.task.model.TaskInfoEntity;
import com.tencent.bk.codecc.task.model.ToolConfigInfoEntity;
import com.tencent.bk.codecc.task.model.ToolMetaEntity;
import com.tencent.bk.codecc.task.service.AbstractToolService;
import com.tencent.bk.codecc.task.service.PathFilterService;
import com.tencent.bk.codecc.task.service.PipelineService;
import com.tencent.bk.codecc.task.vo.BatchRegisterVO;
import com.tencent.bk.codecc.task.vo.TaskUpdateVO;
import com.tencent.bk.codecc.task.vo.ToolConfigInfoVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.auth.api.external.BkAuthExRegisterApi;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.pipeline.Model;
import com.tencent.devops.common.pipeline.enums.ChannelCode;
import com.tencent.devops.common.pipeline.pojo.element.Element;
import com.tencent.devops.common.web.aop.annotation.OperationHistory;
import com.tencent.devops.process.api.ServicePipelineResource;
import com.tencent.devops.process.pojo.PipelineId;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.tencent.devops.common.constant.ComConstants.*;

/**
 * ci流水线工具管理服务层代码
 *
 * @version V1.0
 * @date 2019/4/25
 */
@Service("devopsToolService")
public class DevopsToolServiceImpl extends AbstractToolService
{

    private static Logger logger = LoggerFactory.getLogger(DevopsToolServiceImpl.class);


    @Autowired
    private PathFilterService pathFilterService;


    @Autowired
    private ToolMetaCache toolMetaCache;

    @Autowired
    private PipelineService pipelineService;

    @Autowired
    private TaskDao taskDao;

    @Autowired
    private BkAuthExRegisterApi bkAuthExRegisterApi;


    @Override
    @OperationHistory(funcId = FUNC_REGISTER_TOOL, operType = REGISTER_TOOL)
    public Result<Boolean> registerTools(BatchRegisterVO batchRegisterVO, String userName)
    {
        Result<Boolean> registerResult;
        long taskId = batchRegisterVO.getTaskId();
        TaskInfoEntity taskInfoEntity = taskRepository.findByTaskId(taskId);
        if (null == taskInfoEntity)
        {
            logger.error("task does not exist! task id: {}", taskId);
            throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS, new String[]{String.valueOf(taskId)}, null);
        }
        //配置流水线编排
        pipelineConfig(batchRegisterVO, taskInfoEntity, userName);

        //配置流水线编排
//        ToolConfigInfoVO toolConfig = getBatchToolInfoModelInst(batchRegisterVO, taskInfoEntity);
        List<String> failTools = new ArrayList<>();
        List<String> successTools = new ArrayList<>();

        //配置代码库信息
        taskInfoEntity.setRepoHashId(batchRegisterVO.getRepositoryHashId());
        taskInfoEntity.setBranch(StringUtils.isEmpty(batchRegisterVO.getBranchName()) ? "master" : batchRegisterVO.getBranchName());
        taskInfoEntity.setScmType(batchRegisterVO.getScmType());

        //批量接入工具
        for (ToolConfigInfoVO toolConfigInfoVO : batchRegisterVO.getTools())
        {
            try
            {
                registerTool(toolConfigInfoVO, taskInfoEntity, userName);
                successTools.add(toolConfigInfoVO.getToolName());
            }
            catch (Exception e)
            {
                //tsclua工具不包括在内
                logger.error("register tool fail! tool name: {}", toolConfigInfoVO.getToolName(), e);
                failTools.add(toolConfigInfoVO.getToolName());
            }
        }

        //处理共通化路径
        pathFilterService.addDefaultFilterPaths(taskInfoEntity);
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

        if (CollectionUtils.isNotEmpty(successTools))
        {
            //启动流水线
            String buildId = pipelineService.startPipeline(taskInfoEntity, successTools, userName);
            //更新任务状态
            successTools.forEach(tool ->
                    pipelineService.updateTaskInitStep(String.valueOf(true), taskInfoEntity, buildId, tool, userName)
            );
        }
        return registerResult;
    }


    @Override
    public void registerTool(ToolConfigInfoVO toolConfigInfo, TaskInfoEntity taskInfoEntity, String user)
    {
        ToolConfigInfoEntity toolConfigInfoEntity = new ToolConfigInfoEntity();
        BeanUtils.copyProperties(toolConfigInfo, toolConfigInfoEntity);
        long taskId = toolConfigInfoEntity.getTaskId();
        if (taskId <= ComConstants.COMMON_NUM_1000L)
        {
            toolConfigInfoEntity.setTaskId(taskInfoEntity.getTaskId());
        }
        String toolName = toolConfigInfoEntity.getToolName();
        if (null == taskInfoEntity)
        {
            logger.error("task [{}] has not been registered!", taskId);
            throw new CodeCCException("0");
        }
        //1. 先进行工具信息的保存
        Long currentTime = System.currentTimeMillis();
        configTaskToolInfo(toolConfigInfoEntity);
        toolConfigInfoEntity.setCreatedBy(user);
        toolConfigInfoEntity.setCreatedDate(currentTime);
        toolConfigInfoEntity.setUpdatedBy(user);
        toolConfigInfoEntity.setUpdatedDate(currentTime);
        //接入工具时更新规则
        setDefaultClosedCheckers(toolConfigInfoEntity, user);
        //保存工具信息
        toolRepository.save(toolConfigInfoEntity);
        //3.再添加项目信息
        addNewTool2Proj(toolConfigInfoEntity, taskInfoEntity, user);
        //保存cookie数据，用于下次添加时查询
//        String coverityRegInfoCookie = toolConfigInfoEntity.getParamJson();
//        if (StringUtils.isEmpty(taskInfoEntity.getCovRegCookie()))
//        {
//            taskInfoEntity.setCovRegCookie(coverityRegInfoCookie);
//        }
        logger.info("register tool[{}] for task[{}] successful", toolName, taskId);

    }

    @Override
    @OperationHistory(funcId = FUNC_TOOL_SWITCH)
    public Boolean toolStatusManage(List<String> toolNameList, String manageType, String userName, long taskId)
    {
        TaskInfoEntity taskInfoEntity = taskRepository.findByTaskId(taskId);
        for (String toolName : toolNameList)
        {
            ToolConfigInfoEntity toolConfigInfoEntity = toolRepository.findByTaskIdAndToolName(taskId, toolName);
            if (null == toolConfigInfoEntity)
            {
                logger.error("empty tool config info found out! task id: {}, tool name: {}", taskId, toolName);
                throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS, new String[]{toolName}, null);
            }
            if (ComConstants.CommonJudge.COMMON_Y.value().equals(manageType))
            {
                toolConfigInfoEntity.setFollowStatus(toolConfigInfoEntity.getLastFollowStatus());
                logger.info("enable tool, task id: {}, tool name: {}", toolConfigInfoEntity.getTaskId(),
                        toolConfigInfoEntity.getToolName());
            }
            else
            {
                toolConfigInfoEntity.setLastFollowStatus(toolConfigInfoEntity.getFollowStatus());
                toolConfigInfoEntity.setFollowStatus(ComConstants.FOLLOW_STATUS.WITHDRAW.value());
                logger.info("disable tool, task id: {}, tool name: {}", toolConfigInfoEntity.getTaskId(),
                        toolConfigInfoEntity.getToolName());
            }
            toolRepository.save(toolConfigInfoEntity);
        }

        //如果是蓝盾项目
        if (ComConstants.BsTaskCreateFrom.BS_PIPELINE.value().equalsIgnoreCase(taskInfoEntity.getCreateFrom()) ||
                ComConstants.BsTaskCreateFrom.BS_CODECC.value().equalsIgnoreCase(taskInfoEntity.getCreateFrom()))
        {
            if (ComConstants.CommonJudge.COMMON_Y.value().equalsIgnoreCase(manageType))
            {
                pipelineService.updatePipelineTools(userName, taskId, toolNameList, taskInfoEntity,
                        ComConstants.PipelineToolUpdateType.ADD, null);
            }
            else if (ComConstants.CommonJudge.COMMON_N.value().equalsIgnoreCase(manageType))
            {
                pipelineService.updatePipelineTools(userName, taskId, toolNameList, taskInfoEntity,
                        ComConstants.PipelineToolUpdateType.REMOVE, null);
            }
        }
        return true;
    }

    @Override
    public ToolConfigInfoVO getToolByTaskIdAndName(long taskId, String toolName)
    {
        ToolConfigInfoEntity toolConfigInfoEntity = toolRepository.findByTaskIdAndToolName(taskId, toolName);
        if (null == toolConfigInfoEntity)
        {
            logger.error("no tool info found!, task id: {}, tool name: {}", taskId, toolName);
            throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS, new String[]{toolName}, null);
        }
        ToolConfigInfoVO toolConfigInfoVO = new ToolConfigInfoVO();
        BeanUtils.copyProperties(toolConfigInfoEntity, toolConfigInfoVO, "ignoreCheckers", "checkerProps");
        toolConfigInfoVO.setCheckerProps(toolConfigInfoEntity.getCheckerProps());
        return toolConfigInfoVO;
    }

    @Override
    public ToolConfigInfoVO getToolWithNameByTaskIdAndName(long taskId, String toolName)
    {
        ToolConfigInfoVO toolConfigInfoVO = getToolByTaskIdAndName(taskId, toolName);
        ToolMetaEntity toolMetaEntity = toolMetaCache.getToolFromCache(toolName);
        if(null != toolMetaEntity)
        {
            toolConfigInfoVO.setDisplayName(toolMetaEntity.getDisplayName());
        }
        return toolConfigInfoVO;
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
        boolean deleteTask = bkAuthExRegisterApi.deleteCodeCCTask(String.valueOf(taskId), projectId);
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


    private void formatToolNames(List<String> tools, StringBuffer buffer)
    {
        buffer.append("工具[");
        for (int i = 0; i < tools.size(); i++)
        {
            String toolName = tools.get(i);
            ToolMetaEntity toolMetaEntity = toolMetaCache.getToolFromCache(toolName);
            buffer.append(null != toolMetaEntity ? toolMetaEntity.getDisplayName() : "");
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
            Model modelParam = pipelineService.assembleCreatePipeline(batchRegisterVO, taskInfoEntity, defaultExecuteTime, defaultExecuteDate);
            Result<PipelineId> result = client.get(ServicePipelineResource.class).create(userName, taskInfoEntity.getProjectId(), modelParam, ChannelCode.CODECC);
            if (result.isNotOk() || null == result.getData())
            {
                logger.error("create pipeline fail! err msg: {}", result.getMessage());
                throw new CodeCCException(CommonMessageCode.BLUE_SHIELD_INTERNAL_ERROR);
            }
            pipelineId = result.getData().getId();
            logger.info("create pipeline success! project id: {}, codecc task id: {}, pipeline id: {}", taskInfoEntity.getProjectId(),
                    taskInfoEntity.getTaskId(), pipelineId);

            taskInfoEntity.setPipelineId(pipelineId);
            //表示通过codecc_web平台创建蓝盾codecc任务
            taskInfoEntity.setCreateFrom(ComConstants.BsTaskCreateFrom.BS_CODECC.value());
            //保存定时执行信息
            taskInfoEntity.setExecuteTime(defaultExecuteTime);
            taskInfoEntity.setExecuteDate(defaultExecuteDate);
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
            Element codeElment = pipelineService.getCodeElement(batchRegisterVO, relPath);
            pipelineService.updatePipelineTools(userName, taskId, toolNames, taskInfoEntity, ComConstants.PipelineToolUpdateType.ADD, codeElment);
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
                if (ComConstants.Tool.GOML.name().equals(toolConfig.getToolName()) && StringUtils.isNotEmpty(toolConfig.getParamJson()))
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


}
