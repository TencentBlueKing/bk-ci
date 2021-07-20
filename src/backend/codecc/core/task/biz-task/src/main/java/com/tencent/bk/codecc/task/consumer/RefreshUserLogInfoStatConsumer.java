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

package com.tencent.bk.codecc.task.consumer;

import com.tencent.bk.codecc.task.model.UserLogInfoStatEntity;
import com.tencent.bk.codecc.task.service.UserLogInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_USER_LOG_INFO_STAT;
import static com.tencent.devops.common.web.mq.ConstantsKt.QUEUE_USER_LOG_INFO_STAT;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_USER_LOG_INFO_STAT;

/**
 * 刷新用户登录统计数据
 *
 * @version V1.0
 * @date 2020/10/20
 */
@Component
@Slf4j
public class RefreshUserLogInfoStatConsumer {

    @Autowired
    private UserLogInfoService userLogInfoService;


    @RabbitListener(bindings = @QueueBinding(key = ROUTE_USER_LOG_INFO_STAT,
            value = @Queue(value = QUEUE_USER_LOG_INFO_STAT, durable = "true"),
            exchange = @Exchange(value = EXCHANGE_USER_LOG_INFO_STAT, durable = "true")))
    public void refreshUserLogInfoStat(UserLogInfoStatEntity statEntity) {
        // TODO("not implemented")
    }

}
