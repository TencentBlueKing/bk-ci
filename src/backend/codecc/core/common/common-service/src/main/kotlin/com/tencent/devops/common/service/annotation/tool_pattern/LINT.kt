package com.tencent.devops.common.service.annotation.tool_pattern

import java.lang.annotation.Inherited

@Inherited
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class LINT
