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

package com.tencent.bk.codecc.task.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tencent.bk.codecc.defect.api.OpDefectRestResource;
import com.tencent.bk.codecc.defect.api.ServiceTaskLogOverviewResource;
import com.tencent.bk.codecc.task.dao.CommonDao;
import com.tencent.bk.codecc.task.dao.mongorepository.AnalyzeCountStatRepository;
import com.tencent.bk.codecc.task.dao.mongorepository.CodeLineStatisticRepository;
import com.tencent.bk.codecc.task.dao.mongorepository.TaskStatisticRepository;
import com.tencent.bk.codecc.task.dao.mongorepository.ToolElapseTimeRepository;
import com.tencent.bk.codecc.task.dao.mongorepository.ToolStatisticRepository;
import com.tencent.bk.codecc.task.dao.mongotemplate.TaskDao;
import com.tencent.bk.codecc.task.dao.mongotemplate.ToolDao;
import com.tencent.bk.codecc.task.model.AnalyzeCountStatEntity;
import com.tencent.bk.codecc.task.model.CodeLineStatisticEntity;
import com.tencent.bk.codecc.task.model.TaskStatisticEntity;
import com.tencent.bk.codecc.task.model.ToolElapseTimeEntity;
import com.tencent.bk.codecc.task.model.ToolCountScriptEntity;
import com.tencent.bk.codecc.task.model.ToolStatisticEntity;
import com.tencent.bk.codecc.task.service.TaskService;
import com.tencent.devops.common.api.QueryTaskListReqVO;
import com.tencent.devops.common.api.codecc.util.JsonUtil;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.DefectStatType;
import com.tencent.devops.common.constant.RedisKeyConstants;
import com.tencent.devops.common.util.DateTimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.tencent.devops.common.constant.ComConstants.TOTAL_BLANK;
import static com.tencent.devops.common.constant.ComConstants.TOTAL_CODE;
import static com.tencent.devops.common.constant.ComConstants.TOTAL_COMMENT;
import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_ACTIVE_STAT;
import static com.tencent.devops.common.web.mq.ConstantsKt.QUEUE_ACTIVE_STAT;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_ACTIVE_STAT;

/**
 * 活跃统计任务
 *
 * @version V1.0
 * @date 2020/12/10
 */

@Slf4j
@Component
public class ActiveStatisticConsumer {

    @Autowired
    private Client client;
    @Autowired
    private TaskDao taskDao;
    @Autowired
    private ToolDao toolDao;
    @Autowired
    private CommonDao commonDao;
    @Autowired
    private TaskService taskService;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    @Autowired
    private TaskStatisticRepository taskStatisticRepository;
    @Autowired
    private ToolStatisticRepository toolStatisticRepository;
    @Autowired
    private CodeLineStatisticRepository codeLineStatisticRepository;
    @Autowired
    private AnalyzeCountStatRepository analyzeCountStatRepository;
    @Autowired
    private ToolElapseTimeRepository toolElapseTimeRepository;


    // 清理前days天的数据
    private static final int DEL_REDIS_KEY_BEFORE_DAYS = -8;
    private static final int DEL_REDIS_KEY_BEFORE_DAYS_TOOL = -31;
    // 按长度分割列表
    private static final int PARTITION_LENGTH = 5000;


    /**
     * 定时任务统计每日数据
     */
    @RabbitListener(bindings = @QueueBinding(key = ROUTE_ACTIVE_STAT,
            value = @Queue(value = QUEUE_ACTIVE_STAT, durable = "false"),
            exchange = @Exchange(value = EXCHANGE_ACTIVE_STAT, durable = "false")))
    public void consumer() {
        try {
            String date = DateTimeUtils.getDateByDiff(-1);
            log.info("ActiveStatistic begin date: {}", date);
            long endTime = DateTimeUtils.getTimeStampEnd(date);

            List<Long> openSourceTaskIds = taskService.queryTaskIdByType(DefectStatType.GONGFENG_SCAN);
            List<Long> userTaskIds = taskService.queryTaskIdByType(DefectStatType.USER);

            // 统计任务数
            taskStatistic(endTime, date, DefectStatType.GONGFENG_SCAN.value(), openSourceTaskIds);
            taskStatistic(endTime, date, DefectStatType.USER.value(), userTaskIds);
            log.info("taskStatistic finish.");

            // 统计工具数
            String toolOrder = commonDao.getToolOrder();
            toolStatistic(endTime, date, DefectStatType.GONGFENG_SCAN, openSourceTaskIds, toolOrder);
            toolStatistic(endTime, date, DefectStatType.USER, userTaskIds, toolOrder);
            log.info("toolStatistic finish.");

            // 保存执行分析的任务ID来统计执行次数
            statisticToolAnalyzeCount(date, DefectStatType.GONGFENG_SCAN, toolOrder);
            statisticToolAnalyzeCount(date, DefectStatType.USER, toolOrder);

            // 统计工具分析耗时
            toolAnalyzeElapseTimeStat(date, DefectStatType.GONGFENG_SCAN.value(), toolOrder);
            toolAnalyzeElapseTimeStat(date, DefectStatType.USER.value(), toolOrder);
            log.info("toolAnalyzeElapseTimeStat finish.");


            // 统计每日代码行
            codeLineStatistic(date, DefectStatType.GONGFENG_SCAN.value(), openSourceTaskIds);
            codeLineStatistic(date, DefectStatType.USER.value(), userTaskIds);
            log.info("codeLineStatistic finish.");

            // 统计每日新增代码库/代码分支数
            codeRepoStatDaily();
            log.info("codeRepoStatDaily finish.");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * 统计任务数
     *
     * @param endTime   截止时间戳
     * @param date      统计日期
     * @param dataFrom  数据来源范围
     */
    private void taskStatistic(Long endTime, String date, String dataFrom, List<Long> taskIds) {
        TaskStatisticEntity taskStat = taskStatisticRepository.findFirstByDateAndDataFrom(date, dataFrom);
        if (taskStat == null) {
            taskStat = new TaskStatisticEntity();
        }
        taskStat.setDate(date);
        taskStat.setDataFrom(dataFrom);
        // 任务总数
        Long taskCount = taskDao.findDailyTaskCount(endTime, dataFrom);
        taskStat.setTaskCount(taskCount.intValue());

        // 活跃总数
        String key = String.format("%s%s:%s", RedisKeyConstants.PREFIX_ACTIVE_TASK, date, dataFrom);
        Long activeTaskCount = redisTemplate.opsForSet().size(key);
        taskStat.setActiveCount(activeTaskCount.intValue());

        // 执行次数
        QueryTaskListReqVO reqVO = new QueryTaskListReqVO();
        reqVO.setTaskIds(taskIds);
        long[] startTimeAndEndTime = DateTimeUtils.getStartTimeAndEndTime(date, date);
        reqVO.setStartTime(startTimeAndEndTime[0]);
        reqVO.setEndTime(startTimeAndEndTime[1]);
        Integer taskAnalyzes = client.get(ServiceTaskLogOverviewResource.class).getTaskAnalyzeCount(reqVO).getData();
        taskStat.setAnalyzeCount(taskAnalyzes != null ? taskAnalyzes : 0);

        taskStatisticRepository.save(taskStat);
        // 清理冗余数据
        delRedisKeyBefore7Days(RedisKeyConstants.PREFIX_ACTIVE_TASK, dataFrom);
    }

    /**
     * 统计工具数
     *
     * @param endTime   截止时间戳
     * @param date      统计日期
     * @param dataFrom  数据来源范围
     */
    private void toolStatistic(Long endTime, String date, DefectStatType dataFrom, List<Long> taskIdList,
            String toolOrder) {
        if (StringUtils.isEmpty(toolOrder)) {
            log.error("toolOrder is empty!");
            return;
        }
        // 统计工具数
        List<ToolCountScriptEntity> toolCountEntities = toolDao.findDailyToolCount(taskIdList, endTime);
        Map<String, Integer> toolCountMap = toolCountEntities.stream()
                .collect(Collectors.toMap(ToolCountScriptEntity::getToolName, ToolCountScriptEntity::getCount));
        // 获取活跃数
        String key = String.format("%s%s:%s", RedisKeyConstants.PREFIX_ACTIVE_TOOL, date, dataFrom.value());
        Map<Object, Object> toolActiveCountMap = redisTemplate.opsForHash().entries(key);
        if (null == toolActiveCountMap) {
            toolActiveCountMap = Maps.newHashMap();
        }

        Map<String, ToolStatisticEntity> toolStatMap = Maps.newHashMap();
        List<ToolStatisticEntity> entityList = toolStatisticRepository.findByDateAndDataFrom(date, dataFrom.value());
        if (CollectionUtils.isNotEmpty(entityList)) {
            toolStatMap = entityList.stream()
                    .collect(Collectors.toMap(ToolStatisticEntity::getToolName, Function.identity(), (k, v) -> v));
        }

        String[] toolArr = toolOrder.split(ComConstants.STRING_SPLIT);
        // 获取分析成功次数
        List<Object> succObjects = redisTemplate.executePipelined((RedisCallback<Long>) conn -> {
            for (String tool : toolArr) {
                String redisKey = getToolAnalyzeSuccStatKey(date, dataFrom.value(), tool);
                conn.lLen(redisKey.getBytes());
            }
            return null;
        });
        // 获取分析失败次数
        List<Object> failObjects = redisTemplate.executePipelined((RedisCallback<Long>) conn -> {
            for (String tool : toolArr) {
                String redisKey = getToolAnalyzeFailStatKey(date, dataFrom.value(), tool);
                conn.lLen(redisKey.getBytes());
            }
            return null;
        });

        List<ToolStatisticEntity> toolStatisticEntities = Lists.newArrayList();
        for (int i = 0; i < toolArr.length; i++) {
            String toolName = toolArr[i];
            // 统计该工具总分析次数
            Long succCount = (Long) succObjects.get(i);
            Long failCount = (Long) failObjects.get(i);
            int analyzeCount = 0;
            if (null != succCount) {
                analyzeCount += succCount.intValue();
            }
            if (null != failCount) {
                analyzeCount += failCount.intValue();
            }

            ToolStatisticEntity toolStat = toolStatMap.computeIfAbsent(toolName, v -> new ToolStatisticEntity());
            toolStat.setDate(date);
            toolStat.setDataFrom(dataFrom.value());
            toolStat.setToolName(toolName);
            toolStat.setToolCount(toolCountMap.getOrDefault(toolName, 0));
            toolStat.setActiveCount(Integer.parseInt(toolActiveCountMap.getOrDefault(toolName, 0).toString()));
            toolStat.setAnalyzeCount(analyzeCount);

            toolStatisticEntities.add(toolStat);
        }

        toolStatisticRepository.saveAll(toolStatisticEntities);
        // 清理冗余数据
        delRedisKeyBefore7Days(RedisKeyConstants.PREFIX_ACTIVE_TOOL, dataFrom.value());
        delToolRedisKeyBeforeDays(dataFrom.value(), toolArr);
    }

    /**
     * 每天清理前7天的计数器
     *
     * @param keyPrefix Redis key前缀
     * @param dataFrom  统计数据来源
     */
    private void delRedisKeyBefore7Days(String keyPrefix, String dataFrom) {
        String dateByDiff = DateTimeUtils.getDateByDiff(DEL_REDIS_KEY_BEFORE_DAYS);
        redisTemplate.delete(keyPrefix + dateByDiff + RedisKeyConstants.INTERVAL_FLAG + dataFrom);
    }

    /**
     * 清理各个工具的计数器
     */
    private void delToolRedisKeyBeforeDays(String dataFrom, String[] toolArr) {
        String dateByDiff = DateTimeUtils.getDateByDiff(DEL_REDIS_KEY_BEFORE_DAYS_TOOL);
        redisTemplate.executePipelined((RedisCallback<Object>) conn -> {
            for (String tool : toolArr) {
                String key = getToolAnalyzeFailStatKey(dateByDiff, dataFrom, tool);
                conn.del(key.getBytes());
                key = getToolAnalyzeSuccStatKey(dateByDiff, dataFrom, tool);
                conn.del(key.getBytes());
            }
            return null;
        });
    }

    /**
     * 组装分析成功次数的key
     */
    private String getToolAnalyzeSuccStatKey(String date, String dataFrom, String toolName) {
        return String.format("%s%s:%s:%s", RedisKeyConstants.PREFIX_ANALYZE_SUCC_COUNT, date, dataFrom, toolName);
    }

    /**
     * 组装分析失败次数的key
     */
    private String getToolAnalyzeFailStatKey(String date, String dataFrom, String toolName) {
        return String.format("%s%s:%s:%s", RedisKeyConstants.PREFIX_ANALYZE_FAIL_COUNT, date, dataFrom, toolName);
    }

    /**
     * 统计分析代码行
     *
     * @param date     统计日期
     * @param dataFrom 数据来源范围
     */
    private void codeLineStatistic(String date, String dataFrom, List<Long> taskIdList) {
        CodeLineStatisticEntity codeLineStatEntity = new CodeLineStatisticEntity();
        codeLineStatEntity.setDate(date);
        codeLineStatEntity.setDataFrom(dataFrom);

        String key = RedisKeyConstants.CODE_LINE_STAT + date + RedisKeyConstants.INTERVAL_FLAG + dataFrom;
        Map<Object, Object> objObjMap = redisTemplate.opsForHash().entries(key);
        if (null == objObjMap) {
            objObjMap = Maps.newHashMap();
        }
        codeLineStatEntity.setDailyBlank(Long.parseLong(objObjMap.getOrDefault(TOTAL_BLANK, "0").toString()));
        codeLineStatEntity.setDailyComment(Long.parseLong(objObjMap.getOrDefault(TOTAL_COMMENT, "0").toString()));
        codeLineStatEntity.setDailyCode(Long.parseLong(objObjMap.getOrDefault(TOTAL_CODE, "0").toString()));

        long totalCodeLine = 0;
        List<List<Long>> partitionList = Lists.partition(taskIdList, PARTITION_LENGTH);
        try {
            for (List<Long> taskIds : partitionList) {
                QueryTaskListReqVO reqVO = new QueryTaskListReqVO();
                reqVO.setTaskIds(taskIds);
                Long sumCodeLine = client.get(OpDefectRestResource.class).getTaskCodeLineCount(reqVO).getData();
                if (sumCodeLine != null) {
                    totalCodeLine += sumCodeLine;
                }
            }
        } catch (Exception e) {
            log.error("query statistic total code line fail!", e);
        }
        codeLineStatEntity.setSumCode(totalCodeLine);

        codeLineStatisticRepository.save(codeLineStatEntity);
    }

    /**
     * 保存执行分析的任务ID来统计执行次数
     */
    private void statisticToolAnalyzeCount(String date, DefectStatType dataFrom, String toolOrder) {
        if (StringUtils.isEmpty(toolOrder)) {
            log.error("toolOrder is empty!");
            return;
        }
        String[] toolArr = toolOrder.split(ComConstants.STRING_SPLIT);
        // 获取分析成功次数
        List<Object> succObjects = redisTemplate.executePipelined((RedisCallback<Long>) conn -> {
            for (String tool : toolArr) {
                String redisKey = getToolAnalyzeSuccStatKey(date, dataFrom.value(), tool);
                conn.lRange(redisKey.getBytes(), 0, -1);
            }
            return null;
        });
        // 获取分析失败次数
        List<Object> failObjects = redisTemplate.executePipelined((RedisCallback<Long>) conn -> {
            for (String tool : toolArr) {
                String redisKey = getToolAnalyzeFailStatKey(date, dataFrom.value(), tool);
                conn.lRange(redisKey.getBytes(), 0, -1);
            }
            return null;
        });
        // 保存分析成功数据
        saveToolAnalyzeCountEntities(date, dataFrom, toolArr, succObjects, ComConstants.ScanStatus.SUCCESS.getCode());
        // 保存分析失败数据
        saveToolAnalyzeCountEntities(date, dataFrom, toolArr, failObjects, ComConstants.ScanStatus.FAIL.getCode());
    }

    /**
     * 按工具保存分析次数
     *
     * @param succObjects taskId list in redis
     * @param code        scanStatus
     */
    private void saveToolAnalyzeCountEntities(String date, DefectStatType dataFrom, @NotNull String[] toolArr,
            List<Object> succObjects, int code) {
        List<AnalyzeCountStatEntity> analyzeCountStatEntities = Lists.newArrayList();
        for (int i = 0; i < toolArr.length; i++) {
            Object listObj = succObjects.get(i);
            List<Long> taskIdList;
            if (listObj != null) {
                String listStr = listObj.toString();
                taskIdList = JsonUtil.INSTANCE.to(listStr, new TypeReference<List<Long>>() {
                });
            } else {
                taskIdList = Lists.newArrayList();
            }

            AnalyzeCountStatEntity countStatEntity = new AnalyzeCountStatEntity();
            countStatEntity.setDate(date);
            countStatEntity.setStatus(code);
            countStatEntity.setDataFrom(dataFrom.value());
            countStatEntity.setToolName(toolArr[i]);
            countStatEntity.setTaskIdList(taskIdList);

            analyzeCountStatEntities.add(countStatEntity);
        }

        analyzeCountStatRepository.saveAll(analyzeCountStatEntities);
    }

    /**
     * 组装工具分析耗时key
     */
    private String getToolAnalyzeSuccElapseTimeKey(String date, String dataFrom, String scanStatType) {
        return String
                .format("%s%s:%s:%s", RedisKeyConstants.PREFIX_ANALYZE_SUCC_ELAPSE_TIME, date, dataFrom, scanStatType);
    }

    /**
     * 按是否超快增量统计分析耗时
     *
     * @param date      统计日期
     * @param dataFrom  数据来源
     * @param toolOrder 工具顺序
     */
    private void toolAnalyzeElapseTimeStat(String date, String dataFrom, String toolOrder) {


        String[] toolArr = toolOrder.split(ComConstants.STRING_SPLIT);

        List<ToolElapseTimeEntity> toolElapseTimeEntities = Lists.newArrayList();
        for (ComConstants.ScanStatType scanStatType : ComConstants.ScanStatType.values()) {
            // 获取各个工具的总耗时
            String elapseTimeKey = getToolAnalyzeSuccElapseTimeKey(date, dataFrom, scanStatType.getValue());
            Map<Object, Object> toolElapseTimeMap = redisTemplate.opsForHash().entries(elapseTimeKey);
            if (toolElapseTimeMap == null) {
                toolElapseTimeMap = Maps.newHashMap();
            }

            // 成功分析次数
            String succToolKey = String.format("%s%s:%s:%s", RedisKeyConstants.PREFIX_ANALYZE_SUCC_TOOL, date, dataFrom,
                    scanStatType.getValue());
            Map<Object, Object> toolSuccCountMap = redisTemplate.opsForHash().entries(succToolKey);
            if (toolSuccCountMap == null) {
                toolSuccCountMap = Maps.newHashMap();
            }

            // 失败分析次数
            String failToolKey = String.format("%s%s:%s:%s", RedisKeyConstants.PREFIX_ANALYZE_FAIL_TOOL, date, dataFrom,
                    scanStatType.getValue());
            Map<Object, Object> toolFailCountMap = redisTemplate.opsForHash().entries(failToolKey);
            if (toolFailCountMap == null) {
                toolFailCountMap = Maps.newHashMap();
            }

            for (String tool : toolArr) {
                ToolElapseTimeEntity elapseTimeEntity = new ToolElapseTimeEntity();
                elapseTimeEntity.setDate(date);
                elapseTimeEntity.setToolName(tool);
                elapseTimeEntity.setDataFrom(dataFrom);
                elapseTimeEntity.setScanStatType(scanStatType.getValue());

                String elapseTimeStr = toolElapseTimeMap.getOrDefault(tool, "0").toString();
                elapseTimeEntity.setTotalElapseTime(Long.parseLong(elapseTimeStr));

                String succCount = toolSuccCountMap.getOrDefault(tool, "0").toString();
                elapseTimeEntity.setSuccAnalyzeCount(Long.parseLong(succCount));

                String failCount = toolFailCountMap.getOrDefault(tool, "0").toString();
                elapseTimeEntity.setFailAnalyzeCount(Long.parseLong(failCount));

                toolElapseTimeEntities.add(elapseTimeEntity);
            }
        }

        toolElapseTimeRepository.saveAll(toolElapseTimeEntities);
        log.info("toolAnalyzeElapseTimeStat finish.");
    }

    /**
     * 定时任务 初始化每日新增代码库/代码分支数
     */
    private void codeRepoStatDaily() {
        QueryTaskListReqVO reqVO = new QueryTaskListReqVO();
        // 传递参数 2 表示查询前一天的数据进行初始化
        reqVO.setInitDay(2);
        Boolean isSuccess = client.get(OpDefectRestResource.class).initCodeRepoStatTrend(reqVO).getData();
        if (!isSuccess) {
            log.error("query code repo stat daily fail!");
        }
    }
}
