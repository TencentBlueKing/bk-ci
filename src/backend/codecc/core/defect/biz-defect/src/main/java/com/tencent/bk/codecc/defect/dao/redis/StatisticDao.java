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

package com.tencent.bk.codecc.defect.dao.redis;

import com.google.common.base.CaseFormat;
import com.google.common.collect.Maps;
import com.tencent.bk.codecc.defect.dao.mongorepository.CheckerRepository;
import com.tencent.bk.codecc.defect.model.CheckerDetailEntity;
import com.tencent.bk.codecc.defect.model.CheckerStatisticEntity;
import com.tencent.bk.codecc.defect.model.CommonStatisticEntity;
import com.tencent.devops.common.constant.RedisKeyConstants;
import com.tencent.devops.common.util.ObjectDynamicCreator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import static com.tencent.devops.common.constant.ComConstants.StaticticItem;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 临时统计信息的持久化
 *
 * @date 2019/10/23
 * @version V1.0
 */
@Repository
@Slf4j
public class StatisticDao {
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private CheckerRepository checkerRepository;

    public static final String NEW_AUTHORS = "NEW_AUTHORS";
    public static final String EXIST_AUTHORS = "EXIST_AUTHORS";

    /**
     * 分别统计每次分析各状态告警的数量
     *
     * @param taskId
     * @param toolName
     * @param buildNum
     * @param defectCountList
     * @return
     */
    public void increaseDefectCountByStatusBatch(long taskId,
                                                 String toolName,
                                                 String buildNum,
                                                 List<Pair<StaticticItem, Integer>> defectCountList) {

        redisTemplate.executePipelined((RedisCallback<Long>) connection -> {
            defectCountList.forEach(pair -> {
                String key = String.format("%s%d:%s:%s:%s",
                    RedisKeyConstants.PREFIX_TMP_STATISTIC, taskId, toolName, buildNum, pair.getKey().name());
                connection.incrBy(key.getBytes(), pair.getValue());
                connection.expire(key.getBytes(), 48 * 3600);
                log.info("increase defect count by status batch: {}", key);
            });
            return null;
        });
    }

    /**
     * 分别统计每次分析各规则告警的数量
     *
     * @param taskId
     * @param toolName
     * @param buildNum
     * @param checkerCountMap
     * @return
     */
    public void increaseDefectCheckerCountBatch(long taskId,
                                                String toolName,
                                                String buildNum,
                                                Map<String, Integer> checkerCountMap) {
        log.info("taskId:{},  toolName:{}, buildNum:{}, checkerCountMap.size: {}",
            taskId, toolName, buildNum, checkerCountMap.size());

        if (MapUtils.isEmpty(checkerCountMap)) {
            log.info("increase defect checker count batch map is empty, do not thing for task: {}, {}, {}",
                taskId, toolName, buildNum);
            return;
        }

        redisTemplate.executePipelined((RedisCallback<Long>) connection -> {
            checkerCountMap.forEach((checkerName, count) -> {
                String key = getCheckerTmpStatisticPrefixKey(taskId, toolName, buildNum) + checkerName;
                connection.incrBy(key.getBytes(), count);
                connection.expire(key.getBytes(), 48 * 3600);
                log.info("increase defect checker count batch： {}", key);
            });
            return null;
        });

        // add all checker to a key for scan later
        String summaryKey = getCheckerTmpSummaryStatisticKey(taskId, toolName, buildNum);
        String[] arr = checkerCountMap.keySet().toArray(new String[0]);
        redisTemplate.opsForSet().add(summaryKey, arr);
        redisTemplate.expire(summaryKey, 48 * 3600, TimeUnit.SECONDS);
    }

    private String getCheckerTmpStatisticPrefixKey(long taskId,
                                                  String toolName,
                                                  String buildNum) {
        return String.format("%s%d:%s:%s:", RedisKeyConstants.PREFIX_CHECKER_TMP_STATISTIC,
            taskId, toolName, buildNum);
    }

    private String getCheckerTmpSummaryStatisticKey(long taskId,
                                            String toolName,
                                            String buildNum) {
        return String.format("%s%d:%s:%s",
            RedisKeyConstants.PREFIX_CHECKER_SUMMARY_TMP_STATISTIC, taskId, toolName, buildNum);
    }

    /**
     * 分别统计每次分析各状态告警的数量
     *
     * @param statisticEntity
     * @param buildNum
     * @return
     */
    public void getAndClearDefectStatistic(CommonStatisticEntity statisticEntity, String buildNum) {
        List<String> keyList = new ArrayList<>();
        String key = String.format("%s%d:%s:%s:",
            RedisKeyConstants.PREFIX_TMP_STATISTIC, statisticEntity.getTaskId(),
            statisticEntity.getToolName(), buildNum);
        for (StaticticItem item : StaticticItem.values()) {
            keyList.add(key + item.name());
        }

        List<String> res = redisTemplate.opsForValue().multiGet(keyList);
        Set<String> newAuthors = redisTemplate.opsForSet().members(key + NEW_AUTHORS);
        Set<String> existAuthors = redisTemplate.opsForSet().members(key + EXIST_AUTHORS);
        log.info("get and clear defect statistic for taskId:{},  toolName:{}, "
                + "buildNum:{}, defect count:{}, newAuthors{}, existAuthors{}",
            statisticEntity.getTaskId(), statisticEntity.getToolName(),
            buildNum, res.toString(), newAuthors, existAuthors);
        Map<String, String> statisticMap = Maps.newHashMap();
        StaticticItem[] items = StaticticItem.values();
        for (int i = 0; i < items.length; i++) {
            String statisticKey = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, items[i].name()) + "Count";
            statisticMap.put(statisticKey, res.get(i));
        }
        CommonStatisticEntity newStatistic =
            ObjectDynamicCreator.setFieldValueBySetMethod(statisticMap, CommonStatisticEntity.class);
        ObjectDynamicCreator.copyNonNullPropertiesBySetMethod(
            newStatistic, statisticEntity, CommonStatisticEntity.class);

        statisticEntity.setNewAuthors(newAuthors);
        statisticEntity.setExistAuthors(existAuthors);

        // 获取规则集分类数据
        statisticEntity.setCheckerStatistic(getCheckerStatistic(statisticEntity, buildNum));

        // 获取完后删除临时key
        redisTemplate.delete(keyList);
    }

    private List<CheckerStatisticEntity> getCheckerStatistic(CommonStatisticEntity statisticEntity, String buildNum) {
        String toolName = statisticEntity.getToolName();
        List<String> keyList = new ArrayList<>();

        // get checker map
        String checkerSummaryKey = getCheckerTmpSummaryStatisticKey(statisticEntity.getTaskId(), toolName, buildNum);
        List<String> checkerIds = new ArrayList<>();
        Set<String> checkerIdSet = getCheckerCountMapFromRedis(checkerSummaryKey);
        if (CollectionUtils.isNotEmpty(checkerIdSet)) {
            checkerIds.addAll(checkerIdSet);
        }

        String checkerKeyPrefix = getCheckerTmpStatisticPrefixKey(statisticEntity.getTaskId(), toolName, buildNum);
        Map<String, CheckerDetailEntity> checkerDetailMap = new HashMap<>();
        checkerRepository.findByToolNameAndCheckerKeyIn(toolName, checkerIdSet)
            .forEach(it -> checkerDetailMap.put(it.getCheckerKey(), it));

        // get all checker count data
        Map<String, CheckerStatisticEntity> checkerStatisticEntityMap = new HashMap<>();
        log.info("start to get count result: {}, {}", statisticEntity.getTaskId(), buildNum);
        List<Object> countResult = redisTemplate.executePipelined((RedisCallback<Long>) connection -> {
            for (String checkerName : checkerIds) {
                String checkerKey = checkerKeyPrefix + checkerName;
                keyList.add(checkerKey);
                connection.get(checkerKey.getBytes());
            }
            return null;
        });
        log.info("finish get count result: {}, {}, {}", statisticEntity.getTaskId(), buildNum, countResult.size());

        for (int i = 0; i < checkerIds.size(); i++) {
            String checkerName = checkerIds.get(i);
            CheckerDetailEntity checker = checkerDetailMap.get(checkerName);
            CheckerStatisticEntity item = new CheckerStatisticEntity();

            item.setName(checkerName);

            if (checker != null) {
                item.setId(checker.getEntityId());
                item.setName(checker.getCheckerName());
                item.setSeverity(checker.getSeverity());
            } else {
                log.warn("not found checker for tool: {}, {}", toolName, checkerName);
            }

            String defectCount = (String) countResult.get(i);
            if (NumberUtils.isNumber(defectCount)) {
                item.setDefectCount(Integer.parseInt(defectCount));
            }
            checkerStatisticEntityMap.put(checkerName, item);
        }

        log.info("finish execute pipeline: {}, {}, {}",
            statisticEntity.getTaskId(), buildNum, checkerStatisticEntityMap.size());

        // 处理完后删除临时key
        redisTemplate.delete(keyList);

        return new ArrayList<>(checkerStatisticEntityMap.values());
    }

    private Set<String> getCheckerCountMapFromRedis(String summaryKey) {
        log.info("start to get checker count map from redis: {}", summaryKey);
        try {
            return redisTemplate.opsForSet().members(summaryKey);
        } catch (Throwable t) {
            log.error("get checker statistic data from cursor fail for task", t);
        }
        return new HashSet<>();
    }

    /**
     * 缓存新增告警和遗留告警作者列表
     *
     * @param taskId
     * @param toolName
     * @param buildNum
     * @param newAuthors
     * @param existAuthors
     */
    public void addNewAndExistAuthors(long taskId,
                                      String toolName,
                                      String buildNum,
                                      Set<String> newAuthors,
                                      Set<String> existAuthors) {
        String key = String.format("%s%d:%s:%s:", RedisKeyConstants.PREFIX_TMP_STATISTIC, taskId, toolName, buildNum);
        if (CollectionUtils.isNotEmpty(newAuthors)) {
            redisTemplate.opsForSet().add(key + NEW_AUTHORS, newAuthors.toArray(new String[0]));
        }
        if (CollectionUtils.isNotEmpty(existAuthors)) {
            redisTemplate.opsForSet().add(key + EXIST_AUTHORS, existAuthors.toArray(new String[0]));
        }
    }
}
