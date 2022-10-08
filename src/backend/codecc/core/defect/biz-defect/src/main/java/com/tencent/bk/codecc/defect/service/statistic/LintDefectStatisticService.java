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
 
package com.tencent.bk.codecc.defect.service.statistic;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tencent.bk.codecc.defect.dao.mongorepository.CheckerRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.LintStatisticRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.ToolBuildInfoRepository;
import com.tencent.bk.codecc.defect.model.*;
import com.tencent.bk.codecc.defect.model.incremental.ToolBuildInfoEntity;
import com.tencent.bk.codecc.defect.model.incremental.ToolBuildStackEntity;
import com.tencent.bk.codecc.defect.service.newdefectjudge.NewDefectJudgeService;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.devops.common.constant.ComConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_CLOSE_DEFECT_STATISTIC_LINT;
import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_CLOSE_DEFECT_STATISTIC_LINT_OPENSOURCE;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_CLOSE_DEFECT_STATISTIC_LINT;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_CLOSE_DEFECT_STATISTIC_LINT_OPENSOURCE;

/**
 * Lint告警统计
 *
 * @date 2020/8/13
 * @version V1.0
 */
@Component
@Slf4j
public class LintDefectStatisticService
{
    @Autowired
    private ToolBuildInfoRepository toolBuildInfoRepository;
    @Autowired
    private NewDefectJudgeService newDefectJudgeService;
    @Autowired
    private LintStatisticRepository lintStatisticRepository;
    @Autowired
    private CheckerRepository checkerRepository;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 统计本次扫描的告警，需要统计的信息：
     * 1.本次分析遗留告警总数，文件总数，用于跟上一次分析的结果比较，得到最近一次分析结果，用于项目详情页展示，例如： 告警88247(↑38) 文件1796(↑0)
     * 2.当前遗留新告警数，历史告警数，用于数据报表统计每日告警遗留趋势图
     *  @param taskVO
     * @param toolName
     * @param buildId
     * @param toolBuildStackEntity
     * @param allDefectEntityList
     */
    public void statistic(TaskDetailVO taskVO, String toolName, String buildId, ToolBuildStackEntity toolBuildStackEntity, List<LintDefectV2Entity> allDefectEntityList)
    {
        long taskId = taskVO.getTaskId();
        //计算总的告警数
        Long totalDefectCount = 0L;
        int defectCount = 0;
        int fileCount;
        int newDefectCount = 0;
        int historyDefectCount = 0;
        int totalNewSerious = 0;
        int totalNewNormal = 0;
        int totalNewPrompt = 0;
        //modified by neildwu 2020-06-01 增加历史各严重等级告警的数量统计
        int totalSerious = 0;
        int totalNormal = 0;
        int totalPrompt = 0;
        Set<String> filePathSet = new HashSet<>();
        Map<String, NotRepairedAuthorEntity> authorDefectMap = Maps.newHashMap();
        Map<String, NotRepairedAuthorEntity> existAuthorMap = Maps.newHashMap();
        if (CollectionUtils.isNotEmpty(allDefectEntityList))
        {
            totalDefectCount = Long.valueOf(allDefectEntityList.size());

            // 查询新老告警判定时间
            long newDefectJudgeTime = newDefectJudgeService.getNewDefectJudgeTime(taskId, taskVO);
            for (LintDefectV2Entity defect : allDefectEntityList)
            {
                if (defect.getStatus() != ComConstants.DefectStatus.NEW.value())
                {
                    continue;
                }
                filePathSet.add(defect.getFilePath());
                if (defect.getLineUpdateTime() >= newDefectJudgeTime)
                {
                    // 获取作者告警数统计
                    NotRepairedAuthorEntity notRepairedAuthorEntity = authorDefectMap.get(defect.getAuthor());
                    if (StringUtils.isNotEmpty(defect.getAuthor()) && notRepairedAuthorEntity == null)
                    {
                        notRepairedAuthorEntity = new NotRepairedAuthorEntity();
                        notRepairedAuthorEntity.setName(defect.getAuthor());
                        authorDefectMap.put(defect.getAuthor(), notRepairedAuthorEntity);
                    }

                    // 统计新增告警数
                    newDefectCount++;

                    // 统计新增严重告警数
                    if (ComConstants.SERIOUS == defect.getSeverity())
                    {
                        totalNewSerious++;
                        totalSerious++;
                        if (notRepairedAuthorEntity != null)
                        {
                            notRepairedAuthorEntity.setSeriousCount(notRepairedAuthorEntity.getSeriousCount() + 1);
                        }
                    }

                    // 统计新增一般告警数
                    else if (ComConstants.NORMAL == defect.getSeverity())
                    {
                        totalNewNormal++;
                        totalNormal++;
                        if (notRepairedAuthorEntity != null)
                        {
                            notRepairedAuthorEntity.setNormalCount(notRepairedAuthorEntity.getNormalCount() + 1);
                        }
                    }

                    // 统计新增提示告警数
                    else if (ComConstants.PROMPT_IN_DB == defect.getSeverity() || ComConstants.PROMPT == defect.getSeverity())
                    {
                        totalNewPrompt++;
                        totalPrompt++;
                        if (notRepairedAuthorEntity != null)
                        {
                            notRepairedAuthorEntity.setPromptCount(notRepairedAuthorEntity.getPromptCount() + 1);
                        }
                    }
                    // 统计用户新增告警数总和
                    if (notRepairedAuthorEntity != null)
                    {
                        notRepairedAuthorEntity.setTotalCount(newDefectCount);
                    }
                }
                else
                {
                    NotRepairedAuthorEntity existAuthorEntity = existAuthorMap.get(defect.getAuthor());
                    if (StringUtils.isNotEmpty(defect.getAuthor()) && existAuthorEntity == null) {
                        existAuthorEntity = new NotRepairedAuthorEntity();
                        existAuthorEntity.setName(defect.getAuthor());
                        existAuthorMap.put(defect.getAuthor(), existAuthorEntity);
                    }

                    historyDefectCount++;
                    if (ComConstants.SERIOUS == defect.getSeverity()) {
                        totalSerious++;
                        if (existAuthorEntity != null) {
                            existAuthorEntity.setSeriousCount(existAuthorEntity.getSeriousCount() + 1);
                        }
                    } else if (ComConstants.NORMAL == defect.getSeverity()) {
                        totalNormal++;
                        if (existAuthorEntity != null) {
                            existAuthorEntity.setNormalCount(existAuthorEntity.getNormalCount() + 1);
                        }
                    } else if (ComConstants.PROMPT_IN_DB == defect.getSeverity()
                            || ComConstants.PROMPT == defect.getSeverity()) {
                        totalPrompt++;
                        if (existAuthorEntity != null) {
                            existAuthorEntity.setPromptCount(existAuthorEntity.getPromptCount() + 1);
                        }
                    }

                    if (existAuthorEntity != null) {
                        existAuthorEntity.setTotalCount(historyDefectCount);
                    }
                }
                defectCount ++;
            }
        }

        // 作者关联告警统计信息按告警数量排序
        List<NotRepairedAuthorEntity> authorDefects = Lists.newArrayList(authorDefectMap.values());
        Collections.sort(authorDefects, (o1, o2) -> Integer.compare(o2.getTotalCount(), o1.getTotalCount()));

        String baseBuildId;
        if (toolBuildStackEntity == null)
        {
            ToolBuildInfoEntity toolBuildINfoEntity = toolBuildInfoRepository.findFirstByTaskIdAndToolName(taskId, toolName);
            baseBuildId = toolBuildINfoEntity != null && StringUtils.isNotEmpty(toolBuildINfoEntity.getDefectBaseBuildId()) ? toolBuildINfoEntity.getDefectBaseBuildId() : "";
        }
        else
        {
            baseBuildId = StringUtils.isNotEmpty(toolBuildStackEntity.getBaseBuildId()) ? toolBuildStackEntity.getBaseBuildId() : "";
        }

        // 保存本次分析的统计情况
        fileCount = filePathSet.size();
        int defectChange;
        int fileChange;
        LintStatisticEntity lastLintStatisticEntity = lintStatisticRepository.findFirstByTaskIdAndToolNameAndBuildId(taskId, toolName, baseBuildId);
        if (lastLintStatisticEntity == null)
        {
            defectChange = defectCount;
            fileChange = fileCount;
        }
        else
        {
            defectChange = defectCount - (lastLintStatisticEntity.getDefectCount() == null ? 0 : lastLintStatisticEntity.getDefectCount());
            fileChange = fileCount - (lastLintStatisticEntity.getFileCount() == null ? 0 : lastLintStatisticEntity.getFileCount());
        }

        LintStatisticEntity lintStatisticEntity = new LintStatisticEntity();
        lintStatisticEntity.setTaskId(taskId);
        lintStatisticEntity.setToolName(toolName);
        lintStatisticEntity.setFileCount(fileCount);
        lintStatisticEntity.setDefectCount(defectCount);
        lintStatisticEntity.setNewDefectCount(newDefectCount);
        lintStatisticEntity.setHistoryDefectCount(historyDefectCount);
        lintStatisticEntity.setDefectChange(defectChange);
        lintStatisticEntity.setFileChange(fileChange);
        lintStatisticEntity.setBuildId(buildId);
        lintStatisticEntity.setTotalNewNormal(totalNewNormal);
        lintStatisticEntity.setTotalNewPrompt(totalNewPrompt);
        lintStatisticEntity.setTotalNewSerious(totalNewSerious);
        lintStatisticEntity.setTotalNormal(totalNormal);
        lintStatisticEntity.setTotalPrompt(totalPrompt);
        lintStatisticEntity.setTotalSerious(totalSerious);
        lintStatisticEntity.setTotalDefectCount(totalDefectCount);
        lintStatisticEntity.setAuthorStatistic(authorDefects);
        lintStatisticEntity.setCheckerStatistic(getCheckerStatistic(toolName, allDefectEntityList));
        lintStatisticEntity.setExistAuthorStatistic(Lists.newArrayList(existAuthorMap.values()));

        long currentTime = System.currentTimeMillis();
        lintStatisticEntity.setTime(currentTime);
        lintStatisticEntity = lintStatisticRepository.save(lintStatisticEntity);

        // 异步统计非new状态的告警数
        if (ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value().equalsIgnoreCase(taskVO.getCreateFrom())) {
            rabbitTemplate.convertAndSend(EXCHANGE_CLOSE_DEFECT_STATISTIC_LINT_OPENSOURCE,
                    ROUTE_CLOSE_DEFECT_STATISTIC_LINT_OPENSOURCE, lintStatisticEntity);
        } else {
            rabbitTemplate.convertAndSend(EXCHANGE_CLOSE_DEFECT_STATISTIC_LINT,
                    ROUTE_CLOSE_DEFECT_STATISTIC_LINT, lintStatisticEntity);
        }
    }

    private List<CheckerStatisticEntity> getCheckerStatistic(String toolName,
                                                             List<LintDefectV2Entity> allDefectEntityList) {
        // get checker map
        Set<String> checkerIds = allDefectEntityList.stream()
            .map(LintDefectV2Entity::getChecker).collect(Collectors.toSet());
        Map<String, CheckerDetailEntity> checkerDetailMap = new HashMap<>();
        checkerRepository.findByToolNameAndCheckerKeyIn(toolName, checkerIds)
            .forEach(it -> checkerDetailMap.put(it.getCheckerKey(), it));

        // get lint checker statistic data
        Map<String, CheckerStatisticEntity> checkerStatisticEntityMap = new HashMap<>();
        for (LintDefectV2Entity entity: allDefectEntityList) {
            CheckerStatisticEntity item = checkerStatisticEntityMap.get(entity.getChecker());
            if (item == null)
            {
                item = new CheckerStatisticEntity();
                item.setName(entity.getChecker());

                CheckerDetailEntity checker = checkerDetailMap.get(entity.getChecker());
                if (checker != null) {
                    item.setId(checker.getEntityId());
                    item.setName(checker.getCheckerName());
                    item.setSeverity(checker.getSeverity());
                } else {
                    log.warn("not found checker for tool: {}, {}", toolName, entity.getChecker());
                }
            }
            item.setDefectCount(item.getDefectCount() + 1);
            checkerStatisticEntityMap.put(entity.getChecker(), item);
        }
        return new ArrayList<>(checkerStatisticEntityMap.values());
    }
}
