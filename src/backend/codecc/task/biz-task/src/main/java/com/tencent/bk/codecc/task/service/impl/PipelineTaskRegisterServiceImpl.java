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

package com.tencent.bk.codecc.task.service.impl;

import com.tencent.bk.codecc.defect.api.ServiceCheckerRestResource;
import com.tencent.bk.codecc.defect.vo.CheckerDetailVO;
import com.tencent.bk.codecc.task.constant.TaskConstants;
import com.tencent.bk.codecc.task.constant.TaskMessageCode;
import com.tencent.bk.codecc.task.dao.ToolMetaCache;
import com.tencent.bk.codecc.task.dao.mongorepository.TaskRepository;
import com.tencent.bk.codecc.task.dao.mongorepository.ToolRepository;
import com.tencent.bk.codecc.task.model.TaskInfoEntity;
import com.tencent.bk.codecc.task.model.ToolConfigInfoEntity;
import com.tencent.bk.codecc.task.model.ToolMetaEntity;
import com.tencent.bk.codecc.task.service.AbstractTaskRegisterService;
import com.tencent.bk.codecc.task.service.PipelineService;
import com.tencent.bk.codecc.task.service.TaskRegisterService;
import com.tencent.bk.codecc.task.service.ToolService;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.bk.codecc.task.vo.TaskIdVO;
import com.tencent.bk.codecc.task.vo.ToolConfigInfoVO;
import com.tencent.bk.codecc.task.vo.ToolConfigParamJsonVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.exception.StreamException;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.auth.api.external.BkAuthExRegisterApi;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.tencent.devops.common.constant.CommonMessageCode.UTIL_EXECUTE_FAIL;

/**
 * 流水线创建任务实现类
 * 分三种情况：
 * 已注册且有效，则返回项目已经存在
 * 已注册且无效，则重置为有效，并且添加资源到蓝盾平台，添加或者更新工具配置
 * 未注册，则全新注册
 *
 * @version V1.0
 * @date 2019/5/14
 */
@Service("pipelineTaskRegisterService")
public class PipelineTaskRegisterServiceImpl extends AbstractTaskRegisterService
{
    private static Logger logger = LoggerFactory.getLogger(PipelineTaskRegisterServiceImpl.class);

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private PipelineService pipelineService;

    @Autowired
    private ToolMetaCache toolMetaCache;

    @Autowired
    @Qualifier("devopsToolService")
    private ToolService toolService;

    @Autowired
    @Qualifier("devopsTaskRegisterService")
    private TaskRegisterService taskRegisterService;

    @Autowired
    private ToolRepository toolRepository;

    @Autowired
    private BkAuthExRegisterApi bkAuthExRegisterApi;

    @Autowired
    private Client client;

    @Override
    public TaskIdVO registerTask(TaskDetailVO taskDetailVO, String userName)
    {
        String nameEn = getTaskStreamName(taskDetailVO.getProjectId(), taskDetailVO.getPipelineName());
        taskDetailVO.setNameEn(nameEn);
        boolean streamRegistered = taskRepository.existsByNameEn(nameEn);
        TaskInfoEntity taskInfoEntity = new TaskInfoEntity();
        if (streamRegistered)
        {
            taskInfoEntity = taskRepository.findByNameEn(nameEn);
        }

        //已注册且有效
        if (streamRegistered && taskInfoEntity.getStatus() == 0)
        {
            logger.error("the task has been registered, task name: {}", nameEn);
            throw new CodeCCException(TaskMessageCode.STREAM_HAS_EXISTS_ERR);
        }
        //已注册且无效
        else if (streamRegistered && taskInfoEntity.getStatus() == 1)
        {
            //1. 注册权限中心资源
            bkAuthExRegisterApi.registerCodeCCTask(userName, String.valueOf(taskInfoEntity.getTaskId()),
                    taskInfoEntity.getNameEn(), taskInfoEntity.getProjectId());
            //2. 更新任务信息
            updateTaskInfo(taskDetailVO.getDevopsCodeLang(), taskDetailVO.getPipelineName(),
                    taskDetailVO.getProjectName(), taskDetailVO.getCompilePlat(), taskInfoEntity);

            //3. 添加或者更新工具配置
            addOrConfigTools(taskDetailVO, taskInfoEntity, userName);
            taskRepository.save(taskInfoEntity);
            return new TaskIdVO(taskInfoEntity.getTaskId(), taskInfoEntity.getNameEn());
        }
        //未注册
        else if (!streamRegistered)
        {
            TaskInfoEntity taskInfoResult = taskRegisterService.saveTaskInfo(taskDetailVO, userName);
            if (null == taskInfoResult || !StringUtils.isNumeric(String.valueOf(taskInfoResult.getTaskId())))
            {
                logger.error("save new task info fail! project id: {}, project name: {}", taskDetailVO.getProjectId(),
                        taskDetailVO.getProjectName());
                throw new CodeCCException(TaskMessageCode.REGISTER_TASK_FAIL);
            }
            //将任务注册到权限中心
            bkAuthExRegisterApi.registerCodeCCTask(userName, String.valueOf(taskInfoResult.getTaskId()),
                    taskInfoResult.getNameEn(), taskInfoResult.getProjectId());
            taskInfoResult.setPipelineId(taskDetailVO.getPipelineId());
            taskInfoResult.setCreateFrom(ComConstants.BsTaskCreateFrom.BS_PIPELINE.value());
            //添加或者更新工具配置
            addOrConfigTools(taskDetailVO, taskInfoResult, userName);
            taskRepository.save(taskInfoResult);
            return new TaskIdVO(taskInfoResult.getTaskId(), taskInfoResult.getNameEn());
        }
        else
        {
            logger.error("Project status exception! bs_pipeline_id={}, stream_name={}", taskDetailVO.getPipelineId(),
                    nameEn);
            throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS, new String[]{"项目信息"}, null);
        }
    }

    @Override
    public TaskInfoEntity saveTaskInfo(TaskDetailVO taskDetailVO, String userName)
    {
        return null;
    }


    @Override
    public Boolean updateTaskFromPipeline(TaskDetailVO taskDetailVO, String userName)
    {
        long taskId = taskDetailVO.getTaskId();
        TaskInfoEntity taskInfoEntity = taskRepository.findByTaskId(taskId);
        if (StringUtils.isEmpty(taskInfoEntity.getProjectId()) ||
                !ComConstants.BsTaskCreateFrom.BS_PIPELINE.value().equalsIgnoreCase(taskInfoEntity.getCreateFrom()))
        {
            logger.error("task not from pipeline should be updated, task id: {}", taskId);
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{String.valueOf(taskId)}, null);
        }
        //保存任务信息更新
        setTaskValue(taskDetailVO, taskInfoEntity);
        //添加或者更新工具配置
        addOrConfigTools(taskDetailVO, taskInfoEntity, userName);
        taskRepository.save(taskInfoEntity);
        logger.info("update task info from pipeline successfully! task id: {}", taskId);
        return true;
    }


    private void setTaskValue(TaskDetailVO taskDetailVO, TaskInfoEntity taskInfoEntity)
    {
        if (StringUtils.isNotEmpty(taskDetailVO.getPipelineName()))
        {
            String newCnName = handleCnName(taskDetailVO.getPipelineName());
            if (!newCnName.equals(taskInfoEntity.getNameCn()))
            {
                taskInfoEntity.setNameCn(newCnName);
            }
        }
        if (StringUtils.isNotEmpty(taskDetailVO.getProjectName()))
        {
            taskInfoEntity.setProjectName(taskDetailVO.getProjectName());
        }
        if (StringUtils.isNotEmpty(taskDetailVO.getCompilePlat()))
        {
            taskInfoEntity.setCompilePlat(taskDetailVO.getCompilePlat());
        }
        if (StringUtils.isNotEmpty(taskDetailVO.getDevopsCodeLang()))
        {
            long codeLang = pipelineService.convertDevopsCodeLangToCodeCC(taskDetailVO.getDevopsCodeLang());
            taskInfoEntity.setCodeLang(codeLang);
        }
    }


    /**
     * 更新任务信息
     *
     * @param devopsCodeLang
     * @param pipelineName
     * @param projectName
     * @param compilePlat
     * @param taskInfoEntity
     */
    private void updateTaskInfo(String devopsCodeLang, String pipelineName, String projectName,
                                String compilePlat, TaskInfoEntity taskInfoEntity)
    {
        taskInfoEntity.setStatus(TaskConstants.TaskStatus.ENABLE.value());
        taskInfoEntity.setDisableTime("");

        try
        {
            taskInfoEntity.setCodeLang(pipelineService.convertDevopsCodeLangToCodeCC(devopsCodeLang));
        }
        catch (StreamException e)
        {
            logger.error("deserialize devops code lang fail! code lang info: {}", devopsCodeLang);
            throw new CodeCCException(UTIL_EXECUTE_FAIL);
        }

        //更新任务中文名
        taskInfoEntity.setNameCn(handleCnName(pipelineName));
        //更新蓝盾任务名
        taskInfoEntity.setProjectName(projectName);
        //更新编译平台
        taskInfoEntity.setCompilePlat(compilePlat);
        taskRepository.save(taskInfoEntity);
    }


    /**
     * 更新工具配置信息和工具接入状态
     *
     * @param taskDetailVO
     * @param taskInfoEntity
     * @param userName
     */
    private void addOrConfigTools(TaskDetailVO taskDetailVO, TaskInfoEntity taskInfoEntity, String userName)
    {

        List<String> newAddTools = new ArrayList<>();
        List<String> modifyTools = new ArrayList<>();
        List<String> disableTools = new ArrayList<>();
        List<String> enableTools = new ArrayList<>();

        // 对工具进行分类，区分是新添加工具、信息修改工具、停用工具、启用工具
        classifyToolOperateType(taskDetailVO.getDevopsTools(), taskInfoEntity, newAddTools, modifyTools, disableTools, enableTools);

        // 添加新工具
        List<String> failTools = new ArrayList<>();
        List<String> successTools = new ArrayList<>();
        for (String toolName : newAddTools)
        {
            try
            {
                ToolConfigInfoVO toolConfigInfoVO = instBatchToolInfoModel(taskDetailVO, taskInfoEntity,
                        taskInfoEntity.getTaskId(), toolName);
                toolService.registerTool(toolConfigInfoVO, taskInfoEntity, userName);
                successTools.add(toolName);
            }
            catch (Exception e)
            {
                logger.error("register tool fail! tool name: {}", toolName, e);
                failTools.add(toolName);
            }
        }

        // 如果有添加过该工具，则修改工具配置
        long curTime = System.currentTimeMillis();
        for (String toolName : modifyTools)
        {
            modifyTool(taskDetailVO, taskInfoEntity, toolName, curTime, userName);
        }

        //更新工具停用启用状态
        for (String toolName : enableTools)
        {
            ToolConfigInfoEntity toolConfigInfoEntity = enableOrDisableTool(taskInfoEntity, ComConstants.FOLLOW_STATUS.ACCESSED.value(), toolName);
            if (null != toolConfigInfoEntity)
            {
                toolRepository.save(toolConfigInfoEntity);
                logger.info("enable tool, project id: {}, tool: {}, status = {}", toolConfigInfoEntity.getTaskId(),
                        toolConfigInfoEntity.getToolName(), toolConfigInfoEntity.getFollowStatus());
            }
        }
        for (String toolName : disableTools)
        {
            ToolConfigInfoEntity toolConfigInfoEntity = enableOrDisableTool(taskInfoEntity, ComConstants.FOLLOW_STATUS.WITHDRAW.value(), toolName);
            if (null != toolConfigInfoEntity)
            {
                toolRepository.save(toolConfigInfoEntity);
                logger.info("disable tool, project id: {}, tool: {}, status = {}", toolConfigInfoEntity.getTaskId(),
                        toolConfigInfoEntity.getToolName(), toolConfigInfoEntity.getFollowStatus());
            }
        }
    }


    /**
     * 对工具进行分类，区分是新添加工具、信息修改工具、停用工具、启用工具
     *
     * @param devopsTools
     * @param taskInfoEntity
     * @param newTools
     * @param modifyTools
     * @param disableTools
     * @param enableTools
     */
    private void classifyToolOperateType(String devopsTools, TaskInfoEntity taskInfoEntity,
                                         List<String> newTools, List<String> modifyTools,
                                         List<String> disableTools, List<String> enableTools)
    {
        // 请求工具列表
        Set<String> reqToolSet = new HashSet<>();
        JSONArray newToolJsonArray = new JSONArray(devopsTools);
        for (int i = 0; i < newToolJsonArray.length(); i++)
        {
            if (StringUtils.isNotEmpty(newToolJsonArray.getString(i)))
            {
                reqToolSet.add(newToolJsonArray.getString(i));
            }
        }
        logger.info("req tool set size: {}, req tool content: {}", reqToolSet.size(), reqToolSet.toString());
        // 旧工具列表
        List<ToolConfigInfoEntity> oldToolList = taskInfoEntity.getToolConfigInfoList();
        Map<String, String> oldToolMap;
        if (CollectionUtils.isEmpty(oldToolList))
        {
            oldToolMap = new HashMap<>();
        }
        else
        {
            oldToolMap = oldToolList.stream().
                    collect(Collectors.toMap(
                            ToolConfigInfoEntity::getToolName, toolConfigInfoEntity -> String.valueOf(toolConfigInfoEntity.getFollowStatus()), (k, v) -> v));
        }
        for (String reqTool : reqToolSet)
        {
            if (!oldToolMap.containsKey(reqTool))
            {
                newTools.add(reqTool);
            }
            else
            {
                int toolFollowStatus = Integer.valueOf(oldToolMap.get(reqTool));
                if (ComConstants.FOLLOW_STATUS.WITHDRAW.value() == toolFollowStatus)
                {
                    enableTools.add(reqTool);
                }
                else
                {
                    modifyTools.add(reqTool);
                }
            }
            oldToolMap.remove(reqTool);
        }

        if (MapUtils.isNotEmpty(oldToolMap))
        {
            for (Map.Entry<String, String> entry : oldToolMap.entrySet())
            {
                int toolFollowStatus = Integer.valueOf(entry.getValue());
                if (ComConstants.FOLLOW_STATUS.WITHDRAW.value() != toolFollowStatus)
                {
                    disableTools.add(entry.getKey());
                }
            }
        }
    }


    /**
     * 组装工具添加数据
     *
     * @param taskDetailVO
     * @param taskInfoEntity
     * @param taskId
     * @param toolName
     * @return
     */
    private ToolConfigInfoVO instBatchToolInfoModel(TaskDetailVO taskDetailVO, TaskInfoEntity taskInfoEntity,
                                                    long taskId, String toolName)
    {
        ToolConfigInfoVO toolConfig = new ToolConfigInfoVO();
        toolConfig.setTaskId(taskId);
        toolConfig.setToolName(toolName);
        JSONObject paramJson = getParamJson(taskDetailVO, taskInfoEntity, toolName);
        toolConfig.setParamJson(paramJson.toString());
        return toolConfig;
    }


    /**
     * 获取参数
     *
     * @param taskDetailVO
     * @param taskInfoEntity
     * @param toolName
     * @return
     */
    private JSONObject getParamJson(TaskDetailVO taskDetailVO, TaskInfoEntity taskInfoEntity,
                                    String toolName)
    {
        JSONObject paramJson = new JSONObject();

        List<ToolConfigParamJsonVO> toolConfigParams = taskDetailVO.getDevopsToolParams();
        Map<String, String> toolConfigParamMap;
        if (CollectionUtils.isNotEmpty(toolConfigParams))
        {
            toolConfigParamMap = toolConfigParams.stream().
                    collect(Collectors.toMap(ToolConfigParamJsonVO::getVarName, ToolConfigParamJsonVO::getChooseValue, (k, v) -> v));
        }
        else
        {
            toolConfigParamMap = new HashMap<>();
        }
        //获取工具的配置的个性化参数
        ToolMetaEntity toolMetaEntity = toolMetaCache.getToolFromCache(toolName);
        String toolParamJson = toolMetaEntity.getParams();
        if (StringUtils.isEmpty(toolParamJson))
        {
            return paramJson;
        }

        //如果是蓝盾流水线项目，且参数不带go_path，就设置为空串
        boolean exceptRelPath = false;
        if (ComConstants.BsTaskCreateFrom.BS_PIPELINE.value().equalsIgnoreCase(taskInfoEntity.getCreateFrom()))
        {
            exceptRelPath = true;
            if (CollectionUtils.isNotEmpty(toolConfigParams))
            {
                if (toolConfigParams.stream().noneMatch(toolConfigParamJsonVO ->
                        toolConfigParamJsonVO.getVarName().equalsIgnoreCase(ComConstants.PARAMJSON_KEY_GO_PATH)))
                {
                    toolConfigParamMap.put(ComConstants.PARAMJSON_KEY_GO_PATH, "");
                }
            }
        }

        //将蓝盾传入的个性化参数组装进工具的paramJson字段中
        JSONArray toolParamsArray = new JSONArray(toolParamJson);
        if (toolParamsArray.length() > 0)
        {
            int paramLength = toolParamsArray.length();
            for (int i = 0; i < paramLength; i++)
            {
                JSONObject paramJsonObj = toolParamsArray.getJSONObject(i);
                String varName = paramJsonObj.getString("varName");
                try
                {
                    // 蓝盾流水线项目不需要rel_path参数
                    if (exceptRelPath && ComConstants.PARAMJSON_KEY_REL_PATH.equals(varName))
                    {
                        continue;
                    }
                    String varValue = toolConfigParamMap.get(varName);
                    paramJson.put(varName, varValue);
                }
                catch (JSONException e)
                {
                    logger.error("传入参数错误：参数[{}]不能为空", varName, e);
                    throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{varName}, null);
                }
            }
        }
        return paramJson;
    }


    /**
     * 修改工具
     *
     * @param taskDetailVO
     * @param taskInfoEntity
     * @param toolConfigInfoEntity
     * @param curTime
     * @param userName
     */
    private void modifyTool(TaskDetailVO taskDetailVO, TaskInfoEntity taskInfoEntity,
                            String toolName, long curTime, String userName)
    {
        JSONObject paramJson = getParamJson(taskDetailVO, taskInfoEntity, toolName);
        List<ToolConfigInfoEntity> toolConfigInfoEntityList = taskInfoEntity.getToolConfigInfoList();
        if (CollectionUtils.isNotEmpty(toolConfigInfoEntityList))
        {
            if (paramJson.length() > 0)
            {
                ToolConfigInfoEntity toolConfigInfoEntity = toolConfigInfoEntityList.stream().
                        filter(toolConfigInfo -> toolConfigInfo.getToolName().equalsIgnoreCase(toolName))
                        .findFirst().get();
                String oldParamJson = toolConfigInfoEntity.getParamJson();
                toolConfigInfoEntity.setUpdatedBy(userName);
                toolConfigInfoEntity.setUpdatedDate(curTime);
                toolConfigInfoEntity.setParamJson(paramJson.toString());
                toolRepository.save(toolConfigInfoEntity);
                //只有当eslint工具时，需要添加屏蔽规则
                if (ComConstants.Tool.ESLINT.name().equalsIgnoreCase(toolName))
                {
                    updateIgnoreCheckersWhenModify(taskInfoEntity.getTaskId(), toolName, paramJson.toString(), oldParamJson);
                }

            }
        }
    }


    /**
     * 根据工具设置的变更关闭规则
     *
     * @param taskId
     * @param paramJson
     * @param oldParamJson
     */
    private void updateIgnoreCheckersWhenModify(long taskId, String toolName, String paramJson, String oldParamJson)
    {
        JSONObject paramJsonObj = StringUtils.isBlank(paramJson) ? new JSONObject() : new JSONObject(paramJson);
        JSONObject oldParamJsonObj = StringUtils.isBlank(oldParamJson) ? new JSONObject() : new JSONObject(oldParamJson);

        String eslintRc = paramJsonObj.has("eslint_rc") ? paramJsonObj.getString("eslint_rc") : "";
        String oldEslintRc = oldParamJsonObj.has("eslint_rc") ? oldParamJsonObj.getString("eslint_rc") : "";

        //框架没变化不处理
        //当之前框架不为空时，可直接判断两者是否相等，相等则不处理
        if (StringUtils.isBlank(eslintRc))
        {
            return;
        }
        else if (StringUtils.isNotEmpty(oldEslintRc) && oldEslintRc.equalsIgnoreCase(eslintRc))
        {
            return;
        }

        //1. 查询变更后框架默认关闭的所有规则
        ToolConfigInfoVO toolConfigInfoVO = new ToolConfigInfoVO();
        toolConfigInfoVO.setToolName(ComConstants.Tool.ESLINT.name());
        toolConfigInfoVO.setParamJson(paramJson);
        Result<List<CheckerDetailVO>> allEslintChecker = client.get(ServiceCheckerRestResource.class).queryAllChecker(toolConfigInfoVO);
        if (allEslintChecker.isNotOk() || CollectionUtils.isEmpty(allEslintChecker.getData()))
        {
            logger.error("query all eslint checker fail!");
            return;
        }

        List<String> defaultCloseKeys = allEslintChecker.getData().stream()
                .filter(eslintChecker ->
                        !ComConstants.CheckerPkgKind.DEFAULT.value().equalsIgnoreCase(eslintChecker.getPkgKind())
                )
                .map(CheckerDetailVO::getCheckerKey
                )
                .collect(Collectors.toList());

        //2. 查询变更前框架默认关闭的规则
        ToolConfigInfoVO oldToolCofigInfoVO = new ToolConfigInfoVO();
        oldToolCofigInfoVO.setToolName(ComConstants.Tool.ESLINT.name());
        oldToolCofigInfoVO.setParamJson(oldParamJson);
        Result<List<CheckerDetailVO>> oldEslintChecker = client.get(ServiceCheckerRestResource.class).queryAllChecker(oldToolCofigInfoVO);
        if (oldEslintChecker.isNotOk() || CollectionUtils.isEmpty(oldEslintChecker.getData()))
        {
            logger.error("query all eslint checker fail!");
            return;
        }
        List<String> oldDefaultCloseKeys = oldEslintChecker.getData().stream()
                .filter(eslintChecker ->
                        !ComConstants.CheckerPkgKind.DEFAULT.value().equalsIgnoreCase(eslintChecker.getPkgKind())
                )
                .map(CheckerDetailVO::getCheckerKey
                )
                .collect(Collectors.toList());

        defaultCloseKeys.removeAll(oldDefaultCloseKeys);

        Result<Boolean> ignoreCheckerResult = client.get(ServiceCheckerRestResource.class).mergeIgnoreChecker(taskId, toolName, defaultCloseKeys);
        if (ignoreCheckerResult.isNotOk() || null == ignoreCheckerResult.getData() || !ignoreCheckerResult.getData())
        {
            logger.error("no ignore checker found!");
        }
        else
        {
            logger.info("add close checker success!, task id: {}, tool name: {}", taskId, toolName);
        }
    }

    /**
     * 启用或者停用工具
     *
     * @param taskInfoEntity
     * @param followStatus
     * @param toolName
     * @return
     */
    private ToolConfigInfoEntity enableOrDisableTool(TaskInfoEntity taskInfoEntity, int followStatus, String toolName)
    {
        List<ToolConfigInfoEntity> toolConfigInfoEntityList = taskInfoEntity.getToolConfigInfoList();
        if (CollectionUtils.isNotEmpty(toolConfigInfoEntityList))
        {
            ToolConfigInfoEntity toolConfigInfoEntity = toolConfigInfoEntityList.stream().
                    filter(toolConfigInfo -> toolConfigInfo.getToolName().equalsIgnoreCase(toolName))
                    .findFirst().get();
            toolConfigInfoEntity.setFollowStatus(followStatus);
            return toolConfigInfoEntity;
        }
        return null;
    }


    @Override
    public Boolean modifyTimeAnalysisTask(List<String> executeDate, String executeTime, long taskId, String userName)
    {
        return null;
    }

}
