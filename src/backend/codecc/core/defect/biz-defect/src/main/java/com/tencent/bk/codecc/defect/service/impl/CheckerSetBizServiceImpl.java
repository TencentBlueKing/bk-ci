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
import com.tencent.bk.codecc.defect.dao.mongorepository.*;
import com.tencent.bk.codecc.defect.dao.mongotemplate.CheckerSetDao;
import com.tencent.bk.codecc.defect.dao.mongotemplate.IgnoreCheckerDao;
import com.tencent.bk.codecc.defect.model.CheckerConfigEntity;
import com.tencent.bk.codecc.defect.model.CheckerPackageEntity;
import com.tencent.bk.codecc.defect.model.IgnoreCheckerEntity;
import com.tencent.bk.codecc.defect.model.checkerset.CheckerPropsEntity;
import com.tencent.bk.codecc.defect.model.checkerset.CheckerSetEntity;
import com.tencent.bk.codecc.defect.model.checkerset.CheckerSetTaskRelationshipEntity;
import com.tencent.bk.codecc.defect.service.CheckerService;
import com.tencent.bk.codecc.defect.service.ICheckerSetBizService;
import com.tencent.bk.codecc.defect.service.IV3CheckerSetBizService;
import com.tencent.bk.codecc.defect.service.ToolBuildInfoService;
import com.tencent.bk.codecc.defect.service.specialparam.SpecialParamUtil;
import com.tencent.bk.codecc.defect.vo.CheckerDetailVO;
import com.tencent.bk.codecc.defect.vo.CheckerProps;
import com.tencent.bk.codecc.defect.vo.checkerset.*;
import com.tencent.bk.codecc.task.api.ServiceBaseDataResource;
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.bk.codecc.task.api.ServiceToolRestResource;
import com.tencent.bk.codecc.task.vo.*;
import com.tencent.bk.codecc.task.vo.checkerset.ClearTaskCheckerSetReqVO;
import com.tencent.bk.codecc.task.vo.checkerset.ToolCheckerSetVO;
import com.tencent.bk.codecc.task.vo.checkerset.UpdateCheckerSet2TaskReqVO;
import com.tencent.devops.common.api.BaseDataVO;
import com.tencent.devops.common.api.ToolMetaBaseVO;
import com.tencent.devops.common.api.checkerset.CheckerSetVO;
import com.tencent.devops.common.api.checkerset.DividedCheckerSetsVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.auth.api.external.AuthTaskService;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.CheckerConstants;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.ToolIntegratedStatus;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.util.DateTimeUtils;
import com.tencent.devops.common.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import com.tencent.devops.common.util.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.Collator;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 规则集业务实现类
 *
 * @version V4.0
 * @date 2019/10/29
 */
@Slf4j
@Service
public class CheckerSetBizServiceImpl implements ICheckerSetBizService
{
    @Autowired
    private Client client;

    @Autowired
    private CheckerSetRepository checkerSetRepository;

    @Autowired
    private SpecialParamUtil specialParamUtil;

    @Autowired
    private CheckerService checkerService;

    @Autowired
    private IgnoreCheckerDao ignoreCheckerDao;

    @Autowired
    private CheckerSetDao checkerSetDao;

    @Autowired
    private AuthTaskService authTaskService;

    @Autowired
    private CheckerPackageRepository checkerPackageRepository;

    @Autowired
    private CheckerConfigRepository checkerConfigRepository;

    @Autowired
    private ToolBuildInfoService toolBuildInfoService;

    @Autowired
    private IV3CheckerSetBizService iv3CheckerSetBizService;

    @Autowired
    private CheckerSetTaskRelationshipRepository checkerSetTaskRelationshipRepository;

    /**
     * 查询规则集列表
     *
     * @param toolNames
     * @param user
     * @return
     */
    @Override
    public UserCheckerSetsVO getCheckerSets(List<String> toolNames, String user, String projectId)
    {
        UserCheckerSetsVO userCheckerSetsVO = new UserCheckerSetsVO();

        // 根据项目ID查询旧插件规则集，确认不会查出官方优选官方推荐
        List<CheckerSetEntity> filteredCheckerSetList = iv3CheckerSetBizService
                .findAvailableCheckerSetsByProject(projectId, Arrays.asList(true), ToolIntegratedStatus.P.value());
        if (CollectionUtils.isEmpty(filteredCheckerSetList))
        {
            return userCheckerSetsVO;
        }

        Map<String, DividedCheckerSetsVO> toolCheckerSetMap = new HashMap<>(toolNames.size());
        for (String toolName : toolNames)
        {
            DividedCheckerSetsVO dividedCheckerSets = new DividedCheckerSetsVO();
            dividedCheckerSets.setMyProjUse(Lists.newArrayList());
            dividedCheckerSets.setOthers(Lists.newArrayList());
            dividedCheckerSets.setRecommended(Lists.newArrayList());
            dividedCheckerSets.setToolName(toolName);
            toolCheckerSetMap.put(toolName, dividedCheckerSets);
        }

        //按使用量排序
        List<CheckerSetTaskRelationshipEntity> checkerSetTaskRelationshipEntityList = checkerSetTaskRelationshipRepository.findByProjectId(projectId);
        Map<String, Long> checkerSetCountMap = checkerSetTaskRelationshipEntityList.stream().
                collect(Collectors.groupingBy(CheckerSetTaskRelationshipEntity::getCheckerSetId, Collectors.counting()));
        filteredCheckerSetList.stream()
                .sorted(Comparator.comparingLong(o -> checkerSetCountMap.containsKey(o.getCheckerSetId()) ? -checkerSetCountMap.get(o.getCheckerSetId()) : 0L))
                .forEach(checkerSetEntity ->
                {
                    // 老插件的规则集只包含一个工具的规则，所以这里只需要取第一个规则的工具名即可
                    String toolName = checkerSetEntity.getCheckerProps().get(0).getToolName();
                    DividedCheckerSetsVO dividedCheckerSets = toolCheckerSetMap.get(toolName);
                    if (dividedCheckerSets != null)
                    {
                        CheckerSetVO checkerSetVO = new CheckerSetVO();
                        BeanUtils.copyProperties(checkerSetEntity, checkerSetVO);
                        Long useCount = checkerSetCountMap.get(checkerSetEntity.getCheckerSetId());
                        if (user.equals(checkerSetEntity.getCreator()) ||  useCount != null)
                        {
                            checkerSetVO.setTaskUsage(useCount != null ? Integer.valueOf(useCount.toString()) : 0);
                            dividedCheckerSets.getMyProjUse().add(checkerSetVO);
                        }
                        else if (checkerSetVO.getRecommended().equals(CheckerConstants.CheckerSetRecommended.RECOMMENDED.code()))
                        {
                            dividedCheckerSets.getRecommended().add(checkerSetVO);
                        }
                        else
                        {
                            dividedCheckerSets.getOthers().add(checkerSetVO);
                        }

                        // 官方的规则集，创建者展示为CodeCC
                        if (CheckerConstants.CheckerSetOfficial.OFFICIAL.code() == checkerSetEntity.getOfficial())
                        {
                            checkerSetVO.setCreator("CodeCC");
                        }
                    }
                });

        List<DividedCheckerSetsVO> toolCheckerSets = Lists.newArrayList();
        toolCheckerSetMap.forEach((toolName, dividedCheckerSetsVO) ->
        {
            toolCheckerSets.add(dividedCheckerSetsVO);

            // 推荐的规则集按权重排序
            Collections.sort(dividedCheckerSetsVO.getRecommended(), ((o1, o2) -> Integer.compare(o2.getSortWeight(), o1.getSortWeight())));
        });

        userCheckerSetsVO.setCheckerSets(toolCheckerSets);
        return userCheckerSetsVO;
    }

    /**
     * 查询规则集列表
     *
     * @param toolName
     * @param user
     * @param projectId
     * @return
     */
    @Override
    public PipelineCheckerSetVO getPipelineCheckerSets(String toolName, String user, String projectId)
    {
        PipelineCheckerSetVO pipelineCheckerSetVO = new PipelineCheckerSetVO(Lists.newArrayList());
        UserCheckerSetsVO userCheckerSetsVO = getCheckerSets(Lists.newArrayList(toolName), user, projectId);
        DividedCheckerSetsVO dividedCheckerSets = null;
        if (userCheckerSetsVO != null && CollectionUtils.isNotEmpty(userCheckerSetsVO.getCheckerSets()))
        {
            dividedCheckerSets = userCheckerSetsVO.getCheckerSets().get(0);
        }
        if (dividedCheckerSets != null)
        {
            if (CollectionUtils.isNotEmpty(dividedCheckerSets.getMyProjUse()))
            {
                pipelineCheckerSetVO.getRecords().add(getGroup("myProjUse", "我创建的/我的项目正在使用", dividedCheckerSets.getMyProjUse()));
            }
            if (CollectionUtils.isNotEmpty(dividedCheckerSets.getRecommended()))
            {
                pipelineCheckerSetVO.getRecords().add(getGroup("recommended", "CodeCC推荐", dividedCheckerSets.getRecommended()));
            }
            if (CollectionUtils.isNotEmpty(dividedCheckerSets.getOthers()))
            {
                pipelineCheckerSetVO.getRecords().add(getGroup("others", "更多公开规则集", dividedCheckerSets.getOthers()));
            }
        }
        return pipelineCheckerSetVO;
    }

    /**
     * 更新规则集
     *
     * @param taskId
     * @param toolName
     * @param checkerSetId
     * @param updateCheckerSetReqVO
     * @param user
     * @param projectId
     * @return
     */
    @Override
    public Boolean updateCheckerSet(Long taskId, String toolName, String checkerSetId, UpdateCheckerSetReqVO updateCheckerSetReqVO, String user,
                                    String projectId)
    {
        // 查找最新版本的规则集
        long currentTime = System.currentTimeMillis();
        CheckerSetEntity latestVersionCheckerSetEntity = getLatestVersionCheckerSet(toolName, checkerSetId);
        latestVersionCheckerSetEntity.setLastUpdateTime(currentTime);
        latestVersionCheckerSetEntity.setUpdatedBy(user);
        // 更新规则集名称和可见范围
        if (StringUtils.isNotEmpty(updateCheckerSetReqVO.getCheckerSetName()))
        {
            // 校验名称是否重复
            List<CheckerSetEntity> duplicateCheckerSets = checkerSetRepository.findByToolNameAndCheckerSetName(toolName,
                    updateCheckerSetReqVO.getCheckerSetName());
            if (CollectionUtils.isNotEmpty(duplicateCheckerSets))
            {
                for (CheckerSetEntity duplicateCheckerSet : duplicateCheckerSets)
                {
                    if (!checkerSetId.equals(duplicateCheckerSet.getCheckerSetId()))
                    {
                        log.error("规则集名称已存在");
                        throw new CodeCCException(CommonMessageCode.RECORD_EXIST, new String[]{"checkerSetName"}, null);
                    }
                }
            }

            latestVersionCheckerSetEntity.setCheckerSetName(updateCheckerSetReqVO.getCheckerSetName());
        }
        if (updateCheckerSetReqVO.getScope() != null)
        {
            latestVersionCheckerSetEntity.setScope(updateCheckerSetReqVO.getScope());
        }
        checkerSetDao.updateAllVersionCheckerSet(latestVersionCheckerSetEntity);

        // 保存新版本的规则集
        int newVersion = latestVersionCheckerSetEntity.getVersion() + 1;
        latestVersionCheckerSetEntity.setVersion(newVersion);
        latestVersionCheckerSetEntity.setTasksInUse(Lists.newArrayList(taskId));
        latestVersionCheckerSetEntity.setCreateTime(currentTime);
        ToolConfigInfoWithMetadataVO toolConfig = getToolConfigInfoWithMetadata(taskId, toolName);
        long codeLang = getCodeLang(toolConfig);
        getCheckerPropsAndCount(taskId, toolName, toolConfig, codeLang, latestVersionCheckerSetEntity);
        latestVersionCheckerSetEntity.setEntityId(null);
        checkerSetRepository.save(latestVersionCheckerSetEntity);

        // 加入规则集到任务
        addCheckerSet2Task(user, taskId, new AddCheckerSet2TaskReqVO(Lists.newArrayList(new ToolCheckerSetVO(toolName, checkerSetId, newVersion)),
                ComConstants.CommonJudge.COMMON_N.value(), true));

        // 更新项目内其他任务的规则集
        if (ComConstants.CommonJudge.COMMON_Y.value().equals(updateCheckerSetReqVO.getUpgradeMyOtherTasks()))
        {
            Result<TaskListVO> taskResult = client.get(ServiceTaskRestResource.class).getTaskList(projectId, user);
            if (taskResult.isNotOk() || null == taskResult.getData() || CollectionUtils.isEmpty(taskResult.getData().getEnableTasks()))
            {
                log.error("task list is empty! project id: {}, user: {}", projectId, user);
                throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
            }

            for (TaskDetailVO taskDetailVO : taskResult.getData().getEnableTasks())
            {
                if (taskDetailVO.getTaskId() == taskId || CollectionUtils.isEmpty(taskDetailVO.getToolConfigInfoList()))
                {
                    continue;
                }
                for (ToolConfigInfoVO toolConfigInfoVO : taskDetailVO.getToolConfigInfoList())
                {
                    if (toolConfigInfoVO.getCheckerSet() != null && checkerSetId.equals(toolConfigInfoVO.getCheckerSet().getCheckerSetId()))
                    {
                        addCheckerSet2Task(user, taskDetailVO.getTaskId(), new AddCheckerSet2TaskReqVO(Lists.newArrayList(
                                new ToolCheckerSetVO(toolConfigInfoVO.getToolName(), checkerSetId, newVersion)),
                                ComConstants.CommonJudge.COMMON_N.value(), true));
                    }
                }
            }
        }
        return true;
    }

    /**
     * 任务关联规则集
     *
     * @param user
     * @param taskId
     * @param addCheckerSet2TaskReqVO
     * @return
     */
    @Override
    public boolean addCheckerSet2Task(String user, Long taskId, AddCheckerSet2TaskReqVO addCheckerSet2TaskReqVO)
    {
        Result<TaskDetailVO> taskResult = client.get(ServiceTaskRestResource.class).getTaskInfoById(taskId);
        if (taskResult.isNotOk() || null == taskResult.getData())
        {
            log.error("task information is empty! task id: {}", taskId);
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }
        TaskDetailVO taskDetailVO = taskResult.getData();
        Map<String, ToolConfigInfoVO> toolConfigMap = Maps.newHashMap();
        if (CollectionUtils.isNotEmpty(taskDetailVO.getToolConfigInfoList()))
        {
            for (ToolConfigInfoVO toolConfigInfoVO : taskDetailVO.getToolConfigInfoList())
            {
                toolConfigMap.put(toolConfigInfoVO.getToolName(), toolConfigInfoVO);
            }
        }

        if (CollectionUtils.isNotEmpty(addCheckerSet2TaskReqVO.getToolCheckerSets()))
        {
            List<ToolCheckerSetVO> toolCheckerSets = Lists.newArrayList();
            List<String> updateTools = Lists.newArrayList();
            for (ToolCheckerSetVO toolCheckerSetVO : addCheckerSet2TaskReqVO.getToolCheckerSets())
            {
                // 查询工具当前支持的所有规则
                String toolName = toolCheckerSetVO.getToolName();
                ToolConfigInfoVO toolConfig = toolConfigMap.get(toolName);

                // 如果规则集没有改变，就不做处理
                if (toolConfig.getCheckerSet() != null)
                {
                    ToolCheckerSetVO currentCheckerSet = toolConfig.getCheckerSet();
                    if (currentCheckerSet.getCheckerSetId().equals(toolCheckerSetVO.getCheckerSetId())
                            && currentCheckerSet.getVersion().equals(toolCheckerSetVO.getVersion()))
                    {
                        continue;
                    }
                }

                // 查询所有默认规则和非默认规则
                List<CheckerDetailVO> allCheckers = checkerService.queryAllChecker(taskId, toolName, toolConfig.getParamJson(), taskDetailVO.getCodeLang());
                Map<String, CheckerDetailVO> allCheckerMap = Maps.newHashMap();
                Set<String> defaultCheckers = Sets.newHashSet();
                Set<String> nonDefaultCheckers = Sets.newHashSet();
                if (CollectionUtils.isNotEmpty(allCheckers))
                {
                    for (CheckerDetailVO checkerDetailVO : allCheckers)
                    {
                        allCheckerMap.put(checkerDetailVO.getCheckerKey(), checkerDetailVO);
                        if (ComConstants.CheckerPkgKind.DEFAULT.value().equals(checkerDetailVO.getPkgKind()))
                        {
                            defaultCheckers.add(checkerDetailVO.getCheckerKey());
                        }
                        else
                        {
                            nonDefaultCheckers.add(checkerDetailVO.getCheckerKey());
                        }
                    }
                }

                // 查询规则集中的规则
                Map<String, String> checkerSetCheckerPropsMap = Maps.newHashMap();
                CheckerSetEntity checkerSetEntity;
                if (toolCheckerSetVO.getVersion() == null)
                {
                    checkerSetEntity = getLatestVersionCheckerSet(toolName, toolCheckerSetVO.getCheckerSetId());
                    toolCheckerSetVO.setVersion(checkerSetEntity.getVersion());
                }
                else
                {
                    checkerSetEntity = checkerSetRepository.findFirstByToolNameAndCheckerSetIdAndVersion(toolName, toolCheckerSetVO.getCheckerSetId(),
                            toolCheckerSetVO.getVersion());
                }
                if (checkerSetEntity != null && CollectionUtils.isNotEmpty(checkerSetEntity.getCheckerProps()))
                {
                    for (CheckerPropsEntity checkerSetChecker : checkerSetEntity.getCheckerProps())
                    {
                        checkerSetCheckerPropsMap.put(checkerSetChecker.getCheckerKey(), checkerSetChecker.getProps());
                    }
                }

                // 设置关闭的默认规则和打开的非默认规则
                IgnoreCheckerEntity ignoreChecker = new IgnoreCheckerEntity();
                ignoreChecker.setTaskId(taskId);
                ignoreChecker.setToolName(toolName);
                List<String> closeDefaultCheckers = Lists.newArrayList(defaultCheckers);
                closeDefaultCheckers.removeAll(checkerSetCheckerPropsMap.keySet());
                List<String> openNonDefaultCheckers = Lists.newArrayList();
                for (String checker : checkerSetCheckerPropsMap.keySet())
                {
                    if (nonDefaultCheckers.contains(checker))
                    {
                        openNonDefaultCheckers.add(checker);
                    }
                }
                ignoreChecker.setCloseDefaultCheckers(closeDefaultCheckers);
                ignoreChecker.setOpenNonDefaultCheckers(openNonDefaultCheckers);
                ignoreCheckerDao.upsertIgnoreChecker(ignoreChecker);

                // 保存规则参数
                for (Map.Entry<String, String> entry : checkerSetCheckerPropsMap.entrySet())
                {
                    String checkerPropStr = entry.getValue();
                    if (StringUtils.isEmpty(checkerPropStr))
                    {
                        continue;
                    }
                    CheckerDetailVO checkerDetail = allCheckerMap.get(entry.getKey());
                    if (checkerDetail == null || checkerDetail.getEditable() == null || !checkerDetail.getEditable())
                    {
                        continue;
                    }
                    List<CheckerProps> checkerPropsList = JsonUtil.INSTANCE.to(checkerPropStr, new TypeReference<List<CheckerProps>>()
                    {
                    });
                    if (CollectionUtils.isNotEmpty(checkerPropsList))
                    {
                        CheckerProps checkerProps = checkerPropsList.get(0);
                        checkerService.updateCheckerConfigParam(taskId, toolName, checkerDetail.getCheckerKey(), checkerProps.getPropValue(), user);
                    }
                }

                // 如果之前关联了其他规则集，则需要从规则集已关联的任务列表中清除
                removeTaskInUse(toolConfig, taskId);

                // 记录需要更新规则集的工具和规则集信息
                toolCheckerSets.add(new ToolCheckerSetVO(toolName, toolCheckerSetVO.getCheckerSetId(), toolCheckerSetVO.getVersion()));
                updateTools.add(toolName);
            }

            if (CollectionUtils.isNotEmpty(updateTools))
            {
                // 更新流水线中的规则集
                if (addCheckerSet2TaskReqVO.getNeedUpdatePipeline() != null
                        && addCheckerSet2TaskReqVO.getNeedUpdatePipeline()
                        && CollectionUtils.isNotEmpty(toolCheckerSets))
                {
                    updatePipelineCheckerSet(taskId, user, toolCheckerSets);
                }

                // 为任务设置规则集Id和版本号
                client.get(ServiceToolRestResource.class).addCheckerSet2Task(taskId,
                        new UpdateCheckerSet2TaskReqVO(addCheckerSet2TaskReqVO.getToolCheckerSets()));

                // 设置强制全量扫描标志
                toolBuildInfoService.setForceFullScan(taskId, updateTools);

                // 升级用户
                if (ComConstants.CommonJudge.COMMON_Y.value().equals(addCheckerSet2TaskReqVO.getUpgradeCheckerSetOfUserTasks()))
                {
                    // 查询用户可见的任务列表
                    Set<Long> userTasks = Sets.newHashSet();
                    Result<TaskListVO> taskListResult = client.get(ServiceTaskRestResource.class).getTaskList(taskDetailVO.getProjectId(), user);
                    if (taskListResult.isNotOk() || null == taskListResult.getData() || CollectionUtils.isEmpty(taskListResult.getData().getEnableTasks()))
                    {
                        log.error("task list is empty! project id: {}, user: {}", taskDetailVO.getProjectId(), user);
                        throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
                    }
                    addCheckerSet2TaskReqVO.setUpgradeCheckerSetOfUserTasks(ComConstants.CommonJudge.COMMON_N.value());
                    for (TaskDetailVO userTaskDetailVO : taskListResult.getData().getEnableTasks())
                    {
                        userTasks.add(taskDetailVO.getTaskId());
                        if (userTaskDetailVO.getTaskId() != taskId)
                        {
                            addCheckerSet2Task(user, userTaskDetailVO.getTaskId(), addCheckerSet2TaskReqVO);
                        }
                    }
                }
            }
        }

        return true;
    }

    /**
     * 查询用户创建的规则集列表
     *
     * @param toolName
     * @param user
     * @param projectId
     * @return
     */
    @Override
    public UserCreatedCheckerSetsVO getUserCreatedCheckerSet(String toolName, String user, String projectId)
    {
        // 初始化返回体
        UserCreatedCheckerSetsVO userCreatedCheckerSetsVO = new UserCreatedCheckerSetsVO();
        userCreatedCheckerSetsVO.setUserCreatedCheckerSets(Lists.newArrayList());

        // 查询用户创建的所有规则集
        List<CheckerSetEntity> checkerSetEntities = checkerSetRepository.findByToolNameAndCreator(toolName, user);

        // 只保留最新版本的规则集
        Map<String, CheckerSetEntity> checkerSetLatestVersionMap = Maps.newHashMap();
        if (CollectionUtils.isNotEmpty(checkerSetEntities))
        {
            // 只保留最新版本
            for (CheckerSetEntity checkerSetEntity : checkerSetEntities)
            {
                if (!checkerSetLatestVersionMap.containsKey(checkerSetEntity.getCheckerSetId())
                        || checkerSetLatestVersionMap.get(checkerSetEntity.getCheckerSetId()).getVersion() < checkerSetEntity.getVersion())
                {
                    checkerSetLatestVersionMap.put(checkerSetEntity.getCheckerSetId(), checkerSetEntity);
                }
            }

            // 查询用户可见的项目列表使用规则集的数量
            Map<String, Integer> checkerSetUsageMap = Maps.newHashMap();
            Result<TaskListVO> taskResult = client.get(ServiceTaskRestResource.class).getTaskList(projectId, user);
            if (taskResult.isNotOk() || null == taskResult.getData() || CollectionUtils.isEmpty(taskResult.getData().getEnableTasks()))
            {
                log.error("task list is empty! project id: {}, user: {}", projectId, user);
                throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
            }
            for (TaskDetailVO taskDetailVO : taskResult.getData().getEnableTasks())
            {
                if (CollectionUtils.isEmpty(taskDetailVO.getToolConfigInfoList()))
                {
                    continue;
                }
                for (ToolConfigInfoVO toolConfigInfoVO : taskDetailVO.getToolConfigInfoList())
                {
                    if (!toolName.equals(toolConfigInfoVO.getToolName()) || toolConfigInfoVO.getCheckerSet() == null
                            || StringUtils.isEmpty(toolConfigInfoVO.getCheckerSet().getCheckerSetId()))
                    {
                        continue;
                    }
                    String checkerSetId = toolConfigInfoVO.getCheckerSet().getCheckerSetId();
                    if (checkerSetLatestVersionMap.get(checkerSetId) != null)
                    {
                        if (!checkerSetUsageMap.containsKey(checkerSetId))
                        {
                            checkerSetUsageMap.put(checkerSetId, 0);
                        }
                        checkerSetUsageMap.put(checkerSetId, checkerSetUsageMap.get(checkerSetId) + 1);
                    }
                }
            }

            // 组装响应体
            for (Map.Entry<String, CheckerSetEntity> entry : checkerSetLatestVersionMap.entrySet())
            {
                CheckerSetVO checkerSetVO = new CheckerSetVO();
                BeanUtils.copyProperties(entry.getValue(), checkerSetVO);
                int usage = checkerSetUsageMap.containsKey(entry.getKey()) ? checkerSetUsageMap.get(entry.getKey()) : 0;
                checkerSetVO.setTaskUsage(usage);
                userCreatedCheckerSetsVO.getUserCreatedCheckerSets().add(checkerSetVO);
            }
        }
        return userCreatedCheckerSetsVO;
    }

    /**
     * 清除规则集被使用的任务清单中的指定任务ID
     *
     * @param toolConfig
     * @param taskId
     */
    @Override
    public void removeTaskInUse(ToolConfigInfoVO toolConfig, long taskId)
    {
        if (toolConfig.getCheckerSet() != null)
        {
            String checkerSetId = toolConfig.getCheckerSet().getCheckerSetId();
            int version = toolConfig.getCheckerSet().getVersion();
            CheckerSetEntity checkerSetEntity = checkerSetRepository.findFirstByToolNameAndCheckerSetIdAndVersion(toolConfig.getToolName(), checkerSetId, version);
            checkerSetEntity.getTasksInUse().remove(taskId);
            checkerSetRepository.save(checkerSetEntity);
        }
    }

    /**
     * 查询规则集指定版本的差异
     *
     * @param user
     * @param projectId
     * @param toolName
     * @param checkerSetId
     * @param checkerSetDifferenceVO
     * @return
     */
    @Override
    public CheckerSetDifferenceVO getCheckerSetVersionDifference(String user, String projectId, String toolName, String checkerSetId,
                                                                 CheckerSetDifferenceVO checkerSetDifferenceVO)
    {
        // 查询规则集所有版本
        List<CheckerSetEntity> checkerSetEntities = checkerSetRepository.findByToolNameAndCheckerSetId(toolName, checkerSetId);

        if (CollectionUtils.isNotEmpty(checkerSetEntities))
        {
            // 查询用户可见的任务列表
            Set<Long> userTasks = Sets.newHashSet();
            Result<TaskListVO> taskResult = client.get(ServiceTaskRestResource.class).getTaskList(projectId, user);
            if (taskResult.isNotOk() || null == taskResult.getData() || CollectionUtils.isEmpty(taskResult.getData().getEnableTasks()))
            {
                log.error("task list is empty! project id: {}, user: {}", projectId, user);
                throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
            }
            for (TaskDetailVO taskDetailVO : taskResult.getData().getEnableTasks())
            {
                userTasks.add(taskDetailVO.getTaskId());
            }

            // 统计比较的两个版本的规则数量和规则集被用户项目使用数
            int taskUsage = 0;
            Map<String, String> checkerPropsFrom = Maps.newHashMap();
            Map<String, String> checkerPropsTo = Maps.newHashMap();
            for (CheckerSetEntity checkerSetEntity : checkerSetEntities)
            {
                if (checkerSetDifferenceVO.getFromVersion().equals(checkerSetEntity.getVersion()))
                {
                    checkerSetDifferenceVO.setCheckerCountFrom(checkerSetEntity.getCheckerCount());
                    if (CollectionUtils.isNotEmpty(checkerSetEntity.getCheckerProps()))
                    {
                        for (CheckerPropsEntity checkerPropsEntity : checkerSetEntity.getCheckerProps())
                        {
                            checkerPropsFrom.put(checkerPropsEntity.getCheckerKey(), checkerPropsEntity.getProps());
                        }
                    }
                }
                else if (checkerSetDifferenceVO.getToVersion().equals(checkerSetEntity.getVersion()))
                {
                    checkerSetDifferenceVO.setCheckerCountTo(checkerSetEntity.getCheckerCount());
                    if (CollectionUtils.isNotEmpty(checkerSetEntity.getCheckerProps()))
                    {
                        for (CheckerPropsEntity checkerPropsEntity : checkerSetEntity.getCheckerProps())
                        {
                            checkerPropsTo.put(checkerPropsEntity.getCheckerKey(), checkerPropsEntity.getProps());
                        }
                    }
                    checkerSetDifferenceVO.setLastUpdateTime(DateTimeUtils.getThirteenTimestamp(checkerSetEntity.getLastUpdateTime()));
                }
                if (CollectionUtils.isNotEmpty(userTasks) && CollectionUtils.isNotEmpty(checkerSetEntity.getTasksInUse()))
                {
                    for (long taskIdInUse : checkerSetEntity.getTasksInUse())
                    {
                        if (userTasks.contains(taskIdInUse))
                        {
                            taskUsage++;
                        }
                    }
                }
            }
            checkerSetDifferenceVO.setMyTaskCount(taskUsage);

            // 查询工具所有规则
            Map<String, CheckerDetailVO> toolAllCheckerMap = checkerService.queryAllChecker(toolName);
            List<CheckerPackageEntity> checkerPackageEntities = checkerPackageRepository.findByToolName(toolName);
            Map<String, String> checkerPkgIdNameMap = Maps.newHashMap();
            if (CollectionUtils.isNotEmpty(checkerPackageEntities) && MapUtils.isNotEmpty(toolAllCheckerMap))
            {
                for (CheckerPackageEntity checkerPackageEntity : checkerPackageEntities)
                {
                    checkerPkgIdNameMap.put(checkerPackageEntity.getPkgId(), checkerPackageEntity.getPkgName());
                }
                getOpenAndCloseCheckers(toolName, checkerSetDifferenceVO, checkerPropsFrom, checkerPropsTo);
            }
        }

        return checkerSetDifferenceVO;
    }

    /**
     * 清除任务关联的规则集
     *
     * @param taskId
     * @param toolNames
     */
    @Override
    public Boolean clearTaskCheckerSets(long taskId, List<String> toolNames, String user, boolean needUpdatePipeline)
    {
        if (CollectionUtils.isNotEmpty(toolNames))
        {
            Result<TaskDetailVO> taskResult = client.get(ServiceTaskRestResource.class).getTaskInfoById(taskId);
            if (taskResult.isNotOk() || null == taskResult.getData())
            {
                log.error("task information is empty! task id: {}", taskId);
                throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
            }
            TaskDetailVO taskDetailVO = taskResult.getData();
            clearTaskCheckerSets(taskDetailVO, toolNames, user, needUpdatePipeline);
        }
        return true;
    }

    /**
     * 清除任务关联的规则集
     *
     * @param taskDetail
     * @param toolNames
     * @param user
     */
    @Override
    public Boolean clearTaskCheckerSets(TaskDetailVO taskDetail, List<String> toolNames, String user, boolean needUpdatePipeline)
    {
        if (taskDetail != null && CollectionUtils.isNotEmpty(taskDetail.getToolConfigInfoList()) && CollectionUtils.isNotEmpty(toolNames))
        {
            long taskId = taskDetail.getTaskId();
            Map<String, ToolConfigInfoVO> toolConfigInfoVOMap = Maps.newHashMap();
            if (CollectionUtils.isNotEmpty(taskDetail.getToolConfigInfoList()))
            {
                for (ToolConfigInfoVO toolConfigInfoVO : taskDetail.getToolConfigInfoList())
                {
                    toolConfigInfoVOMap.put(toolConfigInfoVO.getToolName(), toolConfigInfoVO);
                }
            }
            List<String> needClearCheckerSetTools = Lists.newArrayList();
            for (String toolName : toolNames)
            {
                ToolConfigInfoVO toolConfig = toolConfigInfoVOMap.get(toolName);
                if (toolConfig != null && toolConfig.getCheckerSet() != null)
                {
                    needClearCheckerSetTools.add(toolName);

                    // 如果之前关联了其他规则集，则需要从规则集已关联的任务列表中清除
                    removeTaskInUse(toolConfig, taskId);
                }
            }

            if (CollectionUtils.isNotEmpty(needClearCheckerSetTools))
            {
                // 清除任务关联的规则集
                client.get(ServiceToolRestResource.class).clearCheckerSet(taskId, new ClearTaskCheckerSetReqVO(needClearCheckerSetTools, needUpdatePipeline));

                // 清除流水线插件中配置的规则集
                if (needUpdatePipeline && ComConstants.BsTaskCreateFrom.BS_PIPELINE.value().equals(taskDetail.getCreateFrom()) && StringUtils.isNotEmpty(user))
                {
                    List<ToolCheckerSetVO> toolCheckerSetVOS = Lists.newArrayList();
                    for (String toolName : needClearCheckerSetTools)
                    {
                        toolCheckerSetVOS.add(new ToolCheckerSetVO(toolName, "", 0));
                    }
                    client.get(ServiceTaskRestResource.class).updatePipelineTaskCheckerSets(user, taskDetail.getProjectId(), taskDetail.getPipelineId(),
                            taskId, new UpdateCheckerSet2TaskReqVO(toolCheckerSetVOS));
                }
            }
        }
        return true;
    }


    @Override
    public Boolean updateCheckerSetConfigParam(String checkerSetId, Integer version, String checkerName,
                                               String paramName, String displayName, String paramValue)
    {
        CheckerSetEntity checkerSetEntity = checkerSetRepository.findFirstByCheckerSetIdAndVersion(checkerSetId, version);
        if (CollectionUtils.isNotEmpty(checkerSetEntity.getCheckerProps()))
        {
            CheckerPropsEntity checkerProp = checkerSetEntity.getCheckerProps().stream().filter(checkerPropsEntity -> checkerPropsEntity.getCheckerKey().equalsIgnoreCase(checkerName)).
                    findFirst().orElseThrow(() -> new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{"规则名"}, null));
            if (StringUtils.isNotEmpty(checkerProp.getProps()))
            {
                String prop = checkerProp.getProps();
                List<CheckerProps> checkerProps = JsonUtil.INSTANCE.to(prop, new TypeReference<List<CheckerProps>>()
                {
                });
                CheckerProps selectedProp = checkerProps.stream().filter(checkerProperty ->
                        paramName.equalsIgnoreCase(checkerProperty.getPropName())
                ).findFirst().orElseGet(null);
                if (null != selectedProp)
                {
                    selectedProp.setPropValue(paramValue);
                }
                else
                {
                    checkerProps.add(new CheckerProps(paramName, displayName, paramValue));
                }
                checkerProp.setProps(JsonUtil.INSTANCE.toJson(checkerProps));
            }
            else
            {
                checkerProp.setProps(JsonUtil.INSTANCE.toJson(new ArrayList<CheckerProps>()
                {{
                    add(
                            new CheckerProps(paramName, displayName, paramValue));
                }}));
            }
            checkerSetEntity.setVersion(checkerSetEntity.getVersion() + 1);
            checkerSetEntity.setEntityId(null);
            checkerSetRepository.save(checkerSetEntity);
            return true;
        }
        else
        {
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{"规则集"}, null);
        }
    }

    /**
     * 查询工具配置和工具元数据
     *
     * @param taskId
     * @param toolName
     * @return
     */
    private ToolConfigInfoWithMetadataVO getToolConfigInfoWithMetadata(long taskId, String toolName)
    {
        Result<ToolConfigInfoWithMetadataVO> toolResult = client.get(ServiceToolRestResource.class).getToolWithMetadataByTaskIdAndName(taskId, toolName);
        if (toolResult.isNotOk() || null == toolResult.getData())
        {
            log.error("tool info is empty! task id: {}, tool name: {}", taskId, toolName);
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }
        ToolConfigInfoWithMetadataVO toolConfig = toolResult.getData();
        return toolConfig;
    }

    /**
     * 获取代码语言
     *
     * @param toolConfig
     * @return
     */
    private long getCodeLang(ToolConfigInfoWithMetadataVO toolConfig)
    {
        long codeLang;
        if (ComConstants.Tool.COVERITY.name().equals(toolConfig.getToolName()) || ComConstants.Tool.KLOCWORK.name().equals(toolConfig.getToolName()))
        {
            codeLang = toolConfig.getCodeLang();
        }
        else
        {
            // 查询工具属性
            ToolMetaBaseVO toolMetaDetailVO = toolConfig.getToolMetaBaseVO();
            codeLang = toolMetaDetailVO.getLang();
        }
        return codeLang;
    }

    /**
     * 获取打开的规则和规则个数
     *
     * @param taskId
     * @param toolName
     * @param toolConfig
     * @param codeLang
     * @param checkerSetEntity
     */
    private void getCheckerPropsAndCount(long taskId, String toolName, ToolConfigInfoWithMetadataVO toolConfig, long codeLang,
                                         CheckerSetEntity checkerSetEntity)
    {
        // 查询规则参数配置
        List<CheckerConfigEntity> configList = checkerConfigRepository.findByTaskIdAndToolName(taskId, toolName);
        Map<String, String> taskCheckerPropsMap = Maps.newHashMap();
        if (CollectionUtils.isNotEmpty(configList))
        {
            for (CheckerConfigEntity checkerConfigEntity : configList)
            {
                taskCheckerPropsMap.put(checkerConfigEntity.getCheckerKey(), checkerConfigEntity.getProps());
            }
        }

        // 查询打开的规则列表
        Map<String, CheckerDetailVO> openCheckers = checkerService.queryOpenCheckers(taskId, toolName, toolConfig.getParamJson(), codeLang);
        if (MapUtils.isNotEmpty(openCheckers))
        {
            checkerSetEntity.setCheckerCount(openCheckers.size());
            List<CheckerPropsEntity> checkerProps = Lists.newArrayList();
            for (Map.Entry<String, CheckerDetailVO> entry : openCheckers.entrySet())
            {
                CheckerPropsEntity checkerPropsEntity = new CheckerPropsEntity();
                checkerPropsEntity.setCheckerKey(entry.getKey());
                if (taskCheckerPropsMap.get(entry.getKey()) != null)
                {
                    checkerPropsEntity.setProps(taskCheckerPropsMap.get(entry.getKey()));
                }
                else
                {
                    checkerPropsEntity.setProps(entry.getValue().getProps());
                }
                checkerProps.add(checkerPropsEntity);
            }
            checkerSetEntity.setCheckerProps(checkerProps);
        }
    }

    private CheckerSetEntity getLatestVersionCheckerSet(String toolName, String checkerSetId)
    {
        List<CheckerSetEntity> checkerSetEntities = checkerSetRepository.findByToolNameAndCheckerSetId(toolName, checkerSetId);
        CheckerSetEntity latestVersionCheckerSetEntity = null;
        if (CollectionUtils.isNotEmpty(checkerSetEntities))
        {
            // 查找最新版本的规则集
            for (CheckerSetEntity checkerSetEntity : checkerSetEntities)
            {
                if (latestVersionCheckerSetEntity == null || checkerSetEntity.getVersion() > latestVersionCheckerSetEntity.getVersion())
                {
                    latestVersionCheckerSetEntity = checkerSetEntity;
                }
            }
        }
        return latestVersionCheckerSetEntity;
    }

    private void updatePipelineCheckerSet(long taskId, String user, List<ToolCheckerSetVO> toolCheckerSets)
    {
        if (ComConstants.BsTaskCreateFrom.BS_PIPELINE.value().equals(authTaskService.getTaskCreateFrom(taskId)))
        {
            // 查询任务详情
            Result<TaskDetailVO> taskDetailVOResult = client.get(ServiceTaskRestResource.class).getTaskInfoById(taskId);
            if (taskDetailVOResult.isNotOk() || null == taskDetailVOResult.getData())
            {
                log.error("task info is empty! task id: {}", taskId);
                throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
            }
            TaskDetailVO taskDetailVO = taskDetailVOResult.getData();

            // 更新流水线规则集
            Result<Boolean> updateResult = client.get(ServiceTaskRestResource.class).updatePipelineTaskCheckerSets(user, taskDetailVO.getProjectId(), taskDetailVO.getPipelineId(),
                    taskId, new UpdateCheckerSet2TaskReqVO(toolCheckerSets));
            if (updateResult.isNotOk() || false == updateResult.getData())
            {
                log.error("update pipeline checker set failed!! task id: {}", taskId);
                throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
            }
        }
    }

    private void getOpenAndCloseCheckers(String toolName, CheckerSetDifferenceVO checkerSetDifferenceVO, Map<String, String> checkerPropsFrom,
                                         Map<String, String> checkerPropsTo)
    {
        Map<String, CheckerDetailVO> toolAllCheckerMap = checkerService.queryAllChecker(toolName);
        List<CheckerPackageEntity> checkerPackageEntities = checkerPackageRepository.findByToolName(toolName);
        Map<String, String> checkerPkgIdNameMap = Maps.newHashMap();
        Map<String, Integer> checkerPkgSortMap = Maps.newHashMap();
        if (CollectionUtils.isNotEmpty(checkerPackageEntities))
        {
            for (CheckerPackageEntity checkerPackageEntity : checkerPackageEntities)
            {
                checkerPkgIdNameMap.put(checkerPackageEntity.getPkgId(), checkerPackageEntity.getPkgDesc());
                checkerPkgSortMap.put(checkerPackageEntity.getPkgDesc(), Integer.valueOf(checkerPackageEntity.getPkgId()));
            }
        }

        Map<String, List<String>> pkgModifiedCheckers = Maps.newTreeMap(((o1, o2) -> Integer.compare(checkerPkgSortMap.get(o1), checkerPkgSortMap.get(o2))));
        if (MapUtils.isNotEmpty(checkerPropsTo))
        {
            for (Map.Entry<String, String> entry : checkerPropsTo.entrySet())
            {
                String[] checkerInfo = getCheckerInfo(toolAllCheckerMap, entry.getKey(), checkerPkgIdNameMap);
                String checkerName = checkerInfo[0];
                String pkgName = checkerInfo[1];
                if (!checkerPropsFrom.containsKey(entry.getKey()))
                {
                    if (!pkgModifiedCheckers.containsKey(pkgName))
                    {
                        pkgModifiedCheckers.put(pkgName, Lists.newArrayList());
                    }
                    pkgModifiedCheckers.get(pkgName).add("+++" + checkerName);
                }
                else
                {
                    if (StringUtils.isNotEmpty(entry.getValue()))
                    {
                        List<CheckerProps> checkerProps = JsonUtil.INSTANCE.to(entry.getValue(), new TypeReference<List<CheckerProps>>()
                        {
                        });
                        if (CollectionUtils.isNotEmpty(checkerProps))
                        {
                            getModifiedChecker(checkerName, entry.getValue(), checkerPropsFrom.get(entry.getKey()), pkgName, pkgModifiedCheckers);
                        }
                    }
                }
            }

            for (Map.Entry<String, String> entry : checkerPropsFrom.entrySet())
            {
                String[] checkerInfo = getCheckerInfo(toolAllCheckerMap, entry.getKey(), checkerPkgIdNameMap);
                String checkerName = checkerInfo[0];
                String pkgName = checkerInfo[1];
                if (!checkerPropsTo.containsKey(entry.getKey()))
                {
                    if (!pkgModifiedCheckers.containsKey(pkgName))
                    {
                        pkgModifiedCheckers.put(pkgName, Lists.newArrayList());
                    }

                    pkgModifiedCheckers.get(pkgName).add("---" + checkerName);
                }
            }
        }
        if (MapUtils.isNotEmpty(pkgModifiedCheckers))
        {
            checkerSetDifferenceVO.setPackages(Lists.newArrayList());
            List<String> modifiedCheckerPrefix = Lists.newArrayList("+++", "---", "•••");
            for (Map.Entry<String, List<String>> entry : pkgModifiedCheckers.entrySet())
            {
                if (CollectionUtils.isNotEmpty(entry.getValue()))
                {
                    // 先根据字母排序
                    Collections.sort(entry.getValue(), ((o1, o2) -> Collator.getInstance(Locale.ENGLISH).compare(o1.substring(3), o2.substring(3))));

                    // 再根据增、减、改排序
                    Collections.sort(entry.getValue(), ((o1, o2) -> Integer.compare(modifiedCheckerPrefix.indexOf(o1.substring(0, 3)),
                            modifiedCheckerPrefix.indexOf(o2.substring(0, 3)))));
                }
                CheckerPkgDifferenceVO checkerPkgDifferenceVO = new CheckerPkgDifferenceVO(entry.getKey(), entry.getValue());
                checkerSetDifferenceVO.getPackages().add(checkerPkgDifferenceVO);
            }
        }
    }

    private void getModifiedChecker(String checkerName, String latestProps, String currentProps, String pkgName, Map<String, List<String>> pkgModifiedCheckers)
    {
        List<CheckerProps> latestPropsList = JsonUtil.INSTANCE.to(latestProps, new TypeReference<List<CheckerProps>>()
        {
        });
        List<CheckerProps> currentPropsList = JsonUtil.INSTANCE.to(currentProps, new TypeReference<List<CheckerProps>>()
        {
        });
        Map<String, CheckerProps> currentPropsMap = Maps.newHashMap();
        if (CollectionUtils.isNotEmpty(currentPropsList))
        {
            for (CheckerProps currentCheckerProps : currentPropsList)
            {
                currentPropsMap.put(currentCheckerProps.getPropName(), currentCheckerProps);
            }
        }
        if (CollectionUtils.isNotEmpty(latestPropsList))
        {
            for (CheckerProps latestCheckerProps : latestPropsList)
            {
                String propName = latestCheckerProps.getPropName();
                CheckerProps currentCheckerProps = currentPropsMap.get(latestCheckerProps.getPropName());
                if (currentCheckerProps != null && !latestCheckerProps.getPropValue().equals(currentCheckerProps.getPropValue()))
                {
                    if (!pkgModifiedCheckers.containsKey(pkgName))
                    {
                        pkgModifiedCheckers.put(pkgName, Lists.newArrayList());
                    }
                    pkgModifiedCheckers.get(pkgName).add("•••" + checkerName + "(" + propName + currentCheckerProps.getPropValue() + ")"
                            + " >>> " + checkerName + "(" + propName + latestCheckerProps.getPropValue() + ")");
                }
            }
        }
    }

    private String[] getCheckerInfo(Map<String, CheckerDetailVO> toolAllCheckerMap, String checkerKey, Map<String, String> checkerPkgIdNameMap)
    {
        String checkerName;
        String pkgName;
        CheckerDetailVO checkerPOModel = toolAllCheckerMap.get(checkerKey);
        if (checkerPOModel.getNativeChecker())
        {
            checkerName = checkerPOModel.getCheckerName();
            pkgName = checkerPkgIdNameMap.get(checkerPOModel.getPkgKind());
        }
        else
        {
            checkerName = checkerKey;
            pkgName = "规则市场包";
        }
        return new String[]{checkerName, pkgName};
    }

    private PipelineCheckerSetRecordVO getGroup(String id, String name, List<CheckerSetVO> checkerSetVOS)
    {
        // 查询语言参数列表
        Result<List<BaseDataVO>> paramsResult = client.get(ServiceBaseDataResource.class).getParamsByType(ComConstants.KEY_CODE_LANG);
        if (paramsResult.isNotOk() || CollectionUtils.isEmpty(paramsResult.getData()))
        {
            log.error("param list is empty! param type: {}", ComConstants.KEY_CODE_LANG);
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }
        List<BaseDataVO> codeLangParams = paramsResult.getData();

        PipelineCheckerSetRecordVO pipelineCheckerSetRecordVO = new PipelineCheckerSetRecordVO();
        pipelineCheckerSetRecordVO.setId(id);
        pipelineCheckerSetRecordVO.setName(name);
        pipelineCheckerSetRecordVO.setChildren(Lists.newArrayList());

        if (CollectionUtils.isNotEmpty(checkerSetVOS))
        {
            for (CheckerSetVO checkerSetVO : checkerSetVOS)
            {
                PipelineCheckerSetRecordChildVO pipelineCheckerSetRecordChildVO = new PipelineCheckerSetRecordChildVO();
                pipelineCheckerSetRecordChildVO.setRuleSetId(checkerSetVO.getCheckerSetId());
                pipelineCheckerSetRecordChildVO.setRuleSetName(checkerSetVO.getCheckerSetName());
                specialParamUtil.setParam2Object(checkerSetVO.getToolName(), checkerSetVO.getParamJson(), pipelineCheckerSetRecordChildVO, PipelineCheckerSetRecordChildVO.class);
                pipelineCheckerSetRecordChildVO.setCodeLangs(getCodelangs(checkerSetVO.getCodeLang(), codeLangParams));
                pipelineCheckerSetRecordVO.getChildren().add(pipelineCheckerSetRecordChildVO);
            }

        }
        return pipelineCheckerSetRecordVO;
    }

    private List<String> getCodelangs(Long codeLang, List<BaseDataVO> codeLangParams)
    {
        List<String> codeLangs = Lists.newArrayList();
        for (BaseDataVO codeLangParam : codeLangParams)
        {
            int paramCodeInt = Integer.valueOf(codeLangParam.getParamCode());
            if (null != codeLang && (codeLang & paramCodeInt) != 0)
            {
                // 蓝盾流水线使用的是语言别名的第一个值作为语言的ID来匹配的
                codeLangs.add(new JSONArray(codeLangParam.getParamExtend2()).getString(0));
            }
        }
        return codeLangs;
    }
}
