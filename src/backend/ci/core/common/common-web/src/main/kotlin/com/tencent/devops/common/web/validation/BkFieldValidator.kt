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
import com.tencent.devops.common.web.annotation.BkField
import com.tencent.devops.common.web.constant.BkStyleEnum
import com.tencent.devops.common.web.utils.I18nUtil
import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl
import java.util.regex.Pattern
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext

class BkFieldValidator : ConstraintValidator<BkField?, Any?> {

    override fun initialize(parameters: BkField?) = Unit

    /**
     * 实现ConstraintValidator完成自定义校验
     * @param paramValue 参数值
     * @param constraintValidatorContext 约束校验器上下文
     * @return 参数值是否合法
     */
    @SuppressWarnings("ReturnCount")
    override fun isValid(paramValue: Any?, constraintValidatorContext: ConstraintValidatorContext): Boolean {
        val constraintDescriptor = (constraintValidatorContext as ConstraintValidatorContextImpl).constraintDescriptor
        val attributes = constraintDescriptor.attributes
        val required = attributes[REQUIRED] as Boolean // 字段是否必填
        var message = attributes[MESSAGE] as String // 获取接口参数校验的默认错误描述
        // 1、判断参数是否可以为空
        var flag = false
        if (paramValue == null || (paramValue is String && paramValue.isBlank())) {
            flag = true
        }
        if (required && flag) {
            // 如果参数是必填的且值为空则给用户错误提示
            message = I18nUtil.getCodeLanMessage(
                messageCode = CommonMessageCode.PARAMETER_IS_EMPTY,
                defaultMessage = message
            )
            setErrorMessage(constraintValidatorContext, message)
            return false
        }
        // 2、判断参数的长度是否符合规范
        val paramValueStr = paramValue.toString()
        val minLength = attributes[MIN_LENGTH] as Int // 获取参数最小长度
        if (minLength > 0 && paramValueStr.length < minLength) {
            // 参数最小长度不符合要求则给用户错误提示
            message = I18nUtil.getCodeLanMessage(
                messageCode = CommonMessageCode.PARAMETER_LENGTH_TOO_SHORT,
                defaultMessage = message,
                params = arrayOf(minLength.toString())
            )
            setErrorMessage(constraintValidatorContext, message)
            return false
        }
        val maxLength = attributes[MAX_LENGTH] as Int // 获取参数最大长度
        if (maxLength > 0 && paramValueStr.length > maxLength) {
            // 参数最大长度不符合要求则给用户错误提示
            message = I18nUtil.getCodeLanMessage(
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
            message = I18nUtil.getCodeLanMessage(
                messageCode = patternStyle.name,
                defaultMessage = message
            )
            setErrorMessage(constraintValidatorContext, message)
            return false
        }
        return true
    }

    /**
     * 设置自定义错误信息
     * @param constraintValidatorContext 约束校验器上下文
     * @param message 错误信息
     * @return
     */
    private fun setErrorMessage(constraintValidatorContext: ConstraintValidatorContext, message: String) {
        // 设置自定义错误信息
        constraintValidatorContext.disableDefaultConstraintViolation()
        constraintValidatorContext.buildConstraintViolationWithTemplate(message).addConstraintViolation()
    }
}
