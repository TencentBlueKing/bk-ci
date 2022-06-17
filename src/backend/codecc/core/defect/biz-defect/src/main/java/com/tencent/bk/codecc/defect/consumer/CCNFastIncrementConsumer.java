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

import com.tencent.bk.codecc.defect.dao.mongorepository.CCNDefectRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.CCNStatisticRepository;
import com.tencent.bk.codecc.defect.model.BuildEntity;
import com.tencent.bk.codecc.defect.model.CCNDefectEntity;
import com.tencent.bk.codecc.defect.model.CCNStatisticEntity;
import com.tencent.bk.codecc.defect.model.incremental.ToolBuildInfoEntity;
import com.tencent.bk.codecc.defect.model.incremental.ToolBuildStackEntity;
import com.tencent.bk.codecc.defect.service.BuildDefectService;
import com.tencent.bk.codecc.defect.service.statistic.CCNDefectStatisticService;
import com.tencent.bk.codecc.task.vo.AnalyzeConfigInfoVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.devops.common.constant.ComConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Lint告警提交消息队列的消费者
 *
 * @version V1.0
 * @date 2020/3/12
 */
@Component
@Slf4j
public class CCNFastIncrementConsumer extends AbstractFastIncrementConsumer
{
    @Autowired
    private CCNDefectRepository ccnDefectRepository;
    @Autowired
    private BuildDefectService buildDefectService;
    @Autowired
    private CCNDefectStatisticService ccnDefectStatisticService;
    @Autowired
    private CCNStatisticRepository ccnStatisticRepository;

    @Override
    protected void generateResult(AnalyzeConfigInfoVO analyzeConfigInfoVO)
    {
        long taskId = analyzeConfigInfoVO.getTaskId();
        String streamName = analyzeConfigInfoVO.getNameEn();
        String toolName = analyzeConfigInfoVO.getMultiToolType();
        String buildId = analyzeConfigInfoVO.getBuildId();

        TaskDetailVO taskVO = thirdPartySystemCaller.getTaskInfo(streamName);
        BuildEntity buildEntity = buildDao.getAndSaveBuildInfo(buildId);

        ToolBuildStackEntity toolBuildStackEntity = toolBuildStackRepository.findFirstByTaskIdAndToolNameAndBuildId(taskId, toolName, buildId);

        // 因为代码没有变更，默认总平均圈复杂度不变，所以直接取上一个分析的平均圈复杂度
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
        CCNStatisticEntity baseBuildCcnStatistic = ccnStatisticRepository.findFirstByTaskIdAndBuildId(taskVO.getTaskId(), baseBuildId);

        float averageCCN = 0;
        if (baseBuildCcnStatistic != null)
        {
            averageCCN = baseBuildCcnStatistic.getAverageCCN();
        }

        List<CCNDefectEntity> allNewDefectList = ccnDefectRepository.findByTaskIdAndStatus(taskId, ComConstants.DefectStatus.NEW.value());

        // 统计本次扫描的告警
        ccnDefectStatisticService.statistic(taskVO, toolName, averageCCN, buildId, toolBuildStackEntity, allNewDefectList);

        // 更新构建告警快照
        buildDefectService.saveCCNBuildDefect(taskId, toolName, buildEntity, allNewDefectList);

        // 保存质量红线数据
        redLineReportService.saveRedLineData(taskVO, ComConstants.Tool.CCN.name(), buildId);
    }
}
