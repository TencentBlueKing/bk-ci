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

package com.tencent.bk.codecc.apiquery.service.impl;

import com.google.common.base.CaseFormat;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.apiquery.defect.model.TaskLogOverviewModel;
import com.tencent.bk.codecc.apiquery.service.MetaDataService;
import com.tencent.bk.codecc.apiquery.service.TaskLogOverviewService;
import com.tencent.bk.codecc.apiquery.service.ToolService;
import com.tencent.bk.codecc.apiquery.task.dao.TaskDao;
import com.tencent.bk.codecc.apiquery.task.dao.mongotemplate.AnalyzeCountStatDao;
import com.tencent.bk.codecc.apiquery.task.dao.mongotemplate.PlatformInfoDao;
import com.tencent.bk.codecc.apiquery.task.dao.mongotemplate.ToolDao;
import com.tencent.bk.codecc.apiquery.task.dao.mongotemplate.ToolElapseTimeDao;
import com.tencent.bk.codecc.apiquery.task.dao.mongotemplate.ToolStatisticDao;
import com.tencent.bk.codecc.apiquery.task.model.PlatformInfoModel;
import com.tencent.bk.codecc.apiquery.task.model.TaskInfoModel;
import com.tencent.bk.codecc.apiquery.task.model.ToolAnalyzeStatModel;
import com.tencent.bk.codecc.apiquery.task.model.ToolConfigInfoModel;
import com.tencent.bk.codecc.apiquery.task.model.ToolElapseTimeModel;
import com.tencent.bk.codecc.apiquery.task.model.ToolStatisticModel;
import com.tencent.bk.codecc.apiquery.task.model.ToolsRegisterStatisticsModel;
import com.tencent.bk.codecc.apiquery.utils.PageUtils;
import com.tencent.bk.codecc.apiquery.vo.TaskToolInfoReqVO;
import com.tencent.bk.codecc.apiquery.vo.ToolAnalyzeStatVO;
import com.tencent.bk.codecc.apiquery.vo.ToolAnalyzeVO;
import com.tencent.bk.codecc.apiquery.vo.ToolConfigPlatformVO;
import com.tencent.bk.codecc.apiquery.vo.ToolRegisterStatisticsVO;
import com.tencent.bk.codecc.apiquery.vo.ToolRegisterVO;
import com.tencent.bk.codecc.apiquery.vo.op.TaskAndToolStatChartVO;
import com.tencent.bk.codecc.apiquery.vo.op.ToolElapseTimeVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.Page;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.DefectStatType;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.constant.RedisKeyConstants;
import com.tencent.devops.common.service.ToolMetaCacheService;
import com.tencent.devops.common.util.DateTimeUtils;
import com.tencent.devops.common.util.List2StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import com.tencent.devops.common.util.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.tencent.devops.common.constant.ComConstants.DAY_THIRTY;
import static com.tencent.devops.common.util.DateTimeUtils.timestamp2StringDate;

/**
 * OP工具管理服务接口实现
 *
 * @version V1.0
 * @date 2020/4/26
 */

@Slf4j
@Service
public class ToolServiceImpl implements ToolService {
    @Autowired
    private ToolDao toolDao;

    @Autowired
    private AnalyzeCountStatDao analyzeCountStatDao;

    @Autowired
    private ToolStatisticDao toolStatisticDao;

    @Autowired
    private TaskDao taskDao;

    @Autowired
    private PlatformInfoDao platformInfoDao;

    @Autowired
    private RedisTemplate<String,String> redisTemplate;

    @Autowired
    private ToolMetaCacheService toolMetaCacheService;
    @Autowired
    private ToolElapseTimeDao toolElapseTimeDao;


    @Autowired
    private MetaDataService metaDataService;

    @Autowired
    private TaskLogOverviewService taskLogOverviewService;

    @Override
    public Page<ToolConfigPlatformVO> getPlatformInfoList(Long taskId, String toolName, String platformIp,
            Integer pageNum, Integer pageSize, String sortType) {
        // 排序分页(暂支持taskId排序)
        Pageable pageable = PageUtils.INSTANCE.convertPageSizeToPageable(pageNum, pageSize, "task_id", sortType);

        long totalCount = 0;
        int totalPage = 0;
        List<ToolConfigPlatformVO> infoList = Lists.newArrayList();

        Page<ToolConfigInfoModel> toolPage = toolDao.queryToolPlatformInfoPage(toolName, platformIp, taskId, pageable);
        if (toolPage != null) {
            totalCount = toolPage.getCount();
            totalPage = toolPage.getTotalPages();
            pageNum = toolPage.getPage();
            pageSize = toolPage.getPageSize();
            List<ToolConfigInfoModel> toolConfEntityList = toolPage.getRecords();

            if (CollectionUtils.isNotEmpty(toolConfEntityList)) {
                Set<Long> taskIdSet =
                        toolConfEntityList.stream().map(ToolConfigInfoModel::getTaskId).collect(Collectors.toSet());
                List<TaskInfoModel> taskInfoModelList = taskDao.findByTaskIdIn(taskIdSet);

                Map<Long, TaskInfoModel> taskInfoEntityMap = taskInfoModelList.stream()
                        .collect(Collectors.toMap(TaskInfoModel::getTaskId, Function.identity(), (k, v) -> v));

                toolConfEntityList.forEach(entity -> {
                    long entityTaskId = entity.getTaskId();
                    String tool = entity.getToolName();
                    String ip = entity.getPlatformIp();

                    ToolConfigPlatformVO configPlatformVO = new ToolConfigPlatformVO();
                    configPlatformVO.setTaskId(entityTaskId);
                    configPlatformVO.setToolName(tool);

                    // 设置对应的Platform信息
                    if (StringUtils.isBlank(ip)) {
                        ip = "";
                    }
                    configPlatformVO.setIp(ip);

                    TaskInfoModel taskInfo = taskInfoEntityMap.get(entityTaskId);
                    configPlatformVO.setNameCn(taskInfo.getNameCn());
                    configPlatformVO.setNameEn(taskInfo.getNameEn());
                    infoList.add(configPlatformVO);
                });
            }
        }
        return new Page<>(totalCount, pageNum, pageSize, totalPage, infoList);
    }


    @Override
    public ToolConfigPlatformVO getTaskPlatformDetail(Long taskId, String toolName) {
        if (taskId == null || taskId == 0 || StringUtils.isBlank(toolName)) {
            log.error("taskId or toolName is not allowed to be empty!");
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_NULL, new String[]{"taskId or toolName"}, null);
        }

        ToolConfigInfoModel toolConfigInfoModel = toolDao.findByTaskIdAndTool(taskId, toolName);
        if (toolConfigInfoModel == null) {
            log.error("findByTaskIdAndTool data is not found,task [{}] or tool [{}] is invalid!", taskId, toolName);
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{"taskId or toolName"}, null);
        }
        ToolConfigPlatformVO toolConfigPlatformVO = new ToolConfigPlatformVO();
        BeanUtils.copyProperties(toolConfigInfoModel, toolConfigPlatformVO);

        String platformIp = toolConfigInfoModel.getPlatformIp();
        String port = "";
//        String userName = "";
//        String passwd = "";
        if (StringUtils.isNotBlank(platformIp)) {
            List<PlatformInfoModel> platformInfoModelList = platformInfoDao.findByToolNameAndIp(toolName, platformIp);
            if (CollectionUtils.isNotEmpty(platformInfoModelList)) {
                PlatformInfoModel platformVO = platformInfoModelList.iterator().next();
                port = platformVO.getPort();
//                userName = platformVO.getUserName();
//                passwd = platformVO.getPasswd();
            }
        }
        toolConfigPlatformVO.setIp(platformIp);
        toolConfigPlatformVO.setPort(port);
//         toolConfigPlatformVO.setUserName(userName);
//         toolConfigPlatformVO.setPassword(passwd);

        TaskInfoModel taskInfoModel = taskDao.findTaskById(taskId);
        if (taskInfoModel != null) {
            toolConfigPlatformVO.setNameEn(taskInfoModel.getNameEn());
            toolConfigPlatformVO.setNameCn(taskInfoModel.getNameCn());
        }

        return toolConfigPlatformVO;
    }


    /**
     * 获取接入工具的任务ID列表
     *
     * @param taskIds      任务ID集合
     * @param toolName     工具名
     * @param followStatus 跟进状态
     * @param isNot        是否取反查询(跟进状态)
     * @return list
     */
    @Override
    public List<Long> findTaskIdByToolNames(List<Long> taskIds, String toolName, Integer followStatus, boolean isNot) {
        List<String> tools = Lists.newArrayList();
        if (StringUtils.isNotEmpty(toolName)) {
            tools.add(toolName);
        }
        return toolDao.findTaskIdByToolAndStatus(taskIds, tools, followStatus, isNot);
    }


    /**
     * 获取工具注册统计信息
     *
     * @param taskToolInfoReqVO
     * @return
     */
    public List<ToolRegisterStatisticsVO> getToolRegisterStatisticsList(TaskToolInfoReqVO taskToolInfoReqVO) {
        List<ToolRegisterStatisticsVO> toolRegisterStatisticsList = new ArrayList<>();

        // 默认包含屏蔽用户的任务
        if (taskToolInfoReqVO.getHasAdminTask() == null) {
            taskToolInfoReqVO.setHasAdminTask(1);
        } else {
            taskToolInfoReqVO.setExcludeUserList(metaDataService.queryExcludeUserList());
        }

        // 查询t_task_detail表
        List<Long> taskIds = taskDao.findByBgIdAndDeptId(taskToolInfoReqVO);
        // 将字符串时间格式转换成时间戳
        long[] startTimeAndEndTime =
                DateTimeUtils.getStartTimeAndEndTime(taskToolInfoReqVO.getStartTime(), taskToolInfoReqVO.getEndTime());
        long startTime = startTimeAndEndTime[0];
        long endTime = startTimeAndEndTime[1];

        // 根据taskIds,时间 查询统计添加次数(以tool_name和follow_status进行分组)
        List<ToolsRegisterStatisticsModel> toolsRegisterStatisticsModels =
                toolDao.getToolRegisterCount(taskIds, startTime, endTime);
        // key:toolName value:ToolsRegisterStatisticsModel 再次以tool_name分组
        Map<String, List<ToolsRegisterStatisticsModel>> collect = toolsRegisterStatisticsModels.stream()
                .collect(Collectors.groupingBy(ToolsRegisterStatisticsModel::getToolName));

        // 从redis中获取所有toolName
        String[] toolNames =
                redisTemplate.opsForValue().get(RedisKeyConstants.KEY_TOOL_ORDER).split(ComConstants.STRING_SPLIT);
        for (String toolName : toolNames) {
            ToolRegisterStatisticsVO toolRegisterStatisticsVO = new ToolRegisterStatisticsVO();
            // 获取toolDisplayName
            String toolDisplayName = toolMetaCacheService.getToolDisplayName(toolName);
            // 设置工具名
            toolRegisterStatisticsVO.setToolName(toolDisplayName);
            toolRegisterStatisticsVO.setToolKey(toolName);
            if (collect.get(toolName) != null) {
                for (ToolsRegisterStatisticsModel statisticsModel : collect.get(toolName)) {
                    if (ComConstants.FOLLOW_STATUS.ACCESSED.value() == statisticsModel.getFollowStatus()) {
                        // 设置已接入次数
                        toolRegisterStatisticsVO.setAccessCount(statisticsModel.getRegisterCount());
                    } else if (ComConstants.FOLLOW_STATUS.WITHDRAW.value() == statisticsModel.getFollowStatus()) {
                        // 设置已下架次数
                        toolRegisterStatisticsVO.setWithdrawCount(statisticsModel.getRegisterCount());
                    } else {
                        // 设置未跟进次数
                        toolRegisterStatisticsVO.setNotFollowCount(statisticsModel.getRegisterCount());
                    }
                }
            }
            // 设置添加次数
            int addCount = toolRegisterStatisticsVO.getAccessCount() + toolRegisterStatisticsVO.getWithdrawCount()
                    + toolRegisterStatisticsVO.getNotFollowCount();
            toolRegisterStatisticsVO.setAddCount(addCount);

            // 将toolsRegisterStatisticsVO添加进list中
            toolRegisterStatisticsList.add(toolRegisterStatisticsVO);
        }
        ToolRegisterStatisticsVO toolRegisterStatisticsSum = new ToolRegisterStatisticsVO();
        // 设置名字"合计"
        toolRegisterStatisticsSum.setToolName(ComConstants.SUM);
        // 添加次数求和
        int addCountSum = toolRegisterStatisticsList.stream().mapToInt(ToolRegisterStatisticsVO::getAddCount).sum();
        toolRegisterStatisticsSum.setAddCount(addCountSum);
        // 设置未跟进次数求和
        int notFollowCountSum =
                toolRegisterStatisticsList.stream().mapToInt(ToolRegisterStatisticsVO::getNotFollowCount).sum();
        toolRegisterStatisticsSum.setNotFollowCount(notFollowCountSum);
        // 设置已接入次数求和
        int accessCountSum =
                toolRegisterStatisticsList.stream().mapToInt(ToolRegisterStatisticsVO::getAccessCount).sum();
        toolRegisterStatisticsSum.setAccessCount(accessCountSum);
        // 设置已下架次数求和
        int withdrawCount =
                toolRegisterStatisticsList.stream().mapToInt(ToolRegisterStatisticsVO::getWithdrawCount).sum();
        toolRegisterStatisticsSum.setWithdrawCount(withdrawCount);
        toolRegisterStatisticsList.add(0, toolRegisterStatisticsSum);

        return toolRegisterStatisticsList;
    }

    /**
     * 获取工具执行统计数据
     */
    @Override
    public List<ToolAnalyzeStatVO> getToolAnalyzeStatList(TaskToolInfoReqVO reqVO) {
        log.info("op getToolAnalyzeStatList req: {}", reqVO);
        List<ToolAnalyzeStatVO> toolAnalyzeStatList = new ArrayList<>();
        String startTime = reqVO.getStartTime();
        String endTime = reqVO.getEndTime();
        List<String> dates = new ArrayList<>();
        // 获取时间段每一天的时间
        if (StringUtils.isNotEmpty(startTime) && StringUtils.isNotEmpty(endTime)) {
            dates = DateTimeUtils.getStartTimeBetweenEndTime(startTime, endTime);
        }
        Set<String> createFromReq = reqVO.getCreateFrom();
        // 根据来源、时间查询工具执行统计数据
        List<ToolAnalyzeStatModel> toolAnalyzeStatModels = analyzeCountStatDao.getToolAnalyzeStat(dates, createFromReq);
        Map<String, List<ToolAnalyzeStatModel>> collect;
        if (CollectionUtils.isNotEmpty(toolAnalyzeStatModels)) {
            collect = toolAnalyzeStatModels.stream().collect(Collectors.groupingBy(ToolAnalyzeStatModel::getToolName));
        } else {
            collect = Maps.newHashMap();
        }
        // 从redis中获取所有toolName
        String[] toolNames =
                redisTemplate.opsForValue().get(RedisKeyConstants.KEY_TOOL_ORDER).split(ComConstants.STRING_SPLIT);

        List<Long> excludeTaskIds = new ArrayList<>();
        if (reqVO.getHasAdminTask() == 0) {
            long startTimeMillis = System.currentTimeMillis();

            Set<String> createFrom = ComConstants.BsTaskCreateFrom.getByStatType(reqVO.getCreateFrom());
            List<String> excludeUserList = metaDataService.queryExcludeUserList();
            List<TaskInfoModel> taskInfoModels = taskDao.findTaskIdsByBgCreateFrom(createFrom, excludeUserList);
            excludeTaskIds = taskInfoModels.stream().map(TaskInfoModel::getTaskId).collect(Collectors.toList());

            log.info("getToolAnalyzeStatList, taskDao.findTaskIdsByBgCreateFrom(): [{}ms] ",
                    System.currentTimeMillis() - startTimeMillis);
            log.info("getToolAnalyzeStatList, metaDataService.queryExcludeUserList(): {} ", excludeUserList);
            log.info("excludeTaskIds: {} ", excludeTaskIds.size());
        }

        for (String toolName : toolNames) {
            ToolAnalyzeStatVO toolAnalyzeStatVO = new ToolAnalyzeStatVO();
            // 获取toolDisplayName
            String toolDisplayName = toolMetaCacheService.getToolDisplayName(toolName);
            // 设置工具名
            toolAnalyzeStatVO.setToolName(toolDisplayName);
            toolAnalyzeStatVO.setToolKey(toolName);
            List<ToolAnalyzeStatModel> toolAnalyzeStat = collect.get(toolName);
            if (toolAnalyzeStat != null) {
                int successCount = 0;
                int failCount = 0;
                for (ToolAnalyzeStatModel toolAnalyzeStatModel : toolAnalyzeStat) {
                    if (toolAnalyzeStatModel.getStatus() == 0) {
                        // 获取工具执行成功次数
                        List<Long> taskIds = toolAnalyzeStatModel.getTaskIdList();
                        List<Long> eliminateTaskIds = List2StrUtil.listRemoveAllAscension(taskIds, excludeTaskIds);
                        if (CollectionUtils.isNotEmpty(eliminateTaskIds)) {
                            int success = eliminateTaskIds.size();
                            successCount = successCount + success;
                        }
                    } else if (toolAnalyzeStatModel.getStatus() == 1) {
                        // 获取工具执行失败次数
                        List<Long> taskIds = toolAnalyzeStatModel.getTaskIdList();
                        List<Long> eliminateTaskIds = List2StrUtil.listRemoveAllAscension(taskIds, excludeTaskIds);
                        if (CollectionUtils.isNotEmpty(eliminateTaskIds)) {
                            int fail = eliminateTaskIds.size();
                            failCount = failCount + fail;
                        }
                    }
                }
                int totalCount = successCount + failCount;
                toolAnalyzeStatVO.setAnalyzeSuccCount(successCount);
                toolAnalyzeStatVO.setAnalyzeFailCount(failCount);
                toolAnalyzeStatVO.setAnalyzeTotalCount(totalCount);
            }
            toolAnalyzeStatList.add(toolAnalyzeStatVO);
        }
        return toolAnalyzeStatList;
    }

    /**
     * 获取工具注册明细信息
     */
    @Override
    public Page<ToolRegisterVO> getAllToolRegisterList(TaskToolInfoReqVO reqVO, Integer pageNum, Integer pageSize,
            String sortField, String sortType) {
        log.info("op getAllToolRegisterList req: {}", reqVO);

        // 默认包含屏蔽用户的任务
        if (reqVO.getHasAdminTask() == null) {
            reqVO.setHasAdminTask(1);
        } else {
            reqVO.setExcludeUserList(metaDataService.queryExcludeUserList());
        }

        // 获取taskId
        List<Long> taskIds  = taskDao.findByBgIdAndDeptId(reqVO);
        if (CollectionUtils.isEmpty(taskIds)) {
            return new Page<>(0, 0, 0, Lists.newArrayList());
        }
        reqVO.setTaskIds(taskIds);

        // 封装分页类查询工具配置信息
        String sortFieldInDb =
                CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, sortField == null ? "taskId" : sortField);
        Pageable pageable =
                PageUtils.INSTANCE.generaPageableUnlimitedPageSize(pageNum, pageSize, sortFieldInDb, sortType);
        Page<ToolConfigInfoModel> toolInfoPage = toolDao.findToolInfoPage(reqVO, pageable);
        List<ToolConfigInfoModel> toolConfigInfoModels = toolInfoPage.getRecords();
        if (CollectionUtils.isEmpty(toolConfigInfoModels)) {
            return new Page<>(0, 0, 0, Lists.newArrayList());
        }
        // 获取工具查到的任务信息
        Set<Long> taskIdSet =
                toolConfigInfoModels.stream().map(ToolConfigInfoModel::getTaskId).collect(Collectors.toSet());
        List<TaskInfoModel> taskInfoModelList = taskDao.findByTaskIdIn(taskIdSet);
        Map<Long, TaskInfoModel> taskInfoMap = taskInfoModelList.stream()
                .collect(Collectors.toMap(TaskInfoModel::getTaskId, Function.identity(), (k, v) -> v));

        // 组织架构信息
        Map<Object, Object> deptInfo = redisTemplate.opsForHash().entries(RedisKeyConstants.KEY_DEPT_INFOS);

        List<ToolRegisterVO> data = toolConfigInfoModels.stream().map(model -> {
            long taskId = model.getTaskId();
            TaskInfoModel taskInfoModel = taskInfoMap.get(taskId);
            ToolRegisterVO toolRegisterVO = new ToolRegisterVO();
            BeanUtils.copyProperties(model, toolRegisterVO);
            if (taskInfoModel != null) {
                toolRegisterVO.setNameEn(taskInfoModel.getNameEn());
                toolRegisterVO.setNameCn(taskInfoModel.getNameCn());
                toolRegisterVO.setTaskOwner(taskInfoModel.getTaskOwner());
                toolRegisterVO
                        .setBgName(ObjectUtils.toString(deptInfo.get(String.valueOf(taskInfoModel.getBgId())), ""));
                toolRegisterVO
                        .setDeptName(ObjectUtils.toString(deptInfo.get(String.valueOf(taskInfoModel.getDeptId())), ""));
            }
            return toolRegisterVO;
        }).collect(Collectors.toList());

        return new Page<>(toolInfoPage.getCount(), toolInfoPage.getPage(), toolInfoPage.getPageSize(),
                toolInfoPage.getTotalPages(), data);
    }

    /**
     * 获取工具和活跃工具折线图数据
     *
     * @param reqVO 工具和活跃工具请求体
     * @return list
     */
    @Override
    public List<TaskAndToolStatChartVO> toolAndActiveToolStatData(@NotNull TaskToolInfoReqVO reqVO) {
        String toolName = reqVO.getToolName();
        Set<String> createFromReq = reqVO.getCreateFrom();
        // 获取日期 默认显示30天
        List<String> dates =
                DateTimeUtils.getDatesByStartTimeAndEndTime(reqVO.getStartTime(), reqVO.getEndTime(), DAY_THIRTY);

        // 根据时间、来源、工具名 获取ToolStatisticModel集合
        List<ToolStatisticModel> toolStatList =
                toolStatisticDao.findToolCountByCreateFromAndTimeAndToolName(dates, createFromReq, toolName);
        Map<String, ToolStatisticModel> statisticModelMap;
        if (CollectionUtils.isNotEmpty(toolStatList)) {
            statisticModelMap = toolStatList.stream()
                    .collect(Collectors.toMap(ToolStatisticModel::getDate, Function.identity(), (k, v) -> v));
        } else {
            statisticModelMap = Maps.newHashMap();
        }

        List<TaskAndToolStatChartVO> toolAndActiveToolStatList = new ArrayList<>();
        dates.forEach(date -> {
            TaskAndToolStatChartVO taskAndToolStatChartVO = new TaskAndToolStatChartVO();
            taskAndToolStatChartVO.setDate(date);
            ToolStatisticModel model = statisticModelMap.get(date);
            if (model != null) {
                taskAndToolStatChartVO.setCount(model.getToolCount());
                taskAndToolStatChartVO.setActiveCount(model.getActiveCount());
            }
            toolAndActiveToolStatList.add(taskAndToolStatChartVO);
        });

        // 获取当天数据
        String currentDate = DateTimeUtils.getDateByDiff(0);
        if (StringUtils.isEmpty(reqVO.getEndTime()) || currentDate.equals(reqVO.getEndTime())) {
            // 按条件获取任务ID
            TaskToolInfoReqVO reqTaskIdVO = new TaskToolInfoReqVO();
            // 有效任务
            reqTaskIdVO.setStatus(ComConstants.Status.ENABLE.value());

            List<String> redisDataFrom;
            Set<String> createFromSet;
            if (createFromReq == null || createFromReq.size() == 2) {
                createFromSet = Sets.newHashSet(ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value(),
                        ComConstants.BsTaskCreateFrom.BS_PIPELINE.value(),
                        ComConstants.BsTaskCreateFrom.BS_CODECC.value());
                redisDataFrom = Lists.newArrayList(DefectStatType.GONGFENG_SCAN.value(),
                        DefectStatType.USER.value());
            } else {
                if (createFromReq.contains(DefectStatType.GONGFENG_SCAN.value())) {
                    createFromSet = Sets.newHashSet(ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value());
                    redisDataFrom = Lists.newArrayList(DefectStatType.GONGFENG_SCAN.value());
                } else {
                    createFromSet = Sets.newHashSet(ComConstants.BsTaskCreateFrom.BS_PIPELINE.value(),
                            ComConstants.BsTaskCreateFrom.BS_CODECC.value());
                    redisDataFrom = Lists.newArrayList(DefectStatType.USER.value());
                }
            }

            Long toolCount = 0L;
            reqTaskIdVO.setCreateFrom(createFromSet);
            reqTaskIdVO.setHasAdminTask(1);
            List<Long> taskIdList = taskDao.findByBgIdAndDeptId(reqTaskIdVO);
            if (CollectionUtils.isNotEmpty(taskIdList)) {
                // 统计有效状态的工具数
                List<Integer> effectiveStatus = ComConstants.FOLLOW_STATUS.getEffectiveStatus();
                toolCount = toolDao.findToolCountNow(taskIdList, toolName, effectiveStatus);
            }

            // 获取活跃工具数
            int activeToolCount = redisDataFrom.stream().map(dataFrom -> String
                    .format("%s%s:%s", RedisKeyConstants.PREFIX_ACTIVE_TOOL, currentDate, dataFrom))
                    .map(redisKey -> (String) redisTemplate.opsForHash().get(redisKey, toolName))
                    .filter(StringUtils::isNotEmpty).mapToInt(Integer::parseInt).sum();

            TaskAndToolStatChartVO endNodeChartVO = toolAndActiveToolStatList.get(toolAndActiveToolStatList.size() - 1);
            endNodeChartVO.setDate(currentDate);
            endNodeChartVO.setCount(toolCount.intValue());
            endNodeChartVO.setActiveCount(activeToolCount);
        }

        return toolAndActiveToolStatList;
    }

    /**
     * 获取工具分析次数折线图数据
     *
     * @param reqVO 工具和活跃工具请求体
     * @return list
     */
    @Override
    public Map<String, List<TaskAndToolStatChartVO>> toolAnalyzeCountData(TaskToolInfoReqVO reqVO) {
        List<String> toolNames = reqVO.getToolNames();
        String endTime = reqVO.getEndTime();
        Set<String> createFromReq = reqVO.getCreateFrom();
        // 获取日期 默认显示30天
        List<String> dates =
                DateTimeUtils.getDatesByStartTimeAndEndTime(reqVO.getStartTime(), endTime, DAY_THIRTY);

        Map<String, List<TaskAndToolStatChartVO>> toolAnalyzeStatMap = new HashMap<>();
        for (String toolName : toolNames) {
            // 根据时间、来源、工具名 获取ToolStatisticModel集合
            List<ToolStatisticModel> toolStatList =
                    toolStatisticDao.findToolAnalyzeCount(dates, createFromReq, toolName);
            Map<String, ToolStatisticModel> statisticModelMap;
            if (CollectionUtils.isNotEmpty(toolStatList)) {
                statisticModelMap = toolStatList.stream()
                        .collect(Collectors.toMap(ToolStatisticModel::getDate, Function.identity(), (k, v) -> v));
            } else {
                statisticModelMap = Maps.newHashMap();
            }
            List<TaskAndToolStatChartVO> toolAnalyzeStatList = new ArrayList<>();
            dates.forEach(date -> {
                TaskAndToolStatChartVO taskAndToolStatChartVO = new TaskAndToolStatChartVO();
                taskAndToolStatChartVO.setDate(date);
                ToolStatisticModel model = statisticModelMap.get(date);
                if (model != null) {
                    taskAndToolStatChartVO.setAnalyzeCount(model.getAnalyzeCount());
                }
                toolAnalyzeStatList.add(taskAndToolStatChartVO);
            });
            toolAnalyzeStatMap.put(toolName,toolAnalyzeStatList);
        }
        // 获取当天数据
        String currentDate = DateTimeUtils.getDateByDiff(0);
        if (StringUtils.isEmpty(endTime) || currentDate.equals(endTime)) {
            List<String> redisDataFrom;
            if (createFromReq == null || createFromReq.size() == 2) {
                redisDataFrom = Lists.newArrayList(DefectStatType.GONGFENG_SCAN.value(),
                        DefectStatType.USER.value());
            } else {
                if (createFromReq.contains(DefectStatType.GONGFENG_SCAN.value())) {
                    redisDataFrom = Lists.newArrayList(DefectStatType.GONGFENG_SCAN.value());
                } else {
                    redisDataFrom = Lists.newArrayList(DefectStatType.USER.value());
                }
            }
            for (Map.Entry<String, List<TaskAndToolStatChartVO>> entry : toolAnalyzeStatMap.entrySet()) {
                String toolName = entry.getKey();
                List<TaskAndToolStatChartVO> value = entry.getValue();
                List<String> redisKeySuffix = redisDataFrom.stream().map(dataFrom ->
                        String.format("%s:%s:%s", currentDate, dataFrom, toolName)).collect(Collectors.toList());
                // 当天分析次数=成功+失败
                List<Object> executeResult = redisTemplate.executePipelined((RedisCallback<Long>) conn -> {
                    for (String keySuffix : redisKeySuffix) {
                        conn.lLen(String.format("%s%s", RedisKeyConstants.PREFIX_ANALYZE_SUCC_COUNT, keySuffix)
                                .getBytes());
                        conn.lLen(String.format("%s%s", RedisKeyConstants.PREFIX_ANALYZE_FAIL_COUNT, keySuffix)
                                .getBytes());
                    }
                    return null;
                });
                long analyzeCount = executeResult.stream().filter(Objects::nonNull).mapToLong(obj -> (Long) obj).sum();
                TaskAndToolStatChartVO endNodeChartVO = value.get(value.size() - 1);
                endNodeChartVO.setAnalyzeCount((int) analyzeCount);
            }
        }
        return toolAnalyzeStatMap;
    }


    /**
     * 获取工具执行明细信息
     *
     * @param reqVO     工具执行明细请求体
     * @param pageNum   页码
     * @param pageSize  每页数量
     * @param sortField 排序字段
     * @param sortType  排序类型
     * @return page
     */
    @Override
    public Page<ToolAnalyzeVO> getToolAnalyzeInfoList(TaskToolInfoReqVO reqVO, Integer pageNum, Integer pageSize,
            String sortField, String sortType) {
        log.info("op getToolAnalyzeInfoList req: {}", reqVO);
        Set<String> createFrom = reqVO.getCreateFrom();
        String startTime = reqVO.getStartTime();
        String endTime = reqVO.getEndTime();

        List<Long> excludeTaskIds = new ArrayList<>();
        if (reqVO.getHasAdminTask() == 0) {
            long startTimeMillis = System.currentTimeMillis();

            Set<String> createFromRep = ComConstants.BsTaskCreateFrom.getByStatType(reqVO.getCreateFrom());
            List<String> excludeUserList = metaDataService.queryExcludeUserList();
            List<TaskInfoModel> taskInfoModels = taskDao.findTaskIdsByBgCreateFrom(createFromRep, excludeUserList);
            excludeTaskIds = taskInfoModels.stream().map(TaskInfoModel::getTaskId).collect(Collectors.toList());

            log.info("getToolAnalyzeInfoList, taskDao.findTaskIdsByBgCreateFrom(): [{}ms] ",
                    System.currentTimeMillis() - startTimeMillis);

            log.info("getToolAnalyzeInfoList, metaDataService.queryExcludeUserList(): {} ", excludeUserList);
            log.info("excludeTaskIds: {} ", excludeTaskIds.size());
        }

        long toolAnalyzeTaskIdsStartTime = System.currentTimeMillis();
        List<ToolAnalyzeStatModel> toolAnalyzeTaskIds =
                analyzeCountStatDao.getToolAnalyzeTaskIds(reqVO.getToolName(), createFrom, startTime, endTime);
        log.info("getToolAnalyzeInfoList: analyzeCountStatDao.getToolAnalyzeTaskIds(), time consuming: [{}]",
                System.currentTimeMillis() - toolAnalyzeTaskIdsStartTime);

        Map<String, List<ToolAnalyzeStatModel>> collect =
                toolAnalyzeTaskIds.stream().collect(Collectors.groupingBy(ToolAnalyzeStatModel::getToolName));

        Set<String> toolNames = collect.keySet();

        List<ToolAnalyzeVO> toolAnalyzeVOList = new ArrayList<>();

        for (String toolName : toolNames) {
            List<Long> taskIdSuccessList = new ArrayList<>();
            List<Long> taskIdFailList = new ArrayList<>();
            List<ToolAnalyzeStatModel> toolAnalyzeStatModels = collect.get(toolName);
            for (ToolAnalyzeStatModel toolAnalyzeStatModel : toolAnalyzeStatModels) {
                if (toolAnalyzeStatModel.getStatus() == 0) {
                    taskIdSuccessList.addAll(toolAnalyzeStatModel.getTaskIdList());
                } else {
                    taskIdFailList.addAll(toolAnalyzeStatModel.getTaskIdList());
                }
            }
            // 筛除管理员的task_id
            List<Long> eliminateTaskIdSuccessList =
                    List2StrUtil.listRemoveAllAscension(taskIdSuccessList, excludeTaskIds);
            List<Long> eliminateTaskIdFailList = List2StrUtil.listRemoveAllAscension(taskIdFailList, excludeTaskIds);

            Set<Long> taskIdList = new HashSet<>();
            taskIdList.addAll(eliminateTaskIdSuccessList);
            taskIdList.addAll(eliminateTaskIdFailList);

            long taskInfoModelListStartTime = System.currentTimeMillis();
            List<TaskInfoModel> taskInfoModelList = taskDao.findByTaskIds(taskIdList);
            Map<Long, TaskInfoModel> taskInfoModelMap = taskInfoModelList.stream()
                    .collect(Collectors.toMap(TaskInfoModel::getTaskId, Function.identity(), (k, v) -> v));
            log.info("getToolAnalyzeInfoList: taskDao.findByTaskIds(), time consuming: [{}]",
                    System.currentTimeMillis() - taskInfoModelListStartTime);

            List<ToolConfigInfoModel> toolConfigInfoModelList = toolDao.findByTaskIdsAndToolName(taskIdList, toolName);
            Map<Long, ToolConfigInfoModel> toolConfigInfoModelMap = toolConfigInfoModelList.stream()
                    .collect(Collectors.toMap(ToolConfigInfoModel::getTaskId, Function.identity(), (k, v) -> v));

            List<TaskLogOverviewModel> latestList = taskLogOverviewService.findLatestAnalyzeStatus(taskIdList, null);
            Map<Long, TaskLogOverviewModel> taskLogOverviewMap = latestList.stream()
                    .collect(Collectors.toMap(TaskLogOverviewModel::getTaskId, Function.identity(), (k, v) -> v));

            // 组织架构信息
            Map<Object, Object> deptInfo = redisTemplate.opsForHash().entries(RedisKeyConstants.KEY_DEPT_INFOS);

            for (Long taskId : taskIdList) {
                ToolAnalyzeVO toolAnalyzeVO = new ToolAnalyzeVO();
                TaskInfoModel taskInfoModel = taskInfoModelMap.get(taskId);
                BeanUtils.copyProperties(taskInfoModel, toolAnalyzeVO);

                String toolDisplayName = toolMetaCacheService.getToolDisplayName(toolName);
                // 设置工具名
                toolAnalyzeVO.setToolName(toolDisplayName);
                toolAnalyzeVO.setToolKey(toolName);

                int analyzeSuccCount = 0;
                int analyzeFailCount = 0;
                for (Long aLong : eliminateTaskIdSuccessList) {
                    if (aLong.equals(taskId)) {
                        analyzeSuccCount++;
                    }
                }
                for (Long aLong : eliminateTaskIdFailList) {
                    if (aLong.equals(taskId)) {
                        analyzeFailCount++;
                    }
                }
                toolAnalyzeVO.setAnalyzeSuccCount(analyzeSuccCount);
                toolAnalyzeVO.setAnalyzeFailCount(analyzeFailCount);
                toolAnalyzeVO.setAnalyzeTotalCount(analyzeSuccCount + analyzeFailCount);
                toolAnalyzeVO
                        .setBgName(ObjectUtils.toString(deptInfo.get(String.valueOf(taskInfoModel.getBgId())), ""));
                toolAnalyzeVO
                        .setDeptName(ObjectUtils.toString(deptInfo.get(String.valueOf(taskInfoModel.getDeptId())), ""));
                ToolConfigInfoModel toolConfigInfoModel = toolConfigInfoModelMap.get(taskId);
                toolAnalyzeVO.setCreateTime(timestamp2StringDate(toolConfigInfoModel.getCreatedDate()));
                toolAnalyzeVO.setFollowStatus(toolConfigInfoModel.getFollowStatus());
                TaskLogOverviewModel logOverviewModel = taskLogOverviewMap.get(taskInfoModel.getTaskId());
                toolAnalyzeVO.setAnalyzeDate(logOverviewModel == null ? "" :
                        DateTimeUtils.second2DateString(logOverviewModel.getStartTime())
                                + ComConstants.ScanStatus.convertScanStatus(logOverviewModel.getStatus()));
                toolAnalyzeVOList.add(toolAnalyzeVO);
            }
        }

        // 排序
        toolAnalyzeVOList.sort(Comparator.comparingLong(ToolAnalyzeVO::getAnalyzeTotalCount).reversed());
        // 分页
        int total = toolAnalyzeVOList.size();
        pageNum = pageNum == null || pageNum - 1 < 0 ? 0 : pageNum - 1;
        int totalPageNum = 0;
        if (total > 0) {
            totalPageNum = total / pageSize + 1;
        }
        int subListBeginIdx = pageNum * pageSize;
        int subListEndIdx = subListBeginIdx + pageSize;
        if (subListBeginIdx > total) {
            subListBeginIdx = 0;
        }
        List<ToolAnalyzeVO> toolAnalyzeVOPageList =
                toolAnalyzeVOList.subList(subListBeginIdx, Math.min(subListEndIdx, total));

        return new Page<>(total, pageNum + 1, pageSize, totalPageNum, toolAnalyzeVOPageList);
    }

    /**
     * 按条件获取工具分析耗时趋势图、失败次数统计图
     * @param reqVO 请求体
     * @return tool multiple
     */
    @Override
    public Map<String, List<ToolElapseTimeVO>> queryAnalyzeElapseTimeChart(TaskToolInfoReqVO reqVO) {
        String endTime = reqVO.getEndTime();
        // 工具多选
        List<String> toolNames = reqVO.getToolNames();
        // 数据来源多选(默认user)
        Set<String> createFrom = reqVO.getCreateFrom();
        String createFromReq;
        List<String> statType = Lists.newArrayList(DefectStatType.USER.value(), DefectStatType.GONGFENG_SCAN.value());
        if (CollectionUtils.isNotEmpty(createFrom)) {
            if (statType.containsAll(createFrom)) {
                createFromReq = DefectStatType.ALL.value();
            } else {
                createFromReq = createFrom.iterator().next();
            }
        } else {
            createFrom = Sets.newHashSet(DefectStatType.USER.value());
            createFromReq = DefectStatType.USER.value();
        }
        // 默认非超快增量
        String scanStatTypeReq = StringUtils.isEmpty(reqVO.getScanStatType())
                ? ComConstants.ScanStatType.NOT_FAST_INCREMENT.getValue() : reqVO.getScanStatType();
        // 获取日期 默认显示30天
        List<String> dates = DateTimeUtils.getDatesByStartTimeAndEndTime(reqVO.getStartTime(), endTime, DAY_THIRTY);

        // 查询所有条件的数据
        List<ToolElapseTimeModel> elapseTimeList =
                toolElapseTimeDao.findByConditions(dates, createFrom, scanStatTypeReq, toolNames);
        if (CollectionUtils.isEmpty(elapseTimeList)) {
            elapseTimeList = Lists.newArrayList();
        }
        Map<String, List<ToolElapseTimeModel>> toolElapseTimeMap =
                elapseTimeList.stream().collect(Collectors.groupingBy(ToolElapseTimeModel::getToolName));

        // 按工具、日期遍历
        Map<String, List<ToolElapseTimeVO>> toolsElapseTimeChartMap = Maps.newHashMap();
        for (String tool : toolNames) {
            List<ToolElapseTimeVO> toolElapseTimeList = Lists.newArrayList();
            List<ToolElapseTimeModel> toolElapseTimeModels = toolElapseTimeMap.get(tool);
            if (toolElapseTimeModels == null) {
                toolElapseTimeModels = Lists.newArrayList();
            }
            Map<String, ToolElapseTimeModel> elapseTimeModelMap = toolElapseTimeModels.stream()
                    .collect(Collectors.toMap(ToolElapseTimeModel::getDate, Function.identity(), (k, v) -> v));

            dates.forEach(date -> {
                ToolElapseTimeVO toolElapseTimeVO = new ToolElapseTimeVO();
                toolElapseTimeVO.setDate(date);
                toolElapseTimeVO.setScanStatType(scanStatTypeReq);
                toolElapseTimeVO.setDataFrom(createFromReq);
                ToolElapseTimeModel elapseTimeModel = elapseTimeModelMap.get(date);
                if (elapseTimeModel != null) {
                    toolElapseTimeVO.setTotalElapseTime(elapseTimeModel.getTotalElapseTime());
                    toolElapseTimeVO.setSuccAnalyzeCount(elapseTimeModel.getSuccAnalyzeCount());
                    toolElapseTimeVO.setFailAnalyzeCount(elapseTimeModel.getFailAnalyzeCount());
                }

                toolElapseTimeList.add(toolElapseTimeVO);
            });

            toolsElapseTimeChartMap.put(tool, toolElapseTimeList);
        }

        // 获取当天数据
        String currentDate = DateTimeUtils.getDateByDiff(0);
        if (StringUtils.isEmpty(endTime) || currentDate.equals(endTime)) {
            for (Map.Entry<String, List<ToolElapseTimeVO>> entry : toolsElapseTimeChartMap.entrySet()) {
                String toolKey = entry.getKey();
                List<ToolElapseTimeVO> timeVOList = entry.getValue();
                ToolElapseTimeVO timeVO = timeVOList.get(timeVOList.size() - 1);
                // 组装Redis key后缀
                List<String> redisKeySuffix = createFrom.stream()
                        .map(dataFrom -> String.format("%s:%s:%s", currentDate, dataFrom, scanStatTypeReq))
                        .collect(Collectors.toList());
                // 查分析成功次数
                List<Object> resultsSucc = redisTemplate.executePipelined((RedisCallback<Long>) conn -> {
                    for (String keySuffix : redisKeySuffix) {
                        conn.hGet(
                                String.format("%s%s", RedisKeyConstants.PREFIX_ANALYZE_SUCC_TOOL, keySuffix).getBytes(),
                                toolKey.getBytes());
                    }
                    return null;
                });
                long succCount = getTotalCountByResult(resultsSucc);
                timeVO.setSuccAnalyzeCount(succCount);

                // 查询失败次数
                List<Object> resultsFail = redisTemplate.executePipelined((RedisCallback<Long>) conn -> {
                    for (String keySuffix : redisKeySuffix) {
                        conn.hGet(
                                String.format("%s%s", RedisKeyConstants.PREFIX_ANALYZE_FAIL_TOOL, keySuffix).getBytes(),
                                toolKey.getBytes());
                    }
                    return null;
                });
                long failCount = getTotalCountByResult(resultsFail);
                timeVO.setFailAnalyzeCount(failCount);

                // 查询成功总耗时
                List<Object> resultsElapseTime = redisTemplate.executePipelined((RedisCallback<Long>) conn -> {
                    for (String keySuffix : redisKeySuffix) {
                        conn.hGet(String.format("%s%s", RedisKeyConstants.PREFIX_ANALYZE_SUCC_ELAPSE_TIME, keySuffix)
                                .getBytes(), toolKey.getBytes());
                    }
                    return null;
                });
                long totalElapseTime = getTotalCountByResult(resultsElapseTime);
                timeVO.setTotalElapseTime(totalElapseTime);
            }
        }

        return toolsElapseTimeChartMap;
    }

    /**
     * 统计Redis结果集
     *
     * @param objectList 结果集
     * @return 总数
     */
    private long getTotalCountByResult(List<Object> objectList) {
        long count = 0;
        if (CollectionUtils.isNotEmpty(objectList)) {
            for (Object countObj : objectList) {
                if (null != countObj) {
                    count += Long.parseLong(countObj.toString());
                }
            }
        }
        return count;
    }

}
