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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.web.validation

import com.tencent.devops.common.api.constant.PATTERN_STYLE
import com.tencent.devops.common.api.constant.REQUIRED
import com.tencent.devops.common.web.annotation.BkField
import com.tencent.devops.common.web.constant.BkStyleEnum
import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl
import java.util.regex.Pattern
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext

class BkFieldValidator : ConstraintValidator<BkField?, Any?> {

    override fun initialize(parameters: BkField?) {}

    override fun isValid(
        paramValue: Any?,
        constraintValidatorContext: ConstraintValidatorContext
    ): Boolean {
        val constraintDescriptor = (constraintValidatorContext as ConstraintValidatorContextImpl).constraintDescriptor
        val attributes = constraintDescriptor.attributes
        val required = attributes[REQUIRED] as Boolean
        // 判断参数是否可以为空
        var flag = false
        if (paramValue == null || (paramValue is String && paramValue.isBlank())) {
            flag = true
        }
        if (required && flag) {
            return false
        }
        val patternStyle = attributes[PATTERN_STYLE] as BkStyleEnum
        // 判断参数值是否满足配置的正则表达式规范
        if (!flag && !Pattern.matches(patternStyle.style, paramValue.toString())) {
            return false
        }
        return true
    }
}