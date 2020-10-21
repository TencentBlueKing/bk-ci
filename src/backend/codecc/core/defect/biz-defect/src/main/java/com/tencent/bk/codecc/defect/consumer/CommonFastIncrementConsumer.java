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

import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.dao.mongorepository.BuildRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.CommonStatisticRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.DefectRepository;
import com.tencent.bk.codecc.defect.model.BuildEntity;
import com.tencent.bk.codecc.defect.model.CommonStatisticEntity;
import com.tencent.bk.codecc.defect.model.DefectEntity;
import com.tencent.bk.codecc.defect.service.BuildDefectService;
import com.tencent.bk.codecc.defect.service.impl.CommonAnalyzeTaskBizServiceImpl;
import com.tencent.bk.codecc.task.vo.AnalyzeConfigInfoVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.devops.common.constant.ComConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

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

    /**
     * 告警提交
     *
     * @param analyzeConfigInfoVO
     */
    @Override
    public void consumer(AnalyzeConfigInfoVO analyzeConfigInfoVO)
    {
        long beginTime = System.currentTimeMillis();
        try
        {
            log.info("fast increment generate result! {}", analyzeConfigInfoVO);

            // 构建开始
            uploadTaskLog(analyzeConfigInfoVO, ComConstants.Step4Cov.UPLOAD.value(), ComConstants.StepFlag.PROCESSING.value(), System.currentTimeMillis(), 0, null);

            // 构建结束
            uploadTaskLog(analyzeConfigInfoVO, ComConstants.Step4Cov.UPLOAD.value(), ComConstants.StepFlag.SUCC.value(), 0, System.currentTimeMillis(), null);

            // 排队开始
            uploadTaskLog(analyzeConfigInfoVO, ComConstants.Step4Cov.QUEUE.value(), ComConstants.StepFlag.PROCESSING.value(), System.currentTimeMillis(), 0, null);

            // 排队结束
            uploadTaskLog(analyzeConfigInfoVO, ComConstants.Step4Cov.QUEUE.value(), ComConstants.StepFlag.SUCC.value(), 0, System.currentTimeMillis(), null);

            // 扫描开始
            uploadTaskLog(analyzeConfigInfoVO, ComConstants.Step4Cov.ANALYZE.value(), ComConstants.StepFlag.PROCESSING.value(), System.currentTimeMillis(), 0, null);

            // 扫描结束
            uploadTaskLog(analyzeConfigInfoVO, ComConstants.Step4Cov.ANALYZE.value(), ComConstants.StepFlag.SUCC.value(), 0, System.currentTimeMillis(), null);

            // 提交开始
            uploadTaskLog(analyzeConfigInfoVO, ComConstants.Step4Cov.COMMIT.value(), ComConstants.StepFlag.PROCESSING.value(), System.currentTimeMillis(), 0, null);

            // 提交结束
            uploadTaskLog(analyzeConfigInfoVO, ComConstants.Step4Cov.COMMIT.value(), ComConstants.StepFlag.SUCC.value(), 0, System.currentTimeMillis(), null);

            // 生成问题开始
            uploadTaskLog(analyzeConfigInfoVO, ComConstants.Step4Cov.DEFECT_SYNS.value(), ComConstants.StepFlag.PROCESSING.value(), System.currentTimeMillis(), 0, null);

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
                uploadTaskLog(analyzeConfigInfoVO, ComConstants.Step4Cov.DEFECT_SYNS.value(), ComConstants.StepFlag.FAIL.value(), 0, System.currentTimeMillis(), e.getMessage());
                return;
            }

            // 生成问题结束
            uploadTaskLog(analyzeConfigInfoVO, ComConstants.Step4Cov.DEFECT_SYNS.value(), ComConstants.StepFlag.SUCC.value(), 0, System.currentTimeMillis(), null);
        }
        catch (Throwable e)
        {
            log.error("fast increment generate result fail!", e);
        }
        log.info("end fast increment generate result cost: {}", System.currentTimeMillis() - beginTime);
    }

    @Override
    protected void generateResult(AnalyzeConfigInfoVO analyzeConfigInfoVO)
    {
        long taskId = analyzeConfigInfoVO.getTaskId();
        String streamName = analyzeConfigInfoVO.getNameEn();
        String toolName = analyzeConfigInfoVO.getMultiToolType();
        String buildId = analyzeConfigInfoVO.getBuildId();

        TaskDetailVO taskVO = thirdPartySystemCaller.getTaskInfo(streamName);

        List<DefectEntity> allNewDefectList = defectRepository.findByTaskIdAndToolNameAndStatus(taskId, toolName, ComConstants.DefectStatus.NEW.value());
        // 统计新增、关闭、遗留、屏蔽
        int existPromptCount = 0;
        int existNormalCount = 0;
        int existSeriousCount = 0;
        Set<String> newAuthors = Sets.newHashSet();
        for (DefectEntity defectEntity : allNewDefectList)
        {
            newAuthors.addAll(defectEntity.getAuthorList());
            if ((defectEntity.getSeverity() & ComConstants.PROMPT) > 0)
            {
                existPromptCount++;
            }
            if ((defectEntity.getSeverity() & ComConstants.NORMAL) > 0)
            {
                existNormalCount++;
            }
            if ((defectEntity.getSeverity() & ComConstants.SERIOUS) > 0)
            {
                existSeriousCount++;
            }
        }

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
        statisticEntity.setExistPromptCount(existPromptCount);
        statisticEntity.setExistNormalCount(existNormalCount);
        statisticEntity.setExistSeriousCount(existSeriousCount);
        statisticEntity.setNewPromptCount(0);
        statisticEntity.setNewNormalCount(0);
        statisticEntity.setNewSeriousCount(0);
        statisticEntity.setNewAuthors(Sets.newHashSet());
        statisticEntity.setExistAuthors(newAuthors);
        commonStatisticRepository.save(statisticEntity);

        //将数据加入数据平台
//        commonKafkaClient.pushCommonStatisticToKafka(statisticEntity);

        // 保存本次构建遗留告警告警列表快照
        BuildEntity buildEntity = buildRepository.findByBuildId(buildId);
        buildDefectService.saveCommonBuildDefect(taskId, toolName, buildEntity, allNewDefectList);

        // 发送群机器人通知
        commonAnalyzeTaskBizService.sendBotRemind(taskVO, statisticEntity, toolName);

        // 保存质量红线数据
        redLineReportService.saveRedLineData(taskVO, toolName, buildId);
    }
}
