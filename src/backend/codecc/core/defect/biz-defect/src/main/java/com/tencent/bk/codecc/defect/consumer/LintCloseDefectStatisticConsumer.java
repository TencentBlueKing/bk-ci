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

import com.tencent.bk.codecc.defect.dao.mongorepository.LintDefectV2Repository;
import com.tencent.bk.codecc.defect.dao.mongorepository.LintStatisticRepository;
import com.tencent.bk.codecc.defect.model.LintDefectV2Entity;
import com.tencent.bk.codecc.defect.model.LintStatisticEntity;
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
public class LintCloseDefectStatisticConsumer implements IConsumer<LintStatisticEntity> {
    @Autowired
    private LintDefectV2Repository lintDefectV2Repository;
    @Autowired
    private LintStatisticRepository lintStatisticRepository;

    @Override
    public void consumer(LintStatisticEntity lintStatisticEntity) {
        Long taskId = lintStatisticEntity.getTaskId();
        String toolName = lintStatisticEntity.getToolName();

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
        List<LintDefectV2Entity> allCloseDefectList = lintDefectV2Repository.findCloseDefectByTaskIdAndToolName(taskId, toolName);
        for (LintDefectV2Entity defect : allCloseDefectList) {
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
        lintStatisticEntity.setSeriousFixedCount(totalSeriousFixedCount);
        lintStatisticEntity.setNormalFixedCount(totalNormalFixedCount);
        lintStatisticEntity.setPromptFixedCount(totalPromptFixedCount);
        lintStatisticEntity.setSeriousIgnoreCount(totalSeriousIgnoreCount);
        lintStatisticEntity.setNormalIgnoreCount(totalNormalIgnoreCount);
        lintStatisticEntity.setPromptIgnoreCount(totalPromptIgnoreCount);
        lintStatisticEntity.setSeriousMaskCount(totalSeriousMaskCount);
        lintStatisticEntity.setNormalMaskCount(totalNormalMaskCount);
        lintStatisticEntity.setPromptMaskCount(totalPromptMaskCount);

        lintStatisticRepository.save(lintStatisticEntity);
    }

}
