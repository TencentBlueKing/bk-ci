package com.tencent.devops.monitoring.pojo.annotions

/**
 * 元素放入tag
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class InfluxTag