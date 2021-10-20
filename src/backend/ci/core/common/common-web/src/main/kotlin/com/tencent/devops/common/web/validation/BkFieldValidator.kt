/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.web.validation

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.MAX_LENGTH
import com.tencent.devops.common.api.constant.MESSAGE
import com.tencent.devops.common.api.constant.MIN_LENGTH
import com.tencent.devops.common.api.constant.PATTERN_STYLE
import com.tencent.devops.common.api.constant.REQUIRED
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.common.web.annotation.BkField
import com.tencent.devops.common.web.constant.BkStyleEnum
import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl
import java.util.regex.Pattern
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext

class BkFieldValidator : ConstraintValidator<BkField?, Any?> {

    override fun initialize(parameters: BkField?) = Unit

    @SuppressWarnings("ReturnCount")
    override fun isValid(paramValue: Any?, constraintValidatorContext: ConstraintValidatorContext): Boolean {
        val constraintDescriptor = (constraintValidatorContext as ConstraintValidatorContextImpl).constraintDescriptor
        val attributes = constraintDescriptor.attributes
        val required = attributes[REQUIRED] as Boolean
        var message = attributes[MESSAGE] as String // 获取接口参数校验的默认错误描述
        // 1、判断参数是否可以为空
        var flag = false
        if (paramValue == null || (paramValue is String && paramValue.isBlank())) {
            flag = true
        }
        if (required && flag) {
            message = MessageCodeUtil.getCodeLanMessage(CommonMessageCode.PARAMETER_IS_EMPTY, message)
            setErrorMessage(constraintValidatorContext, message)
            return false
        }
        // 2、判断参数的长度是否符合规范
        val paramValueStr = paramValue.toString()
        val minLength = attributes[MIN_LENGTH] as Int
        if (minLength > 0 && paramValueStr.length < minLength) {
            message = MessageCodeUtil.getCodeLanMessage(
                messageCode = CommonMessageCode.PARAMETER_LENGTH_TOO_SHORT,
                defaultMessage = message,
                params = arrayOf(minLength.toString())
            )
            setErrorMessage(constraintValidatorContext, message)
            return false
        }
        val maxLength = attributes[MAX_LENGTH] as Int
        if (maxLength > 0 && paramValueStr.length > maxLength) {
            message = MessageCodeUtil.getCodeLanMessage(
                messageCode = CommonMessageCode.PARAMETER_LENGTH_TOO_LONG,
                defaultMessage = message,
                params = arrayOf(maxLength.toString())
            )
            setErrorMessage(constraintValidatorContext, message)
            return false
        }
        val patternStyle = attributes[PATTERN_STYLE] as BkStyleEnum
        // 3、判断参数值是否满足配置的正则表达式规范
        if (!flag && !Pattern.matches(patternStyle.style, paramValueStr)) {
            message = MessageCodeUtil.getCodeLanMessage(patternStyle.name, message)
            setErrorMessage(constraintValidatorContext, message)
            return false
        }
        return true
    }

    private fun setErrorMessage(constraintValidatorContext: ConstraintValidatorContext, message: String) {
        // 设置错误信息
        constraintValidatorContext.disableDefaultConstraintViolation()
        constraintValidatorContext.buildConstraintViolationWithTemplate(message).addConstraintViolation()
    }
}
