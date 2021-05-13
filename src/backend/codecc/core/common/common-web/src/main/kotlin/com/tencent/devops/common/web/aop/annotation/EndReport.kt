package com.tencent.devops.common.web.aop.annotation

import java.lang.annotation.Inherited

@Inherited
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class EndReport (val isOpenSource: Boolean)