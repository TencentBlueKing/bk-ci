package com.tencent.bk.codecc.task.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_AUTHOR_TRANS;
import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_OPERATION_HISTORY;
import static com.tencent.devops.common.web.mq.ConstantsKt.QUEUE_AUTHOR_TRANS;
import static com.tencent.devops.common.web.mq.ConstantsKt.QUEUE_OPERATION_HISTORY;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_AUTHOR_TRANS;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_OPERATION_HISTORY;


@Configuration
public class TaskCommonMqConfig {

    //author.trans

    @Bean
    public DirectExchange authorTransDirectExchange()
    {
        DirectExchange directExchange = new DirectExchange(EXCHANGE_AUTHOR_TRANS);
        directExchange.setDelayed(true);
        return directExchange;
    }

    @Bean
    public Queue authorTransQueue()
    {
        return new Queue(QUEUE_AUTHOR_TRANS);
    }

    @Bean
    public Binding authorTransBind(Queue authorTransQueue, DirectExchange authorTransDirectExchange)
    {
        return BindingBuilder.bind(authorTransQueue).to(authorTransDirectExchange).with(ROUTE_AUTHOR_TRANS);
    }

    // operation.history

    @Bean
    public DirectExchange operationHistoryExchange()
    {
        DirectExchange directExchange = new DirectExchange(EXCHANGE_OPERATION_HISTORY);
        directExchange.setDelayed(true);
        return directExchange;
    }

    @Bean
    public Queue operationHistoryQueue()
    {
        return new Queue(QUEUE_OPERATION_HISTORY);
    }

    @Bean
    public Binding operationHistoryBind(Queue operationHistoryQueue, DirectExchange operationHistoryExchange)
    {
        return BindingBuilder.bind(operationHistoryQueue).to(operationHistoryExchange).with(ROUTE_OPERATION_HISTORY);
    }

}
