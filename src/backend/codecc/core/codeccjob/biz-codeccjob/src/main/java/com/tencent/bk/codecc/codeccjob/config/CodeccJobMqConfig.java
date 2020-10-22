/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.codecc.codeccjob.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_TASK_CHECKER_CONFIG;
import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_TASK_FILTER_PATH;
import static com.tencent.devops.common.web.mq.ConstantsKt.QUEUE_ADD_TASK_FILTER_PATH;
import static com.tencent.devops.common.web.mq.ConstantsKt.QUEUE_DEL_TASK_FILTER_PATH;
import static com.tencent.devops.common.web.mq.ConstantsKt.QUEUE_IGNORE_CHECKER;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_ADD_TASK_FILTER_PATH;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_DEL_TASK_FILTER_PATH;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_IGNORE_CHECKER;

/**
 * 快速增量的消息队列配置
 *
 * @version V1.0
 * @date 2020/08/07
 */
@Configuration
@Slf4j
public class CodeccJobMqConfig
{
    @Bean
    public DirectExchange addTaskFilterPathDirectExchange()
    {
        DirectExchange directExchange = new DirectExchange(EXCHANGE_TASK_FILTER_PATH);
        directExchange.setDelayed(true);
        return directExchange;
    }

    @Bean
    public Queue addTaskFilterPathQueue()
    {
        return new Queue(QUEUE_ADD_TASK_FILTER_PATH);
    }


    @Bean
    public Binding addTaskFilterPathQueueBind(Queue addTaskFilterPathQueue, DirectExchange addTaskFilterPathDirectExchange)
    {
        return BindingBuilder.bind(addTaskFilterPathQueue).to(addTaskFilterPathDirectExchange).with(ROUTE_ADD_TASK_FILTER_PATH);
    }

    @Bean
    public Queue delTaskFilterPathQueue()
    {
        return new Queue(QUEUE_DEL_TASK_FILTER_PATH);
    }

    @Bean
    public Binding delTaskFilterPathQueueBind(Queue delTaskFilterPathQueue, DirectExchange addTaskFilterPathDirectExchange)
    {
        return BindingBuilder.bind(delTaskFilterPathQueue).to(addTaskFilterPathDirectExchange).with(ROUTE_DEL_TASK_FILTER_PATH);
    }

    @Bean
    public DirectExchange taskCheckerConfigExchange() {
        DirectExchange directExchange = new DirectExchange(EXCHANGE_TASK_CHECKER_CONFIG);
        directExchange.setDelayed(true);
        return directExchange;
    }

    @Bean
    public Binding ignoreCheckerBind(Queue ignoreCheckerQueue, DirectExchange taskCheckerConfigExchange)
    {
        return BindingBuilder.bind(ignoreCheckerQueue).to(taskCheckerConfigExchange).with(ROUTE_IGNORE_CHECKER);
    }

    @Bean
    public Queue ignoreCheckerQueue() {
        return new Queue(QUEUE_IGNORE_CHECKER);
    }
}
