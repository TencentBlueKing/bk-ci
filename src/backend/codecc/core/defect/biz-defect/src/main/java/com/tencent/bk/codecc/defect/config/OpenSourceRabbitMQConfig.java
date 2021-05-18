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
import org.springframework.context.annotation.Configuration;

import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_CLOSE_DEFECT_STATISTIC_CCN_OPENSOURCE;
import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_CLOSE_DEFECT_STATISTIC_LINT_OPENSOURCE;
import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_CLOSE_DEFECT_STATISTIC_OPENSOURCE;
import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_DEFECT_COMMIT_CLUSTER_OPENSOURCE;
import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_DEFECT_COMMIT_METRICS_OPENSOURCE;
import static com.tencent.devops.common.web.mq.ConstantsKt.QUEUE_CLOSE_DEFECT_STATISTIC_CCN_OPENSOURCE;
import static com.tencent.devops.common.web.mq.ConstantsKt.QUEUE_CLOSE_DEFECT_STATISTIC_LINT_OPENSOURCE;
import static com.tencent.devops.common.web.mq.ConstantsKt.QUEUE_CLOSE_DEFECT_STATISTIC_OPENSOURCE;
import static com.tencent.devops.common.web.mq.ConstantsKt.QUEUE_DEFECT_COMMIT_CLUSTER_OPENSOURCE;
import static com.tencent.devops.common.web.mq.ConstantsKt.QUEUE_DEFECT_COMMIT_METRICS_OPENSOURCE;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_CLOSE_DEFECT_STATISTIC_CCN_OPENSOURCE;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_CLOSE_DEFECT_STATISTIC_LINT_OPENSOURCE;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_CLOSE_DEFECT_STATISTIC_OPENSOURCE;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_DEFECT_COMMIT_CLUSTER_OPENSOURCE;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_DEFECT_COMMIT_METRICS_OPENSOURCE;

/**
 * 普通非告警上报的开源项目消息队列配置
 *
 * @version V1.0
 * @date 2020/09/22
 */
@Configuration
@Slf4j
@ConditionalOnProperty(prefix = "spring.application", name = "name", havingValue = "opensourcereport")
public class OpenSourceRabbitMQConfig
{
    @Bean
    public Queue metricsCommitQueue() {
        return new Queue(QUEUE_DEFECT_COMMIT_METRICS_OPENSOURCE);
    }

    @Bean
    public DirectExchange metricsDirectExchange() {
        DirectExchange directExchange = new DirectExchange(EXCHANGE_DEFECT_COMMIT_METRICS_OPENSOURCE);
        directExchange.setDelayed(true);
        return directExchange;
    }

    @Bean
    public Binding metricsQueueBind(Queue metricsCommitQueue, DirectExchange metricsDirectExchange) {
        return BindingBuilder.bind(metricsCommitQueue)
                .to(metricsDirectExchange)
                .with(ROUTE_DEFECT_COMMIT_METRICS_OPENSOURCE);
    }

    @Bean
    public SimpleMessageListenerContainer metricsMessageListenerContainer(
            ConnectionFactory connectionFactory,
            CodeScoringConsumer codeScoringConsumer,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter)
    {
        return getSimpleMessageListenerContainer(QUEUE_DEFECT_COMMIT_METRICS_OPENSOURCE, codeScoringConsumer,
                10, 10, connectionFactory, jackson2JsonMessageConverter);
    }

    @Bean
    public Queue clusterCommitQueue() {
        return new Queue(QUEUE_DEFECT_COMMIT_CLUSTER_OPENSOURCE);
    }

    @Bean
    public DirectExchange clusterDirectExchange() {
        DirectExchange directExchange = new DirectExchange(EXCHANGE_DEFECT_COMMIT_CLUSTER_OPENSOURCE);
        directExchange.setDelayed(true);
        return directExchange;
    }

    @Bean
    public Binding clusterQueueBind(Queue clusterCommitQueue, DirectExchange clusterDirectExchange) {
        return BindingBuilder.bind(clusterCommitQueue)
                .to(clusterDirectExchange)
                .with(ROUTE_DEFECT_COMMIT_CLUSTER_OPENSOURCE);
    }

    @Bean
    public SimpleMessageListenerContainer clusterMessageListenerContainer(
            ConnectionFactory connectionFactory,
            ClusterDefectConsumer clusterDefectConsumer,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter)
    {
        return getSimpleMessageListenerContainer(QUEUE_DEFECT_COMMIT_CLUSTER_OPENSOURCE, clusterDefectConsumer,
                10, 10, connectionFactory, jackson2JsonMessageConverter);
    }

    @Bean
    public Queue openLintCloseDefectStatisticQueue() {
        return new Queue(QUEUE_CLOSE_DEFECT_STATISTIC_LINT_OPENSOURCE);
    }

    @Bean
    public DirectExchange openLintCloseDefectStatisticExchange() {
        DirectExchange directExchange = new DirectExchange(EXCHANGE_CLOSE_DEFECT_STATISTIC_LINT_OPENSOURCE);
        directExchange.setDelayed(true);
        return directExchange;
    }

    @Bean
    public Binding openLintCloseDefectStatisticQueueBind(Queue openLintCloseDefectStatisticQueue,
                                                     DirectExchange openLintCloseDefectStatisticExchange) {
        return BindingBuilder.bind(openLintCloseDefectStatisticQueue)
                .to(openLintCloseDefectStatisticExchange)
                .with(ROUTE_CLOSE_DEFECT_STATISTIC_LINT_OPENSOURCE);
    }

    @Bean
    public SimpleMessageListenerContainer openLintCloseDefectStatisticMessageListenerContainer(
            ConnectionFactory connectionFactory,
            LintCloseDefectStatisticConsumer lintCloseDefectStatisticConsumer,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter)
    {
        return getSimpleMessageListenerContainer(QUEUE_CLOSE_DEFECT_STATISTIC_LINT_OPENSOURCE,
                lintCloseDefectStatisticConsumer, 8, 8,
                connectionFactory, jackson2JsonMessageConverter);
    }

    @Bean
    public Queue openCcnCloseDefectStatisticQueue() {
        return new Queue(QUEUE_CLOSE_DEFECT_STATISTIC_CCN_OPENSOURCE);
    }

    @Bean
    public DirectExchange openCcnCloseDefectStatisticExchange() {
        DirectExchange directExchange = new DirectExchange(EXCHANGE_CLOSE_DEFECT_STATISTIC_CCN_OPENSOURCE);
        directExchange.setDelayed(true);
        return directExchange;
    }

    @Bean
    public Binding openCcnCloseDefectStatisticQueueBind(Queue openCcnCloseDefectStatisticQueue,
                                                    DirectExchange openCcnCloseDefectStatisticExchange) {
        return BindingBuilder.bind(openCcnCloseDefectStatisticQueue)
                .to(openCcnCloseDefectStatisticExchange)
                .with(ROUTE_CLOSE_DEFECT_STATISTIC_CCN_OPENSOURCE);
    }

    @Bean
    public SimpleMessageListenerContainer openCcnCloseDefectStatisticMessageListenerContainer(
            ConnectionFactory connectionFactory,
            CCNCloseDefectStatisticConsumer ccnCloseDefectStatisticConsumer,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter)
    {
        return getSimpleMessageListenerContainer(QUEUE_CLOSE_DEFECT_STATISTIC_CCN_OPENSOURCE, ccnCloseDefectStatisticConsumer,
                8, 8, connectionFactory, jackson2JsonMessageConverter);
    }

    @Bean
    public Queue openCloseDefectStatisticQueue() {
        return new Queue(QUEUE_CLOSE_DEFECT_STATISTIC_OPENSOURCE);
    }

    @Bean
    public DirectExchange openCloseDefectStatisticExchange() {
        DirectExchange directExchange = new DirectExchange(EXCHANGE_CLOSE_DEFECT_STATISTIC_OPENSOURCE);
        directExchange.setDelayed(true);
        return directExchange;
    }

    @Bean
    public Binding openCloseDefectStatisticQueueBind(Queue openCloseDefectStatisticQueue,
                                                 DirectExchange openCloseDefectStatisticExchange) {
        return BindingBuilder.bind(openCloseDefectStatisticQueue)
                .to(openCloseDefectStatisticExchange)
                .with(ROUTE_CLOSE_DEFECT_STATISTIC_OPENSOURCE);
    }

    @Bean
    public SimpleMessageListenerContainer openCloseDefectStatisticMessageListenerContainer(
            ConnectionFactory connectionFactory,
            CommonCloseDefectStatisticConsumer commonCloseDefectStatisticConsumer,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter)
    {
        return getSimpleMessageListenerContainer(QUEUE_CLOSE_DEFECT_STATISTIC_OPENSOURCE,
                commonCloseDefectStatisticConsumer, 8, 8,
                connectionFactory, jackson2JsonMessageConverter);
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
