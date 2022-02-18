package com.tencent.devops.common.api.annotation

@kotlin.annotation.Target(AnnotationTarget.CLASS, AnnotationTarget.FILE)
@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class ServiceInterface(val value: String)
