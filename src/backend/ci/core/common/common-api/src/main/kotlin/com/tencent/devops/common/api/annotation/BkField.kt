package com.tencent.devops.common.api.annotation

import com.tencent.devops.common.api.validation.NotEmptyValidator
import javax.validation.Constraint
import javax.validation.Payload
import kotlin.reflect.KClass

@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [NotEmptyValidator::class])
annotation class BkField(
    val message: String = "this string may be empty",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)