package com.tencent.devops.common.api.annotation

import com.tencent.devops.common.api.validation.BkFieldValidator
import javax.validation.Constraint
import javax.validation.Payload
import kotlin.reflect.KClass

@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [BkFieldValidator::class])
annotation class BkField(
    val patternStyle: String = "commonStyle",
    val message: String = "parameter error",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)