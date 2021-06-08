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

import com.tencent.bk.codecc.defect.condition.AsyncReportCondition;
import com.tencent.bk.codecc.defect.consumer.SuperLargeDefectCommitConsumer;
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
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import static com.tencent.devops.common.web.mq.ConstantsKt.*;

/**
 * 超大项目队列定义及绑定交换器
 *
 * @version V1.0
 * @date 2019/5/27
 */
@Configuration
@Slf4j
@Conditional(AsyncReportCondition.class)
public class CommitDefectSuperLargeProjRabbitMQConfig extends AbstractCommitDefectRabbitMQConfig
{
    /**
     * 大项目并发消费者数
     */
    private static final int SUPER_LARGE_PROJ_CONCURRENT_CONSUMERS = 1;

    /**
     * 大项目最大并发消费者数
     */
    private static final int SUPER_LARGE_PROJ_MAX_CONCURRENT_CONSUMERS = 1;

    @Bean
    public Queue superLargeProjCommitQueue()
    {
        return new Queue(QUEUE_DEFECT_COMMIT_SUPER_LARGE);
    }

    @Bean
    public DirectExchange superLargeProjExchange()
    {
        DirectExchange directExchange = new DirectExchange(EXCHANGE_DEFECT_COMMIT_SUPER_LARGE);
        directExchange.setDelayed(true);
        return directExchange;
    }

    @Bean
    public Binding superLargeProjQueueBind(Queue superLargeProjCommitQueue, DirectExchange superLargeProjExchange)
    {
        return BindingBuilder.bind(superLargeProjCommitQueue).to(superLargeProjExchange).with(ROUTE_DEFECT_COMMIT_SUPER_LARGE);
    }

    @Bean
    public SimpleMessageListenerContainer superLargeProjMessageListenerContainer(
            ConnectionFactory connectionFactory,
            SuperLargeDefectCommitConsumer superLargeDefectCommitConsumer,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter) throws NoSuchMethodException
    {
        return messageListenerContainer(connectionFactory, superLargeDefectCommitConsumer, jackson2JsonMessageConverter,
                CONSUMER_METHOD_NAME, QUEUE_DEFECT_COMMIT_SUPER_LARGE, SUPER_LARGE_PROJ_CONCURRENT_CONSUMERS, SUPER_LARGE_PROJ_MAX_CONCURRENT_CONSUMERS);
    }
}
