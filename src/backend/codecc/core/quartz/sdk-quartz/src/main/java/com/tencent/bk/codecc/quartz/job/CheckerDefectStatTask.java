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
import com.tencent.bk.codecc.task.pojo.RefreshCheckerDefectStatModel;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

/**
 * 规则告警统计定时任务
 *
 * @version V1.0
 * @date 2020/11/17
 */

public class CheckerDefectStatTask implements IScheduleTask {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private static Logger logger = LoggerFactory.getLogger(CheckerDefectStatTask.class);

    @Override
    public void executeTask(@NotNull QuartzJobContext quartzJobContext) {
        Map<String, Object> jobCustomParam = quartzJobContext.getJobCustomParam();
        if (null == jobCustomParam) {
            logger.error("CheckerDefectStatTask job custom param is null");
            return;
        }

        String dataFrom = (String) jobCustomParam.get("dataFrom");
        RefreshCheckerDefectStatModel model = new RefreshCheckerDefectStatModel(dataFrom);

        logger.info("beginning checker defect statistics: {}.", dataFrom);
        rabbitTemplate.convertAndSend("exchange.checker.defect.stat", "route.checker.defect.stat", model);
    }
}
