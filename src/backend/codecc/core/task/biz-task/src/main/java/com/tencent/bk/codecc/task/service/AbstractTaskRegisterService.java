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

package com.tencent.bk.codecc.task.service;

import com.tencent.bk.codecc.defect.api.ServiceCheckerSetRestResource;
import com.tencent.bk.codecc.task.constant.TaskConstants;
import com.tencent.bk.codecc.task.dao.mongorepository.TaskRepository;
import com.tencent.bk.codecc.task.dao.mongorepository.ToolRepository;
import com.tencent.bk.codecc.task.dao.mongotemplate.ToolDao;
import com.tencent.bk.codecc.task.model.TaskInfoEntity;
import com.tencent.bk.codecc.task.model.ToolConfigInfoEntity;
import com.tencent.bk.codecc.task.service.specialparam.SpecialParamUtil;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.bk.codecc.task.vo.ToolConfigInfoVO;
import com.tencent.bk.codecc.task.vo.ToolConfigParamJsonVO;
import com.tencent.devops.common.api.ToolMetaBaseVO;
import com.tencent.devops.common.api.checkerset.CheckerSetVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.CheckerSetType;
import com.tencent.devops.common.constant.ComConstants.Tool;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.constant.RedisKeyConstants;
import com.tencent.devops.common.redis.lock.RedisLock;
import com.tencent.devops.common.service.ToolMetaCacheService;
import com.tencent.devops.common.util.MD5Utils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.tencent.devops.common.util.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.tencent.devops.common.auth.api.pojo.external.AuthExConstantsKt.KEY_CREATE_FROM;
import static com.tencent.devops.common.auth.api.pojo.external.AuthExConstantsKt.PREFIX_TASK_INFO;

/**
 * 任务注册抽象类
 *
 * @version V1.0
 * @date 2019/5/6
 */
@Slf4j
public abstract class AbstractTaskRegisterService implements TaskRegisterService
{
    @Autowired
    private ToolMetaCacheService toolMetaCache;

    @Autowired
    protected TaskRepository taskRepository;

    @Autowired
    protected StringRedisTemplate redisTemplate;

    @Autowired
    protected PipelineService pipelineService;

    @Autowired
    protected ToolService toolService;

    @Autowired
    protected PathFilterService pathFilterService;

    @Autowired
    protected ToolRepository toolRepository;

    @Autowired
    protected SpecialParamUtil specialParamUtil;

    @Autowired
    protected ToolDao toolDao;

    @Autowired
    protected Client client;

    @Override
    public Boolean checkeIsStreamRegistered(String nameEn)
    {
        return taskRepository.existsByNameEn(nameEn);
    }

    /**
     * 创建代码扫描任务
     *
     * @param taskDetailVO
     * @param userName
     */
    protected TaskInfoEntity createTask(TaskDetailVO taskDetailVO, String userName)
    {
        // 生成任务英文名
        taskDetailVO.setStatus(TaskConstants.TaskStatus.ENABLE.value());

        // 校验新接入的项目英文名是否已经被注册过
        if (checkeIsStreamRegistered(taskDetailVO.getNameEn()))
        {
            log.error("the task name has been registered! task name: {}", taskDetailVO.getNameCn());
            throw new CodeCCException(CommonMessageCode.KEY_IS_EXIST, new String[]{taskDetailVO.getNameCn()}, null);
        }

        // 创建新项目到数据库
        TaskInfoEntity taskInfoEntity = new TaskInfoEntity();
        BeanUtils.copyProperties(taskDetailVO, taskInfoEntity, "toolConfigInfoList");
        long currentTime = System.currentTimeMillis();
        taskInfoEntity.setCreatedBy(userName);
        taskInfoEntity.setCreatedDate(currentTime);
        taskInfoEntity.setUpdatedBy(userName);
        taskInfoEntity.setUpdatedDate(currentTime);
        //设置初始项目接口人及项目成员为自己
        List<String> users = new ArrayList<String>()
        {{
            add(userName);
        }};
        taskInfoEntity.setTaskOwner(users);
        taskInfoEntity.setTaskMember(users);

        //获取taskId主键
        long taskId = redisTemplate.opsForValue().increment(RedisKeyConstants.CODECC_TASK_ID, 1L);
        taskInfoEntity.setTaskId(taskId);
        taskDetailVO.setTaskId(taskId);

        //新创建任务设置为增量扫描
        taskInfoEntity.setScanType(ComConstants.ScanType.INCREMENTAL.code);

        //处理共通化路径
        pathFilterService.addDefaultFilterPaths(taskInfoEntity);

        //保存项目信息
        TaskInfoEntity taskInfoResult = taskRepository.save(taskInfoEntity);
        log.info("save task info successfully! task id: {}, entity id: {}", taskId, taskInfoResult.getEntityId());

        // 缓存创建来源
        redisTemplate.opsForHash().put(PREFIX_TASK_INFO + taskId, KEY_CREATE_FROM, taskInfoResult.getCreateFrom());
        return taskInfoResult;
    }

    /**
     * 为了防止蓝盾平台调用创建任务接口添加任务校验中文名称失败，这里对中文名按照校验的正则表达式做一次处理
     *
     * @param cnName
     * @return
     */
    protected String handleCnName(String cnName)
    {
        if (StringUtils.isEmpty(cnName))
        {
            log.error("cn name is empty!");
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{cnName}, null);
        }
        StringBuffer a = new StringBuffer(cnName);
        for (int i = 0; i < a.length(); i++)
        {
            String tmpStr = a.substring(i, i + 1);
            if (!regexMatch("[a-zA-Z0-9_\\u4e00-\\u9fa5]", tmpStr))
            {//单个字符匹配不上用下划线代替
                a.replace(i, i + 1, "_");
            }
        }
        //长度限制50个字符以内
        if (a.length() > 50)
        {
            return a.substring(0, 50);
        }
        return a.toString();
    }


    /**
     * 正则匹配,true表示匹配成功，false表示匹配失败
     *
     * @param regex
     * @param sourceText
     * @return
     */
    private boolean regexMatch(String regex, String sourceText)
    {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(sourceText);
        return matcher.matches();
    }

    /**
     * 获取任务英文名
     *
     * @param projectId
     * @param pipelineId
     * @param createFrom
     * @return
     */
    public String getTaskStreamName(String projectId, String pipelineId, String createFrom)
    {
        String md5Str = MD5Utils.getMD5(String.format("%s%s", projectId, pipelineId));
        String result = null;
        if (ComConstants.BsTaskCreateFrom.BS_PIPELINE.value().equals(createFrom))
        {
            result = ComConstants.PIPELINE_ENNAME_PREFIX + "_" + md5Str;
        }
        else if (ComConstants.BsTaskCreateFrom.BS_CODECC.value().equals(createFrom))
        {
            result = ComConstants.CODECC_ENNAME_PREFIX + "_" + md5Str;
        }
        else if (ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value().equals(createFrom))
        {
            result = ComConstants.GONGFENG_ENNAME_PREFIX + "_" + md5Str;
        }

        return result;
    }

    /**
     * 组装工具添加数据
     *
     * @param taskDetailVO
     * @param toolName
     * @return
     */
    protected ToolConfigInfoVO instBatchToolInfoModel(TaskDetailVO taskDetailVO, String toolName)
    {
        ToolConfigInfoVO toolConfig = new ToolConfigInfoVO();
        toolConfig.setTaskId(taskDetailVO.getTaskId());
        toolConfig.setToolName(toolName);
        JSONObject paramJson = getParamJson(taskDetailVO, toolName, taskDetailVO.getCreateFrom());
        toolConfig.setParamJson(paramJson.toString());
        return toolConfig;
    }

    /**
     * 获取参数
     *
     * @param taskDetailVO
     * @param toolName
     * @param createFrom
     * @return
     */
    @Override
    public JSONObject getParamJson(TaskDetailVO taskDetailVO, String toolName, String createFrom)
    {
        JSONObject paramJson = new JSONObject();

        List<ToolConfigParamJsonVO> toolConfigParams = taskDetailVO.getDevopsToolParams();
        Map<String, String> toolConfigParamMap;
        if (CollectionUtils.isNotEmpty(toolConfigParams))
        {
            toolConfigParamMap = toolConfigParams.stream()
                    .map(toolConfigParamJsonVO ->
                    {
                        if (StringUtils.isEmpty(toolConfigParamJsonVO.getChooseValue()))
                        {
                            toolConfigParamJsonVO.setChooseValue("");
                        }
                        return toolConfigParamJsonVO;
                    })
                    .collect(Collectors.toMap(ToolConfigParamJsonVO::getVarName, ToolConfigParamJsonVO::getChooseValue, (k, v) -> v));
        }
        else
        {
            toolConfigParamMap = new HashMap<>();
        }
        //获取工具的配置的个性化参数
        ToolMetaBaseVO toolMetaBaseVO = toolMetaCache.getToolBaseMetaCache(toolName);
        String toolParamJson = toolMetaBaseVO.getParams();
        if (StringUtils.isEmpty(toolParamJson))
        {
            return paramJson;
        }

        //如果是蓝盾流水线项目，且参数不带go_path，就设置为空串
        boolean exceptRelPath = false;
        if (ComConstants.BsTaskCreateFrom.BS_PIPELINE.value().equalsIgnoreCase(createFrom))
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
        for (int i = 0; i < toolParamsArray.length(); i++)
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
                log.error("传入参数错误：参数[{}]不能为空", varName, e);
                throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{varName}, null);
            }
        }
        return paramJson;
    }

    protected void adaptV3AtomCodeCC(TaskDetailVO taskDetailVO)
    {
        // 所有任务添加 CLOC 工具
        CheckerSetVO clocCheckerSet = new CheckerSetVO();
        clocCheckerSet.setCheckerSetId("standard_cloc");
        clocCheckerSet.setToolList(Collections.singleton(Tool.CLOC.name()));
        clocCheckerSet.setVersion(Integer.MAX_VALUE);
        clocCheckerSet.setCodeLang(1073741824L);
        if ((taskDetailVO.getCodeLang() & 1073741824L) <= 0) {
            taskDetailVO.setCodeLang(taskDetailVO.getCodeLang() + 1073741824L);
        }
        taskDetailVO.getCheckerSetList().add(clocCheckerSet);
        // 初始化规则集列表
        Set<String> checkerSetIdList = taskDetailVO.getCheckerSetList().stream().map(CheckerSetVO::getCheckerSetId).collect(Collectors.toSet());
        Result<List<CheckerSetVO>> result;

        // 兼容旧逻辑判断
        if ((CollectionUtils.isEmpty(taskDetailVO.getLanguages()) && taskDetailVO.getCheckerSetType() == null)
                || taskDetailVO.getCheckerSetType() == CheckerSetType.NORMAL) {
            log.info("query checker set {}", taskDetailVO.getProjectId());
            result = client.get(ServiceCheckerSetRestResource.class)
                    .queryCheckerSets(checkerSetIdList, taskDetailVO.getProjectId());
        } else {
            log.info("query checker set for open source {}", taskDetailVO.getProjectId());
            result = client.get(ServiceCheckerSetRestResource.class).queryCheckerSetsForOpenScan(
                    new HashSet<>(taskDetailVO.getCheckerSetList()), taskDetailVO.getProjectId());
        }

        if (result.isNotOk() || CollectionUtils.isEmpty(result.getData()))
        {
            String errorLog = "query checker sets fail, result: " + result;
            log.error(errorLog);
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL, new String[]{errorLog});
        }
        log.info("adapt v3 atom checker set result for task: " + taskDetailVO.getTaskId() + ", " + result);

        //要对语言进行过滤
        List<CheckerSetVO> resultCheckerSetList = result.getData();
        List<CheckerSetVO> finalCheckerSetList = resultCheckerSetList.stream().filter(checkerSetVO ->
            (checkerSetVO.getCodeLang() & taskDetailVO.getCodeLang()) > 0L
        ).collect(Collectors.toList());
        taskDetailVO.setCheckerSetList(finalCheckerSetList);
        log.info("adapt v3 atom final checker set for task: " + taskDetailVO.getTaskId() + ", " + finalCheckerSetList);

        // 初始化工具列表
        Set<String> reqToolSet = new HashSet<>();
        taskDetailVO.getCheckerSetList().forEach(checkerSetVO ->
        {
            if (CollectionUtils.isNotEmpty(checkerSetVO.getToolList()))
            {
                reqToolSet.addAll(checkerSetVO.getToolList());
            }
        });
        log.info("adapt v3 atom req tool set for task: " + taskDetailVO.getTaskId() + ", " + reqToolSet);

        List<ToolConfigInfoVO> toolList = new ArrayList<>();
        reqToolSet.forEach(toolName ->
        {
            ToolConfigInfoVO toolConfigInfoVO = instBatchToolInfoModel(taskDetailVO, toolName);
            toolList.add(toolConfigInfoVO);
        });
        log.info("adapt v3 atom get tool list for task: " + taskDetailVO.getTaskId() + ", " + toolList);
        taskDetailVO.setToolConfigInfoList(toolList);
    }

    /**
     * 更新保存工具，包括新添加工具、信息修改工具、停用工具、启用工具
     *
     * @param taskDetailVO
     * @param taskInfoEntity
     * @param userName
     * @param forceFullScanTools
     */
    protected void upsert(TaskDetailVO taskDetailVO, TaskInfoEntity taskInfoEntity, String userName,
            List<String> forceFullScanTools) {
        String lockKey = String.format("lock:task:upsert_tool_config:%s", taskInfoEntity.getTaskId());
        RedisLock redisLock = new RedisLock(redisTemplate, lockKey, 5);

        try {
            redisLock.lock();
            upsertCore(taskDetailVO, taskInfoEntity, userName, forceFullScanTools);
        } catch (Exception e) {
            log.error("task upsert tool config fail, task id: {}", taskInfoEntity.getTaskId(), e);
        } finally {
            redisLock.unlock();
        }
    }

    /**
     * 更新保存工具，包括新添加工具、信息修改工具、停用工具、启用工具
     *
     * @param taskDetailVO
     * @param taskInfoEntity
     * @param userName
     * @param forceFullScanTools
     */
    private void upsertCore(TaskDetailVO taskDetailVO, TaskInfoEntity taskInfoEntity, String userName,
            List<String> forceFullScanTools) {
        // 旧工具列表
        TaskInfoEntity refreshTaskInfo = taskRepository.findFirstByTaskId(taskInfoEntity.getTaskId());
        List<ToolConfigInfoEntity> oldToolList = refreshTaskInfo != null
                ? refreshTaskInfo.getToolConfigInfoList()
                : taskInfoEntity.getToolConfigInfoList();

        // 清理脏数据
        clearDirtyToolConfig(taskInfoEntity.getTaskId(), oldToolList);

        Map<String, ToolConfigInfoEntity> oldToolMap;
        if (CollectionUtils.isEmpty(oldToolList))
        {
            oldToolMap = new HashMap<>();
        }
        else
        {
            // 并发导致小部分引用的toolConfig可能为null，需过滤处理
            oldToolMap = oldToolList.stream().filter(Objects::nonNull).collect(Collectors
                    .toMap(ToolConfigInfoEntity::getToolName, toolConfigInfoEntity -> toolConfigInfoEntity,
                            (k, v) -> v));
        }

        List<ToolConfigInfoEntity> toolConfigInfoEntityList = new ArrayList<>();
        long taskId = taskInfoEntity.getTaskId();
        List<ToolConfigInfoVO> toolList = taskDetailVO.getToolConfigInfoList();
        long curTime = System.currentTimeMillis();
        for (ToolConfigInfoVO reqTool : toolList)
        {
            String toolName = reqTool.getToolName();
            ToolConfigInfoEntity toolConfigInfoEntity = oldToolMap.get(toolName);
            if (toolConfigInfoEntity == null)
            {
                try
                {
                    toolConfigInfoEntity = toolService.registerTool(reqTool, taskInfoEntity, userName);
                    toolConfigInfoEntityList.add(toolConfigInfoEntity);
                    log.info("add task {} tool {}", taskId, toolName);
                    forceFullScanTools.add(toolName);
                }
                catch (Exception e)
                {
                    log.error("add task {} tool {} fail!", taskId, toolName, e);
                }
            }
            else
            {
                // 重新启用工具
                if (ComConstants.FOLLOW_STATUS.WITHDRAW.value() == toolConfigInfoEntity.getFollowStatus())
                {
                    toolConfigInfoEntity.setFollowStatus(toolConfigInfoEntity.getLastFollowStatus());
                    toolConfigInfoEntity.setParamJson(reqTool.getParamJson());
                    toolConfigInfoEntity.setUpdatedBy(userName);
                    toolConfigInfoEntity.setUpdatedDate(curTime);

                    // 重新启用的工具需要设置强制全量扫描标志
                    forceFullScanTools.add(toolName);
                    log.info("enable task {} tool {}", taskId, toolName);
                }
                // 修改工具
                else if (!specialParamUtil.isSameParam(toolName, toolConfigInfoEntity.getParamJson(), reqTool.getParamJson()))
                {
                    toolConfigInfoEntity.setParamJson(reqTool.getParamJson());
                    toolConfigInfoEntity.setUpdatedBy(userName);
                    toolConfigInfoEntity.setUpdatedDate(curTime);

                    // 修改了特殊参数的工具需要设置强制全量扫描标志
                    forceFullScanTools.add(toolName);
                    log.info("modify task {} tool {}", taskId, toolName);
                }
                else if (taskDetailVO.isOldAtomCodeChangeToNew())
                {
                    // 老插件切换为新插件需要设置强制全量扫描标志
                    forceFullScanTools.add(toolName);
                }
                toolConfigInfoEntityList.add(toolConfigInfoEntity);
            }

            // 把本次注册的工具列表从老的工具列表里面删除，剩下的就是要停用的工具列表
            oldToolMap.remove(toolName);
        }

        // 停用工具
        if (MapUtils.isNotEmpty(oldToolMap))
        {
            for (Map.Entry<String, ToolConfigInfoEntity> entry : oldToolMap.entrySet())
            {
                ToolConfigInfoEntity toolConfigInfoEntity = entry.getValue();
                int toolFollowStatus = toolConfigInfoEntity.getFollowStatus();
                if (ComConstants.FOLLOW_STATUS.WITHDRAW.value() != toolFollowStatus)
                {
                    toolConfigInfoEntity.setFollowStatus(ComConstants.FOLLOW_STATUS.WITHDRAW.value());
                    toolConfigInfoEntity.setLastFollowStatus(toolFollowStatus);
                    toolConfigInfoEntity.setUpdatedBy(userName);
                    toolConfigInfoEntity.setUpdatedDate(curTime);
                    log.info("disable task {} tool {}", taskId, toolConfigInfoEntity.getToolName());
                }

                toolConfigInfoEntityList.add(toolConfigInfoEntity);
            }
        }

        toolConfigInfoEntityList = toolRepository.saveAll(toolConfigInfoEntityList);
        taskInfoEntity.setToolConfigInfoList(toolConfigInfoEntityList);
        taskRepository.save(taskInfoEntity);
    }

    /**
     * 清理ToolConfig的脏数据
     * 注：目前仅清理task关联之外的多余数据，task自身的重复数据暂不作处理
     *
     * @param taskId
     * @param taskDBRefToolList
     */
    private void clearDirtyToolConfig(long taskId, List<ToolConfigInfoEntity> taskDBRefToolList) {
        List<ToolConfigInfoEntity> totalToolList = toolRepository.findByTaskId(taskId);
        if (CollectionUtils.isEmpty(totalToolList)) {
            return;
        }

        List<ToolConfigInfoEntity> toDelToolList;

        if (CollectionUtils.isEmpty(taskDBRefToolList)) {
            toDelToolList = new ArrayList<>(totalToolList);
        } else {
            if (totalToolList.size() == taskDBRefToolList.size()) {
                return;
            }

            // 并发导致小部分引用的toolConfig可能为null，需过滤处理
            Set<String> inUsingToolIdSet = taskDBRefToolList.stream()
                    .filter(Objects::nonNull)
                    .map(ToolConfigInfoEntity::getEntityId).collect(Collectors.toSet());

            toDelToolList = totalToolList.stream()
                    .filter(tool -> !inUsingToolIdSet.contains(tool.getEntityId())).collect(Collectors.toList());
        }

        if (CollectionUtils.isNotEmpty(toDelToolList)) {
            toolDao.removeByIds(
                    toDelToolList.stream().map(ToolConfigInfoEntity::getEntityId).collect(Collectors.toSet()));

            log.info("clear dirty tool config, task id: {}, total size: {}, del size: {}, del detail: {}",
                    taskId, totalToolList.size(), toDelToolList.size(), toDelToolList);
        }
    }
}
