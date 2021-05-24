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

import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_FAST_INCREMENT_CCN;
import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_FAST_INCREMENT_CLOC;
import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_FAST_INCREMENT_DUPC;
import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_FAST_INCREMENT_LINT;
import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_FAST_INCREMENT_STAT;
import static com.tencent.devops.common.web.mq.ConstantsKt.QUEUE_FAST_INCREMENT_CCN;
import static com.tencent.devops.common.web.mq.ConstantsKt.QUEUE_FAST_INCREMENT_CLOC;
import static com.tencent.devops.common.web.mq.ConstantsKt.QUEUE_FAST_INCREMENT_DUPC;
import static com.tencent.devops.common.web.mq.ConstantsKt.QUEUE_FAST_INCREMENT_LINT;
import static com.tencent.devops.common.web.mq.ConstantsKt.QUEUE_FAST_INCREMENT_STAT;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_FAST_INCREMENT_CCN;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_FAST_INCREMENT_CLOC;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_FAST_INCREMENT_DUPC;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_FAST_INCREMENT_LINT;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_FAST_INCREMENT_STAT;

import com.tencent.bk.codecc.defect.condition.AsyncReportCondition;
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
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

/**
 * 快速增量的消息队列配置
 *
 * @version V1.0
 * @date 2020/08/07
 */
@Configuration
@Slf4j
@Conditional(AsyncReportCondition.class)
public class FastIncrementRabbitMQConfig
{
    @Bean
    public Queue lintFastIncrementQueue()
    {
        return new Queue(QUEUE_FAST_INCREMENT_LINT);
    }

    @Bean
    public DirectExchange lintFastIncrementDirectExchange()
    {
        DirectExchange directExchange = new DirectExchange(EXCHANGE_FAST_INCREMENT_LINT);
        directExchange.setDelayed(true);
        return directExchange;
    }

    @Bean
    public Binding lintFastIncrementQueueBind(Queue lintFastIncrementQueue, DirectExchange lintFastIncrementDirectExchange)
    {
        return BindingBuilder.bind(lintFastIncrementQueue).to(lintFastIncrementDirectExchange).with(ROUTE_FAST_INCREMENT_LINT);
    }

    @Bean
    public SimpleMessageListenerContainer lintFastIncrementMessageListenerContainer(
            ConnectionFactory connectionFactory,
            LintFastIncrementConsumer lintFastIncrementConsumer,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter)
    {
        return getSimpleMessageListenerContainer(QUEUE_FAST_INCREMENT_LINT, lintFastIncrementConsumer, connectionFactory, jackson2JsonMessageConverter);
    }

    @Bean
    public Queue ccnFastIncrementQueue()
    {
        return new Queue(QUEUE_FAST_INCREMENT_CCN);
    }

    @Bean
    public DirectExchange ccnFastIncrementDirectExchange()
    {
        DirectExchange directExchange = new DirectExchange(EXCHANGE_FAST_INCREMENT_CCN);
        directExchange.setDelayed(true);
        return directExchange;
    }

    @Bean
    public Binding ccnFastIncrementQueueBind(Queue ccnFastIncrementQueue, DirectExchange ccnFastIncrementDirectExchange)
    {
        return BindingBuilder.bind(ccnFastIncrementQueue).to(ccnFastIncrementDirectExchange).with(ROUTE_FAST_INCREMENT_CCN);
    }

    @Bean
    public SimpleMessageListenerContainer ccnFastIncrementMessageListenerContainer(
            ConnectionFactory connectionFactory,
            CCNFastIncrementConsumer ccnFastIncrementConsumer,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter)
    {
        return getSimpleMessageListenerContainer(QUEUE_FAST_INCREMENT_CCN, ccnFastIncrementConsumer, connectionFactory, jackson2JsonMessageConverter);
    }

    @Bean
    public Queue dupcFastIncrementQueue()
    {
        return new Queue(QUEUE_FAST_INCREMENT_DUPC);
    }

    @Bean
    public DirectExchange dupcFastIncrementDirectExchange()
    {
        DirectExchange directExchange = new DirectExchange(EXCHANGE_FAST_INCREMENT_DUPC);
        directExchange.setDelayed(true);
        return directExchange;
    }

    @Bean
    public Binding dupcFastIncrementQueueBind(Queue dupcFastIncrementQueue, DirectExchange dupcFastIncrementDirectExchange)
    {
        return BindingBuilder.bind(dupcFastIncrementQueue).to(dupcFastIncrementDirectExchange).with(ROUTE_FAST_INCREMENT_DUPC);
    }

    @Bean
    public SimpleMessageListenerContainer dupcFastIncrementMessageListenerContainer(
            ConnectionFactory connectionFactory,
            DUPCFastIncrementConsumer dupcFastIncrementConsumer,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter)
    {
        return getSimpleMessageListenerContainer(QUEUE_FAST_INCREMENT_DUPC, dupcFastIncrementConsumer, connectionFactory, jackson2JsonMessageConverter);
    }

    @Bean
    public Queue clocFastIncrementQueue()
    {
        return new Queue(QUEUE_FAST_INCREMENT_CLOC);
    }

    @Bean
    public DirectExchange clocFastIncrementDirectExchange()
    {
        DirectExchange directExchange = new DirectExchange(EXCHANGE_FAST_INCREMENT_CLOC);
        directExchange.setDelayed(true);
        return directExchange;
    }

    @Bean
    public Binding clocFastIncrementQueueBind(Queue clocFastIncrementQueue, DirectExchange clocFastIncrementDirectExchange)
    {
        return BindingBuilder.bind(clocFastIncrementQueue).to(clocFastIncrementDirectExchange).with(ROUTE_FAST_INCREMENT_CLOC);
    }

    @Bean
    public SimpleMessageListenerContainer clocFastIncrementMessageListenerContainer(
            ConnectionFactory connectionFactory,
            CLOCFastIncrementConsumer clocFastIncrementConsumer,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter) {
        return getSimpleMessageListenerContainer(QUEUE_FAST_INCREMENT_CLOC, clocFastIncrementConsumer,
                connectionFactory, jackson2JsonMessageConverter);
    }

    @Bean
    public Queue statFastIncrementQueue() {
        return new Queue(QUEUE_FAST_INCREMENT_STAT);
    }

    @Bean
    public DirectExchange statFastIncrementDirectExchange() {
        DirectExchange directExchange = new DirectExchange(EXCHANGE_FAST_INCREMENT_STAT);
        directExchange.setDelayed(true);
        return directExchange;
    }

    @Bean
    public Binding statFastIncrementQueueBind(Queue statFastIncrementQueue,
            DirectExchange statFastIncrementDirectExchange) {
        return BindingBuilder.bind(statFastIncrementQueue).to(statFastIncrementDirectExchange)
                .with(ROUTE_FAST_INCREMENT_STAT);
    }

    @Bean
    public SimpleMessageListenerContainer statFastIncrementMessageListenerContainer(
            ConnectionFactory connectionFactory,
            StatFastIncrementConsumer statFastIncrementConsumer,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter) {
        return getSimpleMessageListenerContainer(QUEUE_FAST_INCREMENT_STAT, statFastIncrementConsumer,
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
