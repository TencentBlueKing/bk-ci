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
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import static com.tencent.devops.common.web.mq.ConstantsKt.*;

/**
 * 工厂类自动配置
 *
 * @version V1.0
 * @date 2019/5/27
 */
@Configuration
@Slf4j
@Conditional(AsyncReportCondition.class)
public class CommitDefectLargeProjRabbitMQConfig extends AbstractCommitDefectRabbitMQConfig
{
    /**
     * 大项目并发消费者数
     */
    private static final int LARGE_PROJ_CONCURRENT_CONSUMERS = 1;

    /**
     * 大项目最大并发消费者数
     */
    private static final int LARGE_PROJ_MAX_CONCURRENT_CONSUMERS = 1;

    @Bean
    public Queue lintLargeProjCommitQueue()
    {
        return new Queue(QUEUE_DEFECT_COMMIT_LINT_LARGE);
    }

    @Bean
    public DirectExchange lintLargeProjExchange()
    {
        DirectExchange directExchange = new DirectExchange(EXCHANGE_DEFECT_COMMIT_LINT_LARGE);
        directExchange.setDelayed(true);
        return directExchange;
    }

    @Bean
    public Binding lintLargeProjQueueBind(Queue lintLargeProjCommitQueue, DirectExchange lintLargeProjExchange)
    {
        return BindingBuilder.bind(lintLargeProjCommitQueue).to(lintLargeProjExchange)
            .with(ROUTE_DEFECT_COMMIT_LINT_LARGE);
    }

    @Bean
    public Queue ccnLargeProjCommitQueue()
    {
        return new Queue(QUEUE_DEFECT_COMMIT_CCN_LARGE);
    }

    @Bean
    public DirectExchange ccnLargeProjExchange()
    {
        DirectExchange directExchange = new DirectExchange(EXCHANGE_DEFECT_COMMIT_CCN_LARGE);
        directExchange.setDelayed(true);
        return directExchange;
    }

    @Bean
    public Binding ccnLargeProjQueueBind(Queue ccnLargeProjCommitQueue, DirectExchange ccnLargeProjExchange)
    {
        return BindingBuilder.bind(ccnLargeProjCommitQueue).to(ccnLargeProjExchange)
            .with(ROUTE_DEFECT_COMMIT_CCN_LARGE);
    }

    @Bean
    public Queue dupcLargeProjCommitQueue()
    {
        return new Queue(QUEUE_DEFECT_COMMIT_DUPC_LARGE);
    }

    @Bean
    public DirectExchange dupcLargeProjExchange()
    {
        DirectExchange directExchange = new DirectExchange(EXCHANGE_DEFECT_COMMIT_DUPC_LARGE);
        directExchange.setDelayed(true);
        return directExchange;
    }

    @Bean
    public Binding dupcLargeProjQueueBind(Queue dupcLargeProjCommitQueue, DirectExchange dupcLargeProjExchange)
    {
        return BindingBuilder.bind(dupcLargeProjCommitQueue).to(dupcLargeProjExchange)
            .with(ROUTE_DEFECT_COMMIT_DUPC_LARGE);
    }

    @Bean
    public Queue clocLargeProjCommitQueue()
    {
        return new Queue(QUEUE_DEFECT_COMMIT_CLOC_LARGE);
    }

    @Bean
    public DirectExchange clocLargeProjExchange()
    {
        DirectExchange directExchange = new DirectExchange(EXCHANGE_DEFECT_COMMIT_CLOC_LARGE);
        directExchange.setDelayed(true);
        return directExchange;
    }

    @Bean
    public Binding clocLargeProjQueueBind(Queue clocLargeProjCommitQueue, DirectExchange clocLargeProjExchange)
    {
        return BindingBuilder.bind(clocLargeProjCommitQueue).to(clocLargeProjExchange)
            .with(ROUTE_DEFECT_COMMIT_CLOC_LARGE);
    }

    @Bean
    public Queue statLargeProjCommitQueue()
    {
        return new Queue(QUEUE_DEFECT_COMMIT_STAT_LARGE);
    }

    @Bean
    public DirectExchange statLargeProjExchange()
    {
        DirectExchange directExchange = new DirectExchange(EXCHANGE_DEFECT_COMMIT_STAT_LARGE);
        directExchange.setDelayed(true);
        return directExchange;
    }

    @Bean
    public Binding statLargeProjQueueBind(Queue statLargeProjCommitQueue, DirectExchange statLargeProjExchange)
    {
        return BindingBuilder.bind(statLargeProjCommitQueue).to(statLargeProjExchange).with(ROUTE_DEFECT_COMMIT_STAT_LARGE);
    }

    @Bean
    public SimpleMessageListenerContainer lintLargeProjMessageListenerContainer(
            ConnectionFactory connectionFactory,
            LintDefectCommitConsumer lintDefectCommitConsumer,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter) throws NoSuchMethodException
    {
        return messageListenerContainer(connectionFactory, lintDefectCommitConsumer, jackson2JsonMessageConverter,
                CONSUMER_METHOD_NAME, QUEUE_DEFECT_COMMIT_LINT_LARGE,
            LARGE_PROJ_CONCURRENT_CONSUMERS, LARGE_PROJ_MAX_CONCURRENT_CONSUMERS);
    }

    @Bean
    public SimpleMessageListenerContainer ccnLargeProjMessageListenerContainer(
            ConnectionFactory connectionFactory,
            CCNDefectCommitConsumer ccnDefectCommitConsumer,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter) throws NoSuchMethodException
    {
        return messageListenerContainer(connectionFactory, ccnDefectCommitConsumer, jackson2JsonMessageConverter,
                CONSUMER_METHOD_NAME, QUEUE_DEFECT_COMMIT_CCN_LARGE,
            LARGE_PROJ_CONCURRENT_CONSUMERS, LARGE_PROJ_MAX_CONCURRENT_CONSUMERS);
    }

    @Bean
    public SimpleMessageListenerContainer dupcLargeProjMessageListenerContainer(
            ConnectionFactory connectionFactory,
            DUPCDefectCommitConsumer dupcDefectCommitConsumer,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter) throws NoSuchMethodException
    {
        return messageListenerContainer(connectionFactory, dupcDefectCommitConsumer, jackson2JsonMessageConverter,
                CONSUMER_METHOD_NAME, QUEUE_DEFECT_COMMIT_DUPC_LARGE,
            LARGE_PROJ_CONCURRENT_CONSUMERS, LARGE_PROJ_MAX_CONCURRENT_CONSUMERS);
    }

    @Bean
    public SimpleMessageListenerContainer clocLargeProjMessageListenerContainer(
            ConnectionFactory connectionFactory,
            CLOCDefectCommitConsumer clocDefectCommitConsumer,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter) throws NoSuchMethodException
    {
        return messageListenerContainer(connectionFactory, clocDefectCommitConsumer, jackson2JsonMessageConverter,
                CONSUMER_METHOD_NAME, QUEUE_DEFECT_COMMIT_CLOC_LARGE,
            LARGE_PROJ_CONCURRENT_CONSUMERS, LARGE_PROJ_MAX_CONCURRENT_CONSUMERS);
    }

    @Bean
    public SimpleMessageListenerContainer statLargeProjMessageListenerContainer(
            ConnectionFactory connectionFactory,
            StatDefectCommitConsumer statDefectCommitConsumer,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter) throws NoSuchMethodException
    {
        return messageListenerContainer(connectionFactory, statDefectCommitConsumer, jackson2JsonMessageConverter,
                CONSUMER_METHOD_NAME, QUEUE_DEFECT_COMMIT_STAT_LARGE, LARGE_PROJ_CONCURRENT_CONSUMERS, LARGE_PROJ_MAX_CONCURRENT_CONSUMERS);
    }
}
