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

import com.tencent.bk.codecc.defect.dao.mongorepository.CommonStatisticRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.DefectRepository;
import com.tencent.bk.codecc.defect.model.CommonStatisticEntity;
import com.tencent.bk.codecc.defect.model.DefectEntity;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.service.IConsumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Lint已关闭告警统计消息队列的消费者
 *
 * @version V1.0
 * @date 2020/3/12
 */
@Component
@Slf4j
public class CommonCloseDefectStatisticConsumer implements IConsumer<CommonStatisticEntity> {
    @Autowired
    private DefectRepository defectRepository;
    @Autowired
    private CommonStatisticRepository commonStatisticRepository;

    @Override
    public void consumer(CommonStatisticEntity statisticEntity) {
        Long taskId = statisticEntity.getTaskId();
        String toolName = statisticEntity.getToolName();

        long totalSeriousFixedCount = 0L;
        long totalNormalFixedCount = 0L;
        long totalPromptFixedCount = 0L;
        long totalSeriousIgnoreCount = 0L;
        long totalNormalIgnoreCount = 0L;
        long totalPromptIgnoreCount = 0L;
        long totalSeriousMaskCount = 0L;
        long totalNormalMaskCount = 0L;
        long totalPromptMaskCount = 0L;

        // 查询所有已关闭的告警
        List<DefectEntity> allCloseDefectList = defectRepository.findCloseDefectByTaskIdAndToolName(taskId, toolName);
        for (DefectEntity defect : allCloseDefectList) {
            int status = defect.getStatus();
            int severity = defect.getSeverity();
            if (status == (ComConstants.DefectStatus.NEW.value() | ComConstants.DefectStatus.FIXED.value())) {
                switch (severity) {
                    case ComConstants.SERIOUS:
                        totalSeriousFixedCount++;
                        break;
                    case ComConstants.NORMAL:
                        totalNormalFixedCount++;
                        break;
                    case ComConstants.PROMPT:
                    case ComConstants.PROMPT_IN_DB:
                        totalPromptFixedCount++;
                        break;
                    default:
                        break;
                }
            } else if (status == (ComConstants.DefectStatus.NEW.value() | ComConstants.DefectStatus.IGNORE.value())) {
                switch (severity) {
                    case ComConstants.SERIOUS:
                        totalSeriousIgnoreCount++;
                        break;
                    case ComConstants.NORMAL:
                        totalNormalIgnoreCount++;
                        break;
                    case ComConstants.PROMPT:
                    case ComConstants.PROMPT_IN_DB:
                        totalPromptIgnoreCount++;
                        break;
                    default:
                        break;
                }
            } else if (status >=  ComConstants.DefectStatus.PATH_MASK.value()) {
                switch (severity) {
                    case ComConstants.SERIOUS:
                        totalSeriousMaskCount++;
                        break;
                    case ComConstants.NORMAL:
                        totalNormalMaskCount++;
                        break;
                    case ComConstants.PROMPT:
                    case ComConstants.PROMPT_IN_DB:
                        totalPromptMaskCount++;
                        break;
                    default:
                        break;
                }
            }
        }
        statisticEntity.setSeriousFixedCount(totalSeriousFixedCount);
        statisticEntity.setNormalFixedCount(totalNormalFixedCount);
        statisticEntity.setPromptFixedCount(totalPromptFixedCount);
        statisticEntity.setSeriousIgnoreCount(totalSeriousIgnoreCount);
        statisticEntity.setNormalIgnoreCount(totalNormalIgnoreCount);
        statisticEntity.setPromptIgnoreCount(totalPromptIgnoreCount);
        statisticEntity.setSeriousMaskCount(totalSeriousMaskCount);
        statisticEntity.setNormalMaskCount(totalNormalMaskCount);
        statisticEntity.setPromptMaskCount(totalPromptMaskCount);

        commonStatisticRepository.save(statisticEntity);
    }

}
