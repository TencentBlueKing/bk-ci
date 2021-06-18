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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy,modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.codecc.defect.config;

import com.tencent.bk.codecc.defect.condition.AsyncReportCondition;
import com.tencent.bk.codecc.defect.consumer.CCNCloseDefectStatisticConsumer;
import com.tencent.bk.codecc.defect.consumer.ClusterDefectConsumer;
import com.tencent.bk.codecc.defect.consumer.CodeScoringConsumer;
import com.tencent.bk.codecc.defect.consumer.CommonCloseDefectStatisticConsumer;
import com.tencent.bk.codecc.defect.consumer.LintCloseDefectStatisticConsumer;
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

import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_CLOSE_DEFECT_STATISTIC;
import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_CLOSE_DEFECT_STATISTIC_CCN;
import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_CLOSE_DEFECT_STATISTIC_LINT;
import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_DEFECT_COMMIT_CLUSTER;
import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_DEFECT_COMMIT_METRICS;
import static com.tencent.devops.common.web.mq.ConstantsKt.QUEUE_CLOSE_DEFECT_STATISTIC;
import static com.tencent.devops.common.web.mq.ConstantsKt.QUEUE_CLOSE_DEFECT_STATISTIC_CCN;
import static com.tencent.devops.common.web.mq.ConstantsKt.QUEUE_CLOSE_DEFECT_STATISTIC_LINT;
import static com.tencent.devops.common.web.mq.ConstantsKt.QUEUE_DEFECT_COMMIT_CLUSTER;
import static com.tencent.devops.common.web.mq.ConstantsKt.QUEUE_DEFECT_COMMIT_METRICS;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_CLOSE_DEFECT_STATISTIC;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_CLOSE_DEFECT_STATISTIC_CCN;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_CLOSE_DEFECT_STATISTIC_LINT;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_DEFECT_COMMIT_CLUSTER;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_DEFECT_COMMIT_METRICS;

/**
 * 普通非告警上报的非开源项目消息队列配置
 *
 * @version V1.0
 * @date 2020/09/22
 */
@Configuration
@Slf4j
@Conditional(AsyncReportCondition.class)
public class RabbitMQConfig
{
    @Bean
    public Queue metricsCommitQueue() {
        return new Queue(QUEUE_DEFECT_COMMIT_METRICS);
    }

    @Bean
    public DirectExchange metricsDirectExchange() {
        DirectExchange directExchange = new DirectExchange(EXCHANGE_DEFECT_COMMIT_METRICS);
        directExchange.setDelayed(true);
        return directExchange;
    }

    @Bean
    public Binding metricsQueueBind(Queue metricsCommitQueue, DirectExchange metricsDirectExchange) {
        return BindingBuilder.bind(metricsCommitQueue)
                .to(metricsDirectExchange)
                .with(ROUTE_DEFECT_COMMIT_METRICS);
    }

    @Bean
    public SimpleMessageListenerContainer metricsMessageListenerContainer(
            ConnectionFactory connectionFactory,
            CodeScoringConsumer codeScoringConsumerr,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter)
    {
        return getSimpleMessageListenerContainer(QUEUE_DEFECT_COMMIT_METRICS, codeScoringConsumerr,
                8, 8, connectionFactory, jackson2JsonMessageConverter);
    }

    @Bean
    public Queue clusterCommitQueue() {
        return new Queue(QUEUE_DEFECT_COMMIT_CLUSTER);
    }

    @Bean
    public DirectExchange clusterDirectExchange() {
        DirectExchange directExchange = new DirectExchange(EXCHANGE_DEFECT_COMMIT_CLUSTER);
        directExchange.setDelayed(true);
        return directExchange;
    }

    @Bean
    public Binding clustersQueueBind(Queue clusterCommitQueue, DirectExchange clusterDirectExchange) {
        return BindingBuilder.bind(clusterCommitQueue)
                .to(clusterDirectExchange)
                .with(ROUTE_DEFECT_COMMIT_CLUSTER);
    }

    @Bean
    public SimpleMessageListenerContainer clusterMessageListenerContainer(
            ConnectionFactory connectionFactory,
            ClusterDefectConsumer clusterDefectConsumer,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter)
    {
        return getSimpleMessageListenerContainer(QUEUE_DEFECT_COMMIT_CLUSTER, clusterDefectConsumer,
                8, 8, connectionFactory, jackson2JsonMessageConverter);
    }

    @Bean
    public Queue lintCloseDefectStatisticQueue() {
        return new Queue(QUEUE_CLOSE_DEFECT_STATISTIC_LINT);
    }

    @Bean
    public DirectExchange lintCloseDefectStatisticExchange() {
        DirectExchange directExchange = new DirectExchange(EXCHANGE_CLOSE_DEFECT_STATISTIC_LINT);
        directExchange.setDelayed(true);
        return directExchange;
    }

    @Bean
    public Binding lintCloseDefectStatisticQueueBind(Queue lintCloseDefectStatisticQueue,
                                                     DirectExchange lintCloseDefectStatisticExchange) {
        return BindingBuilder.bind(lintCloseDefectStatisticQueue)
                .to(lintCloseDefectStatisticExchange)
                .with(ROUTE_CLOSE_DEFECT_STATISTIC_LINT);
    }

    @Bean
    public SimpleMessageListenerContainer lintCloseDefectStatisticMessageListenerContainer(
            ConnectionFactory connectionFactory,
            LintCloseDefectStatisticConsumer lintCloseDefectStatisticConsumer,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter)
    {
        return getSimpleMessageListenerContainer(QUEUE_CLOSE_DEFECT_STATISTIC_LINT, lintCloseDefectStatisticConsumer,
                8, 8, connectionFactory, jackson2JsonMessageConverter);
    }

    @Bean
    public Queue ccnCloseDefectStatisticQueue() {
        return new Queue(QUEUE_CLOSE_DEFECT_STATISTIC_CCN);
    }

    @Bean
    public DirectExchange ccnCloseDefectStatisticExchange() {
        DirectExchange directExchange = new DirectExchange(EXCHANGE_CLOSE_DEFECT_STATISTIC_CCN);
        directExchange.setDelayed(true);
        return directExchange;
    }

    @Bean
    public Binding ccnCloseDefectStatisticQueueBind(Queue ccnCloseDefectStatisticQueue,
                                                    DirectExchange ccnCloseDefectStatisticExchange) {
        return BindingBuilder.bind(ccnCloseDefectStatisticQueue)
                .to(ccnCloseDefectStatisticExchange)
                .with(ROUTE_CLOSE_DEFECT_STATISTIC_CCN);
    }

    @Bean
    public SimpleMessageListenerContainer ccnCloseDefectStatisticMessageListenerContainer(
            ConnectionFactory connectionFactory,
            CCNCloseDefectStatisticConsumer ccnCloseDefectStatisticConsumer,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter)
    {
        return getSimpleMessageListenerContainer(QUEUE_CLOSE_DEFECT_STATISTIC_CCN, ccnCloseDefectStatisticConsumer,
                8, 8, connectionFactory, jackson2JsonMessageConverter);
    }

    @Bean
    public Queue closeDefectStatisticQueue() {
        return new Queue(QUEUE_CLOSE_DEFECT_STATISTIC);
    }

    @Bean
    public DirectExchange closeDefectStatisticExchange() {
        DirectExchange directExchange = new DirectExchange(EXCHANGE_CLOSE_DEFECT_STATISTIC);
        directExchange.setDelayed(true);
        return directExchange;
    }

    @Bean
    public Binding closeDefectStatisticQueueBind(Queue closeDefectStatisticQueue,
                                                 DirectExchange closeDefectStatisticExchange) {
        return BindingBuilder.bind(closeDefectStatisticQueue)
                .to(closeDefectStatisticExchange)
                .with(ROUTE_CLOSE_DEFECT_STATISTIC);
    }

    @Bean
    public SimpleMessageListenerContainer closeDefectStatisticMessageListenerContainer(
            ConnectionFactory connectionFactory,
            CommonCloseDefectStatisticConsumer commonCloseDefectStatisticConsumer,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter)
    {
        return getSimpleMessageListenerContainer(QUEUE_CLOSE_DEFECT_STATISTIC, commonCloseDefectStatisticConsumer,
                8, 8, connectionFactory, jackson2JsonMessageConverter);
    }

    @NotNull
    protected SimpleMessageListenerContainer getSimpleMessageListenerContainer(
            String queueName,
            IConsumer consumer,
            int concurrentConsumers,
            int maxConcurrentConsumers,
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter)
    {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(queueName);
        container.setConcurrentConsumers(concurrentConsumers);
        container.setMaxConcurrentConsumers(maxConcurrentConsumers);
        container.setStartConsumerMinInterval(10000);
        MessageListenerAdapter adapter = new MessageListenerAdapter(consumer, "consumer");
        adapter.setMessageConverter(jackson2JsonMessageConverter);
        container.setMessageListener(adapter);
        return container;
    }
}
