package com.tencent.devops.process.config

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.process.bean.GitCIPipelineUrlBeanImpl
import com.tencent.devops.process.bean.TencentPipelineUrlBeanImpl
import com.tencent.devops.process.engine.atom.parser.DispatchTypeParser
import com.tencent.devops.process.engine.atom.parser.DispatchTypeParserTxImpl
import com.tencent.devops.process.service.BuildVariableService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered

/**
 * 流水线引擎初始化配置类
 *
 * @version 1.0
 */

@Configuration
@ConditionalOnWebApplication
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
class TxPipelineEngineConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "cluster", name = ["tag"], havingValue = "devops")
    fun dispatchTypeParser(
        @Autowired client: Client,
        @Autowired commonDispatchTypeParser: DispatchTypeParser,
        @Autowired buildVariableService: BuildVariableService
    ) = DispatchTypeParserTxImpl(client, commonDispatchTypeParser, buildVariableService)

    @Bean
    @ConditionalOnProperty(prefix = "cluster", name = ["tag"], havingValue = "stream")
    fun pipelineUrlBean(
        @Autowired commonConfig: CommonConfig,
        @Autowired client: Client
    ) = TencentPipelineUrlBeanImpl(commonConfig, client)
}
