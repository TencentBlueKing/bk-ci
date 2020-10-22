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

import com.tencent.bk.codecc.defect.consumer.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.tencent.devops.common.web.mq.ConstantsKt.*;

/**
 * 正常项目队列定义及绑定交换器
 *
 * @version V1.0
 * @date 2019/5/27
 */
@Configuration
@Slf4j
@ConditionalOnProperty(prefix = "spring.application", name = "name", havingValue = "asyncreport-ci")
public class PinpointNormalProjRabbitMQListenerConfig extends RabbitMQListenerConfig
{
    /**
     * 并发消费者数
     */
    private static final int CONCURRENT_CONSUMERS = 16;

    /**
     * 最大并发消费者数
     */
    private static final int MAX_CONCURRENT_CONSUMERS = 16;

    @Bean
    public Queue pinpointNewCommitQueue()
    {
        return new Queue(QUEUE_DEFECT_COMMIT_PINPOINT_NEW);
    }

    @Bean
    public DirectExchange pinpointNewDirectExchange()
    {
        DirectExchange directExchange = new DirectExchange(EXCHANGE_DEFECT_COMMIT_PINPOINT_NEW);
        directExchange.setDelayed(true);
        return directExchange;
    }

    @Bean
    public Binding pinpointNewQueueBind(Queue pinpointNewCommitQueue, DirectExchange pinpointNewDirectExchange)
    {
        return BindingBuilder.bind(pinpointNewCommitQueue).to(pinpointNewDirectExchange).with(ROUTE_DEFECT_COMMIT_PINPOINT_NEW);
    }

    @Bean
    public SimpleMessageListenerContainer pinpointNewMessageListenerContainer(
            ConnectionFactory connectionFactory,
            PinpointDefectCommitConsumer pinpointDefectCommitConsumer,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter) throws NoSuchMethodException
    {
        return messageListenerContainer(connectionFactory, pinpointDefectCommitConsumer, jackson2JsonMessageConverter,
                CONSUMER_METHOD_NAME, QUEUE_DEFECT_COMMIT_PINPOINT_NEW, CONCURRENT_CONSUMERS, MAX_CONCURRENT_CONSUMERS);
    }
}
