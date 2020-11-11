package com.tencent.bk.codecc.schedule.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_ANALYSIS_VERSION;
import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_ANALYZE_DISPATCH;
import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_CHECK_THREAD_ALIVE;
import static com.tencent.devops.common.web.mq.ConstantsKt.QUEUE_ANALYSIS_VERSION;
import static com.tencent.devops.common.web.mq.ConstantsKt.QUEUE_ANALYZE_DISPATCH;
import static com.tencent.devops.common.web.mq.ConstantsKt.QUEUE_CHECK_THREAD_ALIVE;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_ANALYSIS_VERSION;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_ANALYZE_DISPATCH;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_CHECK_THREAD_ALIVE;

@Configuration
public class ScheduleCommonMqConfig {

    // analyze

    @Bean
    public DirectExchange analyzeVersionExchange() {
        DirectExchange directExchange = new DirectExchange(EXCHANGE_ANALYSIS_VERSION);
        directExchange.setDelayed(true);
        return directExchange;
    }

    @Bean
    public Queue analyzeVersionQueue() {
        return new Queue(QUEUE_ANALYSIS_VERSION);
    }

    @Bean
    public Binding analyzeVersionBind(Queue analyzeVersionQueue, DirectExchange analyzeVersionExchange)
    {
        return BindingBuilder.bind(analyzeVersionQueue).to(analyzeVersionExchange).with(ROUTE_ANALYSIS_VERSION);
    }

    // dispatch

    @Bean
    public DirectExchange analyzeDispatchExchange()
    {
        DirectExchange directExchange = new DirectExchange(EXCHANGE_ANALYZE_DISPATCH);
        directExchange.setDelayed(true);
        return directExchange;
    }

    @Bean
    public Queue analyzeDispatchQueue()
    {
        return new Queue(QUEUE_ANALYZE_DISPATCH);
    }

    @Bean
    public Binding analyzeDispatchBind(Queue analyzeDispatchQueue, DirectExchange analyzeDispatchExchange)
    {
        return BindingBuilder.bind(analyzeDispatchQueue).to(analyzeDispatchExchange).with(ROUTE_ANALYZE_DISPATCH);
    }

    // check.thread.alive
    @Bean
    public DirectExchange checkThreadAliveExchange() {
        DirectExchange directExchange = new DirectExchange(EXCHANGE_CHECK_THREAD_ALIVE);
        directExchange.setDelayed(true);
        return directExchange;
    }

    @Bean
    public Binding checkThreadAliveBind(Queue checkThreadAliveQueue, DirectExchange checkThreadAliveExchange)
    {
        return BindingBuilder.bind(checkThreadAliveQueue).to(checkThreadAliveExchange).with(ROUTE_CHECK_THREAD_ALIVE);
    }

    @Bean
    public Queue checkThreadAliveQueue() {
        return new Queue(QUEUE_CHECK_THREAD_ALIVE);
    }
}
