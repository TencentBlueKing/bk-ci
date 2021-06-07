package com.tencent.devops.common.api.annotation

import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import javax.ws.rs.NameBinding

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.TYPE)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@NameBinding
@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
annotation class UserLogin {
}