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

import com.tencent.bk.codecc.defect.component.LintDefectMigrationHelper;
import com.tencent.bk.codecc.defect.dto.AnalysisVersionDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.tencent.devops.common.web.mq.ConstantsKt.*;

/**
 * Lint告警提交消息队列的消费者
 *
 * @version V1.0
 * @date 2020/3/12
 */
@Component
@Slf4j
public class LintDefectMigrationConsumer
{
    @Autowired
    private LintDefectMigrationHelper lintDefectMigrationHelper;

    @RabbitListener(bindings = @QueueBinding(key = ROUTE_LINT_DEFECT_MIGRATION,
            value = @Queue(value = QUEUE_LINT_DEFECT_MIGRATION, durable = "true"),
            exchange = @Exchange(value = EXCHANGE_LINT_DEFECT_MIGRATION, durable = "true", delayed = "true")))
    public void migration(AnalysisVersionDTO analysisVersionDTO)
    {
        log.info("begin migration lint defect, taskId: {}, toolName:{}", analysisVersionDTO.getTaskId(), analysisVersionDTO.getToolName());
        try
        {
            lintDefectMigrationHelper.migration(analysisVersionDTO.getTaskId(), analysisVersionDTO.getToolName());
        }
        catch (Exception e)
        {
            log.error("migration lint defect error! taskId: {}, toolName: {}", analysisVersionDTO.getTaskId(), analysisVersionDTO.getToolName(), e);
        }

    }
}
