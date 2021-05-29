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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencent.bk.codecc.quartz.pojo.QuartzJobContext;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_GONGFENG_STAT_SYNC_NEW;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_GONGFENG_STAT_SYNC_NEW;

/**
 * 同步工蜂统计信息定时任务
 *
 * @version V1.0
 * @date 2020/4/1
 */
public class SyncGongfengStatProjTaskNew implements IScheduleTask {
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private static Logger logger = LoggerFactory.getLogger(SyncGongfengStatProjTaskNew.class);

    @Override
    public void executeTask(@NotNull QuartzJobContext quartzJobContext) {
        Map<String, Object> jobCustomParam = quartzJobContext.getJobCustomParam();
        if (null == jobCustomParam) {
            logger.info("job custom param is null");
            return;
        }

        String bgIdStr = (String) jobCustomParam.get("bgId");
        rabbitTemplate.convertAndSend(EXCHANGE_GONGFENG_STAT_SYNC_NEW, ROUTE_GONGFENG_STAT_SYNC_NEW, bgIdStr);

    }
}
