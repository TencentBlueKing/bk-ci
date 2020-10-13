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

package com.tencent.bk.codecc.apiquery.service.openapi;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.apiquery.defect.dao.DefectDao;
import com.tencent.bk.codecc.apiquery.defect.dao.StatisticDao;
import com.tencent.bk.codecc.apiquery.defect.dao.mongotemplate.CCNDefectDao;
import com.tencent.bk.codecc.apiquery.defect.dao.mongotemplate.DUPCDefectDao;
import com.tencent.bk.codecc.apiquery.defect.dao.mongotemplate.LintDefectDao;
import com.tencent.bk.codecc.apiquery.defect.model.CCNStatisticModel;
import com.tencent.bk.codecc.apiquery.defect.model.DUPCStatisticModel;
import com.tencent.bk.codecc.apiquery.defect.model.DefectStatModel;
import com.tencent.bk.codecc.apiquery.defect.model.LintDefectV2Model;
import com.tencent.bk.codecc.apiquery.defect.model.LintStatisticModel;
import com.tencent.bk.codecc.apiquery.service.MetaDataService;
import com.tencent.bk.codecc.apiquery.task.dao.TaskDao;
import com.tencent.bk.codecc.apiquery.task.dao.ToolMetaCacheServiceImpl;
import com.tencent.bk.codecc.apiquery.task.dao.mongotemplate.ToolDao;
import com.tencent.bk.codecc.apiquery.task.model.TaskInfoModel;
import com.tencent.bk.codecc.apiquery.task.model.ToolConfigInfoModel;
import com.tencent.bk.codecc.apiquery.utils.ConvertUtil;
import com.tencent.bk.codecc.apiquery.utils.PageUtils;
import com.tencent.bk.codecc.apiquery.vo.TaskToolInfoReqVO;
import com.tencent.bk.codecc.apiquery.vo.openapi.CheckerDefectStatVO;
import com.tencent.bk.codecc.apiquery.vo.openapi.TaskOverviewDetailRspVO;
import com.tencent.bk.codecc.apiquery.vo.openapi.TaskOverviewDetailVO;
import com.tencent.bk.codecc.apiquery.vo.report.CommonChartAuthorVO;
import com.tencent.bk.codecc.task.vo.MetadataVO;
import com.tencent.devops.common.api.ToolMetaBaseVO;
import com.tencent.devops.common.api.pojo.Page;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.Tool;
import com.tencent.devops.common.constant.RedisKeyConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
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
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private TaskDao taskDao;

    @Autowired
    private ToolDao toolDao;

    @Autowired
    private StatisticDao statisticDao;

    @Autowired
    private DefectDao defectDao;

    @Autowired
    private LintDefectDao lintDefectDao;

    @Autowired
    private CCNDefectDao ccnDefectDao;

    @Autowired
    protected DUPCDefectDao dupcDefectDao;

    @Autowired
    private MetaDataService metaDataService;

    @Autowired
    private ToolMetaCacheServiceImpl toolMetaCache;


    @Override
    public TaskOverviewDetailRspVO statisticsTaskOverview(TaskToolInfoReqVO reqVO, Integer pageNum, Integer pageSize,
            String sortType) {
        log.info("statisticsTaskOverview req content: {}", reqVO);
        TaskOverviewDetailRspVO rspVO = new TaskOverviewDetailRspVO();
        List<TaskOverviewDetailVO> taskOverviewList = Lists.newArrayList();

        // 如果没传默认不查工蜂扫描
        if (CollectionUtils.isEmpty(reqVO.getCreateFrom())) {
            reqVO.setCreateFrom(Sets.newHashSet(ComConstants.BsTaskCreateFrom.BS_CODECC.value(),
                    ComConstants.BsTaskCreateFrom.BS_PIPELINE.value()));
        }
        if (reqVO.getStatus() == null) {
            reqVO.setStatus(ComConstants.Status.ENABLE.value());
        }

        pageSize = pageSize == null ? 10 : pageSize >= 500 ? 500 : pageSize;
        Pageable pageable = PageUtils.INSTANCE.convertPageSizeToPageable(pageNum, pageSize, "task_id", sortType);

        Page<TaskInfoModel> taskInfoPage = taskDao.findTaskInfoPage(reqVO, pageable);
        List<TaskInfoModel> taskInfoModels = taskInfoPage.getRecords();
        if (CollectionUtils.isNotEmpty(taskInfoModels)) {
            Set<Long> taskIdSet = taskInfoModels.stream().map(TaskInfoModel::getTaskId).collect(Collectors.toSet());

            // 获取指定任务的工具配置列表
            List<ToolConfigInfoModel> toolConfigInfoModels =
                    toolDao.findByToolAndFollowStatus(taskIdSet, null, ComConstants.FOLLOW_STATUS.WITHDRAW.value(),
                            true);
            Map<Long, List<ToolConfigInfoModel>> taskToolConfigMap =
                    toolConfigInfoModels.stream().collect(Collectors.groupingBy(ToolConfigInfoModel::getTaskId));
            // 代码语言转换
            List<MetadataVO> metadataVoList = metaDataService.getCodeLangMetadataList();
            // 组织架构信息
            Map<Object, Object> deptInfo = redisTemplate.opsForHash().entries(RedisKeyConstants.KEY_DEPT_INFOS);

            Map<Long, List<String>> afterFilterIdMap = Maps.newHashMap();
            for (TaskInfoModel taskInfoModel : taskInfoModels) {
                TaskOverviewDetailVO taskOverviewVO = new TaskOverviewDetailVO();
                BeanUtils.copyProperties(taskInfoModel, taskOverviewVO);
                taskOverviewVO
                        .setBgName(ObjectUtils.toString(deptInfo.get(String.valueOf(taskInfoModel.getBgId())), ""));
                taskOverviewVO
                        .setDeptName(ObjectUtils.toString(deptInfo.get(String.valueOf(taskInfoModel.getDeptId())), ""));
                taskOverviewVO.setCenterName(
                        ObjectUtils.toString(deptInfo.get(String.valueOf(taskInfoModel.getCenterId())), ""));
                taskOverviewVO.setCodeLang(ConvertUtil.convertCodeLang(taskInfoModel.getCodeLang(), metadataVoList));

                taskOverviewList.add(taskOverviewVO);
                long taskId = taskInfoModel.getTaskId();
                List<ToolConfigInfoModel> toolInfoList = taskToolConfigMap.get(taskId);
                if (CollectionUtils.isEmpty(toolInfoList)) {
                    log.info("noToolsRegistered task {}", taskId);
                    continue;
                }

                for (ToolConfigInfoModel toolConfigInfo : toolInfoList) {
                    if (ComConstants.FOLLOW_STATUS.WITHDRAW.value() == toolConfigInfo.getFollowStatus()) {
                        continue;
                    }

                    List<String> tools = afterFilterIdMap.computeIfAbsent(taskId, k -> Lists.newArrayList());
                    tools.add(toolConfigInfo.getToolName());
                }
            }

            // 统计各工具
            Map<Long, List<TaskOverviewDetailVO.ToolDefectVO>> allTaskToolStat = batchQueryToolResult(afterFilterIdMap);

            // 赋值项目各工具告警对象列表
            assignTaskToolDefectInfo(taskOverviewList, allTaskToolStat);
        }

        Page<TaskOverviewDetailVO> overviewDetailVoPage =
                new Page<>(taskInfoPage.getCount(), taskInfoPage.getPage(), taskInfoPage.getPageSize(),
                        taskInfoPage.getTotalPages(), taskOverviewList);
        rspVO.setStatisticsTask(overviewDetailVoPage);

        return rspVO;
    }

    @Override
    public Page<CheckerDefectStatVO> statCheckerDefect(TaskToolInfoReqVO reqVO, Integer pageNum, Integer pageSize) {
        log.info("statisticsTaskOverview req content: pageNum[{}] pageSize[{}], {}", pageNum, pageSize, reqVO);
        List<CheckerDefectStatVO> data = Lists.newArrayList();

        pageSize = pageSize == null ? 100 : pageSize >= 500 ? 500 : pageSize;
        Pageable pageable = PageUtils.INSTANCE.convertPageSizeToPageable(pageNum, pageSize, "task_id", "ASC");

        // 设置默认查询条件：默认查工蜂扫描
        if (CollectionUtils.isEmpty(reqVO.getCreateFrom())) {
            reqVO.setCreateFrom(Sets.newHashSet(ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value()));
        }
        // 默认查询有效任务
        if (reqVO.getStatus() == null) {
            reqVO.setStatus(ComConstants.Status.ENABLE.value());
        }
        // 默认查待修复告警
        int defectStatus =
                reqVO.getDefectStatus() == null ? ComConstants.DefectStatus.NEW.value() : reqVO.getDefectStatus();

        Page<TaskInfoModel> taskInfoPage = taskDao.findTaskIdListByCondition(reqVO, pageable);
        List<TaskInfoModel> taskInfoModels = taskInfoPage.getRecords();
        if (CollectionUtils.isNotEmpty(taskInfoModels)) {

            List<Long> taskIdSet = taskInfoModels.stream().map(TaskInfoModel::getTaskId).collect(Collectors.toList());
            List<LintDefectV2Model> defectByGroupChecker =
                    lintDefectDao.findDefectByGroupChecker(taskIdSet, reqVO.getToolName(), defectStatus);
            log.info("findDefectByGroupChecker size: {}", defectByGroupChecker.size());

            if (CollectionUtils.isNotEmpty(defectByGroupChecker)) {
                data = defectByGroupChecker.stream().map(model -> {
                    CheckerDefectStatVO defectStatVO = new CheckerDefectStatVO();
                    BeanUtils.copyProperties(model, defectStatVO);
                    defectStatVO.setCount(model.getLineNum());
                    return defectStatVO;
                }).collect(Collectors.toList());
            }
        }

        return new Page<>(taskInfoPage.getCount(), taskInfoPage.getPage(), taskInfoPage.getPageSize(),
                taskInfoPage.getTotalPages(), data);
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
                if (Tool.CLOC.name().equals(tool)) {
                    continue;
                }
                Set<Long> taskIdSet = toolTaskIdMap.computeIfAbsent(tool, k -> Sets.newHashSet());
                taskIdSet.add(taskId);
            }
        }

        // 按工具分别批量查询
        for (Map.Entry<String, Set<Long>> entry : toolTaskIdMap.entrySet()) {
            String tool = entry.getKey();
            Set<Long> taskIdList = entry.getValue();

            /* 修复ConcurrentMap不允许Null的问题 */
            if (StringUtils.isBlank(tool)) {
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
                batchGetLintAnalyzeStat(tool, taskIdList, allToolsDefectMap, toolDisplayName);
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
    private void batchGetLintAnalyzeStat(String toolName, Set<Long> taskIdList,
            Map<Long, List<TaskOverviewDetailVO.ToolDefectVO>> allToolsDefectMap, String toolDisplayName) {
        List<LintStatisticModel> lintStatisticModels = lintDefectDao.findStatByTaskIdInAndToolIs(taskIdList, toolName);
        if (CollectionUtils.isEmpty(lintStatisticModels)) {
            log.info("lintStatisticModelList isEmpty tool: {}", toolName);
        }
        Map<Long, LintStatisticModel> statisticModelMap = lintStatisticModels.stream()
                .collect(Collectors.toMap(LintStatisticModel::getTaskId, Function.identity(), (k, v) -> v));

        for (Long taskId : taskIdList) {
            TaskOverviewDetailVO.ToolDefectVO toolDefectVO = new TaskOverviewDetailVO.ToolDefectVO();
            toolDefectVO.setToolName(toolName);
            toolDefectVO.setDisplayName(toolDisplayName);

            CommonChartAuthorVO newDefect = new CommonChartAuthorVO();
            CommonChartAuthorVO historyDefect = new CommonChartAuthorVO();

            LintStatisticModel statModel = statisticModelMap.get(taskId);
            if (statModel == null) {
                toolDefectVO.setExist(0);
                toolDefectVO.setNewDefect(newDefect);
                toolDefectVO.setHistoryDefect(historyDefect);
                List<TaskOverviewDetailVO.ToolDefectVO> toolDefectVoList =
                        allToolsDefectMap.computeIfAbsent(taskId, k -> Lists.newArrayList());
                toolDefectVoList.add(toolDefectVO);
                continue;
            }

            int totalNewSerious = statModel.getTotalNewSerious() == null ? 0 : statModel.getTotalNewSerious();
            int totalNewNormal = statModel.getTotalNewNormal() == null ? 0 : statModel.getTotalNewNormal();
            int totalNewPrompt = statModel.getTotalNewPrompt() == null ? 0 : statModel.getTotalNewPrompt();
            newDefect.setSerious(totalNewSerious);
            newDefect.setNormal(totalNewNormal);
            newDefect.setPrompt(totalNewPrompt);

            int totalSerious = statModel.getTotalSerious() == null ? 0 : statModel.getTotalSerious();
            int totalNormal = statModel.getTotalNormal() == null ? 0 : statModel.getTotalNormal();
            int totalPrompt = statModel.getTotalPrompt() == null ? 0 : statModel.getTotalPrompt();
            historyDefect.setSerious(totalSerious == 0 ? 0 : totalSerious - totalNewSerious);
            historyDefect.setNormal(totalNormal == 0 ? 0 : totalNormal - totalNewNormal);
            historyDefect.setPrompt(totalPrompt == 0 ? 0 : totalPrompt - totalNewPrompt);

            toolDefectVO.setExist(statModel.getDefectCount());
            toolDefectVO.setNewDefect(newDefect);
            toolDefectVO.setHistoryDefect(historyDefect);
            List<TaskOverviewDetailVO.ToolDefectVO> toolDefectVoList =
                    allToolsDefectMap.computeIfAbsent(statModel.getTaskId(), k -> Lists.newArrayList());
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

        // 获取告警汇总列表
        List<DefectStatModel> defectModels = defectDao.findDefectByTaskIdInAndToolName(taskIdList, toolName);
        if (CollectionUtils.isEmpty(defectModels)) {
            log.info("defectModels isEmpty tool: {}", toolName);
        }

        Map<Long, List<DefectStatModel>> taskStatMap =
                defectModels.stream().collect(Collectors.groupingBy(DefectStatModel::getTaskId));

        for (Long taskId : taskIdList) {
            TaskOverviewDetailVO.ToolDefectVO toolDefectVO = new TaskOverviewDetailVO.ToolDefectVO();
            toolDefectVO.setToolName(toolName);
            toolDefectVO.setDisplayName(toolDisplayName);

            CommonChartAuthorVO existCount = new CommonChartAuthorVO();
            CommonChartAuthorVO closedCount = new CommonChartAuthorVO();

            List<DefectStatModel> defectStatModels = taskStatMap.get(taskId);
            if (CollectionUtils.isEmpty(defectStatModels)) {
                toolDefectVO.setExistCount(existCount);
                toolDefectVO.setClosedCount(closedCount);
                List<TaskOverviewDetailVO.ToolDefectVO> toolDefectVoList =
                        allToolsDefectMap.computeIfAbsent(taskId, k -> Lists.newArrayList());
                toolDefectVoList.add(toolDefectVO);
                continue;
            }

            // 统计修复和遗留告警数
            for (DefectStatModel defect : defectStatModels) {
                int count = defect.getCount();
                int status = defect.getStatus();
                int severity = defect.getSeverity();

                if ((ComConstants.DefectStatus.FIXED.value() & status) > 0) {
                    if (severity == ComConstants.SERIOUS) {
                        closedCount.setSerious(count);
                    }
                    else if (severity == ComConstants.NORMAL) {
                        closedCount.setNormal(count);
                    }
                    else if (severity == ComConstants.PROMPT || severity == ComConstants.PROMPT_IN_DB) {
                        closedCount.setPrompt(count);
                    }
                }

                if (ComConstants.DefectStatus.NEW.value() == status) {
                    if (severity == ComConstants.SERIOUS) {
                        existCount.setSerious(count);
                    }
                    else if (severity == ComConstants.NORMAL) {
                        existCount.setNormal(count);
                    }
                    else if (severity == ComConstants.PROMPT || severity == ComConstants.PROMPT_IN_DB) {
                        existCount.setPrompt(count);
                    }
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
        List<CCNStatisticModel> ccnStatEntities = ccnDefectDao.batchFindByTaskIdInAndTool(taskIdList, toolName);
        if (CollectionUtils.isEmpty(ccnStatEntities)) {
            log.info("ccnStatEntityList isEmpty tool: {}", toolName);
        }

        Map<Long, List<CCNStatisticModel>> defectMap =
                ccnStatEntities.stream().collect(Collectors.groupingBy(CCNStatisticModel::getTaskId));

        for (Long taskId : taskIdList) {
            TaskOverviewDetailVO.ToolDefectVO toolDefectVO = new TaskOverviewDetailVO.ToolDefectVO();
            toolDefectVO.setToolName(toolName);
            toolDefectVO.setDisplayName(toolDisplayName);

            List<CCNStatisticModel> ccnStatisticModels = defectMap.get(taskId);
            int superHighCount = 0;
            int highCount = 0;
            int mediumCount = 0;
            int lowCount = 0;
            float averageCcn = 0;
            if (CollectionUtils.isNotEmpty(ccnStatisticModels)) {
                CCNStatisticModel model = ccnStatisticModels.iterator().next();
                superHighCount = model.getSuperHighCount() == null ? 0 : model.getSuperHighCount();
                highCount = model.getHighCount() == null ? 0 : model.getHighCount();
                mediumCount = model.getMediumCount() == null ? 0 : model.getMediumCount();
                lowCount = model.getLowCount() == null ? 0 : model.getLowCount();
                BigDecimal bigDecimal = BigDecimal.valueOf(model.getAverageCCN());
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
        List<DUPCStatisticModel> dupcStatEntities = dupcDefectDao.batchFindByTaskIdInAndTool(taskIdList, toolName);
        if (CollectionUtils.isEmpty(dupcStatEntities)) {
            log.info("dupcStatEntityList isEmpty tool: {}", toolName);
        }

        Map<Long, List<DUPCStatisticModel>> defectMap =
                dupcStatEntities.stream().collect(Collectors.groupingBy(DUPCStatisticModel::getTaskId));

        for (Long taskId : taskIdList) {
            TaskOverviewDetailVO.ToolDefectVO toolDefectVO = new TaskOverviewDetailVO.ToolDefectVO();
            toolDefectVO.setToolName(toolName);
            toolDefectVO.setDisplayName(toolDisplayName);

            List<DUPCStatisticModel> dupcStatisticModels = defectMap.get(taskId);
            int defectCount = 0;
            int superHighCount = 0;
            int highCount = 0;
            int mediumCount = 0;
            float dupRate = 0;
            if (CollectionUtils.isNotEmpty(dupcStatisticModels)) {
                DUPCStatisticModel model = dupcStatisticModels.iterator().next();
                defectCount = model.getDefectCount();
                superHighCount = model.getSuperHighCount() == null ? 0 : model.getSuperHighCount();
                highCount = model.getHighCount() == null ? 0 : model.getHighCount();
                mediumCount = model.getMediumCount() == null ? 0 : model.getMediumCount();
                BigDecimal bigDecimal = BigDecimal.valueOf(model.getDupRate());
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
