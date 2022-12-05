/*
 * Tencent is pleased to support the open source community by making BlueKing available.
 * Copyright (C) 2017-2018 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.bk.codecc.defect.service.openapi;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.dao.mongotemplate.CCNDefectDao;
import com.tencent.bk.codecc.defect.dao.mongotemplate.DUPCDefectDao;
import com.tencent.bk.codecc.defect.dao.mongotemplate.DefectDao;
import com.tencent.bk.codecc.defect.dao.mongotemplate.LintDefectDao;
import com.tencent.bk.codecc.defect.model.*;
import com.tencent.bk.codecc.defect.service.newdefectjudge.NewDefectJudgeService;
import com.tencent.bk.codecc.defect.utils.ConvertUtil;
import com.tencent.bk.codecc.defect.vo.admin.DeptTaskDefectReqVO;
import com.tencent.bk.codecc.defect.vo.openapi.CustomTaskOverviewVO;
import com.tencent.bk.codecc.defect.vo.openapi.TaskOverviewDetailRspVO;
import com.tencent.bk.codecc.defect.vo.openapi.TaskOverviewDetailVO;
import com.tencent.bk.codecc.defect.vo.report.CommonChartAuthorVO;
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.bk.codecc.task.api.ServiceToolRestResource;
import com.tencent.bk.codecc.task.api.UserMetaRestResource;
import com.tencent.bk.codecc.task.vo.CustomProjVO;
import com.tencent.bk.codecc.task.vo.MetadataVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.bk.codecc.task.vo.ToolConfigInfoVO;
import com.tencent.devops.common.api.QueryTaskListReqVO;
import com.tencent.devops.common.api.ToolMetaBaseVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.Page;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.Tool;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.constant.RedisKeyConstants;
import com.tencent.devops.common.service.ToolMetaCacheService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import com.tencent.devops.common.util.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * API业务服务层
 *
 * @version V1.0
 * @date 2020/3/17
 */

@Slf4j
@Service
public class ApiBizServiceImpl implements ApiBizService {
    @Autowired
    protected Client client;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private LintDefectDao lintDefectDao;

    @Autowired
    private DefectDao defectDao;

    @Autowired
    private CCNDefectDao ccnDefectDao;

    @Autowired
    protected DUPCDefectDao dupcDefectDao;

    @Autowired
    private ToolMetaCacheService toolMetaCache;

    @Autowired
    private NewDefectJudgeService newDefectJudgeService;


    @Override
    public TaskOverviewDetailRspVO statisticsTaskOverview(DeptTaskDefectReqVO reqVO, Integer pageNum, Integer pageSize,
            Sort.Direction sortType) {
        log.info("statisticsTaskOverview req content: {}", reqVO);
        TaskOverviewDetailRspVO rspVO = new TaskOverviewDetailRspVO();
        List<TaskOverviewDetailVO> taskOverviewList = Lists.newArrayList();

        QueryTaskListReqVO queryTaskListReqVO = new QueryTaskListReqVO();
        BeanUtils.copyProperties(reqVO, queryTaskListReqVO);
        // 如果没传默认不查工蜂扫描
        if (CollectionUtils.isEmpty(queryTaskListReqVO.getCreateFrom())) {
            queryTaskListReqVO.setCreateFrom(Lists.newArrayList(ComConstants.BsTaskCreateFrom.BS_CODECC.value(),
                    ComConstants.BsTaskCreateFrom.BS_PIPELINE.value()));
        }
        if (queryTaskListReqVO.getStatus() == null) {
            queryTaskListReqVO.setStatus(ComConstants.Status.ENABLE.value());
        }

        List<TaskDetailVO> taskDetailVoList = batchGetTaskDetailVoList(queryTaskListReqVO);

        int totalPageNum = 0;
        int total = 0;
        pageNum = pageNum == null || pageNum - 1 < 0 ? 0 : pageNum - 1;
        pageSize = pageSize == null ? 10 : pageSize >= 500 ? 500 : pageSize;
        if (CollectionUtils.isNotEmpty(taskDetailVoList)) {
            // 排序
            if (Sort.Direction.DESC.equals(sortType)) {
                taskDetailVoList.sort((obj1, obj2) -> Long.compare(obj2.getTaskId(), obj1.getTaskId()));
            }
            else {
                taskDetailVoList.sort(Comparator.comparingLong(TaskDetailVO::getTaskId));
            }

            total = taskDetailVoList.size();
            if (total > 0) {
                totalPageNum = (total + pageSize - 1) / pageSize;
            }
            int subListBeginIdx = pageNum * pageSize;
            int subListEndIdx = subListBeginIdx + pageSize;
            if (subListBeginIdx > total) {
                subListBeginIdx = 0;
            }
            taskDetailVoList = taskDetailVoList.subList(subListBeginIdx, Math.min(subListEndIdx, total));

            List<Long> taskIds = taskDetailVoList.stream().map(TaskDetailVO::getTaskId).collect(Collectors.toList());
            queryTaskListReqVO.setTaskIds(taskIds);
            List<ToolConfigInfoVO> toolConfigInfoVoList = batchGetToolConfigInfoVoList(queryTaskListReqVO);
            Map<Long, List<ToolConfigInfoVO>> taskToolConfigMap =
                    toolConfigInfoVoList.stream().collect(Collectors.groupingBy(ToolConfigInfoVO::getTaskId));

            // 获取代码语言元数据
            List<MetadataVO> metadataVoList = getCodeLangMetadataVoList();

            Map<String, String> deptInfo =
                    (Map<String, String>) redisTemplate.opsForHash().entries(RedisKeyConstants.KEY_DEPT_INFOS);

            Map<Long, List<String>> afterFilterIdMap = Maps.newHashMap();
            for (TaskDetailVO taskDetailVO : taskDetailVoList) {
                TaskOverviewDetailVO taskOverviewVO = new TaskOverviewDetailVO();
                BeanUtils.copyProperties(taskDetailVO, taskOverviewVO);
                taskOverviewVO.setBgName(deptInfo.get(String.valueOf(taskDetailVO.getBgId())));
                taskOverviewVO.setDeptName(deptInfo.get(String.valueOf(taskDetailVO.getDeptId())));
                taskOverviewVO.setCenterName(deptInfo.get(String.valueOf(taskDetailVO.getCenterId())));
                taskOverviewVO.setCodeLang(ConvertUtil.convertCodeLang(taskDetailVO.getCodeLang(), metadataVoList));
                taskOverviewList.add(taskOverviewVO);

                long taskId = taskDetailVO.getTaskId();
                List<ToolConfigInfoVO> toolInfoList = taskToolConfigMap.get(taskId);
                if (CollectionUtils.isEmpty(toolInfoList)) {
                    log.info("noToolsRegistered task {}", taskId);
                    continue;
                }

                for (ToolConfigInfoVO toolConfigInfoVO : toolInfoList) {
                    if (ComConstants.FOLLOW_STATUS.WITHDRAW.value() == toolConfigInfoVO.getFollowStatus()) {
                        continue;
                    }

                    List<String> tools = afterFilterIdMap.computeIfAbsent(taskId, k -> Lists.newArrayList());
                    tools.add(toolConfigInfoVO.getToolName());
                }
            }
            // 统计各工具
            Map<Long, List<TaskOverviewDetailVO.ToolDefectVO>> allTaskToolStat = batchQueryToolResult(afterFilterIdMap);

            // 赋值项目各工具告警对象列表
            assignTaskToolDefectInfo(taskOverviewList, allTaskToolStat);
        }

        Page<TaskOverviewDetailVO> overviewDetailVoPage =
                new Page<>(total, pageNum + 1, pageSize, totalPageNum, taskOverviewList);
        rspVO.setStatisticsTask(overviewDetailVoPage);

        return rspVO;
    }

    /**
     * 获取任务详情列表
     *
     * @param queryTaskListReqVO reqVoO
     * @return list
     */
    private List<TaskDetailVO> batchGetTaskDetailVoList(QueryTaskListReqVO queryTaskListReqVO) {
        Result<List<TaskDetailVO>> batchGetTaskList =
                client.get(ServiceTaskRestResource.class).batchGetTaskList(queryTaskListReqVO);
        if (batchGetTaskList.isNotOk() || batchGetTaskList.getData() == null) {
            log.error("queryTaskListReqVO taskList isEmpty! req content: {}", queryTaskListReqVO);
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }
        return batchGetTaskList.getData();
    }

    @Override
    public TaskOverviewDetailRspVO statCustomTaskOverview(String customProjSource, Integer pageNum, Integer pageSize,
            Sort.Direction sortType) {
        TaskOverviewDetailRspVO rspVO = new TaskOverviewDetailRspVO();
        List<CustomTaskOverviewVO> customTaskOverviewVoList = Lists.newArrayList();

        long count = 0;
        int totalPages = 0;

        // 1.分页获取个性化任务列表
        QueryTaskListReqVO reqVO = new QueryTaskListReqVO();
        reqVO.setCustomTaskSource(customProjSource);
        reqVO.setPageNum(pageNum);
        reqVO.setPageSize(pageSize);
        reqVO.setSortField("task_id");
        reqVO.setSortType(sortType != null ? sortType.name() : Sort.Direction.ASC.name());
        Result<Page<CustomProjVO>> pageResult = client.get(ServiceTaskRestResource.class).batchGetCustomTaskList(reqVO);
        if (pageResult.isNotOk()) {
            log.error("batchGetCustomTaskList req is fail!");
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }

        Page<CustomProjVO> customProjVoPage = pageResult.getData();
        if (customProjVoPage != null) {
            count = customProjVoPage.getCount();
            totalPages = customProjVoPage.getTotalPages();
            pageNum = customProjVoPage.getPage();
            pageSize = customProjVoPage.getPageSize();

            List<CustomProjVO> customProjVoList = customProjVoPage.getRecords();
            if (CollectionUtils.isNotEmpty(customProjVoList)) {
                // 根据个性化任务ID集合获取任务信息
                Map<Long, String> taskUrlMap = customProjVoList.stream()
                        .collect(Collectors.toMap(CustomProjVO::getTaskId, CustomProjVO::getUrl));
                reqVO.setTaskIds(Lists.newArrayList(taskUrlMap.keySet()));

                // 获取任务详情
                List<TaskDetailVO> taskDetailVoList = batchGetTaskDetailVoList(reqVO);

                // 获取工具配置列表
                List<ToolConfigInfoVO> toolConfigInfoVoList = batchGetToolConfigInfoVoList(reqVO);
                Map<Long, List<ToolConfigInfoVO>> taskToolConfigMap =
                        toolConfigInfoVoList.stream().collect(Collectors.groupingBy(ToolConfigInfoVO::getTaskId));

                // 组织架构
                Map<String, String> deptInfo =
                        (Map<String, String>) redisTemplate.opsForHash().entries(RedisKeyConstants.KEY_DEPT_INFOS);

                // 获取代码语言元数据
                List<MetadataVO> metadataVoList = getCodeLangMetadataVoList();

                Map<Long, List<String>> afterFilterIdMap = Maps.newHashMap();
                for (TaskDetailVO taskDetailVO : taskDetailVoList) {
                    CustomTaskOverviewVO taskOverviewVO = new CustomTaskOverviewVO();
                    BeanUtils.copyProperties(taskDetailVO, taskOverviewVO);
                    taskOverviewVO.setBgName(deptInfo.get(String.valueOf(taskDetailVO.getBgId())));
                    taskOverviewVO.setDeptName(deptInfo.get(String.valueOf(taskDetailVO.getDeptId())));
                    taskOverviewVO.setCenterName(deptInfo.get(String.valueOf(taskDetailVO.getCenterId())));
                    taskOverviewVO.setCodeLang(ConvertUtil.convertCodeLang(taskDetailVO.getCodeLang(), metadataVoList));

                    long taskId = taskDetailVO.getTaskId();
                    taskOverviewVO.setUrl(taskUrlMap.get(taskId));
                    customTaskOverviewVoList.add(taskOverviewVO);

                    List<ToolConfigInfoVO> toolInfoList = taskToolConfigMap.get(taskId);
                    if (CollectionUtils.isEmpty(toolInfoList)) {
                        log.info("noToolsRegistered task {}", taskId);
                        continue;
                    }
                    // 统计任务都注册了哪些工具
                    for (ToolConfigInfoVO toolConfigInfoVO : toolInfoList) {
                        if (ComConstants.FOLLOW_STATUS.WITHDRAW.value() == toolConfigInfoVO.getFollowStatus()) {
                            continue;
                        }

                        List<String> tools = afterFilterIdMap.computeIfAbsent(taskId, k -> Lists.newArrayList());
                        tools.add(toolConfigInfoVO.getToolName());
                    }
                }
                // 统计各工具告警概览
                Map<Long, List<CustomTaskOverviewVO.ToolDefectVO>> allTaskToolStat =
                        batchQueryToolResult(afterFilterIdMap);

                // 赋值项目各工具告警对象列表
                assignCustomTaskToolDefectInfo(customTaskOverviewVoList, allTaskToolStat);
            }
        }
        else {
            pageNum = pageNum == null || pageNum - 1 < 0 ? 0 : pageNum;
            pageSize = pageSize == null ? 10 : pageSize;
        }

        Page<CustomTaskOverviewVO> overviewVoPage =
                new Page<>(count, pageNum, pageSize, totalPages, customTaskOverviewVoList);
        rspVO.setStatCustomTask(overviewVoPage);
        return rspVO;
    }

    /**
     * 赋值给各任务接入的工具告警列表
     *
     * @param taskOverviewList  任务列表
     * @param allToolsDefectMap 每个任务对应的工具告警对象列表
     */
    private void assignCustomTaskToolDefectInfo(List<CustomTaskOverviewVO> taskOverviewList,
            Map<Long, List<CustomTaskOverviewVO.ToolDefectVO>> allToolsDefectMap) {
        if (CollectionUtils.isEmpty(taskOverviewList) || MapUtils.isEmpty(allToolsDefectMap)) {
            log.error("no data for assignTaskToolDefectInfo!");
            return;
        }

        for (CustomTaskOverviewVO detailVO : taskOverviewList) {
            List<CustomTaskOverviewVO.ToolDefectVO> toolDefectVoList = allToolsDefectMap.get(detailVO.getTaskId());

            if (CollectionUtils.isEmpty(toolDefectVoList)) {
                toolDefectVoList = Lists.newArrayList();
            }

            detailVO.setToolDefectInfo(toolDefectVoList);
        }
    }


    /**
     * 按任务ID批量获取工具配置信息
     *
     * @param queryTaskListReqVO taskIds
     * @return list
     */
    private List<ToolConfigInfoVO> batchGetToolConfigInfoVoList(QueryTaskListReqVO queryTaskListReqVO) {
        Result<List<ToolConfigInfoVO>> toolConfigListRes =
                client.get(ServiceToolRestResource.class).batchGetToolConfigList(queryTaskListReqVO);
        if (toolConfigListRes.isNotOk() || CollectionUtils.isEmpty(toolConfigListRes.getData())) {
            log.error("toolConfigInfo result is empty! {}", queryTaskListReqVO);
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }
        return toolConfigListRes.getData();
    }


    /**
     * 获取代码语言
     *
     * @return metaVO
     */
    private List<MetadataVO> getCodeLangMetadataVoList() {
        Result<Map<String, List<MetadataVO>>> metaDataResult =
                client.get(UserMetaRestResource.class).metadatas(ComConstants.KEY_CODE_LANG);
        if (metaDataResult.isNotOk() || metaDataResult.getData() == null) {
            log.error("meta data result is empty! meta data type {}", ComConstants.KEY_CODE_LANG);
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }
        return metaDataResult.getData().get(ComConstants.KEY_CODE_LANG);
    }

    /**
     * 按工具维度批量查询分析结果
     *
     * @param afterFilterIdMap 项目ID, 工具列表
     * @return 项目ID 工具告警对象列表
     */
    private Map<Long, List<TaskOverviewDetailVO.ToolDefectVO>> batchQueryToolResult(
            Map<Long, List<String>> afterFilterIdMap) {
        Map<Long, List<TaskOverviewDetailVO.ToolDefectVO>> allToolsDefectMap = Maps.newHashMap();
        if (MapUtils.isEmpty(afterFilterIdMap)) {
            return allToolsDefectMap;
        }

        // 按工具维度归类(哪些任务注册了这个工具)
        Map<String, Set<Long>> toolTaskIdMap = Maps.newHashMap();
        for (Map.Entry<Long, List<String>> entry : afterFilterIdMap.entrySet()) {
            Long taskId = entry.getKey();
            List<String> toolList = entry.getValue();

            if (CollectionUtils.isEmpty(toolList)) {
                log.info("batchQueryToolResult task unregistered tool: {}", taskId);
                continue;
            }
            for (String tool : toolList) {
                Set<Long> taskIdSet = toolTaskIdMap.computeIfAbsent(tool, k -> Sets.newHashSet());
                if (Tool.CLOC.name().equals(tool)) {
                    continue;
                }
                taskIdSet.add(taskId);
            }
        }

        // 按工具分别批量查询
        for (Map.Entry<String, Set<Long>> entry : toolTaskIdMap.entrySet()) {
            String tool = entry.getKey();
            Set<Long> taskIdList = entry.getValue();

            /* 修复ConcurrentMap不允许Null的问题 */
            if (StringUtils.isBlank(tool) || Tool.SPOTBUGS.name().equalsIgnoreCase(tool)) {
                log.error("batchQueryToolResult tool is blank: {}", tool);
                continue;
            }
            ToolMetaBaseVO toolMetaBaseVO = toolMetaCache.getToolBaseMetaCache(tool);
            String toolDisplayName = toolMetaBaseVO.getDisplayName();
            String pattern = toolMetaBaseVO.getPattern();

            if (CollectionUtils.isEmpty(taskIdList)) {
                log.info("taskIdList isEmpty tool: {}", tool);
                continue;
            }

            if (ComConstants.ToolPattern.LINT.name().equals(pattern)) {
//                batchGetLintAnalyzeStat(tool, taskIdList, allToolsDefectMap, toolDisplayName);
                batchGetLintDefectStat(tool, taskIdList, allToolsDefectMap, toolDisplayName);
            }
            else if (ComConstants.ToolPattern.CCN.name().equals(pattern)) {
                batchGetCcnAnalyzeStat(tool, taskIdList, allToolsDefectMap, toolDisplayName);
            }
            else if (ComConstants.ToolPattern.DUPC.name().equals(pattern)) {
                batchGetDupcAnalyzeStat(tool, taskIdList, allToolsDefectMap, toolDisplayName);
            }
            else {
                batchGetCommonDefectStat(tool, taskIdList, allToolsDefectMap, toolDisplayName);
            }
        }

        return allToolsDefectMap;
    }


    /**
     * 批量获取Lint工具分析结果
     *
     * @param toolName          工具名称
     * @param taskIdList        项目ID列表
     * @param allToolsDefectMap 保存项目的所有工具告警列表
     * @param toolDisplayName   工具展示名称
     */
    private void batchGetLintAnalyzeStat(String toolName, List<Long> taskIdList,
            Map<Long, List<TaskOverviewDetailVO.ToolDefectVO>> allToolsDefectMap, String toolDisplayName) {
        List<LintStatisticEntity> lintStatEntities = lintDefectDao.findStatByTaskIdInAndToolIs(taskIdList, toolName);
        if (CollectionUtils.isEmpty(lintStatEntities)) {
            log.info("lintStatEntityList isEmpty tool: {}", toolName);
            return;
        }

        for (LintStatisticEntity statEntity : lintStatEntities) {
            TaskOverviewDetailVO.ToolDefectVO toolDefectVO = new TaskOverviewDetailVO.ToolDefectVO();
            toolDefectVO.setToolName(toolName);
            toolDefectVO.setDisplayName(toolDisplayName);
            toolDefectVO.setExist(statEntity.getDefectCount());

            List<TaskOverviewDetailVO.ToolDefectVO> toolDefectVoList =
                    allToolsDefectMap.computeIfAbsent(statEntity.getTaskId(), k -> Lists.newArrayList());
            toolDefectVoList.add(toolDefectVO);
        }

    }

    /**
     * 批量获取Lint工具告警并按严重程度统计新老告警
     *
     * @param toolName          工具名称
     * @param taskIdList        项目ID列表
     * @param allToolsDefectMap 保存项目的所有工具告警列表
     * @param toolDisplayName   工具展示名称
     */
    private void batchGetLintDefectStat(String toolName, Set<Long> taskIdList,
            Map<Long, List<TaskOverviewDetailVO.ToolDefectVO>> allToolsDefectMap, String toolDisplayName) {
        if (CollectionUtils.isEmpty(taskIdList)) {
            log.info("taskIdList isEmpty tool: {}", toolName);
            return;
        }

        List<LintFileEntity> lintDefectEntities = lintDefectDao.findByTaskIdInAndToolIs(taskIdList, toolName);
        if (CollectionUtils.isEmpty(lintDefectEntities)) {
            log.info("lintDefectEntities isEmpty tool: {}", toolName);
        }

        Map<Long, List<LintFileEntity>> defectMap =
                lintDefectEntities.stream().collect(Collectors.groupingBy(LintFileEntity::getTaskId));

        // 批量获取任务新老告警的分界时间
        Map<Long, Long> newDefectJudgeTimeMap = newDefectJudgeService.batchGetNewDefectJudgeTime(taskIdList, toolName);

        for (Long taskId : taskIdList) {
            TaskOverviewDetailVO.ToolDefectVO toolDefectVO = new TaskOverviewDetailVO.ToolDefectVO();
            toolDefectVO.setToolName(toolName);
            toolDefectVO.setDisplayName(toolDisplayName);

            CommonChartAuthorVO newDefect = new CommonChartAuthorVO();
            CommonChartAuthorVO historyDefect = new CommonChartAuthorVO();

            List<LintFileEntity> lintFileEntities = defectMap.get(taskId);
            if (CollectionUtils.isEmpty(lintFileEntities)) {
                toolDefectVO.setExist(0);
                toolDefectVO.setNewDefect(newDefect);
                toolDefectVO.setHistoryDefect(historyDefect);
                List<TaskOverviewDetailVO.ToolDefectVO> toolDefectVoList =
                        allToolsDefectMap.computeIfAbsent(taskId, k -> Lists.newArrayList());
                toolDefectVoList.add(toolDefectVO);
                continue;
            }
            // 判断新老告警的时间
            Long time = newDefectJudgeTimeMap.get(taskId);
            if (time == null) {
                time = 0L;
            }
            int defectCount = 0;
            for (LintFileEntity lintFileEntity : lintFileEntities) {
                List<LintDefectEntity> defectList = lintFileEntity.getDefectList();
                if (CollectionUtils.isEmpty(defectList)) {
                    continue;
                }

                for (LintDefectEntity defectEntity : defectList) {
                    if (ComConstants.DefectStatus.NEW.value() == defectEntity.getStatus()) {
                        int severity = defectEntity.getSeverity();
                        long lineUpdateTime = defectEntity.getLineUpdateTime();
                        if (lineUpdateTime == 0) {
                            lineUpdateTime = lintFileEntity.getCreateTime();
                        }
                        if (lineUpdateTime > time) {
                            newDefect.count(severity);
                        }
                        else {
                            historyDefect.count(severity);
                        }

                        defectCount++;
                    }
                }
            }
            toolDefectVO.setExist(defectCount);
            toolDefectVO.setNewDefect(newDefect);
            toolDefectVO.setHistoryDefect(historyDefect);

            List<TaskOverviewDetailVO.ToolDefectVO> toolDefectVoList =
                    allToolsDefectMap.computeIfAbsent(taskId, k -> Lists.newArrayList());
            toolDefectVoList.add(toolDefectVO);
        }
    }


    /**
     * 批量获取编译工具分析结果
     *
     * @param toolName          工具名称
     * @param taskIdList        项目ID列表
     * @param allToolsDefectMap 保存项目的所有工具告警列表
     * @param toolDisplayName   工具展示名称
     */
    private void batchGetCommonDefectStat(String toolName, Set<Long> taskIdList,
            Map<Long, List<TaskOverviewDetailVO.ToolDefectVO>> allToolsDefectMap, String toolDisplayName) {
        if (CollectionUtils.isEmpty(taskIdList)) {
            log.info("taskIdList isEmpty tool: {}", toolName);
            return;
        }

        // 获取告警列表
        List<DefectEntity> defectEntityList = defectDao.batchQueryDefect(toolName, taskIdList, null, null);
        if (CollectionUtils.isEmpty(defectEntityList)) {
            log.info("commonStatEntityList isEmpty tool: {}", toolName);
        }

        Map<Long, List<DefectEntity>> defectMap = Maps.newHashMap();
        for (DefectEntity defectEntity : defectEntityList) {
            long taskId = defectEntity.getTaskId();
            List<DefectEntity> defectList = defectMap.computeIfAbsent(taskId, val -> Lists.newArrayList());
            defectList.add(defectEntity);
        }

        for (Long taskId : taskIdList) {
            TaskOverviewDetailVO.ToolDefectVO toolDefectVO = new TaskOverviewDetailVO.ToolDefectVO();
            toolDefectVO.setToolName(toolName);
            toolDefectVO.setDisplayName(toolDisplayName);

            CommonChartAuthorVO existCount = new CommonChartAuthorVO();
            CommonChartAuthorVO closedCount = new CommonChartAuthorVO();

            List<DefectEntity> defectEntities = defectMap.get(taskId);
            if (CollectionUtils.isEmpty(defectEntities)) {
                toolDefectVO.setExistCount(existCount);
                toolDefectVO.setClosedCount(closedCount);
                List<TaskOverviewDetailVO.ToolDefectVO> toolDefectVoList =
                        allToolsDefectMap.computeIfAbsent(taskId, k -> Lists.newArrayList());
                toolDefectVoList.add(toolDefectVO);
                continue;
            }

            // 统计修复和遗留告警数
            for (DefectEntity defect : defectEntities) {
                int status = defect.getStatus();
                int severity = defect.getSeverity();

                if ((ComConstants.DefectStatus.FIXED.value() & status) > 0) {
                    closedCount.count(severity);
                }

                if (ComConstants.DefectStatus.NEW.value() == status) {
                    existCount.count(severity);
                }
            }
            toolDefectVO.setClosedCount(closedCount);
            toolDefectVO.setExistCount(existCount);

            List<TaskOverviewDetailVO.ToolDefectVO> toolDefectVoList =
                    allToolsDefectMap.computeIfAbsent(taskId, k -> Lists.newArrayList());
            toolDefectVoList.add(toolDefectVO);
        }
    }


    /**
     * 批量统计圈复杂度
     *
     * @param toolName          工具名称
     * @param taskIdList        项目ID列表
     * @param allToolsDefectMap 保存项目的所有工具告警列表
     * @param toolDisplayName   工具展示名称
     */
    private void batchGetCcnAnalyzeStat(String toolName, Set<Long> taskIdList,
            Map<Long, List<TaskOverviewDetailVO.ToolDefectVO>> allToolsDefectMap, String toolDisplayName) {
        List<CCNStatisticEntity> ccnStatEntities = ccnDefectDao.batchFindByTaskIdInAndTool(taskIdList, toolName);
        if (CollectionUtils.isEmpty(ccnStatEntities)) {
            log.info("ccnStatEntityList isEmpty tool: {}", toolName);
        }

        Map<Long, List<CCNStatisticEntity>> defectMap =
                ccnStatEntities.stream().collect(Collectors.groupingBy(CCNStatisticEntity::getTaskId));

        for (Long taskId : taskIdList) {
            TaskOverviewDetailVO.ToolDefectVO toolDefectVO = new TaskOverviewDetailVO.ToolDefectVO();
            toolDefectVO.setToolName(toolName);
            toolDefectVO.setDisplayName(toolDisplayName);

            List<CCNStatisticEntity> ccnStatisticEntities = defectMap.get(taskId);
            int superHighCount = 0;
            int highCount = 0;
            int mediumCount = 0;
            int lowCount = 0;
            float averageCcn = 0;
            if (CollectionUtils.isNotEmpty(ccnStatisticEntities)) {
                CCNStatisticEntity entity = ccnStatisticEntities.iterator().next();
                superHighCount = entity.getSuperHighCount() == null ? 0 : entity.getSuperHighCount();
                highCount = entity.getHighCount() == null ? 0 : entity.getHighCount();
                mediumCount = entity.getMediumCount() == null ? 0 : entity.getMediumCount();
                lowCount = entity.getLowCount() == null ? 0 : entity.getLowCount();
                BigDecimal bigDecimal = BigDecimal.valueOf(entity.getAverageCCN());
                averageCcn = bigDecimal.setScale(2, BigDecimal.ROUND_HALF_UP).floatValue();
            }

            toolDefectVO.setSuperHighCount(superHighCount);
            toolDefectVO.setHighCount(highCount);
            toolDefectVO.setMediumCount(mediumCount);
            toolDefectVO.setLowCount(lowCount);
            toolDefectVO.setAverageCcn(averageCcn);

            List<TaskOverviewDetailVO.ToolDefectVO> toolDefectVoList =
                    allToolsDefectMap.computeIfAbsent(taskId, k -> Lists.newArrayList());
            toolDefectVoList.add(toolDefectVO);
        }
    }


    /**
     * 批量统计重复率
     *
     * @param toolName          工具名称
     * @param taskIdList        项目ID列表
     * @param allToolsDefectMap 保存项目的所有工具告警列表
     * @param toolDisplayName   工具展示名称
     */
    private void batchGetDupcAnalyzeStat(String toolName, Set<Long> taskIdList,
            Map<Long, List<TaskOverviewDetailVO.ToolDefectVO>> allToolsDefectMap, String toolDisplayName) {
        List<DUPCStatisticEntity> dupcStatEntities = dupcDefectDao.batchFindByTaskIdInAndTool(taskIdList, toolName);
        if (CollectionUtils.isEmpty(dupcStatEntities)) {
            log.info("dupcStatEntityList isEmpty tool: {}", toolName);
        }

        Map<Long, List<DUPCStatisticEntity>> defectMap =
                dupcStatEntities.stream().collect(Collectors.groupingBy(DUPCStatisticEntity::getTaskId));

        for (Long taskId : taskIdList) {
            TaskOverviewDetailVO.ToolDefectVO toolDefectVO = new TaskOverviewDetailVO.ToolDefectVO();
            toolDefectVO.setToolName(toolName);
            toolDefectVO.setDisplayName(toolDisplayName);

            List<DUPCStatisticEntity> dupcStatisticEntities = defectMap.get(taskId);
            int defectCount = 0;
            int superHighCount = 0;
            int highCount = 0;
            int mediumCount = 0;
            float dupRate = 0;
            if (CollectionUtils.isNotEmpty(dupcStatisticEntities)) {
                DUPCStatisticEntity entity = dupcStatisticEntities.iterator().next();
                defectCount = entity.getDefectCount();
                superHighCount = entity.getSuperHighCount() == null ? 0 : entity.getSuperHighCount();
                highCount = entity.getHighCount() == null ? 0 : entity.getHighCount();
                mediumCount = entity.getMediumCount() == null ? 0 : entity.getMediumCount();
                BigDecimal bigDecimal = BigDecimal.valueOf(entity.getDupRate());
                dupRate = bigDecimal.setScale(2, BigDecimal.ROUND_HALF_UP).floatValue();
            }
            toolDefectVO.setDefectCount(defectCount);
            toolDefectVO.setSuperHighCount(superHighCount);
            toolDefectVO.setHighCount(highCount);
            toolDefectVO.setMediumCount(mediumCount);
            toolDefectVO.setDupRate(dupRate);

            List<TaskOverviewDetailVO.ToolDefectVO> toolDefectVoList =
                    allToolsDefectMap.computeIfAbsent(taskId, k -> Lists.newArrayList());
            toolDefectVoList.add(toolDefectVO);
        }
    }


    /**
     * 赋值给各任务接入的工具告警列表
     *
     * @param taskOverviewList  任务列表
     * @param allToolsDefectMap 每个任务对应的工具告警对象列表
     */
    private void assignTaskToolDefectInfo(List<TaskOverviewDetailVO> taskOverviewList,
            Map<Long, List<TaskOverviewDetailVO.ToolDefectVO>> allToolsDefectMap) {
        if (CollectionUtils.isEmpty(taskOverviewList) || MapUtils.isEmpty(allToolsDefectMap)) {
            log.error("no data for assignTaskToolDefectInfo!");
            return;
        }

        for (TaskOverviewDetailVO detailVO : taskOverviewList) {
            List<TaskOverviewDetailVO.ToolDefectVO> toolDefectVoList = allToolsDefectMap.get(detailVO.getTaskId());

            if (CollectionUtils.isEmpty(toolDefectVoList)) {
                toolDefectVoList = Lists.newArrayList();
            }
            detailVO.setToolDefectInfo(toolDefectVoList);
        }
    }

}
