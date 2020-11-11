package com.tencent.devops.common.web.config

import com.tencent.devops.common.web.mq.EXCHANGE_LINT_DEFECT_MIGRATION
import com.tencent.devops.common.web.mq.QUEUE_LINT_DEFECT_MIGRATION
import com.tencent.devops.common.web.mq.ROUTE_LINT_DEFECT_MIGRATION
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.DirectExchange
import org.springframework.amqp.core.Queue
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class LintDefectMigrationMqConfig {
    @Bean
    fun lintDefectMigrationExchange(): DirectExchange {
        val directExchange = DirectExchange(EXCHANGE_LINT_DEFECT_MIGRATION);
        directExchange.isDelayed = true
        return directExchange;
    }

    @Bean
    fun lintDefectMigrationBind(lintDefectMigrationQueue: Queue, lintDefectMigrationExchange: DirectExchange): Binding {
        return BindingBuilder.bind(lintDefectMigrationQueue).to(lintDefectMigrationExchange).with(ROUTE_LINT_DEFECT_MIGRATION);
    }

    @Bean
    fun lintDefectMigrationQueue(): Queue {
        return Queue(QUEUE_LINT_DEFECT_MIGRATION)
    }
}