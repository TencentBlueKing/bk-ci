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
import com.tencent.bk.codecc.apiquery.defect.dao.mongotemplate.FirstAnalyzeSuccessDao;
import com.tencent.bk.codecc.apiquery.defect.model.CLOCStatisticModel;
import com.tencent.bk.codecc.apiquery.defect.model.CodeRepoModel;
import com.tencent.bk.codecc.apiquery.defect.model.DefectStatModel;
import com.tencent.bk.codecc.apiquery.defect.model.FirstAnalysisSuccessModel;
import com.tencent.bk.codecc.apiquery.defect.model.TaskLogOverviewModel;
import com.tencent.bk.codecc.apiquery.service.CodeRepoFromAnalyzeLogService;
import com.tencent.bk.codecc.apiquery.service.CodeRepoService;
import com.tencent.bk.codecc.apiquery.service.ICLOCQueryCodeLineService;
import com.tencent.bk.codecc.apiquery.service.MetaDataService;
import com.tencent.bk.codecc.apiquery.service.TaskLogOverviewService;
import com.tencent.bk.codecc.apiquery.service.TaskService;
import com.tencent.bk.codecc.apiquery.task.dao.TaskDao;
import com.tencent.bk.codecc.apiquery.task.dao.TaskStatisticDao;
import com.tencent.bk.codecc.apiquery.task.dao.mongotemplate.ToolDao;
import com.tencent.bk.codecc.apiquery.task.dao.mongotemplate.UserLogInfoDao;
import com.tencent.bk.codecc.apiquery.task.dao.mongotemplate.UserLogInfoStatDao;
import com.tencent.bk.codecc.apiquery.task.model.CodeRepoFromAnalyzeLogModel;
import com.tencent.bk.codecc.apiquery.task.model.TaskInfoModel;
import com.tencent.bk.codecc.apiquery.task.model.TaskStatisticModel;
import com.tencent.bk.codecc.apiquery.task.model.UserLogInfoModel;
import com.tencent.bk.codecc.apiquery.task.model.UserLogInfoStatModel;
import com.tencent.bk.codecc.apiquery.utils.ConvertUtil;
import com.tencent.bk.codecc.apiquery.utils.PageUtils;
import com.tencent.bk.codecc.apiquery.vo.DeptInfoVO;
import com.tencent.bk.codecc.apiquery.vo.TaskInfoExtVO;
import com.tencent.bk.codecc.apiquery.vo.TaskToolInfoReqVO;
import com.tencent.bk.codecc.apiquery.vo.op.ActiveTaskStatisticsVO;
import com.tencent.bk.codecc.apiquery.vo.op.TaskAndToolStatChartVO;
import com.tencent.bk.codecc.apiquery.vo.op.TaskCodeLineStatVO;
import com.tencent.bk.codecc.apiquery.vo.report.UserLogInfoChartVO;
import com.tencent.bk.codecc.task.vo.MetadataVO;
import com.tencent.devops.common.api.UserLogInfoStatVO;
import com.tencent.devops.common.api.checkerset.CheckerSetVO;
import com.tencent.devops.common.api.pojo.Page;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.BsTaskCreateFrom;
import com.tencent.devops.common.constant.RedisKeyConstants;
import com.tencent.devops.common.util.DateTimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import com.tencent.devops.common.util.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.tencent.devops.common.constant.ComConstants.DAY_FOURTEEN;
import static com.tencent.devops.common.constant.ComConstants.DAY_THIRTY;

/**
 * 任务管理服务接口
 *
 * @version V2.0
 * @date 2020/5/12
 */
@Slf4j
@Service
public class OpTaskServiceImpl implements TaskService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private TaskDao taskDao;

    @Autowired
    private TaskStatisticDao taskStatisticDao;

    @Autowired
    private ToolDao toolDao;

    @Autowired
    private UserLogInfoDao userLogInfoDao;

    @Autowired
    private UserLogInfoStatDao userLogInfoStatDao;

    @Autowired
    private CheckerSetBizServiceImpl checkerSetBizService;

    @Autowired
    private MetaDataService metaDataService;

    @Autowired
    private FirstAnalyzeSuccessDao firstAnalyzeSuccessDao;

    @Autowired
    private TaskLogOverviewService taskLogOverviewService;

    @Autowired
    private CodeRepoFromAnalyzeLogService codeRepoFromAnalyzeLogService;

    @Autowired
    private ICLOCQueryCodeLineService iclocQueryCodeLineService;

    @Autowired
    private CodeRepoService codeRepoService;

    @Override
    public Page<TaskInfoExtVO> getOverAllTaskList(TaskToolInfoReqVO reqVO, Integer pageNum, Integer pageSize,
            String sortField, String sortType) {
        log.info("op getOverAllTaskList req: {}", reqVO);

        // TODO 待优化
        String toolName = reqVO.getToolName();
        if (StringUtils.isNotEmpty(toolName)) {
            // 获取非下架的任务ID
            List<Long> taskIdByToolAndStatus = toolDao.findTaskIdByToolAndStatus(null, Lists.newArrayList(toolName),
                    ComConstants.FOLLOW_STATUS.WITHDRAW.value(), true);
            reqVO.setTaskIds(taskIdByToolAndStatus);
        }
        // 默认包含屏蔽用户的任务
        if (reqVO.getHasAdminTask() == null) {
            reqVO.setHasAdminTask(1);
        } else {
            reqVO.setExcludeUserList(metaDataService.queryExcludeUserList());
        }

        // 为支持全量导出任务列表
        Pageable pageable = PageUtils.INSTANCE.generaPageableUnlimitedPageSize(pageNum, pageSize, sortField, sortType);
        Page<TaskInfoModel> taskPage = taskDao.findTaskInfoPage(reqVO, pageable);

        List<TaskInfoModel> taskInfoModels = taskPage.getRecords();
        List<TaskInfoExtVO> data = Lists.newArrayList();
        if (CollectionUtils.isEmpty(taskInfoModels)) {
            return new Page<>(0, 0, 0, data);
        }

        // 代码语言转换
        List<MetadataVO> metadataVoList = metaDataService.getCodeLangMetadataList();
        // 组织架构信息
        Map<Object, Object> deptInfo = redisTemplate.opsForHash().entries(RedisKeyConstants.KEY_DEPT_INFOS);

        // 等长列表切割维持查询稳定
        List<List<TaskInfoModel>> pageTaskLists = PageUtils.INSTANCE.averageAssignFixLength(taskInfoModels, 1000);
        pageTaskLists.forEach(taskList -> {
            Set<Long> taskIdSet = taskList.stream().map(TaskInfoModel::getTaskId).collect(Collectors.toSet());

            // 获取指定任务的工具配置列表
            List<DefectStatModel> toolStatModels =
                    toolDao.countToolsByTaskIds(taskIdSet, ComConstants.FOLLOW_STATUS.WITHDRAW.value());
            Map<Long, Integer> taskToolCountMap = toolStatModels.stream()
                    .collect(Collectors.toMap(DefectStatModel::getTaskId, DefectStatModel::getCount));

            // 获取任务关联的规则集
            Map<Long, List<CheckerSetVO>> taskCheckerSetMap = checkerSetBizService.getCheckerSetByTaskIdSet(taskIdSet);

            long latestListStartTime = System.currentTimeMillis();
            // 获取最近分析状态
            List<TaskLogOverviewModel> latestList = taskLogOverviewService.findLatestAnalyzeStatus(taskIdSet, null);
            Map<Long, TaskLogOverviewModel> taskLogOverviewMap = latestList.stream()
                    .collect(Collectors.toMap(TaskLogOverviewModel::getTaskId, Function.identity(), (k, v) -> v));
            log.info("getOverAllTaskList: taskLogOverviewService.findLatestAnalyzeStatus(), time consuming: [{}]",
                    System.currentTimeMillis() - latestListStartTime);

            // 获取分析次数
            List<TaskLogOverviewModel> analyzeCount =
                    taskLogOverviewService.queryAnalyzeCount(taskIdSet, null, null, null);
            Map<Long, TaskLogOverviewModel> analyzeCountMap = analyzeCount.stream()
                    .collect(Collectors.toMap(TaskLogOverviewModel::getTaskId, Function.identity(), (k, v) -> v));

            taskList.forEach(taskInfoModel -> {
                TaskInfoExtVO taskInfoExtVO = new TaskInfoExtVO();
                BeanUtils.copyProperties(taskInfoModel, taskInfoExtVO);
                taskInfoExtVO
                        .setBgName(ObjectUtils.toString(deptInfo.get(String.valueOf(taskInfoModel.getBgId())), ""));
                taskInfoExtVO
                        .setDeptName(ObjectUtils.toString(deptInfo.get(String.valueOf(taskInfoModel.getDeptId())), ""));
                taskInfoExtVO.setCenterName(
                        ObjectUtils.toString(deptInfo.get(String.valueOf(taskInfoModel.getCenterId())), ""));
                taskInfoExtVO.setCodeLangStr(ConvertUtil.convertCodeLang(taskInfoModel.getCodeLang(), metadataVoList));

                // 最近分析状态
                TaskLogOverviewModel logOverviewModel = taskLogOverviewMap.get(taskInfoModel.getTaskId());
                taskInfoExtVO.setAnalyzeDate(logOverviewModel == null
                        ? "" : logOverviewModel.getStartTime() == null
                        ? ComConstants.ScanStatus.convertScanStatus(logOverviewModel.getStatus()) :
                        DateTimeUtils.second2DateString(logOverviewModel.getStartTime())
                        + ComConstants.ScanStatus.convertScanStatus(logOverviewModel.getStatus()));

                // 分析次数
                TaskLogOverviewModel taskLogOverviewModel = analyzeCountMap.get(taskInfoModel.getTaskId());
                taskInfoExtVO.setAnalyzeCount(taskLogOverviewModel == null ? 0 : taskLogOverviewModel.getBuildCount());


                long taskId = taskInfoModel.getTaskId();

                // 组装工具信息
                taskInfoExtVO.setTaskToolCount(MapUtils.getIntValue(taskToolCountMap, taskId));

                // 组装规则集信息
                List<CheckerSetVO> checkerSetList =
                        taskCheckerSetMap.computeIfAbsent(taskId, v -> Lists.newArrayList());
//                int count = checkerSetList.stream().mapToInt(CheckerSetModel::getCheckerCount).sum();
                int checkerCount = 0;
                int checkerSetCount = 0;
                if (CollectionUtils.isNotEmpty(checkerSetList)) {
                    checkerSetCount = checkerSetList.size();
                    for (CheckerSetVO checkerSetVo : checkerSetList) {
                        Integer count = checkerSetVo.getCheckerCount();
                        if (count != null && count > 0) {
                            checkerCount += count;
                        }
                    }
                }
                taskInfoExtVO.setCheckerSetCount(checkerSetCount);
                taskInfoExtVO.setCheckerCount(checkerCount);

                data.add(taskInfoExtVO);
            });
        });
        return new Page<>(taskPage.getCount(), taskPage.getPage(), taskPage.getPageSize(), taskPage.getTotalPages(),
                data);
    }

    @Override
    public List<DeptInfoVO> getChildDeptList(String parentId) {
        List<DeptInfoVO> objectList = Lists.newArrayList();
        if (StringUtils.isEmpty(parentId)) {
            parentId = "0";
        }

        String childDeptStr = (String) redisTemplate.opsForHash().get(RedisKeyConstants.KEY_DEPT_TREE, parentId);
        if (StringUtils.isNotEmpty(childDeptStr)) {
            String[] deptIdArr = childDeptStr.split(ComConstants.SEMICOLON);
            List<Object> infoList =
                    redisTemplate.opsForHash().multiGet(RedisKeyConstants.KEY_DEPT_INFOS, Arrays.asList(deptIdArr));

            for (int i = 0; i < deptIdArr.length; i++) {
                DeptInfoVO deptInfoVO = new DeptInfoVO();
                deptInfoVO.setId(deptIdArr[i]);
                deptInfoVO.setName(ObjectUtils.toString(infoList.get(i), ""));
                objectList.add(deptInfoVO);
            }
        }
        return objectList;
    }

    @Override
    public Page<ActiveTaskStatisticsVO> getActiveTaskList(TaskToolInfoReqVO reqVO, Integer pageNum, Integer pageSize,
            String sortField, String sortType) {
        log.info("getActiveTaskList req content: pageNum[{}], pageSize[{}], {}", pageNum, pageSize, reqVO);
        List<ActiveTaskStatisticsVO> data = Lists.newArrayList();

        // 日期范围时间戳
        long startTime = DateTimeUtils.getTimeStampStart(reqVO.getStartTime());
        long endTime = DateTimeUtils.getTimeStampEnd(reqVO.getEndTime());
        reqVO.setStartTime(null);
        reqVO.setEndTime(null);
        // 默认包含屏蔽用户的任务
        if (reqVO.getHasAdminTask() == null) {
            reqVO.setHasAdminTask(1);
        } else {
            reqVO.setExcludeUserList(metaDataService.queryExcludeUserList());
        }

        boolean flag = false;
        List<TaskLogOverviewModel> analyzeCount = null;
        // 如果筛选参数有最小执行次数
        Integer minAnalyzeCount = reqVO.getMinAnalyzeCount();
        if (minAnalyzeCount != null && minAnalyzeCount > 0) {
            flag = true;
            TaskToolInfoReqVO queryTaskIdVO = new TaskToolInfoReqVO();
            BeanUtils.copyProperties(reqVO, queryTaskIdVO);
            List<Long> taskIdSource = taskDao.findByBgIdAndDeptId(queryTaskIdVO);
            if (CollectionUtils.isEmpty(taskIdSource)) {
                log.info("query by dept info for task list is empty: {}", queryTaskIdVO);
                return new Page<>(0, 0, 0, Lists.newArrayList());
            }
            analyzeCount = taskLogOverviewService.queryAnalyzeCount(taskIdSource, null, startTime, endTime);
            if (CollectionUtils.isEmpty(analyzeCount)) {
                log.info("minAnalyzeCount {} start {} end {} for task list is empty.", minAnalyzeCount, startTime,
                        endTime);
                return new Page<>(0, 0, 0, Lists.newArrayList());
            }
            // 符合条件的任务ID
            List<Long> taskIds = analyzeCount.stream().filter(model -> model.getBuildCount() >= minAnalyzeCount)
                    .map(TaskLogOverviewModel::getTaskId).collect(Collectors.toList());
            reqVO.setTaskIds(taskIds);
        }

        Pageable pageable = PageUtils.INSTANCE.convertPageSizeToPageable(pageNum, pageSize, sortField, sortType);
        Page<TaskInfoModel> taskInfoPage = taskDao.findTaskInfoPage(reqVO, pageable);

        List<TaskInfoModel> taskInfoList = taskInfoPage.getRecords();
        if (CollectionUtils.isNotEmpty(taskInfoList)) {
            List<Long> taskIds =
                    taskInfoList.stream().filter(taskInfoModel -> StringUtils.isNotEmpty(taskInfoModel.getToolNames()))
                            .map(TaskInfoModel::getTaskId).collect(Collectors.toList());

            long latestListStartTime = System.currentTimeMillis();
            // 批量获取任务分析日志
            List<TaskLogOverviewModel> latestList = taskLogOverviewService.findLatestAnalyzeStatus(taskIds, null);
            Map<Long, TaskLogOverviewModel> taskLogOverviewMap = latestList.stream()
                    .collect(Collectors.toMap(TaskLogOverviewModel::getTaskId, Function.identity(), (k, v) -> v));
            log.info("getActiveTaskList: taskLogOverviewService.findLatestAnalyzeStatus(), time consuming: [{}]",
                    System.currentTimeMillis() - latestListStartTime);

            // 时间范围内最新build id
            List<TaskLogOverviewModel> buildIdsByStartTime = taskLogOverviewService
                    .findBuildIdsByStartTime(taskIds,null,1L, endTime);
            Set<String> buildIds =
                    buildIdsByStartTime.stream().map(TaskLogOverviewModel::getBuildId).collect(Collectors.toSet());

            // 获取代码仓库信息
            Map<Long, Set<CodeRepoModel>> taskCodeRepoMap = codeRepoService.queryCodeRepoInfo(taskIds, buildIds);

            if (!flag) {
                analyzeCount = taskLogOverviewService.queryAnalyzeCount(taskIds, null, startTime, endTime);
            }
            Map<Long, TaskLogOverviewModel> analyzeCountMap = analyzeCount.stream()
                    .collect(Collectors.toMap(TaskLogOverviewModel::getTaskId, Function.identity(), (k, v) -> v));

            // 代码语言转换
            List<MetadataVO> metadataVoList = metaDataService.getCodeLangMetadataList();
            // 组织架构信息
            Map<Object, Object> deptInfo = redisTemplate.opsForHash().entries(RedisKeyConstants.KEY_DEPT_INFOS);

            data = taskInfoList.stream().map(taskInfoModel -> {
                ActiveTaskStatisticsVO activeTaskVO = new ActiveTaskStatisticsVO();
                BeanUtils.copyProperties(taskInfoModel, activeTaskVO);

                TaskLogOverviewModel logOverviewModel = taskLogOverviewMap.get(taskInfoModel.getTaskId());
                activeTaskVO.setAnalyzeDate(logOverviewModel == null
                        ? "" : logOverviewModel.getStartTime() == null
                        ? ComConstants.ScanStatus.convertScanStatus(logOverviewModel.getStatus()) :
                        DateTimeUtils.second2DateString(logOverviewModel.getStartTime())
                        + ComConstants.ScanStatus.convertScanStatus(logOverviewModel.getStatus()));
//                activeTaskVO.setIsActive(status == null ? "非活跃" : "活跃");

                // 获取代码仓库地址
                Set<CodeRepoModel> codeRepoModels = taskCodeRepoMap.get(taskInfoModel.getTaskId());
                String codeRepoStr = "";
                if (CollectionUtils.isNotEmpty(codeRepoModels)) {
                    List<String> urls = new ArrayList<>();
                    for (CodeRepoModel codeRepoModel : codeRepoModels) {
                        urls.add(codeRepoModel.getUrl());
                    }
                    if (CollectionUtils.isNotEmpty(urls)) {
                        codeRepoStr = urls.stream().filter(str -> str != null && str.startsWith("http"))
                                .collect(Collectors.joining(","));
                    }
                }
                activeTaskVO.setRepoUrl(codeRepoStr);

                // 获取分析次数
                TaskLogOverviewModel taskLogOverviewModel = analyzeCountMap.get(taskInfoModel.getTaskId());
                activeTaskVO.setAnalyzeCount(taskLogOverviewModel == null ? 0 : taskLogOverviewModel.getBuildCount());

                activeTaskVO.setBgName(ObjectUtils.toString(deptInfo.get(String.valueOf(taskInfoModel.getBgId())), ""));
                activeTaskVO
                        .setDeptName(ObjectUtils.toString(deptInfo.get(String.valueOf(taskInfoModel.getDeptId())), ""));
                activeTaskVO.setCenterName(
                        ObjectUtils.toString(deptInfo.get(String.valueOf(taskInfoModel.getCenterId())), ""));
                activeTaskVO.setCodeLang(ConvertUtil.convertCodeLang(taskInfoModel.getCodeLang(), metadataVoList));

                return activeTaskVO;
            }).collect(Collectors.toList());
        }

        return new Page<>(taskInfoPage.getCount(), taskInfoPage.getPage(), taskInfoPage.getPageSize(),
                taskInfoPage.getTotalPages(), data);
    }


    /**
     * 获取任务某个工具的首次分析成功时间对象
     *
     * @param taskId   任务ID
     * @param toolName 工具名
     * @return model or null
     */
    @Override
    public FirstAnalysisSuccessModel getFirstAnalyzeSuccess(Long taskId, String toolName) {

        FirstAnalysisSuccessModel firstAnalysisSuccessModel = null;

        List<FirstAnalysisSuccessModel> firstAnalysisSuccessModelList =
                firstAnalyzeSuccessDao.findByTaskIdAndToolName(taskId, toolName);
        if (CollectionUtils.isNotEmpty(firstAnalysisSuccessModelList)) {
            firstAnalysisSuccessModel = firstAnalysisSuccessModelList.iterator().next();
        }

        return firstAnalysisSuccessModel;
    }

    /**
     * 多条件分页获取任务model列表
     *
     * @param reqVO 请求体
     * @return page
     */
    @Override
    public Page<TaskInfoModel> findTaskInfoPage(TaskToolInfoReqVO reqVO, Pageable pageable) {
        return taskDao.findTaskInfoPage(reqVO, pageable);
    }

    /**
     * 获取每日登录用户列表
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return page
     */
    @Override
    public Page<UserLogInfoStatVO> findDailyUserLogInfoList(String startTime, String endTime, Integer pageNum,
            Integer pageSize, String sortField, String sortType) {

        if (sortField == null) {
            sortField = "login_date";
        }
        if (sortType == null) {
            sortType = "DESC";
        }
        Pageable pageable = PageUtils.INSTANCE.generaPageableUnlimitedPageSize(pageNum, pageSize, sortField, sortType);

        if (StringUtils.isEmpty(startTime) || StringUtils.isEmpty(endTime)) {
            // 默认查询一个星期
            Map<String, String> dateMap = DateTimeUtils.getStartDateAndEndDate();
            // 获取开始时间
            startTime = dateMap.get("startTime");
            // 获取结束时间
            endTime = dateMap.get("endTime");
        }

        Date startDate = new Date(DateTimeUtils.getTimeStamp(startTime));
        Date endDate = new Date(DateTimeUtils.getTimeStamp(endTime));

        List<UserLogInfoModel> userLogInfoList = userLogInfoDao.findDailyUserLogInfoList(startDate, endDate, pageable);

        List<UserLogInfoStatVO> data = Lists.newArrayList();
        if (CollectionUtils.isEmpty(userLogInfoList)) {
            log.info("userLogInfoList is empty!");
            return new Page<>(0, 0, 0, data);
        }

        userLogInfoList.forEach(dateInfo -> {
            LocalDate loginDate = dateInfo.getLoginDate();
            Set<String> userNameList = dateInfo.getUserNameList();
            if (CollectionUtils.isNotEmpty(userNameList)) {
                List<UserLogInfoStatVO> userLogInfoVoList = userNameList.stream().map(userName -> {
                    UserLogInfoStatVO userLogInfoVO = new UserLogInfoStatVO();
                    userLogInfoVO.setUserName(userName);
                    userLogInfoVO.setLastLogin(DateTimeUtils.localDateTransformTimestamp(loginDate));
                    return userLogInfoVO;
                }).collect(Collectors.toList());
                data.addAll(userLogInfoVoList);
            }
        });

        // 按日期倒序、用户名升序排列
        List<UserLogInfoStatVO> userLogInfo = data.stream().sorted(Comparator.comparing(UserLogInfoStatVO::getUserName))
                .sorted(Comparator.comparing(UserLogInfoStatVO::getLastLogin).reversed()).collect(Collectors.toList());

        // 计算分页
        Page<UserLogInfoStatVO> userLogInfoPage =
                getPage(pageable.getPageNumber() + 1, pageable.getPageSize(), userLogInfo);
        List<UserLogInfoStatVO> pageRecords = userLogInfoPage.getRecords();

        List<String> userNameList =
                pageRecords.stream().map(UserLogInfoStatVO::getUserName).collect(Collectors.toList());

        // 根据用户名获取组织架构
        List<UserLogInfoStatModel> orgByUserNameList = userLogInfoStatDao.findOrgByUserNameList(userNameList);
        Map<String, UserLogInfoStatModel> logInfoStatModelMap = orgByUserNameList.stream()
                .collect(Collectors.toMap(UserLogInfoStatModel::getUserName, Function.identity(), (k, v) -> v));

        // 获取组织架构信息
        Map<Object, Object> deptInfo = redisTemplate.opsForHash().entries(RedisKeyConstants.KEY_DEPT_INFOS);
        // 循环遍历
        pageRecords.forEach(userLogInfoVO -> {
            UserLogInfoStatModel orgModel = logInfoStatModelMap.get(userLogInfoVO.getUserName());
            int bgId = 0;
            int deptId = 0;
            int centerId = 0;
            if (orgModel != null) {
                bgId = orgModel.getBgId() == null ? 0 : orgModel.getBgId();
                deptId = orgModel.getDeptId() == null ? 0 : orgModel.getDeptId();
                centerId = orgModel.getCenterId() == null ? 0 : orgModel.getCenterId();
            }
            userLogInfoVO.setBgName(ObjectUtils.toString(deptInfo.get(String.valueOf(bgId)), ""));
            userLogInfoVO.setDeptName(ObjectUtils.toString(deptInfo.get(String.valueOf(deptId)), ""));
            userLogInfoVO.setCenterName(ObjectUtils.toString(deptInfo.get(String.valueOf(centerId)), ""));
        });
        return new Page<>(userLogInfoPage.getCount(), userLogInfoPage.getPage(), userLogInfoPage.getPageSize(),
                userLogInfoPage.getTotalPages(), pageRecords);
    }


    private <T> Page<T> getPage(Integer pageNum, Integer pageSize, @NotNull List<T> tList) {
        int total = tList.size();
        pageNum = pageNum == null || pageNum - 1 < 0 ? 0 : pageNum - 1;

        int pageSizeNum = 10;
        if (pageSize != null && pageSize >= 0) {
            pageSizeNum = pageSize;
        }

        int totalPageNum = 0;
        if (total > 0) {
            totalPageNum = (total + pageSizeNum - 1) / pageSizeNum;
        }
        int subListBeginIdx = pageNum * pageSizeNum;
        int subListEndIdx = subListBeginIdx + pageSizeNum;
        if (subListBeginIdx > total) {
            subListBeginIdx = 0;
        }
        tList = tList.subList(subListBeginIdx, Math.min(subListEndIdx, total));

        return new Page<>(total, pageNum + 1, pageSizeNum, totalPageNum, tList);
    }


    /**
     * 获取总用户登录列表
     *
     * @return page
     */
    @Override
    public Page<UserLogInfoStatVO> findAllUserLogInfoStatList(Integer pageNum, Integer pageSize, String sortField,
            String sortType) {
        List<UserLogInfoStatVO> data = Lists.newArrayList();

        if (sortField == null) {
            // 排序字段为login_date
            sortField = "last_login";
        }
        if (sortType == null) {
            sortType = "DESC";
        }
        Pageable pageable = PageUtils.INSTANCE.generaPageableUnlimitedPageSize(pageNum, pageSize, sortField, sortType);

        Page<UserLogInfoStatModel> userLogInfoStatPage = userLogInfoStatDao.findAllUserLogInfoStatPage(pageable);
        List<UserLogInfoStatModel> userLogInfoStatModels = userLogInfoStatPage.getRecords();

        if (CollectionUtils.isNotEmpty(userLogInfoStatModels)) {
            // 获取组织架构信息
            Map<Object, Object> deptInfo = redisTemplate.opsForHash().entries(RedisKeyConstants.KEY_DEPT_INFOS);
            // 循环遍历
            userLogInfoStatModels.forEach(userLogInfoStatModel -> {
                UserLogInfoStatVO userLogInfoStatVO = new UserLogInfoStatVO();
                BeanUtils.copyProperties(userLogInfoStatModel, userLogInfoStatVO);
                // 根据BgId获得BgName赋值给userLogInfoStatVO
                int bgId = userLogInfoStatModel.getBgId() == null ? 0 : userLogInfoStatModel.getBgId();
                userLogInfoStatVO.setBgName(ObjectUtils.toString(deptInfo.get(String.valueOf(bgId)), ""));

                // 根据DeptId获得DeptName
                int deptId = userLogInfoStatModel.getDeptId() == null ? 0 : userLogInfoStatModel.getDeptId();
                userLogInfoStatVO.setDeptName(ObjectUtils.toString(deptInfo.get(String.valueOf(deptId)), ""));

                // 根据CenterId()获得CenterName
                int centerId = userLogInfoStatModel.getCenterId() == null ? 0 : userLogInfoStatModel.getCenterId();
                userLogInfoStatVO.setCenterName(ObjectUtils.toString(deptInfo.get(String.valueOf(centerId)), ""));
                data.add(userLogInfoStatVO);
            });
        }
        return new Page<>(userLogInfoStatPage.getCount(), userLogInfoStatPage.getPage(),
                userLogInfoStatPage.getPageSize(), userLogInfoStatPage.getTotalPages(), data);
    }

    /**
     * 获取每日用戶登录情况折线图数据
     *
     * @return list
     */
    @Override
    public List<UserLogInfoChartVO> dailyUserLogInfoData(String startTime, String endTime) {
        // 获取日期 默认显示14天
        List<String> dates = DateTimeUtils.getDatesByStartTimeAndEndTime(startTime, endTime, DAY_FOURTEEN);
        // 用于存储每日全部用户登录总次数和新增用户数量
        List<UserLogInfoChartVO> userLogInCountList = new ArrayList<>();
        for (String date : dates) {
            UserLogInfoChartVO userLogInfoChartVO = new UserLogInfoChartVO();
            Integer userLogInCount = null;
            Integer userAddCount = null;
            long[] startTimeAndEndTime = DateTimeUtils.getStartTimeAndEndTime(date, date);
            try {
                // 获取每日登陆用户数量
                userLogInCount = userLogInfoDao.findUserLogInCountByDaily(startTimeAndEndTime);
                // 获取每日新增用户数量
                userAddCount = userLogInfoStatDao.findUserAddCountByDaily(date);
            } catch (Exception e) {
                log.error("Failed to obtain user data", e);
            }

            userLogInfoChartVO.setDate(date);
            userLogInfoChartVO.setUserLogInCount(userLogInCount);
            userLogInfoChartVO.setUserAddCount(userAddCount);
            userLogInCountList.add(userLogInfoChartVO);
        }
        return userLogInCountList;
    }

    /**
     * 获取总用户数折线图数据
     *
     * @return list
     */
    @Override
    public List<UserLogInfoChartVO> sumUserLogInfoStatData(String startTime, String endTime) {
        // 获取日期 默认显示14天
        List<String> dates = DateTimeUtils.getDatesByStartTimeAndEndTime(startTime, endTime, DAY_FOURTEEN);
        List<UserLogInfoChartVO> userSumCount = new ArrayList<>();
        for (String date : dates) {
            UserLogInfoChartVO userLogInfoChartVO = new UserLogInfoChartVO();
            Integer userCount = null;
            try {
                // 获取用户数量
                userCount = userLogInfoStatDao.findUserLogInCount(date);
            } catch (Exception e) {
                log.error("Failed to obtain user data", e);
            }
            // 封装时间
            userLogInfoChartVO.setDate(date);
            // 封装用户数量
            userLogInfoChartVO.setUserLogInCount(userCount);
            userSumCount.add(userLogInfoChartVO);
        }
        return userSumCount;
    }

    /**
     * 获取每周用户登录数折线图数据
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return
     */
    @Override
    public List<UserLogInfoChartVO> weekUserLogInfoData(String startTime, String endTime) {
        // 获取选择范围的每一周的时间
        List<String> weekList = getWeekTime();
        String start = startTime;
        String end = endTime;
        // 字符串截取,截取一周(week)的 startTime:2019/1/21 和 endTime:2019/1/27
        startTime = startTime.substring(startTime.indexOf("周") + 1, startTime.indexOf("-")).replace("/", "-");
        endTime = endTime.substring(endTime.indexOf("-") + 1).replace("/", "-");
        long[] startAndEndTime = DateTimeUtils.getStartTimeAndEndTime(startTime, endTime);
        if (startAndEndTime[0] > startAndEndTime[1]) {
            startTime = end;
            endTime = start;
        } else {
            startTime = start;
            endTime = end;
        }
        List<String> weeks = new ArrayList<>();
        if (!startTime.equals(endTime)) {
            int startIndex = weekList.indexOf(startTime);
            int endIndex = weekList.indexOf(endTime);
            weeks = weekList.subList(startIndex, endIndex + 1);
        } else {
            weeks.add(startTime);
        }
        List<UserLogInfoChartVO> userWeekSumCount = new ArrayList<>();
        for (String week : weeks) {
            UserLogInfoChartVO userLogInfoChartVO = new UserLogInfoChartVO();
            // 字符串截取,截取一周(week)的 startTime:2019/1/21 和 endTime:2019/1/27
            String weekStartTime = week.substring(week.indexOf("周") + 1, week.indexOf("-")).replace("/", "-");
            String weekEndTime = week.substring(week.indexOf("-") + 1).replace("/", "-");
            // 转为时间戳类型
            long[] startTimeAndEndTime = DateTimeUtils.getStartTimeAndEndTime(weekStartTime, weekEndTime);
            // 获取本周用户登录数量
            Integer userCount = userLogInfoDao.findUserLogInCountByDaily(startTimeAndEndTime);
            // 封装时间(折线图x轴)
            userLogInfoChartVO.setDate(week);
            // 封装每一周对应的用户数量(折线图y轴)
            userLogInfoChartVO.setUserLogInCount(userCount);
            userWeekSumCount.add(userLogInfoChartVO);
        }
        return userWeekSumCount;
    }

    /**
     * 获取任务和活跃任务折线图数据
     *
     * @param reqVO 任务工具信息请求体
     * @return list
     */
    @Override
    public List<TaskAndToolStatChartVO> taskAndActiveTaskData(@NotNull TaskToolInfoReqVO reqVO) {
        String endTime = reqVO.getEndTime();
        Set<String> createFromReq = reqVO.getCreateFrom();
        // 获取日期 默认显示30天
        List<String> dates = DateTimeUtils.getDatesByStartTimeAndEndTime(reqVO.getStartTime(), endTime, DAY_THIRTY);
        // 根据时间、来源、获取TaskStatisticModel集合
        List<TaskStatisticModel> taskStatList = taskStatisticDao.findTaskCountByCreateFromAndTime(dates, createFromReq);

        Map<String, TaskStatisticModel> statisticModelMap;
        if (CollectionUtils.isNotEmpty(taskStatList)) {
            statisticModelMap = taskStatList.stream()
                    .collect(Collectors.toMap(TaskStatisticModel::getDate, Function.identity(), (k, v) -> v));
        } else {
            statisticModelMap = Maps.newHashMap();
        }

        List<TaskAndToolStatChartVO> taskStatisticList = new ArrayList<>();
        dates.forEach(date -> {
            TaskAndToolStatChartVO taskAndToolStatChartVO = new TaskAndToolStatChartVO();
            taskAndToolStatChartVO.setDate(date);
            TaskStatisticModel model = statisticModelMap.get(date);
            if (model != null) {
                // 设置任务数量
                taskAndToolStatChartVO.setCount(model.getTaskCount());
                // 设置活跃任务数量
                taskAndToolStatChartVO.setActiveCount(model.getActiveCount());
            }
            taskStatisticList.add(taskAndToolStatChartVO);
        });

        // 获取当天数据
        String currentDate = DateTimeUtils.getDateByDiff(0);
        if (StringUtils.isEmpty(endTime) || currentDate.equals(endTime)) {
            Set<String> createFromSet;
            List<String> redisDataFrom;
            if (createFromReq.size() == 2) {
                createFromSet =
                        Sets.newHashSet(BsTaskCreateFrom.GONGFENG_SCAN.value(), BsTaskCreateFrom.BS_PIPELINE.value(),
                                BsTaskCreateFrom.BS_CODECC.value());
                redisDataFrom = Lists.newArrayList(ComConstants.DefectStatType.GONGFENG_SCAN.value(),
                        ComConstants.DefectStatType.USER.value());
            } else {
                if (createFromReq.contains(ComConstants.DefectStatType.GONGFENG_SCAN.value())) {
                    createFromSet = Sets.newHashSet(BsTaskCreateFrom.GONGFENG_SCAN.value());
                    redisDataFrom = Lists.newArrayList(ComConstants.DefectStatType.GONGFENG_SCAN.value());
                } else {
                    createFromSet =
                            Sets.newHashSet(BsTaskCreateFrom.BS_PIPELINE.value(), BsTaskCreateFrom.BS_CODECC.value());
                    redisDataFrom = Lists.newArrayList(ComConstants.DefectStatType.USER.value());
                }
            }
            long taskCount = taskDao.findByCreateFrom(createFromSet, ComConstants.Status.ENABLE.value());

            int activeTaskCount = redisDataFrom.stream().map(dataFrom -> String
                    .format("%s%s:%s", RedisKeyConstants.PREFIX_ACTIVE_TASK, currentDate, dataFrom))
                    .map(redisKey -> redisTemplate.opsForSet().size(redisKey).toString())
                    .filter(StringUtils::isNotEmpty).mapToInt(Integer::parseInt).sum();

            TaskAndToolStatChartVO endNodeChartVO = taskStatisticList.get(taskStatisticList.size() - 1);
            endNodeChartVO.setCount((int) taskCount);
            endNodeChartVO.setActiveCount(activeTaskCount);
        }

        return taskStatisticList;
    }


    /**
     * 获取年对应的每一周时间段
     *
     * @return list
     */
    @Override
    public List<String> getWeekTime() {
        return DateTimeUtils.getWeekTime();
    }

    /**
     * 获取任务分析折线图数据
     *
     * @param reqVO 任务工具信息请求体
     * @return list
     */
    @Override
    public List<TaskAndToolStatChartVO> taskAnalyzeCountData(TaskToolInfoReqVO reqVO) {
        String endTime = reqVO.getEndTime();
        // 获取日期 默认显示30天
        List<String> dates = DateTimeUtils.getDatesByStartTimeAndEndTime(reqVO.getStartTime(), endTime, DAY_THIRTY);
        // 根据时间、来源、获取TaskStatisticModel集合
        List<TaskStatisticModel> taskStatList =
                taskStatisticDao.findTaskAnalyzeCountByCreateFromAndTime(dates, reqVO.getCreateFrom());
        Assert.notNull(taskStatList, "taskStatistic list is null.");
        Map<String, TaskStatisticModel> statisticModelMap = taskStatList.stream()
                .collect(Collectors.toMap(TaskStatisticModel::getDate, Function.identity(), (k, v) -> v));

        List<TaskAndToolStatChartVO> taskStatisticList = new ArrayList<>();
        dates.forEach(date -> {
            TaskAndToolStatChartVO taskAndToolStatChartVO = new TaskAndToolStatChartVO();
            taskAndToolStatChartVO.setDate(date);
            TaskStatisticModel model = statisticModelMap.get(date);
            if (model != null) {
                // 设置任务分析次数
                taskAndToolStatChartVO.setAnalyzeCount(model.getAnalyzeCount());
            }
            taskStatisticList.add(taskAndToolStatChartVO);
        });

        // 获取当天数据
        String currentDate = DateTimeUtils.getDateByDiff(0);
        if (StringUtils.isEmpty(endTime) || currentDate.equals(endTime)) {
            // 获取taskId
            TaskToolInfoReqVO taskToolInfoReqVO = new TaskToolInfoReqVO();
            taskToolInfoReqVO.setStatus(ComConstants.Status.ENABLE.value());
            taskToolInfoReqVO.setCreateFrom(BsTaskCreateFrom.getByStatType(reqVO.getCreateFrom()));
            taskToolInfoReqVO.setHasAdminTask(1);
            List<Long> taskIds = taskDao.findByBgIdAndDeptId(taskToolInfoReqVO);

            long[] startEnd = DateTimeUtils.getStartTimeAndEndTime(currentDate, currentDate);
            int analyzeCount = taskLogOverviewService.statTaskAnalyzeCount(taskIds, null, startEnd[0], startEnd[1]);
            TaskAndToolStatChartVO endNodeChartVO = taskStatisticList.get(taskStatisticList.size() - 1);
            endNodeChartVO.setAnalyzeCount(analyzeCount);
        }

        return taskStatisticList;
    }

    /**
     * 分页查询任务代码量信息
     *
     * @param reqVO 请求体
     * @return page
     */
    @Override
    public Page<TaskCodeLineStatVO> queryTaskCodeLineStat(TaskToolInfoReqVO reqVO, Integer pageNum,
            Integer pageSize, String sortField, String sortType) {
        log.info("queryTaskCodeLineStat req content: pageNum[{}], pageSize[{}], {}", pageNum, pageSize, reqVO);
        if (reqVO.getHasAdminTask() == null) {
            reqVO.setHasAdminTask(1);
        } else {
            reqVO.setExcludeUserList(metaDataService.queryExcludeUserList());
        }
        // 默认前端排序字段
        String sortFieldInDb =
                CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, sortField == null ? "taskId" : sortField);
        Pageable pageable = PageUtils.INSTANCE.convertPageSizeToPageable(pageNum, pageSize, sortFieldInDb, sortType);
        Page<TaskInfoModel> taskInfoPage = taskDao.findTaskInfoPage(reqVO, pageable);

        List<TaskCodeLineStatVO> taskCodeLineStatVOList = Lists.newArrayList();
        List<TaskInfoModel> taskInfoList = taskInfoPage.getRecords();
        if (CollectionUtils.isNotEmpty(taskInfoList)) {
            List<Long> taskIds = taskInfoList.stream().map(TaskInfoModel::getTaskId).collect(Collectors.toList());

            List<CodeRepoFromAnalyzeLogModel> codeRepoList =
                    codeRepoFromAnalyzeLogService.getCodeRepoListByTaskIds(taskIds);
            Map<Long, CodeRepoFromAnalyzeLogModel> codeRepoFromAnalyzeLogMap = codeRepoList.stream().collect(
                    Collectors.toMap(CodeRepoFromAnalyzeLogModel::getTaskId, Function.identity(), (k, v) -> v));

            long clocStatisticModelsStartTime = System.currentTimeMillis();
            List<CLOCStatisticModel> clocStatisticModels = iclocQueryCodeLineService.queryTaskLastCodeLine(taskIds);
            Map<Long, CLOCStatisticModel> clocStatisticMap = clocStatisticModels.stream()
                    .collect(Collectors.toMap(CLOCStatisticModel::getTaskId, Function.identity(), (k, v) -> v));
            log.info("queryTaskCodeLineStat: iclocQueryCodeLineService.queryTaskLastCodeLine(), time consuming: [{}]",
                    System.currentTimeMillis() - clocStatisticModelsStartTime);

            // 代码语言转换
            List<MetadataVO> metadataVoList = metaDataService.getCodeLangMetadataList();
            // 组织架构信息
            Map<Object, Object> deptInfo = redisTemplate.opsForHash().entries(RedisKeyConstants.KEY_DEPT_INFOS);

            for (TaskInfoModel taskInfoModel : taskInfoList) {
                TaskCodeLineStatVO taskCodeLineStatVO = new TaskCodeLineStatVO();
                BeanUtils.copyProperties(taskInfoModel, taskCodeLineStatVO);
                long taskId = taskInfoModel.getTaskId();

                CodeRepoFromAnalyzeLogModel codeRepoModel = codeRepoFromAnalyzeLogMap.get(taskInfoModel.getTaskId());
                String codeRepoStr = "";
                if (null != codeRepoModel) {
                    Set<String> urls = codeRepoModel.getUrls();
                    if (CollectionUtils.isNotEmpty(urls)) {
                        codeRepoStr = urls.stream().filter(str -> str != null && str.startsWith("http"))
                                .collect(Collectors.joining(","));
                    }
                }
                taskCodeLineStatVO.setRepoUrl(codeRepoStr);
                // 赋值代码行数据
                CLOCStatisticModel clocStatisticModel = clocStatisticMap.get(taskId);
                if (clocStatisticModel != null) {
                    taskCodeLineStatVO.setCodeLineCount(clocStatisticModel.getSumCode());
                    taskCodeLineStatVO.setBlankCount(clocStatisticModel.getSumBlank());
                    taskCodeLineStatVO.setCommentCount(clocStatisticModel.getSumComment());
                }

                taskCodeLineStatVO
                        .setCodeLang(ConvertUtil.convertCodeLang(taskInfoModel.getCodeLang(), metadataVoList));
                taskCodeLineStatVO
                        .setBgName(ObjectUtils.toString(deptInfo.get(String.valueOf(taskInfoModel.getBgId())), ""));
                taskCodeLineStatVO
                        .setDeptName(ObjectUtils.toString(deptInfo.get(String.valueOf(taskInfoModel.getDeptId())), ""));
                taskCodeLineStatVO.setCenterName(
                        ObjectUtils.toString(deptInfo.get(String.valueOf(taskInfoModel.getCenterId())), ""));
                taskCodeLineStatVOList.add(taskCodeLineStatVO);
            }
        }

        return new Page<>(taskInfoPage.getCount(), taskInfoPage.getPage(), taskInfoPage.getPageSize(),
                taskInfoPage.getTotalPages(), taskCodeLineStatVOList);
    }

}
