package com.tencent.devops.experience.filter.annotions

/**
 * 允许外部用户访问
 */
@Target(AnnotationTarget.FUNCTION)
@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
annotation class AllowOuter
