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

package com.tencent.devops.quality.constant

const val QUALITY_RULE_TEMPLATE_NAME_KEY = "quality.ruleTemplate.%s.name" // $s == ID
const val QUALITY_RULE_TEMPLATE_DESC_KEY = "quality.ruleTemplate.%s.desc" // $s == ID
const val QUALITY_RULE_TEMPLATE_STAGE_KEY = "quality.ruleTemplate.%s.stage" // $s == ID
const val QUALITY_CONTROL_POINT_NAME_KEY = "quality.controlPoint.%s.name" // $s == elementType
const val QUALITY_CONTROL_POINT_STAGE_KEY = "quality.controlPoint.%s.stage" // $s == elementType
const val QUALITY_METADATA_DATA_NAME_KEY = "quality.metadata.%s.dataName" // $s == ID
const val QUALITY_METADATA_DATA_ELEMENT_NAME_KEY = "quality.metadata.%s.elementName" // $s == ID
const val QUALITY_METADATA_DATA_DESC_KEY = "quality.metadata.%s.desc" // $s == ID
const val QUALITY_INDICATOR_ELEMENT_NAME_KEY = "quality.indicator.%s.elementName" // $s == ID
const val QUALITY_INDICATOR_NAME_KEY = "quality.indicator.%s.name" // $s == ID
const val QUALITY_INDICATOR_DESC_KEY = "quality.indicator.%s.desc" // $s == ID
const val BK_BEFORE_POSITION = "bkBeforePosition" // 准入-满足条件才能执行控制点
const val BK_AFTER_POSITION = "bkAfterPosition" // 准出-满足条件才能执行后续插件
const val BK_PROJECT_UNDER_NO_QUALITY_USER_GROUP = "bkProjectUnderNoQualityUserGroup" // 项目下无Quality用户组
const val BK_PASSED = "bkPassed" // 已通过：
const val BK_BLOCKED = "bkBlocked" // 已拦截：
const val BK_CURRENT_VALUE = "bkCurrentValue" // {0}当前值({1})，期望{2}
const val BK_VALIDATION_PASSED = "bkValidationPassed" // {0}(#{1})在{2}验证通过
const val BK_VALIDATION_INTERCEPTED = "bkValidationIntercepted" // {0}(#{1})在{2}验证被拦截
const val BK_INTERCEPTION_RULES = "bkInterceptionRules" // 拦截规则
const val BK_INTERCEPTION_METRICS = "bkInterceptionMetrics" // 拦截指标
const val BK_BUILD_INTERCEPTED_TO_BE_REVIEWED = "bkBuildInterceptedToBeReviewed" // {0}({1})被拦截，待审核(审核人{2})
const val BK_BUILD_INTERCEPTED_TERMINATED = "bkBuildInterceptedTerminated" // {0}(#{1})被拦截，已终止
const val BK_NO_TOOL_OR_RULE_ENABLED = "bkNoToolOrRuleEnabled" // 你可能并未添加工具或打开相应规则。
// 用户没有拦截规则{0}权限
const val BK_USER_NO_OPERATE_INTERCEPT_RULE_PERMISSION = "bkUserNoOperateInterceptRulePermission"
const val BK_CREATE_SUCCESS = "bkCreateSuccess" // 创建成功
const val BK_CREATE_FAIL = "bkCreateFail" // 未知的异常，创建失败
const val BK_UPDATE_FAIL = "bkUpdateFail" // 未知的异常，更新失败
const val BK_METRIC_DATA_UPDATE_SUCCESS = "bkMetricDataUpdateSuccess" // 更新指标数据成功

const val BK_TOOL_DESC_STANDARD = "bkToolDescStandard" // 代码规范
const val BK_TOOL_DESC_DEFECT = "bkToolDescDefect" // 代码缺陷
const val BK_TOOL_DESC_SECURITY = "bkToolDescSecurity" // 安全漏洞
const val BK_TOOL_DESC_RIPS = "bkToolDescRips" // 啄木鸟漏洞扫描
const val BK_TOOL_DESC_SENSITIVE = "bkToolDescSensitive" // 敏感信息
const val BK_TOOL_DESC_WOODPECKER_SENSITIVE = "bkToolDescWoodpeckerSensitive" // 啄木鸟敏感信息
const val BK_TOOL_DESC_CCN = "bkToolDescCcn" // 圈复杂度
const val BK_TOOL_DESC_DUPC = "bkToolDescDupc" // 重复率
const val BK_TOOL_NAME_STANDARD = "bkToolNameStandard" // 按维度(推荐)
const val BK_TOOL_NAME_DEFECT = "bkToolNameDefect" // 按维度(推荐)
const val BK_TOOL_NAME_SECURITY = "bkToolNameSecurity" // 按维度(推荐)
const val BK_TOOL_NAME_CCN = "bkToolNameCcn" // 通过计算函数的节点个数来衡量代码复杂性
const val BK_TOOL_NAME_DUPC = "bkToolNameDupc" // 可以检测项目中复制粘贴和重复开发相同功能等问题
const val BK_TOOL_NAME_COVERITY = "bkToolNameCoverity" // 斯坦福大学科学家研究成果，静态源代码分析领域的领导者
const val BK_TOOL_NAME_KLOCWORK = "bkToolNameKlocwork" // 业界广泛使用的商用代码检查工具，与Coverity互补
const val BK_TOOL_NAME_CPPLINT = "bkToolNameCpplint" // 谷歌开源的C++代码风格检查工具
const val BK_TOOL_NAME_ESLINT = "bkToolNameEslint" // JavaScript代码检查工具
const val BK_TOOL_NAME_PYLINT = "bkToolNamePylint" // Python代码风格检查工具
const val BK_TOOL_NAME_GOML = "bkToolNameGoml" // Golang静态代码分析工具
const val BK_TOOL_NAME_CHECKSTYLE = "bkToolNameCheckstyle" // Java代码风格检查工具
const val BK_TOOL_NAME_STYLECOP = "bkToolNameStylecop" // 微软开源的C#静态代码分析工具
const val BK_TOOL_NAME_DETEKT = "bkToolNameDetekt" // Kotlin静态代码分析工具
const val BK_TOOL_NAME_PHPCS = "bkToolNamePhpcs" // PHP代码风格检查工具
const val BK_TOOL_NAME_SENSITIVE = "bkToolNameSensitive" // 可扫描代码中有安全风险的敏感信息
const val BK_TOOL_NAME_OCCHECK = "bkToolNameOccheck" // OC代码风格检查工具
const val BK_TOOL_NAME_WOODPECKER_SENSITIVE = "bkToolNameWoodpeckerSensitive" // 敏感信息检查工具
const val BK_TOOL_NAME_BKCHECK_CPP = "bkToolNameBkcheckCpp" // C++代码风格检查工具
const val BK_TOOL_NAME_BKCHECK_OC = "bkToolNameBkcheckOc" // OC代码风格检查工具
