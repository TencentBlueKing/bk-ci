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

import com.tencent.bk.codecc.defect.dao.mongorepository.CLOCStatisticRepository;
import com.tencent.bk.codecc.defect.dao.mongotemplate.CLOCStatisticsDao;
import com.tencent.bk.codecc.defect.model.CLOCStatisticEntity;
import com.tencent.bk.codecc.defect.model.incremental.ToolBuildInfoEntity;
import com.tencent.bk.codecc.defect.model.incremental.ToolBuildStackEntity;
import com.tencent.bk.codecc.task.vo.AnalyzeConfigInfoVO;
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
public class CLOCFastIncrementConsumer extends AbstractFastIncrementConsumer
{
    @Autowired
    private CLOCStatisticRepository clocStatisticRepository;
    @Autowired
    private CLOCStatisticsDao clocStatisticsDao;

    @Override
    protected void generateResult(AnalyzeConfigInfoVO analyzeConfigInfoVO)
    {
        long taskId = analyzeConfigInfoVO.getTaskId();
        String toolName = analyzeConfigInfoVO.getMultiToolType();
        String buildId = analyzeConfigInfoVO.getBuildId();

        ToolBuildStackEntity toolBuildStackEntity = toolBuildStackRepository.findFirstByTaskIdAndToolNameAndBuildId(taskId, toolName, buildId);

        // 因为代码没有变更，默认代码统计不变，所以直接取上一个分析的代码统计
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
        List<CLOCStatisticEntity> lastClocStatisticEntityList = clocStatisticRepository.findByTaskIdAndToolNameAndBuildId(taskId, toolName, baseBuildId);

        long currentTime = System.currentTimeMillis();
        lastClocStatisticEntityList.forEach(clocStatisticEntity ->
        {
            clocStatisticEntity.setEntityId(null);
            clocStatisticEntity.setBuildId(buildId);
            clocStatisticEntity.setBlankChange(0L);
            clocStatisticEntity.setCodeChange(0L);
            clocStatisticEntity.setCommentChange(0L);
            clocStatisticEntity.setFileNumChange(0L);
            clocStatisticEntity.setUpdatedDate(currentTime);
        });

        clocStatisticsDao.batchUpsertCLOCStatistic(lastClocStatisticEntityList);

        // 更新告警快照基准构建ID
        toolBuildInfoDao.updateDefectBaseBuildId(taskId, toolName, buildId);
    }
}
