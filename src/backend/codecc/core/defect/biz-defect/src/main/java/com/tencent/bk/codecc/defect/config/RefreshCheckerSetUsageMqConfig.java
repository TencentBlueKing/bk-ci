package com.tencent.bk.codecc.defect.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_REFRESH_CHECKERSET_USAGE;
import static com.tencent.devops.common.web.mq.ConstantsKt.QUEUE_REFRESH_CHECKERSET_USAGE;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_REFRESH_CHECKERSET_USAGE;

@Configuration
public class RefreshCheckerSetUsageMqConfig {
    @Bean
    public DirectExchange refreshCheckersetUsageExchange() {
        DirectExchange directExchange = new DirectExchange(EXCHANGE_REFRESH_CHECKERSET_USAGE);
        directExchange.setDelayed(true);
        return directExchange;
    }

    @Bean
    public Binding refreshCheckersetUsageBind(Queue refreshCheckersetUsageQueue, DirectExchange refreshCheckersetUsageExchange)
    {
        return BindingBuilder.bind(refreshCheckersetUsageQueue).to(refreshCheckersetUsageExchange).with(ROUTE_REFRESH_CHECKERSET_USAGE);
    }

    @Bean
    public Queue refreshCheckersetUsageQueue() {
        return new Queue(QUEUE_REFRESH_CHECKERSET_USAGE);
    }
}
