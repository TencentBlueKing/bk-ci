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

package com.tencent.bk.codecc.schedule.config;

import com.rabbitmq.client.Channel;
import com.tencent.bk.codecc.schedule.consumer.AnalyzeTaskConsumer;
import com.tencent.bk.codecc.schedule.vo.PushVO;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.MethodRabbitListenerEndpoint;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;

import java.util.HashMap;
import java.util.Map;

import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_ANALYZE_DISPATCH;
import static com.tencent.devops.common.web.mq.ConstantsKt.QUEUE_ANALYZE_DISPATCH;
import static com.tencent.devops.common.web.mq.ConstantsKt.QUEUE_ANALYZE_DISPATCH_OPENSOURCE;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_ANALYZE_DISPATCH;
import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_ANALYZE_DISPATCH_OPENSOURCE;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_ANALYZE_DISPATCH_OPENSOURCE;

/**
 * 工厂类自动配置
 *
 * @version V1.0
 * @date 2019/5/27
 */
@Configuration
@Slf4j
public class RabbitMQListenerConfig {
    @Autowired
    private BeanFactory beanFactory;

    @Bean
    public Queue scheduleQueue() {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("x-max-priority", 10);
        return new Queue(QUEUE_ANALYZE_DISPATCH, true, false, false, arguments);
    }

    @Bean
    public DirectExchange scheduleExchange() {
        DirectExchange directExchange = new DirectExchange(EXCHANGE_ANALYZE_DISPATCH);
        directExchange.setDelayed(true);
        return directExchange;
    }

    @Bean
    public Binding scheduleQueueBind(Queue scheduleQueue, DirectExchange scheduleExchange) {
        return BindingBuilder.bind(scheduleQueue).to(scheduleExchange).with(ROUTE_ANALYZE_DISPATCH);
    }

    @Bean
    public Queue analyzeScheduleOpensourceQueue() {
        Map<String, Object> arguments = new HashMap<>();
        return new Queue(QUEUE_ANALYZE_DISPATCH_OPENSOURCE, true, false, false, arguments);
    }

    @Bean
    public DirectExchange analyzeScheduleOpensourceExchange() {
        DirectExchange directExchange = new DirectExchange(EXCHANGE_ANALYZE_DISPATCH_OPENSOURCE);
        directExchange.setDelayed(true);
        return directExchange;
    }

    @Bean
    public Binding analyzeScheduleOpensourceQueueBind(Queue analyzeScheduleOpensourceQueue, DirectExchange analyzeScheduleOpensourceExchange) {
        return BindingBuilder.bind(analyzeScheduleOpensourceQueue).to(analyzeScheduleOpensourceExchange).with(ROUTE_ANALYZE_DISPATCH_OPENSOURCE);
    }

    /**
     * 手动注册容器，设置AcknowledgeMode为MANUAL
     *
     * @param connectionFactory
     */
    @Bean
    public SimpleMessageListenerContainer messageListenerContainer(
            ConnectionFactory connectionFactory,
            AnalyzeTaskConsumer analyzeTaskConsumer,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter) throws NoSuchMethodException {
        return getSimpleMessageListenerContainer(QUEUE_ANALYZE_DISPATCH, connectionFactory, analyzeTaskConsumer, jackson2JsonMessageConverter);
    }

    /**
     * 手动注册容器，设置AcknowledgeMode为MANUAL
     *
     * @param connectionFactory
     */
    @Bean
    public SimpleMessageListenerContainer opensourceMessageListenerContainer(
            ConnectionFactory connectionFactory,
            AnalyzeTaskConsumer analyzeTaskConsumer,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter) throws NoSuchMethodException {
        return getSimpleMessageListenerContainer(QUEUE_ANALYZE_DISPATCH_OPENSOURCE, connectionFactory, analyzeTaskConsumer, jackson2JsonMessageConverter);
    }

    @NotNull
    protected SimpleMessageListenerContainer getSimpleMessageListenerContainer(
            String queueName,
            ConnectionFactory connectionFactory,
            AnalyzeTaskConsumer analyzeTaskConsumer,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter) throws NoSuchMethodException {
        MethodRabbitListenerEndpoint endpoint = new MethodRabbitListenerEndpoint();
        DefaultMessageHandlerMethodFactory defaultMessageHandlerMethodFactory = new DefaultMessageHandlerMethodFactory();
        defaultMessageHandlerMethodFactory.setBeanFactory(beanFactory);
        defaultMessageHandlerMethodFactory.afterPropertiesSet();

        endpoint.setBean(analyzeTaskConsumer);
        endpoint.setMethod(analyzeTaskConsumer.getClass().getDeclaredMethod("schedule", PushVO.class, Channel.class, Map.class));
        endpoint.setMessageHandlerMethodFactory(defaultMessageHandlerMethodFactory);
        endpoint.setBeanFactory(beanFactory);

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setMessageConverter(jackson2JsonMessageConverter);
        SimpleMessageListenerContainer container = factory.createListenerContainer(endpoint);
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(queueName);
        container.setAcknowledgeMode(AcknowledgeMode.MANUAL);

        return container;
    }
}
