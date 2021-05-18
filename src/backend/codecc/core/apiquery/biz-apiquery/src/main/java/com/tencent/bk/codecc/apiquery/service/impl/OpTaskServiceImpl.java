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

import com.google.common.collect.Lists;
import com.tencent.bk.codecc.apiquery.defect.model.TaskLogModel;
import com.tencent.bk.codecc.apiquery.service.MetaDataService;
import com.tencent.bk.codecc.apiquery.service.TaskService;
import com.tencent.bk.codecc.apiquery.task.dao.TaskDao;
import com.tencent.bk.codecc.apiquery.task.dao.mongotemplate.ToolDao;
import com.tencent.bk.codecc.apiquery.task.model.TaskInfoModel;
import com.tencent.bk.codecc.apiquery.task.model.ToolConfigInfoModel;
import com.tencent.bk.codecc.apiquery.utils.ConvertUtil;
import com.tencent.bk.codecc.apiquery.utils.PageUtils;
import com.tencent.bk.codecc.apiquery.vo.CheckerSetVO;
import com.tencent.bk.codecc.apiquery.vo.DeptInfoVO;
import com.tencent.bk.codecc.apiquery.vo.TaskInfoExtVO;
import com.tencent.bk.codecc.apiquery.vo.TaskToolInfoReqVO;
import com.tencent.bk.codecc.apiquery.vo.ToolConfigInfoVO;
import com.tencent.bk.codecc.apiquery.vo.op.ActiveTaskStatisticsVO;
import com.tencent.bk.codecc.task.vo.MetadataVO;
import com.tencent.devops.common.api.pojo.Page;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.RedisKeyConstants;
import com.tencent.devops.common.util.DateTimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
    private Client client;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private TaskDao taskDao;

    @Autowired
    private ToolDao toolDao;

    @Autowired
    private CheckerSetBizServiceImpl checkerSetBizService;

    @Autowired
    private TaskLogServiceImpl taskLogService;

    @Autowired
    private MetaDataService metaDataService;


    @Override
    public Page<TaskInfoExtVO> getOverAllTaskList(TaskToolInfoReqVO reqVO, Integer pageNum, Integer pageSize,
            String sortField, String sortType) {
        List<TaskInfoExtVO> data = Lists.newArrayList();
        log.info("op getOverAllTaskList req: {}", reqVO);

        // TODO 待优化
        String toolName = reqVO.getToolName();
        if (StringUtils.isNotEmpty(toolName)) {
            // 获取非下架的任务ID
            List<Long> taskIdByToolAndStatus =
                    toolDao.findTaskIdByToolAndStatus(toolName, ComConstants.FOLLOW_STATUS.WITHDRAW.value(), true);
            reqVO.setTaskIds(taskIdByToolAndStatus);
        }

        Pageable pageable = PageUtils.INSTANCE.convertPageSizeToPageable(pageNum, pageSize, sortField, sortType);
        Page<TaskInfoModel> taskPage = taskDao.findTaskInfoPage(reqVO, pageable);

        List<TaskInfoModel> taskInfoModels = taskPage.getRecords();
        if (CollectionUtils.isNotEmpty(taskInfoModels)) {
            Set<Long> taskIdSet = taskInfoModels.stream().map(TaskInfoModel::getTaskId).collect(Collectors.toSet());

            // 获取指定任务的工具配置列表
            List<ToolConfigInfoModel> toolConfigInfoModels =
                    toolDao.findByToolAndFollowStatus(taskIdSet, null, ComConstants.FOLLOW_STATUS.WITHDRAW.value(),
                            true);
            Map<Long, List<ToolConfigInfoVO>> taskToolConfigMap = toolConfigInfoModels.stream().map(model -> {
                ToolConfigInfoVO toolConfigInfoVO = new ToolConfigInfoVO();
                BeanUtils.copyProperties(model, toolConfigInfoVO);
                return toolConfigInfoVO;
            }).collect(Collectors.groupingBy(ToolConfigInfoVO::getTaskId));

            // 获取任务关联的规则集
            Map<Long, List<CheckerSetVO>> taskCheckerSetMap = checkerSetBizService.getCheckerSetByTaskIdSet(taskIdSet);
            // 获取任务分析次数
            Map<Long, Integer> analyzeMap = taskLogService.batchTaskLogCountList(taskIdSet, toolName);
            // 代码语言转换
            List<MetadataVO> metadataVoList = metaDataService.getCodeLangMetadataList();
            // 组织架构信息
            Map<Object, Object> deptInfo = redisTemplate.opsForHash().entries(RedisKeyConstants.KEY_DEPT_INFOS);

            taskInfoModels.forEach(taskInfoModel -> {
                TaskInfoExtVO taskInfoExtVO = new TaskInfoExtVO();
                BeanUtils.copyProperties(taskInfoModel, taskInfoExtVO);
                taskInfoExtVO
                        .setBgName(ObjectUtils.toString(deptInfo.get(String.valueOf(taskInfoModel.getBgId())), ""));
                taskInfoExtVO
                        .setDeptName(ObjectUtils.toString(deptInfo.get(String.valueOf(taskInfoModel.getDeptId())), ""));
                taskInfoExtVO.setCenterName(
                        ObjectUtils.toString(deptInfo.get(String.valueOf(taskInfoModel.getCenterId())), ""));
                taskInfoExtVO.setCodeLangStr(ConvertUtil.convertCodeLang(taskInfoModel.getCodeLang(), metadataVoList));

                long taskId = taskInfoModel.getTaskId();

                // 组装工具信息
                List<ToolConfigInfoVO> toolConfigInfoList =
                        taskToolConfigMap.computeIfAbsent(taskId, v -> Lists.newArrayList());
                taskInfoExtVO.setToolConfigInfoList(toolConfigInfoList);

                // 组装规则集信息
                List<CheckerSetVO> checkerSetList =
                        taskCheckerSetMap.computeIfAbsent(taskId, v -> Lists.newArrayList());
                taskInfoExtVO.setCheckerSetList(checkerSetList);
//                int count = checkerSetList.stream().mapToInt(CheckerSetModel::getCheckerCount).sum();
                int checkerCount = 0;
                if (CollectionUtils.isNotEmpty(checkerSetList)) {
                    for (CheckerSetVO checkerSetVo : checkerSetList) {
                        Integer count = checkerSetVo.getCheckerCount();
                        if (count != null && count > 0) {
                            checkerCount += count;
                        }
                    }
                }
                taskInfoExtVO.setCheckerCount(checkerCount);

                // 组装分析次数
                int analyze = 0;
                Integer analyzeCount = analyzeMap.get(taskId);
                if (analyzeCount != null) {
                    analyze = analyzeCount;
                }
                taskInfoExtVO.setAnalyzeCount(analyze);

                data.add(taskInfoExtVO);
            });
        }
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

        Pageable pageable = PageUtils.INSTANCE.convertPageSizeToPageable(pageNum, pageSize, sortField, sortType);
        Page<TaskInfoModel> taskInfoPage = taskDao.findTaskInfoPage(reqVO, pageable);

        List<TaskInfoModel> taskInfoList = taskInfoPage.getRecords();
        if (CollectionUtils.isNotEmpty(taskInfoList)) {
            List<Long> taskIds =
                    taskInfoList.stream().filter(taskInfoModel -> StringUtils.isNotEmpty(taskInfoModel.getToolNames()))
                            .map(TaskInfoModel::getTaskId).collect(Collectors.toList());

            // 批量获取任务分析日志
            Map<Long, List<TaskLogModel>> taskLogListMap =
                    taskLogService.batchFindByTaskIdListAndTime(taskIds, startTime, endTime);
            // 代码语言转换
            List<MetadataVO> metadataVoList = metaDataService.getCodeLangMetadataList();
            // 组织架构信息
            Map<Object, Object> deptInfo = redisTemplate.opsForHash().entries(RedisKeyConstants.KEY_DEPT_INFOS);

            data = taskInfoList.stream().map(taskInfoModel -> {
                ActiveTaskStatisticsVO activeTaskVO = new ActiveTaskStatisticsVO();
                BeanUtils.copyProperties(taskInfoModel, activeTaskVO);

                List<TaskLogModel> logModels = taskLogListMap.get(taskInfoModel.getTaskId());
                activeTaskVO.setIsActive(CollectionUtils.isEmpty(logModels) ? "非活跃" : "活跃");

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

}
