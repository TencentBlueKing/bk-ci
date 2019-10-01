package com.tencent.devops.plugin.codecc

import com.tencent.devops.plugin.codecc.config.CodeccConfig
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered

@Configuration
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
class AutoConfiguration {

    @Bean
    fun codeccConfig() = CodeccConfig()

    @Bean
    fun coverityApi(codeccConfig: CodeccConfig): CodeccApi {
        return CodeccApi(
            codeccApiUrl = codeccConfig.codeccApiGateWay,
            createPath = codeccConfig.createPath,
            deletePath = codeccConfig.deletePath,
            updatePath = codeccConfig.updatePath,
            existPath = codeccConfig.existPath,
            report = codeccConfig.report,
            getRuleSetsPath = codeccConfig.getRuleSetsPath
        )
    }
}