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
import com.tencent.bk.codecc.defect.model.CommonStatisticEntity;
import com.tencent.devops.common.constant.RedisKeyConstants;
import com.tencent.devops.common.util.ObjectDynamicCreator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import static com.tencent.devops.common.constant.ComConstants.StaticticItem;

import java.util.*;

/**
 * 临时统计信息的持久化
 *
 * @date 2019/10/23
 * @version V1.0
 */
@Repository
@Slf4j
public class StatisticDao
{
    @Autowired
    private StringRedisTemplate redisTemplate;

    public static final String NEW_AUTHORS = "NEW_AUTHORS";
    public static final String EXIST_AUTHORS = "EXIST_AUTHORS";

    /**
     * 分别统计每次分析各状态告警的数量
     *
     * @param taskId
     * @param toolName
     * @param buildNum
     * @param staticticItem
     * @param count
     * @return
     */
    public void increaseDefectCountByStatus(long taskId, String toolName, String buildNum, StaticticItem staticticItem, long count)
    {
        String key = String.format("%s%d:%s:%s:%s", RedisKeyConstants.PREFIX_TMP_STATISTIC, taskId, toolName, buildNum, staticticItem.name());
        Long currCount = redisTemplate.opsForValue().increment(key, count);
        log.info("taskId:{},  toolName:{}, buildNum:{}, type:{}, currCount:{}", taskId, toolName, buildNum, staticticItem.name(), currCount);
    }

    /**
     * 分别统计每次分析各状态告警的数量
     *
     * @param statisticEntity
     * @param buildNum
     * @return
     */
    public void getAndClearDefectStatistic(CommonStatisticEntity statisticEntity, String buildNum)
    {
        List<String> keyList = new ArrayList<>();
        String key = String.format("%s%d:%s:%s:", RedisKeyConstants.PREFIX_TMP_STATISTIC, statisticEntity.getTaskId(), statisticEntity.getToolName(), buildNum);
        for (StaticticItem item : StaticticItem.values())
        {
            keyList.add(key + item.name());
        }
        List<String> res = redisTemplate.opsForValue().multiGet(keyList);
        Set<String> newAuthors = redisTemplate.opsForSet().members(key + NEW_AUTHORS);
        Set<String> existAuthors = redisTemplate.opsForSet().members(key + EXIST_AUTHORS);
        log.info("taskId:{},  toolName:{}, buildNum:{}, defect count:{}, newAuthors{}, existAuthors{}", statisticEntity.getTaskId(),
                statisticEntity.getToolName(), buildNum, res.toString(), newAuthors, existAuthors);
        Map<String, String> staticticMap = Maps.newHashMap();
        StaticticItem[] items = StaticticItem.values();
        for (int i = 0; i < items.length; i++)
        {
            String statisticKey = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, items[i].name()) + "Count";
            staticticMap.put(statisticKey, res.get(i));
        }
        CommonStatisticEntity newStatistic = ObjectDynamicCreator.setFieldValueBySetMethod(staticticMap, CommonStatisticEntity.class);
        ObjectDynamicCreator.copyNonNullPropertiesBySetMethod(newStatistic, statisticEntity, CommonStatisticEntity.class);

        statisticEntity.setNewAuthors(newAuthors);
        statisticEntity.setExistAuthors(existAuthors);

        // 获取完后删除临时key
        redisTemplate.delete(keyList);
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
    public void addNewAndExistAuthors(long taskId, String toolName, String buildNum, Set<String> newAuthors, Set<String> existAuthors)
    {
        String key = String.format("%s%d:%s:%s:", RedisKeyConstants.PREFIX_TMP_STATISTIC, taskId, toolName, buildNum);
        if (CollectionUtils.isNotEmpty(newAuthors))
        {
            redisTemplate.opsForSet().add(key + NEW_AUTHORS, newAuthors.toArray(new String[newAuthors.size()]));
        }
        if (CollectionUtils.isNotEmpty(existAuthors))
        {
            redisTemplate.opsForSet().add(key + EXIST_AUTHORS, existAuthors.toArray(new String[existAuthors.size()]));
        }
    }
}
