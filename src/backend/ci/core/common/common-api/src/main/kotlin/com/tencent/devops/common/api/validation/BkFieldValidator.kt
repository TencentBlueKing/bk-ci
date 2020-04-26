package com.tencent.devops.common.api.validation

import com.tencent.devops.common.api.annotation.BkField
import com.tencent.devops.common.api.util.JsonUtil
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext

class BkFieldValidator : ConstraintValidator<BkField?, Any?> {

    override fun initialize(parameters: BkField?) {}

    override fun isValid(
        paramObject: Any?,
        constraintValidatorContext: ConstraintValidatorContext
    ): Boolean {
        println(paramObject?.let { JsonUtil.toJson(it) })
        //return paramObject?.toString()?.isNotEmpty() ?: false
        return false
    }
}