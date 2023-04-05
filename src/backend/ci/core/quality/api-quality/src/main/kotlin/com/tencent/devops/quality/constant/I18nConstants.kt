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

const val BK_PROJECT_QUALITY_RULE = "bkProjectQualityRule"// 项目下红线规则
const val BK_PROJECT_UNDER_NO_QUALITY_USER_GROUP = "bkProjectUnderNoQualityUserGroup"// 项目下无红线用户组
const val BK_PROJECT_QUALITY_GROUPING= "bkProjectQualityGrouping"// 项目下红线分组
const val BK_PASSED = "bkPassed"// 已通过：
const val BK_BLOCKED= "bkBlocked"// 已拦截：
const val BK_CURRENT_VALUE = "bkCurrentValue"// {0}当前值({1})，期望${2}\n
const val BK_VALIDATION_PASSED = "bkValidationPassed"// {0}(#{1})在{2}验证通过
const val BK_VALIDATION_INTERCEPTED = "bkValidationIntercepted"// {0}(#{1})在{2}验证被拦截
const val BK_INTERCEPTION_RULES = "bkInterceptionRules"// 拦截规则
const val BK_INTERCEPTION_METRICS = "bkInterceptionMetrics"// 拦截指标
const val BK_BUILD_INTERCEPTED_TO_BE_REVIEWED = "bkBuildInterceptedToBeReviewed"// {0}({1})被拦截，待审核(审核人{2})
const val BK_BUILD_INTERCEPTED_TERMINATED = "bkBuildInterceptedTerminated"// {0}(#{1})被拦截，已终止
const val BK_NO_TOOL_OR_RULE_ENABLED = "bkNoToolOrRuleEnabled"// 你可能并未添加工具或打开相应规则。
const val BK_USER_NO_OPERATE_INTERCEPT_RULE_PERMISSION = "bkUserNoOperateInterceptRulePermission"// 用户没有拦截规则{0}权限
const val BK_CREATE_SUCCESS = "bkCreateSuccess"// 创建成功
const val BK_CREATE_FAIL = "bkCreateFail"// 未知的异常，创建失败
const val BK_UPDATE_FAIL = "bkUpdateFail"// 未知的异常，更新失败
const val BK_METRIC_DATA_UPDATE_SUCCESS = "bkMetricDataUpdateSuccess" // 更新指标数据成功
