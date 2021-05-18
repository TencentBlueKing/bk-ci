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

import com.tencent.bk.codecc.defect.consumer.CCNDefectCommitConsumer;
import com.tencent.bk.codecc.defect.consumer.CLOCDefectCommitConsumer;
import com.tencent.bk.codecc.defect.consumer.DUPCDefectCommitConsumer;
import com.tencent.bk.codecc.defect.consumer.LintDefectCommitConsumer;
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
@ConditionalOnProperty(prefix = "spring.application", name = "name", havingValue = "opensourcereport")
public class CommitDefectOpenSourceRabbitMQConfig extends AbstractCommitDefectRabbitMQConfig
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
    public Queue lintOpenSourceCommitQueue()
    {
        return new Queue(QUEUE_DEFECT_COMMIT_LINT_OPENSOURCE);
    }

    @Bean
    public DirectExchange lintOpenSourceDirectExchange()
    {
        DirectExchange directExchange = new DirectExchange(EXCHANGE_DEFECT_COMMIT_LINT_OPENSOURCE);
        directExchange.setDelayed(true);
        return directExchange;
    }

    @Bean
    public Binding lintOpenSourceQueueBind(Queue lintOpenSourceCommitQueue, DirectExchange lintOpenSourceDirectExchange)
    {
        return BindingBuilder.bind(lintOpenSourceCommitQueue).to(lintOpenSourceDirectExchange).with(ROUTE_DEFECT_COMMIT_LINT_OPENSOURCE);
    }

    @Bean
    public Queue ccnOpenSourceCommitQueue()
    {
        return new Queue(QUEUE_DEFECT_COMMIT_CCN_OPENSOURCE);
    }

    @Bean
    public DirectExchange ccnOpenSourceDirectExchange()
    {
        DirectExchange directExchange = new DirectExchange(EXCHANGE_DEFECT_COMMIT_CCN_OPENSOURCE);
        directExchange.setDelayed(true);
        return directExchange;
    }

    @Bean
    public Binding ccnOpenSourceQueueBind(Queue ccnOpenSourceCommitQueue, DirectExchange ccnOpenSourceDirectExchange)
    {
        return BindingBuilder.bind(ccnOpenSourceCommitQueue).to(ccnOpenSourceDirectExchange).with(ROUTE_DEFECT_COMMIT_CCN_OPENSOURCE);
    }

    @Bean
    public Queue dupcOpenSourceCommitQueue()
    {
        return new Queue(QUEUE_DEFECT_COMMIT_DUPC_OPENSOURCE);
    }

    @Bean
    public DirectExchange dupcOpenSourceDirectExchange()
    {
        DirectExchange directExchange = new DirectExchange(EXCHANGE_DEFECT_COMMIT_DUPC_OPENSOURCE);
        directExchange.setDelayed(true);
        return directExchange;
    }

    @Bean
    public Binding dupcOpenSourceQueueBind(Queue dupcOpenSourceCommitQueue, DirectExchange dupcOpenSourceDirectExchange)
    {
        return BindingBuilder.bind(dupcOpenSourceCommitQueue).to(dupcOpenSourceDirectExchange).with(ROUTE_DEFECT_COMMIT_DUPC_OPENSOURCE);
    }

    @Bean
    public Queue clocOpenSourceCommitQueue()
    {
        return new Queue(QUEUE_DEFECT_COMMIT_CLOC_OPENSOURCE);
    }

    @Bean
    public DirectExchange clocOpenSourceDirectExchange()
    {
        DirectExchange directExchange = new DirectExchange(EXCHANGE_DEFECT_COMMIT_CLOC_OPENSOURCE);
        directExchange.setDelayed(true);
        return directExchange;
    }

    @Bean
    public Binding clocOpenSourceQueueBind(Queue clocOpenSourceCommitQueue, DirectExchange clocOpenSourceDirectExchange)
    {
        return BindingBuilder.bind(clocOpenSourceCommitQueue).to(clocOpenSourceDirectExchange).with(ROUTE_DEFECT_COMMIT_CLOC_OPENSOURCE);
    }

    @Bean
    public Queue statOpenSourceCommitQueue()
    {
        return new Queue(QUEUE_DEFECT_COMMIT_STAT_OPENSOURCE);
    }

    @Bean
    public DirectExchange statOpenSourceDirectExchange()
    {
        DirectExchange directExchange = new DirectExchange(EXCHANGE_DEFECT_COMMIT_STAT_OPENSOURCE);
        directExchange.setDelayed(true);
        return directExchange;
    }

    @Bean
    public Binding statOpenSourceQueueBind(Queue statOpenSourceCommitQueue, DirectExchange statOpenSourceDirectExchange)
    {
        return BindingBuilder.bind(statOpenSourceCommitQueue).to(statOpenSourceDirectExchange).with(ROUTE_DEFECT_COMMIT_STAT_OPENSOURCE);
    }

    @Bean
    public SimpleMessageListenerContainer lintOpenSourceMessageListenerContainer(
            ConnectionFactory connectionFactory,
            LintDefectCommitConsumer lintDefectCommitConsumer,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter) throws NoSuchMethodException
    {
        return messageListenerContainer(connectionFactory, lintDefectCommitConsumer, jackson2JsonMessageConverter,
                CONSUMER_METHOD_NAME, QUEUE_DEFECT_COMMIT_LINT_OPENSOURCE, CONCURRENT_CONSUMERS, MAX_CONCURRENT_CONSUMERS);
    }

    @Bean
    public SimpleMessageListenerContainer ccnOpenSourceMessageListenerContainer(
            ConnectionFactory connectionFactory,
            CCNDefectCommitConsumer ccnDefectCommitConsumer,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter) throws NoSuchMethodException
    {
        return messageListenerContainer(connectionFactory, ccnDefectCommitConsumer, jackson2JsonMessageConverter,
                CONSUMER_METHOD_NAME, QUEUE_DEFECT_COMMIT_CCN_OPENSOURCE, CONCURRENT_CONSUMERS, MAX_CONCURRENT_CONSUMERS);
    }

    @Bean
    public SimpleMessageListenerContainer dupcOpenSourceMessageListenerContainer(
            ConnectionFactory connectionFactory,
            DUPCDefectCommitConsumer dupcDefectCommitConsumer,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter) throws NoSuchMethodException
    {
        return messageListenerContainer(connectionFactory, dupcDefectCommitConsumer, jackson2JsonMessageConverter,
                CONSUMER_METHOD_NAME, QUEUE_DEFECT_COMMIT_DUPC_OPENSOURCE, CONCURRENT_CONSUMERS, MAX_CONCURRENT_CONSUMERS);
    }

    @Bean
    public SimpleMessageListenerContainer clocOpenSourceMessageListenerContainer(
            ConnectionFactory connectionFactory,
            CLOCDefectCommitConsumer clocDefectCommitConsumer,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter) throws NoSuchMethodException
    {
        return messageListenerContainer(connectionFactory, clocDefectCommitConsumer, jackson2JsonMessageConverter,
                CONSUMER_METHOD_NAME, QUEUE_DEFECT_COMMIT_CLOC_OPENSOURCE, CONCURRENT_CONSUMERS, MAX_CONCURRENT_CONSUMERS);
    }
}
