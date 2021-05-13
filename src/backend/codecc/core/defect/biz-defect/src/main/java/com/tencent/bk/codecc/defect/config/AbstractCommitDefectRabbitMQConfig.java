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

import com.tencent.bk.codecc.defect.consumer.AbstractDefectCommitConsumer;
import com.tencent.bk.codecc.defect.vo.CommitDefectVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.MethodRabbitListenerEndpoint;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;

/**
 * 工厂类自动配置
 *
 * @version V1.0
 * @date 2019/5/27
 */
@Configuration
@Slf4j
public class AbstractCommitDefectRabbitMQConfig
{
    @Autowired
    private BeanFactory beanFactory;

    /**
     * 最大并发消费者数
     */
    protected static final String CONSUMER_METHOD_NAME = "commitDefect";

    /**
     * 自定义监听容器
     * @param connectionFactory
     * @param defectCommitConsumer
     * @param jackson2JsonMessageConverter
     * @param methodName
     * @param queueName
     * @param concurrentConsumers
     * @param maxConcurrentConsumers
     * @return
     * @throws NoSuchMethodException
     */
    public SimpleMessageListenerContainer messageListenerContainer(
            ConnectionFactory connectionFactory,
            AbstractDefectCommitConsumer defectCommitConsumer,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter,
            String methodName,
            String queueName,
            int concurrentConsumers,
            int maxConcurrentConsumers) throws NoSuchMethodException
    {
        MethodRabbitListenerEndpoint endpoint = new MethodRabbitListenerEndpoint();
        DefaultMessageHandlerMethodFactory defaultMessageHandlerMethodFactory = new DefaultMessageHandlerMethodFactory();
        defaultMessageHandlerMethodFactory.setBeanFactory(beanFactory);
        defaultMessageHandlerMethodFactory.afterPropertiesSet();

        endpoint.setBean(defectCommitConsumer);
        endpoint.setMethod(defectCommitConsumer.getClass().getMethod(methodName, CommitDefectVO.class));
        endpoint.setMessageHandlerMethodFactory(defaultMessageHandlerMethodFactory);
        endpoint.setBeanFactory(beanFactory);

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setMessageConverter(jackson2JsonMessageConverter);
        SimpleMessageListenerContainer container = factory.createListenerContainer(endpoint);
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(queueName);
        container.setConcurrentConsumers(concurrentConsumers);
        container.setMaxConcurrentConsumers(maxConcurrentConsumers);
        return container;
    }
}
