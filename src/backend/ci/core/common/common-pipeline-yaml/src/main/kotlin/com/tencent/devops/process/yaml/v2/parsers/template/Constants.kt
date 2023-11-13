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

package com.tencent.devops.process.yaml.v2.parsers.template

object Constants {
    const val templateDirectory = ".ci/templates/"

    // 引用模板的关键字
    const val TEMPLATE_KEY = "template"

    //  模板变量关键字
    const val PARAMETERS_KEY = "parameters"

    // 对象类型模板Job,Variable的模板路径关键字
    const val OBJECT_TEMPLATE_PATH = "name"

    // 分隔远程库和文件关键字
    const val FILE_REPO_SPLIT = "@"

    // 模板最多引用数和最大深度
    const val MAX_TEMPLATE_NUMB = 10
    const val MAX_TEMPLATE_DEEP = 5

    // 质量红线数量和红线中的规则数量
    const val STAGE_CHECK_GATE_NUMB = 10
    const val STAGE_CHECK_GATE_RULE_NUMB = 100

    // 异常模板
    const val TEMPLATE_ID_DUPLICATE = "Format error: ID [%s] in template [%s] and template [%s] are duplicated"
    const val TEMPLATE_ROOT_ID_DUPLICATE = "[%s] Format error: IDs [%s] are duplicated"
    const val TRANS_AS_ERROR = "[%s]Keyword [%s] format error"
    const val REPO_NOT_FOUND_ERROR =
        "[%s]The referenced repository [%s] should first be declared by the resources keyword"
    const val REPO_CYCLE_ERROR = "Repository: Cyclic dependency"
    const val TEMPLATE_CYCLE_ERROR = "There is a [%s] circular dependency in template [%s] and template [%s]"

    //        const val TEMPLATE_NUMB_BEYOND =
//            "[%s]The number of referenced template files exceeds the threshold [$MAX_TEMPLATE_NUMB] "
//        const val TEMPLATE_DEEP_BEYOND = "[%s]The template nesting depth exceeds the threshold [$MAX_TEMPLATE_DEEP]"
//        const val TEMPLATE_FORMAT_ERROR = "[%s]Template YAML does not meet the specification"
    const val YAML_FORMAT_ERROR = "[%s] Format error: %s"
    const val ATTR_MISSING_ERROR = "[%s]Required attributes [%s] are missing"

    //        const val TEMPLATE_KEYWORDS_ERROR = "[%s]Template YAML does not meet the specification. " +
//            "The %s template can only contain parameters, resources and %s keywords"
//        const val EXTENDS_TEMPLATE_EXTENDS_ERROR = "[%s]The extends keyword cannot be nested"
//        const val EXTENDS_TEMPLATE_ON_ERROR = "[%s]Triggers are not supported in the template"
    const val VALUE_NOT_IN_ENUM = "[%s to %s][%s=%s]Parameter error, the expected value is [%s]"
    const val PARAMETER_FORMAT_ERROR = "[%s]Parameter format error [%s]"
    const val EXPRESSION_EVALUATE_ERROR = "[%s]Expression [%s] evaluate error [%s]"

    //        const val FINALLY_FORMAT_ERROR = "final stage not support stage's template"
    const val STAGE_CHECK_GATE_NUMB_BEYOND =
        "[%s][%s]The number of gates reaches the limit:  no more than $STAGE_CHECK_GATE_NUMB. "
    const val STAGE_CHECK_GATE_RULE_NUMB_BEYOND =
        "[%s][%s][%s]The number of rules reaches the limit:  no more than $STAGE_CHECK_GATE_RULE_NUMB. "
}
