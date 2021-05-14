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

import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_FAST_INCREMENT_CCN_OPENSOURCE;
import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_FAST_INCREMENT_CLOC_OPENSOURCE;
import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_FAST_INCREMENT_DUPC_OPENSOURCE;
import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_FAST_INCREMENT_LINT_OPENSOURCE;
import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_FAST_INCREMENT_STAT_OPENSOURCE;
import static com.tencent.devops.common.web.mq.ConstantsKt.QUEUE_FAST_INCREMENT_CCN_OPENSOURCE;
import static com.tencent.devops.common.web.mq.ConstantsKt.QUEUE_FAST_INCREMENT_CLOC_OPENSOURCE;
import static com.tencent.devops.common.web.mq.ConstantsKt.QUEUE_FAST_INCREMENT_DUPC_OPENSOURCE;
import static com.tencent.devops.common.web.mq.ConstantsKt.QUEUE_FAST_INCREMENT_LINT_OPENSOURCE;
import static com.tencent.devops.common.web.mq.ConstantsKt.QUEUE_FAST_INCREMENT_STAT_OPENSOURCE;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_FAST_INCREMENT_CCN_OPENSOURCE;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_FAST_INCREMENT_CLOC_OPENSOURCE;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_FAST_INCREMENT_DUPC_OPENSOURCE;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_FAST_INCREMENT_LINT_OPENSOURCE;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_FAST_INCREMENT_STAT_OPENSOURCE;

import com.tencent.bk.codecc.defect.consumer.CCNFastIncrementConsumer;
import com.tencent.bk.codecc.defect.consumer.CLOCFastIncrementConsumer;
import com.tencent.bk.codecc.defect.consumer.DUPCFastIncrementConsumer;
import com.tencent.bk.codecc.defect.consumer.LintFastIncrementConsumer;
import com.tencent.bk.codecc.defect.consumer.StatFastIncrementConsumer;
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

/**
 * 快速增量的消息队列配置
 *
 * @version V1.0
 * @date 2020/08/07
 */
@Configuration
@Slf4j
@ConditionalOnProperty(prefix = "spring.application", name = "name", havingValue = "opensourcereport")
public class FastIncrementOpenSourceRabbitMQConfig
{
    @Bean
    public Queue lintFastIncrOpenSourceQueue()
    {
        return new Queue(QUEUE_FAST_INCREMENT_LINT_OPENSOURCE);
    }

    @Bean
    public DirectExchange lintFastIncrOpenSourceDirectExchange()
    {
        DirectExchange directExchange = new DirectExchange(EXCHANGE_FAST_INCREMENT_LINT_OPENSOURCE);
        directExchange.setDelayed(true);
        return directExchange;
    }

    @Bean
    public Binding lintFastIncrOpenSourceQueueBind(Queue lintFastIncrOpenSourceQueue, DirectExchange lintFastIncrOpenSourceDirectExchange)
    {
        return BindingBuilder.bind(lintFastIncrOpenSourceQueue).to(lintFastIncrOpenSourceDirectExchange).with(ROUTE_FAST_INCREMENT_LINT_OPENSOURCE);
    }

    @Bean
    public SimpleMessageListenerContainer lintFastIncrOpenSourceMessageListenerContainer(
            ConnectionFactory connectionFactory,
            LintFastIncrementConsumer lintFastIncrementConsumer,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter)
    {
        return getSimpleMessageListenerContainer(QUEUE_FAST_INCREMENT_LINT_OPENSOURCE, lintFastIncrementConsumer, connectionFactory, jackson2JsonMessageConverter);
    }

    @Bean
    public Queue ccnFastIncrOpenSourceQueue()
    {
        return new Queue(QUEUE_FAST_INCREMENT_CCN_OPENSOURCE);
    }

    @Bean
    public DirectExchange ccnFastIncrOpenSourceDirectExchange()
    {
        DirectExchange directExchange = new DirectExchange(EXCHANGE_FAST_INCREMENT_CCN_OPENSOURCE);
        directExchange.setDelayed(true);
        return directExchange;
    }

    @Bean
    public Binding ccnFastIncrOpenSourceQueueBind(Queue ccnFastIncrOpenSourceQueue, DirectExchange ccnFastIncrOpenSourceDirectExchange)
    {
        return BindingBuilder.bind(ccnFastIncrOpenSourceQueue).to(ccnFastIncrOpenSourceDirectExchange).with(ROUTE_FAST_INCREMENT_CCN_OPENSOURCE);
    }

    @Bean
    public SimpleMessageListenerContainer ccnFastIncrOpenSourceMessageListenerContainer(
            ConnectionFactory connectionFactory,
            CCNFastIncrementConsumer ccnFastIncrementConsumer,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter)
    {
        return getSimpleMessageListenerContainer(QUEUE_FAST_INCREMENT_CCN_OPENSOURCE, ccnFastIncrementConsumer, connectionFactory, jackson2JsonMessageConverter);
    }

    @Bean
    public Queue dupcFastIncrOpenSourceQueue()
    {
        return new Queue(QUEUE_FAST_INCREMENT_DUPC_OPENSOURCE);
    }

    @Bean
    public DirectExchange dupcFastIncrOpenSourceDirectExchange()
    {
        DirectExchange directExchange = new DirectExchange(EXCHANGE_FAST_INCREMENT_DUPC_OPENSOURCE);
        directExchange.setDelayed(true);
        return directExchange;
    }

    @Bean
    public Binding dupcFastIncrOpenSourceQueueBind(Queue dupcFastIncrOpenSourceQueue, DirectExchange dupcFastIncrOpenSourceDirectExchange)
    {
        return BindingBuilder.bind(dupcFastIncrOpenSourceQueue).to(dupcFastIncrOpenSourceDirectExchange).with(ROUTE_FAST_INCREMENT_DUPC_OPENSOURCE);
    }

    @Bean
    public SimpleMessageListenerContainer dupcFastIncrOpenSourceMessageListenerContainer(
            ConnectionFactory connectionFactory,
            DUPCFastIncrementConsumer dupcFastIncrementConsumer,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter)
    {
        return getSimpleMessageListenerContainer(QUEUE_FAST_INCREMENT_DUPC_OPENSOURCE, dupcFastIncrementConsumer, connectionFactory, jackson2JsonMessageConverter);
    }

    @Bean
    public Queue clocFastIncrOpenSourceQueue()
    {
        return new Queue(QUEUE_FAST_INCREMENT_CLOC_OPENSOURCE);
    }

    @Bean
    public DirectExchange clocFastIncrOpenSourceDirectExchange()
    {
        DirectExchange directExchange = new DirectExchange(EXCHANGE_FAST_INCREMENT_CLOC_OPENSOURCE);
        directExchange.setDelayed(true);
        return directExchange;
    }

    @Bean
    public Binding clocFastIncrOpenSourceQueueBind(Queue clocFastIncrOpenSourceQueue, DirectExchange clocFastIncrOpenSourceDirectExchange)
    {
        return BindingBuilder.bind(clocFastIncrOpenSourceQueue).to(clocFastIncrOpenSourceDirectExchange).with(ROUTE_FAST_INCREMENT_CLOC_OPENSOURCE);
    }

    @Bean
    public SimpleMessageListenerContainer clocFastIncrOpenSourceMessageListenerContainer(
            ConnectionFactory connectionFactory,
            CLOCFastIncrementConsumer clocFastIncrementConsumer,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter) {
        return getSimpleMessageListenerContainer(QUEUE_FAST_INCREMENT_CLOC_OPENSOURCE, clocFastIncrementConsumer,
                connectionFactory, jackson2JsonMessageConverter);
    }

    @Bean
    public Queue statFastIncrOpenSourceQueue() {
        return new Queue(QUEUE_FAST_INCREMENT_STAT_OPENSOURCE);
    }

    @Bean
    public DirectExchange statFastIncrOpenSourceDirectExchange() {
        DirectExchange directExchange = new DirectExchange(EXCHANGE_FAST_INCREMENT_STAT_OPENSOURCE);
        directExchange.setDelayed(true);
        return directExchange;
    }

    @Bean
    public Binding statFastIncrOpenSourceQueueBind(Queue statFastIncrOpenSourceQueue,
            DirectExchange statFastIncrOpenSourceDirectExchange) {
        return BindingBuilder.bind(statFastIncrOpenSourceQueue).to(statFastIncrOpenSourceDirectExchange)
                .with(ROUTE_FAST_INCREMENT_STAT_OPENSOURCE);
    }

    @Bean
    public SimpleMessageListenerContainer statFastIncrOpenSourceMessageListenerContainer(
            ConnectionFactory connectionFactory,
            StatFastIncrementConsumer statFastIncrementConsumer,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter) {
        return getSimpleMessageListenerContainer(QUEUE_FAST_INCREMENT_STAT_OPENSOURCE, statFastIncrementConsumer,
                connectionFactory, jackson2JsonMessageConverter);
    }

    @NotNull
    protected SimpleMessageListenerContainer getSimpleMessageListenerContainer(
            String queueName,
            IConsumer fastIncrementConsumer,
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter) {
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
