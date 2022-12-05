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

package com.tencent.bk.codecc.codeccjob.consumer;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.codeccjob.dao.ToolMetaCacheServiceImpl;
import com.tencent.bk.codecc.codeccjob.dao.mongorepository.CheckerDefectStatRepository;
import com.tencent.bk.codecc.codeccjob.dao.mongorepository.CheckerRepository;
import com.tencent.bk.codecc.codeccjob.dao.mongotemplate.CheckerSetDao;
import com.tencent.bk.codecc.codeccjob.dao.mongotemplate.CheckerSetTaskRelationshipDao;
import com.tencent.bk.codecc.codeccjob.dao.mongotemplate.DefectDao;
import com.tencent.bk.codecc.codeccjob.dao.mongotemplate.LintDefectV2Dao;
import com.tencent.bk.codecc.defect.model.CheckerDefectStatEntity;
import com.tencent.bk.codecc.defect.model.CheckerDetailEntity;
import com.tencent.bk.codecc.defect.model.CheckerStatisticEntity;
import com.tencent.bk.codecc.defect.model.CheckerStatisticExtEntity;
import com.tencent.bk.codecc.defect.model.checkerset.CheckerPropsEntity;
import com.tencent.bk.codecc.defect.model.checkerset.CheckerSetEntity;
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.bk.codecc.task.api.ServiceToolRestResource;
import com.tencent.bk.codecc.task.pojo.RefreshCheckerDefectStatModel;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.ToolPattern;
import com.tencent.devops.common.constant.RedisKeyConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_CHECKER_DEFECT_STAT;
import static com.tencent.devops.common.web.mq.ConstantsKt.QUEUE_CHECKER_DEFECT_STAT;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_CHECKER_DEFECT_STAT;

/**
 * 规则告警统计消费者
 *
 * @version V1.0
 * @date 2020/11/13
 */
@Slf4j
@Component
public class CheckerDefectStatConsumer {

    @Autowired
    private Client client;
    @Autowired
    private ToolMetaCacheServiceImpl toolMetaCache;
    @Autowired
    private CheckerRepository checkerRepository;
    @Autowired
    private CheckerSetDao checkerSetDao;
    @Autowired
    private LintDefectV2Dao lintDefectDao;
    @Autowired
    private DefectDao defectDao;
    @Autowired
    private CheckerDefectStatRepository checkerDefectStatRepository;
    @Autowired
    private CheckerSetTaskRelationshipDao checkerSetTaskRelationshipDao;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;


    /**
     * 定时按规则统计各种状态的告警数
     * @param model model
     */
    @RabbitListener(bindings = @QueueBinding(key = ROUTE_CHECKER_DEFECT_STAT,
            value = @Queue(value = QUEUE_CHECKER_DEFECT_STAT),
            exchange = @Exchange(value = EXCHANGE_CHECKER_DEFECT_STAT)))
    public void consumer(RefreshCheckerDefectStatModel model) {
        Assert.notNull(model, "RefreshCheckerDefectStatModel must not be null!");
        String dataFrom = model.getDataFrom();
        // 判断统计数据来源
        List<String> createFrom;
        if (ComConstants.DefectStatType.GONGFENG_SCAN.value().equals(dataFrom)) {
            createFrom = Lists.newArrayList(ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value());
        } else if (ComConstants.DefectStatType.ALL.value().equals(dataFrom)) {
            createFrom = Lists.newArrayList(ComConstants.BsTaskCreateFrom.BS_CODECC.value(),
                    ComConstants.BsTaskCreateFrom.BS_PIPELINE.value(),
                    ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value());
        } else {
            createFrom = Lists.newArrayList(ComConstants.BsTaskCreateFrom.BS_CODECC.value(),
                    ComConstants.BsTaskCreateFrom.BS_PIPELINE.value());
        }

        // 获取任务ID列表
        Result<List<Long>> result = client.get(ServiceTaskRestResource.class).queryTaskIdByCreateFrom(createFrom);
        List<Long> taskIdList = result.getData();
        if (CollectionUtils.isEmpty(taskIdList)) {
            log.info("taskIdList is empty!");
            return;
        }

        // 获取工具列表
        String toolOrderStr = client.get(ServiceToolRestResource.class).findToolOrder().getData();
        Assert.notNull(toolOrderStr, "tool data must not be null!");
        List<String> toolList = Lists.newArrayList(toolOrderStr.split(ComConstants.STRING_SPLIT));

        // 忽略工具列表
        ArrayList<String> commonPatternList =
                Lists.newArrayList(ToolPattern.COVERITY.name(), ToolPattern.KLOCWORK.name(),
                        ToolPattern.PINPOINT.name());
        // 定义告警状态
        int exist = ComConstants.DefectStatus.NEW.value();
        int fixed = exist | ComConstants.DefectStatus.FIXED.value();
        int ignore = exist | ComConstants.DefectStatus.IGNORE.value();
        int exclude = exist | ComConstants.DefectStatus.PATH_MASK.value();
        int exclude2 = exist | ComConstants.DefectStatus.CHECKER_MASK.value();

        for (String toolName : toolList) {
            List<CheckerDetailEntity> checkerDetailEntities = checkerRepository.findByToolName(toolName);
            if (CollectionUtils.isEmpty(checkerDetailEntities)) {
                log.info("tool checker detail is empty: {}", toolName);
                continue;
            }

            // 创建时间
            Map<String, Long> checkerCreateTimeMap = Maps.newHashMap();
            for (CheckerDetailEntity checkerDetailEntity : checkerDetailEntities) {
                Long createdDate = checkerDetailEntity.getCreatedDate();
                if (createdDate == null) {
                    createdDate = 0L;
                }
                checkerCreateTimeMap.put(checkerDetailEntity.getCheckerKey(), createdDate);
            }

            List<String> checkerKeyList = Lists.newArrayList(checkerCreateTimeMap.keySet());
            Map<String, Integer> checkerUsageMap = getCheckerUsageMap(taskIdList, checkerKeyList);

            String toolPattern = toolMetaCache.getToolPattern(toolName);
            try {
                log.info("waiting 0.5s!");
                Thread.sleep(500L);
            } catch (InterruptedException e) {
                log.error("thread sleep fail!", e);
            }

            // 分批统计各状态规则告警数
            List<CheckerStatisticEntity> existStatEntities;
            List<CheckerStatisticEntity> fixedStatEntities;
            List<CheckerStatisticEntity> ignoreStatEntities;
            List<CheckerStatisticEntity> excludeStatEntities;
            if (ToolPattern.LINT.name().equals(toolPattern)) {
                existStatEntities =
                        lintDefectDao.findStatByTaskIdAndToolChecker(taskIdList, toolName, exist, checkerKeyList);
                fixedStatEntities =
                        lintDefectDao.findStatByTaskIdAndToolChecker(taskIdList, toolName, fixed, checkerKeyList);
                ignoreStatEntities =
                        lintDefectDao.findStatByTaskIdAndToolChecker(taskIdList, toolName, ignore, checkerKeyList);
                excludeStatEntities =
                        lintDefectDao.findStatByTaskIdAndToolChecker(taskIdList, toolName, exclude, checkerKeyList);

            } else if ((commonPatternList.contains(toolPattern))) {
                existStatEntities = defectDao
                        .findStatByTaskIdAndToolChecker(taskIdList, toolName, Lists.newArrayList(exist),
                                checkerKeyList);
                fixedStatEntities = defectDao
                        .findStatByTaskIdAndToolChecker(taskIdList, toolName, Lists.newArrayList(fixed),
                                checkerKeyList);
                ignoreStatEntities = defectDao
                        .findStatByTaskIdAndToolChecker(taskIdList, toolName, Lists.newArrayList(ignore),
                                checkerKeyList);
                excludeStatEntities = defectDao
                        .findStatByTaskIdAndToolChecker(taskIdList, toolName, Lists.newArrayList(exclude, exclude2),
                                checkerKeyList);
            } else {
                log.info("the other tool checker is no need for statistics: {}", toolName);
                continue;
            }
            Map<String, Integer> existCountMap = existStatEntities.stream()
                    .collect(Collectors.toMap(CheckerStatisticEntity::getId, CheckerStatisticEntity::getDefectCount));
            Map<String, Integer> fixedCountMap = fixedStatEntities.stream()
                    .collect(Collectors.toMap(CheckerStatisticEntity::getId, CheckerStatisticEntity::getDefectCount));
            Map<String, Integer> ignoreCountMap = ignoreStatEntities.stream()
                    .collect(Collectors.toMap(CheckerStatisticEntity::getId, CheckerStatisticEntity::getDefectCount));
            Map<String, Integer> excludeCountMap = excludeStatEntities.stream()
                    .collect(Collectors.toMap(CheckerStatisticEntity::getId, CheckerStatisticEntity::getDefectCount));

            List<CheckerDefectStatEntity> dataList =
                    generateCheckerDefectStatEntities(toolName, dataFrom, checkerUsageMap, checkerKeyList,
                            existCountMap, fixedCountMap, ignoreCountMap, excludeCountMap, checkerCreateTimeMap);

            checkerDefectStatRepository.saveAll(dataList);
        }
        // 记录更新时间
        redisTemplate.opsForValue()
                .set(RedisKeyConstants.CHECKER_DEFECT_STAT_TIME, String.valueOf(System.currentTimeMillis()));
    }

    /**
     * 组装生成规则告警统计实体
     *
     * @param toolName        工具
     * @param dataFrom        统计数据来源 enum DefectStatType
     * @param checkerUsageMap 规则使用量
     * @param checkers        规则列表
     * @param existCountMap   待修复数
     * @param fixedCountMap   已修复数
     * @param ignoreCountMap  已忽略数
     * @param excludeCountMap 已屏蔽数
     * @param checkerCreateTimeMap 规则创建时间
     * @return list
     */
    @NotNull
    private List<CheckerDefectStatEntity> generateCheckerDefectStatEntities(String toolName, String dataFrom,
            Map<String, Integer> checkerUsageMap, @NotNull List<String> checkers, Map<String, Integer> existCountMap,
            Map<String, Integer> fixedCountMap, Map<String, Integer> ignoreCountMap,
            Map<String, Integer> excludeCountMap, Map<String, Long> checkerCreateTimeMap) {
        long timeMillis = System.currentTimeMillis();
        List<CheckerDefectStatEntity> dataList = Lists.newArrayList();
        for (String checker : checkers) {
            int existCount = MapUtils.getIntValue(existCountMap, checker);
            int fixedCount = MapUtils.getIntValue(fixedCountMap, checker);
            int ignoreCount = MapUtils.getIntValue(ignoreCountMap, checker);
            int excludeCount = MapUtils.getIntValue(excludeCountMap, checker);

            CheckerDefectStatEntity statEntity = new CheckerDefectStatEntity();
            statEntity.setStatDate(timeMillis);
            statEntity.setToolName(toolName);
            statEntity.setCheckerName(checker);
            statEntity.setCheckerCreatedDate(MapUtils.getLongValue(checkerCreateTimeMap, checker));
            statEntity.setDataFrom(dataFrom);
            statEntity.setOpenCheckerTaskCount(MapUtils.getIntValue(checkerUsageMap, checker));
            statEntity.setDefectTotalCount(existCount + fixedCount + ignoreCount + excludeCount);
            statEntity.setExistCount(existCount);
            statEntity.setFixedCount(fixedCount);
            statEntity.setIgnoreCount(ignoreCount);
            statEntity.setExcludedCount(excludeCount);
            dataList.add(statEntity);
        }
        return dataList;
    }

    // 批量统计各规则使用量
    @NotNull
    private Map<String, Integer> getCheckerUsageMap(List<Long> taskIdList, List<String> checkers) {
        Set<String> checkerSetIdSet = Sets.newHashSet();
        List<CheckerSetEntity> checkerSetEntities = checkerSetDao.findByCheckerNameList(checkers);
        Map<String, Set<String>> checkerSetIdMap = Maps.newHashMap();
        for (CheckerSetEntity checkerSet : checkerSetEntities) {
            List<CheckerPropsEntity> checkerProps = checkerSet.getCheckerProps();
            checkerProps.forEach(checkerPropsEntity -> {
                String checkerKey = checkerPropsEntity.getCheckerKey();
                if (checkers.contains(checkerKey)) {
                    Set<String> checkerSetIds = checkerSetIdMap.computeIfAbsent(checkerKey, v -> Sets.newHashSet());
                    checkerSetIds.add(checkerSet.getCheckerSetId());
                    checkerSetIdSet.add(checkerSet.getCheckerSetId());
                }
            });
        }

        List<CheckerStatisticExtEntity> checkerSetTaskRelationships =
                checkerSetTaskRelationshipDao.findTaskIdByCheckerSetIds(checkerSetIdSet, taskIdList);
        Map<String, Set<Long>> checkerSetUsageMap = checkerSetTaskRelationships.stream()
                .collect(Collectors.toMap(CheckerStatisticExtEntity::getId, CheckerStatisticExtEntity::getTaskInUse));

        Map<String, Integer> checkerUsageMap = Maps.newHashMap();
        for (Map.Entry<String, Set<String>> entry : checkerSetIdMap.entrySet()) {
            String checkerKey = entry.getKey();
            Set<String> checkerSetIds = entry.getValue();
            if (CollectionUtils.isEmpty(checkerSetIds)) {
                checkerUsageMap.put(checkerKey, 0);
                log.info("checker is not in any checkerSet: {}", checkerKey);
                continue;
            }

            Set<Long> taskInUseSet = Sets.newHashSet();
            for (String checkerSetId : checkerSetIds) {
                Set<Long> taskIdSet = checkerSetUsageMap.get(checkerSetId);
                if (CollectionUtils.isNotEmpty(taskIdSet)) {
                    taskInUseSet.addAll(taskIdSet);
                }
            }

            checkerUsageMap.put(checkerKey, taskInUseSet.size());
        }
        return checkerUsageMap;
    }

}
