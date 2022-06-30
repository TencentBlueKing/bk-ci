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

package com.tencent.bk.codecc.defect.consumer;

import com.tencent.bk.codecc.defect.dao.mongorepository.BuildRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.CheckerRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.CommonStatisticRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.DefectRepository;
import com.tencent.bk.codecc.defect.model.BuildEntity;
import com.tencent.bk.codecc.defect.model.CheckerDetailEntity;
import com.tencent.bk.codecc.defect.model.CheckerStatisticEntity;
import com.tencent.bk.codecc.defect.model.CommonStatisticEntity;
import com.tencent.bk.codecc.defect.model.DefectEntity;
import com.tencent.bk.codecc.defect.pojo.CommonDefectStatisticModel;
import com.tencent.bk.codecc.defect.service.BuildDefectService;
import com.tencent.bk.codecc.defect.service.impl.CommonAnalyzeTaskBizServiceImpl;
import com.tencent.bk.codecc.defect.service.newdefectjudge.NewDefectJudgeService;
import com.tencent.bk.codecc.defect.service.statistic.CommonDefectStatisticService;
import com.tencent.bk.codecc.task.vo.AnalyzeConfigInfoVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.web.aop.annotation.EndReport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Lint告警提交消息队列的消费者
 *
 * @version V1.0
 * @date 2020/3/12
 */
@Component
@Slf4j
public class CommonFastIncrementConsumer extends AbstractFastIncrementConsumer
{
    @Autowired
    private BuildRepository buildRepository;
    @Autowired
    private DefectRepository defectRepository;
    @Autowired
    private BuildDefectService buildDefectService;
    @Autowired
    private CommonStatisticRepository commonStatisticRepository;
    @Autowired
    @Qualifier("CommonAnalyzeTaskBizService")
    private CommonAnalyzeTaskBizServiceImpl commonAnalyzeTaskBizService;
    @Autowired
    private CheckerRepository checkerRepository;
    @Autowired
    private NewDefectJudgeService newDefectJudgeService;
    @Autowired
    private CommonDefectStatisticService commonDefectStatisticService;

    /**
     * 告警提交
     *
     * @param analyzeConfigInfoVO
     */
    @EndReport(isOpenSource = false)
    @Override
    public void consumer(AnalyzeConfigInfoVO analyzeConfigInfoVO)
    {
        long beginTime = System.currentTimeMillis();
        try
        {
            log.info("fast increment generate result! {}", analyzeConfigInfoVO);

            // 构建开始
            uploadTaskLog(analyzeConfigInfoVO, ComConstants.Step4Cov.UPLOAD.value(), ComConstants.StepFlag.PROCESSING.value(), System.currentTimeMillis(), 0, null, false);

            // 构建结束
            uploadTaskLog(analyzeConfigInfoVO, ComConstants.Step4Cov.UPLOAD.value(), ComConstants.StepFlag.SUCC.value(), 0, System.currentTimeMillis(), null, false);

            // 排队开始
            uploadTaskLog(analyzeConfigInfoVO, ComConstants.Step4Cov.QUEUE.value(), ComConstants.StepFlag.PROCESSING.value(), System.currentTimeMillis(), 0, null, false);

            // 排队结束
            uploadTaskLog(analyzeConfigInfoVO, ComConstants.Step4Cov.QUEUE.value(), ComConstants.StepFlag.SUCC.value(), 0, System.currentTimeMillis(), null, false);

            // 扫描开始
            uploadTaskLog(analyzeConfigInfoVO, ComConstants.Step4Cov.ANALYZE.value(), ComConstants.StepFlag.PROCESSING.value(), System.currentTimeMillis(), 0, null, false);

            // 扫描结束
            uploadTaskLog(analyzeConfigInfoVO, ComConstants.Step4Cov.ANALYZE.value(), ComConstants.StepFlag.SUCC.value(), 0, System.currentTimeMillis(), null, false);

            // 提交开始
            uploadTaskLog(analyzeConfigInfoVO, ComConstants.Step4Cov.COMMIT.value(), ComConstants.StepFlag.PROCESSING.value(), System.currentTimeMillis(), 0, null, false);

            // 提交结束
            uploadTaskLog(analyzeConfigInfoVO, ComConstants.Step4Cov.COMMIT.value(), ComConstants.StepFlag.SUCC.value(), 0, System.currentTimeMillis(), null, false);

            // 生成问题开始
            uploadTaskLog(analyzeConfigInfoVO, ComConstants.Step4Cov.DEFECT_SYNS.value(), ComConstants.StepFlag.PROCESSING.value(), System.currentTimeMillis(), 0, null, false);

            try
            {
                // 生成当前遗留告警的统计信息
                generateResult(analyzeConfigInfoVO);

                // 保存代码库信息
                upsertCodeRepoInfo(analyzeConfigInfoVO);
            }
            catch (Throwable e)
            {
                e.printStackTrace();
                log.error("fast increment generate result fail!", e);
                // 发送提单失败的分析记录
                uploadTaskLog(analyzeConfigInfoVO, ComConstants.Step4Cov.DEFECT_SYNS.value(), ComConstants.StepFlag.FAIL.value(), 0, System.currentTimeMillis(), e.getMessage(), false);
                return;
            }

            // 生成问题结束
            uploadTaskLog(analyzeConfigInfoVO, ComConstants.Step4Cov.DEFECT_SYNS.value(), ComConstants.StepFlag.SUCC.value(), 0, System.currentTimeMillis(), null, true);
        }
        catch (Throwable e)
        {
            log.error("fast increment generate result fail!", e);
        }
        log.info("end fast increment generate result cost: {}", System.currentTimeMillis() - beginTime);
    }

    @Override
    protected void generateResult(AnalyzeConfigInfoVO analyzeConfigInfoVO) {
        long taskId = analyzeConfigInfoVO.getTaskId();
        String streamName = analyzeConfigInfoVO.getNameEn();
        String toolName = analyzeConfigInfoVO.getMultiToolType();
        String buildId = analyzeConfigInfoVO.getBuildId();
        TaskDetailVO taskVO = thirdPartySystemCaller.getTaskInfo(streamName);
        List<DefectEntity> allNewDefectList =
            defectRepository.findByTaskIdAndToolNameAndStatus(taskId, toolName, ComConstants.DefectStatus.NEW.value());

        // 当任务没设置新问题判定时间时，统一取任务的创建时间，所以不要传toolName；保证前端展示跟后端统计的一致性
        long newDefectJudgeTime = newDefectJudgeService.getNewDefectJudgeTime(taskId, taskVO);
        log.info("saveAndStatisticDefect cov fast, newDefectJudgeTime: {}", newDefectJudgeTime);
        CommonDefectStatisticModel statistic =
                commonDefectStatisticService.statistic(newDefectJudgeTime, allNewDefectList);

        CommonStatisticEntity statisticEntity = new CommonStatisticEntity();
        statisticEntity.setTaskId(taskId);
        statisticEntity.setToolName(toolName);
        statisticEntity.setTime(System.currentTimeMillis());
        statisticEntity.setBuildId(buildId);
        statisticEntity.setNewCount(0);
        statisticEntity.setFixedCount(0);
        statisticEntity.setExistCount(allNewDefectList.size());
        statisticEntity.setCloseCount(0);
        statisticEntity.setExcludeCount(0);
        statisticEntity.setExistPromptCount(statistic.getExistPromptCount());
        statisticEntity.setExistNormalCount(statistic.getExistNormalCount());
        statisticEntity.setExistSeriousCount(statistic.getExistSeriousCount());
        statisticEntity.setNewPromptCount(statistic.getNewPromptCount());
        statisticEntity.setNewNormalCount(statistic.getNewNormalCount());
        statisticEntity.setNewSeriousCount(statistic.getNewSeriousCount());
        statisticEntity.setNewAuthors(statistic.getNewAuthors());
        statisticEntity.setExistAuthors(statistic.getExistAuthors());
        statisticEntity.setPromptAuthors(statistic.getNewPromptAuthors());
        statisticEntity.setNormalAuthors(statistic.getNewNormalAuthors());
        statisticEntity.setSeriousAuthors(statistic.getNewSeriousAuthors());
        statisticEntity.setExistPromptAuthors(statistic.getExistPromptAuthors());
        statisticEntity.setExistNormalAuthors(statistic.getExistNormalAuthors());
        statisticEntity.setExistSeriousAuthors(statistic.getExistSeriousAuthors());
        statisticEntity.setCheckerStatistic(getCheckerStatistic(toolName, allNewDefectList));
        commonStatisticRepository.save(statisticEntity);

        // 保存本次构建遗留告警告警列表快照
        BuildEntity buildEntity = buildRepository.findFirstByBuildId(buildId);
        buildDefectService.saveCommonBuildDefect(taskId, toolName, buildEntity, allNewDefectList);

        // 改由MQ汇总发送 {@link EmailNotifyServiceImpl#sendWeChatBotRemind(RtxNotifyModel, TaskInfoEntity)}
        // 发送群机器人通知
        // commonAnalyzeTaskBizService.sendBotRemind(taskVO, statisticEntity, toolName);

        // 保存质量红线数据
        redLineReportService.saveRedLineData(taskVO, toolName, buildId);
    }

    private List<CheckerStatisticEntity> getCheckerStatistic(String toolName, List<DefectEntity> allDefectEntityList) {
        // get checker map
        Set<String> checkerIds = allDefectEntityList.stream()
            .map(DefectEntity::getCheckerName).collect(Collectors.toSet());
        Map<String, CheckerDetailEntity> checkerDetailMap = new HashMap<>();
        checkerRepository.findByToolNameAndCheckerKeyIn(toolName, checkerIds)
            .forEach(it -> checkerDetailMap.put(it.getCheckerKey(), it));

        // get lint checker statistic data
        Map<String, CheckerStatisticEntity> checkerStatisticEntityMap = new HashMap<>();
        for (DefectEntity entity: allDefectEntityList) {
            CheckerStatisticEntity item = checkerStatisticEntityMap.get(entity.getCheckerName());
            if (item == null) {
                item = new CheckerStatisticEntity();
                item.setName(entity.getCheckerName());

                CheckerDetailEntity checker = checkerDetailMap.get(entity.getCheckerName());
                if (checker != null) {
                    item.setId(checker.getEntityId());
                    item.setName(checker.getCheckerName());
                    item.setSeverity(checker.getSeverity());
                } else {
                    log.warn("not found checker for tool: {}, {}", toolName, entity.getCheckerName());
                }
            }
            item.setDefectCount(item.getDefectCount() + 1);
            checkerStatisticEntityMap.put(entity.getCheckerName(), item);
        }
        return new ArrayList<>(checkerStatisticEntityMap.values());
    }
}
