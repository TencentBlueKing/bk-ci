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

package com.tencent.bk.codecc.quartz.job;

import com.tencent.bk.codecc.quartz.pojo.QuartzJobContext;
import com.tencent.bk.codecc.task.pojo.TriggerPipelineModel;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;


/**
 * 触发灰度池任务定时任务
 *
 * @version V1.0
 * @date 2021/1/7
 */
public class TriggerGrayPoolScheduledTask implements IScheduleTask {
    private static Logger logger = LoggerFactory.getLogger(TriggerGrayPoolScheduledTask.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public void executeTask(@NotNull QuartzJobContext quartzJobContext) {
        Map<String, Object> jobCustomParam = quartzJobContext.getJobCustomParam();
        if (null == jobCustomParam) {
            logger.info("job custom param is null!");
            return;
        }
        String projectId = String.valueOf(jobCustomParam.get("projectId"));
        String pipelineId = String.valueOf(jobCustomParam.get("pipelineId"));
        String taskId = String.valueOf(jobCustomParam.get("taskId"));
        String gongfengId = String.valueOf(jobCustomParam.get("gongfengId"));
        String userName = String.valueOf(jobCustomParam.get("owner"));
        TriggerPipelineModel triggerPipelineModel = new TriggerPipelineModel(
                projectId, pipelineId, Long.parseLong(taskId), Integer.parseInt(gongfengId), userName, null, null, null
        );
        rabbitTemplate.convertAndSend("exchange.gray.task.pool", "route.gray.task.pool.trigger", triggerPipelineModel);
    }
}
