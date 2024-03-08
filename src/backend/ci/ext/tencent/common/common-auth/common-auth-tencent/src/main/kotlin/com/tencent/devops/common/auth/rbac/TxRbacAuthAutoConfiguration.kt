package com.tencent.devops.common.auth.rbac

import com.tencent.devops.common.auth.rbac.code.RbacExperienceAuthServiceCode
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered

@Configuration
@ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "rbac")
@ConditionalOnWebApplication
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
class TxRbacAuthAutoConfiguration {
    @Bean
    fun experienceAuthServiceCode() = RbacExperienceAuthServiceCode()
}
