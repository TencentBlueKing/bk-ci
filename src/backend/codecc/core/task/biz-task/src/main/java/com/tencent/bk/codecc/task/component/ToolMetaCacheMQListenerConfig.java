/*
 * Tencent is pleased to support the open source community by making BlueKing available.
 * Copyright (C) 2017-2018 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.bk.codecc.task.component;

import com.tencent.bk.codecc.task.consumer.RefreshToolMetaCacheConsumer;
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
public class ToolMetaCacheMQListenerConfig {
    @Value("${server.port:#{null}}")
    private String localPort;

    @Bean
    public RabbitAdmin toolMetaCacheRabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    @Bean
    public Queue toolMetaCacheQueue() {
        String queueName =
                String.format("%s.%s.%s", QUEUE_REFRESH_TOOLMETA_CACHE, IPUtils.INSTANCE.getInnerIP(), localPort);
        return new Queue(queueName);
    }

    @Bean
    public FanoutExchange toolMetaCacheFanoutExchange() {
        FanoutExchange fanoutExchange = new FanoutExchange(EXCHANGE_REFRESH_TOOLMETA_CACHE, false, true);
        fanoutExchange.setDelayed(true);
        return fanoutExchange;
    }

    @Bean
    public Binding toolMetaCacheQueueBind(Queue toolMetaCacheQueue, FanoutExchange toolMetaCacheFanoutExchange) {
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
            Jackson2JsonMessageConverter jackson2JsonMessageConverter) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(toolMetaCacheQueue.getName());
        container.setConcurrentConsumers(2);
        container.setMaxConcurrentConsumers(2);
        container.setAmqpAdmin(toolMetaCacheRabbitAdmin);
        container.setStartConsumerMinInterval(10000);
        container.setConsecutiveActiveTrigger(5);
        MessageListenerAdapter adapter =
            new MessageListenerAdapter(refreshToolMetaCacheConsumer, "refreshToolMetaCache");
        adapter.setMessageConverter(jackson2JsonMessageConverter);
        container.setMessageListener(adapter);
        return container;
    }
}
