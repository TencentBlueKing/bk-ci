package com.tencent.devops.multijar

import com.tencent.devops.leaf.plugin.LeafSpringBootProperties
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.Ordered

@Configuration
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
class MultijarCommonConfiguration {
    @Bean
    @Primary
    fun leafSpringBootProperties(): LeafSpringBootProperties {
        return LeafSpringBootProperties()
    }
}
