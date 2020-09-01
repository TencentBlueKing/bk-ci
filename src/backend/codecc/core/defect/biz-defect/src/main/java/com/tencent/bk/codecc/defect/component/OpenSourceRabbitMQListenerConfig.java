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

package com.tencent.bk.codecc.defect.component;

import com.tencent.bk.codecc.defect.consumer.DefectCommitConsumer;
import com.tencent.bk.codecc.defect.vo.CommitDefectVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.MethodRabbitListenerEndpoint;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;

import static com.tencent.devops.common.web.mq.ConstantsKt.*;
import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_OPENSOURCE_DEFECT_COMMIT_CLOC;
import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_OPENSOURCE_DEFECT_COMMIT_DUPC;

/**
 * 工厂类自动配置
 *
 * @version V1.0
 * @date 2019/5/27
 */
@Configuration
@Slf4j
@ConditionalOnProperty(prefix = "spring.application", name = "name", havingValue = "opensourcereport")
public class OpenSourceRabbitMQListenerConfig
{
    @Autowired
    private BeanFactory beanFactory;

    @Bean
    public Queue lintCommitQueue()
    {
        return new Queue(QUEUE_OPENSOURCE_DEFECT_COMMIT_LINT);
    }

    @Bean
    public FanoutExchange lintFanoutExchange()
    {
        FanoutExchange fanoutExchange = new FanoutExchange(EXCHANGE_OPENSOURCE_DEFECT_COMMIT_LINT, true, false);
        fanoutExchange.setDelayed(true);
        return fanoutExchange;
    }

    @Bean
    public Binding lintQueueBind(Queue lintCommitQueue, FanoutExchange lintFanoutExchange)
    {
        return BindingBuilder.bind(lintCommitQueue).to(lintFanoutExchange);
    }

    @Bean
    public Queue ccnCommitQueue()
    {
        return new Queue(QUEUE_OPENSOURCE_DEFECT_COMMIT_CCN);
    }

    @Bean
    public FanoutExchange ccnFanoutExchange()
    {
        FanoutExchange fanoutExchange = new FanoutExchange(EXCHANGE_OPENSOURCE_DEFECT_COMMIT_CCN, true, false);
        fanoutExchange.setDelayed(true);
        return fanoutExchange;
    }

    @Bean
    public Binding ccnQueueBind(Queue ccnCommitQueue, FanoutExchange ccnFanoutExchange)
    {
        return BindingBuilder.bind(ccnCommitQueue).to(ccnFanoutExchange);
    }

    @Bean
    public Queue dupcCommitQueue()
    {
        return new Queue(QUEUE_OPENSOURCE_DEFECT_COMMIT_DUPC);
    }

    @Bean
    public FanoutExchange dupcFanoutExchange()
    {
        FanoutExchange fanoutExchange = new FanoutExchange(EXCHANGE_OPENSOURCE_DEFECT_COMMIT_DUPC, true, false);
        fanoutExchange.setDelayed(true);
        return fanoutExchange;
    }

    @Bean
    public Binding dupcQueueBind(Queue dupcCommitQueue, FanoutExchange dupcFanoutExchange)
    {
        return BindingBuilder.bind(dupcCommitQueue).to(dupcFanoutExchange);
    }

    @Bean
    public Queue clocCommitQueue()
    {
        return new Queue(QUEUE_OPENSOURCE_DEFECT_COMMIT_CLOC);
    }

    @Bean
    public FanoutExchange clocFanoutExchange()
    {
        FanoutExchange fanoutExchange = new FanoutExchange(EXCHANGE_OPENSOURCE_DEFECT_COMMIT_CLOC, true, false);
        fanoutExchange.setDelayed(true);
        return fanoutExchange;
    }

    @Bean
    public Binding clocQueueBind(Queue clocCommitQueue, FanoutExchange clocFanoutExchange)
    {
        return BindingBuilder.bind(clocCommitQueue).to(clocFanoutExchange);
    }

    @Bean
    public Queue pinpointCommitQueue()
    {
        return new Queue(QUEUE_OPENSOURCE_DEFECT_COMMIT_PINPOINT);
    }

    @Bean
    public FanoutExchange pinpointFanoutExchange()
    {
        FanoutExchange fanoutExchange = new FanoutExchange(EXCHANGE_OPENSOURCE_DEFECT_COMMIT_PINPOINT, true, false);
        fanoutExchange.setDelayed(true);
        return fanoutExchange;
    }

    @Bean
    public Binding pinpointQueueBind(Queue pinpointCommitQueue, FanoutExchange pinpointFanoutExchange)
    {
        return BindingBuilder.bind(pinpointCommitQueue).to(pinpointFanoutExchange);
    }

    /**
     * 手动注册容器
     *
     * @param connectionFactory
     */
    @Bean
    public SimpleMessageListenerContainer lintMessageListenerContainer(
            ConnectionFactory connectionFactory,
            DefectCommitConsumer defectCommitConsumer,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter) throws NoSuchMethodException
    {
        return messageListenerContainer(connectionFactory, defectCommitConsumer, jackson2JsonMessageConverter,
                "lintCommitDefect", QUEUE_OPENSOURCE_DEFECT_COMMIT_LINT);
    }

    @Bean
    public SimpleMessageListenerContainer ccnMessageListenerContainer(
            ConnectionFactory connectionFactory,
            DefectCommitConsumer defectCommitConsumer,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter) throws NoSuchMethodException
    {
        return messageListenerContainer(connectionFactory, defectCommitConsumer, jackson2JsonMessageConverter,
                "ccnCommitDefect", QUEUE_OPENSOURCE_DEFECT_COMMIT_CCN);
    }

    @Bean
    public SimpleMessageListenerContainer dupcMessageListenerContainer(
            ConnectionFactory connectionFactory,
            DefectCommitConsumer defectCommitConsumer,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter) throws NoSuchMethodException
    {
        return messageListenerContainer(connectionFactory, defectCommitConsumer, jackson2JsonMessageConverter,
                "dupcCommitDefect", QUEUE_OPENSOURCE_DEFECT_COMMIT_DUPC);
    }

    @Bean
    public SimpleMessageListenerContainer clocMessageListenerContainer(
            ConnectionFactory connectionFactory,
            DefectCommitConsumer defectCommitConsumer,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter) throws NoSuchMethodException
    {
        return messageListenerContainer(connectionFactory, defectCommitConsumer, jackson2JsonMessageConverter,
                "clocCommitDefect", QUEUE_OPENSOURCE_DEFECT_COMMIT_CLOC);
    }

    @Bean
    public SimpleMessageListenerContainer pinpointMessageListenerContainer(
            ConnectionFactory connectionFactory,
            DefectCommitConsumer defectCommitConsumer,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter) throws NoSuchMethodException
    {
        return messageListenerContainer(connectionFactory, defectCommitConsumer, jackson2JsonMessageConverter,
                "pinpointCommitDefect", QUEUE_OPENSOURCE_DEFECT_COMMIT_PINPOINT);
    }

    public SimpleMessageListenerContainer messageListenerContainer(
            ConnectionFactory connectionFactory,
            DefectCommitConsumer defectCommitConsumer,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter, String methodName, String queueName) throws NoSuchMethodException
    {
        MethodRabbitListenerEndpoint endpoint = new MethodRabbitListenerEndpoint();
        DefaultMessageHandlerMethodFactory defaultMessageHandlerMethodFactory = new DefaultMessageHandlerMethodFactory();
        defaultMessageHandlerMethodFactory.setBeanFactory(beanFactory);
        defaultMessageHandlerMethodFactory.afterPropertiesSet();

        endpoint.setBean(defectCommitConsumer);
        endpoint.setMethod(defectCommitConsumer.getClass().getDeclaredMethod(methodName, CommitDefectVO.class));
        endpoint.setMessageHandlerMethodFactory(defaultMessageHandlerMethodFactory);
        endpoint.setBeanFactory(beanFactory);

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setMessageConverter(jackson2JsonMessageConverter);
        SimpleMessageListenerContainer container = factory.createListenerContainer(endpoint);
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(queueName);
        container.setConcurrentConsumers(16);
        container.setMaxConcurrentConsumers(16);
        return container;
    }
}
