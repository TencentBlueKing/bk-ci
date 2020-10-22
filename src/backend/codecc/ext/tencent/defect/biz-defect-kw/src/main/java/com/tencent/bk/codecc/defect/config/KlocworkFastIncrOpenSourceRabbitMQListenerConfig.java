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

package com.tencent.bk.codecc.defect.config;

import com.tencent.bk.codecc.defect.consumer.CommonFastIncrementConsumer;
import com.tencent.devops.common.service.IConsumer;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.tencent.devops.common.web.mq.ConstantsKt.*;
import static com.tencent.devops.common.web.mq.TencentConstantsKt.EXCHANGE_FAST_INCREMENT_KLOCWORK_OPENSOURCE;
import static com.tencent.devops.common.web.mq.TencentConstantsKt.QUEUE_FAST_INCREMENT_KLOCWORK_OPENSOURCE;
import static com.tencent.devops.common.web.mq.TencentConstantsKt.ROUTE_FAST_INCREMENT_KLOCWORK_OPENSOURCE;

/**
 * 快速增量的消息队列配置
 *
 * @version V1.0
 * @date 2020/08/07
 */
@Configuration
@Slf4j
@ConditionalOnProperty(prefix = "spring.application", name = "name", havingValue = "opensourcereport")
public class KlocworkFastIncrOpenSourceRabbitMQListenerConfig
{
    @Bean
    public Queue klocworkFastIncrOpenSourceQueue()
    {
        return new Queue(QUEUE_FAST_INCREMENT_KLOCWORK_OPENSOURCE);
    }

    @Bean
    public DirectExchange klocworkFastIncrOpenSourceDirectExchange()
    {
        DirectExchange directExchange = new DirectExchange(EXCHANGE_FAST_INCREMENT_KLOCWORK_OPENSOURCE);
        directExchange.setDelayed(true);
        return directExchange;
    }

    @Bean
    public Binding klocworkFastIncrOpenSourceQueueBind(Queue klocworkFastIncrOpenSourceQueue, DirectExchange klocworkFastIncrOpenSourceDirectExchange)
    {
        return BindingBuilder.bind(klocworkFastIncrOpenSourceQueue).to(klocworkFastIncrOpenSourceDirectExchange).with(ROUTE_FAST_INCREMENT_KLOCWORK_OPENSOURCE);
    }

    @Bean
    public SimpleMessageListenerContainer klocworkFastIncrOpenSourceMessageListenerContainer(
            ConnectionFactory connectionFactory,
            CommonFastIncrementConsumer commonFastIncrementConsumer,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter)
    {
        return getSimpleMessageListenerContainer(QUEUE_FAST_INCREMENT_KLOCWORK_OPENSOURCE, commonFastIncrementConsumer, connectionFactory, jackson2JsonMessageConverter);
    }

    @NotNull
    protected SimpleMessageListenerContainer getSimpleMessageListenerContainer(
            String queueName,
            IConsumer fastIncrementConsumer,
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter)
    {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(queueName);
        container.setConcurrentConsumers(10);
        container.setMaxConcurrentConsumers(10);
        container.setStartConsumerMinInterval(10000);
        container.setConsecutiveActiveTrigger(10);
        MessageListenerAdapter adapter = new MessageListenerAdapter(fastIncrementConsumer, "consumer");
        adapter.setMessageConverter(jackson2JsonMessageConverter);
        container.setMessageListener(adapter);
        return container;
    }
}
