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

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.google.common.base.CaseFormat;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.apiquery.defect.dao.DefectDao;
import com.tencent.bk.codecc.apiquery.defect.dao.mongotemplate.CCNClusterStatisticDao;
import com.tencent.bk.codecc.apiquery.defect.dao.mongotemplate.CCNDefectDao;
import com.tencent.bk.codecc.apiquery.defect.dao.mongotemplate.DUPCDefectDao;
import com.tencent.bk.codecc.apiquery.defect.dao.mongotemplate.LintDefectDao;
import com.tencent.bk.codecc.apiquery.defect.dao.mongotemplate.StandardClusterStatisticDao;
import com.tencent.bk.codecc.apiquery.defect.model.CCNStatisticModel;
import com.tencent.bk.codecc.apiquery.defect.model.CcnClusterStatisticModel;
import com.tencent.bk.codecc.apiquery.defect.model.CodeRepoModel;
import com.tencent.bk.codecc.apiquery.defect.model.DUPCStatisticModel;
import com.tencent.bk.codecc.apiquery.defect.model.DefectModel;
import com.tencent.bk.codecc.apiquery.defect.model.DefectStatModel;
import com.tencent.bk.codecc.apiquery.defect.model.LintDefectV2Model;
import com.tencent.bk.codecc.apiquery.defect.model.LintStatisticModel;
import com.tencent.bk.codecc.apiquery.defect.model.MetricsModel;
import com.tencent.bk.codecc.apiquery.defect.model.StandardClusterStatisticModel;
import com.tencent.bk.codecc.apiquery.defect.model.TaskLogOverviewModel;
import com.tencent.bk.codecc.apiquery.hyperlink.TaskHyperlink;
import com.tencent.bk.codecc.apiquery.pojo.TaskDefectSummary;
import com.tencent.bk.codecc.apiquery.service.CodeRepoService;
import com.tencent.bk.codecc.apiquery.service.MetaDataService;
import com.tencent.bk.codecc.apiquery.service.MetricsService;
import com.tencent.bk.codecc.apiquery.service.TaskLogOverviewService;
import com.tencent.bk.codecc.apiquery.task.dao.TaskDao;
import com.tencent.bk.codecc.apiquery.task.dao.ToolMetaCacheServiceImpl;
import com.tencent.bk.codecc.apiquery.task.dao.mongotemplate.ToolDao;
import com.tencent.bk.codecc.apiquery.task.model.TaskInfoModel;
import com.tencent.bk.codecc.apiquery.task.model.ToolConfigInfoModel;
import com.tencent.bk.codecc.apiquery.utils.ConvertUtil;
import com.tencent.bk.codecc.apiquery.utils.PageUtils;
import com.tencent.bk.codecc.apiquery.vo.TaskToolInfoReqVO;
import com.tencent.bk.codecc.apiquery.vo.op.DimensionStatVO;
import com.tencent.bk.codecc.apiquery.vo.op.TaskDefectSummaryVO;
import com.tencent.bk.codecc.apiquery.vo.openapi.CheckerDefectStatVO;
import com.tencent.bk.codecc.apiquery.vo.openapi.TaskOverviewDetailRspVO;
import com.tencent.bk.codecc.apiquery.vo.openapi.TaskOverviewDetailVO;
import com.tencent.bk.codecc.apiquery.vo.report.CommonChartAuthorVO;
import com.tencent.bk.codecc.task.vo.MetadataVO;
import com.tencent.devops.common.api.ToolMetaBaseVO;
import com.tencent.devops.common.api.pojo.Page;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.DefectStatus;
import com.tencent.devops.common.constant.ComConstants.Tool;
import com.tencent.devops.common.constant.ComConstants.ToolPattern;
import com.tencent.devops.common.constant.RedisKeyConstants;
import com.tencent.devops.common.util.DateTimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import com.tencent.devops.common.util.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
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
    private DefectDao defectDao;

    @Autowired
    private LintDefectDao lintDefectDao;

    @Autowired
    private CCNDefectDao ccnDefectDao;

    @Autowired
    protected DUPCDefectDao dupcDefectDao;
    @Autowired
    private CCNClusterStatisticDao ccnClusterStatisticDao;
    @Autowired
    private StandardClusterStatisticDao standardClusterStatisticDao;
    @Autowired
    private MetaDataService metaDataService;
    @Autowired
    private ToolMetaCacheServiceImpl toolMetaCache;
    @Autowired
    private CodeRepoService codeRepoService;
    @Autowired
    private MetricsService metricsService;
    @Autowired
    private TaskLogOverviewService taskLogOverviewService;


    private static final int NEW_FLAG = DefectStatus.NEW.value();
    private static final int FIXED = NEW_FLAG | DefectStatus.FIXED.value();
    private static final List<Integer> NEW_ADD = Lists.newArrayList(NEW_FLAG, NEW_FLAG | DefectStatus.FIXED.value());
    private static final List<Integer> EXCLUDE =
            Lists.newArrayList(NEW_FLAG | DefectStatus.PATH_MASK.value(), NEW_FLAG | DefectStatus.CHECKER_MASK.value());
    private static final String CREATE_TIME = "create_time";
    private static final String FIXED_TIME = "fixed_time";
    private static final String EXCLUDE_TIME = "exclude_time";


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
        if (reqVO.getHasAdminTask() == null) {
            reqVO.setHasAdminTask(1);
        } else {
            reqVO.setExcludeUserList(metaDataService.queryExcludeUserList());
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
        // 默认查待修复告警 | -> 按位或
        int defectStatus = reqVO.getDefectStatus() == null ? DefectStatus.NEW.value() :
                reqVO.getDefectStatus() | DefectStatus.NEW.value();

        long startTime = DateTimeUtils.getTimeStampStart(reqVO.getStartTime());
        long endTime = DateTimeUtils.getTimeStampEnd(reqVO.getEndTime());

        Page<TaskInfoModel> taskInfoPage = taskDao.findTaskIdListByCondition(reqVO, pageable);
        List<TaskInfoModel> taskInfoModels = taskInfoPage.getRecords();
        if (CollectionUtils.isNotEmpty(taskInfoModels)) {

            List<Long> taskIdSet = taskInfoModels.stream().map(TaskInfoModel::getTaskId).collect(Collectors.toList());
            List<LintDefectV2Model> defectByGroupChecker = lintDefectDao
                    .findDefectByGroupChecker(taskIdSet, reqVO.getToolName(), defectStatus, startTime, endTime);
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

            if (ToolPattern.LINT.name().equals(pattern)) {
                batchGetLintAnalyzeStat(tool, taskIdList, allToolsDefectMap, toolDisplayName);
            }
            else if (ToolPattern.CCN.name().equals(pattern)) {
                batchGetCcnAnalyzeStat(tool, taskIdList, allToolsDefectMap, toolDisplayName);
            }
            else if (ToolPattern.DUPC.name().equals(pattern)) {
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

                if ((DefectStatus.FIXED.value() & status) > 0) {
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

                if (DefectStatus.NEW.value() == status) {
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

    /**
     * 按维度分页统计任务时间范围内的告警汇总数据
     *
     * @param reqVO 请求体
     * @return page
     */
    @Override
    public Page<TaskDefectSummaryVO> queryTaskDefectSum(TaskToolInfoReqVO reqVO, Integer pageNum, Integer pageSize,
            String sortField, String sortType) {
        log.info("queryTaskDefectSum req content: pageNum[{}] pageSize[{}], {}", pageNum, pageSize, reqVO);

        // 设置默认参数
        setDefectConditions(reqVO);

        // 日期范围时间戳
        long startTime = DateTimeUtils.getTimeStamp(reqVO.getStartTime());
        long endTime = DateTimeUtils.getTimeStamp(reqVO.getEndTime());
        reqVO.setStartTime(null);
        reqVO.setEndTime(null);

        return getTaskDefectSummaryVOPage(reqVO, pageNum, pageSize, sortField, sortType, startTime, endTime);
    }

    private void setDefectConditions(@NotNull TaskToolInfoReqVO reqVO) {
        // 默认查使用中的任务
        if (reqVO.getStatus() == null) {
            reqVO.setStatus(ComConstants.Status.ENABLE.value());
        }
        // 筛除排除人员
        if (reqVO.getHasAdminTask() == null || reqVO.getHasAdminTask() != 1) {
            reqVO.setExcludeUserList(metaDataService.queryExcludeUserList());
        }
        // 如果没传默认查非开源的任务,先筛选有效工具的任务ID
        List<Long> reqTaskIds;
        List<String> opDimensionTools = toolMetaCache.getOpDimensionTools();
        Set<String> createFrom = reqVO.getCreateFrom();
        if (createFrom != null && createFrom.size() == 1
                && createFrom.contains(ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value())) {
            List<Long> list = taskDao.findByBgIdAndDeptId(reqVO);
            reqTaskIds = toolDao.findTaskIdByToolAndStatus(list, opDimensionTools,
                    ComConstants.FOLLOW_STATUS.WITHDRAW.value(), true);
        } else {
            if (CollectionUtils.isEmpty(createFrom)) {
                reqVO.setCreateFrom(Sets.newHashSet(ComConstants.BsTaskCreateFrom.BS_CODECC.value(),
                        ComConstants.BsTaskCreateFrom.BS_PIPELINE.value()));
            }
            List<Long> list = taskDao.findByBgIdAndDeptId(reqVO);
            reqTaskIds = toolDao.findTaskIdByToolAndStatus(list, opDimensionTools,
                    ComConstants.FOLLOW_STATUS.ACCESSED.value(), false);
        }
        reqVO.setTaskIds(reqTaskIds);
    }


    /**
     * 按条件分页查询维度告警数据
     * @param reqVO 请求体
     * @return page
     */
    @NotNull
    private Page<TaskDefectSummaryVO> getTaskDefectSummaryVOPage(@NotNull TaskToolInfoReqVO reqVO, Integer pageNum,
            Integer pageSize, String sortField, String sortType, long startTime, long endTime) {

        // 封装分页类查询任务详情信息
        String sortFieldInDb =
                CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, sortField == null ? "taskId" : sortField);
        Pageable pageable = PageUtils.INSTANCE.convertPageSizeToPageable(pageNum, pageSize, sortFieldInDb, sortType);
        Page<TaskInfoModel> taskInfoPage = taskDao.findTaskInfoPage(reqVO, pageable);
        List<TaskInfoModel> taskInfoModels = taskInfoPage.getRecords();

        List<TaskDefectSummaryVO> resDataList = Lists.newArrayList();
        if (CollectionUtils.isNotEmpty(taskInfoModels)) {
            // 任务ID
            Set<Long> taskIdSet = taskInfoModels.stream().map(TaskInfoModel::getTaskId).collect(Collectors.toSet());

            long begin = System.currentTimeMillis();
            // 时间范围内最新build id
            List<TaskLogOverviewModel> buildIdsByStartTime = taskLogOverviewService
                    .findBuildIdsByStartTime(taskIdSet, null, 1L, endTime);
            log.info("queryTaskDefectSum.findBuildIdsByStartTime cost: {}", System.currentTimeMillis() - begin);
            Set<String> buildIds =
                    buildIdsByStartTime.stream().map(TaskLogOverviewModel::getBuildId).collect(Collectors.toSet());

            // 获取代码仓库信息
            begin = System.currentTimeMillis();
            Map<Long, Set<CodeRepoModel>> taskCodeRepoMap = codeRepoService.queryCodeRepoInfo(taskIdSet, buildIds);
            log.info("queryTaskDefectSum.queryCodeRepoInfo cost: {}", System.currentTimeMillis() - begin);

            // 任务度量信息
            Map<Long, MetricsModel> taskMetricsMap = metricsService.getTaskMetricsMap(taskIdSet, buildIds);
            // 重复率
            Map<Long, DUPCStatisticModel> taskDUPCStatMap = getTaskDUPCStatisticMap(taskIdSet, buildIds);
            // 圈复杂度
            Map<Long, CcnClusterStatisticModel> taskCCNStatMap = getTaskCCNStatisticMap(taskIdSet, buildIds);
            // 规范类千行问题数
            Map<Long, StandardClusterStatisticModel> standardStatMap = getTaskStandardStatisticMap(taskIdSet, buildIds);

            // 最近分析状态
            begin = System.currentTimeMillis();
            Map<Long, String> taskLatestStatusMap = getTaskLatestAnalyzeStatusMap(taskIdSet);
            log.info("queryTaskDefectSum.getTaskLatestAnalyzeStatusMap cost: {}", System.currentTimeMillis() - begin);

            // 获取任务已接入状态的工具列表
            List<ToolConfigInfoModel> toolConfigInfoModels = toolDao.findByToolAndFollowStatus(taskIdSet, null,
                    ComConstants.FOLLOW_STATUS.ACCESSED.value(), false);

            // 遍历按任务和维度分组统计
            Map<String, Set<Long>> toolTaskIdsMap = Maps.newHashMap();
            for (ToolConfigInfoModel toolModel : toolConfigInfoModels) {
                long taskId = toolModel.getTaskId();
                String toolName = toolModel.getToolName();

                // IP_CHECK不计入算分逻辑
                if (Tool.IP_CHECK.name().equals(toolName)) {
                    continue;
                }

                String type = toolMetaCache.getToolBaseMetaCache(toolName).getType();
                if (ComConstants.ToolType.CLOC.name().equals(type) || ComConstants.ToolType.STAT.name().equals(type)) {
                    continue;
                }

                toolTaskIdsMap.computeIfAbsent(toolName, k -> Sets.newHashSet()).add(taskId);
            }

            // 按工具分别查询所有任务的告警数
            Map<Long, Map<String, DimensionStatVO>> taskAllDimensionMap = Maps.newHashMap();
            queryAllDimensionDefect(startTime, endTime, toolTaskIdsMap, taskAllDimensionMap);

            // 代码语言转换
            List<MetadataVO> metadataVoList = metaDataService.getCodeLangMetadataList();
            // 组织架构信息
            Map<Object, Object> deptInfo = redisTemplate.opsForHash().entries(RedisKeyConstants.KEY_DEPT_INFOS);

            for (TaskInfoModel taskInfoModel : taskInfoModels) {
                TaskDefectSummaryVO summaryVO = new TaskDefectSummaryVO();
                BeanUtils.copyProperties(taskInfoModel, summaryVO);
                summaryVO.setCodeLang(ConvertUtil.convertCodeLang(taskInfoModel.getCodeLang(), metadataVoList));
                summaryVO.setBgName(ObjectUtils.toString(deptInfo.get(String.valueOf(taskInfoModel.getBgId())), ""));
                summaryVO
                        .setDeptName(ObjectUtils.toString(deptInfo.get(String.valueOf(taskInfoModel.getDeptId())), ""));
                summaryVO.setCenterName(
                        ObjectUtils.toString(deptInfo.get(String.valueOf(taskInfoModel.getCenterId())), ""));

                long taskId = taskInfoModel.getTaskId();
                summaryVO.setAnalyzeDate(taskLatestStatusMap.getOrDefault(taskId, "--"));

                setCodeRepoBranchInfo(taskId, taskCodeRepoMap, summaryVO);

                Map<String, DimensionStatVO> dimensionStatVOMap = taskAllDimensionMap.get(taskId);
                if (null == dimensionStatVOMap) {
                    dimensionStatVOMap = Maps.newHashMap();
                }
                summaryVO.setDefectVo(dimensionStatVOMap
                        .getOrDefault(ComConstants.ToolType.DEFECT.name(), new DimensionStatVO()));
                summaryVO.setSecurityVo(dimensionStatVOMap
                        .getOrDefault(ComConstants.ToolType.SECURITY.name(), new DimensionStatVO()));
                summaryVO.setStandardVo(dimensionStatVOMap
                        .getOrDefault(ComConstants.ToolType.STANDARD.name(), new DimensionStatVO()));
                summaryVO.setCcnVo(
                        dimensionStatVOMap.getOrDefault(ComConstants.ToolType.CCN.name(), new DimensionStatVO()));
                summaryVO.setDupcVo(
                        dimensionStatVOMap.getOrDefault(ComConstants.ToolType.DUPC.name(), new DimensionStatVO()));

                MetricsModel metricsModel = taskMetricsMap.get(taskId);
                if (null != metricsModel) {
                    summaryVO.setRdIndicatorsScore(metricsModel.getRdIndicatorsScore());
                }

                StandardClusterStatisticModel standardClusterStat = standardStatMap.get(taskId);
                if (null != standardClusterStat) {
                    summaryVO.getStandardVo().setAverageThousandDefect(standardClusterStat.getAverageThousandDefect());
                }

                DUPCStatisticModel dupcStatModel = taskDUPCStatMap.get(taskId);
                if (null != dupcStatModel) {
                    DimensionStatVO dupcVo = summaryVO.getDupcVo();
                    dupcVo.setDupRate(dupcStatModel.getDupRate());
                    dupcVo.setExistTotalCount(dupcStatModel.getDefectCount());
                }

                CcnClusterStatisticModel ccnClusterStatModel = taskCCNStatMap.get(taskId);
                if (null != ccnClusterStatModel) {
                    DimensionStatVO ccnVo = summaryVO.getCcnVo();
                    ccnVo.setAverageThousandDefect(ccnClusterStatModel.getAverageThousandDefect());
                    ccnVo.setExistTotalCount(ccnClusterStatModel.getTotalCount());
                }

                resDataList.add(summaryVO);
            }
        }
        return new Page<>(taskInfoPage.getCount(), taskInfoPage.getPage(), taskInfoPage.getPageSize(),
                taskInfoPage.getTotalPages(), resDataList);
    }

    /**
     * 获取重复率统计信息
     */
    private Map<Long, DUPCStatisticModel> getTaskDUPCStatisticMap(Set<Long> taskIds, Set<String> buildIds) {
        if (CollectionUtils.isEmpty(taskIds) || CollectionUtils.isEmpty(buildIds)) {
            return Maps.newHashMap();
        }

        List<DUPCStatisticModel> dupcStatisticModels = dupcDefectDao.batchFindByTaskIdAndBuildId(taskIds, buildIds);
        return dupcStatisticModels.stream()
                .collect(Collectors.toMap(DUPCStatisticModel::getTaskId, Function.identity(), (k, v) -> v));
    }

    /**
     * 获取圈复杂度统计信息
     */
    private Map<Long, CcnClusterStatisticModel> getTaskCCNStatisticMap(Set<Long> taskIds, Set<String> buildIds) {
        if (CollectionUtils.isEmpty(taskIds) || CollectionUtils.isEmpty(buildIds)) {
            return Maps.newHashMap();
        }

        List<CcnClusterStatisticModel> ccnStatModels =
                ccnClusterStatisticDao.batchFindByTaskIdAndBuildId(taskIds, buildIds);
        return ccnStatModels.stream()
                .collect(Collectors.toMap(CcnClusterStatisticModel::getTaskId, Function.identity(), (k, v) -> v));
    }

    /**
     * 获取规范类告警千行问题数
     */
    private Map<Long, StandardClusterStatisticModel> getTaskStandardStatisticMap(Set<Long> taskIds,
            Set<String> buildIds) {
        if (CollectionUtils.isEmpty(taskIds) || CollectionUtils.isEmpty(buildIds)) {
            return Maps.newHashMap();
        }

        List<StandardClusterStatisticModel> standardClusterStatisticModels =
                standardClusterStatisticDao.batchFindByTaskIdAndBuildId(taskIds, buildIds);
        return standardClusterStatisticModels.stream()
                .collect(Collectors.toMap(StandardClusterStatisticModel::getTaskId, Function.identity(), (k, v) -> v));
    }

    private void setCodeRepoBranchInfo(long taskId, @NotNull Map<Long, Set<CodeRepoModel>> taskCodeRepoMap,
            TaskDefectSummaryVO summaryVO) {
        StringBuilder repoUrl = new StringBuilder();
        StringBuilder branch = new StringBuilder();
        Set<CodeRepoModel> codeRepos = taskCodeRepoMap.get(taskId);
        if (CollectionUtils.isNotEmpty(codeRepos)) {
            for (CodeRepoModel codeRepo : codeRepos) {
                repoUrl.append(codeRepo.getUrl()).append(ComConstants.STRING_SPLIT);
                branch.append(codeRepo.getBranch()).append(ComConstants.STRING_SPLIT);
            }
            if (repoUrl.length() > 0) {
                repoUrl.deleteCharAt(repoUrl.length() - 1);
            }
            if (branch.length() > 0) {
                branch.deleteCharAt(branch.length() - 1);
            }
        }

        summaryVO.setRepoUrl(repoUrl.toString());
        summaryVO.setBranch(branch.toString());
    }

    @NotNull
    private Map<Long, String> getTaskLatestAnalyzeStatusMap(Set<Long> taskIdSet) {
        Map<Long, String> taskLatestStatusMap = Maps.newHashMap();
        List<TaskLogOverviewModel> latestList = taskLogOverviewService.findLatestAnalyzeStatus(taskIdSet, null);
        for (TaskLogOverviewModel overviewModel : latestList) {
            if (overviewModel == null || overviewModel.getStartTime() == null) {
                continue;
            }
            String latestStatusStr = DateTimeUtils.second2DateString(overviewModel.getStartTime())
                    + ComConstants.ScanStatus.convertScanStatus(overviewModel.getStatus());
            taskLatestStatusMap.put(overviewModel.getTaskId(), latestStatusStr);
        }
        return taskLatestStatusMap;
    }

    /**
     * 分别给任务各个维度统计工具信息
     * @param startTime           告警筛选开始时间
     * @param endTime             告警筛选截止时间
     * @param toolTaskIdsMap      工具 任务id集合
     * @param taskAllDimensionMap 任务 维度 vo
     */
    private void queryAllDimensionDefect(long startTime, long endTime, @NotNull Map<String, Set<Long>> toolTaskIdsMap,
            Map<Long, Map<String, DimensionStatVO>> taskAllDimensionMap) {
        for (Map.Entry<String, Set<Long>> entry : toolTaskIdsMap.entrySet()) {
            String tool = entry.getKey();
            Set<Long> taskIds = entry.getValue();
            log.info("tool: {}, taskId size: {}", tool, taskIds.size());

            ToolMetaBaseVO toolMetaBaseVO = toolMetaCache.getToolBaseMetaCache(tool);
            String toolType = toolMetaBaseVO.getType();
            String pattern = toolMetaBaseVO.getPattern();

            // 单独处理这两款工具
            if (ToolPattern.CCN.name().equals(pattern) || ToolPattern.DUPC.name().equals(pattern)) {
                for (Long taskId : taskIds) {
                    Map<String, DimensionStatVO> statVOMap =
                            taskAllDimensionMap.computeIfAbsent(taskId, k -> Maps.newHashMap());
                    DimensionStatVO statVO = statVOMap.computeIfAbsent(toolType, k -> new DimensionStatVO());
                    statVO.addToolNum();
                }
                continue;
            }

            Map<Long, DimensionStatVO> taskDimensionMap;
            if (ToolPattern.LINT.name().equals(pattern)) {
                taskDimensionMap = batchQueryLintDefectStat(taskIds, tool, startTime, endTime, toolType);
            } else {
                taskDimensionMap = batchQueryCommonDefectStat(taskIds, tool, startTime, endTime);
            }

            for (Map.Entry<Long, DimensionStatVO> voEntry : taskDimensionMap.entrySet()) {
                Map<String, DimensionStatVO> statVOMap =
                        taskAllDimensionMap.computeIfAbsent(voEntry.getKey(), k -> Maps.newHashMap());
                DimensionStatVO statVO = statVOMap.computeIfAbsent(toolType, k -> new DimensionStatVO());
                DimensionStatVO entryDimensionStat = voEntry.getValue();
                statVO.addNewCount(entryDimensionStat.getNewCount());
                statVO.addFixedCount(entryDimensionStat.getFixedCount());
                statVO.addExistTotalCount(entryDimensionStat.getExistTotalCount());
                statVO.addExistSeriousCount(entryDimensionStat.getExistSeriousCount());
                statVO.addExcludedCount(entryDimensionStat.getExcludedCount());
                statVO.addToolNum();
            }
        }
    }

    /**
     * 统计lint类工具限定时间范围内的各种状态告警
     */
    @NotNull
    private Map<Long, DimensionStatVO> batchQueryLintDefectStat(Set<Long> taskIds, String tool, long startTime,
            long endTime, String type) {
        boolean isSecurityType = ComConstants.ToolType.SECURITY.name().equals(type);

        long begin = System.currentTimeMillis();
        List<DefectStatModel> newAddDefect =
                lintDefectDao.batchStatDefect(taskIds, tool, NEW_ADD, CREATE_TIME, startTime, endTime);
        List<DefectStatModel> fixedDefect =
                lintDefectDao.batchStatDefect(taskIds, tool, Lists.newArrayList(FIXED), FIXED_TIME, startTime, endTime);
        List<DefectStatModel> existDefect =
                lintDefectDao.batchQueryDefect(taskIds, tool, Lists.newArrayList(NEW_FLAG), CREATE_TIME, 1L, endTime);
        log.info("batchQueryLintDefectStat cost: {}", System.currentTimeMillis() - begin);

        Map<Long, Integer> taskDefectExcludeMap = null;
        if (isSecurityType) {
            List<DefectStatModel> excludeDefect =
                    lintDefectDao.batchStatDefect(taskIds, tool, EXCLUDE, EXCLUDE_TIME, startTime, endTime);
            taskDefectExcludeMap = excludeDefect.stream()
                    .collect(Collectors.toMap(DefectStatModel::getTaskId, DefectStatModel::getCount));
        }

        Map<Long, Integer> taskDefectNewAddMap =
                newAddDefect.stream().collect(Collectors.toMap(DefectStatModel::getTaskId, DefectStatModel::getCount));
        Map<Long, Integer> taskDefectFixedMap =
                fixedDefect.stream().collect(Collectors.toMap(DefectStatModel::getTaskId, DefectStatModel::getCount));

        Map<Long, Integer> taskDefectExistMap = Maps.newHashMap();
        Map<Long, Integer> taskDefectExistSeriousMap = Maps.newHashMap();
        Map<Long, List<DefectStatModel>> taskGroupMap =
                existDefect.stream().collect(Collectors.groupingBy(DefectStatModel::getTaskId));

        for (Map.Entry<Long, List<DefectStatModel>> entry : taskGroupMap.entrySet()) {
            Long taskId = entry.getKey();
            List<DefectStatModel> defectModels = entry.getValue();
            int existCount = 0;

            for (DefectStatModel model : defectModels) {
                int defectCount = model.getCount();
                if (defectCount > 0) {
                    if (ComConstants.SERIOUS == model.getSeverity()) {
                        taskDefectExistSeriousMap.put(taskId, defectCount);
                    }
                    existCount += defectCount;
                }
            }
            taskDefectExistMap.put(taskId, existCount);
        }

        Map<Long, DimensionStatVO> taskDimensionMap = Maps.newHashMap();
        for (Long taskId : taskIds) {
            DimensionStatVO statVO = new DimensionStatVO();
            statVO.addNewCount(taskDefectNewAddMap.get(taskId));
            statVO.addFixedCount(taskDefectFixedMap.get(taskId));
            statVO.addExistTotalCount(taskDefectExistMap.get(taskId));
            statVO.addExistSeriousCount(taskDefectExistSeriousMap.get(taskId));
            if (isSecurityType) {
                statVO.addExcludedCount(taskDefectExcludeMap.get(taskId));
            }
            taskDimensionMap.put(taskId, statVO);
        }

        return taskDimensionMap;
    }

    /**
     * 统计common类工具限定时间范围内的各种状态告警
     */
    @NotNull
    private Map<Long, DimensionStatVO> batchQueryCommonDefectStat(Set<Long> taskIds, String tool, long startTime,
            long endTime) {
        long begin = System.currentTimeMillis();
        List<DefectStatModel> newAddDefect =
                defectDao.batchStatDefectCountInStatus(taskIds, tool, NEW_ADD, CREATE_TIME, startTime, endTime);
        List<DefectStatModel> fixedDefect = defectDao
                .batchStatDefectCountInStatus(taskIds, tool, Lists.newArrayList(FIXED), FIXED_TIME, startTime, endTime);
        List<DefectModel> existDefect =
                defectDao.batchStatDefect(taskIds, tool, Lists.newArrayList(NEW_FLAG), CREATE_TIME, 1, endTime);
        List<DefectStatModel> excludeDefect =
                defectDao.batchStatDefectCountInStatus(taskIds, tool, EXCLUDE, EXCLUDE_TIME, startTime, endTime);
        log.info("batchQueryCommonDefectStat tool:{}, taskId size: {}, cost: {}", tool, taskIds.size(),
                System.currentTimeMillis() - begin);

        Map<Long, Integer> taskDefectNewAddMap =
                newAddDefect.stream().collect(Collectors.toMap(DefectStatModel::getTaskId, DefectStatModel::getCount));
        Map<Long, Integer> taskDefectFixedMap =
                fixedDefect.stream().collect(Collectors.toMap(DefectStatModel::getTaskId, DefectStatModel::getCount));
        Map<Long, Integer> taskDefectExcludeMap =
                excludeDefect.stream().collect(Collectors.toMap(DefectStatModel::getTaskId, DefectStatModel::getCount));

        Map<Long, Integer> taskDefectExistMap = Maps.newHashMap();
        Map<Long, Integer> taskDefectExistSeriousMap = Maps.newHashMap();
        Map<Long, List<DefectModel>> taskGroupMap =
                existDefect.stream().collect(Collectors.groupingBy(DefectModel::getTaskId));

        for (Map.Entry<Long, List<DefectModel>> entry : taskGroupMap.entrySet()) {
            Long taskId = entry.getKey();
            List<DefectModel> defectModels = entry.getValue();
            int existCount = 0;

            for (DefectModel model : defectModels) {
                int defectCount = model.getLineNumber();
                if (defectCount > 0) {
                    if (ComConstants.SERIOUS == model.getSeverity()) {
                        taskDefectExistSeriousMap.put(taskId, defectCount);
                    }
                    existCount += defectCount;
                }
            }
            taskDefectExistMap.put(taskId, existCount);
        }

        Map<Long, DimensionStatVO> taskDimensionMap = Maps.newHashMap();
        for (Long taskId : taskIds) {
            DimensionStatVO statVO = new DimensionStatVO();
            statVO.addNewCount(taskDefectNewAddMap.get(taskId));
            statVO.addFixedCount(taskDefectFixedMap.get(taskId));
            statVO.addExistTotalCount(taskDefectExistMap.get(taskId));
            statVO.addExistSeriousCount(taskDefectExistSeriousMap.get(taskId));
            statVO.addExcludedCount(taskDefectExcludeMap.get(taskId));
            taskDimensionMap.put(taskId, statVO);
        }
        return taskDimensionMap;
    }

    /**
     * 按维度统计数据并生成文件
     * @param reqVO     请求体
     * @param fileIndex 带路径的文件名
     */
    @Override
    public void exportDimensionToFile(@NotNull TaskToolInfoReqVO reqVO, @NotNull String fileIndex) {
        String[] fileIdxArr = fileIndex.split("/");
        String fileName = fileIdxArr[fileIdxArr.length - 1];

        int pageSize = 100;
        int pageNum = 1;

        int pageCount;
        ExcelWriter excelWriter = null;
        try {
            // 标记正在进行导出操作
            redisTemplate.opsForValue().set(RedisKeyConstants.DIMENSION_FILE_EXPORT_FLAG, Boolean.toString(false));

            // 最长有效期3小时
            redisTemplate.opsForValue()
                    .set(RedisKeyConstants.DIMENSION_FILE_FLAG + fileName, ComConstants.FileStatus.DOING.getCode(), 3,
                            TimeUnit.HOURS);
            redisTemplate.opsForValue().set(RedisKeyConstants.DIMENSION_FILE_NAME, fileName, 3, TimeUnit.HOURS);

            excelWriter = EasyExcel.write(fileIndex, TaskDefectSummary.class).registerWriteHandler(new TaskHyperlink())
                    .build();
            WriteSheet writeSheet = EasyExcel.writerSheet().build();

            // 先设置默认值,确定查询范围
            setDefectConditions(reqVO);
            // 日期范围时间戳
            long startTime = DateTimeUtils.getTimeStamp(reqVO.getStartTime());
            long endTime = DateTimeUtils.getTimeStamp(reqVO.getEndTime());
            reqVO.setStartTime(null);
            reqVO.setEndTime(null);
            log.info("exportDimensionToFile --> reqVO: {}", reqVO);

            do {
                log.info("exportDimensionToFile pageNum: {}", pageNum);
                Page<TaskDefectSummaryVO> taskDefectVOPage =
                        getTaskDefectSummaryVOPage(reqVO, pageNum, pageSize, "task_id", "ACE", startTime, endTime);

                List<TaskDefectSummaryVO> records = taskDefectVOPage.getRecords();
                pageCount = records.size();
                List<TaskDefectSummary> data = records.stream().map(item -> {
                    TaskDefectSummary summary = new TaskDefectSummary();
                    BeanUtils.copyProperties(item, summary);
                    if (item.getRdIndicatorsScore() > 0) {
                        BigDecimal bigDecimal = BigDecimal.valueOf(item.getRdIndicatorsScore());
                        summary.setRdIndicatorsScore(bigDecimal.setScale(2, BigDecimal.ROUND_HALF_UP).toString());

                        BigDecimal bigDecimalStar = BigDecimal.valueOf(item.getRdIndicatorsScore() / 20);
                        summary.setQualityStar(bigDecimalStar.setScale(1, BigDecimal.ROUND_HALF_UP).toString());
                    }
                    DimensionStatVO defectVo = item.getDefectVo();
                    if (defectVo.getToolNum() > 0) {
                        summary.setDefectExistTotalCount(defectVo.getExistTotalCount());
                        summary.setDefectExistSeriousCount(defectVo.getExistSeriousCount());
                        summary.setDefectNewCount(defectVo.getNewCount());
                        summary.setDefectFixedCount(defectVo.getFixedCount());
                        summary.setDefectExcludedCount(defectVo.getExcludedCount());
                        summary.setDefectToolNum(defectVo.getToolNum());
                    }
                    DimensionStatVO securityVo = item.getSecurityVo();
                    if (securityVo.getToolNum() > 0) {
                        summary.setSecurityExistTotalCount(securityVo.getExistTotalCount());
                        summary.setSecurityExistSeriousCount(securityVo.getExistSeriousCount());
                        summary.setSecurityNewCount(securityVo.getNewCount());
                        summary.setSecurityFixedCount(securityVo.getFixedCount());
                        summary.setSecurityExcludedCount(securityVo.getExcludedCount());
                        summary.setSecurityToolNum(securityVo.getToolNum());
                    }
                    DimensionStatVO standardVo = item.getStandardVo();
                    if (standardVo.getToolNum() > 0) {
                        summary.setStandardExistTotalCount(standardVo.getExistTotalCount());
                        BigDecimal bigDecimal = BigDecimal.valueOf(standardVo.getAverageThousandDefect());
                        summary.setStandardAverageThousandDefect(
                                bigDecimal.setScale(1, BigDecimal.ROUND_HALF_UP).toString());
                        summary.setStandardNewCount(standardVo.getNewCount());
                        summary.setStandardFixedCount(standardVo.getFixedCount());
                        summary.setStandardToolNum(standardVo.getToolNum());
                    }
                    DimensionStatVO ccnVo = item.getCcnVo();
                    if (ccnVo.getToolNum() > 0) {
                        summary.setCcnExistTotalCount(ccnVo.getExistTotalCount());
                        BigDecimal bigDecimal = BigDecimal.valueOf(ccnVo.getAverageThousandDefect());
                        summary.setCcnAverageThousandDefect(
                                bigDecimal.setScale(1, BigDecimal.ROUND_HALF_UP).toString());
                    }
                    DimensionStatVO dupcVo = item.getDupcVo();
                    if (dupcVo.getToolNum() > 0) {
                        summary.setDupExistTotalCount(dupcVo.getExistTotalCount());
                        BigDecimal bigDecimal = BigDecimal.valueOf(dupcVo.getDupRate());
                        summary.setDupRate(bigDecimal.setScale(2, BigDecimal.ROUND_HALF_UP).toString() + "%");
                    }

                    return summary;
                }).collect(Collectors.toList());

                excelWriter.write(data, writeSheet);

                pageNum++;
            } while (pageCount >= pageSize);
        } finally {
            if (excelWriter != null) {
                excelWriter.finish();
            }
        }
        // 恢复导出状态,改为空闲
        redisTemplate.opsForValue().set(RedisKeyConstants.DIMENSION_FILE_EXPORT_FLAG, Boolean.toString(true));

        // 标记文件已生成
        redisTemplate.opsForValue()
                .set(RedisKeyConstants.DIMENSION_FILE_FLAG + fileName, ComConstants.FileStatus.FINISH.getCode(), 3,
                        TimeUnit.HOURS);
        log.info("exportDimensionToFile finish.");
    }

}
