package com.tencent.devops.plugin.task

import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

/**
 * Created by Aaron Sheng on 2018/5/1.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FILE)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
annotation class Task