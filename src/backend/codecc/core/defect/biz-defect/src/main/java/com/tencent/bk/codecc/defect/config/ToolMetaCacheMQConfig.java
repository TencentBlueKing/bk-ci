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

import com.tencent.bk.codecc.defect.consumer.RefreshToolMetaCacheConsumer;
import com.tencent.devops.common.util.IPUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_REFRESH_TOOLMETA_CACHE;
import static com.tencent.devops.common.web.mq.ConstantsKt.QUEUE_REFRESH_TOOLMETA_CACHE;

/**
 * 工厂类自动配置
 *
 * @version V1.0
 * @date 2019/5/27
 */
//@Configuration
@Slf4j
public class ToolMetaCacheMQConfig
{
    @Value("${server.port:#{null}}")
    private String localPort;

    @Bean
    public RabbitAdmin toolMetaCacheRabbitAdmin(ConnectionFactory connectionFactory)
    {
        return new RabbitAdmin(connectionFactory);
    }

    @Bean
    public Queue toolMetaCacheQueue()
    {
        String queueName = String.format("%s.%s.%s", QUEUE_REFRESH_TOOLMETA_CACHE, IPUtils.INSTANCE.getInnerIP(), localPort);
        return new Queue(queueName);
    }

    @Bean
    public FanoutExchange toolMetaCacheFanoutExchange()
    {
        FanoutExchange fanoutExchange = new FanoutExchange(EXCHANGE_REFRESH_TOOLMETA_CACHE, false, true);
        fanoutExchange.setDelayed(true);
        return fanoutExchange;
    }

    @Bean
    public Binding toolMetaCacheQueueBind(Queue toolMetaCacheQueue, FanoutExchange toolMetaCacheFanoutExchange)
    {
        return BindingBuilder.bind(toolMetaCacheQueue).to(toolMetaCacheFanoutExchange);
    }

    /**
     * 手动注册容器
     *
     * @param connectionFactory
     */
    @Bean
    public SimpleMessageListenerContainer toolMetaCacheMessageListenerContainer(
            ConnectionFactory connectionFactory,
            Queue toolMetaCacheQueue,
            RabbitAdmin toolMetaCacheRabbitAdmin,
            RefreshToolMetaCacheConsumer refreshToolMetaCacheConsumer,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter)
    {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(toolMetaCacheQueue.getName());
        container.setConcurrentConsumers(2);
        container.setMaxConcurrentConsumers(2);
        container.setAmqpAdmin(toolMetaCacheRabbitAdmin);
        container.setStartConsumerMinInterval(10000);
        container.setConsecutiveActiveTrigger(5);
        MessageListenerAdapter adapter = new MessageListenerAdapter(refreshToolMetaCacheConsumer, "refreshToolMetaCache");
        adapter.setMessageConverter(jackson2JsonMessageConverter);
        container.setMessageListener(adapter);
        return container;
    }
}
